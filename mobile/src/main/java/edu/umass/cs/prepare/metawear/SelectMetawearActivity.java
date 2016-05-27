package edu.umass.cs.prepare.metawear;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

import edu.umass.cs.prepare.R;

public class SelectMetawearActivity extends AppCompatActivity implements BleScannerFragment.ScannerCommunicationBus {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        data.putExtra("metawear-device", device);
        setResult(RESULT_OK, data);
        finish();
    }
}
