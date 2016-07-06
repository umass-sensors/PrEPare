package edu.umass.cs.prepare.communication.local;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.shared.communication.BroadcastInterface;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;

/**
 * Specifies how a mobile service should notify the other application components of important events,
 * e.g. the service started/stopped.
 * <br><br>
 * This specific implementation simply broadcasts a message via an {@link Intent} for other
 * application components to receive.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see BroadcastInterface
 * @see Context
 * @see Intent
 */
public class Broadcaster implements BroadcastInterface {

    /** The application component from which the message is sent. **/
    private Context context;

    public Broadcaster(Context context){
        this.context = context;
    }

    /**
     * Broadcasts sensor data to send to other mobile application components.
     * @param context the context from which the sensor data is sent.
     * @param sensorType the type of sensor, corresponding to the Sensor class constants, i.e. Sensor.TYPE_ACCELEROMETER
     * @param timestamps list of timestamps corresponding to sensor events
     * @param values list of sensor readings
     */
    public static void broadcastSensorData(Context context, SharedConstants.SENSOR_TYPE sensorType, long[] timestamps, float[] values){
        Intent intent = new Intent();
        DataLayerUtil.serialize(sensorType).to(intent);
        intent.putExtra(Constants.KEY.SENSOR_TYPE, sensorType);
        intent.putExtra(Constants.KEY.SENSOR_DATA, values);
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamps);
        intent.setAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        manager.sendBroadcast(intent);
    }

    /**
     * Broadcasts a message to send to other mobile application components.
     * @param context the context from which the message is sent.
     * @param message the message being broadcast
     */
    public static void broadcastMessage(Context context, int message){
        Intent intent = new Intent();
        intent.putExtra(SharedConstants.KEY.MESSAGE, message);
        intent.setAction(Constants.ACTION.BROADCAST_MESSAGE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        manager.sendBroadcast(intent);
    }

    @Override
    public void broadcastMessage(int message) {
        Intent intent = new Intent();
        intent.putExtra(SharedConstants.KEY.MESSAGE, message);
        intent.setAction(Constants.ACTION.BROADCAST_MESSAGE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        manager.sendBroadcast(intent);
    }
}
