package edu.umass.cs.prepare.view.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.ServiceManager;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.recording.RecordingService;
import edu.umass.cs.prepare.view.activities.MainActivity;
import edu.umass.cs.prepare.view.tutorial.StandardTutorial;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;

/**
 * This fragment allows the user to record video. It is a rather simple layout with
 * a recording toggle button and a surface view for previewing the video.
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see RecordingService
 * @see SurfaceView
 */
public class RecordingFragment extends Fragment {

    /** The view containing the video recording preview. **/
    private SurfaceView mSurfaceView;

    /** Button that controls video recording, i.e. on/off switch. **/
    private Button recordingButton;

    /** Handles services on the mobile application. */
    private ServiceManager serviceManager;

    /** The main UI containing this fragment. **/
    private MainActivity UI;

    /** Indicates whether audio should be recorded. **/
    private boolean recordAudio;

    /** Gives access to shared application preferences. **/
    private ApplicationPreferences applicationPreferences;

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        //the intent filter specifies the messages we are interested in receiving
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceManager = ServiceManager.getInstance(getActivity());
        applicationPreferences = ApplicationPreferences.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_recording, container, false);
        recordingButton = (Button) view.findViewById(R.id.start_button);
        if (RecordingService.isRecording)
            recordingButton.setBackgroundResource(R.drawable.ic_stop_white_24dp);
        if (!applicationPreferences.showTutorial())
            enableRecordingButton();
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface_camera);
        UI = (MainActivity) getActivity();
        return view;
    }

    /**
     * Called when all required permissions have been granted.
     */
    private void onPermissionsGranted(){
        int[] position = new int[2];
        mSurfaceView.getLocationOnScreen(position);
        int w = mSurfaceView.getWidth();
        int h = mSurfaceView.getHeight();
        serviceManager.startRecordingService(position[0], position[1], w, h, recordAudio);
    }

    /**
     * Request permissions required for video recording. These include
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link android.Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(){
        List<String> permissionGroup = new ArrayList<>(Arrays.asList(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }));

        recordAudio = applicationPreferences.recordAudio();
        if (recordAudio){
            permissionGroup.add(Manifest.permission.RECORD_AUDIO);
        }

        String[] permissions = permissionGroup.toArray(new String[permissionGroup.size()]);

        if (!hasPermissionsGranted(permissions)) {
            requestPermissions(permissions, MainActivity.REQUEST_CODE.RECORDING);
            return;
        }
        checkDrawOverlayPermission();
    }

    /**
     * Check the draw overlay permission. This is required to run the video recording service in
     * a background service.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (!Settings.canDrawOverlays(getContext().getApplicationContext())) {
            /** if not, construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse(getString(R.string.app_package_identifier_prefix) + getActivity().getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, MainActivity.REQUEST_CODE.WINDOW_OVERLAY);
        }else{
            onPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MainActivity.REQUEST_CODE.RECORDING: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.CAMERA:
                                UI.showStatus(getString(R.string.video_permission_denied));
                                return;
                            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                                UI.showStatus(getString(R.string.video_permission_denied));
                                return;
                            case Manifest.permission.RECORD_AUDIO:
                                recordAudio = false;
                                UI.showStatus(getString(R.string.audio_permission_denied));
                                break;
                            default:
                                return;
                        }
                    }
                }
                checkDrawOverlayPermission();
            }
        }
    }

    /**
     * Check the specified permissions
     * @param permissions list of Strings indicating permissions
     * @return true if ALL permissions are granted, false otherwise
     */
    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.REQUEST_CODE.WINDOW_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /** if so check once again if we have permission */
                if (Settings.canDrawOverlays(getContext())) {
                    onPermissionsGranted();
                }
            }
        }
    }

    /**
     * The receiver listens for messages from the {@link RecordingService}, e.g. was the
     * service started/stopped, and updates the fragment views accordingly.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(SharedConstants.KEY.MESSAGE, -1);
                    if (message == SharedConstants.MESSAGES.RECORDING_SERVICE_STARTED){
                        recordingButton.setBackgroundResource(R.drawable.ic_stop_white_24dp);
                    } else if (message == SharedConstants.MESSAGES.RECORDING_SERVICE_STOPPED){
                        recordingButton.setBackgroundResource(R.drawable.ic_play_arrow_white_24dp);
                    }
                }
            }
        }
    };

    /**
     * Shows the video recording tutorial. The recording button is disabled during the tutorial
     * and enabled once completed. Once complete, the view pager will update to the settings tab.
     * @param viewPager view pager handle responsible for updating the visible tab.
     */
    public void showTutorial(final ViewPager viewPager){
        String tutorialText = String.format(Locale.getDefault(), getString(R.string.tutorial_recording),
                applicationPreferences.getSaveDirectory());
        new StandardTutorial(getActivity(), recordingButton)
                .setDescription(tutorialText)
                .setButtonText(getString(R.string.tutorial_next))
                .setTutorialListener(new StandardTutorial.TutorialListener() {
            @Override
            public void onReady(final StandardTutorial tutorial) {
                recordingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getActivity(), R.string.recording_button_disabled_tutorial_mode, Toast.LENGTH_LONG).show();
                    }
                });
                tutorial.showTutorial();
            }

            @Override
            public void onComplete(StandardTutorial tutorial) {
                viewPager.setCurrentItem(MainActivity.PAGES.SETTINGS.getPageNumber(), true);
                enableRecordingButton();
            }
        }).build();
    }

    /**
     * Enables the recording button.
     */
    private void enableRecordingButton(){
        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!RecordingService.isRecording) {
                    //TODO: Do Android versions prior to M require run-time permission request for overlay?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        requestPermissions();
                    else
                        onPermissionsGranted();
                } else {
                    serviceManager.stopRecordingService();
                }
            }
        });
    }
}