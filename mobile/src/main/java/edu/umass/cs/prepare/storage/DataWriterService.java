package edu.umass.cs.prepare.storage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import autovalue.shaded.org.apache.commons.lang.ArrayUtils;
import edu.umass.cs.prepare.MHLClient.MHLConnectionStateHandler;
import edu.umass.cs.prepare.MHLClient.MHLMobileIOClient;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLSensorReading;
import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.wearable.DataReceiverService;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.view.activities.MainActivity;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;

/**
 * The Data Writer Service is responsible for writing all sensor data to their respective files.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see DataReceiverService
 * @see Service
 */
public class DataWriterService extends Service {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = DataWriterService.class.getName();

    /** CSV extension */
    private static final String CSV_EXTENSION = ".csv";

    /** Indicates whether data should be sent to the server. **/
    private boolean writeServer;

    /** Client responsible for communicating to the server. **/
    private MHLMobileIOClient client;

    /**
     * Mapping from sensor identifiers, e.g. "ACCELEROMETER_WEARABLE", to file writers
     */
    private final HashMap<String, AsyncFileWriter> fileWriterHashMap = new HashMap<>();

    /**
     * Provides access to all shared application preferences.
     */
    private ApplicationPreferences applicationPreferences;

    /** The current network connection state. **/
    private static NETWORK_STATE networkState;

    /** Set of network connection states. **/
    public enum NETWORK_STATE {
        DISCONNECTED,
        CONNECTED,
        CONNECTION_FAILED
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationPreferences = ApplicationPreferences.getInstance(this);
    }

    /**
     * Gets the file writer associated with the given sensor identifier, instantiating it if necessary.
     * @param filename the name of the file, i.e. the sensor identifier
     * @return the file writer object
     */
    private AsyncFileWriter getFileWriter(String filename) {
        File directory = new File(applicationPreferences.getSaveDirectory());
        if(!directory.exists()) {
            if (directory.mkdirs()){
                Toast.makeText(this, String.format("Created directory %s", directory.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, String.format("Failed to create directory %s. Please set the directory in Settings",
                        directory.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }
        }
        AsyncFileWriter writer = fileWriterHashMap.get(filename);
        if (writer == null) {
            String fullFileName = filename + String.valueOf(System.currentTimeMillis()) + CSV_EXTENSION;

            try {
                writer = new AsyncFileWriter(new File(directory, fullFileName));
                writer.open();
                fileWriterHashMap.put(filename, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return writer;
    }

    /** used to receive messages from other components of the handheld app through intents, i.e. receive labels **/
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_SENSOR_DATA)) {

                    long[] timestamps = intent.getLongArrayExtra(Constants.KEY.TIMESTAMP);
                    float[] values = intent.getFloatArrayExtra(Constants.KEY.SENSOR_DATA);
                    SharedConstants.SENSOR_TYPE sensorType = DataLayerUtil.deserialize(SharedConstants.SENSOR_TYPE.class).from(intent);

                    if (sensorType == SharedConstants.SENSOR_TYPE.BATTERY_METAWEAR) return; //ignore battery readings

                    StringBuilder builder = new StringBuilder(timestamps.length * (Constants.BYTES_PER_TIMESTAMP + Constants.BYTES_PER_SENSOR_READING + 6));

                    boolean receivedRSSI = sensorType.getSensor().equals(SharedConstants.SENSOR.RSSI.TITLE);
                    final String formatString;
                    if (receivedRSSI)
                        formatString = "%f\n";
                    else
                        formatString = "%f,%f,%f\n";

                    boolean metawear = sensorType.getDevice().equals(SharedConstants.DEVICE.METAWEAR.TITLE);
                    for (int i = 0; i < timestamps.length; i++) {
                        long timestamp = timestamps[i];
                        final float[] reading;
                        if (receivedRSSI){
                            reading = new float[]{values[i]};
                        }
                        else {
                            reading = new float[]{
                                    values[3 * i] * (metawear ? SharedConstants.GRAVITY : 1),
                                    values[3 * i + 1] * (metawear ? SharedConstants.GRAVITY : 1),
                                    values[3 * i + 2] * (metawear ? SharedConstants.GRAVITY : 1)};
                        }

                        if (writeServer) {
                            client.addSensorReading(MHLSensorReading.getReading(sensorType, timestamp, reading));
                            //we must wait briefly after adding to the queue, otherwise subsequent data will not be received
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ignored) {}
                        }
                        if (applicationPreferences.writeLocal()){
                            builder.append(String.format(Locale.getDefault(), "%d,", timestamp));
                            builder.append(String.format(Locale.getDefault(), formatString, ArrayUtils.toObject(reading)));
                        }
                    }

                    if (applicationPreferences.writeLocal())
                        getFileWriter(sensorType.name()).append(builder.toString());
                }
            }
        }
    };

    /**
     * Called when the service is started; we should initialize the file writer objects and
     * connect to the server if possible.
     */
    private void init(){
        networkState = NETWORK_STATE.DISCONNECTED;
        writeServer = applicationPreferences.writeServer();
        if (writeServer) {
            client = new MHLMobileIOClient(applicationPreferences.getIpAddress(), SharedConstants.SERVER_PORT, 0);
            client.setConnectionStateHandler(new MHLConnectionStateHandler() {
                @Override
                public void onConnected() {
                    sendMessage(SharedConstants.MESSAGES.SERVER_CONNECTION_SUCCEEDED);
                    networkState = NETWORK_STATE.CONNECTED;
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(SharedConstants.NOTIFICATION_ID.DATA_WRITER_SERVICE, getNotification());
                }

                @Override
                public void onConnectionFailed() {
                    writeServer = false;
                    sendMessage(SharedConstants.MESSAGES.SERVER_CONNECTION_FAILED);
                    networkState = NETWORK_STATE.CONNECTION_FAILED;
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(SharedConstants.NOTIFICATION_ID.DATA_WRITER_SERVICE, getNotification());
                }
            });
            client.connect();
        }
    }

    /**
     * Registers a receiver with an intent filter for message
     * {@link edu.umass.cs.prepare.constants.Constants.ACTION#BROADCAST_SENSOR_DATA}, i.e.
     * the service listens for sensor data sent from another application component.
     */
    private void registerReceiver(){
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
        broadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * Unregisters the broadcast receiver.
     */
    private void unregisterReceiver(){
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to other mobile application components.
     * @param message an integer message being sent
     */
    private void sendMessage(int message){
        Intent intent = new Intent();
        intent.putExtra(SharedConstants.KEY.MESSAGE, message);
        intent.setAction(Constants.ACTION.BROADCAST_MESSAGE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }

    /**
     * Builds and returns the updated notification. The content text and icon depend on the
     * current network connection status. If the connection failed, then the user may also
     * swipe away the notification.
     * @return the notification object. Call {@link #startForeground(int, Notification)}
     * or {@link NotificationManager#notify(int, Notification)} to display the notification.
     */
    private Notification getNotification(){
        Intent notificationIntent = new Intent(DataWriterService.this, MainActivity.class); //open main activity when user clicks on notification
        notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(DataWriterService.this, 0, notificationIntent, 0);

        final int icon;
        final String contentText;
        if (networkState == NETWORK_STATE.CONNECTED){
            icon = R.drawable.ic_cloud_done_white_24dp;
            contentText = getString(R.string.notification_network_connected);
        } else if (networkState == NETWORK_STATE.DISCONNECTED) {
            icon = R.drawable.ic_cloud_white_24dp;
            contentText = getString(R.string.notification_network_connecting);
        } else if (networkState == NETWORK_STATE.CONNECTION_FAILED) {
            icon = R.drawable.ic_cloud_error_white_24dp;
            contentText = getString(R.string.notification_network_failed);
        } else {
            icon = R.drawable.ic_cloud_white_24dp;
            contentText = getString(R.string.notification_network_connecting);
        }
        Log.d(TAG, "notification " + networkState.name());

        return new NotificationCompat.Builder(DataWriterService.this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setContentIntent(pendingIntent)
                .setOngoing(networkState != NETWORK_STATE.CONNECTION_FAILED).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if (intent == null || intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
            if (!applicationPreferences.writeServer() && !applicationPreferences.writeLocal()){
                stopSelf(); //no need to continue if not saving the data
            }else {
                init();
                registerReceiver();
                startForeground(SharedConstants.NOTIFICATION_ID.DATA_WRITER_SERVICE, getNotification());
            }
        } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)) {
            Log.i(TAG, "Received Stop Service Intent");
            stopService();
        } else if (intent.getAction().equals(SharedConstants.ACTIONS.QUERY_CONNECTION_STATE)) {
            queryConnectionState();
        }

        return START_STICKY;
    }

    /**
     * Queries the network connection state, informing all listening application components.
     */
    private void queryConnectionState(){
        if (networkState == NETWORK_STATE.DISCONNECTED){
            sendMessage(SharedConstants.MESSAGES.SERVER_DISCONNECTED);
            Log.d(TAG, "disconnected");
        } else if (networkState == NETWORK_STATE.CONNECTED){
            sendMessage(SharedConstants.MESSAGES.SERVER_CONNECTION_SUCCEEDED);
            Log.d(TAG, "connected");
        } else if (networkState == NETWORK_STATE.CONNECTION_FAILED){
            sendMessage(SharedConstants.MESSAGES.SERVER_CONNECTION_FAILED);
            Log.d(TAG, "failed");
        }
    }

    /**
     * Stops the data writer background service.
     */
    private void stopService(){
        unregisterReceiver();
        if (applicationPreferences.writeLocal())
            closeAllWriters();

        sendMessage(SharedConstants.MESSAGES.SERVER_DISCONNECTED);

        Log.d(TAG, "stop service");
        stopForeground(true);
        stopSelf();
    }

    /**
     * Closes all file open writers.
     */
    private void closeAllWriters(){
        for (String key : fileWriterHashMap.keySet()){
            fileWriterHashMap.get(key).close();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}