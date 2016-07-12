package edu.umass.cs.shared.metawear;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.processor.Counter;

import java.util.Map;

import edu.umass.cs.shared.R;
import edu.umass.cs.shared.communication.BroadcastInterface;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.util.SensorBuffer;


/**
 * The shared Sensor Service defines the base service for collecting sensor data from the Metawear
 * device. The implementation may, for instance, be very different if the Metawear data should be
 * streamed to the phone or to a wearable device.
 *
 * The sensor service is intended to be an always-on service which listens for advertisements
 * from the <a href="https://mbientlab.com/metawearc/">MetaWear C</a> board. After the initial
 * connection to the application, the Metawear board will be configured to only advertise
 * Bluetooth packets when motion has been detected. While connected to the phone, if the board
 * detects several seconds of no motion, then it disconnects and re-enters low power motion
 * detection mode.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SensorService extends Service implements ServiceConnection {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** The Bluetooth device handle of the <a href="https://mbientlab.com/metawearc/">MetaWear C</a> board. **/
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

        /** Module responsible for the advertisement settings on the Metawear board. **/
        private Settings settingsModule;

    /** The approximate sampling rate of the accelerometer. If the sampling rate is not supported by
     * the Metawear device, then the closest supported sampling rate is used. **/
    private int accelerometerSamplingRate;

    /** The approximate sampling rate of the gyroscope. If the sampling rate is not supported by
     * the Metawear device, then the closest supported sampling rate is used. **/
    private int gyroscopeSamplingRate;

    /** The sampling rate of the received signal strength indicator (RSSI) stream. **/
    private int rssiSamplingRate;

    /** Indicates whether the LED on the Metawear device should be turned on during streaming.
     * This is useful for notifying the user when data is being collected; however, it decreases
     * the battery life of the device. **/
    private boolean blinkLedWhileRunning;

    /** Indicates whether received signal strength indicator (RSSI) streaming is enabled. **/
    private boolean enableRSSI;

    /** Indicates whether gyroscope is enabled on the Metawear. **/
    private boolean enableGyroscope;

    /** Sensor buffer size. */
    private static final int BUFFER_SIZE = 1;

    /** The buffer containing the accelerometer readings. **/
    protected final SensorBuffer accelerometerBuffer = new SensorBuffer(BUFFER_SIZE, 3);

    /** The buffer containing the gyroscope readings. **/
    protected final SensorBuffer gyroscopeBuffer = new SensorBuffer(BUFFER_SIZE, 3);

    /** The buffer containing the RSSI readings. **/
    protected final SensorBuffer rssiBuffer = new SensorBuffer(BUFFER_SIZE, 1);

    /**
     * Possible sources of disconnection from the Metawear board.
     */
    private enum DISCONNECT_SOURCE {
        /** No known source of disconnection **/
        UNKNOWN,
        /** The Metawear board is no longer in motion and need not be connected to the phone **/
        NO_MOTION_DETECTED,
        /** Bluetooth has been disabled and a connection cannot be established **/
        BLUETOOTH_DISABLED,
        /** The device is no longer in range **/
        OUT_OF_RANGE,
        /** The user requested a disconnection explicitly **/
        DISCONNECT_REQUESTED
    }

    /** The source of disconnection from the Metawear board. **/
    private DISCONNECT_SOURCE disconnectSource = DISCONNECT_SOURCE.UNKNOWN;

    /** The unique address of the Metawear device. **/
    private String address;

    /** Handles the recurring RSSI requests. **/
    private Handler handler;

    /** Thread for the RSSI request handler. **/
    private HandlerThread hThread;

    /**
     * The broadcaster is responsible for handling communication from the Sensor service
     * to other application components, both on the mobile and wearable side.
     */
    private BroadcastInterface broadcaster;

    /**
     * Number of seconds that no motion must be detected before disconnection.
     */
    private static final int NO_MOTION_DURATION = 5;

    /**
     * Threshold for the sum over difference in square magnitude, below which no motion is detected.
     */
    private static final float MOTION_THRESHOLD = 0.1875f;

    /**
     * The index into the window of magnitudes.
     */
    private int magnitudesIndex = 0;

    /**
     * The previous square accelerometer magnitude measurement.
     */
    private float prevMagnitudeSq = 0;

    /**
     * The number of milliseconds after disconnecting from the board due to no motion before attempting to reconnect.
     */
    private static final int RECONNECTION_TIMEOUT_MILLIS = 2000;


    /**
     * Sets the callback function for the given buffer, which is called when the buffer is full.
     * @param buffer The buffer of sensor timestamps and readings
     * @param callback The function called when the buffer is full
     * **/
    protected void setOnBufferFullCallback(SensorBuffer buffer, SensorBuffer.OnBufferFullCallback callback){
        buffer.setOnBufferFullCallback(callback);
    }

    /**
     * Sets the broadcaster, which defines how messages and sensor data should be shared with
     * other application components.
     * @param broadcaster the broadcaster implementation
     */
    protected void setBroadcaster(BroadcastInterface broadcaster){
        this.broadcaster = broadcaster;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
                onServiceStarted();
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)) {
                onServiceStopped();
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.CANCEL_CONNECTING)) {
                onServiceStopped();
            }
        } else {
            Log.d(TAG, "Service restarted after killed by OS.");
            onServiceStarted();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateListener, filter);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Unregistering receiver");
        unregisterReceiver(bluetoothStateListener);
        super.onDestroy();
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
        address = preferences.getString(getString(R.string.pref_device_key), getString(R.string.pref_device_default));
    }

    /**
     * Called when the sensor service is started, by command from the handheld application.
     */
    protected void onServiceStarted(){
        if (mIsBound) return;
        loadPreferences();
        //TODO : If BT is off?
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        try {
            btDevice = btManager.getAdapter().getRemoteDevice(address);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
            if (broadcaster != null)
                broadcaster.broadcastMessage(SharedConstants.MESSAGES.INVALID_ADDRESS);
            //TODO: Notify user that address is not valid
            return;
        }
        mIsBound = getApplicationContext().bindService(new Intent(SensorService.this, MetaWearBleService.class),
                SensorService.this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard = ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Log.d(TAG, "Connected!");
                onMetawearConnected();
            }

            @Override
            public void disconnected() {
                Log.d(TAG, "Disconnected!");
                onMetawearDisconnected();
            }

            @Override
            public void failure(int status, Throwable error) {
                error.printStackTrace();
                if (disconnectSource == DISCONNECT_SOURCE.UNKNOWN)
                    connect();
            }
        });
        Log.d(TAG, "Service connected...");
        connect();
    }

    private void doUnbind(){
        if (mIsBound) {
            getApplicationContext().unbindService(SensorService.this);
            mIsBound = false;
        }
    }

    /**
     * Called when the application disconnects from the Metawear board.
     */
    protected void onMetawearDisconnected(){
        Log.d(TAG, "onDisconnected(): " + disconnectSource.name());
        switch (disconnectSource){
            case BLUETOOTH_DISABLED:
                if (handler != null)
                    handler.removeCallbacksAndMessages(null);
                doUnbind();
                break;
            case DISCONNECT_REQUESTED:
                if (handler != null)
                    handler.removeCallbacksAndMessages(null);
                doUnbind();
                if (broadcaster != null)
                    broadcaster.broadcastMessage(SharedConstants.MESSAGES.METAWEAR_DISCONNECTED);
                stopForeground(true);
                stopSelf();
                return;
            case NO_MOTION_DETECTED:
                if (handler != null)
                    handler.removeCallbacksAndMessages(null);
                postReconnect();
                break;
            case UNKNOWN:
                connect();
                return;
        }
        disconnectSource = DISCONNECT_SOURCE.UNKNOWN;
        if (broadcaster != null)
            broadcaster.broadcastMessage(SharedConstants.MESSAGES.METAWEAR_DISCONNECTED);
    }

    protected void onConnectionRequest(){
        if (broadcaster != null)
            broadcaster.broadcastMessage(SharedConstants.MESSAGES.METAWEAR_CONNECTING);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called once the Metawear board is connected
     */
    protected void onMetawearConnected(){
        loadPreferences();
        getModules();
        mwBoard.removeRoutes();
        stopSensors();
        handleBoardDisconnectionEvent();
        setSamplingRates();
        startSensors();
        if (broadcaster != null)
            broadcaster.broadcastMessage(SharedConstants.MESSAGES.METAWEAR_CONNECTED);
    }

    /**
     * Requests the relevant modules from the Metawear board.
     */
    private void getModules(){
        try {
            motionModule = mwBoard.getModule(Bmi160Accelerometer.class);
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
        //ensures that if the disconnect monitor event does not properly get called, then we can reconnect to the board
        //settingsModule.configure().setAdInterval((short) 1000, (byte) 0).commit();
        settingsModule.handleEvent().fromDisconnect()
                .monitor(new DataSignal.ActivityHandler() {
                    @Override
                    public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                        stopSensors();

                        // start low power motion detection on the board
                        motionModule.enableMotionDetection(Bmi160Accelerometer.MotionType.ANY_MOTION);
                        motionModule.configureAnyMotionDetection().setDuration(10).commit();
                        motionModule.startLowPower();

                        // stop advertisements
                        settingsModule.configure().setAdInterval((short) 25, (byte) 1).commit();
                    }
        }).commit();
    }

    /**
     * Starts all enabled sensors and blinks LED on the Metawear board.
     */
    private void startSensors() {
        if (blinkLedWhileRunning)
            turnOnLed(Led.ColorChannel.GREEN, true);
        if (enableGyroscope)
            startGyroscope();
        if (enableRSSI)
            startRSSI();
        startAccelerometerWithNoMotionDetection();
    }

    /**
     * Stops sensors/LED on the Metawear board.
     */
    private void stopSensors(){
        if (ledModule != null) {
            ledModule.stop(true);
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
     * Called when the Metawear board is no longer in motion. At this point, the sensors
     * will be stopped, the board will listen for motion and the phone and board will disconnect.
     */
    private void onNoMotionDetected(){
        disconnectSource = DISCONNECT_SOURCE.NO_MOTION_DETECTED;
        startMotionDetectionThenDisconnect();
    }

    /**
     * Starts collecting accelerometer data from the Metawear board until a no motion event has
     * been detected.
     */
    private void startAccelerometerWithNoMotionDetection(){
        Log.d(TAG, "Routing accelerometer data...");
        final float[] diffInMagnitudeWindow = new float[NO_MOTION_DURATION * accelerometerSamplingRate];
        accModule.routeData().fromAxes()
                .stream(SharedConstants.METAWEAR_STREAM_KEY.ACCELEROMETER)
                .commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe(SharedConstants.METAWEAR_STREAM_KEY.ACCELEROMETER, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                CartesianFloat reading = msg.getData(CartesianFloat.class);
                                float x = reading.x(), y = reading.y(), z = reading.z();
                                long timestamp = msg.getTimestamp().getTimeInMillis();
                                onAccelerometerReadingReceived(timestamp, x, y, z);
                                synchronized (accelerometerBuffer) { //add sensor data to the appropriate buffer
                                    accelerometerBuffer.addReading(timestamp, x, y, z);
                                }

                                float magnitudeSq = x*x + y*y + z*z;
                                diffInMagnitudeWindow[magnitudesIndex++] = Math.abs(magnitudeSq - prevMagnitudeSq);
                                prevMagnitudeSq = magnitudeSq;

                                if (magnitudesIndex >= NO_MOTION_DURATION){
                                    float sumOverDiffInMagnitude = 0f;
                                    while (magnitudesIndex > 0){
                                        sumOverDiffInMagnitude += diffInMagnitudeWindow[magnitudesIndex];
                                        magnitudesIndex--;
                                    }
                                    if (sumOverDiffInMagnitude < MOTION_THRESHOLD){
                                        onNoMotionDetected();
                                    }
                                }
                            }
                        });
                        Log.d(TAG, "Starting accelerometer...");
                        accModule.enableAxisSampling();
                        accModule.start();
                        onAccelerometerStarted();
                    }

                    @Override
                    public void failure(Throwable error) {
                        error.printStackTrace();
                    }
                });
    }

    /**
     * Starts motion detection on the Metawear board, then disconnects from the board.
     */
    private void startMotionDetectionThenDisconnect(){
        if (motionModule != null)
            motionModule.routeData().fromMotion().process(new Counter())
                    .monitor(new DataSignal.ActivityHandler() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                            //turnOnLed(Led.ColorChannel.BLUE, false); //TODO: Don't turn on LED in final system due to high battery consumption
                            settingsModule.startAdvertisement();
                        }
                    }).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            disconnect();
                        }
            });
    }

    /**
     * Starts the Gyroscope sensor on the Metawear board.
     */
    private void startGyroscope() {
        Log.d(TAG, "Routing gyroscope data...");
        gyroModule.routeData().fromAxes().stream(SharedConstants.METAWEAR_STREAM_KEY.GYROSCOPE).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe(SharedConstants.METAWEAR_STREAM_KEY.GYROSCOPE, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                CartesianFloat reading = msg.getData(CartesianFloat.class);
                                onGyroscopeReadingReceived(msg.getTimestamp().getTimeInMillis(), reading.x(), reading.y(), reading.z());
                                synchronized (gyroscopeBuffer) { //add sensor data to the appropriate buffer
                                    gyroscopeBuffer.addReading(msg.getTimestamp().getTimeInMillis(), reading.x(), reading.y(), reading.z());
                                }
                            }
                        });
                        Log.d(TAG, "Starting gyroscope...");
                        gyroModule.start();
                        onGyroscopeStarted();
                    }
                });
    }

    /**
     * Streams received signal strength indicator (RSSI) between the Metawear board and the wearable.
     */
    private void startRSSI() {
        Log.d(TAG, "Routing RSSI data...");
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        if (hThread != null && hThread.isAlive()) {
            hThread.interrupt();
            hThread.quit();
        }
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
                        //TODO: Can we not get timestamp for RSSI reading from Metawear board? Then make sure formatting matches that from board
                        long timestamp = System.currentTimeMillis();
                        onRSSIReadingReceived(timestamp, result);
                        synchronized (rssiBuffer) { //add sensor data to the appropriate buffer
                            rssiBuffer.addReading(timestamp, (int) result);
                        }
                    }

                    @Override
                    public void failure(Throwable error) {
                        error.printStackTrace();
                    }
                });

                if (mwBoard.isConnected())
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

    protected void onAccelerometerReadingReceived(long timestamp, float x, float y, float z){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onGyroscopeReadingReceived(long timestamp, float x, float y, float z){
        //DO NOTHING: this is meant for subclasses to override
    }

    protected void onRSSIReadingReceived(long timestamp, int rssi){
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
                .setRiseTime((short) 125).setFallTime((short)125)
                .setHighTime((short) 250).setPulseDuration((short) 1000)
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
                    turnOnLed(Led.ColorChannel.RED, false);
                }
                onBatteryLevelReceived(result);
            }

            @Override
            public void failure(Throwable error) {
                error.printStackTrace();
            }
        });
    }

    /**
     * Called when the sensor service is stopped, by command from the handheld application.
     */
    protected void onServiceStopped(){
        Log.d(TAG, "Request service stopped.");
        disconnectSource = DISCONNECT_SOURCE.DISCONNECT_REQUESTED;
        if (mwBoard != null && mwBoard.isConnected()) {
            startMotionDetectionThenDisconnect();
        } else {
            disconnect();
            onMetawearDisconnected();
        }
    }

    /**
     * Connect to the Metawear board.
     */
    protected void connect(){
        mwBoard.connect();
        onConnectionRequest();
    }

    /**
     * Attempt to reconnect to the Metawear board after {@link #RECONNECTION_TIMEOUT_MILLIS} milliseconds.
     */
    protected void postReconnect(){
        if (hThread == null) {
            hThread = new HandlerThread("HandlerThread");
            hThread.start();
        }
        Handler reconnectionHandler = new Handler(hThread.getLooper());
        Runnable reconnectAfterDelayTask = new Runnable() {
            @Override
            public void run() {
                connect();
            }
        };
        reconnectionHandler.postDelayed(reconnectAfterDelayTask, RECONNECTION_TIMEOUT_MILLIS);
    }

    /**
     * Disconnect from the Metawear board.
     */
    protected void disconnect(){
        if (mwBoard != null)
            mwBoard.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when Bluetooth is disabled. If currently connected to the Metawear board, safely
     * disconnect; otherwise go immediately to the {@link #onMetawearDisconnected()} method.
     */
    protected void onBluetoothDisabled(){
        disconnectSource = DISCONNECT_SOURCE.BLUETOOTH_DISABLED;
        if (mwBoard != null && mwBoard.isConnected()) {
            startMotionDetectionThenDisconnect();
        } else {
            disconnect();
            onMetawearDisconnected();
        }
    }

    /**
     * Called when Bluetooth is enabled. The service should be restarted and a connection attempt
     * should be made.
     */
    protected void onBluetoothEnabled(){
        onServiceStarted();
    }

    /**
     * Listens for Bluetooth state changes. If the user disables Bluetooth on the phone,
     * then the service should gracefully disconnect from the board and when Bluetooth
     * is re-enabled, it should reattempt to connect immediately.
     */
    private final BroadcastReceiver bluetoothStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, getString(R.string.bluetooth_off));
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, getString(R.string.bluetooth_disabled));
                        onBluetoothDisabled();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, getString(R.string.bluetooth_on));
                        onBluetoothEnabled();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, getString(R.string.bluetooth_enabled));
                        break;
                }
            }
        }
    };
}
