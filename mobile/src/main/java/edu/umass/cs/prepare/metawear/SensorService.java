package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.Locale;

import edu.umass.cs.prepare.communication.local.Broadcaster;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.view.activities.MainActivity;
import edu.umass.cs.shared.preferences.ApplicationPreferences;
import edu.umass.cs.shared.util.BatteryUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.util.SensorBuffer;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.R;

/**
 * The Sensor Service is responsible for streaming sensor data from the
 * <a href="https://mbientlab.com/metawearc/">MetaWear C</a> tag to the phone. The data streams
 * include accelerometer, gyroscope and the received signal strength indicator (RSSI) between
 * the tag and the phone.
 * <br> <hr> <br>
 * To begin collecting sensor data, bind to the {@link SensorService} and start an intent
 * with action {@link edu.umass.cs.shared.constants.SharedConstants.ACTIONS#START_SERVICE},
 * passing in a {@link String} extra with key {@link SharedConstants.KEY#UUID}
 * to identify the Bluetooth device by its address. To stop the service, use action
 * {@link edu.umass.cs.shared.constants.SharedConstants.ACTIONS#STOP_SERVICE}.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SensorService extends edu.umass.cs.shared.metawear.SensorService {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** Responsible for managing the services, e.g. sensor service, on the mobile application **/
    private ServiceManager serviceManager;

    public static int WEARABLE_COLLECTION_DURATION_MILLIS = 3000;

    private enum CONTENT_TEXT_IDENTIFIER {
        COLLECTING_DATA,
        LISTENING_FOR_MOVEMENT,
        BLUETOOTH_DISABLED
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        showForegroundNotification(CONTENT_TEXT_IDENTIFIER.LISTENING_FOR_MOVEMENT);
    }

    @Override
    public void onCreate() {
        setBroadcaster(new Broadcaster(this));
        serviceManager = ServiceManager.getInstance(this);
        setOnBufferFullCallback(accelerometerBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                Broadcaster.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR, timestamps, values);
            }
        });
        setOnBufferFullCallback(gyroscopeBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                Broadcaster.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR, timestamps, values);
            }
        });
        setOnBufferFullCallback(rssiBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                Broadcaster.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.RSSI, timestamps, values);
            }
        });
        super.onCreate();
    }

    @Override
    protected void onMetawearConnected(){
        serviceManager.startSensorService();
        serviceManager.startDataWriterService();
        super.onMetawearConnected();
        showForegroundNotification(CONTENT_TEXT_IDENTIFIER.COLLECTING_DATA);
        queryBatteryLevel();
    }

    @Override
    protected void onMetawearDisconnected() {
        if (disconnectSource == DISCONNECT_SOURCE.BLUETOOTH_DISABLED){
            showForegroundNotification(CONTENT_TEXT_IDENTIFIER.BLUETOOTH_DISABLED);
        }else {
            showForegroundNotification(CONTENT_TEXT_IDENTIFIER.LISTENING_FOR_MOVEMENT);
        }
        super.onMetawearDisconnected();
        if (ApplicationPreferences.getInstance(this).useAndroidWear()) {
            HandlerThread hThread = new HandlerThread("StopWearableSensorServiceThread");
            hThread.start();
            Handler stopServiceHandler = new Handler(hThread.getLooper());
            stopServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    serviceManager.stopSensorService();
                }
            }, WEARABLE_COLLECTION_DURATION_MILLIS); //TODO Cancel on handler if reconnected
        } else {
            serviceManager.stopDataWriterService();
        }
    }

    private void showForegroundNotification(CONTENT_TEXT_IDENTIFIER state){
        Notification notification = getUpdatedNotification(-1, state);
        startForeground(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE, notification);
    }

    private Notification getUpdatedNotification(int batteryLevel, CONTENT_TEXT_IDENTIFIER state){
        Intent queryBatteryLevelIntent = new Intent(this, SensorService.class);
        queryBatteryLevelIntent.setAction(SharedConstants.ACTIONS.QUERY_BATTERY_LEVEL);
        PendingIntent queryBatteryLevelPendingIntent = PendingIntent.getService(this, 0, queryBatteryLevelIntent, 0);

        Intent notificationIntent = new Intent(this, MainActivity.class); //open main activity when user clicks on notification
        notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        // notify the user that the foreground service has started
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentIntent(pendingIntent);


        String contentText;
        if (state == CONTENT_TEXT_IDENTIFIER.COLLECTING_DATA){
            contentText = getString(R.string.connection_notification);
            notificationBuilder = notificationBuilder.setVibrate(new long[]{0, 50, 150, 200});
        }else if (state == CONTENT_TEXT_IDENTIFIER.LISTENING_FOR_MOVEMENT) {
            contentText = getString(R.string.sensor_service_notification);
        }else if (state == CONTENT_TEXT_IDENTIFIER.BLUETOOTH_DISABLED){
            contentText = "Please enable Bluetooth";
        }else{
            contentText = "";
        }
        if (batteryLevel >= 0)
            contentText += String.format(Locale.getDefault(), "(battery: %d%%)", batteryLevel);
        notificationBuilder = notificationBuilder.setContentText(contentText);

        if (batteryLevel >= 0)
            notificationBuilder = notificationBuilder.addAction(BatteryUtil.getBatteryLevelIconId(batteryLevel),
                    "Battery Level", queryBatteryLevelPendingIntent);

        return notificationBuilder.build();
    }

    private void showBatteryLevelNotification(int percentage){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE,
                getUpdatedNotification(percentage, CONTENT_TEXT_IDENTIFIER.COLLECTING_DATA));
    }

    @Override
    protected void onBatteryLevelReceived(int percentage) {
        showBatteryLevelNotification(percentage);
        Broadcaster.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.BATTERY_METAWEAR, new long[]{System.currentTimeMillis()}, new float[]{percentage});
    }

}
