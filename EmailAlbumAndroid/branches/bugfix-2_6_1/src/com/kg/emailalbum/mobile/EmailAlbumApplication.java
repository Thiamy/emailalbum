package com.kg.emailalbum.mobile;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dFpta2o4RmVfZ2FrTk15U3lXeEltdHc6MQ", mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast_text)
public class EmailAlbumApplication extends Application {

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }

}
