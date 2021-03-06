package edu.umass.cs.prepare.view.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.view.preference.SwitchPreference;
import edu.umass.cs.prepare.view.activities.MainActivity;
import edu.umass.cs.prepare.view.activities.SelectDeviceActivity;
import edu.umass.cs.prepare.view.tools.ConnectionStatusActionProvider;
import edu.umass.cs.prepare.view.tutorial.StandardTutorial;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;

/**
 * The Settings fragment allows the user to modify all shared applications preferences, e.g.
 * whether data should be sent to the server or written locally, at what rate sensors should
 * sample, and what sensors should be enabled. Custom preferences, including the directory
 * chooser preference, the switch preference and the device selection preference are all
 * managed here. Upon initial startup, a tutorial is shown which ensures the user explicitly
 * enables the Metawear service.
 */
public class SettingsFragment extends PreferenceFragment {
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SettingsFragment.class.getName();

    /** Request code to identify that the user selected the directory when the activity called for result returns. **/
    private static final int SELECT_DIRECTORY_REQUEST_CODE = 1;

    /** Request code to identify that the user selected the device address when the activity called for result returns. **/
    private static final int SELECT_DEVICE_REQUEST_CODE = 2;

    /** Preference view for selecting the directory. **/
    private Preference prefDirectory;

    /** Preference view for selecting the device. **/
    private Preference prefDevice;

    /** Gives access to the shared application preferences. **/
    private ApplicationPreferences applicationPreferences;

    /** Metawear service enabled preference, which uses a {@link android.widget.Switch} instead of a check box. **/
    private SwitchPreference toggleServicePreference;

    /** The settings tutorial sequence. **/
    private StandardTutorial tutorial;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        applicationPreferences = ApplicationPreferences.getInstance(getActivity());
        String path = applicationPreferences.getSaveDirectory();

        prefDirectory = findPreference(getString(R.string.pref_directory_key));
        prefDirectory.setSummary(path);
        prefDirectory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                final Intent chooserIntent = new Intent(getActivity(), DirectoryChooserActivity.class);

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

        String address = applicationPreferences.getMwAddress();

        prefDevice = findPreference(getString(R.string.pref_device_key));
        prefDevice.setSummary(address);
        prefDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(getActivity(), SelectDeviceActivity.class), SELECT_DEVICE_REQUEST_CODE);
                return true;
            }
        });

        final Preference ipAddressPreference = findPreference(getString(R.string.pref_ip_key));
        EditText ipAddressEditText = ((EditTextPreference) ipAddressPreference).getEditText();
        ipAddressEditText.setFilters(new InputFilter[]{new InputFilter() {
            /**
             * Filters the text input to ensure it is a valid IP address
             * @param source source text
             * @param start start index
             * @param end end index
             * @param dest destination span
             * @param destStart destination start index
             * @param destEnd destination end index
             * @return null if valid, "" otherwise
             * @see <a href="http://stackoverflow.com/questions/8661915/what-androidinputtype-should-i-use-for-entering-an-ip-address">SKT's answer</a>
             */
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destStart, int destEnd) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, destStart) +
                            source.subSequence(start, end) +
                            destTxt.substring(destEnd);
                    if (!resultingTxt.matches("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String split : splits) {
                            if (Integer.valueOf(split) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        }});
        ipAddressPreference.setSummary(((EditTextPreference) ipAddressPreference).getText());
        ipAddressPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(o.toString());
                return true;
            }
        });

        toggleServicePreference = (SwitchPreference) findPreference(getString(R.string.pref_connect_key));
        toggleServicePreference.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                applicationPreferences.edit().putBoolean(getString(R.string.pref_connect_key), enabled).apply();

                if (tutorial != null)
                    tutorial.dismiss();
                if (enabled)
                    ServiceManager.getInstance(getActivity()).startMetawearService();
                else
                    ServiceManager.getInstance(getActivity()).stopMetawearService();

            }
        });

        findPreference(getString(R.string.pref_server_key)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if ((Boolean) o)
                    ((MainActivity)getActivity()).networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                else
                    ((MainActivity)getActivity()).networkStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
                return true;
            }
        });

        findPreference(getString(R.string.pref_wearable_key)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if ((Boolean) o)
                    ((MainActivity)getActivity()).wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISCONNECTED);
                else
                    ((MainActivity)getActivity()).wearableStatusActionProvider.setStatus(ConnectionStatusActionProvider.CONNECTION_STATUS.DISABLED);
                return true;
            }
        });

        findPreference(getString(R.string.pref_subject_id_key)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(String.format(Locale.getDefault(), "%03d", (Integer)o));
                return true;
            }
        });

        findPreference(getString(R.string.pref_check_for_updates_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DownloadTask().execute(getVersionFile());
                return true;
            }
        });
    }

    private String downloadText(String url) {
        int BUFFER_SIZE = 2000;
        InputStream in = null;
        try {
            in = openHttpConnection(url);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        String str = "";
        if (in != null) {
            InputStreamReader isr = new InputStreamReader(in);
            int charRead;
            char[] inputBuffer = new char[BUFFER_SIZE];
            try {
                while ((charRead = isr.read(inputBuffer)) > 0) {
                    // ---convert the chars to a String---
                    String readString = String.copyValueOf(inputBuffer, 0, charRead);
                    str += readString;
                    inputBuffer = new char[BUFFER_SIZE];
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        }
        return str;
    }

    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        return null;
    }

    private String getVersionFile(){
        return "https://drive.google.com/uc?export=view&id=0Byr6oHEGQO73SVJSSFRHT2hzSUU";
    }

    private String getApkFile(){
        return "https://drive.google.com/uc?export=view&id=0Byr6oHEGQO73Q0laWURLM3hZbHM";
    }

    private InputStream openHttpConnection(String urlString) throws IOException {
        InputStream in = null;
        int response = -1;

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");

        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            throw ex;
        }
        return in;
    }

    class DownloadTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected String doInBackground(String... url) {
            return downloadText(url[0]);
        }

        @Override
        protected void onPostExecute(String version) {
            String currentVersion = getVersionName(getActivity());
            Toast.makeText(getActivity(), "current version: " + currentVersion + ", newest version: " + version, Toast.LENGTH_LONG).show();
            if (currentVersion == null || version == null)
                return;
            if (!version.equals(currentVersion)){
                Intent updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getApkFile()));
                startActivity(updateIntent);
            }
        }
    }

    public void showTutorial(){
        tutorial = new StandardTutorial(getActivity(), toggleServicePreference.getSwitch())
                .setDescription(getString(R.string.tutorial_enable_service))
                .enableButton(false)
                .setTutorialListener(new StandardTutorial.TutorialListener() {
            @Override
            public void onReady(final StandardTutorial tutorial) {
                tutorial.showTutorial();
            }

            @Override
            public void onComplete(StandardTutorial tutorial) {
                applicationPreferences.edit().putBoolean(getString(R.string.pref_show_tutorial_key), false).apply();
            }
        }).build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DIRECTORY_REQUEST_CODE) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                String dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                applicationPreferences.edit().putString(getString(R.string.pref_directory_key), dir).apply();
                prefDirectory.setSummary(dir);
            }
        }else if (requestCode == SELECT_DEVICE_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getStringExtra(SharedConstants.KEY.UUID);
                applicationPreferences.edit().putString(getString(R.string.pref_device_key), address).apply();
                prefDevice.setSummary(address);
            }
        }
    }
}