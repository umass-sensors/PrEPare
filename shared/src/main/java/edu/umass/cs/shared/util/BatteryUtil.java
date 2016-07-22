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

    public static final double TOTAL_LIFETIME = 70; // hours

    /**
     * Computes the estimated lifetime of the Metawear battery, given the current battery level.
     * The lifetime refers to time collecting data and not the physical lifetime, i.e. downtime
     * is not included in the measurement.
     *
     * @param batteryLevel the current battery level in [0,100]
     * @return an estimate of the remaining battery lifetime
     */
    public static double getBatteryLifetimeEstimate(int batteryLevel){
        return BatteryUtil.TOTAL_LIFETIME * (batteryLevel / 100.0);
    }

    /**
     * Returns the auto-generated integer ID associated with the icon that best captures
     * the specified battery level.
     * @param percentage The battery level, between 1 and 100.
     * @return an integer identifier for the most suitable icon
     */
    public static int getBatteryLevelIconId(int percentage){
        if (percentage >= 95){
            return R.drawable.ic_battery_full_white;
        }else if (percentage >= 85){
            return R.drawable.ic_battery_90_white;
        }else if (percentage >= 75){
            return R.drawable.ic_battery_80_white;
        }else if (percentage >= 65){
            return R.drawable.ic_battery_70_white;
        }else if (percentage >= 55){
            return R.drawable.ic_battery_60_white;
        }else if (percentage >= 45){
            return R.drawable.ic_battery_50_white;
        }else if (percentage >= 30){
            return R.drawable.ic_battery_40_white;
        }else if (percentage >= 15){
            return R.drawable.ic_battery_30_white;
        }else if (percentage >= 5){
            return R.drawable.ic_battery_low_white;
        }else if (percentage >= 1){
            return R.drawable.ic_battery_critical_white;
        }else{
            return R.drawable.ic_battery_empty;
        }
    }
}
