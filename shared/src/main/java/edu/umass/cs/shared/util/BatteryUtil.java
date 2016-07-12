package edu.umass.cs.shared.util;

import edu.umass.cs.shared.R;

/**
 * This utility class exposes static methods regarding the battery life on the Metawear device,
 * e.g. getting the icon corresponding to each battery level interval.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class BatteryUtil {

    /**
     * Returns the auto-generated integer ID associated with the icon that best captures
     * the specified battery level.
     * @param percentage The battery level, between 1 and 100.
     * @return an integer identifier for the most suitable icon
     */
    public static int getBatteryLevelIconId(int percentage){
        if (percentage >= 95){
            return R.drawable.ic_battery_full;
        }else if (percentage >= 85){
            return R.drawable.ic_battery_90;
        }else if (percentage >= 75){
            return R.drawable.ic_battery_80;
        }else if (percentage >= 65){
            return R.drawable.ic_battery_70;
        }else if (percentage >= 55){
            return R.drawable.ic_battery_60;
        }else if (percentage >= 45){
            return R.drawable.ic_battery_50;
        }else if (percentage >= 30){
            return R.drawable.ic_battery_40;
        }else if (percentage >= 15){
            return R.drawable.ic_battery_30;
        }else if (percentage >= 5){
            return R.drawable.ic_battery_low;
        }else if (percentage >= 1){
            return R.drawable.ic_battery_critical;
        }else{
            return R.drawable.ic_battery_empty;
        }
    }
}
