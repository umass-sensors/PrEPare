package edu.umass.cs.prepare.metawear;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.recording.RecordingService;
import edu.umass.cs.prepare.storage.DataWriterService;
import edu.umass.cs.shared.SharedConstants;

/**
 * The service manager maintains the application services on the mobile device. These include
 * the {@link BeaconService}, {@link SensorService}, {@link RecordingService} and
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

    /** singleton instance of the remote sensor manager */
    private static ServiceManager instance;

    private final Context context;

    /** return singleton instance of the remote sensor manager, instantiating if necessary */
    public static synchronized ServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceManager(context.getApplicationContext());
        }

        return instance;
    }

    private ServiceManager(Context context) {
        this.context = context;
    }

    /**
     * Starts the {@link RecordingService} via an {@link Intent}
     */
    public void startRecordingService(){

        Log.d(TAG, "start recording service");

        Intent startServiceIntent = new Intent(context, RecordingService.class);

        //identify the intent by the START_SERVICE action, defined in the Constants class
        startServiceIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);

        int[] position = new int[2];
        //mSurfaceView.getLocationInWindow(position);
        position[0] = 0;
        position[1] = 0;
        startServiceIntent.putExtra(Constants.KEY.SURFACE_WIDTH, 100); //mSurfaceView.getWidth());
        startServiceIntent.putExtra(Constants.KEY.SURFACE_HEIGHT, 100); //mSurfaceView.getHeight());
        startServiceIntent.putExtra(Constants.KEY.SURFACE_X, position[0]);
        startServiceIntent.putExtra(Constants.KEY.SURFACE_Y, position[1]);

        //start sensor service
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
     * Starts the Beacon scanning service on the mobile device.
     */
    public void startLocalBeaconService(){
        Intent startIntent = new Intent(context, BeaconService.class);
        startIntent.setAction(SharedConstants.ACTIONS.START_SERVICE);
        context.startService(startIntent);
    }

    /**
     * Stops the Beacon service on the mobile device.
     */
    public void stopLocalBeaconService(){
        Intent startIntent = new Intent(context, BeaconService.class);
        startIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        context.startService(startIntent);
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
        Intent startIntent = new Intent(context, DataWriterService.class);
        startIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        context.startService(startIntent);
    }

    /**
     * Stops the Metawear service on the mobile device.
     */
    public void stopMetawearService(){
        Intent startServiceIntent = new Intent(context, SensorService.class);
        startServiceIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        context.startService(startServiceIntent);
    }
}
