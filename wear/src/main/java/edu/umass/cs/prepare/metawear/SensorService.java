package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import edu.umass.cs.prepare.PermissionsActivity;
import edu.umass.cs.prepare.R;
import edu.umass.cs.shared.BatteryUtil;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.shared.SensorBuffer;
import edu.umass.cs.prepare.DataClient;

/**
 * The Sensor Service is responsible for streaming sensor data from the Metawear tag to the
 * mobile device via the wearable device. Available sensors include the accelerometer,
 * gyroscope and the Metawear-to-wearable received signal strength indicator (RSSI).
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SensorService extends edu.umass.cs.shared.metawear.SensorService {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** used to communicate with the handheld application */
    private DataClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        client = DataClient.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (intent.getAction().equals(SharedConstants.ACTIONS.QUERY_BATTERY_LEVEL)){
                queryBatteryLevel();
            }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onMetawearConnected(){
        startForeground();
        client.sendMessage(SharedConstants.MESSAGES.METAWEAR_CONNECTED);
        setOnBufferFullCallback(accelerometerBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(String[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR, timestamps.clone(), values.clone());
            }
        });
        setOnBufferFullCallback(gyroscopeBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(String[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR, timestamps.clone(), values.clone());
            }
        });
        setOnBufferFullCallback(rssiBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(String[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.WEARABLE_TO_METAWEAR_RSSI, timestamps.clone(), values.clone());
            }
        });
        queryBatteryLevel();
        super.onMetawearConnected();
    }

    @Override
    protected void onRSSIReadingReceived(String timestamp, int rssi){
        //Log.d(TAG, String.valueOf(rssi));
    }

    private void startForeground(){
        Notification notification = getUpdatedNotification(-1);
        startForeground(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE, notification);
    }

    private Notification getUpdatedNotification(int batteryLevel){
        Intent stopIntent = new Intent(this, SensorService.class);
        stopIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        Intent queryBatteryLevelIntent = new Intent(this, SensorService.class);
        queryBatteryLevelIntent.setAction(SharedConstants.ACTIONS.QUERY_BATTERY_LEVEL);
        PendingIntent queryBatteryLevelPendingIntent = PendingIntent.getService(this, 0, queryBatteryLevelIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pill);

        // notify the user that the foreground service has started
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_pill)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setOngoing(true)
                .setVibrate(new long[]{0, 50, 150, 200})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(android.R.drawable.ic_delete, getString(R.string.stop_service), stopPendingIntent);

        if (batteryLevel > 0)
                notificationBuilder = notificationBuilder.addAction(BatteryUtil.getBatteryLevelIconId(batteryLevel),
                        SharedConstants.ACTIONS.QUERY_BATTERY_LEVEL, queryBatteryLevelPendingIntent);

        return notificationBuilder.build();
    }

    @Override
    protected void onBatteryLevelReceived(int percentage){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(SharedConstants.NOTIFICATION_ID.METAWEAR_SENSOR_SERVICE, getUpdatedNotification(percentage));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        client.sendMessage(SharedConstants.MESSAGES.METAWEAR_CONNECTING);
        super.onServiceConnected(name, service);
    }
}
