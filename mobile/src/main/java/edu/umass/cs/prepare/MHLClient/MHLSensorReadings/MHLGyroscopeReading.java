package edu.umass.cs.prepare.MHLClient.MHLSensorReadings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by erikrisinger on 6/14/16.
 */
class MHLGyroscopeReading extends MHLSensorReading {

    private double x, y, z;
    private long timestamp;

    public MHLGyroscopeReading(int userID, String deviceType, long t, float... values){
        super(userID, deviceType, "SENSOR_" + deviceType + "_GYRO");

        this.timestamp = t;
        this.x = values[0];
        this.y = values[1];
        this.z = values[2];
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
