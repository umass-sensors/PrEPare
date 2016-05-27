/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package edu.umass.cs.prepare.metawear;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;

/**
 * A placeholder fragment containing a simple view.
 */
public class DeviceSetupActivityFragment extends Fragment {
    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    private FragmentSettings settings;

    private TextView txtAccelerometer;

    public DeviceSetupActivityFragment() {
    }

    /**
     * Messenger service for exchanging messages with the background service
     */
    private Messenger mService = null;
    /**
     * Variable indicating if this activity is connected to the service
     */
    private boolean mIsBound;
    /**
     * Messenger receiving messages from the background service to update UI
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /**
     * Handler to handle incoming messages
     */
    static class IncomingHandler extends Handler {
        private final WeakReference<DeviceSetupActivityFragment> mMainActivity;

        IncomingHandler(DeviceSetupActivityFragment mainActivity) {
            mMainActivity = new WeakReference<>(mainActivity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.SENSOR_STARTED:
                {
                    //mMainActivity.get().updateStatus("sensor started.");
                    //mMainActivity.get().onSensorStarted();
                    break;
                }
                case Constants.MESSAGE.SENSOR_STOPPED:
                {
                    //mMainActivity.get().updateStatus("sensor stopped.");
                    break;
                }
                case Constants.MESSAGE.STATUS:
                {
                    //mMainActivity.get().updateStatus(msg.getData().getString(Constants.KEY.STATUS));
                    break;
                }
                case Constants.MESSAGE.ACCELEROMETER_READING:
                {
                    mMainActivity.get().displayAccelerometerReading(msg.getData().getFloatArray(Constants.KEY.ACCELEROMETER_READING));
                    break;
                }
                case Constants.MESSAGE.BATTERY_LEVEL:
                {
                    mMainActivity.get().updateBatteryLevel(msg.getData().getInt(Constants.KEY.BATTERY_LEVEL));
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Connection with the service
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            //updateStatus("Attached to the sensor service.");
            mIsBound = true;
            try {
                Message msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mIsBound = false;
            mService = null;
            //updateStatus("Disconnected from the sensor service.");
        }
    };

    /**
     * Binds the activity to the background service
     */
    void doBindService() {
        getActivity().bindService(new Intent(getActivity(), SensorService.class), mConnection, Context.BIND_AUTO_CREATE);
        //updateStatus("Binding to Service...");
    }

    /**
     * Unbind this activity from the background service
     */
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, Constants.MESSAGE.UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            //updateStatus("Unbinding from Service...");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        doBindService();

        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings= (FragmentSettings) owner;
    }

    @Override
    public void onDestroy() {
        doUnbindService();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_device_setup, container, false);
    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.acc_start).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent startServiceIntent = new Intent(getActivity(), SensorService.class);
                startServiceIntent.putExtra("metawear-device", settings.getBtDevice());
                startServiceIntent.setAction("start-service");
                getActivity().startService(startServiceIntent);
            }
        });
        view.findViewById(R.id.acc_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startServiceIntent = new Intent(getActivity(), SensorService.class);
                startServiceIntent.setAction("stop-service");
                getActivity().startService(startServiceIntent);
            }
        });
        txtAccelerometer = ((TextView)view.findViewById(R.id.acc_output));
        txtAccelerometer.setText(String.format(getString(R.string.initial_sensor_readings), 0f, 0f, 0f));
    }

    private void displayAccelerometerReading(final float[] reading){
        if (getActivity() == null) return;
        float x = reading[0];
        float y = reading[1];
        float z = reading[2];
        final String output = String.format(getString(R.string.initial_sensor_readings), x, y, z);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtAccelerometer.setText(output);
            }
        });
    }

    private Bitmap batteryLevelBitmap;

    /**
     * display the battery level in the UI
     * @param percentage battery level in the range of [0,100]
     */
    public void updateBatteryLevel(final int percentage){
        if (batteryLevelBitmap == null)
            batteryLevelBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_battery_image_set);
        int nImages = 11;
        int height = batteryLevelBitmap.getHeight();
        int width = batteryLevelBitmap.getWidth();
        int width_per_image = width / nImages;
        int index = (percentage + 5) / (nImages - 1);
        int x = width_per_image * index;
        final Bitmap batteryLevelSingleBitmap = Bitmap.createBitmap(batteryLevelBitmap, x, 0, width_per_image, height);

        Resources res = getResources();
        final BitmapDrawable icon = new BitmapDrawable(res,batteryLevelSingleBitmap);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //txtDeviceInfo.setText(percentage);
                //imgBatteryStatus.setImageBitmap(batteryLevelSingleBitmap);
                //noinspection ConstantConditions
                ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
                ((AppCompatActivity)getActivity()).getSupportActionBar().setIcon(icon);
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("");
            }
        });

    }
}
