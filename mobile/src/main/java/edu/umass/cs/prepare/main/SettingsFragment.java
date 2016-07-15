package edu.umass.cs.prepare.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.View;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.tutorial.StandardTutorial;

public class SettingsFragment extends PreferenceFragment {
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SensorReadingFragment.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean showTutorial = preferences.getBoolean(getString(R.string.pref_show_tutorial_key),
                getResources().getBoolean(R.bool.pref_show_tutorial_default));
        if (showTutorial)
            showTutorial();
        super.onViewCreated(view, savedInstanceState);
    }

    void showTutorial(){
        StandardTutorial tutorial = new StandardTutorial(getActivity(), getView(), getString(R.string.tutorial_settings), getString(R.string.tutorial_next), null);
        tutorial.setTutorialListener(new StandardTutorial.TutorialListener() {
            @Override
            public void onReady(final StandardTutorial tutorial) {
                tutorial.showTutorial();
            }

            @Override
            public void onFinish(StandardTutorial tutorial) {
                viewPager.setCurrentItem(1, true);
            }
        });

    }

    private ViewPager viewPager;

    public void setViewPager(ViewPager viewPager){
        this.viewPager = viewPager;
    }
}