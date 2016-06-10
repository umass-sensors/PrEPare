package edu.umass.cs.prepare.recording;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.umass.cs.prepare.R;
import edu.umass.cs.prepare.constants.Constants;
import edu.umass.cs.prepare.main.MainActivity;
import edu.umass.cs.shared.SharedConstants;

/**
 * Background video recording service. Due to the questionable legal and ethical nature of secret
 * video recording, Android APIs explicitly disallow video recording without a visible preview
 * surface. This makes recording video in the background rather difficult. A workaround is to
 * create a surface view of size 1x1 pixel with highest-priority Z-order which persists even
 * when the application is not visible to the user. Although the preview must always be visible
 * to the user, the user is unlikely to notice it because it is only 1x1 pixel large.
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
    private boolean record_audio;

    /** the directory where the video data is stored **/
    private String save_directory;

    /** indicates whether the service is currently recording video **/
    private boolean isRecording = false;

    /** width in pixels of the {@link SurfaceView} which displays the video recording preview **/
    private int width;

    /** height in pixels of the {@link SurfaceView} which displays the video recording preview **/
    private int height;

    /**
     * Loads shared preferences, e.g. the directory where the video data should be saved and
     * whether or not audio recording is enabled.
     */
    private void loadPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String defaultDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name)).getAbsolutePath();
        save_directory = preferences.getString(getString(R.string.pref_directory_key), defaultDirectory);

        record_audio = preferences.getBoolean(getString(R.string.pref_audio_key),
                getResources().getBoolean(R.bool.pref_audio_default));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO: Not entirely certain but I believe this occurs when the surface view is not properly destroyed
        if (intent == null){
            stopForeground(true);
            stopSelf();
        } else if (intent.getAction().equals(SharedConstants.ACTIONS.START_SERVICE)) {
            loadPreferences();

            mSurfaceView = new SurfaceView(getApplicationContext());
            sHolder = mSurfaceView.getHolder();
            sHolder.addCallback(this);

            //noinspection deprecation
            sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); //TODO: Supposedly not necessary anymore?

            WindowManager winMan = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            //TODO: FLAG_NOT_TOUCHABLE with TYPE_SYSTEM_ALERT?

            //surface view dimensions and position specified where service intent is called
            params.gravity = Gravity.TOP | Gravity.START;
            params.x=intent.getIntExtra(Constants.KEY.SURFACE_X, 0);
            params.y=intent.getIntExtra(Constants.KEY.SURFACE_Y, 0);
            width = intent.getIntExtra(Constants.KEY.SURFACE_WIDTH, 1);
            height = intent.getIntExtra(Constants.KEY.SURFACE_HEIGHT, 1);

            //display the surface view as a stand-alone window
            winMan.addView(mSurfaceView, params);
            mSurfaceView.setZOrderOnTop(true);
            sHolder.setFixedSize(width, height);
            sHolder.setFormat(PixelFormat.TRANSPARENT);

            Intent notificationIntent = new Intent(this, MainActivity.class); //open main activity when user clicks on notification
            notificationIntent.setAction(Constants.ACTION.NAVIGATE_TO_APP);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Intent stopIntent = new Intent(this, RecordingService.class);
            stopIntent.setAction(SharedConstants.ACTIONS.STOP_SERVICE);
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);

            // notify the user that the foreground service has started
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText("Video currently recording...")
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .setOngoing(true)
                    .setVibrate(new long[]{0, 50, 150, 200})
                    .setPriority(Notification.PRIORITY_MAX)
                    .addAction(android.R.drawable.ic_delete, getString(R.string.stop_service), stopPendingIntent)
                    .setContentIntent(pendingIntent).build();

            startForeground(SharedConstants.NOTIFICATION_ID.RECORDING_SERVICE, notification);

        }else if (intent.getAction().equals(SharedConstants.ACTIONS.STOP_SERVICE) && isRecording){
            stopRecording();
            stopSelf();
        }else if (intent.getAction().equals(Constants.ACTION.MINIMIZE_VIDEO) && isRecording){
            //there is no functionality to minimize the video, but setting the surface size to 1x1 pixel should suffice
            sHolder.setFixedSize(1, 1);
            //mSurfaceView.setZOrderOnTop(true);
        }else if (intent.getAction().equals(Constants.ACTION.MAXIMIZE_VIDEO) && isRecording){
            //return to the original width and height
            //mSurfaceView.setZOrderOnTop(false);
            sHolder.setFixedSize(width,height);
        }

        return START_STICKY;
    }

    /**
     * Starts the video recording, along with audio if enabled. The video file is
     * stored in the directory specified in the application preferences.
     */
    @SuppressWarnings("deprecation")
    public void startRecording(){
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        camera.unlock();
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(camera);
        if (record_audio) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (record_audio) {
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        }
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setOutputFile(new File(save_directory, "VIDEO" + String.valueOf(System.currentTimeMillis()) + ".mp4").getAbsolutePath());
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
    }

    /**
     * Stops video/audio recording and releases the media recorder.
     */
    public void stopRecording(){
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
        Toast.makeText(getApplicationContext(), String.valueOf(isRecording), Toast.LENGTH_LONG).show();
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
}