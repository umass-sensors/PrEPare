package edu.umass.cs.prepare.main;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.communication.wearable.RemoteSensorManager;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.metawear.SelectDeviceActivity;
import edu.umass.cs.prepare.metawear.SensorService;
import edu.umass.cs.prepare.preferences.SettingsActivity;
import edu.umass.cs.shared.constants.SharedConstants;

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

    /** whether video recording should include audio **/
    boolean record_audio;

    boolean showTutorial;

    /** Indicates whether the {@link SensorService} should run over the wearable or mobile device. **/
    private boolean runServiceOverWearable;

    /** Indicates whether the {@link SensorService} is turned on. **/
    private boolean serviceEnabled;

    /** Request identifiers **/
    interface REQUEST_CODE {
        int RECORDING = 1;
        int WINDOW_OVERLAY = 2;
        int SELECT_DEVICE = 3;
        int SET_PREFERENCES = 4;
        int ENABLE_BLUETOOTH = 5;
    }

    /** The sensor manager which handles sensors on the wearable device remotely */
    private RemoteSensorManager remoteSensorManager;

    /** Handles services on the mobile application. */
    private ServiceManager serviceManager;

    /** The unique address of the Metawear device. **/
    private String mwAddress;

    /** Image corresponding to the current Metawear battery level. **/
    private Bitmap batteryLevelBitmap;

    /** The action bar at the top of the main UI **/
    private ActionBar actionBar;

    private TextView txtStatus;

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (viewPager.getCurrentItem() == 2)
            serviceManager.maximizeVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        serviceManager.minimizeVideo();
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
        showTutorial = preferences.getBoolean(getString(R.string.pref_show_tutorial_key),
                getResources().getBoolean(R.bool.pref_show_tutorial_default));
    }

    ViewPager viewPager;

    private int selectedPage = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SensorReadingFragment sensorReadingFragment = new SensorReadingFragment();
        final RecordingFragment recordingFragment = new RecordingFragment();
        final SettingsFragment settingsFragment = new SettingsFragment();

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
            private final String[] tabTitles = new String[]{"Settings", "Data", "Recording"};

            @Override
            public android.app.Fragment getItem(int position) {
                if (position == 0)
                    return settingsFragment;
                else if (position == 1)
                    return sensorReadingFragment;
                else
                    return recordingFragment;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                // Generate title based on item position
                return tabTitles[position];
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                selectedPage = position;
                if (position == 2){
                    serviceManager.maximizeVideo();
                }else{
                    serviceManager.minimizeVideo();
                }
//                if (position == 0) {
//
//                }else if (position == 1) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    sensorReadingFragment.showTutorial(viewPager);
//                }else {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    recordingFragment.showTutorial(viewPager);
//                }
                //sensorReadingFragment.dismissTutorial();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        loadPreferences();
                        if (selectedPage != -1 && showTutorial) {
                            if (selectedPage == 0){
                                //settingsFragment.showTutorial(viewPager);
                            } else if (selectedPage == 1){
                                sensorReadingFragment.showTutorial(viewPager);
                            } else {
                                recordingFragment.showTutorial(viewPager);
                            }
                            selectedPage = -1;
                        }
                        break;
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        break;
                    case ViewPager.SCROLL_STATE_SETTLING:
                        break;
                }
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);

        loadPreferences();
        remoteSensorManager = RemoteSensorManager.getInstance(this);
        serviceManager = ServiceManager.getInstance(this);

        actionBar = getSupportActionBar();

//        if (mwAddress.equals(getString(R.string.pref_device_default))){
//            startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
//        } else {
//            startMetawearService();
//        }
        settingsFragment.setViewPager(viewPager);

        txtStatus = (TextView) findViewById(R.id.status);
    }

    private void startMetawearService(){
        if (!serviceEnabled) {
            showStatus(getString(R.string.status_service_stopped));
            return;
        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE.SET_PREFERENCES){
            boolean serviceEnabledBefore = serviceEnabled;
            loadPreferences();
            if (serviceEnabledBefore != serviceEnabled){
                if (serviceEnabled)
                    startMetawearService();
                else {
                    stopMetawearService();
                    serviceManager.stopDataWriterService();
                    showStatus(getString(R.string.status_service_stopped));
                }
            }

        }else if (requestCode == REQUEST_CODE.SELECT_DEVICE){
            if (data != null) {
                mwAddress = data.getStringExtra(SharedConstants.KEY.UUID);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(getString(R.string.pref_device_key), mwAddress);
                editor.apply();
                startMetawearService();
                //showTutorial();
            }else{
                finish(); //can't return to the main UI if there is no device available
            }
        }
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
     * Shows a removable status message at the bottom of the application.
     * @param message the status message shown
     */
    void showStatus(String message){
        txtStatus.setText(message);
//        View mainUI = MainActivity.this.findViewById(R.id.fragment);
//        assert mainUI != null;
//        Snackbar snack = Snackbar.make(mainUI, message, Snackbar.LENGTH_INDEFINITE);
//        View view = snack.getView();
//        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
//        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        snack.show();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(SharedConstants.KEY.MESSAGE, -1);
                    if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTING){
                        showStatus(getString(R.string.status_connection_attempt));
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_CONNECTED){
                        showStatus(getString(R.string.status_connected));
                        remoteSensorManager.startSensorService();
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_DISCONNECTED) {
                        serviceManager.stopDataWriterService();
                        HandlerThread hThread = new HandlerThread("StopWearableSensorServiceThread");
                        hThread.start();
                        Handler stopServiceHandler = new Handler(hThread.getLooper());
                        stopServiceHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                remoteSensorManager.stopSensorService();
                            }
                        }, 5000);
                    } else if (message == SharedConstants.MESSAGES.INVALID_ADDRESS){
                        showStatus(getString(R.string.status_invalid_address));
                        startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
                    } else if (message == SharedConstants.MESSAGES.BLUETOOTH_DISABLED){
                        showStatus(getString(R.string.status_bluetooth_disabled));
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_CODE.ENABLE_BLUETOOTH);
                    } else if (message == SharedConstants.MESSAGES.BLUETOOTH_UNSUPPORTED){
                        showStatus(getString(R.string.status_bluetooth_unsupported));
                    }
                }
            }
        }
    };
}
