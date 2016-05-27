package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.os.IBinder;
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

import cs.umass.edu.shared.SharedConstants;
import edu.umass.cs.prepare.Constants;
import edu.umass.cs.prepare.DataClient;
import cs.umass.edu.prepare.R;

//import edu.umass.cs.mobile.R;
//import edu.umass.cs.mobile.constants.Constants;
//import edu.umass.cs.mobile.preferences.SettingsActivity;
//import edu.umass.cs.mobile.storage.FileUtil;

/**
 * The Sensor Service is responsible for streaming accelerometer and signal strength data from
 * the MetaWear tag to the phone.
 *
 * TODO: Extend DataClient to differentiate between watch & tag data, also add rssi option
 */
public class SensorService extends Service implements ServiceConnection {

    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** used to communicate with the handheld application */
    private DataClient client;

    private BluetoothDevice btDevice;

    private boolean mIsBound = false;

    /** A handle to the Metawear board **/
    private MetaWearBoard mwBoard;

    /** Module that handles streaming accelerometer data from the Metawear board **/
    private Accelerometer accModule;

    /** Used to access user preferences shared across different application components **/
    SharedPreferences preferences;

    private int accelerometerSamplingRate;
    private int rssiSamplingRate;
    private BufferedWriter accelerometerFileWriter;
    private BufferedWriter rssiFileWriter;

    private boolean turnOnLedWhileRunning;
    private boolean enableAccelerometer;
    private boolean enableRSSI;

    private static class ThreeAxisSensorBuffer {
        private String[] timestamps;
        private int bufferSize;
        private float[] values;
        private int index;

        ThreeAxisSensorBuffer(int bufferSize){
            this.bufferSize = bufferSize;
            index = 0;
            timestamps = new String[bufferSize];
            values = new float[3*bufferSize];
        }

        private OnBufferFullCallback callback = null;
        interface OnBufferFullCallback{
            void onBufferFull(String[] timestamps, float[] values);
        }
        public void setOnBufferFullCallback(OnBufferFullCallback callback){
            this.callback = callback;
        }

        public void addReading(String timestamp, float x, float y, float z){
            timestamps[index] = timestamp;
            values[3 * index] = x;
            values[3 * index + 1] = y;
            values[3 * index + 2] = z;
            index++;
            if (callback != null && index == bufferSize) {
                callback.onBufferFull(timestamps, values);
                index = 0;
            }
        }
    }

    private ThreeAxisSensorBuffer accelBuffer, gyroBuffer;

    /** Buffer size */
    private static final int BUFFER_SIZE = 256;

    @Override
    public void onCreate() {
        super.onCreate();
        client = DataClient.getInstance(this);
    }

    @Override
    public void onDestroy(){
        mwBoard.disconnect();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.START_SERVICE)){
            Log.d(TAG, "Metawear service started.");
            loadSharedPreferences();
            setup();
            String mwMacAddress= intent.getStringExtra("metawear-mac-address");
            BluetoothManager btManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            btDevice= btManager.getAdapter().getRemoteDevice(mwMacAddress);
            getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        } else if (intent.getAction().equals(Constants.ACTION.STOP_SERVICE)){
            Log.d(TAG, "Metawear service stopped.");
            stopAccelerometer();
            mwBoard.disconnect();
            stopForeground(true);
            if (mIsBound){
                getApplicationContext().unbindService(this); //TODO: Also in onDestroy()
                mIsBound = false;
            }
            stopSelf();
        }
//        else if (intent.getAction().equals(Constants.KEY.CANCEL_CONNECTING)){
//            mwBoard.disconnect();
//        }
        return START_STICKY;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);

        //sendMessageToClients(Constants.MESSAGE.CONNECTING);

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                //sendMessageToClients(Constants.MESSAGE.CONNECTED);
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
        stopIntent.setAction(SharedConstants.COMMANDS.STOP_METAWEAR_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

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
                .addAction(android.R.drawable.ic_delete, SharedConstants.COMMANDS.STOP_METAWEAR_SERVICE, pendingIntent).build();

        startForeground(101, notification);
    }

    private void setup(){
        accelBuffer = new ThreeAxisSensorBuffer(BUFFER_SIZE);
        gyroBuffer = new ThreeAxisSensorBuffer(BUFFER_SIZE);
        accelBuffer.setOnBufferFullCallback(new ThreeAxisSensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(String[] timestamps, float[] values) {
                client.sendSensorData(Sensor.TYPE_ACCELEROMETER, timestamps.clone(), values.clone());
            }
        });
        gyroBuffer.setOnBufferFullCallback(new ThreeAxisSensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(String[] timestamps, float[] values) {
                client.sendSensorData(Sensor.TYPE_GYROSCOPE, timestamps.clone(), values.clone());
            }
        });
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

        turnOnLedWhileRunning = preferences.getBoolean(getString(R.string.pref_led_key),
                getResources().getBoolean(R.bool.pref_led_default));

        enableAccelerometer = preferences.getBoolean(getString(R.string.pref_accelerometer_key),
                getResources().getBoolean(R.bool.pref_accelerometer_default));

        enableRSSI = preferences.getBoolean(getString(R.string.pref_rssi_key),
                getResources().getBoolean(R.bool.pref_rssi_default));
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
                                synchronized (accelBuffer) { //add sensor data to the appropriate buffer
                                    accelBuffer.addReading(msg.getTimestampAsString(), reading.x(), reading.y(), reading.z());
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
        return null;
    }
}
