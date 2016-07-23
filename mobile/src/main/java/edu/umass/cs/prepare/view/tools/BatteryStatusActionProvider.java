package edu.umass.cs.prepare.view.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import edu.umass.cs.prepare.R;
import edu.umass.cs.shared.preferences.ApplicationPreferences;
import edu.umass.cs.shared.util.BatteryUtil;

public class BatteryStatusActionProvider extends ActionProvider {

    /** Context for accessing resources. */
    private final Context mContext;

    /** The image displaying the current battery level. **/
    private ImageView imgBatteryStatus;

    public BatteryStatusActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateActionView() {
        // Inflate the action view to be shown on the action bar.
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.action_provider_battery_status, null);
        imgBatteryStatus = (ImageView) view.findViewById(R.id.batteryStatus);
        int batteryLevel = ApplicationPreferences.getInstance(mContext).getBatteryLevel();
        imgBatteryStatus.setImageResource(BatteryUtil.getBatteryLevelIconId(batteryLevel));
        return view;
    }

    /**
     * Updates the battery status image given the current battery level.
     * @param percentage the battery level in [0, 100]
     */
    public void updateBatteryStatus(int percentage) {
        imgBatteryStatus.setImageResource(BatteryUtil.getBatteryLevelIconId(percentage));
    }
}