package com.example.caris_wheelchair;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class IntegratedActivity extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    /** General **/
    //TODO: Configure file naming with different terrain states
    static final String LOG_TAG = "IntegratedActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static final int REQUEST_IMU_PERMISSION = 202;
    private static String fileNameAudio;

    DateFormat dateFormatDisplay;
    DateFormat dateFormatSave;
    Date date;
    String dateDisplayed;
    String dateSaved;

    /** Audio specific **/
    private static final int AUDIO_BIT_RATE = 255;
    private static final int AUDIO_SAMP_RATE = 16000;

    private MediaRecorder audioRecorder;
    private MediaPlayer audioPlayer;

    private boolean audioRecorded = false;
    private boolean isRecordingAudio = false;
    private boolean isPlayingAudio = false;

    /** Camera specific **/
    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;

    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;

    private Size imageDimension;
    private ImageReader imageReader;

    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public class RecordingTimerTask extends TimerTask
    {
        @Override
        public void run() {
            takePicture();
        }
    }

    protected Timer recordingCameraTimer;
    protected IntegratedActivity.RecordingTimerTask recordingCameraTask;
    protected boolean isRecordingCamera = false;

    private final long RECORD_PERIOD = 1500;
    private final int IMG_WIDTH = 1080;
    private final int IMG_HEIGHT = 1920;

    /** IMU Specific**/
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private long lastUpdate = SystemClock.elapsedRealtime();
    private long totalTime = 0;
    private long timeThreshold = 0;

    private ArrayList<Float[]> data = new ArrayList<>();// will store the raw IMU data - may expand to other sensors as well

    private boolean isPaused = true; // tracks IMU data capture switch toggle
    private boolean accelOrGyro = true; // is true when looking for accel data, false when looking for gyro

    final long SIZE_KB = 1024L;  // for storage tracking
    final long SIZE_GB = SIZE_KB * SIZE_KB * SIZE_KB;

    /**************************************** General methods ****************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integrated);

        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateFormatDisplay = new SimpleDateFormat(getString(R.string.date_save_format));
        dateFormatSave = new SimpleDateFormat(getString(R.string.date_save_format));
        date = new Date();
        dateDisplayed = dateFormatDisplay.format(date);
        dateSaved = dateFormatSave.format(date);

        // Camera
        try {
            textureView = findViewById(R.id.cameraPreview);
            textureView.setSurfaceTextureListener(textureListener);
            takePictureButton = findViewById(R.id.button_photo);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecordingCamera) {
                    stopRecordingCamera();
                    isRecordingCamera = false;
                }
                else {
                    startRecordingCamera();
                    isRecordingCamera = true;
                }
            }
        });

        // IMU
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        float minDelayAccel = senAccelerometer.getMinDelay(); // returns time in MICROSECONDS
        float minDelayGyro = senGyroscope.getMinDelay();

        if (minDelayAccel >= minDelayGyro ) {
            timeThreshold = (long) minDelayAccel / 1000; // conversion from micro to milli seconds
        } else {
            timeThreshold = (long) minDelayGyro / 1000;
        }

        Log.d("timeThreshold", ( (Long) timeThreshold).toString());

        Switch IMUtoggle = findViewById(R.id.switch_record);
        IMUtoggle.setOnCheckedChangeListener(this);
        IMUtoggle.setChecked(false);

        // Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(IntegratedActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(IntegratedActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(IntegratedActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (audioRecorder != null) {
            audioRecorder.release();
            audioRecorder = null;
        }

        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
        if (isRecordingCamera) {
            stopRecordingCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // isPaused = false; // going to leave this commented out for now as is messing with initialization
        Log.e(LOG_TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(LOG_TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        senSensorManager.unregisterListener(this);
        isPaused = true;
        super.onPause();

    }

    /* Check radio buttons in the layout to tag saved files with terrain type*/
    /*
    protected String getCurrentGroundState() {
        RadioButton grass = findViewById(R.id.grassButton);
        RadioButton gravel = findViewById(R.id.gravelButton);
        RadioButton pavement = findViewById(R.id.pavementButton);
        RadioButton indoor = findViewById(R.id.pavementButton);

        String state_names = "";
        if (grass.isChecked()) {
            state_names += "grass_";
        } if (gravel.isChecked()) {
            state_names += "gravel_";
        } if (pavement.isChecked()) {
            state_names += "pavement_";
        } if (indoor.isChecked()) {
            state_names += "indoor_";
        }
        return state_names;
    }
    */


    /**************************************** Audio methods ****************************************/

    /** Occurs when Record button pressed On/Off **/
    public void onRecordAudio(View view) {
        TextView textView = findViewById(R.id.button_audio);
        if (isRecordingAudio) {
            stopRecordingAudio();
            //textView.setText(getString(R.string.record_text));
        } else {
            startRecordingAudio();
            //textView.setText(getString(R.string.stop_text));
        }
        isRecordingAudio = !isRecordingAudio;
    }

    /** Occurs when Play button pressed On/Off (Not used currently in integrated environment)**/
    public void onPlayAudio(View view) {
        TextView textView = findViewById(R.id.button_audio);
        if (isPlayingAudio) {
            stopPlayingAudio();
            //textView.setText(getString(R.string.play_text));
        } else {
            startPlayingAudio();
            //textView.setText(getString(R.string.stop_text));
        }
        isPlayingAudio = !isPlayingAudio;
    }

    /** Helper method to create a MediaRecorder object and start it **/
    private void startRecordingAudio() {
        //TextView textView_time = findViewById(R.id.textView_time);
        //textView_time.setText(dateDisplayed);

        // Check if sd card is writeable, and retrieve available space
        //TextView textView_sdCardSpace = findViewById(R.id.textView_sdCardSpace) ;

        final File saveFile = new File(getPublicAlbumStorageDir("CARIS"),"AUDIO_"+dateSaved+".3gp");
        fileNameAudio = saveFile.toString();
        String availableSpace = String.valueOf(saveFile.getFreeSpace() / 1024.0 / 1024.0) ;
        availableSpace += " MB";
        //textView_sdCardSpace.setText(availableSpace);

        /* Create media audioRecorder object */
        audioRecorder = new MediaRecorder(); //TODO: Optimize encoding, sampling rate, file format
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // Standard for phone audio
        audioRecorder.setOutputFile(fileNameAudio);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        audioRecorder.setAudioSamplingRate(AUDIO_SAMP_RATE);
        audioRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE);

        /*File saveFile = null;
        if (isExternalStorageWritable()) {
            try {
                saveFile = getPublicAlbumStorageDir("audio");
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "Could not get SD card directory");
            }
        } else { // Record to external cache directory by default
            try {
                saveFile = Environment.getExternalStorageDirectory();
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "Could not get external cache directory");
            }
        }
        fileNameAudio = saveFile.getAbsolutePath();
        fileNameAudio += "/" + dateSaved + ".3gp"; // Filename of output
        */

        try {
            audioRecorder.prepare();
            audioRecorder.start();
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "MediaRecorder is null");
        } catch (IOException e) {
            Log.e(LOG_TAG, "MediaRecorder couldn't open file");
        }
    }

    /** Helper method to end and close MediaRecorder **/
    private void stopRecordingAudio() {
        try {
            audioRecorder.stop();
            audioRecorder.release();
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "MediaRecorder is null");
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, "Could not open file");
        }
        // Retrieve metadata about the recorded file
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(fileNameAudio);
        String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long dur = Long.parseLong(duration);
        String seconds = String.valueOf((dur % 60000) / 1000);
        String minutes = String.valueOf(dur / 60000);
        String audio_length_str = "";

        if (seconds.length() == 1) {
            audio_length_str = "0" + minutes + ":0" + seconds;
        } else {
            audio_length_str = "0" + minutes + ":" + seconds;
        }

        //TextView textView_time = findViewById(R.id.textView_timeAudio);
        //textView_time.setText(audio_length_str);

        audioRecorder = null;
    }

    /** Helper method to create a MediaPlayer Object and start it **/
    private void startPlayingAudio() {
        if (audioRecorded) {
            audioPlayer = new MediaPlayer();
            try {
                audioPlayer.setDataSource(fileNameAudio);
                audioPlayer.prepare();
                audioPlayer.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Player prepare() failed");
            }
        }
        else {
            Toast.makeText(IntegratedActivity.this, "No audio to play!", Toast.LENGTH_LONG).show();
        }
    }

    /** Helper method to close a MediaPlayer Object **/
    private void stopPlayingAudio() {
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    /** Checks if external storage is available for read and write **/
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /** Gets a File representing the appropriate directory on the external storage**/
    public File getPublicAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File[] files = ContextCompat.getExternalFilesDirs(getApplicationContext(), albumName);
        if (!files[0].mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        if (files.length == 1)
            return files[0];
        else
            return files[1];

    }

    /**************************************** Camera methods ****************************************/

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // What goes here
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(LOG_TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    protected boolean takePicture() {
        if (null == cameraDevice) {
            Log.e(LOG_TAG, "cameraDevice is null");
            return false;
        }

        //CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            /* Could uncomment to get native resolution of the camera*/
            //CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            //Size[] jpegSizes = null;
            //if (characteristics != null) {
            //    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            //}
            //if (jpegSizes != null && 0 < jpegSizes.length) {
            //    width = jpegSizes[0].getWidth();
            //    height = jpegSizes[0].getHeight();
            //}
            ImageReader reader = ImageReader.newInstance(IMG_WIDTH, IMG_HEIGHT, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            Surface currentSurface = reader.getSurface();
            if (currentSurface == null) {
                return false;
            }
            outputSurfaces.add(currentSurface);
            SurfaceTexture currentSurfaceTexture = textureView.getSurfaceTexture();
            if (currentSurfaceTexture == null) {
                return false;
            }
            outputSurfaces.add(new Surface(currentSurfaceTexture));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            Date date = new Date();
            String dateSaved = dateFormatSave.format(date);
            final File saveFile = new File(getPublicAlbumStorageDir("CARIS"),"IMG_"+dateSaved+".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }

                    finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(saveFile);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast toast = Toast.makeText(IntegratedActivity.this, "Saved:" + saveFile, Toast.LENGTH_SHORT);
                    //toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 200);
                    //toast.setDuration(Toast.LENGTH_SHORT);
                    //toast.show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return true;
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            assert surface != null;
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(IntegratedActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    protected void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(LOG_TAG, "Camera is open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(IntegratedActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        Log.e(LOG_TAG, "openCamera X");
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(LOG_TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    protected void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    protected void startRecordingCamera() {
        try {
            recordingCameraTask = new IntegratedActivity.RecordingTimerTask();
            recordingCameraTimer = new Timer();
            recordingCameraTimer.scheduleAtFixedRate(recordingCameraTask, 0, RECORD_PERIOD);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    protected void stopRecordingCamera() {
        if (null != recordingCameraTimer) {
            try {
                recordingCameraTimer.cancel();
                recordingCameraTimer.purge();
                recordingCameraTimer = null;
                recordingCameraTask = null;
            } catch(IllegalArgumentException e ) {
                e.printStackTrace();
            }
        }
    }

    /**************************************** IMU methods ****************************************/
    /*
     * save current data batch and start a new one
     */
    public void newBatchTest(View view) {
        long availableSpace = getAvailableSpace();

        if (availableSpace > 1) { // verify there is > 1GB storage
            if (!data.isEmpty()) { // verify an empty batch is not being saved
                if (isPaused) { // verify our data collection switch is OFF before saving
                    saveDataToStorage(data);

                    lastUpdate = 0;
                    totalTime = 0; // reset timer
                    data.clear(); // reset data batch

                } else {
                    toastMessage("Toggle data collection switch OFF before creating new batch");
                }

            } else {
                toastMessage("Cannot save an empty data batch!");
            }

        } else {
            toastMessage("Less than 1GB remaining... \n Please clear space on storage device.");
            lastUpdate = 0;
            totalTime = 0; // reset timer
            data.clear(); // reset data batch
        }
    }

    /*
     * @params arraylist of float arrays containing timestamp and xyz accelerometer data
     * converts to a CSV then saves CSV on external storage.
     */
    public void saveDataToStorage(ArrayList<Float[]> data) {
        try {
            final File csvOutputFile= new File(getPublicAlbumStorageDir("CARIS"),"IMU_"+dateSaved+".txt");;
            FileWriter fw = new FileWriter(csvOutputFile);


            for (Float[] f : data ) { // iterate through and write each series of data to a line in csv format
                fw.write(convertToCSVFormat(f));
                fw.write("\n\r");
            }

            fw.close();

            toastMessage("Data save successful!");

        } catch (IOException ex) {
            Log.e("STORAGE", ex.getMessage(), ex);
            toastMessage("Data save failed.");
        }
    }
    /** Legacy **/
    /*
     * checks if app has permission to write files and prompts user for write permission if
     * it does not initially have it
     */
    /*
    public void checkWriteFilePermission() {
        if( ContextCompat.checkSelfPermission(IntegratedActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions( IntegratedActivity.this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }
    */

    /*
     * @params an array of floats containing timestamp and xyz accelerometer data.
     * @returns a concatenated csv string of the data line
     */
    public String convertToCSVFormat(Float[] data) {
        StringBuilder csvLineBuilder = new StringBuilder();
        for (float f : data) {
            csvLineBuilder.append(f);
            csvLineBuilder.append(",");
        }
        return csvLineBuilder.toString();

    }

    /** Legacy **/
    /*
     * @returns a string containing a concatenated string of 'day of year' & 'time' in 24H format.
     */
    /*
    public String getCurrentTimeAndDate() {
        Calendar cal = Calendar.getInstance();
        Integer day = cal.get(Calendar.DAY_OF_MONTH);
        Integer month = cal.get(Calendar.MONTH);
        Integer year = cal.get(Calendar.YEAR);
        Date time = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("HH_mm_ss", Locale.CANADA);

        // convert day, month, and year values to strings to be added to the final return string
        String formattedDate = dateFormat.format(time);
        String currentDay = day.toString();
        String currentMonth = month.toString();
        String currentYear = year.toString();

        return formattedDate + currentYear + "_" + currentMonth + "_" + currentDay;
    }
    */

    /*
     * toast available free space left on device when prompted
     */
    public void queryFreeSpace(View view) {
        long availableSpace = getAvailableSpace();

        if (availableSpace > 1) {
            Toast toast = Toast.makeText(getApplicationContext(), "Available Space: "+availableSpace+"GB", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 20);
            toast.show();
        } else {
            toastMessage("Less than 1GB remaining... \n Please clear space on storage device.");
        }
    }

    /*
     * @returns available space on phone storage in gb
     */
    public long getAvailableSpace(){
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return ( (long) stat.getAvailableBlocks() * (long) stat.getBlockSize() )/SIZE_GB;
    }

    /*
     * toast a message on app
     */
    public void toastMessage(String message) {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 20);
        toast.show();
    }

    @Override
    /*
     * tracks if IMU data collection switch is toggled ON or OFF
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
        isPaused = !isChecked;
    }

    @Override
    /*
     * when sensor value changes, save data every "timeThreshold" milliseconds
     * saves data in a list of float[] arrays
     */
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (!isPaused) {
            Sensor mySensor = sensorEvent.sensor;
            long currentTime = SystemClock.elapsedRealtime();

            // IF sensorChanged is the accelerometer, create a new line of data including timestamp
            if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION
                    && accelOrGyro) {

                Float[] rowData = new Float[]{(float) totalTime / 1000, sensorEvent.values[0],
                        sensorEvent.values[1], sensorEvent.values[2]};
                data.add(rowData); // adds row of 4 column data (timestamp, x/y/z linear acceleration_

                totalTime += (currentTime - lastUpdate); // keep track of total time of this data batch
                lastUpdate = currentTime; // reset most recent update

                accelOrGyro = false;

                // IF sensorChanged is the gyro, add the gyro data to the most recent existing accelerometer
                // data and set the search for new accelerometer data
            } else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE
                    && !accelOrGyro) {

                // Appending current accelerometer data to add gyro data
                Float[] rowData = new Float[]{sensorEvent.values[0], sensorEvent.values[1],
                        sensorEvent.values[2]};
                int maxIndex = data.size() - 1;
                Float[] temp = data.get(maxIndex);
                Float[] appendedData = {temp[0], temp[1], temp[2], temp[3], rowData[0], rowData[1], rowData[2]};
                data.remove(maxIndex);
                data.add(maxIndex, appendedData);

                totalTime += (currentTime - lastUpdate); // keep track of total time of this data batch
                lastUpdate = currentTime; // reset most recent update

                accelOrGyro = true;
            }
        } else {
            lastUpdate = SystemClock.elapsedRealtime();
        }
    }

    @Override
    // blank for now
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
