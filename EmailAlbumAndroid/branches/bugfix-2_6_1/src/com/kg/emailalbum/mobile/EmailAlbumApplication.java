package com.kg.emailalbum.mobile;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dDN6NDdnN2I2eWU1SW5XNmNyWUljWmc6MQ",
        resToastText = R.string.crash_toast_text)
public class EmailAlbumApplication extends Application {

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }


}
