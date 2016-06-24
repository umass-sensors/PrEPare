package edu.umass.cs.shared.MHLClient.MHLSensorReadings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by erikrisinger on 6/14/16.
 */
public class MHLGyroscopeReading extends MHLSensorReading {

    private double x, y, z;
    private long timestamp;

    public MHLGyroscopeReading(int userID, String deviceType, long t, float x, float y, float z){
        super(userID, deviceType, "SENSOR_" + deviceType + "_GYRO");

        this.timestamp = t;
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
