package edu.umass.cs.prepare.view.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.view.activities.MainActivity;
import edu.umass.cs.prepare.view.tutorial.StandardTutorial;
import edu.umass.cs.prepare.view.SensorDataListAdapter;
import edu.umass.cs.shared.communication.DataLayerUtil;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;

// In this case, the fragment displays simple text based on the page
public class SensorReadingFragment extends Fragment {

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorReadingFragment.class.getName();

    /** List of formatted sensor readings outputs **/
    private final ArrayList<String> sensorReadings = new ArrayList<>();

    /** List of sensors associated with each reading **/
    private final ArrayList<String> sensors = new ArrayList<>();

    /** List of devices associated with each reading **/
    private final ArrayList<String> devices = new ArrayList<>();

    /** Links the {@link #sensorReadings} to a UI view. **/
    private SensorDataListAdapter sensorReadingAdapter;

    private ViewPager viewPager;

    private ApplicationPreferences applicationPreferences;

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

    private StandardTutorial tutorial;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (int i = 0; i < SharedConstants.SENSOR_TYPE.values().length; i++) {
            SharedConstants.SENSOR_TYPE sensor_type = SharedConstants.SENSOR_TYPE.values()[i];
            if (sensor_type.display()) {
                sensorReadings.add("--");
                devices.add(sensor_type.getDevice());
                sensors.add(sensor_type.getSensor());
            }
        }
        applicationPreferences = ApplicationPreferences.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_sensor_readings, container, false);

        sensorReadingAdapter = new SensorDataListAdapter(getActivity(), sensors, devices, sensorReadings);
        sensorDataList = (ListView) view.findViewById(R.id.lv_sensor_readings);
        sensorDataList.setAdapter(sensorReadingAdapter);
        sensorReadingAdapter.notifyDataSetChanged();
        sensorReadingAdapter.setOnRowClickedListener(new SensorDataListAdapter.OnRowClickedListener() {
            @Override
            public void onRowClicked(int row) {
                if (tutorial != null)
                    tutorial.dismiss();

                String sensor = sensors.get(row);
                String device = devices.get(row);

                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.dialog_sensor_data, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(dialogLayout);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        continueTutorial();
                    }
                });
                alertDialog.show();
                alertDialog.getWindow().setLayout(1000, 800);

                TextView txtDevice = (TextView) dialogLayout.findViewById(R.id.device);
                TextView txtSensor = (TextView) dialogLayout.findViewById(R.id.sensor);
                TextView txtSamplingRate = (TextView) dialogLayout.findViewById(R.id.samplingRate);
                Button buttonOK = (Button) dialogLayout.findViewById(R.id.sensorDataButtonOK);

                txtDevice.setText("Device: " + device);
                txtSensor.setText("Sensor: " + sensor);
                txtSamplingRate.setText("Sampling Rate: " + getSamplingRate(sensor, device) + " Hz");
                buttonOK.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        alertDialog.cancel();
                    }
                });
            }
        });

        return view;
    }

    private int getSamplingRate(String sensor, String device){
        if (device.equals(SharedConstants.DEVICE.METAWEAR.TITLE) &&
                sensor.equals(SharedConstants.SENSOR.ACCELEROMETER.TITLE)){
            return applicationPreferences.getAccelerometerSamplingRate();
        } else if (device.equals(SharedConstants.DEVICE.METAWEAR.TITLE) &&
                sensor.equals(SharedConstants.SENSOR.GYROSCOPE.TITLE)){
            return applicationPreferences.getGyroscopeSamplingRate();
        } else if (sensor.equals(SharedConstants.SENSOR.RSSI.TITLE)){
            return applicationPreferences.getRssiSamplingRate();
        } else
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

    public void showTutorial(final ViewPager viewPager){
        if (!applicationPreferences.showTutorial()) return;
        this.viewPager = viewPager;
        View listItem = getViewByPosition(0, sensorDataList);
        tutorial = new StandardTutorial(getActivity(), listItem)
                .setDescription(getString(R.string.tutorial_sensor_data))
                .enableButton(false)
                .setTutorialListener(new StandardTutorial.TutorialListener() {
                    @Override
                    public void onReady(final StandardTutorial tutorial) {
                        tutorial.showTutorial();
                    }

                    @Override
                    public void onComplete(StandardTutorial tutorial) {

                    }
                }).build();
    }

    public void continueTutorial(){
        if (!applicationPreferences.showTutorial()) return;
        View icon = sensorReadingAdapter.imageView;
        new StandardTutorial(getActivity(), icon)
                .setDescription(getString(R.string.tutorial_device_icon))
                .setButtonText(getString(R.string.tutorial_next))
                .setTutorialListener(new StandardTutorial.TutorialListener() {
            @Override
            public void onReady(final StandardTutorial tutorial) {
                tutorial.showTutorial();
            }

            @Override
            public void onComplete(StandardTutorial tutorial) {
                viewPager.setCurrentItem(MainActivity.PAGES.RECORDING.getPageNumber(), true);
            }
        }).build();
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