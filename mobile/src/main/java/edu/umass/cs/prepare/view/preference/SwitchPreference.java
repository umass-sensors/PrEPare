package edu.umass.cs.prepare.view.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import edu.umass.cs.prepare.R;

public class SwitchPreference extends Preference {
    /**
     * The toggle switch associated with the preference
     */
    private Switch enableServiceButton;

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    public SwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View rootView) {
        super.onBindView(rootView);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        enableServiceButton = (Switch) rootView.findViewById(R.id.enableServiceButton);
        enableServiceButton.setChecked(preferences.getBoolean(getContext().getString(R.string.pref_connect_key),
                getContext().getResources().getBoolean(R.bool.pref_connect_default)));
        enableServiceButton.setOnCheckedChangeListener(onCheckedChangeListener);
        if (onViewCreatedListener != null)
            onViewCreatedListener.onViewCreated(enableServiceButton);
    }

    /**
     * Sets the listener which handles the event that the user checked or unchecked the preference
     * @param onCheckedChangeListener the listener object
     */
    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener){
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    public interface OnViewCreatedListener {
        void onViewCreated(View view);
    }

    private OnViewCreatedListener onViewCreatedListener;

    public void setOnViewCreatedListener(OnViewCreatedListener onViewCreatedListener){
        this.onViewCreatedListener = onViewCreatedListener;
    }

    /**
     * Retrieves the handle to the switch view
     * @return {@link View} object
     */
    public View getSwitch(){
        return enableServiceButton;
    }
}