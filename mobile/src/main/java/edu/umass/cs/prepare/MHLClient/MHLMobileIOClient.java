package edu.umass.cs.prepare.MHLClient;

import android.util.Log;

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

    /** The thread responsible for the client-server connection. **/
    private final Thread clientThread;

    private static final int CONNECTION_TIMEOUT = 5000;

    public MHLMobileIOClient(String ip, int port){
        this.sensorReadingQueue = new MHLBlockingSensorReadingQueue();
        clientThread = new Thread(new MHLClientThread(ip, port));
    }

    /**
     * Establishes a connection to the server and awaits any incoming data.
     */
    public void connect(){
        clientThread.start();
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
            Socket socket;
            MHLTransmissionThread transmissionThread;

            Log.d(TAG, "Opening socket.");
            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT);
                Log.d(TAG, "Connected");

                transmissionThread = new MHLTransmissionThread(socket);
                new Thread(transmissionThread).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class MHLTransmissionThread implements Runnable {
        private Socket clientSocket;
        private BufferedWriter output;

        public MHLTransmissionThread(Socket clientSocket){
            this.clientSocket = clientSocket;

            try {
                this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        public void run(){
            while (!Thread.currentThread().isInterrupted()){

                try {
                    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                    ArrayList<MHLSensorReading> latestReadings = new ArrayList<>();
                    sensorReadingQueue.drainTo(latestReadings);

                    for (int i = latestReadings.size() - 1; i >= 0; i--){
                        MHLSensorReading reading = latestReadings.get(i);
                        output.write(reading.toJSONString() + "\n");
                        output.flush();
                    }
                    Thread.sleep(10);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
