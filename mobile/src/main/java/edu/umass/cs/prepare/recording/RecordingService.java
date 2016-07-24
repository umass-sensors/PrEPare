package edu.umass.cs.prepare.recording;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.communication.local.Broadcaster;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.view.activities.CameraReminderDialogActivity;
import edu.umass.cs.prepare.view.activities.MainActivity;
import edu.umass.cs.shared.constants.SharedConstants;
import edu.umass.cs.shared.preferences.ApplicationPreferences;

/**
 * Background video recording service. Due to the questionable legal and ethical nature of secret
 * video recording, Android API explicitly disallows video recording without a visible preview
 * surface. This makes recording video in the background rather difficult. A workaround is to
 * create a surface view of size 1x1 pixel with highest-priority Z-order which persists even
 * when the application is not visible to the user. Although the preview must always be visible
 * to the user, the user will not notice it at minimal size on a high-resolution display.
 *
 * This workaround requires the {@link android.Manifest.permission#SYSTEM_ALERT_WINDOW} permission,
 * which allows the application to place the video surface in the foreground, even above other
 * running applications.
 *
 * @author snoran
 * @affiliation University of Massachusetts Amherst
 *
 * @see MediaRecorder
 * @see SurfaceView
 * @see SurfaceHolder
 * @see android.Manifest.permission#SYSTEM_ALERT_WINDOW
 */
public class RecordingService extends Service implements SurfaceHolder.Callback
{
    /** Holder for the {@link SurfaceView} which displays the recording **/
    private SurfaceHolder sHolder;

    /* Surface view responsible for displaying the video recording preview */
    private SurfaceView mSurfaceView;

    /** Object for handling video recording **/
    private MediaRecorder mMediaRecorder;

    @SuppressWarnings("deprecation")
    private Camera camera;

    /** indicates whether audio should be recorded in addition to video **/
    private boolean recordAudio;

    /** indicates whether the service is currently recording video **/
    public static boolean isRecording = false;

    /** width in pixels of the {@link SurfaceView} which displays the video recording preview **/
    private int width;

    /** height in pixels of the {@link SurfaceView} which displays the video recording preview **/
    private int height;

    public static final int CAMERA_REMINDER_TIMEOUT_MINUTES = 2;

    private ApplicationPreferences applicationPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationPreferences = ApplicationPreferences.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO: Not entirely certain but I believe this occurs when the surface view is not properly destroyed
        if (intent == null){
            stopForeground(true);
            stopSelf();
        } else if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
            mSurfaceView = new SurfaceView(getApplicationContext());
            sHolder = mSurfaceView.getHolder();
            sHolder.addCallback(this);

            //noinspection deprecation
            sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); //TODO: Supposedly not necessary anymore?

            WindowManager winMan = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);

            //TODO: FLAG_NOT_TOUCHABLE with TYPE_SYSTEM_ALERT?

            //surface view dimensions and position specified where service intent is called
            params.gravity = Gravity.TOP | Gravity.START;
            params.x=intent.getIntExtra(Constants.KEY.SURFACE_X, 0);
            params.y=intent.getIntExtra(Constants.KEY.SURFACE_Y, 0);
            width = intent.getIntExtra(Constants.KEY.SURFACE_WIDTH, 1);
            height = intent.getIntExtra(Constants.KEY.SURFACE_HEIGHT, 1);
            recordAudio = intent.getBooleanExtra(Constants.KEY.RECORD_AUDIO, getResources().getBoolean(R.bool.pref_audio_default));

            //display the surface view as a stand-alone window
            winMan.addView(mSurfaceView, params);
            mSurfaceView.setZOrderOnTop(true);
            sHolder.setFixedSize(width, height);
            sHolder.setFormat(PixelFormat.TRANSPARENT);

            startForeground(SharedConstants.NOTIFICATION_ID.RECORDING_SERVICE, getNotification("Video currently recording..."));

        }else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE) && isRecording){
            stopRecording();
            Broadcaster.broadcastMessage(this, SharedConstants.MESSAGES.RECORDING_SERVICE_STOPPED);
            stopSelf();
        }else if (intent.getAction().equals(Constants.ACTION.MINIMIZE_VIDEO) && isRecording){
            //there is no functionality to minimize the video, but setting the surface size to 1x1 pixel should suffice
            sHolder.setFixedSize(1, 1);
            //mSurfaceView.setZOrderOnTop(true);
        }else if (intent.getAction().equals(Constants.ACTION.MAXIMIZE_VIDEO) && isRecording){
            //return to the original width and height
            //mSurfaceView.setZOrderOnTop(false);
            sHolder.setFixedSize(width,height);
        }else if (intent.getAction().equals(Constants.ACTION.SET_CAMERA_REMINDER) && isRecording){
            setUpCameraReminder();
        }

        return START_STICKY;
    }

    /**
     * Starts the video recording, along with audio if enabled. The video file is
     * stored in the directory specified in the application preferences.
     */
    @SuppressWarnings("deprecation")
    private void startRecording(){
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        camera.setParameters(parameters);
        camera.unlock();
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(camera);
        if (recordAudio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (recordAudio) {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setOutputFile(new File(applicationPreferences.getSaveDirectory(), "VIDEO" + String.valueOf(System.currentTimeMillis()) + ".mp4").getAbsolutePath());
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setPreviewDisplay(sHolder.getSurface());
        mMediaRecorder.setOrientationHint(90);
        try{
            mMediaRecorder.prepare();

        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }

        mMediaRecorder.start();
        isRecording = true;
        Broadcaster.broadcastMessage(this, SharedConstants.MESSAGES.RECORDING_SERVICE_STARTED);
    }

    private Handler handler;
    private Runnable cameraReminder;

    private void setUpCameraReminder(){
        if (applicationPreferences.showCameraReminder()) {
            if (handler == null) {
                handler = new Handler();
                cameraReminder = new Runnable() {
                    @Override
                    public void run() {
                        if (RecordingService.isRecording) {
                            Intent dialogIntent = new Intent(RecordingService.this, CameraReminderDialogActivity.class);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(dialogIntent);
                        }
                    }
                };
            }
            handler.removeCallbacks(cameraReminder);
            handler.postDelayed(cameraReminder, CAMERA_REMINDER_TIMEOUT_MINUTES * 60000);
        }
    }

    /**
     * Returns the notification displayed during background recording.
     * @param contentText The text displayed on the notification
     * @return the notification handle
     */
    private Notification getNotification(String contentText){
        Intent notificationIntent = new Intent(this, MainActivity.class); //open main activity when user clicks on notification
        notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(Constants.KEY.PAGE_INDEX, MainActivity.PAGES.RECORDING.getPageNumber());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

        // notify the user that the foreground service has started
        return new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_videocam_white_24dp)
                .setOngoing(true)
                .setVibrate(new long[]{0, 50, 150, 200})
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.ic_stop_white_24dp, getString(R.string.stop_service), stopPendingIntent)
                .setContentIntent(pendingIntent).build();
    }

    /**
     * Stops video/audio recording and releases the media recorder.
     */
    private void stopRecording(){
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
        }
        if (camera != null){
            camera.release();
            camera = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
        }
        if (sHolder != null) {
            sHolder.setFixedSize(1, 1);
            sHolder.getSurface().release();
            sHolder = null;
        }
        isRecording = false;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (!isRecording)
            startRecording();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (isRecording)
            stopRecording();
    }

    /**
     * Finds the preview size which closest matches the specified dimensions.
     * @param sizes the list of available camera sizes
     * @param w the preferred width
     * @param h the preferred height
     * @return the closest matching camera size
     * @see <a href="http://stackoverflow.com/questions/17804309/android-camera-preview-wrong-aspect-ratio">Response by Henric</a>
     */
    @SuppressWarnings("deprecation")
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes==null) return null;

        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Find closest size with suitable aspect ratio
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // if no size is within the tolerance of the aspect ratio, choose the size with the closest height
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }
}