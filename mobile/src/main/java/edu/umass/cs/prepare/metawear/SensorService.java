package edu.umass.cs.prepare.metawear;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

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

    /** Messenger used by clients */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /** List of bound clients/activities to this service */
    private final ArrayList<Messenger> mClients = new ArrayList<>();

    private ServiceManager serviceManager;

    @Override
    public void onCreate() {
        serviceManager = ServiceManager.getInstance(this);
        super.onCreate();
    }

    /** Handler to handle incoming messages **/
    private static class IncomingHandler extends Handler {
        private final WeakReference<SensorService> mService;

        IncomingHandler(SensorService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.REGISTER_CLIENT:
                    mService.get().mClients.add(msg.replyTo);
                    break;
                case Constants.MESSAGE.UNREGISTER_CLIENT:
                    mService.get().mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onMetawearConnected(){
        showForegroundNotification();
        setOnBufferFullCallback(accelerometerBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                DataReceiverService.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR, timestamps, values);
            }
        });
        setOnBufferFullCallback(gyroscopeBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                DataReceiverService.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.GYROSCOPE_METAWEAR, timestamps, values);
            }
        });
        setOnBufferFullCallback(rssiBuffer, new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                DataReceiverService.broadcastSensorData(SensorService.this, SharedConstants.SENSOR_TYPE.PHONE_TO_METAWEAR_RSSI, timestamps, values);
            }
        });
        queryBatteryLevel();
        sendMessageToClients(Constants.MESSAGE.CONNECTED);
        serviceManager.startDataWriterService();
        serviceManager.startRecordingService();
        super.onMetawearConnected();
    }

    private void showForegroundNotification(){
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

        Intent notificationIntent = new Intent(this, MainActivity.class); //open main activity when user clicks on notification
        notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        // notify the user that the foreground service has started
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 50, 150, 200})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(android.R.drawable.ic_delete, getString(R.string.stop_service), stopPendingIntent);

        if (batteryLevel >= 0)
            notificationBuilder = notificationBuilder.addAction(BatteryUtil.getBatteryLevelIconId(batteryLevel),
                    "Battery Level", queryBatteryLevelPendingIntent);

        return notificationBuilder.build();
    }

    private void showBatteryLowNotification(int percentage){
        //TODO
    }

    @Override
    protected void onBatteryLevelReceived(int percentage){
        if (percentage < 10)
            showBatteryLowNotification(percentage);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        sendMessageToClients(Constants.MESSAGE.CONNECTING);
        super.onServiceConnected(name, service);
    }

    @Override
    protected void onRSSIReadingReceived(long timestamp, int rssi) {
        super.onRSSIReadingReceived(timestamp, rssi);
    }

    @Override
    protected void onGyroscopeReadingReceived(long timestamp, float x, float y, float z) {
        super.onGyroscopeReadingReceived(timestamp, x, y, z);
    }

    @Override
    protected void onAccelerometerReadingReceived(long timestamp, float x, float y, float z) {
        sendAccelerometerReadingToClients(timestamp, x, y, z);
        super.onAccelerometerReadingReceived(timestamp, x, y, z);
    }

    private void sendAccelerometerReadingToClients(long timestamp, float x, float y, float z){
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send message value
                Bundle b = new Bundle();
                b.putLong(Constants.KEY.TIMESTAMP, timestamp);
                b.putFloatArray(Constants.KEY.ACCELEROMETER_READING, new float[]{x, y, z});
                Message msg = Message.obtain(null, Constants.MESSAGE.ACCELEROMETER_READING);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     * Sends the specified message to attached clients
     */
    private void sendMessageToClients(int message) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send message value
                Bundle b = new Bundle();
                Message msg = Message.obtain(null, message);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

}
