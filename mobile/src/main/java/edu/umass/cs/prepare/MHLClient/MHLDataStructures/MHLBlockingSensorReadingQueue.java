package edu.umass.cs.prepare.MHLClient.MHLDataStructures;

import java.util.concurrent.ArrayBlockingQueue;

import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLSensorReading;

/**
 * Created by erikrisinger on 6/14/16.
 */
public class MHLBlockingSensorReadingQueue extends ArrayBlockingQueue<MHLSensorReading> {
    private static final int QUEUE_SIZE = 5000;
    public MHLBlockingSensorReadingQueue(){
        super(QUEUE_SIZE);
    }
}