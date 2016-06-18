package edu.umass.cs.prepare.MHLClient.MHLSensorReadings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A 3-axis accelerometer reading.
 */
public class MHLAccelerometerReading extends MHLSensorReading {

    /** Acceleration along the x-axis. **/
    private float x;
    /** Acceleration along the y-axis. **/
    private float y;
    /** Acceleration along the z-axis. **/
    private float z;

    /** The formatted timestamp of the sensor event. **/
    private long timestamp;

    private static final String SENSOR_TYPE = "SENSOR_ACCEL";

    public MHLAccelerometerReading(int userID, String deviceType, long timestamp, float x, float y, float z){
        super(userID, deviceType, SENSOR_TYPE);

        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public JSONObject toJSONObject(){
        JSONObject data = new JSONObject();
        JSONObject obj = new JSONObject();

        try {
            data.put("t", timestamp);
            data.put("x", x);
            data.put("y", y);
            data.put("z", z);

            obj.put("user_id", userID);
            obj.put("device_type", deviceType);
            obj.put("sensor_type", sensorType);
            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
