package edu.umass.cs.prepare.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import edu.umass.cs.prepare.R;

public class CustomAdapter extends BaseAdapter {

    interface OnRowClickedListener{
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

    public CustomAdapter(Context context, ArrayList<String> sensors, ArrayList<String> devices, ArrayList<String> sensorReadings) {
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
            rowView[position] = inflater.inflate(R.layout.item_sensor_reading, parent, false);
        }

        holder.txtSensor = (TextView) rowView[position].findViewById(R.id.txtSensor);
        holder.imgDevice = (ImageView) rowView[position].findViewById(R.id.imgDevice);
        holder.txtReading = (TextView) rowView[position].findViewById(R.id.txtReading);

        if (imageView == null && position == 0)
            imageView = holder.imgDevice;

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
            case "Wearable":
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.wearable);
            case "Metawear":
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pill);
            case "Mobile":
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.rssi);
            default:
                return null;
        }
    }

}
