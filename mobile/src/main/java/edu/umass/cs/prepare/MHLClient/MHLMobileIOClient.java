package edu.umass.cs.prepare.MHLClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import edu.umass.cs.prepare.MHLClient.MHLDataStructures.MHLBlockingSensorReadingQueue;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLSensorReading;

/**
 * The client is responsible for handing the server connection and sending data to the server.
 */
public class MHLMobileIOClient {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = MHLMobileIOClient.class.getName();

    /** The blocking queue containing the sensor data. **/
    private final MHLBlockingSensorReadingQueue sensorReadingQueue;

    /** The thread responsible for establishing the client-server connection. **/
    private final Thread clientThread;

    /** The thread responsible for communicating with, e.g. sending data to, the server. **/
    private Thread transmissionThread;

    /** The web socket to the server. **/
    private final Socket socket;

    /**
     * The number of milliseconds after which a server connection attempt ends and the
     * {@link MHLConnectionStateHandler#onConnectionFailed()} method is called.
     */
    private static final int CONNECTION_TIMEOUT = 5000;

    /**
     * The connection state handler is notified when the connection to the server is successfully
     * established or when the connection attempt has failed.
     */
    private MHLConnectionStateHandler connectionStateHandler = null;

    public MHLMobileIOClient(String ip, int port){
        this.sensorReadingQueue = new MHLBlockingSensorReadingQueue();
        clientThread = new Thread(new MHLClientThread(ip, port));
        socket = new Socket();
    }

    /**
     * Sets the connection state handler to respond to successful/failed connection attempts.
     * @param connectionStateHandler The connection state handler must define the
     * {@link MHLConnectionStateHandler#onConnected() onConnected()} and
     * {@link MHLConnectionStateHandler#onConnectionFailed() onConnectionFailed()} methods.
     */
    public void setConnectionStateHandler(MHLConnectionStateHandler connectionStateHandler){
        this.connectionStateHandler = connectionStateHandler;
    }

    /**
     * Establishes a connection to the server.
     */
    public void connect(){
        clientThread.start();
    }

    /**
     * Awaits data to send to the server. This is called once a connection has been established.
     */
    private void onConnected(){
        try {
            transmissionThread = new Thread(new MHLTransmissionThread());
            transmissionThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnects from the server, i.e. closes the socket, and stops the data transmission thread.
     */
    public void disconnect(){
        transmissionThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds the given sensor reading to the blocking queue, so that it will be sent to the server
     * @param reading the sensor reading object
     */
    public void sendSensorReading(MHLSensorReading reading){
        sensorReadingQueue.offer(reading);
    }

    private class MHLClientThread implements Runnable {
        private String ip;
        private int port;

        public MHLClientThread(String ip, int port){
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT);

                if (connectionStateHandler != null)
                    connectionStateHandler.onConnected();
                MHLMobileIOClient.this.onConnected();

            } catch (IOException e) {
                if (connectionStateHandler != null)
                    connectionStateHandler.onConnectionFailed();
            }
        }
    }

    /**
     * A transmission thread is responsible for sending data to the server after a client-server
     * connection has been established.
     */
    private class MHLTransmissionThread implements Runnable {

        /** Writes json-formatted data to the server. **/
        private final BufferedWriter output;

        public MHLTransmissionThread() throws IOException{
            this.output = new BufferedWriter(new OutputStreamWriter(MHLMobileIOClient.this.socket.getOutputStream()));
        }

        public void run(){
            while (!Thread.currentThread().isInterrupted()){
                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                ArrayList<MHLSensorReading> latestReadings = new ArrayList<>();
                sensorReadingQueue.drainTo(latestReadings);

                for (int i = latestReadings.size() - 1; i >= 0; i--){
                    MHLSensorReading reading = latestReadings.get(i);
                    try {
                        // the newline character is necessary to flush the stream
                        output.write(reading.toJSONString() + "\n");
                        output.flush();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(10);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
