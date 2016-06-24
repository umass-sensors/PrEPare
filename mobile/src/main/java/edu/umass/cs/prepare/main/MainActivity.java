package edu.umass.cs.prepare.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.wearable.MessageApi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umass.cs.prepare.metawear.DataReceiverService;
import edu.umass.cs.prepare.metawear.SelectMetawearActivity;
import edu.umass.cs.prepare.metawear.ServiceManager;
import edu.umass.cs.prepare.recording.RecordingService;
import edu.umass.cs.shared.DataLayerUtil;
import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.metawear.RemoteSensorManager;
import edu.umass.cs.prepare.metawear.SensorService;
import edu.umass.cs.prepare.preferences.SettingsActivity;

/**
 * The Main Activity is the entry point for the application. It involves the main user interface
 * and is responsible for managing all background sensor services. These services handle
 * {@link DataReceiverService receiving data from the wearable},
 * {@link SensorService collecting data from the Metawear tag directly to the phone},
 * {@link DataReceiverService writing data to storage},
 * recording video in the background, and
 * {@link RemoteSensorManager managing the services available on the wearable}.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class MainActivity extends AppCompatActivity {

    /** View which displays the accelerometer readings from the Metawear tag TODO: make array adapter list view for displaying multiple modalities **/
    private TextView txtAccelerometer;

    /**
     * Messenger service for exchanging messages with the background service
     */
    private Messenger mService = null;

    /**
     * indicates if this activity is bound to the {@link SensorService}
     */
    private boolean mIsBound;

    /**
     * Messenger receiving messages from the background service to update UI
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /** whether video recording should include audio **/
    private boolean record_audio;

    private boolean runServiceOverWearable;

    /** Permission request identifier **/
    private static final int PERMISSION_REQUEST = 1;

    /** code to post/handler request for permission */
    private final static int WINDOW_OVERLAY_REQUEST = 2;

    /** Request code to identify that the user selected the device address when the activity called for result returns. **/
    private static final int SELECT_DEVICE_REQUEST_CODE = 3;

    /** The sensor manager which handles sensors on the wearable device remotely */
    private RemoteSensorManager remoteSensorManager;

    /** Handles services on the mobile application. */
    private ServiceManager serviceManager;

    public SurfaceView mSurfaceView;

    /** The unique address of the Metawear device. **/
    private String mwMacAddress;

    /**
     * Handler to handle incoming messages
     */
    static class IncomingHandler extends Handler {
        private final WeakReference<MainActivity> mMainActivity;

        IncomingHandler(MainActivity mainActivity) {
            mMainActivity = new WeakReference<>(mainActivity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE.SENSOR_STARTED:
                {
                    //mMainActivity.get().updateStatus("sensor started.");
                    //mMainActivity.get().onSensorStarted();
                    break;
                }
                case Constants.MESSAGE.SENSOR_STOPPED:
                {
                    //mMainActivity.get().updateStatus("sensor stopped.");
                    break;
                }
                case Constants.MESSAGE.STATUS:
                {
                    //mMainActivity.get().updateStatus(msg.getData().getString(Constants.KEY.STATUS));
                    break;
                }
                case Constants.MESSAGE.ACCELEROMETER_READING:
                {
                    mMainActivity.get().displayAccelerometerReading(msg.getData().getFloatArray(Constants.KEY.ACCELEROMETER_READING));
                    break;
                }
                case Constants.MESSAGE.BATTERY_LEVEL:
                {
                    mMainActivity.get().updateBatteryLevel(msg.getData().getInt(Constants.KEY.BATTERY_LEVEL));
                    break;
                }
                case Constants.MESSAGE.CONNECTING:
                {
                    mMainActivity.get().showConnectingDialog();
                    break;
                }
                case Constants.MESSAGE.CONNECTED:
                {
                    mMainActivity.get().cancelConnectingDialog();
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ProgressDialog connectDialog;

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
    protected void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }

        super.onStop();
    }

    private void cancelConnectingDialog() {
        connectDialog.dismiss();
    }

    /**
     * Connection with the service
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //TODO: Change mService to a list to contain multiple services, try using log cat with className to check this holds for DataReceiverService
            mService = new Messenger(service);
            //updateStatus("Attached to the sensor service.");
            mIsBound = true;
            try {
                Message msg = Message.obtain(null, Constants.MESSAGE.REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mIsBound = false;
            mService = null;
            //updateStatus("Disconnected from the sensor service.");
        }
    };

    /**
     * Binds the activity to the background service
     */
    private void doBindService() {
        bindService(new Intent(this, SensorService.class), mConnection, Context.BIND_AUTO_CREATE);
        //updateStatus("Binding to Service...");
    }

    /**
     * Unbind this activity from the background service
     */
    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, Constants.MESSAGE.UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            //updateStatus("Unbinding from Service...");
        }
    }

    /**
     * Loads shared user preferences, e.g. whether video/audio is enabled
     */
    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        record_audio = preferences.getBoolean(getString(R.string.pref_audio_key),
                getResources().getBoolean(R.bool.pref_audio_default));
        runServiceOverWearable = false;
//        preferences.getBoolean(getString(R.string.pref_wearable_key),
//                getResources().getBoolean(R.bool.pref_wearable_default)); //TODO uncomment
        mwMacAddress = preferences.getString(getString(R.string.pref_device_key),
                getString(R.string.pref_device_default));
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

    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadPreferences();
        if (!runServiceOverWearable)
            doBindService();

        remoteSensorManager = RemoteSensorManager.getInstance(this);
        //TODO: Is this necessary?
        remoteSensorManager.setRemoteSensorListener(new RemoteSensorManager.RemoteSensorListener() {
            @Override
            public void onMessageResult(String path, byte[] msg, MessageApi.SendMessageResult sendMessageResult) {

            }
        });

        serviceManager = ServiceManager.getInstance(this);

        startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!RecordingService.isRecording){
                    //TODO: Do Android versions prior to M require run-time permission request for overlay?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        requestPermissions();
                    else
                        onPermissionsGranted();
                }else{
                    serviceManager.stopRecordingService();
                }

            }
        });
        if (RecordingService.isRecording)
            startButton.setBackgroundResource(android.R.drawable.ic_media_pause);
//        Button stopButton = (Button) findViewById(R.id.stop_button);
//        assert stopButton != null;
//        stopButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stopServices();
//            }
//        });
        txtAccelerometer = ((TextView) findViewById(R.id.sensor_readings));
        txtAccelerometer.setText(String.format(getString(R.string.initial_sensor_readings), 0f, 0f, 0f));
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);

        if (mwMacAddress.equals(getString(R.string.pref_device_default))){
            startActivityForResult(new Intent(MainActivity.this, SelectMetawearActivity.class), SELECT_DEVICE_REQUEST_CODE);
        }
        startMetawearService();
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

    /**
     * Stops all ongoing services
     */
    private void stopServices(){
        serviceManager.stopDataWriterService();
        stopMetawearService();
    }

    private void startMetawearService(){
        if (runServiceOverWearable)
            remoteSensorManager.startMetawearService();
        else
            serviceManager.startMetawearService(); //CE:03:BF:17:58:41 //DC:00:25:17:8E:CF //F6:8D:FC:1A:E4:50
    }

    @Override
    public void onDestroy() {
        doUnbindService();
        super.onDestroy();
    }

    private void stopMetawearService(){
        if (runServiceOverWearable){
            remoteSensorManager.stopMetawearService();
        }else{
            serviceManager.stopMetawearService();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WINDOW_OVERLAY_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /** if so check once again if we have permission */
                if (Settings.canDrawOverlays(this)) {
                    onPermissionsGranted();
                }
            }
        }else if (requestCode == Constants.ACTION.REQUEST_SET_PREFERENCES){
            loadPreferences();
            if (runServiceOverWearable)
                doUnbindService();
            else
                doBindService();
        }else if (requestCode == SELECT_DEVICE_REQUEST_CODE){
            if (data != null) {
                mwMacAddress = data.getStringExtra(SharedConstants.KEY.UUID);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(getString(R.string.pref_device_key), mwMacAddress);
                editor.apply();
            }else{
                finish(); //can't return to the main UI if there is no device available
            }
        }
    }

    /**
     * Displays a single accelerometer reading on the main UI
     * @param reading a 3-dimensional floating point vector representing the x, y and z accelerometer values respectively.
     */
    private void displayAccelerometerReading(final float[] reading){
        float x = reading[0];
        float y = reading[1];
        float z = reading[2];
        final String output = String.format(getString(R.string.initial_sensor_readings), x, y, z);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtAccelerometer.setText(output);
            }
        });
    }

    private Bitmap batteryLevelBitmap;

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
                ActionBar actionBar = getSupportActionBar();
                assert actionBar != null;
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
            startActivityForResult(openSettings, Constants.ACTION.REQUEST_SET_PREFERENCES, null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            /** if not construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, WINDOW_OVERLAY_REQUEST);
        }else{
            onPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) {
                    //updateStatus("Permission Request Cancelled.");
                    return;
                }
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
     * Request required permissions, depending on the application settings. Permissions
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * {@link android.Manifest.permission#BLUETOOTH BLUETOOTH},
     * {@link android.Manifest.permission#BLUETOOTH_ADMIN BLUETOOTH_ADMIN},
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION}
     * are always required, because the data collection from the Bean cannot work without
     * these permissions. If video recording is enabled, then additionally the
     * {@link android.Manifest.permission#CAMERA CAMERA} permission is required. For audio
     * recording, which is disabled by default, the
     * {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is required.
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
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_REQUEST);
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
                    float[] values = intent.getFloatArrayExtra(Constants.KEY.SENSOR_DATA);
                    float[] averages = new float[3];
                    for (int i = 0; i < values.length; i++) {
                        averages[i % 3] += values[i];
                    }
                    for (int j = 0; j < averages.length; j++) {
                        averages[j] /= (values.length / 3f);
                    }
                    SharedConstants.SENSOR_TYPE sensorType = DataLayerUtil.deserialize(SharedConstants.SENSOR_TYPE.class).from(intent);
                    if (sensorType == SharedConstants.SENSOR_TYPE.ACCELEROMETER_METAWEAR){
                        displayAccelerometerReading(averages);
                    }
                }else if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(SharedConstants.KEY.MESSAGE, -1);
                    if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTING){
                        showStatus("Listening for movement...");
                        cancelConnectingDialog();
                        showConnectingDialog();
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTED){
                        showStatus("Connected to pill bottle.");
                        cancelConnectingDialog();
//                        remoteSensorManager.startSensorService(); //TODO uncomment
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_DISCONNECTED) {
                        serviceManager.stopDataWriterService();
//                        remoteSensorManager.stopSensorService(); //TODO uncomment
                    } else if (message == SharedConstants.MESSAGES.RECORDING_SERVICE_STARTED){
                        startButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                    } else if (message == SharedConstants.MESSAGES.RECORDING_SERVICE_STOPPED){
                        startButton.setBackgroundResource(android.R.drawable.ic_media_play);
                    }
                }
            }
        }
    };
}
