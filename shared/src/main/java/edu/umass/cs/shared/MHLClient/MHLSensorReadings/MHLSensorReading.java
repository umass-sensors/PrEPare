package edu.umass.cs.shared.MHLClient.MHLSensorReadings;

import org.json.JSONObject;

/**
 * Created by erikrisinger on 6/14/16.
 */
public abstract class MHLSensorReading {

    protected int userID;
    protected String deviceType;
    protected String sensorType;

    public MHLSensorReading(int userID, String deviceType, String sensorType){
        this.userID = userID;
        this.deviceType = deviceType;
        this.sensorType = sensorType;
    }

    public int getUserID(){
        return userID;
    }

    public String getDeviceType(){
        return deviceType;
    }

    public String getSensorType(){
        return sensorType;
    }

    public abstract JSONObject toJSONObject();

    public String toJSONString(){
        return this.toJSONObject().toString();
    }
}
