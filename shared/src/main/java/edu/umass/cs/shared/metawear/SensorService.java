package edu.umass.cs.shared.metawear;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.processor.Average;
import com.mbientlab.metawear.processor.Comparison;
import com.mbientlab.metawear.processor.Counter;
import com.mbientlab.metawear.processor.Maths;
import com.mbientlab.metawear.processor.Rss;
import com.mbientlab.metawear.processor.Time;

import java.util.Map;

import cs.umass.edu.shared.R;
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

        /** Detects motion events to minimize overall power consumption of the Metawear board. **/
        private Bmi160Accelerometer motionModule;

        /** Module that handles streaming accelerometer data from the Metawear board. **/
        private Accelerometer accModule;

        /** Module that handles streaming accelerometer data from the Metawear board. **/
        private Gyro gyroModule;

        /** Module that handles LED state for on-board notifications. **/
        private Led ledModule;

        /** Module that handles logging of sensor data on the Metawear board. **/
        private Logging loggingModule;

        /** Module responsible for the advertisement settings on the Metawear board. **/
        private Settings settingsModule;

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
    private boolean blinkLedWhileRunning;

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

    /** Indicates whether the sensor service is currently connected to the Metawear board. **/
    private boolean isConnected = false;

    /** The unique address of the Metawear device. **/
    private String mwMacAddress;

    /** Handles the recurring RSSI requests. **/
    private Handler handler;

    /** Thread for the RSSI request handler. **/
    private HandlerThread hThread;

    @Override
    public void onDestroy(){
        onServiceStopped();
        super.onDestroy();
    }

    protected void disconnect(){
        isConnected = false;
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
                loadPreferences();
                onServiceStarted();
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)){
                onServiceStopped();
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.CANCEL_CONNECTING)){
                disconnect();
            }
        return START_STICKY;
    }

    /**
     * Gets all relevant shared preferences.
     */
    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        accelerometerSamplingRate = Integer.parseInt(preferences.getString(getString(R.string.pref_accelerometer_sampling_rate_key),
                getString(R.string.pref_accelerometer_sampling_rate_default)));
        gyroscopeSamplingRate = Integer.parseInt(preferences.getString(getString(R.string.pref_gyroscope_sampling_rate_key),
                getString(R.string.pref_gyroscope_sampling_rate_default)));
        rssiSamplingRate = Integer.parseInt(preferences.getString(getString(R.string.pref_rssi_sampling_rate_key),
                getString(R.string.pref_rssi_sampling_rate_default)));
        blinkLedWhileRunning = preferences.getBoolean(getString(R.string.pref_led_key), getResources().getBoolean(R.bool.pref_led_default));
        enableGyroscope = preferences.getBoolean(getString(R.string.pref_gyroscope_key), getResources().getBoolean(R.bool.pref_gyroscope_default));
        enableRSSI = preferences.getBoolean(getString(R.string.pref_rssi_key), getResources().getBoolean(R.bool.pref_rssi_default));
        mwMacAddress = preferences.getString(getString(R.string.pref_device_key), getString(R.string.pref_device_default));
    }

    /**
     * Called when the sensor service is started, by command from the handheld application.
     */
    protected void onServiceStarted(){
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        try {
            btDevice = btManager.getAdapter().getRemoteDevice(mwMacAddress);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
            //TODO: Notify user that address is not valid
            return;
        }
        mIsBound = getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (mwBoard == null)
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
                if (isConnected)
                    mwBoard.connect();
                else {
                    if (handler != null)
                        handler.removeCallbacksAndMessages(null);

                    Handler reconnectionHandler = new Handler(hThread.getLooper());
                    Runnable reconnectAfterDelayTask = new Runnable() {
                        @Override
                        public void run() {
                            mwBoard.connect();
                        }
                    };
                    reconnectionHandler.postDelayed(reconnectAfterDelayTask, 3000);
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
     * Called once the Metawear board is connected
     */
    protected void onMetawearConnected(){
        isConnected = true;
        getModules();
        stopSensors();
        mwBoard.removeRoutes();
        handleBoardDisconnectionEvent();
        setSamplingRates();
        startSensors();
    }

    /**
     * Requests the relevant modules from the Metawear board.
     */
    private void getModules(){
        try {
            motionModule = mwBoard.getModule(Bmi160Accelerometer.class);
            loggingModule = mwBoard.getModule(Logging.class);
            accModule = mwBoard.getModule(Accelerometer.class);
            gyroModule = mwBoard.getModule(Gyro.class);
            ledModule = mwBoard.getModule(Led.class);
            settingsModule = mwBoard.getModule(Settings.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepares the Metawear board for sensor data collection.
     */
    private void setSamplingRates() {
        accModule.setOutputDataRate((float) accelerometerSamplingRate);
        gyroModule.setOutputDataRate((float) gyroscopeSamplingRate);
    }

    /**
     * Sends commands to the board upon disconnection. When the board is disconnected, we specifically
     * want to start low power motion detection, stop all other sensors and stop advertisements.
     */
    private void handleBoardDisconnectionEvent(){
        settingsModule.handleEvent().fromDisconnect().monitor(new DataSignal.ActivityHandler() {
            @Override
            public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                stopSensors();

                // start low power motion detection on the board
                motionModule.enableMotionDetection(Bmi160Accelerometer.MotionType.ANY_MOTION);
                motionModule.configureAnyMotionDetection().setDuration(10).commit();
                motionModule.startLowPower();

                // stop advertisements
                settingsModule.configure().setAdInterval((short) 100, (byte) 2).commit();
            }
        }).commit();
    }

    private void startSensors() {
        if (blinkLedWhileRunning)
            turnOnLed(Led.ColorChannel.GREEN, true);
        if (enableGyroscope)
            startGyroscope();
        if (enableRSSI)
            startRSSI();
        startAccelerometerWithNoMotionDetection();
    }

    private void stopSensors(){
        if (ledModule != null) {
            ledModule.stop(true);
        }
        if (loggingModule != null) {
            loggingModule.stopLogging();
        }
        if (gyroModule != null) {
            gyroModule.stop();
        }
        if (accModule != null) {
            accModule.stop();
            accModule.disableAxisSampling();
        }
        if (motionModule != null){
            motionModule.stop();
            motionModule.disableMotionDetection();
        }
    }

    /**
     * Starts collecting accelerometer data from the Metawear board until a no motion event has
     * been detected.
     */
    private void startAccelerometerWithNoMotionDetection(){
        accModule.routeData().fromAxes()
                .split()
                .branch()
                    .process(new Rss())
                    .process(new Maths(Maths.Operation.SUBTRACT, 1))
                    .process(new Maths(Maths.Operation.ABS_VALUE, 0))
                    .process(new Average((byte) 127))
                    .process(new Time(Time.OutputMode.ABSOLUTE, 2000))
                    .process(new Comparison(Comparison.Operation.LT, 0.006))
                    .stream("no-motion")
                .branch()
                    .stream("accelerometer-stream")
                .end()
                .commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("no-motion", new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                stopSensors();
                                startMotionDetection();
                            }
                        });
                        result.subscribe("accelerometer-stream", new RouteManager.MessageHandler() {
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

    private void startMotionDetection(){
        motionModule.routeData().fromMotion().process(new Counter())
                .monitor(new DataSignal.ActivityHandler() {
                    @Override
                    public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                        turnOnLed(Led.ColorChannel.GREEN, false);
                        settingsModule.startAdvertisement();
                    }
                }).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                disconnect();
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
                        error.printStackTrace();
                    }
                });

                handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(queryRSSITask, delay);
    }

    protected void onAccelerometerStarted(){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onGyroscopeStarted(){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onAccelerometerReadingReceived(String timestamp, float x, float y, float z){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onGyroscopeReadingReceived(String timestamp, float x, float y, float z){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onRSSIReadingReceived(String timestamp, int rssi){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onBatteryLevelReceived(int percentage){
        //DO NOTHING: this is meant for subclasses to override
    }

    /**
     * Turns on the LED on the Metawear device.
     */
    private void turnOnLed(Led.ColorChannel color, boolean ongoing){
        ledModule.stop(true);
        byte repeat = -1;
        if (!ongoing)
            repeat = 1;
        ledModule.configureColorChannel(color)
                .setHighIntensity((byte) 15).setLowIntensity((byte) 0)
                .setHighTime((short) 1000).setPulseDuration((short) 500)
                .setRepeatCount(repeat)
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
                if (result <= 10) {
                    turnOnLed(Led.ColorChannel.RED, true);
                }
                onBatteryLevelReceived(result);
            }

            @Override
            public void failure(Throwable error) {
                Log.e("Metawear Error", error.toString());
            }
        });
    }

    /**
     * Called when the sensor service is stopped, by command from the handheld application.
     */
    protected void onServiceStopped(){
        isConnected = false;
        disconnect();
        if (hThread != null) {
            hThread.interrupt(); //TODO: I believe this will cancel the reconnection attempt, but make sure of this!
            hThread.quitSafely();
        }
        if (mIsBound) {
            getApplicationContext().unbindService(SensorService.this);
            mIsBound = false;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
