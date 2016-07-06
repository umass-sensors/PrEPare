package edu.umass.cs.prepare.constants;

import android.app.Service;
import android.content.Intent;

/**
 * The Constants class stores various constants that are used across multiple mobile application
 * components. This includes identifiers for intent actions used when the main UI communicates
 * with local services; and keys for identifying values passed between application components.
 *
 * @author snoran
 * @affiliation University of Massachusetts Amherst
 */
public class Constants {

    /** Number of bytes required to represent a formatted Unix timestamp, used for efficient string allocation **/
    public static final int BYTES_PER_TIMESTAMP = 20;

    /** Number of bytes required to represent a formatted float reading, used for efficient string allocation **/
    public static final int BYTES_PER_SENSOR_READING = 10;

    /** An action describes commands delivered to a service, sent by calling start
     * {@link android.content.Context#startService(Intent) startService(Intent)}.
     * @see Intent
     * @see Service
     */
    public interface ACTION {
        String MINIMIZE_VIDEO = "edu.umass.cs.prepare.action.minimize-video";
        String MAXIMIZE_VIDEO = "edu.umass.cs.prepare.action.maximize-video";
        String NAVIGATE_TO_APP = "edu.umass.cs.prepare.action.navigate-to-application";
        String BROADCAST_SENSOR_DATA = "edu.umass.cs.prepare.action.broadcast-sensor-data";
        String BROADCAST_MESSAGE = "edu.umass.cs.prepare.action.broadcast-message";
    }

    /**
     * A key identify an object or value that is passed between application components, via an {@link Intent}
     */
    public interface KEY {
        String SURFACE_WIDTH = "edu.umass.cs.prepare.key.surface-width";
        String SURFACE_HEIGHT = "edu.umass.cs.prepare.key.surface-height";
        String SURFACE_X = "edu.umass.cs.prepare.key.surface-x";
        String SURFACE_Y = "edu.umass.cs.prepare.key.surface-y";
        String SENSOR_DATA = "edu.umass.cs.prepare.key.sensor-data";
        String SENSOR_TYPE = "edu.umass.cs.prepare.key.sensor-type";
        String TIMESTAMP = "edu.umass.cs.prepare.key.timestamp";
    }

}
