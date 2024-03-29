package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

public class ShowPics extends Activity implements OnGestureListener {
	private ImageView mImgView;
	private ImageView mPrevImgView;
	private ImageView mNextImgView;
	private ViewGroup mContainer;
	private ArrayList<String> mImageNames;
	private String mAlbumName;
	private int mPosition;
	private int mOldPosition;
	private GestureDetector mGestureScanner;
	private ZipFile archive;
	private final static int SET_AS_ID = 1;
	private static final int SAVE_ID = 2;
	private static final int SEND_ID = 3;
	private static final int ABOUT_ID = 4;

	private static final int BACKWARD = -1;
	private static final int FORWARD = 1;

	private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE = 0;
	private static final int TRANSITION_LENGTH = 500;

	private static Uri sStorageURI = Images.Media.EXTERNAL_CONTENT_URI;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.content_view);

		mImgView = (ImageView) findViewById(R.id.showpic);
		mPrevImgView = (ImageView) findViewById(R.id.prevpic);
		mNextImgView = (ImageView) findViewById(R.id.nextpic);
		mContainer = (ViewGroup) findViewById(R.id.container);
		mContainer.setPersistentDrawingCache(ViewGroup.PERSISTENT_NO_CACHE);
		mGestureScanner = new GestureDetector(this);

		mOldPosition = -1;

		if (getIntent() != null) {
			mImageNames = getIntent().getStringArrayListExtra("PICS");
			mAlbumName = getIntent().getStringExtra("ALBUM");
			mPosition = getIntent().getIntExtra("POSITION", 0);
		}

		if (savedInstanceState != null) {
			mImageNames = savedInstanceState.getStringArrayList("PICS") != null ? savedInstanceState
					.getStringArrayList("PICS")
					: mImageNames;
			mAlbumName = savedInstanceState.getString("ALBUM") != null ? savedInstanceState
					.getString("ALBUM")
					: mAlbumName;
			mPosition = savedInstanceState.getInt("POSITION");
		}

		try {
			File albumFile = new File(new URI(mAlbumName.toString().replace(
					" ", "%20")));
			archive = new ZipFile(albumFile);
			showPicture();
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void resetViews() {
		try {
			InputStream imageIS;
			if (mOldPosition < mPosition) {
				// Gone forward
				mPrevImgView.setImageDrawable(mImgView.getDrawable());
				mImgView.setImageDrawable(mNextImgView.getDrawable());
				mImgView.setVisibility(View.VISIBLE);
				mNextImgView.setVisibility(View.GONE);
				mImgView.requestFocus();
				if (mPosition + 1 < mImageNames.size()) {
					imageIS = archive.getInputStream(archive
							.getEntry(mImageNames.get(mPosition + 1)));
					mNextImgView.setImageBitmap(BitmapFactory
							.decodeStream(imageIS));
					imageIS.close();
				}
			} else if (mOldPosition > mPosition) {
				// Gone backward
				mNextImgView.setImageDrawable(mImgView.getDrawable());
				mImgView.setImageDrawable(mPrevImgView.getDrawable());
				mImgView.setVisibility(View.VISIBLE);
				mPrevImgView.setVisibility(View.GONE);
				mImgView.requestFocus();
				if (mPosition - 1 >= 0) {
					imageIS = archive.getInputStream(archive
							.getEntry(mImageNames.get(mPosition - 1)));
					mPrevImgView.setImageBitmap(BitmapFactory
							.decodeStream(imageIS));
					imageIS.close();
				}
			}
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void showPicture() {
		try {
			InputStream imageIS;
			if (mOldPosition == -1) {
				// First load, initialize from archive
				imageIS = archive.getInputStream(archive.getEntry(mImageNames
						.get(mPosition)));
				mImgView.setImageBitmap(BitmapFactory.decodeStream(imageIS));
				imageIS.close();
				if (mPosition > 0) {
					imageIS = archive.getInputStream(archive
							.getEntry(mImageNames.get(mPosition - 1)));
					mPrevImgView.setImageBitmap(BitmapFactory
							.decodeStream(imageIS));
					imageIS.close();
				}
				if (mPosition < mImageNames.size() - 1) {
					imageIS = archive.getInputStream(archive
							.getEntry(mImageNames.get(mPosition + 1)));
					mNextImgView.setImageBitmap(BitmapFactory
							.decodeStream(imageIS));
					imageIS.close();
				}
			} else if (mOldPosition < mPosition) {
				// Going forward
				applyRotation(FORWARD, 0, -90);
			} else if (mOldPosition > mPosition) {
				// Going backward
				applyRotation(BACKWARD, 0, 90);
			}
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void goBack() {
		mOldPosition = mPosition;
		mPosition = Math.max(0, mPosition - 1);
		if (mOldPosition != mPosition) {
			showPicture();
		} else {
			toastFirstOrLast();
		}
	}

	private void goForward() {
		mOldPosition = mPosition;
		mPosition = Math.min(mImageNames.size() - 1, mPosition + 1);
		if (mOldPosition != mPosition) {
			showPicture();
		} else {
			toastFirstOrLast();
		}
	}

	private void toastFirstOrLast() {
		if (mPosition == 0) {
			Toast.makeText(this, R.string.first_pic, Toast.LENGTH_SHORT).show();
		} else if (mPosition == mImageNames.size() - 1) {
			Toast.makeText(this, R.string.last_pic, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return mGestureScanner.onTouchEvent(me);
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (velocityX < 0) {
			goForward();
			return true;
		} else if (velocityX > 0) {
			goBack();
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		openOptionsMenu();
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		openOptionsMenu();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_UP:
			goBack();
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
			goForward();
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			openOptionsMenu();
		}
		return super.onKeyDown(keyCode, event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("POSITION", mPosition);
		outState.putStringArrayList("PICS", mImageNames);
		outState.putString("ALBUM", mAlbumName);
		super.onSaveInstanceState(outState);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean result = super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, SET_AS_ID, 0, R.string.menu_set_as);
		item.setIcon(android.R.drawable.ic_menu_set_as);
		item = menu.add(0, SEND_ID, 0, R.string.menu_share);
		item.setIcon(android.R.drawable.ic_menu_share);
		item = menu.add(0, SAVE_ID, 0, R.string.menu_save);
		item.setIcon(android.R.drawable.ic_menu_save);
		item = menu.add(0, ABOUT_ID, 0, R.string.menu_about);
		item.setIcon(android.R.drawable.ic_menu_help);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		Intent intent = null;
		switch (item.getItemId()) {
		case SAVE_ID:
			intent = new Intent("org.openintents.action.PICK_DIRECTORY");
			intent.setData(Uri.parse("file:///sdcard"));
			intent.putExtra("org.openintents.extra.TITLE",
					getText(R.string.select_directory));
			intent.putExtra("org.openintents.extra.BUTTON_TEXT",
					getText(R.string.btn_select_directory));

			startActivityForResult(intent, ACTIVITY_PICK_DIRECTORY_TO_SAVE);
			return true;
		case SEND_ID:
		case SET_AS_ID:
			try {
				Uri fileUri = storePicture(saveTmpPicture(new File(
						EmailAlbumViewer.TMP_DIR_NAME)));
				if (item.getItemId() == SEND_ID) {
					intent = new Intent(Intent.ACTION_SEND, fileUri);
					intent.putExtra(Intent.EXTRA_STREAM, fileUri);
					intent.setType("image/jpeg");
				} else if (item.getItemId() == SET_AS_ID) {
					intent = new Intent(Intent.ACTION_ATTACH_DATA, fileUri);

				}
				startActivity(Intent.createChooser(intent,
						getText(R.string.chooser_share)));

			} catch (IOException e) {
				Toast.makeText(
						this,
						getText(R.string.error_other_intents) + " : "
								+ e.getLocalizedMessage(), Toast.LENGTH_SHORT)
						.show();
			}

			return true;

		case ABOUT_ID:
			intent = new Intent(this, AboutDialog.class);
			startActivity(intent);
			return true;

		}
		return super.onOptionsItemSelected(item);
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
		case ACTIVITY_PICK_DIRECTORY_TO_SAVE:
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String dirname = data.getDataString();
				try {
					savePicture(new File(Uri.parse(dirname).getEncodedPath()));
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(this, e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}

			}
			break;
		}
	}

	private File saveTmpPicture(File destDir) throws IOException {
		File destFile = null;
		if (destDir != null) {
			if (!destDir.exists()) {
				try {
					destDir.mkdirs();
				} catch (Exception e) {
					destDir = null;
				}
			} else if (!destDir.isDirectory()) {
				destDir = null;
			}
		}

		if (destDir == null) {
			destDir = getCacheDir();
		}

		try {
			destFile = savePicture(destDir);
		} catch (IOException e) {
			if (!destDir.equals(getCacheDir())) {
				destFile = savePicture(getCacheDir());
			}
		}
		return destFile;
	}

	private File savePicture(File destDir) throws FileNotFoundException,
			IOException {
		File destFile;
		destFile = new File(destDir, mImageNames.get(mPosition).substring(
				mImageNames.get(mPosition).lastIndexOf('/')).toLowerCase());

		OutputStream destFileOS = new FileOutputStream(destFile);
		InputStream imageIS = archive.getInputStream(archive
				.getEntry(mImageNames.get(mPosition)));
		byte[] buffer = new byte[200];
		int len = 0;
		while ((len = imageIS.read(buffer)) >= 0) {
			destFileOS.write(buffer, 0, len);
		}
		destFileOS.close();
		return destFile;
	}

	private Uri storePicture(File imageFile) {
		ContentResolver cr = getContentResolver();
		String imageName = mImageNames.get(mPosition);
		imageName = imageName.substring(imageName.lastIndexOf('/') + 1);
		ContentValues values = new ContentValues(7);
		values.put(Images.Media.TITLE, imageName);
		values.put(Images.Media.DISPLAY_NAME, imageName);
		values.put(Images.Media.DESCRIPTION, "");
		values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.ORIENTATION, 0);
		File parentFile = imageFile.getParentFile();
		String path = parentFile.toString().toLowerCase();
		String name = parentFile.getName().toLowerCase();
		values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
		values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
		values.put("_data", imageFile.toString());

		Uri uri = cr.insert(sStorageURI, values);

		return uri;
	}

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param direction
	 *            BACKWARD / FORWARD
	 * @param start
	 *            the start angle at which the rotation must begin
	 * @param end
	 *            the end angle of the rotation
	 */
	private void applyRotation(int direction, float start, float end) {
		// Find the center of the container
		final float centerX = mContainer.getWidth() / 2.0f;
		final float centerY = mContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end,
				centerX, centerY, 310.0f, true);
		rotation.setDuration(TRANSITION_LENGTH / 2);
		rotation.setFillAfter(false);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(direction));

		mContainer.startAnimation(rotation);
	}

	/**
	 * This class listens for the end of the first half of the animation. It
	 * then posts a new action that effectively swaps the views when the
	 * container is rotated 90 degrees and thus invisible.
	 */
	private final class DisplayNextView implements Animation.AnimationListener {
		private final int mDirection;

		private DisplayNextView(int direction) {
			mDirection = direction;
		}

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			mContainer.post(new SwapViews(mDirection));
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	/**
	 * This class is responsible for swapping the views and start the second
	 * half of the animation.
	 */
	private final class SwapViews implements Runnable {
		private final int mDirection;
		

		public SwapViews(int position) {
			mDirection = position;
		}

		public void run() {
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (mDirection == FORWARD) {
				mImgView.setVisibility(View.GONE);
				mNextImgView.setVisibility(View.VISIBLE);
				mNextImgView.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY,
						310.0f, false);
			} else {
				mImgView.setVisibility(View.GONE);
				mPrevImgView.setVisibility(View.VISIBLE);
				mPrevImgView.requestFocus();

				rotation = new Rotate3dAnimation(-90, 0, centerX, centerY,
						310.0f, false);
			}

			rotation.setDuration(TRANSITION_LENGTH / 2);
			rotation.setFillAfter(false);
			rotation.setInterpolator(new DecelerateInterpolator());
			rotation.setAnimationListener(new Animation.AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {
					resetViews();
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

			});

			mContainer.startAnimation(rotation);
		}
	}

}
