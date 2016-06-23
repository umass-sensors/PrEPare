package edu.umass.cs.prepare.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import edu.umass.cs.prepare.metawear.SensorService;

/**
 * This class handles watch reboot events. If the wearable is rebooted then
 * the {@link SensorService} should be restarted.
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent service = new Intent(context, SensorService.class);
        context.startService(service);

    }

}