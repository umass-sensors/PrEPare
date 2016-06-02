package edu.umass.cs.shared.metawear;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Led;

import java.io.IOException;
import java.util.Map;

import cs.umass.edu.shared.R;
import edu.umass.cs.shared.PreferenceMapSerializer;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.shared.SensorBuffer;


/**
 * The shared Sensor Service defines the base service for collecting sensor data from the Metawear
 * device. The implementation may, for instance, be very different if the Metawear data should be
 * streamed to the phone or to a wearable device. Subclasses of Sensor Service may handle events,
 * such as receiving sensor readings or connecting to the Metawear device, differently by overriding
 * the appropriate methods.
 * 
 * TODO: Buffer on the Metawear device itself: http://projects.mbientlab.com/using-offline-logging-with-the-metawear-android-api/
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SensorService extends Service implements ServiceConnection {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** The Bluetooth device handle of the <a href="https://mbientlab.com/metawearc/">MetaWear C</a> tag **/
    private BluetoothDevice btDevice;

    /** Indicates whether the sensor service is bound to the {@link MetaWearBleService} **/
    private boolean mIsBound = false;

    /** A handle to the Metawear board **/
    private MetaWearBoard mwBoard;

    /** Module that handles streaming accelerometer data from the Metawear board **/
    private Accelerometer accModule;

    /** Module that handles streaming accelerometer data from the Metawear board **/
    private Gyro gyroModule;

    /** Module that handles LED state for on-board notifications. **/
    private Led ledModule;

    /** The approximate sampling rate of the accelerometer. If the sampling rate is not supported by
     * the Metawear device, then the closest supported sampling rate is used. **/
    private int accelerometerSamplingRate;

    /** The approximate sampling rate of the gyroscope. If the sampling rate is not supported by
     * the Metawear device, then the closest supported sampling rate is used. **/
    private int gyroscopeSamplingRate;

    /** The sampling rate of the phone-to-Metawear received signal strength indicator (RSSI) stream. **/
    private int rssiSamplingRate;

    /** Indicates whether the LED on the Metawear device should be turned on during streaming.
     * This is useful for notifying the user when data is being collected; however, it decreases
     * the battery life of the device. **/
    private boolean turnOnLedWhileRunning;

    /** Indicates whether accelerometer is enabled on the Metawear. **/
    private boolean enableAccelerometer;

    /** Indicates whether phone-to-Metawear received signal strength indicator (RSSI) is enabled. **/
    private boolean enableRSSI;

    /** Indicates whether gyroscope is enabled on the Metawear. **/
    private boolean enableGyroscope;

    /** Sensor buffer size. */
    private static final int BUFFER_SIZE = 256;

    /** The buffer containing the accelerometer readings. **/
    protected final SensorBuffer accelerometerBuffer = new SensorBuffer(BUFFER_SIZE, 3);

    /** The buffer containing the gyroscope readings. **/
    protected final SensorBuffer gyroscopeBuffer = new SensorBuffer(BUFFER_SIZE, 3);

    /** The buffer containing the RSSI readings. **/
    protected final SensorBuffer rssiBuffer = new SensorBuffer(BUFFER_SIZE, 1);

    /** Indicates whether the sensor service is currently running, i.e. collecting sensor data from the Metawear tag. **/
    private boolean isRunning = false;

    /** The unique address of the Metawear device. **/
    private String mwMacAddress;

    @Override
    public void onDestroy(){
        onServiceStopped();
        super.onDestroy();
    }

    protected void disconnect(){
        if (mwBoard != null)
            mwBoard.disconnect();
    }

    /**
     * Sets the callback function for the given buffer, which is called when the buffer is full.
     * @param buffer The buffer of sensor timestamps and readings
     * @param callback The function called when the buffer is full
     * **/
    protected void setOnBufferFullCallback(SensorBuffer buffer, SensorBuffer.OnBufferFullCallback callback){
        buffer.setOnBufferFullCallback(callback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)){
                parsePreferences(intent.getByteArrayExtra(SharedConstants.KEY.PREFERENCES));
                onServiceStarted();
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)){
                onServiceStopped();
            }
        return START_STICKY;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Log.d(TAG, "Connected!");
                onMetawearConnected();
            }

            @Override
            public void disconnected() {
                Log.d(TAG, "Disconnected!");
                if (isRunning)
                    mwBoard.connect(); //try reconnecting
                else {
                    if (mIsBound) {
                        getApplicationContext().unbindService(SensorService.this);
                        mIsBound = false;
                    }
                    if (hThread != null)
                        hThread.quitSafely();
                    stopForeground(true);
                    stopSelf();
                }
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

    /**
     * Gets all relevant shared preferences, given a key-value preference mapping.
     * @param serializedPreferenceMap A mapping from preference keys to values, serialized as a byte array
     */
    private void parsePreferences(final byte[] serializedPreferenceMap){
        Map<String, ?> preferenceMap;
        try {
            preferenceMap = PreferenceMapSerializer.deserialize(serializedPreferenceMap);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        accelerometerSamplingRate = (Integer) preferenceMap.get(getString(R.string.pref_accelerometer_sampling_rate_key));
        gyroscopeSamplingRate = (Integer) preferenceMap.get(getString(R.string.pref_gyroscope_sampling_rate_key));
        rssiSamplingRate = (Integer) preferenceMap.get(getString(R.string.pref_rssi_sampling_rate_key));
        turnOnLedWhileRunning = (Boolean) preferenceMap.get(getString(R.string.pref_led_key));
        enableAccelerometer = (Boolean) preferenceMap.get(getString(R.string.pref_accelerometer_key));
        enableGyroscope = (Boolean) preferenceMap.get(getString(R.string.pref_gyroscope_key));
        enableRSSI = (Boolean) preferenceMap.get(getString(R.string.pref_rssi_key));
        mwMacAddress = (String) preferenceMap.get(getString(R.string.pref_device_key));
    }

    /**
     * Prepares the Metawear board for sensor data collection.
     */
    private void ready() {
        try {
            accModule = mwBoard.getModule(Accelerometer.class);
            // Set the output data rate to 25Hz or closet valid value
            accModule.setOutputDataRate((float) accelerometerSamplingRate);

            gyroModule = mwBoard.getModule(Gyro.class);
            gyroModule.setOutputDataRate((float) gyroscopeSamplingRate);

            ledModule = mwBoard.getModule(Led.class);

        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called once the Metawear board is connected
     */
    protected void onMetawearConnected(){
        isRunning = true;
        ready();
        if (enableAccelerometer)
            startAccelerometer();
        if (enableGyroscope)
            startGyroscope();
        if (enableRSSI)
            startRSSI();
        ledModule.stop(true);
        if (turnOnLedWhileRunning)
            turnOnLed();
    }


    protected void onGyroscopeStarted(){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onAccelerometerStarted(){
        //DO NOTHING: this is meant for subclasses to override
    }

    /**
     * Called when the sensor service is started, by command from the handheld application.
     */
    protected void onServiceStarted(){
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btDevice = btManager.getAdapter().getRemoteDevice(mwMacAddress);
        mIsBound = getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
    }

    /**
     * Called when the sensor service is stopped, by command from the handheld application.
     */
    protected void onServiceStopped(){
        if (ledModule != null){
            ledModule.stop(true);
        }
        try {
            Thread.sleep(SharedConstants.LED_RESPONSE_WAIT_MILLIS); //to ensure that the LED is turned off before disconnecting
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (gyroModule != null){
            gyroModule.stop();
        }
        if (accModule != null){
            accModule.stop();
            accModule.disableAxisSampling();
        }
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        isRunning = false;
        disconnect();
    }

    protected void onAccelerometerReadingReceived(String timestamp, float x, float y, float z){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onGyroscopeReadingReceived(@SuppressWarnings("unused") String timestamp, @SuppressWarnings("unused") float x,
                                              @SuppressWarnings("unused") float y, @SuppressWarnings("unused") float z){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onRSSIReadingReceived(String timestamp, int rssi){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onBatteryLevelReceived(int percentage){
        //DO NOTHING: this is meant for subclasses to override
    }

    private void startAccelerometer(){
        accModule.routeData().fromAxes().stream(SharedConstants.METAWEAR_STREAM_KEY.ACCELEROMETER).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe(SharedConstants.METAWEAR_STREAM_KEY.ACCELEROMETER, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                CartesianFloat reading = msg.getData(CartesianFloat.class);
                                onAccelerometerReadingReceived(msg.getTimestampAsString(), reading.x(), reading.y(), reading.z());
                                synchronized (accelerometerBuffer) { //add sensor data to the appropriate buffer
                                    accelerometerBuffer.addReading(msg.getTimestampAsString(), reading.x(), reading.y(), reading.z());
                                }
                            }
                        });
                        accModule.enableAxisSampling();
                        accModule.start();
                        onAccelerometerStarted();
                    }
                });
    }

    private void startGyroscope() {
        gyroModule.routeData().fromAxes().stream(SharedConstants.METAWEAR_STREAM_KEY.GYROSCOPE).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe(SharedConstants.METAWEAR_STREAM_KEY.GYROSCOPE, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                CartesianFloat reading = msg.getData(CartesianFloat.class);
                                onGyroscopeReadingReceived(msg.getTimestampAsString(), reading.x(), reading.y(), reading.z());
                                synchronized (gyroscopeBuffer) { //add sensor data to the appropriate buffer
                                    gyroscopeBuffer.addReading(msg.getTimestampAsString(), reading.x(), reading.y(), reading.z());
                                }
                            }
                        });
                        gyroModule.start();
                        onGyroscopeStarted();
                    }
                });
    }


    private Handler handler;
    private HandlerThread hThread;

    /**
     * Streams received signal strength indicator (RSSI) between the Metawear board and the wearable.
     */
    private void startRSSI() {
        hThread = new HandlerThread("HandlerThread");
        hThread.start();

        handler = new Handler(hThread.getLooper());
        final int delay = (int) (1000.0 / rssiSamplingRate); // milliseconds
        Runnable queryRSSITask = new Runnable() {
            @Override
            public void run() {
                mwBoard.readRssi().onComplete(new AsyncOperation.CompletionHandler<Integer>() {
                    @Override
                    public void success(final Integer result) {
                        //TODO: Can we not get timestamp for RSSI reading from Metawear board?
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        onRSSIReadingReceived(timestamp, result);
                        synchronized (rssiBuffer) { //add sensor data to the appropriate buffer
                            rssiBuffer.addReading(timestamp, (int)result);
                        }
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e("Metawear Error", error.toString());
                    }
                });

                handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(queryRSSITask, delay);
    }

    /**
     * Turns on the LED on the Metawear device.
     */
    private void turnOnLed(){
        ledModule.configureColorChannel(Led.ColorChannel.BLUE)
                .setHighIntensity((byte) 31).setLowIntensity((byte) 31)
                .setHighTime((short) 1000).setPulseDuration((short) 1000)
                .setRepeatCount((byte) -1)
                .commit();
        ledModule.play(false);
    }

    /**
     * Queries the battery level of the Metawear device. To receive the result, override
     * the {@link #onBatteryLevelReceived(int)} method. Its argument is the received battery
     * level percentage in the range 1 to 100.
     */
    protected void queryBatteryLevel(){
        mwBoard.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
            @Override
            public void success(final Byte result) {
                onBatteryLevelReceived(result);
            }

            @Override
            public void failure(Throwable error) {
                Log.e("Metawear Error", error.toString());
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
