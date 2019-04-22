package com.example.caris_wheelchair;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.caris_wheelchair.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Check if this device has a camera
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**Called when user taps the Send button on the username field**/
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String user = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, user);
        startActivity(intent);
    }

    /**Called when user taps the Send button on the username field**/
    public void useCamera(View view) {
        Intent intent = new Intent(this, CameraCapture.class);
        startActivity(intent);
    }

    /** Called when the user taps the Microphone button**/
    public void openMicrophone(View view) {
        Intent intent = new Intent(this, MicrophoneActivity.class);
        startActivity(intent);
    }

    /** Called when the user taps the Microphone button**/
    public void openIntegrated(View view) {
        Intent intent = new Intent(this, IntegratedActivity.class);
        startActivity(intent);
    }
    /**called when user taps "IMU" button on main activity**/
    public void startIMU(View view){
        Intent intent = new Intent(MainActivity.this, IMUCapture.class);
        startActivity(intent);
    }
}
