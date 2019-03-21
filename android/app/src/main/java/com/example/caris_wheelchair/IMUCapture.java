package com.example.caris_wheelchair;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
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

public class IMUCapture extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {
    // required for implementation of onSensorChanged(SensorEvent)
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = SystemClock.elapsedRealtime();
    private long totalTime = 0;
    private long timeThreshold = 100;

    // will store the data that we want to output - may expand to other sensors as well
    private ArrayList<Float[]> lin_accel = new ArrayList<>();

    // tracks IMU data capture switch toggle
    private boolean isPaused = true;

    final long SIZE_KB = 1024L;
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

    // save current data batch and start new batch
    public void newBatchTest(View view) {
        //@TODO implement a check for empty batches to ensure empty batches are not being saved
        long availableSpace = getAvailableSpace();

        // needs to verify there is sufficient storage space to start a new batch
        if (availableSpace > 1) {
            if (isPaused) {
//                Toast toast = Toast.makeText(getApplicationContext(), "Batch Size: "+lin_accel.size()+"\nTime: "+totalTime, Toast.LENGTH_LONG );
//                toast.setGravity(Gravity.BOTTOM, 0,20);
//                toast.show();

                //@TODO SAVE DATA IN HERE (call saveDataToStorage())

                saveDataToStorage(lin_accel);

                lastUpdate = 0;
                totalTime = 0; // reset timer
                lin_accel.clear(); // reset data batch

            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Toggle data collection switch OFF before creating new batch", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, 20);
                toast.show();
            }
        } else {
            lowStorageMessage();
            //@TODO must also save and empty the most recent batch if it is not empty!
        }
    }

    /*
     * @params arraylist of float arrays containing timestamp and xyz accelerometer data
     * converts to a CSV then saves CSV on external storage.
     */
    public void saveDataToStorage(ArrayList<Float[]> data) {
        //@TODO save the CSV file into the DOWNLOADS folder with naming such that each data set is unique

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File csvOutputFile = new File(path, "IMU_data_"+getCurrentTimeAndDate()+".csv");


        try {
            FileOutputStream fw = new FileOutputStream(csvOutputFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fw);

            for (Float[] f : data ) {
                myOutWriter.write(convertToCSVFormat(f));
            }

            myOutWriter.close();
            fw.close();

            Toast toast = Toast.makeText(getApplicationContext(), "Data save successful", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 20);
            toast.show();

        } catch (IOException ex) {
            Log.e("STORAGE", ex.getMessage(), ex);
            Toast toast = Toast.makeText(getApplicationContext(), "Data save failed", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 20);
            toast.show();
        }


    }

    /*
     * @params a string containing a concatenated string of 'day of year' & 'time' in 24H format.
     * @returns a
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
        // initializations
        Calendar cal = Calendar.getInstance();
        Integer day = cal.get(Calendar.DAY_OF_MONTH);
        Integer month = cal.get(Calendar.MONTH);
        Integer year = cal.get(Calendar.YEAR);
        Date time = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.CANADA);

        // convert day, month, and year values to strings to be added to the final return string
        String formattedDate = dateFormat.format(time);
        String currentDay = day.toString();
        String currentMonth = month.toString();
        String currentYear = year.toString();

        return formattedDate + currentYear + "_" + currentMonth + "_" + currentDay;
    }

    /*
     * toast available free space left on device
     */
    public void queryFreeSpace(View view) {
        long availableSpace = getAvailableSpace();

        if (availableSpace > 1) {
            Toast toast = Toast.makeText(getApplicationContext(), "Available Space: "+availableSpace+"GB", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, 20);
            toast.show();
        } else {
            lowStorageMessage();
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
     * toast a low storage message on app
     */
    public void lowStorageMessage() {
        Toast toast = Toast.makeText(getApplicationContext(), "Less than 1GB remaining\nPlease clear space on device", Toast.LENGTH_LONG);
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
