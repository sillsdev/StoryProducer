package org.sil.storyproducer;

import android.app.Application;
import android.content.Context;

/**
 * Created by Grant Dennison on 9/13/2016.
 */
public class Main extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        Main.context = getApplicationContext();

        FileSystem.init();
    }

    public static Context getAppContext() {
        return Main.context;
    }
}
