package edu.umass.cs.shared.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;

import edu.umass.cs.shared.R;

/**
 * A simplified preference manager which exposes the relevant variables
 * and updates the variables automatically when the preferences are changed.
 */
public class ApplicationPreferences implements SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean recordAudio,
                    showTutorial,
                    showCameraReminder,
                    writeLocal,
                    writeServer,
                    blinkLedWhileRunning,
                    enableRSSI,
                    enableGyroscope,
                    enablePillBottle,
                    useAndroidWear,
                    runServiceOverWearable,
                    enableWearableGyroscope;
    
    private int batteryLevel,
                rssiSamplingRate,
                accelerometerSamplingRate,
                gyroscopeSamplingRate,
                wearableAccelerometerSamplingRate,
                wearableGyroscopeSamplingRate,
                subjectID;

    private String  saveDirectory,
                    mwAddress,
                    ipAddress;

    private SharedPreferences preferences;

    private static ApplicationPreferences instance;

    private Context context;

    public static ApplicationPreferences getInstance(Context context){
        if (instance == null)
            instance = new ApplicationPreferences(context);
        return instance;
    }

    private ApplicationPreferences(Context context){
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        this.context = context;
        loadPreferences();
    }

    private void loadPreferences(){
        final String defaultDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                context.getString(R.string.app_name)).getAbsolutePath();
        saveDirectory = preferences.getString(context.getString(R.string.pref_directory_key),
                defaultDirectory);
        showCameraReminder = preferences.getBoolean(context.getString(R.string.pref_camera_reminder_key),
                context.getResources().getBoolean(R.bool.pref_camera_reminder_default));
        mwAddress = preferences.getString(context.getString(R.string.pref_device_key),
                context.getString(R.string.pref_device_default));
        showTutorial = preferences.getBoolean(context.getString(R.string.pref_show_tutorial_key),
                context.getResources().getBoolean(R.bool.pref_show_tutorial_default));
        batteryLevel = preferences.getInt(context.getString(R.string.pref_battery_level_key),
                context.getResources().getInteger(R.integer.pref_battery_level_default));
        accelerometerSamplingRate = Integer.parseInt(preferences.getString(context.getString(R.string.pref_accelerometer_sampling_rate_key),
                context.getString(R.string.pref_accelerometer_sampling_rate_default)));
        gyroscopeSamplingRate = Integer.parseInt(preferences.getString(context.getString(R.string.pref_gyroscope_sampling_rate_key),
                context.getString(R.string.pref_gyroscope_sampling_rate_default)));

        wearableAccelerometerSamplingRate = Integer.parseInt(preferences.getString(context.getString(R.string.pref_wearable_accelerometer_sampling_rate_key),
                context.getString(R.string.pref_wearable_accelerometer_sampling_rate_default)));
        wearableGyroscopeSamplingRate = Integer.parseInt(preferences.getString(context.getString(R.string.pref_wearable_gyroscope_sampling_rate_key),
                context.getString(R.string.pref_wearable_accelerometer_sampling_rate_default)));

        rssiSamplingRate = Integer.parseInt(preferences.getString(context.getString(edu.umass.cs.shared.R.string.pref_rssi_sampling_rate_key),
                context.getString(edu.umass.cs.shared.R.string.pref_rssi_sampling_rate_default)));
        ipAddress = preferences.getString(context.getString(R.string.pref_ip_key),
                context.getString(R.string.pref_ip_default));
        writeLocal = preferences.getBoolean(context.getString(R.string.pref_local_key),
                context.getResources().getBoolean(R.bool.pref_local_default));
        writeServer = preferences.getBoolean(context.getString(R.string.pref_server_key),
                context.getResources().getBoolean(R.bool.pref_server_default));
        blinkLedWhileRunning = preferences.getBoolean(context.getString(R.string.pref_led_key),
                context.getResources().getBoolean(R.bool.pref_led_default));
        enableGyroscope = preferences.getBoolean(context.getString(R.string.pref_gyroscope_key),
                context.getResources().getBoolean(R.bool.pref_gyroscope_default));
        enableRSSI = preferences.getBoolean(context.getString(R.string.pref_rssi_key),
                context.getResources().getBoolean(R.bool.pref_rssi_default));
        recordAudio = preferences.getBoolean(context.getString(R.string.pref_audio_key),
                context.getResources().getBoolean(R.bool.pref_audio_default));
        enablePillBottle = preferences.getBoolean(context.getString(R.string.pref_connect_key),
                context.getResources().getBoolean(R.bool.pref_connect_default));
        useAndroidWear = preferences.getBoolean(context.getString(R.string.pref_wearable_key),
                context.getResources().getBoolean(R.bool.pref_wearable_default));
        runServiceOverWearable = preferences.getBoolean(context.getString(R.string.pref_run_service_over_wearable_key),
                context.getResources().getBoolean(R.bool.pref_run_service_over_wearable_default));
        enableWearableGyroscope = preferences.getBoolean(context.getString(R.string.pref_wearable_gyroscope_key),
                context.getResources().getBoolean(R.bool.pref_wearable_gyroscope_default));
        subjectID = preferences.getInt(context.getString(R.string.pref_subject_id_key),
                context.getResources().getInteger(R.integer.pref_subject_id_default));
    }
    
    public boolean recordAudio(){
        return recordAudio;
    }

    public boolean showTutorial(){
        return showTutorial;
    }

    public boolean enableRSSI(){
        return enableRSSI;
    }

    public boolean enableGyroscope(){
        return enableGyroscope;
    }

    public boolean enableWearableGyroscope(){
        return enableWearableGyroscope;
    }

    public boolean blinkLedWhileRunning(){
        return blinkLedWhileRunning;
    }

    public boolean writeLocal(){
        return writeLocal;
    }

    public boolean writeServer(){
        return writeServer;
    }

    public boolean showCameraReminder(){
        return showCameraReminder;
    }

    public boolean useAndroidWear(){
        return useAndroidWear;
    }

    public boolean enablePillBottle(){
        return enablePillBottle;
    }

    public boolean runServiceOverWearable(){
        return runServiceOverWearable;
    }

    public String getMwAddress(){
        return mwAddress;
    }

    public String getIpAddress(){
        return ipAddress;
    }

    public String getSaveDirectory(){
        File directory = new File(saveDirectory);
        if(!directory.exists()) {
            if (directory.mkdirs()){
                Toast.makeText(context, String.format("Created directory %s", directory.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, String.format("Failed to create directory %s. Please set the directory in Settings",
                        directory.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }
        }
        return saveDirectory;
    }

    public int getBatteryLevel(){
        return batteryLevel;
    }

    public int getAccelerometerSamplingRate(){
        return accelerometerSamplingRate;
    }

    public int getGyroscopeSamplingRate(){
        return gyroscopeSamplingRate;
    }

    public int getWearableAccelerometerSamplingRate(){
        return wearableAccelerometerSamplingRate;
    }

    public int getWearableGyroscopeSamplingRate(){
        return wearableGyroscopeSamplingRate;
    }

    public int getRssiSamplingRate(){
        return rssiSamplingRate;
    }

    public int getSubjectID(){
        return subjectID;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
       loadPreferences();
    }

    public SharedPreferences.Editor edit(){
        return preferences.edit();
    }
}
