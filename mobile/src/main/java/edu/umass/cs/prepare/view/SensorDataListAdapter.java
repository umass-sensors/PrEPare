package edu.umass.cs.prepare.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import edu.umass.cs.prepare.R;
import edu.umass.cs.shared.constants.SharedConstants;

public class SensorDataListAdapter extends BaseAdapter {

    public interface OnRowClickedListener{
        void onRowClicked(int row);
    }

    private OnRowClickedListener onRowClickedListener;

    public void setOnRowClickedListener(OnRowClickedListener onRowClickedListener){
        this.onRowClickedListener = onRowClickedListener;
    }

    private ArrayList<String> sensorReadings, devices, sensors;
    Context context;
    private LayoutInflater inflater=null;
    public ImageView imageView;
    public TextView dataView;

    public SensorDataListAdapter(Context context, ArrayList<String> sensors, ArrayList<String> devices, ArrayList<String> sensorReadings) {
        this.sensorReadings = sensorReadings;
        this.sensors = sensors;
        this.devices = devices;
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rowView = new View[sensorReadings.size()];
    }
    @Override
    public int getCount() {
        return sensorReadings.size();
    }

    @Override
    public Object getItem(int position) {
        return sensorReadings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class Holder
    {
        TextView txtSensor, txtReading;
        ImageView imgDevice;
    }

    private View[] rowView;

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder=new Holder();

        if (rowView[position] == null) {
            rowView[position] = inflater.inflate(R.layout.listitem_sensor_reading, parent, false);
        }

        holder.txtSensor = (TextView) rowView[position].findViewById(R.id.txtSensor);
        holder.imgDevice = (ImageView) rowView[position].findViewById(R.id.imgDevice);
        holder.txtReading = (TextView) rowView[position].findViewById(R.id.txtReading);

        if (imageView == null && position == 0)
            imageView = holder.imgDevice;

        if (dataView == null && position == 0)
            dataView = holder.txtReading;

        holder.txtReading.setText(sensorReadings.get(position));
        holder.imgDevice.setImageBitmap(getBitmapFromDevice(devices.get(position)));
        holder.txtSensor.setText(sensors.get(position));

        if (onRowClickedListener != null) {
            rowView[position].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onRowClickedListener != null)
                        onRowClickedListener.onRowClicked(position);
                }
            });
        }
        return rowView[position];
    }

    private Bitmap getBitmapFromDevice(String device){
        switch (device){
            case SharedConstants.DEVICE.WEARABLE.TITLE:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_wearable);
            case SharedConstants.DEVICE.METAWEAR.TITLE:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pill);
            case SharedConstants.DEVICE.MOBILE.TITLE:
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_rssi);
            default:
                return null;
        }
    }

}
