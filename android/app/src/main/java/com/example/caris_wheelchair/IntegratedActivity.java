package com.example.caris_wheelchair;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class IntegratedActivity extends AppCompatActivity {

    /*
    General
    */
    static final String LOG_TAG = "IntegratedActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static String fileNameAudio;

    DateFormat dateFormatDisplay;
    DateFormat dateFormatSave;
    Date date;
    String dateDisplayed;
    String dateSaved;

    /*
    Audio specific
     */

    private static final int AUDIO_BIT_RATE = 255;
    private static final int AUDIO_SAMP_RATE = 16000;

    private MediaRecorder audioRecorder;
    private MediaPlayer audioPlayer;

    private boolean audioRecorded = false;

    // Request permission to RECORD_AUDIO
    //private String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    private boolean isRecordingAudio = false;
    private boolean isPlayingAudio = false;

    /*
    Camera specific
     */

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

    /** Android Camera2 relevant objects **/
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
    private final int IMG_WIDTH = 768;
    private final int IMG_HEIGHT = 1024;

    /**************************************** General methods ****************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integrated);

        dateFormatDisplay = new SimpleDateFormat(getString(R.string.date_save_format));
        dateFormatSave = new SimpleDateFormat(getString(R.string.date_save_format));
        date = new Date();
        dateDisplayed = dateFormatDisplay.format(date);
        dateSaved = dateFormatSave.format(date);

        textureView = findViewById(R.id.cameraPreview);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = findViewById(R.id.button_photo);
        assert takePictureButton != null;

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
        super.onPause();
    }

    /**************************************** Audio methods ****************************************/

    /** Occurs when Record button pressed On/Off **/
    public void onRecordAudio(View view) {
        TextView textView = findViewById(R.id.button_audio);
        if (isRecordingAudio) {
            stopRecordingAudio();
            textView.setText(getString(R.string.record_text));
        } else {
            startRecordingAudio();
            textView.setText(getString(R.string.stop_text));
        }
        isRecordingAudio = !isRecordingAudio;
    }

    /** Occurs when Play button pressed On/Off (Not used currently in integrated env)**/
    public void onPlayAudio(View view) {
        TextView textView = findViewById(R.id.button_audio);
        if (isPlayingAudio) {
            stopPlayingAudio();
            textView.setText(getString(R.string.play_text));
        } else {
            startPlayingAudio();
            textView.setText(getString(R.string.stop_text));
        }
        isPlayingAudio = !isPlayingAudio;
    }

    /** Helper method to create a MediaRecorder object and start it **/
    private void startRecordingAudio() {
        //TextView textView_time = findViewById(R.id.textView_time);
        //textView_time.setText(dateDisplayed);

        // Check if sd card is writeable, and retrieve available space
        //TextView textView_sdCardSpace = findViewById(R.id.textView_sdCardSpace) ;

        final File saveFile = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.example.caris_wheelchair/"+dateSaved+".3gp");
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
            assert currentSurface != null;
            outputSurfaces.add(currentSurface);
            SurfaceTexture currentSurfaceTexture = textureView.getSurfaceTexture();
            assert currentSurfaceTexture != null;
            outputSurfaces.add(new Surface(currentSurfaceTexture));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            Date date = new Date();
            String dateSaved = dateFormatSave.format(date);
            final File file = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.example.caris_wheelchair/"+dateSaved+".jpg");
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
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
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
                    Toast toast = Toast.makeText(IntegratedActivity.this, "Saved:" + file, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 200);
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.show();
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
}
