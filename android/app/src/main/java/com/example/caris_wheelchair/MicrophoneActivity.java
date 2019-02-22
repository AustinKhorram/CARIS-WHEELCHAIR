package com.example.caris_wheelchair;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MicrophoneActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MicrophoneActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    private static final int AUDIO_BIT_RATE = 255;
    private static final int AUDIO_SAMP_RATE = 16000;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    // Request permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    private boolean isRecording = false;
    private boolean isPlaying = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                                @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (! permissionToRecordAccepted) finish();
    }


    public void onRecord(View view) {
        TextView textView = findViewById(R.id.button_record);
        if (isRecording) {
            stopRecording();
            textView.setText(getString(R.string.record_text));
        } else {
            startRecording();
            textView.setText(getString(R.string.stop_text));
        }
        isRecording = !isRecording;
    }

    public void onPlay(View view) {
        TextView textView = findViewById(R.id.button_play);
        if (isPlaying) {
            stopPlaying();
            textView.setText(getString(R.string.play_text));
        } else {
            startPlaying();
            textView.setText(getString(R.string.stop_text));
        }
        isPlaying = !isPlaying;
    }

    private void startRecording() {
        recorder = new MediaRecorder(); //TODO: Optimize encoding and sampling rate
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // Standard for phone audio
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setAudioSamplingRate(AUDIO_SAMP_RATE);
        recorder.setAudioEncodingBitRate(AUDIO_BIT_RATE);

        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Recorder prepare() failed");
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();

            // Retrieve metadata about the recorded file
            MediaMetadataRetriever mmdr = new MediaMetadataRetriever();
            mmdr.setDataSource(fileName);
            String duration =
                    mmdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long dur = Long.parseLong(duration);
            String seconds = String.valueOf((dur % 60000) / 1000);
            String minutes = String.valueOf(dur / 60000);
            String audio_length_str = "";

            if (seconds.length() == 1) {
                audio_length_str = "0" + minutes + ":0" + seconds;
            } else {
                audio_length_str = "0" + minutes + ":" + seconds;
            }

            TextView textView_time = findViewById(R.id.textView_timeAudio);
            textView_time.setText(audio_length_str);

            recorder = null;
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Player prepare() failed");
        }
    }

    private void stopPlaying() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //TODO: Configure to save to an SD card
        DateFormat dateFormatDisplay = new SimpleDateFormat(getString(R.string.date_display_format));
        DateFormat dateFormatSave = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        String dateDisplayed = dateFormatDisplay.format(date);
        String dateSaved = dateFormatSave.format(date);

        TextView textView_time = findViewById(R.id.textView_time);
        textView_time.setText(dateDisplayed);

        // Record to external cache directory
        try {
            fileName = getExternalCacheDir().getAbsolutePath();
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Could not get external cache directory");
        }
        fileName += "/" + dateSaved + ".3gp";
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }
}
