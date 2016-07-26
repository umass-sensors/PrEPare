package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.Locale;

import edu.umass.cs.prepare.communication.Broadcaster;
import edu.umass.cs.prepare.communication.DataClient;
import edu.umass.cs.shared.util.BatteryUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.util.SensorBuffer;
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

    /** used to communicate with the handheld application */
    private DataClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        setBroadcaster(new Broadcaster(this));
        client = DataClient.getInstance(this);
        setOnBufferFullCallback(accelerometerBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR, timestamps.clone(), values.clone());
            }
        });
        setOnBufferFullCallback(gyroscopeBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR, timestamps.clone(), values.clone());
            }
        });
        setOnBufferFullCallback(rssiBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.RSSI, timestamps.clone(), values.clone());
            }
        });
    }

    @Override
    protected void onMetawearConnected(){
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
    }

    private void showForegroundNotification(CONTENT_TEXT_IDENTIFIER state){
        Notification notification = getUpdatedNotification(-1, state);
        startForeground(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE, notification);
    }

    /**
     * Returns the notification displayed during background data collection.
     * @param batteryLevel The current battery level
     * @return the notification handle
     */
    private Notification getUpdatedNotification(int batteryLevel, CONTENT_TEXT_IDENTIFIER state){
        Intent queryBatteryLevelIntent = new Intent(this, SensorService.class);
        queryBatteryLevelIntent.setAction(SharedConstants.ACTIONS.QUERY_BATTERY_LEVEL);
        PendingIntent queryBatteryLevelPendingIntent = PendingIntent.getService(this, 0, queryBatteryLevelIntent, 0);

        // notify the user that the foreground service has started
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true);

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
        client.sendSensorData(SharedConstants.SENSOR_TYPE.RSSI, new long[]{System.currentTimeMillis()}, new float[]{percentage});
    }

}

