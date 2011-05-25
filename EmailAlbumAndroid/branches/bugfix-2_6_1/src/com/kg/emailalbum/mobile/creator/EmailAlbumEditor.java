/**
 * Copyright 2009, 2010 Kevin Gaudin
 *
 * This file is part of EmailAlbum.
 *
 * EmailAlbum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EmailAlbum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EmailAlbum.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kg.emailalbum.mobile.creator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kg.emailalbum.mobile.EmailAlbumPreferences;
import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.ui.QuickAction;
import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.BitmapUtil;
import com.kg.emailalbum.mobile.util.CacheManager;
import com.kg.emailalbum.mobile.util.Compatibility;
import com.kg.oifilemanager.filemanager.FileManagerProvider;
import com.kg.oifilemanager.intents.FileManagerIntents;

/**
 * Manages a set of pictures chosen by the user in order to export them as an
 * EmailAlbum jar. Allows to rearrange them, rotate them, add captions.
 * 
 * Uses Android TouchInterceptor extended ListView.
 * 
 * @author Kevin Gaudin
 * 
 */
public class EmailAlbumEditor extends ListActivity {

    private static final String STATE_SELECTED_URI = "SELECTED_URI";
    private static final String STATE_ROTATIONS = "ROTATIONS";
    private static final String STATE_CAPTIONS = "CAPTIONS";
    private static final String STATE_URIS = "URIS";

    /**
     * Different kinds of albums we can generate.
     */
    enum AlbumTypes {
        EMAILALBUM, ZIP, MAIL;

        public static AlbumTypes fromString(String string) {
            if ("emailalbum".equals(string)) {
                return EMAILALBUM;
            } else if ("zip".equals(string)) {
                return ZIP;
            } else if ("mail".equals(string)) {
                return MAIL;
            }
            return EMAILALBUM;
        }
    }

    /**
     * The different sizes which can be used when resizing pictures. 1024x768 is
     * not proposed to the user for the moment as we have memory issues when
     * loading pictures so large.
     */
    enum PictureSizes {
        S640X480(640, 480), S800X600(800, 600), S1024X768(1024, 768);

        int mWidth;
        int mHeight;

        PictureSizes(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public static PictureSizes fromString(String strValue) {
            int iX = strValue.indexOf('x');
            int width = Integer.parseInt(strValue.substring(0, iX));
            switch (width) {
            case 640:
                return S640X480;
            case 800:
                return S800X600;
            case 1024:
                return S1024X768;
            default:
                return S640X480;
            }
        }
    }

    /**
     * Adapter for handling AlbumItems.
     */
    public class AlbumAdapter extends BaseAdapter {
        /**
         * Keeps references to UI elements to avoid looking for them. This
         * should be attached to a row item with View.setTag().
         */
        private class ViewHolder {
            public ImageView thumb = null;
            public TextView txtCaption = null;
        }

        private final String LOG_TAG = AlbumAdapter.class.getSimpleName();

        /** The album being edited */
        protected List<AlbumItem> mContentModel = Collections.synchronizedList(new LinkedList<AlbumItem>());

        /**
         * Add an item to the list.
         * 
         * @param item
         *            The {@link AlbumItem} to add.
         */
        public void add(AlbumItem item) {
            mContentModel.add(item);
            notifyDataSetChanged();
        }

        /**
         * Add a collection of items to the list.
         * 
         * @param item
         *            The {@link Collection} containing all the
         *            {@link AlbumItem}s to add.
         */
        public void addAll(Collection<AlbumItem> items) {
            mContentModel.addAll(items);
            notifyDataSetChanged();
        }

        /**
         * Add a collection of items to the list from their Uris.
         * 
         * @param item
         *            The {@link Collection} containing all the {@link Uri}s to
         *            add.
         */
        public void addAllUris(Collection<Uri> items) {
            boolean excludedVideo = false;
            for (Uri uri : items) {
                if (!uri.toString().startsWith("content://media/external/video")) {
                    mContentModel.add(new AlbumItem(uri, null, ItemsLoader.getThumbnail(getApplicationContext(), uri)));
                } else {
                    if (!excludedVideo) {
                        excludedVideo = true;
                        Toast.makeText(getApplicationContext(), R.string.error_exclude_video, Toast.LENGTH_LONG).show();
                    }
                }
            }
            notifyDataSetChanged();
        }

        /**
         * Add an item to the list from it's Uri. This is a convenience method
         * to properly initialize an AlbumItem when all you have is an Uri.
         * 
         * @param item
         *            The {@link Uri} of the item to add.
         */
        public void addUri(Uri uri) {
            mContentModel.add(new AlbumItem(uri, null, ItemsLoader.getThumbnail(getApplicationContext(), uri)));
            notifyDataSetChanged();
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getCount()
         */
        @Override
        public int getCount() {
            if (mContentModel == null)
                return 0;
            return mContentModel.size();
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getItem(int)
         */
        @Override
        public Object getItem(int position) {
            return mContentModel.get(position);
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            if (convertView == null) {
                // No view to recycle, create a new one
                convertView = getLayoutInflater().inflate(R.layout.album_editor_line, null, false);

                // Initialize the ViewHolder with this view's references
                // to frequently used UI components
                holder = new ViewHolder();
                holder.thumb = (ImageView) convertView.findViewById(R.id.album_editor_thumb);
                holder.txtCaption = (TextView) convertView.findViewById(R.id.album_editor_image_caption);

                // Associate the ViewHolder to the row view.
                convertView.setTag(holder);
            } else {
                // We are recycling a previously created View,
                // let's just retrieve it's ViewHolder.
                holder = (ViewHolder) convertView.getTag();
            }

            AlbumItem albumItem = mContentModel.get(position);
            Bitmap thumb = null;
            try {
                thumb = albumItem.getThumb();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(LOG_TAG, "Error : ", e);
            }

            if (thumb != null) {
                // We have a thumbnail for this item, put it in the ImageView
                holder.thumb.setImageDrawable(new BitmapDrawable(thumb));
            } else {
                // No thumbnail available, just put a generic picture in the
                // ImageView
                holder.thumb.setImageResource(R.drawable.robot);
            }

            if (albumItem.caption != null && !"".equals(albumItem.caption)) {
                // The user has set a caption for this item, display it.
                holder.txtCaption.setText(albumItem.caption);
            } else {
                // No caption, tell the user that he can edit the item.
                holder.txtCaption.setText(getText(R.string.clk_to_edit));
            }

            return convertView;
        }

        /**
         * Change an item position in the list
         * 
         * @param from
         *            The position of the item to be moved.
         * @param to
         *            The desired position of the item after the move.
         */
        protected void moveItem(int from, int to) {
            AlbumItem item = mContentModel.remove(from);
            mContentModel.add(to, item);
            notifyDataSetChanged();
        }

        /**
         * Remove an item from the list.
         * 
         * @param which
         *            The position of the item to be removed.
         */
        protected void removeItem(int which) {
            mContentModel.remove(which);
        }

        /**
         * Rotate the thumbnail of an item.
         * 
         * @param selectedItem
         *            The position of the item to be rotated.
         * @param angle
         *            Rotation angle in degrees.
         */
        public void rotate(AlbumItem selectedItem, int angle) {
            selectedItem.rotate(angle);
            notifyDataSetChanged();
        }

        public void changeItemUri(AlbumItem selectedItem, Uri newUri) {
            if (newUri.toString().contains(".cache/temp")) {
                ErrorReporter.getInstance().handleSilentException(
                        new Exception("Asked to change an AlbumItem from " + selectedItem.uri.toString()
                                + " to a .cache/temp picture " + newUri.toString() + " !"));
            }
            selectedItem.uri = newUri;
            selectedItem.thumbUri = ItemsLoader.getThumbnail(getApplicationContext(), newUri);
            notifyDataSetChanged();
        }

        public ArrayList<Uri> getUris() {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            synchronized (mContentModel) {
                for (AlbumItem item : mContentModel) {
                    uris.add(item.uri);
                }
            }
            return uris;
        }

        public ArrayList<String> getCaptions() {
            ArrayList<String> captions = new ArrayList<String>();
            synchronized (mContentModel) {
                for (AlbumItem item : mContentModel) {
                    captions.add(item.caption);
                }
            }
            return captions;
        }

        public ArrayList<Integer> getRotations() {
            ArrayList<Integer> rotations = new ArrayList<Integer>();
            synchronized (mContentModel) {
                for (AlbumItem item : mContentModel) {
                    rotations.add(item.rotation);
                }
            }
            return rotations;
        }

        public AlbumItem getItem(Uri uri) {
            AlbumItem result = null;
            if (uri != null) {
                Iterator<AlbumItem> iterItems = mContentModel.iterator();
                while (iterItems.hasNext() && result == null) {
                    AlbumItem item = iterItems.next();
                    if (uri.equals(item.uri)) {
                        result = item;
                    }
                }
            }
            return result;
        }
    }

    /**
     * An album item. It is composed of an Uri (the source of the picture), a
     * thumbnail, a caption, and a rotation angle.
     * 
     * Captions should never alter the original pictures, we only display them
     * with UI components in the album viewer.
     * 
     * Rotation angle is applied only at the final album export.
     */
    class AlbumItem {
        String caption = "";
        int rotation = 0;
        Uri thumbUri = null;
        Uri uri = null;

        /**
         * Create a new item for this album.
         * 
         * @param uri
         *            The Uri of the source for this item.
         * @param caption
         *            The caption set by the user.
         * @param thumb
         *            The generated Thumbnail.
         */
        public AlbumItem(Uri uri, String caption, Uri thumbUri) {
            if (uri.toString().contains(".cache/temp")) {
                ErrorReporter.getInstance().handleSilentException(
                        new Exception("Asked to create a new AlbumItem from a .cache/temp picture" + uri.toString()
                                + " !"));
            }
            this.uri = uri;
            this.caption = caption != null ? caption : "";
            this.thumbUri = thumbUri;
        }

        public void rotate(int angle) {
            if (thumbUri != null) {
                Context ctx = EmailAlbumEditor.this.getApplicationContext();
                File storageDir = new CacheManager(ctx).getCacheDir("creator");
                File rotFile = new File(storageDir, "rot-" + thumbUri.getLastPathSegment());
                try {
                    Bitmap rotBmp = BitmapUtil.rotate(
                            BitmapLoader.load(ctx, thumbUri, null, null, Config.RGB_565, false), angle);
                    if (rotBmp != null) {
                        Log.d(LOG_TAG, "Saving rotated Thumbnail : " + angle);
                        rotBmp.compress(CompressFormat.JPEG, ItemsLoader.THUMBNAILS_QUALITY, new FileOutputStream(
                                rotFile));
                        rotation += angle;
                        thumbUri = FileManagerProvider.getContentUri(rotFile);
                    } else {
                        Toast.makeText(ctx, R.string.error_out_of_mem_rotate, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error : ", e);
                    Toast.makeText(ctx, R.string.error_out_of_mem_rotate, Toast.LENGTH_LONG).show();
                }
            } else {
                ErrorReporter.getInstance().putCustomData("AlbumItem.uri", uri.toString());
                ErrorReporter.getInstance().putCustomData("AlbumItem.caption", this.caption);
                ErrorReporter.getInstance().putCustomData("AlbumItem.rotation", "" + this.rotation);
                ErrorReporter.getInstance().putCustomData("AlbumItem.thumbUri", "null");
                ErrorReporter.getInstance().handleException(
                        new Exception("Rotating an AlbumItem without a thumbUri !!!"));
            }
        }

        public Bitmap getThumb() throws IOException {
            if (thumbUri != null) {
                return BitmapLoader.load(EmailAlbumEditor.this.getApplicationContext(), thumbUri, null, null);
            }
            return null;
        }
    }

    public class StateHolder {
        AlbumAdapter adapter = null;
        Bitmap previewPic = null;
        AlbumItem selectedItem = null;
        ExportAlbumTask exportTask = null;
    }

    // Activities returning results
    private static final int ACTIVITY_SELECT_PICTURES = 0;
    private static final int ACTIVITY_PICK_EXPORT_DIR = 1;
    private static final int ACTIVITY_EDIT_PICTURE = 2;

    // structure details for the generated album
    private static final String ALBUM_PICTURES_PATH = "com/kg/emailalbum/viewer/pictures/";
    public static final String ALBUM_CONTENT_FILE = ALBUM_PICTURES_PATH + "content";
    public static final String ZIP_CONTENT_FILE = "content.txt";

    // Dialogs
    private static final int DIALOG_EDIT_CAPTION = 0;
    static final int DIALOG_PROGRESS_EXPORT = 1;

    private static final int DIALOG_WAIT_EDIT_CAPTION = 2;

    private static final String LOG_TAG = EmailAlbumEditor.class.getSimpleName();

    // Menu items
    private static final int MENU_PREFS_ID = 0;
    public static final String EXTRA_EMAILALBUM_INTENT = "EMAILALBUM_INTENT";

    /** A reference to the list adapter */
    private AlbumAdapter mAdapter;

    private ExportAlbumTask mExportAlbumTask = null;

    /**
     * Listener which applies items moves when TouchInterceptor sends drop
     * events.
     */
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
            mAdapter.moveItem(from, to);
        }
    };

    /** A reference to the ListView */
    private ListView mList = null;

    /** A holder for asynchronous loading of the bitmap to be previewed */
    protected Bitmap mPreviewPic = null;

    /**
     * Listener which applies items removals when TouchInterceptor sends remove
     * events.
     */
    private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
        public void remove(int which) {
            mAdapter.removeItem(which);
        }
    };

    /** Current selected AlbumItem */
    private AlbumItem mSelectedItem = null;

    /** ProgressDialog for album exports, reused for each export */
    private ProgressDialog progressDialog;

    /**
     * Start the activity allowing the user to select pictures to be added to
     * the album.
     */
    protected void addImages() {
        Intent i = new Intent(getApplicationContext(), SelectPictures.class);
        // TODO
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        // i.setType("image/*");
        startActivityForResult(i, ACTIVITY_SELECT_PICTURES);
    }

    /**
     * Starts album export process. Actually only start the pick directory
     * activity, see {@link #onActivityResult(int, int, Intent)} for album
     * generation start.
     */
    protected void exportAlbum() {
        pickDirectory(ACTIVITY_PICK_EXPORT_DIR);
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
        case ACTIVITY_SELECT_PICTURES:
            // User should have selected a few pictures
            if (resultCode == RESULT_OK && data != null) {
                Parcelable[] resultArray = data.getParcelableArrayExtra(SelectPictures.RESULT_URIS);
                if (resultArray != null && resultArray.length != 0) {
                    List<Uri> uris = new ArrayList<Uri>();
                    for (Parcelable parcelable : resultArray) {
                        // The SelectPictures activity returns a list of Uris.
                        uris.add((Uri) parcelable);
                    }
                    // Add all these uris to the album
                    mAdapter.addAllUris(uris);
                }
            }
            break;

        case ACTIVITY_PICK_EXPORT_DIR:
            // Returning from directory selection for album export
            if (resultCode == RESULT_OK && data != null) {
                // obtain the dir name
                String dirname = data.getDataString();
                // Start the asynchronous export
                mExportAlbumTask = new ExportAlbumTask(EmailAlbumEditor.this, ExportAlbumTask.EXPORT);
                mExportAlbumTask.execute(new File(Uri.parse(dirname).getEncodedPath()));

            }
            break;

        case ACTIVITY_EDIT_PICTURE:
            if (resultCode == Activity.RESULT_OK) {
                dismissDialog(DIALOG_EDIT_CAPTION);
                Uri editedPictureUri = data.getData();
                if (editedPictureUri.getScheme().startsWith(ContentResolver.SCHEME_FILE)) {
                    // Workaround to make Photoshop.com android app able to
                    // re-edit its results
                    File imageFile = new File(editedPictureUri.getPath());
                    Uri editedPictureContentUri = BitmapUtil.getContentUriFromFile(getApplicationContext(), imageFile);
                    if (editedPictureContentUri != null) {
                        editedPictureUri = editedPictureContentUri;
                    }
                }
                mAdapter.changeItemUri(mSelectedItem, editedPictureUri);
            }
        default:
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_editor);

        // If an orientation change occurred, retrieve previous state
        StateHolder state = (StateHolder) getLastNonConfigurationInstance();
        if (state != null) {
            mAdapter = state.adapter;
            mSelectedItem = state.selectedItem;
            mPreviewPic = state.previewPic;
            mExportAlbumTask = state.exportTask;
        } else {
            mAdapter = new AlbumAdapter();
        }

        // Sets background dithering for older versions of android (1.5 & 1.6)
        findViewById(R.id.album_editor_root).getBackground().setDither(true);

        // If a previous instance state is available, retrieve it
        if (mAdapter.getCount() == 0 && (savedInstanceState != null && savedInstanceState.containsKey(STATE_URIS))) {
            ArrayList<String> captions = savedInstanceState.containsKey(STATE_CAPTIONS) ? savedInstanceState
                    .getStringArrayList(STATE_CAPTIONS) : null;
            ArrayList<Integer> rotations = savedInstanceState.containsKey(STATE_ROTATIONS) ? savedInstanceState
                    .getIntegerArrayList(STATE_ROTATIONS) : null;
            AlbumItem item = null;
            int i = 0;
            for (Parcelable pUri : savedInstanceState.getParcelableArrayList(STATE_URIS)) {
                item = new AlbumItem((Uri) pUri, captions != null ? captions.get(i) : "", null);
                item.rotation = rotations != null ? rotations.get(i) : 0;
                mAdapter.add(item);
                i++;
            }
            if (savedInstanceState.containsKey(STATE_SELECTED_URI)) {
                mSelectedItem = mAdapter.getItem((Uri) savedInstanceState.getParcelable(STATE_SELECTED_URI));
            }
        }

        setListAdapter(mAdapter);

        // If the activity has been recreated after an orientation change during
        // the album export process, we have to relink the running task with the
        // new instance of the activity AFTER the adapter has been linked to the
        // ListView.
        if (mExportAlbumTask != null) {
            mExportAlbumTask.setEditor(EmailAlbumEditor.this);
        }

        retrieveIntentData();

        // When the user clicks on an item, open the item edit dialog
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showEditDialog(position);
            }

        });

        // Buttons initialisation (background dithering + click listeners)
        Button btn = (Button) findViewById(R.id.btn_pick_pictures);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addImages();
            }

        });
        btn = (Button) findViewById(R.id.btn_album_export);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportAlbum();
            }
        });
        btn = (Button) findViewById(R.id.btn_album_share);
        btn.getBackground().setDither(true);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAlbum();
            }
        });

        mList = getListView();
        // Our list is a TouchInterceptor, we have to set listeners to
        // be notified whent the user alters the content of the list
        ((TouchInterceptor) mList).setDropListener(mDropListener);
        ((TouchInterceptor) mList).setRemoveListener(mRemoveListener);

        // List with transparent background
        mList.setCacheColorHint(0);
    }

    /**
     * Fill the adapter with uris received from other apps
     */
    private void retrieveIntentData() {
        Intent i = getIntent();
        String actionSendMultiple = Compatibility.getActionSendMultiple();
        if (i.getAction() != null) {
            if (i.getAction().equals(Intent.ACTION_SEND)) {
                Uri toSend = (Uri) i.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.d(LOG_TAG, "Uri to send : " + toSend);
                mAdapter.addUri(toSend);

            } else if (actionSendMultiple != null && i.getAction().equals(actionSendMultiple)) {
                if (i.getBooleanExtra(EXTRA_EMAILALBUM_INTENT, false)) {
                    Toast.makeText(getApplicationContext(), R.string.error_select_activity, Toast.LENGTH_LONG).show();
                    finish();
                }
                ArrayList<Uri> toSend = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                Log.d(LOG_TAG, "Uri to send : " + toSend);
                mAdapter.addAllUris(toSend);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_EDIT_CAPTION:
            // Creation of the item edit dialog

            // Inflate the dialog content layout
            ViewGroup editCaption = (ViewGroup) getLayoutInflater().inflate(R.layout.dialog_edit_caption, null);
            // Build the dialog
            dialog = new AlertDialog.Builder(this).setView(editCaption).create();

            // Add a dismiss listener to perform actions related to user choices
            // in the dialog.
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dlg) {
                    // Retrieve the caption and store it
                    EditText txtField = (EditText) ((Dialog) dlg).findViewById(R.id.dialog_textfield);
                    if (mSelectedItem != null && txtField != null && txtField.getText() != null) {
                        mSelectedItem.caption = txtField.getText().toString().trim();
                        mAdapter.notifyDataSetChanged();
                    }
                }

            });
            return dialog;
        case DIALOG_PROGRESS_EXPORT:
            // Create the progress dialog used when exporting the album
            setProgressDialog(new ProgressDialog(this));
            getProgressDialog().setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            getProgressDialog().setMessage(getText(R.string.exporting));
            getProgressDialog().setCancelable(true);
            return getProgressDialog();
        case DIALOG_WAIT_EDIT_CAPTION:
            // Another progress dialog used when loading the picture for the
            // edit item dialog.
            dialog = new ProgressDialog(this);
            ((ProgressDialog) dialog).setProgressStyle(ProgressDialog.STYLE_SPINNER);
            ((ProgressDialog) dialog).setMessage(getText(R.string.preparing_preview));
            ((ProgressDialog) dialog).setCancelable(false);
            return dialog;
        default:
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, MENU_PREFS_ID, 0, R.string.menu_prefs);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_PREFS_ID:
            startPreferencesActivity();
            return true;
        default:
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
        case DIALOG_EDIT_CAPTION:
            // Prepare a dialog for editing caption an rotating picture
            if (mPreviewPic != null && mSelectedItem != null) {
                // Prepare the image. Click on the image to close the dialog.
                ImageView imgPrv = (ImageView) dialog.findViewById(R.id.image_preview);
                imgPrv.setImageBitmap(mPreviewPic);
                imgPrv.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        dismissDialog(DIALOG_EDIT_CAPTION);
                    }

                });

                // Fill the caption field with previously entered caption
                EditText captionEditor = (EditText) dialog.findViewById(R.id.dialog_textfield);
                captionEditor.setText(mSelectedItem.caption);

                // Initialize picture rotation buttons
                ImageButton btn = (ImageButton) dialog.findViewById(R.id.btn_rotate_cw);
                // First, give this button a reference to the picture preview
                // ImageView
                btn.setTag(imgPrv);
                btn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Tell the adapter that the user wants this picture to
                        // be rotated in the final album
                        mAdapter.rotate(mSelectedItem, 90);

                        // Retrieve the reference to the preview ImageView
                        ImageView imgV = (ImageView) (v.getTag());
                        BitmapDrawable bmpDrw = (BitmapDrawable) imgV.getDrawable();
                        // Apply the rotation to the preview bitmap
                        imgV.setImageBitmap(BitmapUtil.rotate(bmpDrw.getBitmap(), 90));
                    }

                });

                btn = (ImageButton) dialog.findViewById(R.id.btn_rotate_ccw);
                // First, give this button a reference to the picture preview
                // ImageView
                btn.setTag(imgPrv);
                btn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Tell the adapter that the user wants this picture to
                        // be rotated in the final album
                        mAdapter.rotate(mSelectedItem, -90);

                        // Retrieve the reference to the preview ImageView
                        ImageView imgV = (ImageView) (v.getTag());
                        BitmapDrawable bmpDrw = (BitmapDrawable) imgV.getDrawable();
                        // Apply the rotation to the preview bitmap
                        imgV.setImageBitmap(BitmapUtil.rotate(bmpDrw.getBitmap(), -90));
                    }

                });

                // Initialize picture edit button
                btn = (ImageButton) dialog.findViewById(R.id.btn_edit);
                final Intent intent = new Intent(Intent.ACTION_EDIT);
                Log.d(LOG_TAG, "Is " + mSelectedItem.uri + " editable ?");
                intent.setDataAndType(mSelectedItem.uri, "image/jpeg");
                if (getPackageManager().resolveActivity(intent, 0) != null) {
                    btn.setVisibility(View.VISIBLE);
                    btn.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            startActivityForResult(intent, ACTIVITY_EDIT_PICTURE);
                        }

                    });
                } else {
                    btn.setVisibility(View.INVISIBLE);
                }
            } else {
                dismissDialog(DIALOG_EDIT_CAPTION);
            }

            break;
        case DIALOG_WAIT_EDIT_CAPTION:
            break;

        default:
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        // On orientation change, keep a reference to our adapter.
        StateHolder state = new StateHolder();
        state.adapter = mAdapter;
        state.selectedItem = mSelectedItem;
        state.previewPic = mPreviewPic;
        state.exportTask = mExportAlbumTask;
        if(mExportAlbumTask != null) {
            mExportAlbumTask.setEditor(null);
        }
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(STATE_URIS, mAdapter.getUris());
        outState.putStringArrayList(STATE_CAPTIONS, mAdapter.getCaptions());
        outState.putIntegerArrayList(STATE_ROTATIONS, mAdapter.getRotations());
        if (mSelectedItem != null) {
            outState.putParcelable(STATE_SELECTED_URI, mSelectedItem.uri);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Starts the pick directory activity.
     * 
     * @param requestCode
     *            The code which will be returned in
     *            {@link #onActivityResult(int, int, Intent)} when the pick
     *            directory activity ends.
     */
    private void pickDirectory(int requestCode) {
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);

        intent.setData(Uri.fromFile(android.os.Environment.getExternalStorageDirectory()));
        intent.putExtra(FileManagerIntents.EXTRA_TITLE, getText(R.string.select_directory));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getText(R.string.btn_select_directory));
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "Error before picking directory", e);
        }
    }

    /**
     * Starts the process of sending an album.
     */
    private void sendAlbum() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(EmailAlbumEditor.this);
        AlbumTypes albumType = AlbumTypes.fromString(prefs.getString("albumtype",
                getString(R.string.pref_def_albumtype)));
        if (AlbumTypes.MAIL.equals(albumType) && !Compatibility.isSendMultipleAppAvailable(getApplicationContext())) {
            albumType = AlbumTypes.fromString(getString(R.string.pref_def_albumtype));
        }
        if (albumType.equals(AlbumTypes.MAIL)) {
            CacheManager cm = new CacheManager(getApplicationContext());
            File dir = cm.getCacheDir("temp");
            cm.clearCache("temp");

            mExportAlbumTask = new ExportAlbumTask(EmailAlbumEditor.this, ExportAlbumTask.SEND_MULTIPLE);
            mExportAlbumTask.execute(dir);

        } else {
            File dir = new CacheManager(getApplicationContext()).getCacheDir();
            mExportAlbumTask = new ExportAlbumTask(EmailAlbumEditor.this, ExportAlbumTask.SEND);
            mExportAlbumTask.execute(dir);
        }
    }

    /**
     * Called after the asynchronous generation of the album ends and starts the
     * choice for an activity able to send the generated album file.
     * 
     * @param album
     *            The Uri of the album to be sent.
     */
    void sendAlbum(Uri album) {
        if (album.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            album = Uri.withAppendedPath(FileManagerProvider.CONTENT_URI, album.getEncodedPath());
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM, album);

        QuickAction qa = QuickAction.buildChooser(findViewById(R.id.btn_album_share), intent, true);
        if (qa != null) {
            qa.show();
        } else {
            Toast.makeText(this, R.string.error_no_send_activity, Toast.LENGTH_LONG).show();
        }

        // intent.putExtra(Intent.EXTRA_SUBJECT, "album.jar");
        // startActivity(Intent.createChooser(intent,
        // getText(R.string.send_album_with)));
    }

    /**
     * Displays a dialog for editing album item properties (caption + rotation).
     * 
     * @param position
     *            The position of the item in the list.
     */
    private void showEditDialog(int position) {
        // Display a indeterminate progress dialog while loading a preview of
        // the picture.
        // This has been made asynchronous because big pictures could take a few
        // seconds to load.
        showDialog(DIALOG_WAIT_EDIT_CAPTION);
        mSelectedItem = (AlbumItem) mAdapter.getItem(position);
        AsyncTask<AlbumItem, Integer, Bitmap> previewLoader = new AsyncTask<AlbumItem, Integer, Bitmap>() {

            @Override
            protected Bitmap doInBackground(AlbumItem... albumItems) {
                try {
                    // Load the picture preview and rotate it to the user
                    // specified angle.
                    return BitmapUtil.rotate(BitmapLoader.load(getApplicationContext(), albumItems[0].uri, null, null),
                            albumItems[0].rotation);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error : ", e);
                }
                return null;
            }

            /*
             * (non-Javadoc)
             * 
             * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
             */
            @Override
            protected void onPostExecute(Bitmap result) {
                // Place the result Bitmap in this class field in order
                // to transmit it to the not yet existing dialog.
                mPreviewPic = result;
                // Just remove the dialog. Don't dismiss it because it
                // would prevent the progress dialog to be reused later.
                removeDialog(DIALOG_WAIT_EDIT_CAPTION);
                // Show the item edition dialog
                showDialog(DIALOG_EDIT_CAPTION);
            }

        };
        previewLoader.execute(mSelectedItem);
    }

    /**
     * Start the settings activity.
     */
    private void startPreferencesActivity() {
        Intent i = new Intent(getApplicationContext(), EmailAlbumPreferences.class);
        i.putExtra(EmailAlbumPreferences.EXTRA_SCREEN, EmailAlbumPreferences.SCREEN_CREATOR);
        startActivity(i);
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

}
