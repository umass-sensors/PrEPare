package edu.umass.cs.prepare.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import edu.umass.cs.shared.SharedConstants;
import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.metawear.SelectMetawearActivity;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.File;

/**
 * The Settings Activity handles all shared application preferences. These settings include the
 * {@link cs.umass.edu.shared.R.string#pref_directory_key save directory},
 * {@link cs.umass.edu.shared.R.string#pref_device_key address of the Metawear device},
 * the {@link cs.umass.edu.shared.R.string#pref_accelerometer_sampling_rate_key accelerometer},
 * {@link cs.umass.edu.shared.R.string#pref_gyroscope_sampling_rate_key gyroscope} and
 * {@link cs.umass.edu.shared.R.string#pref_rssi_sampling_rate_key RSSI} sampling rates as
 * well as the {@link cs.umass.edu.shared.R.string#pref_accelerometer_key accelerometer},
 * {@link cs.umass.edu.shared.R.string#pref_gyroscope_key gyroscope} and
 * {@link cs.umass.edu.shared.R.string#pref_rssi_key RSSI} enabled states.
 *
 * By ease of convention, preference keys, defaults, summaries and other related values are maintained in
 * {@link cs.umass.edu.shared.R.xml#preferences}, while the preference definitions and preference layout are
 * maintained in {@link R.xml#preferences}.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class SettingsActivity extends PreferenceActivity {

    /** Request code to identify that the user selected the directory when the activity called for result returns. **/
    private static final int SELECT_DIRECTORY_REQUEST_CODE = 1;

    /** Request code to identify that the user selected the device address when the activity called for result returns. **/
    private static final int SELECT_DEVICE_REQUEST_CODE = 2;

    /** Preference view for selecting the directory. **/
    private Preference prefDirectory;

    /** Preference view for selecting the device. **/
    private Preference prefDevice;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String defaultDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name)).getAbsolutePath();
        String path = preferences.getString(getString(R.string.pref_directory_key), defaultDirectory);

        prefDirectory = findPreference(getString(R.string.pref_directory_key));
        prefDirectory.setSummary(path);
        prefDirectory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                final Intent chooserIntent = new Intent(SettingsActivity.this, DirectoryChooserActivity.class);

                final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                        .newDirectoryName(getString(R.string.app_name))
                        .allowReadOnlyDirectory(true)
                        .allowNewDirectoryNameModification(true)
                        .build();

                chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);

                startActivityForResult(chooserIntent, SELECT_DIRECTORY_REQUEST_CODE);
                return true;
            }
        });

        String address = preferences.getString(getString(R.string.pref_device_key), getString(R.string.pref_device_default));

        prefDevice = findPreference(getString(R.string.pref_device_key));
        prefDevice.setSummary(address);
        prefDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(SettingsActivity.this, SelectMetawearActivity.class), SELECT_DEVICE_REQUEST_CODE);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DIRECTORY_REQUEST_CODE) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                String dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(getString(R.string.pref_directory_key), dir);
                editor.apply();
                prefDirectory.setSummary(dir);
            }
        }else if (requestCode == SELECT_DEVICE_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                String address = data.getStringExtra(SharedConstants.KEY.UUID);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(getString(R.string.pref_device_key), address);
                editor.apply();
                prefDevice.setSummary(address);
            }
        }
    }
}