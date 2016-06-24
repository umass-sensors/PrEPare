package edu.umass.cs.prepare.metawear;

import android.content.Intent;
import android.util.Log;

import edu.umass.cs.prepare.Broadcaster;
import edu.umass.cs.shared.SharedConstants;

/**
 * The Beacon service is responsible for monitoring the Metawear device while it is in beacon mode.
 * It is an always-on service that triggers the Metawear {@link SensorService} when the beacon is
 * in range.
 */
@Deprecated
public class BeaconService extends edu.umass.cs.shared.metawear.BeaconService {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = BeaconService.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        setBroadcaster(new Broadcaster(this));
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
