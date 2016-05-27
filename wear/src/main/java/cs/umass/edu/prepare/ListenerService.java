package cs.umass.edu.prepare;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.UnsupportedEncodingException;

import cs.umass.edu.shared.SharedConstants;

/**
 * The Listener Service is responsible for handling messages received from the handheld device.
 * Currently, this includes only commands to start and stop the sensor service, but it could
 * also include commands to change the sampling rate, or provide some sort of notification on
 * the wearable device.
 */
public class ListenerService extends WearableListenerService {
    /** used for debugging purposes */
    private static final String TAG = ListenerService.class.getName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Received Message");
        if (messageEvent.getPath().equals(SharedConstants.COMMANDS.START_SENSOR_SERVICE)) {
            Intent startServiceIntent = new Intent(this, SensorService.class);
            startServiceIntent.setAction(Constants.ACTION.START_SERVICE);
            startService(startServiceIntent);
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.STOP_SENSOR_SERVICE)) {
            Intent stopServiceIntent = new Intent(this, SensorService.class);
            stopServiceIntent.setAction(Constants.ACTION.STOP_SERVICE);
            startService(stopServiceIntent);

            //note: we call startService() instead of stopService() and pass in an intent with the stop service action,
            //so that the service can unregister the sensors and do anything else it needs to do and then call stopSelf()
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.START_METAWEAR_SERVICE)) {
            Log.d(TAG, "Received Message: Start metawear service");
            String mwMacAddress = "";
            try {
                mwMacAddress = new String(messageEvent.getData(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Mac ID is " + mwMacAddress);
            Intent startServiceIntent = new Intent(this, cs.umass.edu.prepare.metawear.SensorService.class);
            startServiceIntent.setAction(Constants.ACTION.START_SERVICE);
            startServiceIntent.putExtra("metawear-mac-address", mwMacAddress);
            startService(startServiceIntent);
        }else if (messageEvent.getPath().equals(SharedConstants.COMMANDS.STOP_METAWEAR_SERVICE)) {
            Intent stopServiceIntent = new Intent(this, cs.umass.edu.prepare.metawear.SensorService.class);
            stopServiceIntent.setAction(Constants.ACTION.STOP_SERVICE);
            startService(stopServiceIntent);

            //note: we call startService() instead of stopService() and pass in an intent with the stop service action,
            //so that the service can unregister the sensors and do anything else it needs to do and then call stopSelf()
        }
    }
}