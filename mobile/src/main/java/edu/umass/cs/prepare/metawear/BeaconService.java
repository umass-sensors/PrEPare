package edu.umass.cs.prepare.metawear;

import android.util.Log;

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

    private ServiceManager serviceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        setBroadcaster(new Broadcaster(this));
        serviceManager = ServiceManager.getInstance(this);
    }

    @Override
    protected void onBeaconInRange(String address, double distance) {
        Log.d(TAG, "address: " + address);
        Log.d(TAG, "distance: " + distance);

        //serviceManager.startMetawearService();
    }
}
