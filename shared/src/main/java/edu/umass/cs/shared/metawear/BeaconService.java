package edu.umass.cs.shared.metawear;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

import cs.umass.edu.shared.R;
import edu.umass.cs.shared.SharedConstants;

/**
 * The Beacon service is responsible for monitoring the Metawear device while it is in beacon mode.
 * It is an always-on service that listens for beacons within a specified range.
 */
public class BeaconService extends Service implements BeaconConsumer {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = BeaconService.class.getName();

    /** Manages the beacon service, which locates nearby beacons. **/
    private BeaconManager beaconManager;

    /** The maximum distance (in meters) from the beacon at which we should trigger the sensor service **/
    protected float beaconDistanceThreshold;

    /** Specifies the format of the IBeacon advertising ID associated with Metawear devices **/
    private static final String METAWEAR_BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        beaconDistanceThreshold = Integer.parseInt(preferences.getString(getString(R.string.pref_beacon_distance_key),
                getString(R.string.pref_beacon_distance_default)));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)){
                loadPreferences();
                beaconManager = BeaconManager.getInstanceForApplication(this);
                BeaconManager.setDistanceModelUpdateUrl(null); //ensures the application does not need to access remote URL
                try {
                    beaconManager.getBeaconParsers().add(new BeaconParser().
                            setBeaconLayout(METAWEAR_BEACON_LAYOUT));
                }catch(UnsupportedOperationException e){
                    e.printStackTrace();
                    //TODO: For some reason getBeaconParsers() might return an immutable collection, look into this...
                }
                beaconManager.bind(this);
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)){
                if (beaconManager != null)
                    beaconManager.unbind(this);
            }
        return START_STICKY;
    }

    protected void onBeaconInRange(String address, double distance){

    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "Beacon service connected.");
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            beaconManager.setRangeNotifier(new RangeNotifier() {
                @Override
                public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                    Log.d(TAG, beacons.size() + " beacons in region.");
                    for (Beacon beacon : beacons) {
                        if (beacon.getDistance() < beaconDistanceThreshold) {
                            onBeaconInRange(beacon.getBluetoothAddress(), beacon.getDistance());
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
