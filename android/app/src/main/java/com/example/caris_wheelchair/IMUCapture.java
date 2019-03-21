package com.example.caris_wheelchair;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.widget.Toast;
import android.view.View;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.FileWriter;

public class IMUCapture extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    // required for implementation of onSensorChanged(SensorEvent)
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = SystemClock.elapsedRealtime();
    private long totalTime = 0;
    private long timeThreshold = 100;

    private ArrayList<Float[]> lin_accel = new ArrayList<>();// will store the raw IMU data - may expand to other sensors as well

    private boolean isPaused = true; // tracks IMU data capture switch toggle

    final long SIZE_KB = 1024L;  // for storage tracking
    final long SIZE_GB = SIZE_KB * SIZE_KB * SIZE_KB;

    @Override
    // setup
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imucapture);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // enable return to home function
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // initialize sensor variables
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Switch IMUtoggle = findViewById(R.id.imu_data_collection);
        IMUtoggle.setOnCheckedChangeListener(this);
        IMUtoggle.setChecked(false);

    }

    /*
     * save current data batch and start a new one
     */
    public void newBatchTest(View view) {
        long availableSpace = getAvailableSpace();

        if (availableSpace > 1) { // verify there is > 1GB storage
            if (!lin_accel.isEmpty()) { // verify an empty batch is not being saved
                if (isPaused) { // verify our data collection switch is OFF before saving
                    saveDataToStorage(lin_accel);

                    lastUpdate = 0;
                    totalTime = 0; // reset timer
                    lin_accel.clear(); // reset data batch

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
            lin_accel.clear(); // reset data batch
        }
    }

    /*
     * @params arraylist of float arrays containing timestamp and xyz accelerometer data
     * converts to a CSV then saves CSV on external storage.
     */
    public void saveDataToStorage(ArrayList<Float[]> data) {
        //@TODO save the CSV file into the DOWNLOADS folder with naming such that each data set is unique

        try {
            String fileName = "IMU_data_" + getCurrentTimeAndDate() + ".txt";
            File csvOutputFolder = new File(Environment.getExternalStorageDirectory() // folder initialization
                    + "/Android/data/com.example.caris_wheelchair/IMU_data/");

            if (!csvOutputFolder.exists()) { // if folder does not exist, it is created
                csvOutputFolder.mkdirs();
            }

            File csvOutputFile = new File(csvOutputFolder, fileName); // .csv file initialization

            if (!csvOutputFile.exists()) { // if the file does not exist, it is created
                csvOutputFile.createNewFile();
            }

            checkWriteFilePermission(); // Check to make sure we have permission to write files to external storage
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

    /*
     * checks if app has permission to write files and prompts user for write permission if
     * it does not initially have it
     */
    public void checkWriteFilePermission() {
        if( ContextCompat.checkSelfPermission(IMUCapture.this, Manifest.permission.WRITE_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions( IMUCapture.this , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

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

    /*
     * @returns a string containing a concatenated string of 'day of year' & 'time' in 24H format.
     */
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
     * when sensor value changes, save data every 100 ms
     * saves data in a list of float[] arrays
     */
    public void onSensorChanged(SensorEvent sensorEvent){

        if (!isPaused) {
            Sensor mySensor = sensorEvent.sensor;
            long currentTime = SystemClock.elapsedRealtime();

            // reads data every "timeThreshold" milliseconds
            if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && currentTime - lastUpdate > timeThreshold) {
                Float[] rowData = new Float[] {(float)totalTime/1000, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]};
                lin_accel.add(rowData); // adds row of 4 column data (timestamp, x/y/z linear acceleration_

                totalTime += (currentTime - lastUpdate); // keep track of total time of this data batch
                lastUpdate = currentTime; // reset most recent update

            }
        } else {
            lastUpdate = SystemClock.elapsedRealtime();
        }

    }

    @Override
    // blank for now
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    // necessary to define this method for sensor listening
    protected void onPause(){
        super.onPause();
        senSensorManager.unregisterListener(this);
        isPaused = true;
    }

    @Override
    // necessary to define this method for sensor listening
    protected void onResume(){
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // isPaused = false; // going to leave this commented out for now as is messing with initialization
    }

}
