package edu.umass.cs.prepare.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.metawear.SensorService;

/**
 * This class handles boot events. If the mobile device is rebooted and the
 * {@link SensorService} was running, then the service should be restarted.
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = preferences.getBoolean(context.getString(R.string.pref_connect_key),
                context.getResources().getBoolean(R.bool.pref_connect_default));
        if (enabled)
            ServiceManager.getInstance(context).startMetawearService();
    }

}