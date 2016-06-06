package edu.umass.cs.prepare.metawear;

import android.content.Intent;
import android.util.Log;

import edu.umass.cs.prepare.DataClient;
import edu.umass.cs.shared.SharedConstants;

/**
 * The Beacon service is responsible for monitoring the Metawear device while it is in beacon mode.
 * It is an always-on service that triggers the Metawear {@link SensorService} when the beacon is
 * in range.
 */
public class BeaconService extends edu.umass.cs.shared.metawear.BeaconService {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = BeaconService.class.getName();

    private DataClient client;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)){
                if (client == null)
                    client = DataClient.getInstance(this);
                client.sendMessage(SharedConstants.MESSAGES.BEACON_SERVICE_STARTED);
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)){
                if (client == null)
                    client = DataClient.getInstance(this);
                client.sendMessage(SharedConstants.MESSAGES.BEACON_SERVICE_STOPPED);
            }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onBeaconInRange(String address, double distance) {
        Log.d(TAG, "address: " + address);
        Log.d(TAG, "distance: " + distance);
        Intent startServiceIntent = new Intent(this, edu.umass.cs.prepare.metawear.SensorService.class);
        startServiceIntent.putExtra(SharedConstants.KEY.UUID, address);
        startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
        startService(startServiceIntent);
    }
}
