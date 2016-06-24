package edu.umass.cs.prepare.communication;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import edu.umass.cs.prepare.sensors.SensorService;
import edu.umass.cs.shared.SharedConstants;

/**
 * The Listener Service is responsible for handling messages received from the handheld device.
 * Currently, this includes only commands to start and stop the sensor service, but it could
 * also include commands to change the sampling rate, or provide some sort of notification on
 * the wearable device.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class ListenerService extends WearableListenerService {
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = ListenerService.class.getName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(SharedConstants.COMMANDS.START_SENSOR_SERVICE)) {
            Log.d(TAG, "Got start sensor service command.");
            Intent startServiceIntent = new Intent(this, SensorService.class);
            startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
            startService(startServiceIntent);
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.STOP_SENSOR_SERVICE)) {
            Intent stopServiceIntent = new Intent(this, SensorService.class);
            stopServiceIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
            startService(stopServiceIntent);
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.START_METAWEAR_SERVICE)) {
            Intent startServiceIntent = new Intent(this, edu.umass.cs.prepare.metawear.SensorService.class);
            startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
            startService(startServiceIntent);
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.STOP_METAWEAR_SERVICE)) {
            Intent stopServiceIntent = new Intent(this, edu.umass.cs.prepare.metawear.SensorService.class);
            stopServiceIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
            startService(stopServiceIntent);
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.CANCEL_METAWEAR_CONNECTION)) {
            Intent cancelServiceIntent = new Intent(this, edu.umass.cs.prepare.metawear.SensorService.class);
            cancelServiceIntent.setAction(SharedConstants.ACTIONS.CANCEL_CONNECTING);
            startService(cancelServiceIntent);
        }
    }
}