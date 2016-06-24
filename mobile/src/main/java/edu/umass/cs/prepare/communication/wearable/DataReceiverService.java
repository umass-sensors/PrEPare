package edu.umass.cs.prepare.communication.wearable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.shared.DataLayerUtil;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.prepare.constants.Constants;

/**
 * The Data Receiver Service listens for data sent from the wearable to the handheld device. In
 * particular, we expect sensor data from the wearable and the Metawear C tag via the wearable.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see com.google.android.gms.common.api.GoogleApiClient
 * @see WearableListenerService
 */
public class DataReceiverService extends WearableListenerService {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = DataReceiverService.class.getName();

    private ServiceManager serviceManager;

    @Override
    public void onCreate() {
        serviceManager = ServiceManager.getInstance(this);
        super.onCreate();
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        Log.i(TAG, "Connected to: " + peer.getDisplayName() + " [" + peer.getId() + "]");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        Log.i(TAG, "Disconnected from: " + peer.getDisplayName() + " [" + peer.getId() + "]");
    }

    //Note: This is only called when the data is actually changed!! Since we have a timestamp in the data event, that is no problem
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri(); //easy way to manipulate path we receive
                String path = uri.getPath();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();

                if (path.equals(SharedConstants.DATA_LAYER_CONSTANTS.SENSOR_PATH)) {
                    SharedConstants.SENSOR_TYPE sensorType = SharedConstants.SENSOR_TYPE.values()[dataMap.getInt(SharedConstants.KEY.SENSOR_TYPE)];
                    long[] timestamps = dataMap.getLongArray(SharedConstants.KEY.TIMESTAMPS);
                    float[] values = dataMap.getFloatArray(SharedConstants.KEY.SENSOR_VALUES);
                    Log.d(TAG, "Data received on mobile application : " + sensorType.name());
                    broadcastSensorData(this, sensorType, timestamps, values);
                }
                else if (path.equals(SharedConstants.DATA_LAYER_CONSTANTS.MESSAGE_PATH)){
                    int message = dataMap.getInt(SharedConstants.KEY.MESSAGE);
                    broadcastMessage(this, message);
                    if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTED){
                        Log.d(TAG, "Received message CONNECTED.");
                        serviceManager.startDataWriterService();
                    }
//                    if (dataMap.getInt(SharedConstants.KEY.MESSAGE) == SharedConstants.MESSAGES.BEACON_WITHIN_RANGE) {
//                        RemoteSensorManager.getInstance(this).startMetawearService();
//                    }
                }
            }
        }
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
        context.sendBroadcast(intent);
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
}