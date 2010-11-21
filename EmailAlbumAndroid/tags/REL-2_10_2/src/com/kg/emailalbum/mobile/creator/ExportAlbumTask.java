package com.kg.emailalbum.mobile.creator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.acra.ErrorReporter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.creator.EmailAlbumEditor.AlbumAdapter;
import com.kg.emailalbum.mobile.creator.EmailAlbumEditor.AlbumItem;
import com.kg.emailalbum.mobile.creator.EmailAlbumEditor.AlbumTypes;
import com.kg.emailalbum.mobile.creator.EmailAlbumEditor.PictureSizes;
import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.BitmapUtil;
import com.kg.emailalbum.mobile.util.Compatibility;
import com.kg.emailalbum.mobile.util.HumanReadableProperties;
import com.kg.emailalbum.mobile.util.IntentHelper;
import com.kg.emailalbum.mobile.util.Toaster;

/**
 * Asynchronous task for exporting current album to an EmailAlbum jar file.
 */
public class ExportAlbumTask extends AsyncTask<File, Integer, Uri> {
    private EmailAlbumEditor mEditor;

    // These constants are used to tell the task that it has to send
    // the result album or only store it.
    /**
     * Export to internal storage.
     */
    public static final int EXPORT = 0;
    /**
     * Send through ACTION_SEND Intent.
     */
    public static final int SEND = 1;
    /**
     * Send through ACTION_SEND_MULTIPLE Intent.
     */
    public static final int SEND_MULTIPLE = 2;

    int mReason;
    private HumanReadableProperties mContentFileBuilder;
    private List<AlbumItem> mContentModel;
    
    
    private static final String ALBUM_PICTURES_PATH = "com/kg/emailalbum/viewer/pictures/";
    public static final String ALBUM_CONTENT_FILE = ALBUM_PICTURES_PATH + "content";
    public static final String ZIP_CONTENT_FILE = "content.txt";
    private static final int DEFAULT_JPG_QUALITY = 70;

    // Preferences

    private static final String LOG_TAG = ExportAlbumTask.class.getSimpleName();
    private String mAlbumName = null;
    private boolean mAddTimestamp = true;
    private int mPictureQuality = DEFAULT_JPG_QUALITY;
    private PictureSizes mPictureSize = null;
    private AlbumTypes mAlbumType = AlbumTypes.EMAILALBUM;

    private Context mContext;

    /**
     * Create a new export task.
     * 
     * @param reason
     *            The reason for this export : {@link #EXPORT} or {@link #SEND}
     */
    public ExportAlbumTask(EmailAlbumEditor editor, int reason) {
        setEditor(editor);
        mReason = reason;
        mContentFileBuilder = new HumanReadableProperties();
        initPrefs();
    }

    public void setEditor(EmailAlbumEditor editor) {
        mEditor = editor;
        if(mEditor != null) {
            mContentModel = ((AlbumAdapter)mEditor.getListAdapter()).mContentModel;
            mContext = mEditor.getApplicationContext();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {
        // Before exporting, show the progress dialog
        mEditor.showDialog(EmailAlbumEditor.DIALOG_PROGRESS_EXPORT);
        super.onPreExecute();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Uri doInBackground(File... dests) {
        // Reset progress dialog in case of reuse.
        publishProgress(0);

        ErrorReporter.getInstance().addCustomData("nbPics", "" + mContentModel.size());
        ErrorReporter.getInstance().addCustomData("ExportSize", mPictureSize.name());

        File album;
        if (mAlbumType.equals(AlbumTypes.MAIL)) {
            ErrorReporter.getInstance().addCustomData("Format", "Mail Attachments");
            album = dests[0];
            // Total progress count.
            int count = mContentModel.size();
            int itemNumber = 0;
            synchronized (mContentModel) {
                for (AlbumItem item : mContentModel) {
                    ErrorReporter.getInstance().addCustomData("CurrentPic", "" + itemNumber);
                    try {
                        // Create the file name. All pictures have to be
                        // named so that their alphabetical order is the
                        // order set by the user for the album.
                        // This is due to the use of Properties.load() in
                        // the jar bundled Viewer.
                        String picName = new Formatter().format("img%04d.jpg", itemNumber,
                                item.uri.getLastPathSegment()).toString();

                        // Load and resize a full color picture
                        Bitmap bmp = BitmapLoader.load(mContext, item.uri, mPictureSize.getWidth(),
                                mPictureSize.getHeight(), Bitmap.Config.ARGB_8888, false);
                        if (bmp != null) {
                            ErrorReporter.getInstance().addCustomData("Apply rotation ?",
                                    "" + (item.rotation % 360 != 0));
                            if (item.rotation % 360 != 0) {
                                // Apply the user specified rotation
                                bmp = BitmapUtil.rotate(bmp, item.rotation);
                            }

                            OutputStream out = new FileOutputStream(new File(album, picName));

                            // Write the picture to the temp dir
                            ErrorReporter.getInstance().addCustomData("bmp is null ?", "" + (bmp == null));
                            bmp.compress(CompressFormat.JPEG, mPictureQuality, out);

                            mContentFileBuilder.put(picName, item.caption);

                            // Get rid of the bitmap to avoid memory leaks
                            bmp.recycle();
                            out.close();
                        } else {
                            ErrorReporter.getInstance().addCustomData("item.uri", item.uri.toString());
                            ErrorReporter.getInstance().addCustomData("mPictureSize.getWidth()",
                                    "" + mPictureSize.getWidth());
                            ErrorReporter.getInstance().addCustomData("mPictureSize.getHeight()",
                                    "" + mPictureSize.getHeight());
                            ErrorReporter
                                    .getInstance()
                                    .handleException(
                                            new Exception(
                                                    "Could not load image while creating archive! (BitmapLoader result is null)"));
                            new Toaster(mContext, R.string.album_creation_image_error, Toast.LENGTH_LONG)
                                    .start();
                        }
                        itemNumber++;
                        publishProgress((int) (((float) itemNumber / (float) count) * 100));
                        System.gc();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error while creating temp pics", e);
                    }
                }
            }
        } else { // Zip or Jar
            // Generate a filename based on current date and time
            CharSequence timestamp = "";
            if (mAddTimestamp) {
                timestamp = DateFormat.format("_yyyyMMdd_hhmm", Calendar.getInstance());
            }

            String albumExtension = ".jar";
            ErrorReporter.getInstance().addCustomData("Format", "EmailAlbum");
            if (mAlbumType == AlbumTypes.ZIP) {
                ErrorReporter.getInstance().addCustomData("Format", "Zip");
                albumExtension = ".zip";
            }
            album = new File(dests[0], mAlbumName.replaceAll("\\W", "_") + timestamp + albumExtension);
            try {
                // Total progress count.
                // 14 is the number of files contained in an 'empty'
                // EmailAlbum
                // jar. The last +1 is for the content file description

                int count = (mAlbumType == AlbumTypes.EMAILALBUM ? 14 : 0) + mContentModel.size() + 1;

                ZipEntry entry = null;
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(album));
                int entryNumber = 0;

                // First copy the album skeleton (only for EmailAlbums, not
                // for
                // zip albums)
                if (mAlbumType == AlbumTypes.EMAILALBUM) {
                    ZipInputStream in = new ZipInputStream(mContext.getAssets().open(mContext.getAssets().list("")[0]));
                    while ((entry = in.getNextEntry()) != null) {
                        out.putNextEntry(new ZipEntry(entry.getName()));
                        byte[] buffer = new byte[2048];
                        int bytesRead = 0;
                        while ((bytesRead = in.read(buffer)) >= 0) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.closeEntry();
                        in.closeEntry();
                        entryNumber++;
                        publishProgress((int) (((float) entryNumber / (float) count) * 100));
                    }
                    in.close();
                }

                String entryName = "";
                int itemNumber = 0;
                synchronized (mContentModel) {
                    for (AlbumItem item : mContentModel) {
                        ErrorReporter.getInstance().addCustomData("CurrentPic", "" + itemNumber);
                        // Create the file name. All pictures have to be
                        // named so that their alphabetical order is the
                        // order set by the user for the album.
                        // This is due to the use of Properties.load() in
                        // the jar bundled Viewer.
                        entryName = new Formatter().format("img%04d_%s.jpg", itemNumber, item.uri.getLastPathSegment())
                                .toString();

                        // Associate the caption with the picture name.
                        mContentFileBuilder.put(entryName, item.caption);

                        if (mAlbumType == AlbumTypes.EMAILALBUM) {
                            entry = new ZipEntry(ALBUM_PICTURES_PATH + entryName);
                        } else {
                            entry = new ZipEntry(entryName);
                        }

                        // Load and resize a full color picture
                        Bitmap bmp = BitmapLoader.load(mContext, item.uri, mPictureSize.getWidth(),
                                mPictureSize.getHeight(), Bitmap.Config.ARGB_8888, false);
                        if (bmp != null) {
                            ErrorReporter.getInstance().addCustomData("Apply rotation ?",
                                    "" + (item.rotation % 360 != 0));
                            if (item.rotation % 360 != 0) {
                                // Apply the user specified rotation
                                bmp = BitmapUtil.rotate(bmp, item.rotation);
                            }
                            out.putNextEntry(entry);
                            // Write the picture to the album
                            ErrorReporter.getInstance().addCustomData("bmp is null ?", "" + (bmp == null));
                            bmp.compress(CompressFormat.JPEG, mPictureQuality, out);
                            // Get rid of the bitmap to avoid memory leaks
                            bmp.recycle();
                            out.closeEntry();
                        } else {
                            ErrorReporter.getInstance().addCustomData("item.uri", item.uri.toString());
                            ErrorReporter.getInstance().addCustomData("mPictureSize.getWidth()",
                                    "" + mPictureSize.getWidth());
                            ErrorReporter.getInstance().addCustomData("mPictureSize.getHeight()",
                                    "" + mPictureSize.getHeight());
                            ErrorReporter
                                    .getInstance()
                                    .handleException(
                                            new Exception(
                                                    "Could not load image while creating archive! (BitmapLoader result is null)"));
                            new Toaster(mContext, R.string.album_creation_image_error, Toast.LENGTH_LONG)
                                    .start();
                        }
                        itemNumber++;
                        publishProgress((int) (((float) (entryNumber + itemNumber) / (float) count) * 100));
                        System.gc();
                    }
                }
                // finally write content file
                if (mAlbumType == AlbumTypes.EMAILALBUM) {
                    entry = new ZipEntry(ALBUM_CONTENT_FILE);
                    out.putNextEntry(entry);
                    mContentFileBuilder.store(out, mAlbumName);
                } else {
                    entry = new ZipEntry(ZIP_CONTENT_FILE);
                    out.putNextEntry(entry);
                    mContentFileBuilder.storeHumanReadable(out, mAlbumName);
                }
                out.closeEntry();
                out.finish();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error while creating album", e);
            }
        }
        publishProgress(100);
        return Uri.fromFile(album);
    }

    @Override
    protected void onPostExecute(final Uri result) {
        // Album generation done, close the progress dialog
        try {
            mEditor.dismissDialog(EmailAlbumEditor.DIALOG_PROGRESS_EXPORT);
        } catch (IllegalArgumentException e) {
            // do nothing
        }
        
        Handler handler = new Handler();
        // Optionally send the resulting file if the user requested it
        if (mReason == SEND) {
            handler.post(new Runnable(){

                @Override
                public void run() {
                    mEditor.sendAlbum(result);
                }
                
            });
        } else if (mReason == SEND_MULTIPLE) {
            final StringWriter bodyWriter = new StringWriter();
            try {
                mContentFileBuilder.storeHumanReadable(bodyWriter, null, null);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error : ", e);
                ErrorReporter.getInstance().handleSilentException(e);
            }
            
            handler.post(new Runnable() {
                
                @Override
                public void run() {
                    IntentHelper.sendAllPicturesInFolder(mEditor.findViewById(R.id.btn_album_share), new File(result.getPath()),
                            mAlbumName, bodyWriter.toString());
                }
            });

        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

        Log.d(LOG_TAG, "export progress : " + progress[0]);
        mEditor.getProgressDialog().setProgress(progress[0]);

    }

    private void initPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mEditor);
        mPictureQuality = prefs.getInt("picturesquality", DEFAULT_JPG_QUALITY);
        mPictureSize = PictureSizes.fromString(prefs.getString("picturessize",
                mEditor.getString(R.string.pref_def_picturessize)));
        mAlbumName = prefs.getString("albumname", mEditor.getString(R.string.pref_def_albumname));
        mAlbumType = AlbumTypes
                .fromString(prefs.getString("albumtype", mEditor.getString(R.string.pref_def_albumtype)));
        if (AlbumTypes.MAIL.equals(mAlbumType)
                && !Compatibility.isSendMultipleAppAvailable(mEditor.getApplicationContext())) {
            mAlbumType = AlbumTypes.fromString(mEditor.getString(R.string.pref_def_albumtype));
        }
        mAddTimestamp = prefs.getBoolean("albumtimestamp", mAddTimestamp);
    }
}
