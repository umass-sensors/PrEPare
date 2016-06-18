package edu.umass.cs.shared;

/**
 * Data buffer for arbitrary dimensional sensor data, e.g. 3-axis accelerometer data,
 * 4-axis quaternion stream or single-axis RSSI readings.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SensorBuffer {
    /** The size of the buffer, in number of readings. **/
    private final int bufferSize;

    /** The number of dimensions per sensor reading. **/
    private final int nDimensions;

    /** The list of timestamps associated with each sequential sensor reading. **/
    private final long[] timestamps;

    /** The [{@link #nDimensions} x {@link #bufferSize}]-length list of sensor readings.  **/
    private final float[] values;

    /** The index into the list of sensor readings. **/
    private int index;

    public SensorBuffer(int bufferSize, int nDimensions) {
        this.bufferSize = bufferSize;
        this.nDimensions = nDimensions;
        index = 0;
        timestamps = new long[bufferSize];
        values = new float[nDimensions * bufferSize];
    }

    /** The callback wrapper that specifies how to handle the full buffer. **/
    private OnBufferFullCallback callback = null;

    public interface OnBufferFullCallback {
        void onBufferFull(long[] timestamps, float[] values);
    }

    public void setOnBufferFullCallback(OnBufferFullCallback callback) {
        this.callback = callback;
    }

    /**
     * Appends a reading to the sensor buffer, updating the current index and
     * calling the {@link SensorBuffer.OnBufferFullCallback OnBufferFullCallback}
     * if the buffer becomes full.
     * @param timestamp the timestamp at which the sensor event occurred
     * @param values sensor values, e.g. a single value or an xyz data point
     */
    public void addReading(long timestamp, float... values) {
        timestamps[index] = timestamp;
        System.arraycopy(values, 0, this.values, nDimensions * index, values.length);
        index++;
        if (callback != null && index == bufferSize) {
            callback.onBufferFull(this.timestamps, this.values);
            index = 0;
        }
    }
}