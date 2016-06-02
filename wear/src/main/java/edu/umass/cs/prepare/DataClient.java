package edu.umass.cs.prepare;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.umass.cs.shared.SharedConstants;

/**
 * The DataClient is responsible for sending data from the wearable device to the handheld application.
 *
 * See <a href=https://github.com/pocmo/SensorDashboard/blob/master/wear/src/main/java/com/github/pocmo/sensordashboard/DeviceClient.java>
 *     Sensor Dashboard</a>
 * for similar work.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see GoogleApiClient
 * @see DataApi
 */
public class DataClient {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = DataClient.class.getName();

    /** Google API client used to communicate between devices over data layer */
    private final GoogleApiClient googleApiClient;

    /** static singleton - we should only have one data client! */
    private static DataClient instance;

    /** timeout when connecting to the handheld device: If not connected after 5 seconds, return failure */
    private static final int CLIENT_CONNECTION_TIMEOUT = 5000;

    /** used to send data on a separate non-UI thread */
    private final ExecutorService executorService;

    /** returns the singleton instance of the class, instantiating if necessary */
    public static DataClient getInstance(Context context) {
        if (instance == null){
            instance = new DataClient(context.getApplicationContext());
        }
        return instance;
    }

    private DataClient(Context context){
        googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();

        executorService = Executors.newCachedThreadPool();
    }

    /**
     * Sends the sensor data via the data layer to the handheld application. This calls
     * {@link #sendSensorDataInBackground(SharedConstants.SENSOR_TYPE, String[], float[])} so as not to block
     * program execution on the main thread.
     * @param sensorType the sensor from which the data is received, defined in {@link SharedConstants.SENSOR_TYPE}
     * @param timestamps a sequence of timestamps corresponding to when the values were measured
     * @param values a list sensor readings
     */
    public void sendSensorData(final SharedConstants.SENSOR_TYPE sensorType, final String[] timestamps, final float[] values) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                sendSensorDataInBackground(sensorType, timestamps, values);
            }
        });
    }

    /**
     * Sends the sensor data via the data layer to the handheld application in a background thread.
     * @param sensorType the sensor from which the data is received, defined in {@link SharedConstants.SENSOR_TYPE}
     * @param timestamps a sequence of timestamps corresponding to when the values were measured
     * @param values a list sensor readings
     */
    private void sendSensorDataInBackground(final SharedConstants.SENSOR_TYPE sensorType, final String[] timestamps, final float[] values) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(SharedConstants.DATA_LAYER_CONSTANTS.SENSOR_PATH);

        dataMap.getDataMap().putInt(SharedConstants.KEY.SENSOR_TYPE, sensorType.ordinal()); //TODO: .ordinal() is considered sloppy
        dataMap.getDataMap().putStringArray(SharedConstants.KEY.TIMESTAMPS, timestamps);
        dataMap.getDataMap().putFloatArray(SharedConstants.KEY.SENSOR_VALUES, values);

        PutDataRequest putDataRequest = dataMap.asPutDataRequest();
        send(putDataRequest);
    }

    /**
     * Connects to the Google API client if necessary.
     * @return True if successful, false otherwise.
     */
    private boolean validateConnection() {
        if (googleApiClient.isConnected()) {
            return true;
        }

        ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

        return result.isSuccess();
    }

    /**
     * Sends the data mapped to the put request to the mobile device via the Google API client.
     * @param putDataRequest encodes the data being sent to the mobile device. A {@link PutDataRequest}
     *                       is analogous to an {@link Intent}, which allows cross-context communication.
     */
    private void send(PutDataRequest putDataRequest) {
        if (validateConnection()) {
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    //use dataItemResult.getStatus().isSuccess() to see if successful
                }
            });
        }
    }
}
