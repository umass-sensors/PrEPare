package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;

import java.io.BufferedWriter;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.preferences.SettingsActivity;
import edu.umass.cs.prepare.storage.FileUtil;
import edu.umass.cs.prepare.R;

/**
 * The Sensor Service is responsible for streaming accelerometer and signal strength data from
 * the MetaWear tag to the phone.
 */
public class SensorService extends Service implements ServiceConnection {

    private BluetoothDevice btDevice;

    private boolean mIsBound = false;

    /** A handle to the Metawear board **/
    private MetaWearBoard mwBoard;

    /** Module that handles streaming accelerometer data from the Metawear board **/
    private Accelerometer accModule;

    /** Messenger used by clients */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /** List of bound clients/activities to this service */
    private ArrayList<Messenger> mClients = new ArrayList<>();

    /** Used to access user preferences shared across different application components **/
    SharedPreferences preferences;

    private int accelerometerSamplingRate;
    private int rssiSamplingRate;
    private BufferedWriter accelerometerFileWriter;
    private BufferedWriter rssiFileWriter;

    private boolean turnOnLedWhileRunning;
    private boolean enableAccelerometer;
    private boolean enableRSSI;

    /**
     * Handler to handle incoming messages
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<SensorService> mService;

        IncomingHandler(SensorService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.REGISTER_CLIENT:
                    mService.get().mClients.add(msg.replyTo);
                    break;
                case Constants.MESSAGE.UNREGISTER_CLIENT:
                    mService.get().mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.START_SERVICE)){
            loadSharedPreferences();
            btDevice = intent.getParcelableExtra("metawear-device");
            getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        } else if (intent.getAction().equals(Constants.ACTION.STOP_SERVICE)){
            stopAccelerometer();
            stopForeground(true);
            if (mIsBound){
                getApplicationContext().unbindService(this); //TODO: Also in onDestroy()
                mIsBound = false;
            }
            stopSelf();
        } else if (intent.getAction().equals(Constants.KEY.CANCEL_CONNECTING)){
            mwBoard.disconnect();
        }
        return START_STICKY;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);

        sendMessageToClients(Constants.MESSAGE.CONNECTING);

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                sendMessageToClients(Constants.MESSAGE.CONNECTED);
                ready();
                startAccelerometer();
                showForegroundNotification();
            }

            @Override
            public void disconnected() {
                mwBoard.connect();
            }

            @Override
            public void failure(int status, Throwable error) {
                mwBoard.connect();
            }
        });
        mwBoard.connect();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    private void showForegroundNotification(){
        Intent stopIntent = new Intent(this, SensorService.class);
        stopIntent.setAction(Constants.ACTION.STOP_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DeviceSetupActivityFragment.class), 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pill);

        // notify the user that the foreground service has started
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText("Collecting sensor data...")
                .setSmallIcon(R.drawable.ic_pill)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setOngoing(true)
                .setVibrate(new long[]{0, 50, 150, 200})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(android.R.drawable.ic_delete, "stop-service", pendingIntent)
                .setContentIntent(contentIntent).build();

        startForeground(Constants.NOTIFICATION_ID.SENSOR_SERVICE, notification);
    }

    /**
     * Load all relevant shared preferences; these include the accelerometer and RSSI sampling rates,
     * the filenames and the directory where data should be written.
     */
    private void loadSharedPreferences(){
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        accelerometerSamplingRate = Integer.parseInt(preferences.getString(getString(R.string.pref_accelerometer_sampling_rate_key),
                getString(R.string.pref_accelerometer_sampling_rate_default)));
        rssiSamplingRate = Integer.parseInt(preferences.getString(getString(R.string.pref_rssi_sampling_rate_key),
                getString(R.string.pref_rssi_sampling_rate_default)));

        String accelerometerFileName = preferences.getString(Constants.PREFERENCES.FILE_NAME.ACCELEROMETER.KEY,
                Constants.PREFERENCES.FILE_NAME.ACCELEROMETER.DEFAULT);
        String rssiFileName = preferences.getString(Constants.PREFERENCES.FILE_NAME.RSSI.KEY,
                Constants.PREFERENCES.FILE_NAME.RSSI.DEFAULT);

        String path = preferences.getString(getString(R.string.pref_directory_key), SettingsActivity.DEFAULT_DIRECTORY);

        assert path != null;
        File directory = new File(path);

        accelerometerFileWriter = FileUtil.getFileWriter(accelerometerFileName, directory);
        rssiFileWriter = FileUtil.getFileWriter(rssiFileName, directory);

        turnOnLedWhileRunning = preferences.getBoolean(getString(R.string.pref_led_key),
                getResources().getBoolean(R.bool.pref_led_default));

        enableAccelerometer = preferences.getBoolean(getString(R.string.pref_accelerometer_key),
                getResources().getBoolean(R.bool.pref_accelerometer_default));

        enableRSSI = preferences.getBoolean(getString(R.string.pref_rssi_key),
                getResources().getBoolean(R.bool.pref_rssi_default));
    }


    /**
     * Sends a xyz accelerometer readings to listening clients, i.e. main UI
     * @param x acceleration along x axis
     * @param y acceleration along y axis
     * @param z acceleration along z axis
     */
    private void sendAccelerometerValuesToClients(float x, float y, float z) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send message value
                Bundle b = new Bundle();
                b.putFloatArray(Constants.KEY.ACCELEROMETER_READING, new float[]{x, y, z});
                android.os.Message msg = android.os.Message.obtain(null, Constants.MESSAGE.ACCELEROMETER_READING);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Sends the specified message to attached clients
     */
    private void sendMessageToClients(int message) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send message value
                Bundle b = new Bundle();
                android.os.Message msg = android.os.Message.obtain(null, message);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Called when the mwBoard field is ready to be used
     */
    public void ready() {
        try {
            accModule = mwBoard.getModule(Accelerometer.class);
            // Set the output data rate to 25Hz or closet valid value
            accModule.setOutputDataRate((float) accelerometerSamplingRate);

            mwBoard.readRssi().onComplete(new AsyncOperation.CompletionHandler<Integer>() {
                @Override
                public void success(final Integer result) {
                    Log.i("TAG", String.valueOf(result));
                }

                @Override
                public void failure(Throwable error) {
                    Log.e("Metawear Error", error.toString());
                }
            });

            mwBoard.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
                @Override
                public void success(final Byte result) {
                    Log.i("TAG", String.valueOf(result));
                    //updateBatteryLevel(result); //TODO: Send message back with handler;
                }

                @Override
                public void failure(Throwable error) {
                    Log.e("Metawear Error", error.toString());
                }
            });

        } catch (UnsupportedModuleException e) {
            //Snackbar.make(getActivity().findViewById(R.id.device_setup_fragment), e.getMessage(),
            //        Snackbar.LENGTH_SHORT).show();
            //TOOD: Send message back to UI
        }
    }

    private static final String ACCELEROMETER_STREAM_KEY = "accelerometer-stream";

    private void startAccelerometer(){
        if (!enableAccelerometer) return;
        accModule.routeData().fromAxes().stream(ACCELEROMETER_STREAM_KEY).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe(ACCELEROMETER_STREAM_KEY, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                CartesianFloat reading = msg.getData(CartesianFloat.class);
                                //Log.i("tutorial", msg.getData(CartesianFloat.class).toString());
                                sendAccelerometerValuesToClients(reading.x(), reading.y(), reading.z());
                                String line = String.format("%s, %f, %f, %f", msg.getTimestampAsString(), reading.x(), reading.y(), reading.z());
                                synchronized (accelerometerFileWriter) {
                                    FileUtil.writeToFile(line, accelerometerFileWriter);
                                }
                            }
                        });

                        accModule.enableAxisSampling();
                        accModule.start();
                    }
                });
    }

    private void stopAccelerometer(){
        if (accModule != null){
            accModule.stop();
            accModule.disableAxisSampling();
        }
        if (mwBoard != null)
            mwBoard.removeRoutes();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
