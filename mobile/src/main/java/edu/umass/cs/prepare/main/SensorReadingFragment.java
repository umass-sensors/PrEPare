package edu.umass.cs.prepare.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.tutorial.StandardTutorial;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;

// In this case, the fragment displays simple text based on the page
public class SensorReadingFragment extends Fragment {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorReadingFragment.class.getName();

    /** List of formatted sensor readings outputs **/
    private ArrayList<String> sensorReadings;

    /** List of sensors associated with each reading **/
    private ArrayList<String> sensors;

    /** List of devices associated with each reading **/
    private ArrayList<String> devices;

    /** Links the {@link #sensorReadings} to a UI view. **/
    private CustomAdapter sensorReadingAdapter;

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_SENSOR_DATA);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    private int accelerometerSamplingRate, gyroscopeSamplingRate, rssiSamplingRate;

    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        accelerometerSamplingRate = Integer.parseInt(preferences.getString(getString(edu.umass.cs.shared.R.string.pref_accelerometer_sampling_rate_key),
                getString(edu.umass.cs.shared.R.string.pref_accelerometer_sampling_rate_default)));
        gyroscopeSamplingRate = Integer.parseInt(preferences.getString(getString(edu.umass.cs.shared.R.string.pref_gyroscope_sampling_rate_key),
                getString(edu.umass.cs.shared.R.string.pref_gyroscope_sampling_rate_default)));
        rssiSamplingRate = Integer.parseInt(preferences.getString(getString(edu.umass.cs.shared.R.string.pref_rssi_sampling_rate_key),
                getString(edu.umass.cs.shared.R.string.pref_rssi_sampling_rate_default)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorReadings = new ArrayList<>();
        devices = new ArrayList<>();
        sensors = new ArrayList<>();
        for (int i = 0; i < SharedConstants.SENSOR_TYPE.values().length-2; i++) {
            SharedConstants.SENSOR_TYPE sensor_type = SharedConstants.SENSOR_TYPE.values()[i];
            sensorReadings.add("--");
            devices.add(sensor_type.getDevice());
            sensors.add(sensor_type.getSensor());

            Log.d(TAG, sensor_type.getDevice());
            Log.d(TAG, sensor_type.getSensor());
            Log.d(TAG, sensor_type.name());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_sensor_readings, container, false);

        sensorReadingAdapter = new CustomAdapter(getActivity(), sensors, devices, sensorReadings);
        sensorDataList = (ListView) view.findViewById(R.id.lv_sensor_readings);
        assert sensorDataList != null;
        sensorDataList.setAdapter(sensorReadingAdapter);
        sensorReadingAdapter.notifyDataSetChanged();
        sensorReadingAdapter.setOnRowClickedListener(new CustomAdapter.OnRowClickedListener() {
            @Override
            public void onRowClicked(int row) {
                String sensor = sensors.get(row);
                String device = devices.get(row);

                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.dialog_layout, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(dialogLayout);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                alertDialog.getWindow().setLayout(1000, 800);

                TextView txtDevice = (TextView) dialogLayout.findViewById(R.id.device);
                TextView txtSensor = (TextView) dialogLayout.findViewById(R.id.sensor);
                TextView txtSamplingRate = (TextView) dialogLayout.findViewById(R.id.samplingRate);

                txtDevice.setText("Device: " + device);
                txtSensor.setText("Sensor: " + sensor);
                txtSamplingRate.setText("Sampling Rate: " + getSamplingRate(sensor, device));

//                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
//                alertDialog.setTitle(sensor);
//                alertDialog.setMessage(message);
//                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//                            }
//                        });
//
//                LayoutInflater inflater = getActivity().getLayoutInflater();
//                FrameLayout f1 = (FrameLayout) alertDialog.findViewById(android.R.id.body);
//                f1.addView(inflater.inflate(R.layout.dialog_view, f1, false));
//                alertDialog.show();
            }
        });

        return view;
    }

    private int getSamplingRate(String sensor, String device){
        loadPreferences();
        if (device.equals("Metawear") && sensor.equals("Accelerometer")){
            return accelerometerSamplingRate;
        }else if (device.equals("Metawear") && sensor.equals("Gyroscope")){
            return gyroscopeSamplingRate;
        }else if (sensor.equals("RSSI")){
            return rssiSamplingRate;
        }else
            return 60; //TODO
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private StandardTutorial tutorial;

    void showTutorial(final ViewPager viewPager){
        View listItem = getViewByPosition(0, sensorDataList);
        View icon = sensorReadingAdapter.imageView;
        tutorial = new StandardTutorial(getActivity(), listItem, getString(R.string.tutorial_sensor_data), getString(R.string.tutorial_next),
        new StandardTutorial(getActivity(), icon, getString(R.string.tutorial_device_icon), getString(R.string.tutorial_next), null));
        tutorial.setTutorialListener(new StandardTutorial.TutorialListener() {
                    @Override
                    public void onReady(final StandardTutorial tutorial) {
                        tutorial.showTutorial();
                    }

                    @Override
                    public void onFinish(StandardTutorial tutorial) {
                        viewPager.setCurrentItem(2, true);
                    }
                });
    }

    void dismissTutorial(){
        tutorial.dismiss();
    }

    private ListView sensorDataList;

    /**
     * Displays a single accelerometer reading on the main UI
     * @param reading a 3-dimensional floating point vector representing the x, y and z accelerometer values respectively.
     */
    private void displaySensorReading(final SharedConstants.SENSOR_TYPE sensorType, final float[] reading){
        final String output;
        if (sensorType != SharedConstants.SENSOR_TYPE.RSSI) {
            float x = reading[0];
            float y = reading[1];
            float z = reading[2];
            output = String.format(getString(R.string.initial_sensor_readings), x, y, z);
        } else {
            float rssi = reading[0];
            output = rssi + " dBm";
        }
        Log.d(TAG, output);
        sensorDataList.setAdapter(sensorReadingAdapter);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sensorReadings.set(sensorType.ordinal(), output);
                sensorReadingAdapter.notifyDataSetChanged();
            }
        });
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_SENSOR_DATA)) {
                    SharedConstants.SENSOR_TYPE sensorType = DataLayerUtil.deserialize(SharedConstants.SENSOR_TYPE.class).from(intent);
                    float[] values = intent.getFloatArrayExtra(Constants.KEY.SENSOR_DATA);
                    if (sensorType == SharedConstants.SENSOR_TYPE.BATTERY_METAWEAR){
                        //updateBatteryLevel((int)values[0]);
                        return;
                    }

                    float[] averages = new float[3];
                    for (int i = 0; i < values.length; i++) {
                        averages[i % 3] += values[i];
                    }
                    for (int j = 0; j < averages.length; j++) {
                        averages[j] /= (values.length / 3f);
                    }
                    displaySensorReading(sensorType, averages);
                }
            }
        }
    };
}