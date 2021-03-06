package edu.umass.cs.prepare.view.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.prepare.R;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment.*;
import com.mbientlab.metawear.MetaWearBleService;

/**
 * This UI allows the user to select a pill bottle to connect. Only one device may be selected.
 * All devices within range will be displayed along with their signal strength (RSSI) in order
 * to differentiate devices with the same name.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see <a href="https://github.com/mbientlab/Metawear-SampleAndroidApp/blob/master/app/src/main/java/com/mbientlab/metawear/app/ScannerActivity.java">ScannerActivity</a>
 * @see ScannerCommunicationBus
 * @see MetaWearBleService
 */
public class SelectDeviceActivity extends AppCompatActivity implements ScannerCommunicationBus, ServiceConnection {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SelectDeviceActivity.class.getName();

    /**
     * Unique Metawear device identifiers
     */
    private final static UUID[] serviceUUIDs;

    static {
        serviceUUIDs = new UUID[] {
                MetaWearBoard.METAWEAR_SERVICE_UUID,
                MetaWearBoard.METABOOT_SERVICE_UUID
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_metawear);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device) {
        final Intent data = new Intent();
        data.putExtra(SharedConstants.KEY.UUID, device.getAddress());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MetaWearBleService.LocalBinder serviceBinder = (MetaWearBleService.LocalBinder) iBinder;
        serviceBinder.executeOnUiThread();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    public UUID[] getFilterServiceUuids() {
        return serviceUUIDs;
    }

    @Override
    public long getScanDuration() {
        return 10000L;
    }
}