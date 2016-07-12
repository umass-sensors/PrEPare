package edu.umass.cs.prepare.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umass.cs.prepare.metawear.SelectDeviceActivity;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.recording.RecordingService;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.communication.wearable.RemoteSensorManager;
import edu.umass.cs.prepare.metawear.SensorService;
import edu.umass.cs.prepare.preferences.SettingsActivity;

/**
 * The Main Activity is the entry point for the application. It is the primary UI and allows
 * the user to interact with the system.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = MainActivity.class.getName();

    /** List of formatted sensor readings outputs **/
    private ArrayList<String> sensorReadings;

    /** Links the {@link #sensorReadings} to a UI view. **/
    private ArrayAdapter<String> sensorReadingAdapter;

    /** whether video recording should include audio **/
    private boolean record_audio;

    /** Indicates whether the {@link SensorService} should run over the wearable or mobile device. **/
    private boolean runServiceOverWearable;

    /** Indicates whether the {@link SensorService} is turned on. **/
    private boolean serviceEnabled;

    /** Request identifiers **/
    private interface REQUEST_CODE {
        int RECORDING = 1;
        int WINDOW_OVERLAY = 2;
        int SELECT_DEVICE = 3;
        int SET_PREFERENCES = 4;
    }

    /** The sensor manager which handles sensors on the wearable device remotely */
    private RemoteSensorManager remoteSensorManager;

    /** Handles services on the mobile application. */
    private ServiceManager serviceManager;

    /** The view containing the video recording preview. **/
    private SurfaceView mSurfaceView;

    /** The unique address of the Metawear device. **/
    private String mwAddress;

    /** Notifies the user that the mobile device is attempting to connect to the Metawear board. **/
    private ProgressDialog connectDialog;

    /** Button that controls video recording, i.e. on/off switch. **/
    private Button recordingButton;

    /** Image corresponding to the current Metawear battery level. **/
    private Bitmap batteryLevelBitmap;

    /** The action bar at the top of the main UI **/
    private ActionBar actionBar;

    public static int label = 0;

    private void showConnectingDialog(){
        if (connectDialog != null && connectDialog.isShowing())
            connectDialog.cancel();
        connectDialog = new ProgressDialog(this);
        connectDialog.setTitle(getString(R.string.title_connecting));
        connectDialog.setMessage(getString(R.string.message_wait));
        connectDialog.setCancelable(false);
        connectDialog.setCanceledOnTouchOutside(false);
        connectDialog.setIndeterminate(true);
        connectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!runServiceOverWearable) { //TODO: What if it changes during running?
                    Intent startServiceIntent = new Intent(MainActivity.this, SensorService.class);
                    startServiceIntent.setAction(SharedConstants.ACTIONS.CANCEL_CONNECTING);
                    startService(startServiceIntent);
                } else {
                    remoteSensorManager.cancelMetawearConnection();
                }

            }
        });
        connectDialog.show();
    }

    /**
     * Cancel the connection notification dialog.
     */
    private void cancelConnectingDialog() {
        if (connectDialog != null)
            connectDialog.dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceManager.maximizeVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        serviceManager.minimizeVideo();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }

        cancelConnectingDialog();
        super.onStop();
    }

    /**
     * Loads shared user preferences, e.g. whether video/audio is enabled
     */
    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        record_audio = preferences.getBoolean(getString(R.string.pref_audio_key),
                getResources().getBoolean(R.bool.pref_audio_default));
        runServiceOverWearable = preferences.getBoolean(getString(R.string.pref_wearable_key),
                getResources().getBoolean(R.bool.pref_wearable_default));
        mwAddress = preferences.getString(getString(R.string.pref_device_key),
                getString(R.string.pref_device_default));
        serviceEnabled = preferences.getBoolean(getString(R.string.pref_connect_key),
                getResources().getBoolean(R.bool.pref_connect_default));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadPreferences();

        remoteSensorManager = RemoteSensorManager.getInstance(this);
        serviceManager = ServiceManager.getInstance(this);

        recordingButton = (Button) findViewById(R.id.start_button);
        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!RecordingService.isRecording) {
                    //TODO: Do Android versions prior to M require run-time permission request for overlay?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        requestPermissions();
                    else
                        onPermissionsGranted();
                } else {
                    serviceManager.stopRecordingService();
                }

            }
        });
        if (RecordingService.isRecording)
            recordingButton.setBackgroundResource(android.R.drawable.ic_media_pause);

        Button labelButton = (Button) findViewById(R.id.label_button);
        assert labelButton != null;
        labelButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        showStatus("Labeling event");
                        label = 1;
                        return true;
                    case MotionEvent.ACTION_UP:
                        showStatus("No event");
                        label = 0;
                        return true;
                }
                return false;
            }
        });

        sensorReadings = new ArrayList<>();
        for (int i = 0; i < SharedConstants.SENSOR_TYPE.values().length; i++)
            sensorReadings.add(SharedConstants.SENSOR_TYPE.values()[i].name() + ": unavailable.");
        sensorReadingAdapter = new ArrayAdapter<>(this, R.layout.sensor_reading_item, sensorReadings);
        ListView listView = (ListView) findViewById(R.id.lv_sensor_readings);
        assert listView != null;
        listView.setAdapter(sensorReadingAdapter);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        actionBar = getSupportActionBar();

        if (mwAddress.equals(getString(R.string.pref_device_default))){
            startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
        } else {
            Log.d(TAG, "Starting Metawear service on startup");
            startMetawearService();
        }
    }

    /**
     * Called when all required permissions have been granted.
     */
    private void onPermissionsGranted(){
        int[] position = new int[2];
        mSurfaceView.getLocationInWindow(position);
        int w = mSurfaceView.getWidth();
        int h = mSurfaceView.getHeight();
        serviceManager.startRecordingService(position[0], position[1], w, h);
    }

    private void startMetawearService(){
        if (!serviceEnabled) return;
        if (runServiceOverWearable)
            remoteSensorManager.startMetawearService();
        else
            serviceManager.startMetawearService(); //CE:03:BF:17:58:41 //DC:00:25:17:8E:CF //F6:8D:FC:1A:E4:50
    }

    private void stopMetawearService(){
        if (runServiceOverWearable){
            remoteSensorManager.stopMetawearService();
        }else{
            serviceManager.stopMetawearService();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE.WINDOW_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /** if so check once again if we have permission */
                if (Settings.canDrawOverlays(this)) {
                    onPermissionsGranted();
                }
            }
        }else if (requestCode == REQUEST_CODE.SET_PREFERENCES){
            Log.d(TAG, "preferences changed");
            boolean serviceEnabledBefore = serviceEnabled;
            boolean runServiceOverWearableBefore = runServiceOverWearable;
            loadPreferences();
            if (serviceEnabledBefore != serviceEnabled || runServiceOverWearableBefore != runServiceOverWearable){
                if (serviceEnabled)
                    startMetawearService();
                else
                    stopMetawearService();
            }

        }else if (requestCode == REQUEST_CODE.SELECT_DEVICE){
            if (data != null) {
                mwAddress = data.getStringExtra(SharedConstants.KEY.UUID);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(getString(R.string.pref_device_key), mwAddress);
                editor.apply();
                startMetawearService();
            }else{
                finish(); //can't return to the main UI if there is no device available
            }
        }
    }

    /**
     * Displays a single accelerometer reading on the main UI
     * @param reading a 3-dimensional floating point vector representing the x, y and z accelerometer values respectively.
     */
    private void displaySensorReading(final SharedConstants.SENSOR_TYPE sensorType, final float[] reading){
        float x = reading[0];
        float y = reading[1];
        float z = reading[2];
        final String output = String.format(getString(R.string.initial_sensor_readings), x, y, z);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sensorReadings.set(sensorType.ordinal(), output);
                sensorReadingAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * display the battery level in the UI
     * @param percentage battery level in the range of [0,100]
     */
    private void updateBatteryLevel(final int percentage){
        if (batteryLevelBitmap == null)
            batteryLevelBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_battery_image_set);
        int nImages = 11;
        int height = batteryLevelBitmap.getHeight();
        int width = batteryLevelBitmap.getWidth();
        int width_per_image = width / nImages;
        int index = (percentage + 5) / (nImages - 1);
        int x = width_per_image * index;
        final Bitmap batteryLevelSingleBitmap = Bitmap.createBitmap(batteryLevelBitmap, x, 0, width_per_image, height);

        Resources res = getResources();
        final BitmapDrawable icon = new BitmapDrawable(res,batteryLevelSingleBitmap);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setIcon(icon);
                actionBar.setTitle("");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent openSettings = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(openSettings, REQUEST_CODE.SET_PREFERENCES, null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the draw overlay permission. This is required to run the video recording service in
     * a background service.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            /** if not, construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE.WINDOW_OVERLAY);
        }else{
            onPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE.RECORDING: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.CAMERA:
                                showStatus("Video Permission Denied!");
                                return;
                            case Manifest.permission.RECORD_AUDIO:
                                record_audio = false;
                                showStatus("Audio Permission Denied : Continuing with audio disabled.");
                                break;
                            default:
                                //required permission not granted, abort
                                //updateStatus(permissions[i] + " Permission Denied - cannot continue");
                                return;
                        }
                    }
                }
                checkDrawOverlayPermission();
            }
        }
    }

    /**
     * Request permissions required for video recording. These include
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link android.Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions(){
        List<String> permissionGroup = new ArrayList<>(Arrays.asList(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }));

        if (record_audio){
            permissionGroup.add(Manifest.permission.RECORD_AUDIO);
        }

        String[] permissions = permissionGroup.toArray(new String[permissionGroup.size()]);

        if (!hasPermissionsGranted(permissions)) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE.RECORDING);
            return;
        }
        checkDrawOverlayPermission();
    }

    /**
     * Check the specified permissions
     * @param permissions list of Strings indicating permissions
     * @return true if ALL permissions are granted, false otherwise
     */
    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows a removable status message at the bottom of the application.
     * @param message the status message shown
     */
    private void showStatus(String message){
        View mainUI = MainActivity.this.findViewById(R.id.fragment);
        assert mainUI != null;
        Snackbar snack = Snackbar.make(mainUI, message, Snackbar.LENGTH_LONG);
        View view = snack.getView();
        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_SENSOR_DATA)) {
                    SharedConstants.SENSOR_TYPE sensorType = DataLayerUtil.deserialize(SharedConstants.SENSOR_TYPE.class).from(intent);
                    float[] values = intent.getFloatArrayExtra(Constants.KEY.SENSOR_DATA);
                    float[] averages = new float[3];
                    for (int i = 0; i < values.length; i++) {
                        averages[i % 3] += values[i];
                    }
                    for (int j = 0; j < averages.length; j++) {
                        averages[j] /= (values.length / 3f);
                    }
                    displaySensorReading(sensorType, averages);
                }else if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(SharedConstants.KEY.MESSAGE, -1);
                    if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTING){
                        showStatus("Listening for movement...");
                        //showConnectingDialog();
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTED){
                        showStatus("Connected to pill bottle.");
                        //cancelConnectingDialog();
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_DISCONNECTED) {
                        serviceManager.stopDataWriterService();
                    } else if (message == SharedConstants.MESSAGES.RECORDING_SERVICE_STARTED){
                        recordingButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                    } else if (message == SharedConstants.MESSAGES.RECORDING_SERVICE_STOPPED){
                        recordingButton.setBackgroundResource(android.R.drawable.ic_media_play);
                    } else if (message == SharedConstants.MESSAGES.INVALID_ADDRESS){
                        showStatus("Invalid Bluetooth Address.");
                        startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
                    }
                }
            }
        }
    };
}
