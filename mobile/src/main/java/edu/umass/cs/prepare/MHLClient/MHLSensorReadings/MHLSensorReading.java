package edu.umass.cs.prepare.MHLClient.MHLSensorReadings;

import org.json.JSONObject;

import edu.umass.cs.shared.constants.SharedConstants;

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

    public static MHLSensorReading getReading(SharedConstants.SENSOR_TYPE sensorType, long timestamp, float... values){
        String device = sensorType.getDevice().toUpperCase();
        String sensor = sensorType.getSensor().toUpperCase();
        switch (sensor) {
            case SharedConstants.SENSOR.ACCELEROMETER.NAME:
                return new MHLAccelerometerReading(0, device, timestamp, values);
            case SharedConstants.SENSOR.GYROSCOPE.NAME:
                return new MHLGyroscopeReading(0, device, timestamp, values);
            case SharedConstants.SENSOR.RSSI.NAME:
                return new MHLRSSIReading(0, device, timestamp, values);
            default:
                return null;
        }
    }
}
