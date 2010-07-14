package com.kg.emailalbum.mobile.viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.acra.ErrorReporter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.ZipUtil;
import com.kg.oifilemanager.filemanager.FileManagerProvider;

public class ArchiveSlideshowList extends SlideshowList {

    private static final String LOG_TAG = ArchiveSlideshowList.class
            .getSimpleName();
    private ZipFile mArchive;
    private Bundle mCaptions;
    private Context mContext;
    private ArrayList<String> mItemNames;
    private int mTargetSize = 900;

    public ArchiveSlideshowList(Context context, Uri archiveUri, int targetSize) {
        try {
            File albumFile = new File(archiveUri.getPath());
            mArchive = new ZipFile(albumFile);
            mItemNames = ZipUtil.getPicturesFilesList(mArchive);
            mCaptions = ZipUtil.loadCaptions(mArchive);
            mContext = context;
            mTargetSize = targetSize;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while opening archive file.", e);
            ErrorReporter.getInstance().handleException(e);
        }
    }

    @Override
    public SlideshowItem get(int position) {
        Log.d(LOG_TAG, "Get SlideshowItem "
                + mItemNames.get(position));
        SlideshowItem result = new SlideshowItem();
        try {
            if (mTargetSize > 0) {
                Log.d(LOG_TAG, "Load image "
                        + mItemNames.get(position));
                result.bitmap = BitmapLoader.load(mContext, mArchive, mArchive
                        .getEntry(mItemNames.get(position)), mTargetSize,
                        mTargetSize);
            }
            String shortName = mItemNames.get(position).substring(
                    mItemNames.get(position).lastIndexOf('/') + 1);
            if(mCaptions != null) {
                result.caption = mCaptions.getString(shortName);
            }
            result.name = FileManagerProvider.getContentUri(mArchive.getName(), mItemNames.get(position)).toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error : ", e);
            ErrorReporter.getInstance().handleException(e);
        }
        return result;
    }

    @Override
    public int size() {
        if (mArchive != null) {
            return mItemNames.size();
        }
        return 0;
    }

    @Override
    public InputStream getOriginalInputStream(int position) throws IOException {
        if (mArchive != null) {
            return ZipUtil.getInputStream(mArchive, mArchive
                    .getEntry(mItemNames.get(position)));
        }
        return null;
    }

    @Override
    public Uri getAlbumUri() {
        return Uri.parse(mArchive.getName());
    }
}
