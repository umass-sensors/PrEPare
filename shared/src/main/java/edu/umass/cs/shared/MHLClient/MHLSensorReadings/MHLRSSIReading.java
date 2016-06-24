package edu.umass.cs.shared.MHLClient.MHLSensorReadings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by erikrisinger on 6/15/16.
 */
public class MHLRSSIReading extends MHLSensorReading {

    private long timestamp;
    private int rssi;

    public MHLRSSIReading(int userID, String deviceType, long t, int rssi){
        super(userID, deviceType, "SENSOR_RSSI");

        this.timestamp = t;
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
