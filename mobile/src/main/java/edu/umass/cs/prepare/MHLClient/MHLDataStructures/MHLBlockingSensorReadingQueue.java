package edu.umass.cs.prepare.MHLClient.MHLDataStructures;

import java.util.concurrent.ArrayBlockingQueue;

import edu.umass.cs.prepare.MHLClient.MHLSensorReadings.MHLSensorReading;

/**
 * This class is a blocking queue of {@link MHLSensorReading sensor readings}. It
 * has no important functionality but to limit the capacity of the blocking queue.
 */
public class MHLBlockingSensorReadingQueue extends ArrayBlockingQueue<MHLSensorReading> {
    private static final int QUEUE_CAPACITY = 5000;
    public MHLBlockingSensorReadingQueue(){
        super(QUEUE_CAPACITY);
    }
}
