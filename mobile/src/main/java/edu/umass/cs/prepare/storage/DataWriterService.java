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
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;

import edu.umass.cs.prepare.MHLClient.MHLMobileIOClient;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLAccelerometerReading;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLGyroscopeReading;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLRSSIReading;
import edu.umass.cs.shared.DataLayerUtil;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.main.MainActivity;
import edu.umass.cs.prepare.metawear.DataReceiverService;

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

    /** The IP address of the server where the data should be sent. **/
    private static final String SERVER_IP_ADDRESS = "192.168.25.150";

    /** The port for the server where the data should be sent. **/
    private static final int SERVER_PORT = 9999;

    private BufferedWriter accelerometerWearableWriter;
    private BufferedWriter gyroscopeWearableWriter;
    private BufferedWriter accelerometerMetawearWriter;
    private BufferedWriter gyroscopeMetawearWriter;
    private BufferedWriter rssiMetawearToPhoneWriter;
    private BufferedWriter rssiMetawearToWearableWriter;

    /** Indicates whether data should be written to storage. **/
    private boolean writeLocal = true;

    /** Indicates whether data should be sent to the server. **/
    private boolean writeServer = true;

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
        writeLocal = true;
        writeServer = false;
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
                                client.sendSensorReading(new MHLRSSIReading(0, "Metawear", timestamp, rssi));
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

                            if (writeLocal)
                                builder.append(String.format("%s,%f,%f,%f\n", timestamp, x, y, z));

                            if (writeServer) {
                                if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR) {
                                    client.sendSensorReading(new MHLAccelerometerReading(0, "Metawear", timestamp, x, y, z));
                                } else if (sensorType == SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR) {
                                    client.sendSensorReading(new MHLGyroscopeReading(0, "Metawear", timestamp, x, y, z));
                                } else if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_WEARABLE) {
                                    client.sendSensorReading(new MHLAccelerometerReading(0, "Android-Wear", timestamp, x, y, z));
                                } else if (sensorType == SharedConstants.SENSOR_TYPE.GYROSCOPE_WEARABLE) {
                                    client.sendSensorReading(new MHLGyroscopeReading(0, "Android-Wear", timestamp, x, y, z));
                                }
                                //we must wait briefly after adding to the queue, otherwise subsequent data will not be received
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ignored) {}
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

    @Override
    public void onCreate(){
        loadPreferences();
        //TODO: In onCreate or when started?
        if (writeLocal)
            initializeFileWriters();
        if (writeServer) {
            client = new MHLMobileIOClient(SERVER_IP_ADDRESS, SERVER_PORT);
            client.connect();
        }
    }

    /**
     * Registers a receiver with an intent filter for message
     * {@link edu.umass.cs.prepare.constants.Constants.ACTION#BROADCAST_SENSOR_DATA}, i.e.
     * the service listens for sensor data sent from another application component.
     */
    private void registerReceiver(){
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if (intent == null) return START_STICKY;
        if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
            registerReceiver();

            Intent notificationIntent = new Intent(this, MainActivity.class); //open main activity when user clicks on notification
            notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Intent stopIntent = new Intent(this, DataWriterService.class);
            stopIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_text))
                    .setSmallIcon(android.R.drawable.ic_menu_save)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MAX) //otherwise buttons will not show up!
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_delete, getString(R.string.stop_service), stopPendingIntent).build();

            startForeground(SharedConstants.NOTIFICATION_ID.DATA_WRITER_SERVICE, notification);
        } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)) {
            Log.i(TAG, "Received Stop Service Intent");

            try {
                unregisterReceiver(receiver);
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }
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