package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import edu.umass.cs.prepare.communication.local.Broadcaster;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.main.MainActivity;
import edu.umass.cs.shared.BatteryUtil;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.shared.SensorBuffer;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.R;

/**
 * The Sensor Service is responsible for streaming sensor data from the
 * <a href="https://mbientlab.com/metawearc/">MetaWear C</a> tag to the phone. The data streams
 * include accelerometer, gyroscope and the received signal strength indicator (RSSI) between
 * the tag and the phone.
 * <br> <hr> <br>
 * To begin collecting sensor data, bind to the {@link SensorService} and start an intent
 * with action {@link edu.umass.cs.prepare.constants.Constants.ACTION#START_SERVICE},
 * passing in a {@link String} extra with key {@link SharedConstants.KEY#UUID}
 * to identify the Bluetooth device by its address. To stop the service, use action
 * {@link edu.umass.cs.prepare.constants.Constants.ACTION#STOP_SERVICE}.
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)){
                showForegroundNotification(false);
            }
        return super.onStartCommand(intent, flags, startId);
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
                Broadcaster.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.PHONE_TO_METAWEAR_RSSI, timestamps, values);
            }
        });
        super.onCreate();
    }

    @Override
    protected void onMetawearConnected(){
        serviceManager.startDataWriterService();
        super.onMetawearConnected();
        showForegroundNotification(true);
        queryBatteryLevel();
    }

    @Override
    protected void onMetawearDisconnected() {
        super.onMetawearDisconnected();
        serviceManager.stopDataWriterService();
        showForegroundNotification(false);
    }

    private void showForegroundNotification(boolean connected){
        Notification notification = getUpdatedNotification(-1, connected);
        startForeground(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE, notification);
    }

    private Notification getUpdatedNotification(int batteryLevel, boolean connected){
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
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 50, 150, 200});

        String contentText;
        if (connected){
            contentText = getString(R.string.connection_notification);
        }else {
            contentText = getString(R.string.sensor_service_notification);
        }
        if (batteryLevel >= 0)
            contentText += String.format("(battery: %d%%)", batteryLevel);
        notificationBuilder = notificationBuilder.setContentText(contentText);

        if (batteryLevel >= 0)
            notificationBuilder = notificationBuilder.addAction(BatteryUtil.getBatteryLevelIconId(batteryLevel),
                    "Battery Level", queryBatteryLevelPendingIntent);

        return notificationBuilder.build();
    }

    private void showBatteryLevelNotification(int percentage){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE, getUpdatedNotification(percentage, true));
    }

    @Override
    protected void onBatteryLevelReceived(int percentage) {
        showBatteryLevelNotification(percentage);
    }

}
