package edu.umass.cs.prepare.communication.local;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

import edu.umass.cs.prepare.communication.wearable.RemoteSensorManager;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.metawear.SensorService;
import edu.umass.cs.prepare.recording.RecordingService;
import edu.umass.cs.prepare.storage.DataWriterService;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;

/**
 * The service manager maintains the application services on the mobile device. These include
 * the {@link SensorService}, {@link RecordingService} and
 * {@link DataWriterService}.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see Service
 */
public class ServiceManager {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = ServiceManager.class.getName();

    /** Singleton instance of the remote sensor manager */
    private static ServiceManager instance;

    /** The application context is used to bind the service manager to the application. **/
    private final Context context;

    /** Returns the singleton instance of the remote sensor manager, instantiating it if necessary. */
    public static synchronized ServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceManager(context.getApplicationContext());
        }

        return instance;
    }

    private ServiceManager(Context context) {
        this.context = context;
        remoteSensorManager = RemoteSensorManager.getInstance(context);
        applicationPreferences = ApplicationPreferences.getInstance(context);
    }

    private final RemoteSensorManager remoteSensorManager;

    private final ApplicationPreferences applicationPreferences;

    /**
     * Starts the {@link RecordingService} via an {@link Intent}
     */
    public void startRecordingService(int x, int y, int width, int height, boolean recordAudio){
        Intent startServiceIntent = new Intent(context, RecordingService.class);

        startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);

        startServiceIntent.putExtra(Constants.KEY.SURFACE_WIDTH, width);
        startServiceIntent.putExtra(Constants.KEY.SURFACE_HEIGHT, height);
        startServiceIntent.putExtra(Constants.KEY.SURFACE_X, x);
        startServiceIntent.putExtra(Constants.KEY.SURFACE_Y, y);
        startServiceIntent.putExtra(Constants.KEY.RECORD_AUDIO, recordAudio);

        context.startService(startServiceIntent);
    }

    /**
     * Stops the {@link RecordingService} via an {@link Intent}
     */
    public void stopRecordingService(){
        Intent stopServiceIntent = new Intent(context, RecordingService.class);
        stopServiceIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        context.startService(stopServiceIntent);

    }

    /**
     * Minimizes the video recording display surface.
     */
    public void minimizeVideo(){
        Intent minimizeIntent = new Intent(context, RecordingService.class);
        minimizeIntent.setAction(Constants.ACTION.MINIMIZE_VIDEO);
        context.startService(minimizeIntent);

    }

    /**
     * Maximizes the video recording display surface.
     */
    public void maximizeVideo(){
        Intent maximizeIntent = new Intent(context, RecordingService.class);
        maximizeIntent.setAction(Constants.ACTION.MAXIMIZE_VIDEO);
        context.startService(maximizeIntent);
    }

    /**
     * Sets up a reminder in the case that the camera runs too long in the background (user may have forgotten!)
     */
    public void enableCameraReminder(){
        Intent cameraReminderIntent = new Intent(context, RecordingService.class);
        cameraReminderIntent.setAction(Constants.ACTION.SET_CAMERA_REMINDER);
        context.startService(cameraReminderIntent);
    }

    /**
     * Starts the data writer service on the mobile device.
     */
    public void startDataWriterService(){
        Intent startIntent = new Intent(context, DataWriterService.class);
        startIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
        context.startService(startIntent);
    }

    /**
     * Stops the data writer service on the mobile device.
     */
    public void stopDataWriterService(){
        Log.d(TAG, "stop data writer service");
        Intent startIntent = new Intent(context, DataWriterService.class);
        startIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        context.startService(startIntent);
    }

    /**
     * Starts the Metawear service on the mobile device.
     */
    public void startMetawearService(){
        if (!applicationPreferences.enablePillBottle()) return;
//        if (applicationPreferences.useAndroidWear()){
//            remoteSensorManager.startMetawearService();
//        }else {
            Intent startServiceIntent = new Intent(context, SensorService.class);
            startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
            context.startService(startServiceIntent);
//        }
    }

    /**
     * Stops the Metawear service on the mobile device.
     */
    public void stopMetawearService(){
//        if (applicationPreferences.useAndroidWear()){
//            remoteSensorManager.stopMetawearService();
//        }else {
            Intent startServiceIntent = new Intent(context, SensorService.class);
            startServiceIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
            context.startService(startServiceIntent);
//        }
    }

    /**
     * Starts the sensor service on the wearable device.
     */
    public void startSensorService(){
        if (applicationPreferences.useAndroidWear())
            remoteSensorManager.startSensorService();
    }

    /**
     * Stops the sensor service on the wearable device.
     */
    public void stopSensorService(){
        remoteSensorManager.stopSensorService();
    }

    /**
     * Returns whether the given service is running
     * @param serviceClass a reference to a service class
     * @return true if the service is running, false otherwise
     */
    public boolean isServiceRunning(Class<? extends Service> serviceClass){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void queryMetawearState(){
//        if (applicationPreferences.useAndroidWear()){
////            remoteSensorManager.queryConnectionState(); //TODO
//        }else {
            Intent startServiceIntent = new Intent(context, SensorService.class);
            startServiceIntent.setAction(SharedConstants.ACTIONS.QUERY_CONNECTION_STATE);
            context.startService(startServiceIntent);
//        }
    }

    public void queryWearableState(){
        if (applicationPreferences.useAndroidWear())
            remoteSensorManager.queryWearableState();
    }

    public void queryNetworkState(){
        Intent startServiceIntent = new Intent(context, DataWriterService.class);
        startServiceIntent.setAction(SharedConstants.ACTIONS.QUERY_CONNECTION_STATE);
        context.startService(startServiceIntent);
    }
}
