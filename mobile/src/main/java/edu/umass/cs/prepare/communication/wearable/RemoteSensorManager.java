package edu.umass.cs.prepare.communication.wearable;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.umass.cs.prepare.communication.local.Broadcaster;
import edu.umass.cs.shared.constants.SharedConstants;

/**
 * The Remote Sensor Manager is responsible for remotely communicating with the motion sensors
 * on the wearable device. For instance, it can send commands to the wearable device to start/stop
 * sensor data collection services on the wearable.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see com.google.android.gms.wearable.DataApi
 * @see GoogleApiClient
 */
public class RemoteSensorManager {
    /** used for debugging purposes */
    private static final String TAG = RemoteSensorManager.class.getName();

    /** the number of milliseconds to wait for a client connection */
    private static final int CLIENT_CONNECTION_TIMEOUT = 5000;

    /** singleton instance of the remote sensor manager */
    private static RemoteSensorManager instance;

    /** used for asynchronous message sending on a non-UI thread */
    private final ExecutorService executorService;

    /** the Google API client is responsible for communicating with the wearable device over the data layer */
    private final GoogleApiClient googleApiClient;

    /** return singleton instance of the remote sensor manager, instantiating if necessary */
    public static synchronized RemoteSensorManager getInstance(Context context) {
        if (instance == null) {
            instance = new RemoteSensorManager(context.getApplicationContext());
        }

        return instance;
    }

    private RemoteSensorManager(Context context) {
        this.googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();

        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * validate the connection between the handheld and the wearable device.
     * @return true if successful, false if unsuccessful, i.e. no connection after {@link RemoteSensorManager#CLIENT_CONNECTION_TIMEOUT} milliseconds
     */
    private boolean validateConnection() {
        if (googleApiClient.isConnected()) {
            return true;
        }

        ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

        return result.isSuccess();
    }

    /** send a message to the wearable device to start data collection on the wearable */
    public void startSensorService() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Start Sensor Service");
                sendMessageInBackground(SharedConstants.COMMANDS.START_SENSOR_SERVICE, null);
            }
        });
    }

    /** send a message to the wearable device to stop data collection on the wearable */
    public void stopSensorService() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Stop Sensor Service");
                sendMessageInBackground(SharedConstants.COMMANDS.STOP_SENSOR_SERVICE, null);
            }
        });
    }

    /** send a message to the wearable device to start data collection on the Metawear tag */
    public void startMetawearService(){
        Log.d(TAG, "startMetawearService called.");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Sending message to wearable...");
                sendMessageInBackground(SharedConstants.COMMANDS.START_METAWEAR_SERVICE, null);
            }
        });
    }

    /** send a message to the wearable device to stop data collection on the Metawear tag */
    public void stopMetawearService() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Stop Sensor Service");
                sendMessageInBackground(SharedConstants.COMMANDS.STOP_METAWEAR_SERVICE, null);
            }
        });
    }

    /**
     * sends a command/message (referred to as a path in Google API logic) to the wearable application
     * @param path the message sent to the wearable device
     */
    private void sendMessageInBackground(final String path, final byte[] msg) {
        if (validateConnection()) {
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await().getNodes();

            if (nodes.size() == 0 && !path.equals(SharedConstants.COMMANDS.STOP_SENSOR_SERVICE)){
                Log.d(TAG, "No connected nodes.");
                Broadcaster.broadcastMessage(googleApiClient.getContext(), SharedConstants.MESSAGES.WEARABLE_CONNECTION_FAILED);
                return;
            }
            for (Node node : nodes) {
                Log.i(TAG, "add node " + node.getDisplayName());
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, msg).
                        setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                Log.d(TAG, "startOrStopInBackground(" + path + "): " + sendMessageResult.getStatus().isSuccess());
                                if (!sendMessageResult.getStatus().isSuccess() && !path.equals(SharedConstants.COMMANDS.STOP_SENSOR_SERVICE))
                                    Broadcaster.broadcastMessage(googleApiClient.getContext(), SharedConstants.MESSAGES.WEARABLE_CONNECTION_FAILED);
                            }
                        });
            }
        } else {
            Log.w(TAG, "No connections available");
        }
    }

    public void queryWearableState(){
        Log.d(TAG, "Query wearable state.");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Sending message to wearable...");
                sendMessageInBackground(SharedConstants.COMMANDS.QUERY_WEARABLE_STATE, null);
            }
        });
    }
}
