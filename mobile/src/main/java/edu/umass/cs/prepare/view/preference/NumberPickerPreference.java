package edu.umass.cs.prepare.view.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import java.util.Locale;

import edu.umass.cs.prepare.R;

/**
 * http://stackoverflow.com/questions/20758986/android-preferenceactivity-dialog-with-number-picker
 */
public class NumberPickerPreference extends DialogPreference {

    public static final int DEFAULT_MAX_VALUE = 100;
    public static final int DEFAULT_MIN_VALUE = 0;
    public static final boolean DEFAULT_WRAP_SELECTOR_WHEEL = true;

    /** Minimum value allowed **/
    private final int minValue;

    /** Maximum value allowed **/
    private final int maxValue;

    /** Indicates whether the circular behavoir is enabled. **/
    private final boolean wrapSelectorWheel;

    /** Reference to the number picker object. **/
    private NumberPicker picker;

    /** The currently selected value. **/
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
        minValue = a.getInteger(R.styleable.NumberPickerPreference_minValue, DEFAULT_MIN_VALUE);
        maxValue = a.getInteger(R.styleable.NumberPickerPreference_maxValue, DEFAULT_MAX_VALUE);
        wrapSelectorWheel = a.getBoolean(R.styleable.NumberPickerPreference_wrapSelectorWheel, DEFAULT_WRAP_SELECTOR_WHEEL);
        a.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setWrapSelectorWheel(wrapSelectorWheel);
        picker.setValue(getValue());
        picker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {
                return String.format(Locale.getDefault(), "%03d", i);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            picker.clearFocus();
            int newValue = picker.getValue();
            if (callChangeListener(newValue)) {
                setValue(newValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, minValue);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(minValue) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        this.value = value;
        persistInt(this.value);
    }

    public int getValue() {
        return this.value;
    }
}