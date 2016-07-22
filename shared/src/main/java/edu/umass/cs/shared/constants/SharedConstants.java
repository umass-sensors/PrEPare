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
        int BLUETOOTH_UNSUPPORTED = 10;
        int NO_MOTION_DETECTED = 11;
        int METAWEAR_SERVICE_STOPPED = 12;
        int SERVER_CONNECTION_FAILED = 13;
        int WEARABLE_SERVICE_STARTED = 14;
        int WEARABLE_SERVICE_STOPPED = 15;
        int WEARABLE_CONNECTED = 16;
        int WEARABLE_DISCONNECTED = 17;
        int SERVER_CONNECTION_SUCCEEDED = 18;
        int SERVER_DISCONNECTED = 19;
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
        ACCELEROMETER_WEARABLE(true) {
            @Override
            public String getSensor(){
                return  SENSOR.ACCELEROMETER.TITLE;
            }
            @Override
            public String getDevice(){
                return  DEVICE.WEARABLE.TITLE;
            }
        },
        GYROSCOPE_WEARABLE(true) {
            @Override
            public String getSensor(){
                return SENSOR.GYROSCOPE.TITLE;
            }
            @Override
            public String getDevice(){
                return DEVICE.WEARABLE.TITLE;
            }
        },
        ACCELEROMETER_METAWEAR(true) {
            @Override
            public String getSensor(){
                return SENSOR.ACCELEROMETER.TITLE;
            }
            @Override
            public String getDevice(){
                return DEVICE.METAWEAR.TITLE;
            }
        },
        GYROSCOPE_METAWEAR(true) {
            @Override
            public String getSensor(){
                return SENSOR.GYROSCOPE.TITLE;
            }
            @Override
            public String getDevice(){
                return DEVICE.METAWEAR.TITLE;
            }
        },
        RSSI(true) {
            @Override
            public String getSensor(){
                return SENSOR.RSSI.TITLE;
            }
            @Override
            public String getDevice(){
                return DEVICE.MOBILE.TITLE;
            }
        },
        BATTERY_METAWEAR(false) {
            @Override
            public String getSensor(){
                return SENSOR.BATTERY.TITLE;
            }
            @Override
            public String getDevice(){
                return DEVICE.METAWEAR.TITLE;
            }
        },
        UNKNOWN(false) {
            @Override
            public String getSensor(){
                return null;
            }
            @Override
            public String getDevice(){
                return null;
            }
        };

        public String getSensor(){
            return null;
        }
        public String getDevice(){
            return null;
        }
        private final boolean display;
        SENSOR_TYPE(boolean display){
            this.display = display;
        }
        public boolean display(){
            return display;
        }

    }

    public interface METAWEAR_STREAM_KEY{
        String ACCELEROMETER = "edu.umass.cs.prepare.metawear-accelerometer-stream";
        String GYROSCOPE = "edu.umass.cs.prepare.metawear-gyroscope-stream";
    }

    public interface NOTIFICATION_ID {
        int WEARABLE_SENSOR_SERVICE = 101;
        int METAWEAR_SENSOR_SERVICE = 102;
        int DATA_WRITER_SERVICE = 103;
        int RECORDING_SERVICE = 104;
    }

    /** The port for the server where the data should be sent. **/
    public static final int SERVER_PORT = 9999;

    public interface DEVICE {
        interface METAWEAR {
            String NAME = "METAWEAR";
            String TITLE = "Metawear";
        }
        interface WEARABLE {
            String NAME = "WEARABLE";
            String TITLE = "Wearable";
        }
        interface MOBILE {
            String NAME = "MOBILE";
            String TITLE = "Mobile";
        }
    }

    public interface SENSOR {
        interface ACCELEROMETER {
            String NAME = "ACCELEROMETER";
            String TITLE = "Accelerometer";
        }
        interface GYROSCOPE {
            String NAME = "GYROSCOPE";
            String TITLE = "Gyroscope";
        }
        interface RSSI {
            String NAME = "RSSI";
            String TITLE = "RSSI";
        }
        interface BATTERY {
            String NAME = "BATTERY";
            String TITLE = "Battery";
        }
    }


}
