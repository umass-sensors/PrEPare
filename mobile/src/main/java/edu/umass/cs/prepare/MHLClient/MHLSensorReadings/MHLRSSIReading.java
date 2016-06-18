package edu.umass.cs.prepare.MHLClient.MHLSensorReadings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A received signal strength indicator (RSSI) reading.
 */
public class MHLRSSIReading extends MHLSensorReading {

    private long timestamp;
    private int rssi;

    private static final String SENSOR_TYPE = "SENSOR_RSSI";

    public MHLRSSIReading(int userID, String deviceType, long timestamp, int rssi){
        super(userID, deviceType, SENSOR_TYPE);

        this.timestamp = timestamp;
        this.rssi = rssi;
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject data = new JSONObject();
        JSONObject obj = new JSONObject();

        try {
            data.put("t", timestamp);
            data.put("rssi", rssi);

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
