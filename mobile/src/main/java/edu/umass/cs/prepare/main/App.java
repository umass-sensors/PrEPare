package edu.umass.cs.prepare.main;

import android.app.Application;

import wearprefs.WearPrefs;

public class App extends Application {

    @Override public void onCreate() {
        super.onCreate();

        // Initialize WearPrefs for the default SharedPreferences file
        WearPrefs.init(this);
    }

}