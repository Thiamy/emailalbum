/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class EmailAlbumViewer extends ListActivity {

	// Options menu items
	private static final int LOAD_ALBUM_ID = 1;
	private static final int SAVE_ALL_ID = 2;
	private static final int ABOUT_ID = 3;

	// Context menu items
	private static final int SAVE_SELECTED_ID = 4;

	private static final int ACTIVITY_PICK_FILE = 1;
	private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL = 2;
	private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED = 3;
	private ArrayList<Map<String, String>> mContentModel;
	private Uri mAlbumFileUri = null;
	private ZipFile mArchive = null;
	private ProgressDialog mProgress;
	private Context context;
	PhotoAdapter mAdapter;

	private static final String KEY_FULLNAME = "imageName";
	private static final String KEY_THUMBNAIL = "thumbnail";
	private static final String KEY_SHORTNAME = "imageShortName";

	protected static final String KEY_THMBCREAT_ENTRY_POSITION = "entryPosition";
	protected static final String KEY_THMBCREAT_THUMB_NAME = "thumbName";
	public static final String TMP_DIR_NAME = "/sdcard/tmp/";

	private class ArchiveRetriever implements Runnable {

		@Override
		public void run() {
			try {
				InputStream intentInputStream = getContentResolver()
						.openInputStream(mAlbumFileUri);
				File tempArchive = File.createTempFile("emailalbum", ".zip",
						getCacheDir());
				OutputStream tempOS = new FileOutputStream(tempArchive);

				byte[] buffer = new byte[256];
				int readBytes = -1;
				while ((readBytes = intentInputStream.read(buffer)) != -1) {
					tempOS.write(buffer, 0, readBytes);
				}
				mAlbumFileUri = Uri.parse(tempArchive.toURI().toString());
				tempOS.close();
				intentInputStream.close();
				archiveCopyHandler.sendEmptyMessage(0);

			} catch (IOException e) {
				Message msg = new Message();
				Bundle data = new Bundle();
				msg.arg1 = -1;
				data.putString("EXCEPTION", e.getLocalizedMessage());
				msg.setData(data);
				archiveCopyHandler.sendMessage(msg);
			}
		}

	}

	private Handler archiveCopyHandler = new Handler() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mProgress.dismiss();
			if (msg.arg1 < 0) {
				Toast.makeText(context, R.string.error_saving,
						Toast.LENGTH_SHORT).show();
			}
			fillData(true);
		}

	};

	private Handler thumbnailsCreationHandler = new Handler() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.arg1 < 0) {
				if (msg.getData().getString("EXCEPTION_CLASS").equals(
						OutOfMemoryError.class.getSimpleName())) {
					Toast.makeText(context, R.string.error_out_of_mem,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(context,
							msg.getData().getString("EXCEPTION"),
							Toast.LENGTH_SHORT).show();
				}
			} else {
				int position = msg.getData().getInt(
						KEY_THMBCREAT_ENTRY_POSITION);
				String thumbName = msg.getData().getString(
						KEY_THMBCREAT_THUMB_NAME);
				setProgress((position+1) * 10000 / mContentModel.size());
				mAdapter.updateThumbnail(position, thumbName);
			}
		}

	};
	private Handler saveAllHandler = new Handler() {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.arg1 < 0) {
				Toast.makeText(context, msg.getData().getShort("EXCEPTION"),
						Toast.LENGTH_SHORT).show();
				setProgressBarVisibility(false);
			} else {
				int position = msg.what;
				setProgress((position + 1) * 10000 / mContentModel.size());
			}
		}

	};

	private ThumbnailsCreator mThmbCreator;
	private int posPictureToSave;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.content_list);
		mAdapter = new PhotoAdapter();
		setListAdapter(mAdapter);
		
		context = this;
		// if (getIntent() != null) {
		// Toast.makeText(this, "Intent action : " + getIntent().getAction(),
		// Toast.LENGTH_SHORT).show();
		// }

		if (savedInstanceState != null) {
			if (savedInstanceState.getString("albumFileUri") != null) {
				mAlbumFileUri = Uri.parse(savedInstanceState
						.getString("albumFileUri"));
				fillData(false);
				if (savedInstanceState.getBoolean("thumbCreatorInterrupted")) {
					startThumbnailsCreation(false);
				}
			}
		} else if (getIntent() != null
				&& Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			mAlbumFileUri = getIntent().getData();
			if (mAlbumFileUri.getScheme()
					.equals(ContentResolver.SCHEME_CONTENT)) {

				mProgress = ProgressDialog.show(this,
						getText(R.string.title_prog_retrieve_archive),
						getText(R.string.msg_prog_retrieve_archive), true,
						false);
				new Thread(new ArchiveRetriever()).start();

			} else {
				fillData(true);
			}
		}

		if (mAlbumFileUri == null) {
			
			//openAlbum();
			

		}

	}

	private void openAlbum() {
		Intent intent = new Intent("org.openintents.action.PICK_FILE");
		intent.setData(Uri.parse("file:///sdcard"));
		intent.putExtra("org.openintents.extra.TITLE",
				getText(R.string.select_file));
		intent.putExtra("org.openintents.extra.BUTTON_TEXT",
				getText(R.string.btn_select_file));
		try {
			startActivityForResult(intent, ACTIVITY_PICK_FILE);
		} catch (ActivityNotFoundException e) {
			alertOIFileManagerIsMissing();
		}
	}

	private void alertOIFileManagerIsMissing() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_filemanager_missing).setCancelable(
				false).setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						getOIFileManager();
					}
				}).setNegativeButton(android.R.string.no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected void getOIFileManager() {
		Intent i = new Intent(Intent.ACTION_VIEW, Uri
				.parse("market://search?q=pname:org.openintents.filemanager"));
		startActivity(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (mThmbCreator != null && mThmbCreator.isAlive()) {
			mThmbCreator.stopCreation();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isFinishing()) {
			// Delete cached album files
			for (File thisFile : getCacheDir().listFiles()) {
				thisFile.delete();
			}

			// Delete generated thumbnails
			for (String thisFileName : fileList()) {
				deleteFile(thisFileName);
			}
		}
	}

	private void startThumbnailsCreation(boolean clearThumbnails) {
		if (mContentModel != null) {
			ArrayList<String> pictureNames = new ArrayList<String>();
			Iterator<Map<String, String>> i = mContentModel.iterator();
			while (i.hasNext()) {
				Map<String, String> entry = (Map<String, String>) i.next();
				pictureNames.add(entry.get(KEY_FULLNAME));
			}
			mThmbCreator = new ThumbnailsCreator(this, mArchive, pictureNames,
					thumbnailsCreationHandler, clearThumbnails);
			mThmbCreator.start();
		}
	}

	private void fillData(boolean clearThumbnails) {
		if (mAlbumFileUri != null) {
			try {
				File selectedFile = new File(new URI(mAlbumFileUri.toString()
						.replace(" ", "%20")));
				mArchive = new ZipFile(selectedFile);
			} catch (Exception e) {
				Toast.makeText(this,
						R.string.open_archive_error + e.getLocalizedMessage(),
						Toast.LENGTH_SHORT).show();
			}

			if (mArchive == null) {
				Toast.makeText(this, R.string.open_archive_error,
						Toast.LENGTH_SHORT).show();
			} else {
				try {
					// Now create an array adapter and set it to display using
					// our row
					mContentModel = new ArrayList<Map<String, String>>();
					setTitle(mAlbumFileUri.getLastPathSegment());
					ZipEntry entry = null;
					Enumeration<? extends ZipEntry> entries = mArchive
							.entries();
					while (entries.hasMoreElements()) {
						entry = entries.nextElement();
						if (entry.getName().endsWith(".jpg")
								|| entry.getName().endsWith(".JPG")
								|| entry.getName().endsWith(".gif")
								|| entry.getName().endsWith(".GIF")
								|| entry.getName().endsWith(".png")
								|| entry.getName().endsWith(".PNG")) {
							Map<String, String> contentLine = new HashMap<String, String>();
							contentLine.put(KEY_FULLNAME, entry.getName());
							contentLine
									.put(
											KEY_SHORTNAME,
											entry
													.getName()
													.substring(
															entry
																	.getName()
																	.lastIndexOf(
																			'/') + 1));
							mContentModel.add(contentLine);
						}
					}
					registerForContextMenu(getListView());
					getListView().setSelection(
							getListView().getFirstVisiblePosition());
				} catch (Exception e) {
					Toast.makeText(this, e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}
		}
		if (mContentModel != null) {
			setProgressBarVisibility(true);
			startThumbnailsCreation(clearThumbnails);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean result = super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, LOAD_ALBUM_ID, 0, R.string.menu_load_album);
		item.setIcon(android.R.drawable.ic_menu_gallery);
		item = menu.add(0, SAVE_ALL_ID, 0, R.string.menu_save_all);
		item.setIcon(android.R.drawable.ic_menu_save);
		item = menu.add(0, ABOUT_ID, 0, R.string.menu_about);
		item.setIcon(android.R.drawable.ic_menu_help);
		return result;
	}
	
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(mContentModel == null) {
			menu.findItem(SAVE_ALL_ID).setEnabled(false);
		} else {
			menu.findItem(SAVE_ALL_ID).setEnabled(true);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case LOAD_ALBUM_ID:
			openAlbum();
			return true;
		case SAVE_ALL_ID:
			pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL);
			return true;
		case ABOUT_ID:
			Intent intent = new Intent(this, AboutDialog.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		posPictureToSave = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
		menu.setHeaderTitle(R.string.menu_save);
		menu.add(0, SAVE_SELECTED_ID, 0, R.string.menu_save_selected);
		menu.add(0, SAVE_ALL_ID, 0, R.string.menu_save_all);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_ALL_ID:
			pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL);
			return true;
		case SAVE_SELECTED_ID:
			pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void pickDirectory(int requestCode) {
		Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
		intent.setData(Uri.parse("file:///sdcard"));
		intent.putExtra("org.openintents.extra.TITLE",
				getText(R.string.select_directory));
		intent.putExtra("org.openintents.extra.BUTTON_TEXT",
				getText(R.string.btn_select_directory));
		try {
			startActivityForResult(intent, requestCode);
		} catch (ActivityNotFoundException e) {
			alertOIFileManagerIsMissing();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVITY_PICK_FILE:
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String filename = data.getDataString();
				if (filename != null) {
					mAlbumFileUri = Uri.parse(filename);
				}
				fillData(true);
			}
			break;
		case ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED:
		case ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL:
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String dirname = data.getDataString();
				try {
					if (requestCode == ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED) {
						savePicture(posPictureToSave,
								new File(Uri.parse(dirname).getEncodedPath()));
					} else {
						saveAllPictures(new File(Uri.parse(dirname)
								.getEncodedPath()));
					}
				} catch (IOException e) {
					Log.e(this.getClass().getName(),
							"onActivityResult() exception", e);
					Toast.makeText(this, e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}

			}
			break;
		}
	}

	private void saveAllPictures(final File file) throws IOException {
		setProgress(0);
		setProgressBarVisibility(true);
		new Thread() {

			/* (non-Javadoc)
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {
				for(int i = 0; i < mContentModel.size(); i++) {
					try {
						savePicture(i, file);
						saveAllHandler.sendEmptyMessage(i);
					} catch (FileNotFoundException e) {
						Log.e(this.getClass().getSimpleName(), "Error while saving pictures", e);
						Message msg = new Message();
						Bundle data = new Bundle();
						msg.arg1 = -1;
						data.putString("EXCEPTION", e.getMessage());
						msg.setData(data);
						saveAllHandler.sendMessage(msg);					} catch (IOException e) {
					}
				}
			}
			
			
		}.start();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mAlbumFileUri != null) {
			outState.putString("albumFileUri", mAlbumFileUri.toString());
		}
		if (mThmbCreator != null && mThmbCreator.isAlive()) {
			mThmbCreator.stopCreation();
			outState.putBoolean("thumbCreatorInterrupted", true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mThmbCreator == null
				|| (mThmbCreator != null && !mThmbCreator.isAlive())) {
			startThumbnailsCreation(false);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ShowPics.class);

		ArrayList<String> imageNames = new ArrayList<String>();
		for (Map<String, String> contentItem : mContentModel) {
			imageNames.add(contentItem.get(KEY_FULLNAME));
		}

		i.putExtra("PICS", imageNames);
		i.putExtra("ALBUM", mAlbumFileUri.toString());
		i.putExtra("POSITION", position);
		startActivity(i);
	}

	public class PhotoAdapter extends BaseAdapter {

		public int getCount() {
			if(mContentModel == null) return 0; 
			return mContentModel.size();
		}

		public Object getItem(int position) {
			return mContentModel.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			// Make an ImageView to show a thumbnail
			View result;
			// if(convertView == null ||
			// convertView.findViewById(R.id.thumbnail) == null) {
			result = getLayoutInflater().inflate(R.layout.content_row, null,
					false);
			// } else {
			// result = convertView;
			// }
			ImageView i = (ImageView) result.findViewById(R.id.thumbnail);
			String thumbName = mContentModel.get(position).get(KEY_THUMBNAIL);
			if (thumbName != null) {
				i.setImageDrawable(new BitmapDrawable(thumbName));
			}

			TextView t = (TextView) result.findViewById(R.id.image_name);
			t.setText(mContentModel.get(position).get(KEY_SHORTNAME));
			return result;
		}

		public void updateThumbnail(int position, String thumbName) {
			mContentModel.get(position).put(KEY_THUMBNAIL, thumbName);
			notifyDataSetChanged();
		}

	}

	private File savePicture(int position, File destDir)
			throws FileNotFoundException, IOException {
		File destFile;
		Map<String, String> imgModel = mContentModel.get(position);
		destFile = new File(destDir, imgModel.get(KEY_SHORTNAME).toLowerCase());

		OutputStream destFileOS = new FileOutputStream(destFile);
		InputStream imageIS = ZipUtil.getInputStream(mArchive, mArchive
				.getEntry(imgModel.get(KEY_FULLNAME)));
		byte[] buffer = new byte[2048];
		int len = 0;
		while ((len = imageIS.read(buffer)) >= 0) {
			destFileOS.write(buffer, 0, len);
		}
		destFileOS.close();
		imageIS.close();
		return destFile;
	}
}
