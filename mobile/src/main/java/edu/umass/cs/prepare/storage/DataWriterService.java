package edu.umass.cs.prepare.storage;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;

import edu.umass.cs.prepare.MHLClient.MHLConnectionStateHandler;
import edu.umass.cs.prepare.MHLClient.MHLMobileIOClient;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLAccelerometerReading;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLGyroscopeReading;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLRSSIReading;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.main.MainActivity;
import edu.umass.cs.prepare.communication.wearable.DataReceiverService;

/**
 * The Data Writer Service is responsible for writing all sensor data to their respective files.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see DataReceiverService
 * @see FileUtil
 * @see Service
 */
public class DataWriterService extends Service {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = DataWriterService.class.getName();

    private BufferedWriter accelerometerWearableWriter;
    private BufferedWriter gyroscopeWearableWriter;
    private BufferedWriter accelerometerMetawearWriter;
    private BufferedWriter gyroscopeMetawearWriter;
    private BufferedWriter rssiMetawearToPhoneWriter;
    private BufferedWriter rssiMetawearToWearableWriter;

    /** Indicates whether data should be written to storage. **/
    private boolean writeLocal = false;

    /** Indicates whether data should be sent to the server. **/
    private boolean writeServer = false;

    /** Client responsible for communicating to the server. **/
    private MHLMobileIOClient client;

    /** The directory where the sensor data is stored. **/
    private File directory;

    public void initializeFileWriters(){
        accelerometerWearableWriter = FileUtil.getFileWriter(DataWriterService.this, "ACCELEROMETER_WEARABLE", directory);
        gyroscopeWearableWriter = FileUtil.getFileWriter(DataWriterService.this, "GYRO_WEARABLE", directory);
        accelerometerMetawearWriter = FileUtil.getFileWriter(DataWriterService.this, "ACCELEROMETER_METAWEAR", directory);
        gyroscopeMetawearWriter = FileUtil.getFileWriter(DataWriterService.this, "GYRO_METAWEAR", directory);
        rssiMetawearToWearableWriter = FileUtil.getFileWriter(DataWriterService.this, "WEARABLE_TO_METAWEAR_RSSI", directory);
        rssiMetawearToPhoneWriter = FileUtil.getFileWriter(DataWriterService.this, "PHONE_TO_METAWEAR_RSSI", directory);
    }

    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String defaultDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name)).getAbsolutePath();
        final String dir = preferences.getString(getString(R.string.pref_directory_key), defaultDirectory);
        if (dir == null)
            directory = new File(defaultDirectory);
        else
            directory = new File(dir);
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

                    StringBuilder builder = new StringBuilder(timestamps.length * (Constants.BYTES_PER_TIMESTAMP + Constants.BYTES_PER_SENSOR_READING + 6));
                    if (sensorType == SharedConstants.SENSOR_TYPE.WEARABLE_TO_METAWEAR_RSSI ||
                            sensorType == SharedConstants.SENSOR_TYPE.PHONE_TO_METAWEAR_RSSI){

                        for (int i = 0; i < timestamps.length; i++) {
                            long timestamp = timestamps[i];
                            int rssi = (int)values[i];
                            if (writeLocal)
                                builder.append(String.format("%s,%d\n", timestamp, rssi));
                            if (writeServer) {
                                client.addSensorReading(new MHLRSSIReading(0, "Metawear", timestamp, rssi));
                                //we must wait briefly after adding to the queue, otherwise subsequent data will not be received
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ignored) {}
                            }
                        }
                    }else {

                        for (int i = 0; i < timestamps.length; i++) {
                            long timestamp = timestamps[i];
                            float x = values[3 * i];
                            float y = values[3 * i + 1];
                            float z = values[3 * i + 2];

                            if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR){
                                x *= SharedConstants.GRAVITY;
                                y *= SharedConstants.GRAVITY;
                                z *= SharedConstants.GRAVITY;
                            }

                            if (writeLocal)
                                builder.append(String.format("%s,%f,%f,%f\n", timestamp, x, y, z));

                            if (writeServer) {
                                if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR) {
                                    client.addSensorReading(new MHLAccelerometerReading(0, "METAWEAR", timestamp, x, y, z));
                                } else if (sensorType == SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR) {
                                    client.addSensorReading(new MHLGyroscopeReading(0, "METAWEAR", timestamp, x, y, z));
                                } else if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_WEARABLE) {
                                    client.addSensorReading(new MHLAccelerometerReading(0, "WEARABLE", System.currentTimeMillis(), x, y, z)); //TODO: Get event timestamp from wearable, not in nanoseconds since boot
                                } else if (sensorType == SharedConstants.SENSOR_TYPE.GYROSCOPE_WEARABLE) {
                                    client.addSensorReading(new MHLGyroscopeReading(0, "WEARABLE", System.currentTimeMillis(), x, y, z));
                                }
                                //we must wait briefly after adding to the queue, otherwise subsequent data will not be received
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ignored) {} //TODO: Should I be doing this on main UI thread?
                            }
                        }
                    }

                    if (!writeLocal) return;

                    String line = builder.toString();
                    if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_WEARABLE) {
                        synchronized (accelerometerWearableWriter) {
                            FileUtil.writeToFile(line, accelerometerWearableWriter);
                        }
                    } else if (sensorType == SharedConstants.SENSOR_TYPE.GYROSCOPE_WEARABLE){
                        synchronized (gyroscopeWearableWriter) {
                            FileUtil.writeToFile(line, gyroscopeWearableWriter);
                        }
                    } else if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR){
                        synchronized (accelerometerMetawearWriter) {
                            Log.d(TAG, line);
                            FileUtil.writeToFile(line, accelerometerMetawearWriter);
                        }
                    } else if (sensorType == SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR){
                        synchronized (gyroscopeMetawearWriter) {
                            FileUtil.writeToFile(line, gyroscopeMetawearWriter);
                        }
                    } else if (sensorType == SharedConstants.SENSOR_TYPE.WEARABLE_TO_METAWEAR_RSSI){
                        synchronized (rssiMetawearToWearableWriter) {
                            FileUtil.writeToFile(line, rssiMetawearToWearableWriter);
                        }
                    } else if (sensorType == SharedConstants.SENSOR_TYPE.PHONE_TO_METAWEAR_RSSI){
                        synchronized (rssiMetawearToPhoneWriter) {
                            FileUtil.writeToFile(line, rssiMetawearToPhoneWriter);
                        }
                    }
                }
            }
        }
    };

    private void init(){
        loadPreferences();
        if (writeLocal)
            initializeFileWriters();
        if (writeServer) {
            client = new MHLMobileIOClient(SharedConstants.SERVER_IP_ADDRESS, SharedConstants.SERVER_PORT, 0);
            client.setConnectionStateHandler(new MHLConnectionStateHandler() {
                @Override
                public void onConnected() {

                }

                @Override
                public void onConnectionFailed() {
                    // if the connection fails, then write locally instead
//                    if (!writeLocal) {
//                        initializeFileWriters();
//                        writeLocal = true;
//                    }
                    writeServer = false;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if (intent == null) return START_STICKY;
        if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
            init();
            registerReceiver();

            Intent notificationIntent = new Intent(DataWriterService.this, MainActivity.class); //open main activity when user clicks on notification
            notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(DataWriterService.this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(DataWriterService.this)
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText(getString(R.string.data_writer_notification))
                    .setSmallIcon(android.R.drawable.ic_menu_save)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).build();

            startForeground(SharedConstants.NOTIFICATION_ID.DATA_WRITER_SERVICE, notification);
        } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)) {
            Log.i(TAG, "Received Stop Service Intent");

            unregisterReceiver();
            if (writeLocal)
                closeAllWriters();

            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    /**
     * Closes all file open writers.
     */
    private void closeAllWriters(){
        if (accelerometerWearableWriter != null)
            FileUtil.closeWriter(accelerometerWearableWriter);
        if (accelerometerWearableWriter != null)
            FileUtil.closeWriter(gyroscopeWearableWriter);
        if (accelerometerWearableWriter != null)
            FileUtil.closeWriter(accelerometerMetawearWriter);
        if (accelerometerWearableWriter != null)
            FileUtil.closeWriter(gyroscopeMetawearWriter);
        if (accelerometerWearableWriter != null)
            FileUtil.closeWriter(rssiMetawearToPhoneWriter);
        if (accelerometerWearableWriter != null)
            FileUtil.closeWriter(rssiMetawearToWearableWriter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}