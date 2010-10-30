package com.kg.emailalbum.mobile.util;

import static com.kg.emailalbum.mobile.util.Reversed.reversed;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.kg.emailalbum.mobile.creator.EmailAlbumEditor;
import com.kg.emailalbum.mobile.ui.QuickAction;
import com.kg.oifilemanager.filemanager.FileManagerProvider;

public class IntentHelper {
    private final static String LOG_TAG = IntentHelper.class.getSimpleName();
    
    
    /**
     * Sends pictures contained in a folder as multiple attachments.
     * @param activityCtx 
     *          An Activity context - passing the Application context will crash.
     * @param folder
     *          The folder containing the pictures.
     * @param subject
     *          The subject (used for emails)
     * @param body
     *          The message body
     */
    public static void sendAllPicturesInFolder(View view, File folder, String subject, String body) {
        Intent sendIntent = prepareSendFolderIntent(folder, subject, body);
        
        QuickAction.buildChooser(view, sendIntent, true).show();
        
//        Intent chooserIntent = new Intent(activityCtx, ActivityPickerActivity.class);
//        chooserIntent.putExtra(ActivityPickerActivity.EXTRA_QUERY_INTENT, sendIntent);
//        activityCtx.startActivity(chooserIntent);
    }

    public static void sendAllPicturesInFolder(Context activityCtx, File folder, String subject, String body) {
        Intent sendIntent = prepareSendFolderIntent(folder, subject, body);
        activityCtx.startActivity(sendIntent);
        
        
//        Intent chooserIntent = new Intent(activityCtx, ActivityPickerActivity.class);
//        chooserIntent.putExtra(ActivityPickerActivity.EXTRA_QUERY_INTENT, sendIntent);
//        activityCtx.startActivity(chooserIntent);
    }

    /**
     * @param folder
     * @param subject
     * @param body
     * @return
     */
    private static Intent prepareSendFolderIntent(File folder, String subject, String body) {
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (File item : reversed(Arrays.asList(folder.listFiles()))) {
            if(!item.getName().startsWith(".")) {
                uris.add(FileManagerProvider.getContentUri(item));
            }
        }

        Intent sendIntent = new Intent(Compatibility.getActionSendMultiple());

        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Log.d(LOG_TAG, "Sending list of Uris : " + uris.toString());
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        sendIntent.putExtra(EmailAlbumEditor.EXTRA_EMAILALBUM_INTENT, true);
        sendIntent.setType("image/*");
        return sendIntent;
    }

}
