package edu.umass.cs.prepare.view.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Locale;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.view.tools.BatteryStatusActionProvider;
import edu.umass.cs.prepare.view.fragments.RecordingFragment;
import edu.umass.cs.prepare.view.fragments.SensorReadingFragment;
import edu.umass.cs.prepare.view.fragments.SettingsFragment;
import edu.umass.cs.prepare.view.tools.ConnectionStatusActionProvider;
import edu.umass.cs.prepare.view.tutorial.ConnectionStatusTutorial;
import edu.umass.cs.prepare.view.tutorial.StandardTutorial;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;
import edu.umass.cs.shared.util.BatteryUtil;

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

    private static int COLOR_ANIMATION_DURATION_MILLIS = 3000;

    /** Request identifiers **/
    public interface REQUEST_CODE {
        int RECORDING = 1;
        int WINDOW_OVERLAY = 2;
        int SELECT_DEVICE = 3;
        int ENABLE_BLUETOOTH = 5;
    }

    /**
     * Defines all available tabs in the main UI
     */
    public enum PAGES {
        SENSOR_DATA {
            @Override
            public String getTitle() {
                return "Data";
            }

            @Override
            public int getPageNumber() {
                return 0;
            }
        },
        RECORDING {
            @Override
            public String getTitle() {
                return "Recording";
            }

            @Override
            public int getPageNumber() {
                return 1;
            }
        },
        SETTINGS {
            @Override
            public String getTitle() {
                return "Settings";
            }

            @Override
            public int getPageNumber() {
                return 2;
            }
        };

        /**
         * Indicates the title of the page. This will be displayed in the tab.
         * Default is an empty string. Override this to return a different title.
         * @return the page title
         */
        public String getTitle(){
            return "";
        }

        /**
         * Indicates the page number of the page. If omitted, it will return
         * its position in the enum. Override this to specify a different page number.
         * @return the page number
         */
        public int getPageNumber(){
            return ordinal();
        }

        /**
         * Returns the number of pages available.
         * @return the length of {@link #values()}
         */
        static int getCount(){
            return values().length;
        }
        static final int NONE = -1;
    }

    /** Handles services on the mobile application. */
    private ServiceManager serviceManager;

    /** Displays status messages, e.g. connection station. **/
    private TextView txtStatus;

    /** Handle to the sensor data tab. **/
    private View sensorTab;

    /** Maintains the tabs and the tab layout interactions. **/
    private ViewPager viewPager;

    /** The selected tab. **/
    private int selectedPage = PAGES.NONE;

    /** The fragment within the sensor data tab. **/
    private SensorReadingFragment sensorReadingFragment;

    /** The tutorial sequence entry point. **/
    private StandardTutorial tutorial;

    /** The view which displays the battery level in the toolbar. **/
    private View batteryStatusView;

    /** The view which displays the network connection status in the toolbar. **/
    private View networkStatusView;

    /** The view which displays the wearable connection status in the toolbar. **/
    private View wearableStatusView;

    /** The view which displays the pill bottle connection status in the toolbar. **/
    private View metawearStatusView;

    /** Animates the sensor data tab when the device connects. **/
    private ValueAnimator colorAnimation;

    /** Simplified application preference manager. **/
    private ApplicationPreferences applicationPreferences;

    /** A handle to the battery level view shown in the main toolbar. **/
    private BatteryStatusActionProvider batteryStatusActionProvider;

    /** Handle to the action provider that displays the Metawear connection status. **/
    private ConnectionStatusActionProvider metawearStatusActionProvider;

    /** Handle to the action provider that displays the Android wear connection status. **/
    public ConnectionStatusActionProvider wearableStatusActionProvider;

    /** Handle to the action provider that displays the server connection status. **/
    public ConnectionStatusActionProvider networkStatusActionProvider;

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
        serviceManager.enableCameraReminder();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        sensorReadingFragment = new SensorReadingFragment();
        final RecordingFragment recordingFragment = new RecordingFragment();
        final SettingsFragment settingsFragment = new SettingsFragment();

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
            private final String[] tabTitles = new String[PAGES.getCount()];
            //instance initializer:
            {
                for (PAGES page : PAGES.values())
                    tabTitles[page.getPageNumber()] = page.getTitle();
            }

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
                // do nothing
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
                        if (selectedPage != PAGES.NONE && applicationPreferences.showTutorial()) {
                            if (selectedPage != PAGES.SENSOR_DATA.getPageNumber()) {
                                if (selectedPage == PAGES.RECORDING.getPageNumber()) {
                                    recordingFragment.showTutorial(viewPager);
                                } else if (selectedPage == PAGES.SETTINGS.getPageNumber()) {
                                    settingsFragment.showTutorial();
                                }
                            }
                            selectedPage = PAGES.NONE;
                        }
                        if (selectedPage == PAGES.SENSOR_DATA.getPageNumber()) {
                            if (colorAnimation != null)
                                colorAnimation.cancel();
                            sensorTab.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorTabHighlightInitial));
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
        sensorTab = ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(PAGES.SENSOR_DATA.getPageNumber());
        View recordingTab = ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(PAGES.RECORDING.getPageNumber());
        View settingsTab = ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(PAGES.SETTINGS.getPageNumber());
        sensorTab.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTabHighlightInitial));
        recordingTab.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTabHighlightInitial));
        settingsTab.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTabHighlightInitial));
        tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTabHighlightInitial));

        serviceManager = ServiceManager.getInstance(this);
        applicationPreferences = ApplicationPreferences.getInstance(this);

        txtStatus = (TextView) findViewById(R.id.status);

        // change tab if specified
        if (getIntent() != null) {
            int pageNumber = getIntent().getIntExtra(Constants.KEY.PAGE_INDEX, PAGES.SENSOR_DATA.getPageNumber());
            viewPager.setCurrentItem(pageNumber);
        }
    }

    /**
     * Starts the toolbar tutorial
     */
    private void startTutorial(){
        if (!applicationPreferences.showTutorial()) return;
        tutorial = new StandardTutorial(MainActivity.this, batteryStatusView)
                .setDescription(getString(R.string.tutorial_battery_status))
                .enableButton(false).setTutorialListener(new StandardTutorial.TutorialListener() {
            @Override
            public void onReady(StandardTutorial tutorial) {
                tutorial.showTutorial();
            }

            @Override
            public void onComplete(StandardTutorial tutorial) {
                continueTutorial();
            }
        }).build();
    }

    private void continueTutorial(){
        if (!applicationPreferences.showTutorial()) return;
        StandardTutorial metawearStatusTutorial = new ConnectionStatusTutorial(MainActivity.this, metawearStatusView)
                .setConnectedIcon(R.drawable.ic_pill_white_24dp)
                .setDisconnectedIcon(R.drawable.ic_pill_white_24dp)
                .setErrorIcon(R.drawable.ic_pill_error_white_24dp)
                .setDisabledIcon(R.drawable.ic_pill_off_white_24dp)
                .setDescription(getString(R.string.tutorial_metawear_status))
                .setButtonText(getString(R.string.tutorial_next));
        StandardTutorial wearableStatusTutorial = new ConnectionStatusTutorial(MainActivity.this, wearableStatusView)
                .setConnectedIcon(R.drawable.ic_watch_white_24dp)
                .setDisconnectedIcon(R.drawable.ic_watch_white_24dp)
                .setErrorIcon(R.drawable.ic_watch_error_white_24dp)
                .setDisabledIcon(R.drawable.ic_watch_off_white_24dp)
                .setDescription(getString(R.string.tutorial_wearable_status))
                .setButtonText(getString(R.string.tutorial_next));
        StandardTutorial networkStatusTutorial = new ConnectionStatusTutorial(MainActivity.this, networkStatusView)
                .setConnectedIcon(R.drawable.ic_cloud_done_white_24dp)
                .setDisconnectedIcon(R.drawable.ic_cloud_white_24dp)
                .setErrorIcon(R.drawable.ic_cloud_error_white_24dp)
                .setDisabledIcon(R.drawable.ic_cloud_off_white_24dp)
                .setDescription(getString(R.string.tutorial_network_status))
                .setButtonText(getString(R.string.tutorial_next));
        metawearStatusTutorial.setTutorialListener(new StandardTutorial.TutorialListener() {
                    @Override
                    public void onReady(StandardTutorial tutorial) {
                        tutorial.showTutorial();
                    }

                    @Override
                    public void onComplete(StandardTutorial tutorial) {
                        sensorReadingFragment.showTutorial(viewPager);
                    }
                });
        StandardTutorial.buildSequence(metawearStatusTutorial, wearableStatusTutorial, networkStatusTutorial);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE.SELECT_DEVICE){
            if (data != null) {
                String address = data.getStringExtra(SharedConstants.KEY.UUID);
                applicationPreferences.edit().putString(getString(R.string.pref_device_key), address).apply();
                if (applicationPreferences.showTutorial())
                    startTutorial();
                else
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                batteryStatusActionProvider.updateBatteryStatus(percentage);
            }
        });
        applicationPreferences.edit().putInt(getString(R.string.pref_battery_level_key), percentage).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        batteryStatusActionProvider = (BatteryStatusActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_battery_status));
        batteryStatusView = MenuItemCompat.getActionView(menu.findItem(R.id.action_battery_status));
        batteryStatusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.dialog_battery_status, (ViewGroup) findViewById(android.R.id.content), false);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(dialogLayout);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (tutorial != null)
                            tutorial.dismiss();
                    }
                });
                alertDialog.show();

                TextView txtBatteryLevel = (TextView) dialogLayout.findViewById(R.id.batteryLevel);
                TextView txtLifetimeEstimate = (TextView) dialogLayout.findViewById(R.id.estimatedLifetime);
                Button buttonOK = (Button) dialogLayout.findViewById(R.id.buttonOK);

                txtBatteryLevel.setText(String.format(Locale.getDefault(), getString(R.string.battery_level), applicationPreferences.getBatteryLevel()));
                txtLifetimeEstimate.setText(String.format(Locale.getDefault(), getString(R.string.estimated_lifetime),
                        new DecimalFormat("#.##").format(BatteryUtil.getBatteryLifetimeEstimate(applicationPreferences.getBatteryLevel()))));
                buttonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.cancel();
                    }
                });
            }
        });

        wearableStatusActionProvider = (ConnectionStatusActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_connection_status));
        wearableStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.DEFAULT,
                R.drawable.ic_watch_white_24dp);
        wearableStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.CONNECTED,
                R.drawable.ic_watch_white_24dp);
        wearableStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED,
                R.drawable.ic_watch_off_white_24dp);
        wearableStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR,
                R.drawable.ic_watch_error_white_24dp);
        wearableStatusView = MenuItemCompat.getActionView(menu.findItem(R.id.action_connection_status));
        if (applicationPreferences.useAndroidWear()) {
            serviceManager.queryWearableState();
        }else {
            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
        }
        wearableStatusActionProvider.setOnClickListener(new ConnectionStatusActionProvider.OnClickListener() {
            @Override
            public void onClick(ConnectionStatusActionProvider.CONNECTION_STATUS state) {
                final String info;
                switch (state){
                    case DISABLED:
                        info = "Android Wear is disabled. Go to settings to re-enable it.";
                        break;
                    case DISCONNECTED:
                        info = "The wearable is connected but the wearable service is not running.";
                        break;
                    case CONNECTED:
                        info = "Connected to the wearable and collecting data.";
                        break;
                    case ERROR:
                        info = "Failed to connect to wearable. Make sure that Bluetooth is enabled and your band is configured.";
                        break;
                    case DEFAULT:
                        info = "Default icon..."; //TODO
                        break;
                    default:
                        return;
                }
                Toast toast = Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG);
                int[] location = new int[2];
                wearableStatusView.getLocationOnScreen(location);
                toast.setGravity(Gravity.TOP|Gravity.START, location[0], location[1]);
                toast.show();
            }
        });
        wearableStatusActionProvider.setContentDescription(getString(R.string.content_description_wearable_status));

        metawearStatusActionProvider = (ConnectionStatusActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_metawear_status));
        metawearStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.DEFAULT,
                R.drawable.ic_pill_white_24dp);
        metawearStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.CONNECTED,
                R.drawable.ic_pill_white_24dp);
        metawearStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED,
                R.drawable.ic_pill_off_white_24dp);
        metawearStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR,
                R.drawable.ic_pill_error_white_24dp);
        metawearStatusView = MenuItemCompat.getActionView(menu.findItem(R.id.action_metawear_status));
        if (applicationPreferences.enablePillBottle()){
            serviceManager.queryMetawearState();
        }else{
            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
        }
        metawearStatusActionProvider.setOnClickListener(new ConnectionStatusActionProvider.OnClickListener() {
            @Override
            public void onClick(ConnectionStatusActionProvider.CONNECTION_STATUS state) {
                final String info;
                switch (state){
                    case DISABLED:
                        info = "Pill bottle is disabled. Go to settings to re-enable it.";
                        break;
                    case DISCONNECTED:
                        info = "Waiting for motion before connecting to the pill bottle.";
                        break;
                    case CONNECTED:
                        info = "Connected to the pill bottle and collecting data.";
                        break;
                    case ERROR:
                        info = "Failed to connect to pill bottle. Make sure that Bluetooth is enabled and your pill bottle battery is in.";
                        break;
                    case DEFAULT:
                        info = "Default icon..."; //TODO
                        break;
                    default:
                        return;
                }
                Toast toast = Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG);
                int[] location = new int[2];
                metawearStatusView.getLocationOnScreen(location);
                toast.setGravity(Gravity.TOP|Gravity.START, location[0], location[1]);
                toast.show();
            }
        });
        metawearStatusActionProvider.setContentDescription(getString(R.string.content_description_metawear_status));

        networkStatusActionProvider = (ConnectionStatusActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_network_status));
        networkStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.DEFAULT,
                R.drawable.ic_cloud_white_24dp);
        networkStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.CONNECTED,
                R.drawable.ic_cloud_done_white_24dp);
        networkStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED,
                R.drawable.ic_cloud_off_white_24dp);
        networkStatusActionProvider.setDrawable(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR,
                R.drawable.ic_cloud_error_white_24dp);
        networkStatusView = MenuItemCompat.getActionView(menu.findItem(R.id.action_network_status));
        if (applicationPreferences.writeServer()) {
            serviceManager.queryNetworkState();
        } else {
            networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
        }
        networkStatusActionProvider.setOnClickListener(new ConnectionStatusActionProvider.OnClickListener() {
            @Override
            public void onClick(ConnectionStatusActionProvider.CONNECTION_STATUS state) {
                final String info;
                switch (state){
                    case DISABLED:
                        info = "Server connection is disabled. Go to settings to re-enable it.";
                        break;
                    case DISCONNECTED:
                        info = "Waiting for incoming data before connecting to the server.";
                        break;
                    case CONNECTED:
                        info = "Writing data to server at " + applicationPreferences.getIpAddress();
                        break;
                    case ERROR:
                        info = "Server connection failed. Make sure the IP address is correct and the server is up.";
                        break;
                    case DEFAULT:
                        info = "Default icon..."; //TODO
                        break;
                    default:
                        return;
                }
                Toast toast = Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG);
                int[] location = new int[2];
                networkStatusView.getLocationOnScreen(location);
                toast.setGravity(Gravity.TOP|Gravity.START, location[0], location[1]);
                toast.show();
            }
        });
        networkStatusActionProvider.setContentDescription(getString(R.string.content_description_network_status));

        if (applicationPreferences.getMwAddress().equals(getString(R.string.pref_device_default))){
            startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
        } else if (!applicationPreferences.showTutorial()) {
            serviceManager.startMetawearService();
        } else {
            startTutorial();
        }
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
                    switch (message){
                        case SharedConstants.MESSAGES.METAWEAR_CONNECTING:
                            showStatus(getString(R.string.status_connection_attempt));
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                            if (applicationPreferences.writeServer())
                                networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                            break;
                        case SharedConstants.MESSAGES.METAWEAR_SERVICE_STOPPED:
                            showStatus(getString(R.string.status_service_stopped));
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
                            //wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
                            //networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
                            break;
                        case SharedConstants.MESSAGES.METAWEAR_CONNECTED:
                            showStatus(getString(R.string.status_connected));
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.CONNECTED);
                            if (viewPager.getCurrentItem() != PAGES.SENSOR_DATA.getPageNumber())
                                highlightTab();
                            break;
                        case SharedConstants.MESSAGES.METAWEAR_DISCONNECTED:
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                            if (colorAnimation != null)
                                colorAnimation.cancel();
                            break;
                        case SharedConstants.MESSAGES.WEARABLE_SERVICE_STARTED:
                            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.CONNECTED);
                            break;
                        case SharedConstants.MESSAGES.WEARABLE_SERVICE_STOPPED:
                            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                            break;
                        case SharedConstants.MESSAGES.WEARABLE_CONNECTION_FAILED:
                            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            break;
                        case SharedConstants.MESSAGES.WEARABLE_DISCONNECTED:
                            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            break;
                        case SharedConstants.MESSAGES.NO_MOTION_DETECTED:
                            showStatus(getString(R.string.status_no_motion));
                            break;
                        case SharedConstants.MESSAGES.INVALID_ADDRESS:
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            showStatus(getString(R.string.status_invalid_address));
                            startActivityForResult(new Intent(MainActivity.this, SelectDeviceActivity.class), REQUEST_CODE.SELECT_DEVICE);
                            break;
                        case SharedConstants.MESSAGES.BLUETOOTH_UNSUPPORTED:
                            showStatus(getString(R.string.status_bluetooth_unsupported));
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            break;
                        case SharedConstants.MESSAGES.BLUETOOTH_DISABLED:
                            showStatus(getString(R.string.status_bluetooth_disabled));
                            metawearStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_CODE.ENABLE_BLUETOOTH);
                            break;
                        case SharedConstants.MESSAGES.SERVER_CONNECTION_FAILED:
                            showStatus(getString(R.string.status_server_connection_failed));
                            networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.ERROR);
                            break;
                        case SharedConstants.MESSAGES.SERVER_CONNECTION_SUCCEEDED:
                            networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.CONNECTED);
                            break;
                        case SharedConstants.MESSAGES.SERVER_DISCONNECTED:
                            if (applicationPreferences.writeServer())
                                networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                            else
                                networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
                            break;
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

    /**
     * Highlights the sensor data tab repeatedly using a color animation.
     * To stop the animation use {@link ValueAnimator#cancel() colorAnimation.cancel()}
     */
    private void highlightTab(){
        int colorFrom = ContextCompat.getColor(this, R.color.colorTabHighlightInitial);
        int colorTo = ContextCompat.getColor(this, R.color.colorTabHighlightFinal);
        colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo, colorFrom);
        colorAnimation.setDuration(COLOR_ANIMATION_DURATION_MILLIS); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                sensorTab.setBackgroundColor((int) animator.getAnimatedValue());
            }

        });
        colorAnimation.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimation.start();
    }
}
