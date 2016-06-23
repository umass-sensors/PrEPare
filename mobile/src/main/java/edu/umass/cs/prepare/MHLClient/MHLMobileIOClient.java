package edu.umass.cs.prepare.MHLClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import edu.umass.cs.prepare.MHLClient.MHLDataStructures.MHLBlockingSensorReadingQueue;
import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLSensorReading;

public class MHLMobileIOClient {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = MHLMobileIOClient.class.getName();

    /** The blocking queue containing the sensor data. **/
    private volatile MHLBlockingSensorReadingQueue sensorReadingQueue;

    /** The thread responsible for establishing the client-server connection. **/
    private final Thread connectionThread;

    /** The user ID associated with the user establishing the connection **/
    private final int userID;

    /**
     * The connection state handler is notified when the connection to the server is successfully
     * established or when the connection attempt has failed.
     */
    private MHLConnectionStateHandler connectionStateHandler;

    /**
     * The number of milliseconds after which a server connection attempt ends and the
     * {@link MHLConnectionStateHandler#onConnectionFailed()} method is called.
     */
    private static final int CONNECTION_TIMEOUT = 5000;

    /** The web socket to the server. **/
    private final Socket socket;

    //constructor for pre-existing (external) queue
    public MHLMobileIOClient(final MHLBlockingSensorReadingQueue q, final String ip, final int port, final int id){
        this.sensorReadingQueue = q;
        this.userID = id;
        socket = new Socket();
        connectionThread = new Thread(new Runnable(){
            @Override
            public void run() {

                MHLTransmissionThread transmissionThread;

                System.out.println("STARTING SENSOR THREAD");

                try {
                    System.out.println("connecting to server: " + ip + ":" + port);
                    socket.connect(new InetSocketAddress(ip, port), CONNECTION_TIMEOUT);
                    System.out.println("connected");
                }
                catch (Exception e){
                    e.printStackTrace();
                    if (connectionStateHandler != null)
                        connectionStateHandler.onConnectionFailed();
                    return;
                }

                //connection successful -- launch transmission thread
                transmissionThread = new MHLTransmissionThread(socket);
                new Thread(transmissionThread).start();
            }
        });
    }

    //constructor for self-contained queue
    public MHLMobileIOClient(final String ip, final int port, final int id){
        this(new MHLBlockingSensorReadingQueue(), ip, port, id);
    }

    public void setConnectionStateHandler(MHLConnectionStateHandler connectionStateHandler){
        this.connectionStateHandler = connectionStateHandler;
    }

    public boolean addSensorReading(MHLSensorReading reading){
        if (!socket.isConnected() || socket.isClosed())
            connect();

        return sensorReadingQueue.offer(reading);
    }

    public void connect(){
        if (connectionThread.isAlive())
            connectionThread.interrupt();

        connectionThread.start();
    }

    class MHLTransmissionThread implements Runnable {
        private Socket clientSocket;
        private BufferedWriter output;
        private BufferedReader input;

        public MHLTransmissionThread(Socket clientSocket){
            this.clientSocket = clientSocket;

            try {
                this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        public void run(){
            ArrayList<MHLSensorReading> latestReadings;

            //connect to data collection server (DCS), return on failed handshake
//            System.out.println("calling connectToServer()");
            this.connectToServer();
//            System.out.println("called connectToServer()");
//            if (!running) return;

            //transmit data continuously until stopped
            while (!Thread.currentThread().isInterrupted()){
                //auto reconnect in case of interruption
//                if (!running) this.connectToServer();
//                System.out.println("inside while");
                try {
                    latestReadings = new ArrayList<>();
                    sensorReadingQueue.drainTo(latestReadings);

                    for (int i = latestReadings.size() - 1; i >= 0; i--){
                        MHLSensorReading reading = latestReadings.get(i);
                        output.write(reading.toJSONString() + "\n");
                        output.flush();
                    }
                    Thread.sleep(10);
                } catch (IOException | InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        private void connectToServer(){
            try {

                System.out.println("connectToServer()");

                //send user ID
                output.write("ID," + userID + "\n");
                output.flush();

                //read in ACK
                String ackString = input.readLine();
                String[] ack = ackString.split(",");

                System.out.println(ackString);

                //expecting "ACK" with user ID echoed back as CSV string, e.g.: "ACK,0"
                if (!("ACK".equals(ack[0]) && Integer.parseInt(ack[1]) == userID)){
                    System.out.println("failed to receive correct ACK from DCS");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
