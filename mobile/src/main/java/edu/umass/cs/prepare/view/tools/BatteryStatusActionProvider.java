package edu.umass.cs.prepare.view.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import org.w3c.dom.Text;

import edu.umass.cs.prepare.R;
import edu.umass.cs.shared.util.BatteryUtil;

public class BatteryStatusActionProvider extends ActionProvider {

    /** Context for accessing resources. */
    private final Context mContext;

    private ImageView imgBatteryStatus;

    public BatteryStatusActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public View onCreateActionView() {
        // Inflate the action view to be shown on the action bar.
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.action_provider_battery_status, null);
        imgBatteryStatus = (ImageView) view.findViewById(R.id.batteryStatus);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int batteryLevel = preferences.getInt(mContext.getString(R.string.pref_battery_level_key),
                mContext.getResources().getInteger(R.integer.pref_battery_level_default));
        imgBatteryStatus.setImageResource(BatteryUtil.getBatteryLevelIconId(batteryLevel));
        return view;
    }

    public void updateBatteryStatus(int percentage) {
        imgBatteryStatus.setImageResource(BatteryUtil.getBatteryLevelIconId(percentage));
    }
}