package edu.umass.cs.prepare.metawear;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.prepare.R;

/**
 * This activity allows the user to select an available Metawear device by its unique identifier.
 * All BLE Metawear devices are listed, along with their signal strength and address.
 * @see BleScannerFragment
 */
public class SelectMetawearActivity extends AppCompatActivity implements BleScannerFragment.ScannerCommunicationBus {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_metawear);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public UUID[] getFilterServiceUuids() {
        return new UUID[] {MetaWearBoard.METAWEAR_SERVICE_UUID};
    }

    @Override
    public long getScanDuration() {
        return 10000L;
    }

    @Override
    public void onDeviceSelected(final BluetoothDevice device) {
        Intent data = new Intent();
        data.putExtra(SharedConstants.KEY.UUID, device.getAddress());
        setResult(RESULT_OK, data);
        finish();
    }
}
