package edu.umass.cs.prepare.metawear;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

import edu.umass.cs.shared.DataLayerUtil;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.prepare.constants.Constants;

/**
 * The Data Receiver Service listens for data sent from the wearable to the handheld device. In
 * particular, we expect sensor data from the wearable and the Metawear C tag via the wearable.
 *
 * @see com.google.android.gms.common.api.GoogleApiClient
 * @see WearableListenerService
 */
public class DataReceiverService extends WearableListenerService {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = DataReceiverService.class.getName();

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
                    String[] timestamps = dataMap.getStringArray(SharedConstants.KEY.TIMESTAMPS);
                    float[] values = dataMap.getFloatArray(SharedConstants.KEY.SENSOR_VALUES);
                    Log.d(TAG, "Data received on mobile application : " + sensorType.name());
                    broadcastSensorData(this, sensorType, timestamps, values);
                }
//                else if (path.equals(SharedConstants.DATA_LAYER_CONSTANTS.STATUS_CONNECTED)){
//                    sendMessageToClients(Constants.MESSAGE.CONNECTED);
//                }
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
    public static void broadcastSensorData(Context context, SharedConstants.SENSOR_TYPE sensorType, String[] timestamps, float[] values){
        StringBuilder line = new StringBuilder(timestamps.length * (Constants.BYTES_PER_TIMESTAMP + Constants.BYTES_PER_SENSOR_READING + 6));
        if (sensorType == SharedConstants.SENSOR_TYPE.WEARABLE_TO_METAWEAR_RSSI ||
                sensorType == SharedConstants.SENSOR_TYPE.PHONE_TO_METAWEAR_RSSI){

            for (int i = 0; i < timestamps.length; i++) {
                String timestamp = timestamps[i];
                float rssi = values[i];
                line.append(String.format("%s,%d\n", timestamp, (int)rssi));
            }
        }else {

            for (int i = 0; i < timestamps.length; i++) {
                String timestamp = timestamps[i];
                float x = values[3 * i];
                float y = values[3 * i + 1];
                float z = values[3 * i + 2];

                line.append(String.format("%s,%f,%f,%f\n", timestamp, x, y, z));
            }
        }

        Intent intent = new Intent();
        DataLayerUtil.serialize(sensorType).to(intent);
        intent.putExtra(Constants.KEY.SENSOR_DATA, line.toString());
        intent.setAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
        context.sendBroadcast(intent);
    }
}