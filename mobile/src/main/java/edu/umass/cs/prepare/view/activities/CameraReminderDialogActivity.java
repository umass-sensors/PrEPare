package edu.umass.cs.prepare.view.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import java.util.Locale;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.recording.RecordingService;

/**
 * This view consists only of a dialog that informs the user that the camera
 * has been running in the background. The dialog must be wrapped in an
 * activity, so that it can be opened from the {@link RecordingService}
 * (although it violates Android design guidelines).
 *
 * @author snoran
 * @affiliation University of Massachusetts Amherst
 *
 * @see RecordingService
 * @see AlertDialog
 */
public class CameraReminderDialogActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_reminder)
                .setMessage(String.format(Locale.getDefault(), getString(R.string.notification_reminder),
                        RecordingService.CAMERA_REMINDER_TIMEOUT_MINUTES))
                .setPositiveButton(R.string.button_reminder_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ServiceManager.getInstance(CameraReminderDialogActivity.this).enableCameraReminder();
                        finish();
                    }
                })
                .setNegativeButton(R.string.button_reminder_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CameraReminderDialogActivity.this);
                        preferences.edit().putBoolean(getString(R.string.pref_camera_reminder_key), false).apply();
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
