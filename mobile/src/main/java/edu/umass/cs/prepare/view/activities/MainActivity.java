package edu.umass.cs.prepare.view.activities;

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
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.TextView;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.metawear.SelectDeviceActivity;
import edu.umass.cs.prepare.recording.RecordingFragment;
import edu.umass.cs.prepare.view.fragments.SensorReadingFragment;
import edu.umass.cs.prepare.view.fragments.SettingsFragment;
import edu.umass.cs.shared.communication.DataLayerUtil;
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

    boolean showTutorial;

    /** Request identifiers **/
    public interface REQUEST_CODE {
        int RECORDING = 1;
        int WINDOW_OVERLAY = 2;
        int SELECT_DEVICE = 3;
        int ENABLE_BLUETOOTH = 5;
    }

    public enum PAGES {
        SETTINGS,
        SENSOR_DATA,
        RECORDING;
        public int getPageNumber(){
            return ordinal();
        }
        static int getCount(){
            return PAGES.values().length;
        }
        static final int NONE = -1;
    }

    /** Handles services on the mobile application. */
    private ServiceManager serviceManager;

    /** The unique address of the Metawear device. **/
    private String mwAddress;

    /** Image corresponding to the current Metawear battery level. **/
    private Bitmap batteryLevelBitmap;

    private TextView txtStatus;

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        filter.addAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
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
        if (viewPager.getCurrentItem() == PAGES.RECORDING.getPageNumber())
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
        mwAddress = preferences.getString(getString(R.string.pref_device_key),
                getString(R.string.pref_device_default));
        showTutorial = preferences.getBoolean(getString(R.string.pref_show_tutorial_key),
                getResources().getBoolean(R.bool.pref_show_tutorial_default));
    }

    ViewPager viewPager;

    private int selectedPage = PAGES.NONE;

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
                if (position == PAGES.SETTINGS.getPageNumber())
                    return settingsFragment;
                else if (position == PAGES.SENSOR_DATA.getPageNumber())
                    return sensorReadingFragment;
                else
                    return recordingFragment;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return tabTitles[position];
            }

            @Override
            public int getCount() {
                return PAGES.getCount();
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                selectedPage = position;
                if (position == PAGES.RECORDING.getPageNumber()){
                    serviceManager.maximizeVideo();
                }else{
                    serviceManager.minimizeVideo();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_IDLE:
                        loadPreferences();
                        if (selectedPage != PAGES.NONE && showTutorial) {
                            if (selectedPage == PAGES.SENSOR_DATA.getPageNumber()){
                                sensorReadingFragment.showTutorial(viewPager);
                            } else if (selectedPage == PAGES.RECORDING.getPageNumber()) {
                                recordingFragment.showTutorial(viewPager);
                            }
                            selectedPage = PAGES.NONE;
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
        serviceManager = ServiceManager.getInstance(this);

        settingsFragment.setViewPager(viewPager);

        txtStatus = (TextView) findViewById(R.id.status);


//        SharedPreferences.OnSharedPreferenceChangeListener prefListener;
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

//        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//            @Override
//            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//                loadPreferences();
////                if (key.equals(getString(R.string.pref_show_tutorial_key))){
////                    if (!showTutorial){
////                        serviceManager.startMetawearService();
////                    }
////                }
//
//                Log.d(TAG, "Settings key changed: " + key);
////                if(key.equals(getString(R.))
////                    getLoaderManager().restartLoader(LOADER_ID, null, tCallbacks);
//
//            }
//        };
//        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        if (mwAddress.equals(getString(R.string.pref_device_default))){
            startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
        } else if (!showTutorial) {
            serviceManager.startMetawearService();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE.SELECT_DEVICE){
            if (data != null) {
                mwAddress = data.getStringExtra(SharedConstants.KEY.UUID);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(getString(R.string.pref_device_key), mwAddress);
                editor.apply();
                if (!showTutorial)
                    serviceManager.startMetawearService();
            }else{
                finish(); //can't return to the main UI if there is no device available
            }
        }
    }

    /**
     * display the battery level in the action bar
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
                menu.getItem(0).setIcon(icon);
            }
        });

    }

    private Menu menu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    /**
     * Shows a removable status message at the bottom of the application.
     * @param message the status message shown
     */
    public void showStatus(String message){
        txtStatus.setText(message);
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
                        serviceManager.startSensorService();
                    } else if (message == SharedConstants.MESSAGES.METAWEAR_DISCONNECTED) {
                        serviceManager.stopDataWriterService();
                        HandlerThread hThread = new HandlerThread("StopWearableSensorServiceThread");
                        hThread.start();
                        Handler stopServiceHandler = new Handler(hThread.getLooper());
                        stopServiceHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                serviceManager.stopSensorService();
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
                }else if (intent.getAction().equals(Constants.ACTION.BROADCAST_SENSOR_DATA)){
                    SharedConstants.SENSOR_TYPE sensorType = DataLayerUtil.deserialize(SharedConstants.SENSOR_TYPE.class).from(intent);
                    float[] values = intent.getFloatArrayExtra(Constants.KEY.SENSOR_DATA);
                    if (sensorType == SharedConstants.SENSOR_TYPE.BATTERY_METAWEAR){
                        updateBatteryLevel((int)values[0]);
                    }
                }
            }
        }
    };
}
