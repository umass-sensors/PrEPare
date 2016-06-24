package edu.umass.cs.prepare.sensors;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.DataClient;
import edu.umass.cs.shared.SensorBuffer;
import edu.umass.cs.shared.SharedConstants;

/**
 * The wearable sensor service is responsible for collecting sensor data on the wearable device.
 * Sensors available through this service include accelerometer and gyroscope sensors.
 * It is an ongoing service that is initiated and terminated through {@link Intent} actions.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see DataClient
 * @see Service
 * @see Sensor
 * @see SensorEventListener
 */
public class SensorService extends Service implements SensorEventListener {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorService.class.getName();

    /** Sensor Manager object for registering and unregistering system sensors */
    private SensorManager mSensorManager;

    /** device accelerometer sensor */
    private Sensor accelerometer;

    /** device gyroscope sensor */
    private Sensor gyroscope;

    /** used to communicate with the handheld application */
    private DataClient client;

    /** Buffer size */
    private static final int BUFFER_SIZE = 1;

    /** The buffer containing the accelerometer readings **/
    private final SensorBuffer accelerometerBuffer = new SensorBuffer(BUFFER_SIZE, 3);

    /** The buffer containing the gyroscope readings **/
    private final SensorBuffer gyroscopeBuffer = new SensorBuffer(BUFFER_SIZE, 3);

    @Override
    public void onCreate() {
        super.onCreate();
        client = DataClient.getInstance(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
                Log.d(TAG, "Started sensor service.");
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pill);

                //notify the user that the application has started
                Notification notification = new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.app_name))
                        .setTicker(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_text))
                        .setSmallIcon(R.drawable.ic_pill)
                        .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                        .setOngoing(true)
                        .setVibrate(new long[]{0, 50, 100, 150, 200})
                        .setPriority(Notification.PRIORITY_MAX)
                        .build();

                startForeground(SharedConstants.NOTIFICATION_ID.WEARABLE_SENSOR_SERVICE, notification); //id is arbitrary, so we choose id=1

                registerSensors();
            } else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE)) {
                unregisterSensors();
                stopForeground(true);
                stopSelf();
            }
        }

        return START_STICKY;
    }

    /**
     * register accelerometer and gyroscope sensor listeners and initialize respective buffers
     */
    private void registerSensors(){
        accelerometerBuffer.setOnBufferFullCallback(new SensorBuffer.OnBufferFullCallback() {
            @Override
            public void onBufferFull(long[] timestamps, float[] values) {
                client.sendSensorData(SharedConstants.SENSOR_TYPE.ACCELEROMETER_WEARABLE, timestamps.clone(), values.clone());
            }
        });
//        gyroscopeBuffer.setOnBufferFullCallback(new SensorBuffer.OnBufferFullCallback() {
//            @Override
//            public void onBufferFull(long[] timestamps, float[] values) {
//                client.sendSensorData(SharedConstants.SENSOR_TYPE.GYROSCOPE_WEARABLE, timestamps.clone(), values.clone());
//            }
//        });

        //get handles to the hardware sensors
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (mSensorManager == null){
            Log.e(TAG, "ERROR: Could not retrieve sensor manager...");
            return;
        }

//        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.w(TAG, "No Accelerometer found");
        }

//        if (gyroscope != null) {
//            mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
//        } else {
//            Log.w(TAG, "No gyroscope found");
//        }
    }

    /**
     * unregister the sensor listeners, this is important for the battery life!
     */
    private void unregisterSensors() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, accelerometer);
//            mSensorManager.unregisterListener(this, gyroscope);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //TODO: When the service is ended, the remaining data is not saved because it does not fill buffer
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            synchronized (accelerometerBuffer) { //add sensor data to the appropriate buffer
                accelerometerBuffer.addReading(event.timestamp, event.values);
            }
        }
//        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            synchronized (gyroscopeBuffer) {
//                gyroscopeBuffer.addReading(event.timestamp, event.values);
//            }
//        }
        else{
            Log.w(TAG, "Sensor Not Supported!");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed: " + accuracy);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
