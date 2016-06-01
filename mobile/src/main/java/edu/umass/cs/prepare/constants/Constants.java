package edu.umass.cs.prepare.constants;

/**
 * The Constants class stores various constants that are used across various classes, including
 * identifiers for intent actions used when the main UI communicates with the sensor service; the
 * list of activity labels; the error and warning messages displayed to the user; and so on.
 *
 * @author snoran
 *
 */
public class Constants {

    /** Number of bytes required to represent a timestamp string, used for efficient allocation **/
    public static final int BYTES_PER_TIMESTAMP = 20;

    /** Number of bytes required to represent a float reading, used for efficient allocation **/
    public static final int BYTES_PER_SENSOR_READING = 10;

    /** Intent actions used to communicate between the main UI and the sensor service
     * @see android.content.Intent */
    public interface ACTION {
        String START_SERVICE = "edu.umass.cs.bluedroid.action.start-service";
        String NOTIFY = "edu.umass.cs.bluedroid.action.notify";
        String STOP_SERVICE = "edu.umass.cs.bluedroid.action.stop-service";
        String MINIMIZE_VIDEO = "edu.umass.cs.bluedroid.action.minimize-video";
        String MAXIMIZE_VIDEO = "edu.umass.cs.bluedroid.action.maximize-video";

        String NAVIGATE_TO_APP = "this too!";
        String BROADCAST_SENSOR_DATA = "edu.umass.cs.bluedroid.action.broadcast-sensor-data";
        int REQUEST_SET_PREFERENCES = 5; //TODO: WHERE TO PUT REQUESTS??
    }

    public interface KEY {
        String STATUS = "edu.umass.cs.bluedroid.key.status";
        String ACCELEROMETER_READING = "edu.umass.cs.bluedroid.key.accelerometer-reading";
        String BATTERY_LEVEL = "edu.umass.cs.bluedroid.key.battery-level";
        String SURFACE_WIDTH = "edu.umass.cs.bluedroid.key.surface-width";
        String SURFACE_HEIGHT = "edu.umass.cs.bluedroid.key.surface-height";
        String SURFACE_X = "edu.umass.cs.bluedroid.key.surface-x";
        String SURFACE_Y = "edu.umass.cs.bluedroid.key.surface-y";
        String CANCEL_CONNECTING = "edu.umass.cs.bluedroid.key.cancel-connecting";
        String SENSOR_DATA = "edu.umass.cs.bluedroid.key.sensor-data";
        String SENSOR_TYPE = "edu.umass.cs.bluedroid.key.sensor-type";
        String TIMESTAMP = "edu.umass.cs.bluedroid.key.timestamp";
    }

    public interface MESSAGE {
        int REGISTER_CLIENT = 0;
        int UNREGISTER_CLIENT = 1;
        int SENSOR_STARTED = 2;
        int SENSOR_STOPPED = 3;
        int STATUS = 4;
        int ACCELEROMETER_READING = 5;
        int BATTERY_LEVEL = 6;
        int CONNECTING = 7;
        int CONNECTED = 8;
    }
}
