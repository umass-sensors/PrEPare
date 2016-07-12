package edu.umass.cs.shared.constants;

import android.content.Intent;

/**
 * This file contains constants that are shared between the handheld and wearable applications,
 * such as data layer communications tags and shared service commands.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SharedConstants {

    public static final float GRAVITY = 9.8f;

    /**
     * Tags that define the type of communication between the handheld and wearable application.
     * These are used as keys to identify data packages sent over Bluetooth to the mobile device.
     * @see SharedConstants.KEY
     */
    public interface DATA_LAYER_CONSTANTS {
        String SENSOR_PATH = "/sensors/";
        String MESSAGE_PATH = "/message/";
    }

    /**
     * Commands sent from the handheld application to the wearable application.
     */
    public interface COMMANDS {
        String START_SENSOR_SERVICE = "edu.umass.cs.prepare.commands.start-sensor-service";
        String STOP_SENSOR_SERVICE = "edu.umass.cs.prepare.commands.stop-sensor-service";
        String START_METAWEAR_SERVICE = "edu.umass.cs.prepare.commands.start-metawear-service";
        String STOP_METAWEAR_SERVICE = "edu.umass.cs.prepare.commands.stop-metawear-service";
        String START_BEACON_SERVICE = "edu.umass.cs.prepare.commands.start-beacon-service";
        String STOP_BEACON_SERVICE = "edu.umass.cs.prepare.commands.stop-beacon-service";
    }

    public interface MESSAGES {
        int BEACON_WITHIN_RANGE = 0;
        int METAWEAR_CONNECTED = 1;
        int METAWEAR_CONNECTING = 2;
        int METAWEAR_DISCONNECTED = 3;
        int BEACON_SERVICE_STARTED = 4;
        int BEACON_SERVICE_STOPPED = 5;
        int RECORDING_SERVICE_STARTED = 6;
        int RECORDING_SERVICE_STOPPED = 7;
        int INVALID_ADDRESS = 8;
        int BLUETOOTH_DISABLED = 9;
    }

    /**
     * Actions sent to bound services via an {@link Intent} handle.
     */
    public interface ACTIONS {
        String START_SERVICE = "edu.umass.cs.prepare.action.start-service";
        String STOP_SERVICE = "edu.umass.cs.prepare.action.stop-service";
        String QUERY_BATTERY_LEVEL = "edu.umass.cs.prepare.action.query-battery-level";
    }

    /**
     * Keys used to identify data instances from data packages sent over Bluetooth to the mobile device.
     * @see SharedConstants.DATA_LAYER_CONSTANTS
     */
    public interface KEY {
        String UUID = "edu.umass.cs.prepare.key.uuid";
        String TIMESTAMPS = "edu.umass.cs.prepare.key.timestamps";
        String SENSOR_VALUES = "edu.umass.cs.prepare.key.sensor-values";
        String SENSOR_TYPE = "edu.umass.cs.prepare.key.sensor-type";
        String MESSAGE = "edu.umass.cs.prepare.key.message";
    }

    /**
     * The sensor type identifies the sensing modality of the associated data stream sent from
     * the wearable device to the mobile device. This may be motion data from the wearable itself
     * or from any other connected device, e.g. a Metawear tag, connected to the wearable.
     */
    public enum SENSOR_TYPE {
        ACCELEROMETER_WEARABLE,
        GYROSCOPE_WEARABLE,
        ACCELEROMETER_METAWEAR,
        GYROSCOPE_METAWEAR,
        WEARABLE_TO_METAWEAR_RSSI,
        PHONE_TO_METAWEAR_RSSI,
        BATTERY_METAWEAR,
        UNKNOWN
    }

    public interface METAWEAR_STREAM_KEY{
        String ACCELEROMETER = "edu.umass.cs.prepare.metawear-accelerometer-stream";
        String NO_MOTION = "edu.umass.cs.prepare.metawear-no-motion-stream";
        String GYROSCOPE = "edu.umass.cs.prepare.metawear-gyroscope-stream";
    }

    public interface NOTIFICATION_ID {
        int WEARABLE_SENSOR_SERVICE = 101;
        int METAWEAR_SENSOR_SERVICE = 102;
        int DATA_WRITER_SERVICE = 103;
        int RECORDING_SERVICE = 104;
    }

    /** The IP address of the server where the data should be sent. **/
    public static final String SERVER_IP_ADDRESS = "192.168.25.150"; //"192.168.25.150"; //"192.168.26.217"

    /** The port for the server where the data should be sent. **/
    public static final int SERVER_PORT = 9999;
}
