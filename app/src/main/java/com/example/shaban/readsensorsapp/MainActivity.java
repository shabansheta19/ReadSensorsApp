package com.example.shaban.readsensorsapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends WearableActivity {

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometerSensor;
    private Sensor gyroSensor;
    private Sensor linearAccelerometerSensor;


    private EditText serverIpEditText;
    private EditText numOfSamplesEditText;
    private EditText fileNameEditText;
    private TextView resultTextView;
    private Button startBtn;
    private Button storeBtn;

    private String header = "seconds, ax, ay, az, amag, lax, lay, laz, lamag, gyrox, gyroy, gyroz, rotaionx, rotaiony, rotaionz \\r\\n";
    private String data;
    private int counter;
    private int numOfSamples;
    private String fileName = "";
    private long prevRecordTime;
    private long currentTime;
    private long prevListenerTime;

    private boolean recordingEnable = false;

    float ax, ay, az, amag, lax, lay, laz, lamag, gyrox, gyroy, gyroz, rotaionx, rotaiony, rotaionz;
    float curAngleX,curAngleY,curAngleZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        linearAccelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.start();

        serverIpEditText = (EditText)findViewById(R.id.serverIpEditTxt);
        numOfSamplesEditText = (EditText)findViewById(R.id.numOfSamplesEditTxt);
        fileNameEditText = (EditText)findViewById(R.id.fileNameEditTxt);
        resultTextView = (TextView)findViewById(R.id.resultTextView);
        startBtn = (Button) findViewById(R.id.startBtn);
        storeBtn = (Button) findViewById(R.id.storeBtn);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtn.setVisibility(View.INVISIBLE);
                recordingEnable = true;
                data = header;
                counter = 0;
                numOfSamples = Integer.parseInt(numOfSamplesEditText.getText().toString());
                prevRecordTime = System.currentTimeMillis();
                prevListenerTime = System.currentTimeMillis();
                storeBtn.setVisibility(View.VISIBLE);
                fileName = fileNameEditText.getText().toString();
            }
        });

        storeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeBtn.setVisibility(View.INVISIBLE);
                String url = "http://" + serverIpEditText.getText().toString() + ":8081/store";
                JSONObject jsonObj = new JSONObject();
                try {
                    jsonObj.put("fileName", fileName);
                    jsonObj.put("data", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JsonObjectRequest jsObjRequest = new
                        JsonObjectRequest(Request.Method.POST, url, jsonObj,new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("Response","success");
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Response","failed");
                            }
                        });
                requestQueue.add(jsObjRequest);
                recordingEnable = false;
                startBtn.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensor();
    }

    void registerSensor() {
        //just in case
        if (sensorManager == null)
            sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // I have no desire to deal with the accuracy events

            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                currentTime = System.currentTimeMillis();
                String str = "";
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    ax = event.values[0];
                    ay = event.values[1];
                    az = event.values[2];
                    str = "ACCELEROMETER_x = " + Float.toString(ax);
                } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gyrox = event.values[0];
                    gyroy = event.values[1];
                    gyroz = event.values[2];
                    curAngleX = getNewCurrentAngle(curAngleX,gyrox,currentTime- prevListenerTime);
                    curAngleX = getNewCurrentAngle(curAngleY,gyroy,currentTime- prevListenerTime);
                    curAngleX = getNewCurrentAngle(curAngleZ,gyroz,currentTime- prevListenerTime);
                    prevListenerTime = System.currentTimeMillis();
                    str = "GYROSCOPE_x = " + Float.toString(gyrox);
                } else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    lax = event.values[0];
                    lay = event.values[1];
                    laz = event.values[2];
                    str = "LINEAR_ACCELERATION_x = " + Float.toString(lax);
                }
                if (resultTextView != null)
                    resultTextView.setText(str);

                if (recordingEnable && counter <= numOfSamples && ((currentTime - prevRecordTime) >= 100)) {
                    String record = (10*counter) + ",  " + ax +",  " + ay + ",   " + az + "  ,  " + getMagnitude(ax,ay,az);
                    record += ",  " + lax + ",  " + lay + ",   " + laz + "  ,  " + getMagnitude(lax,lay,laz);
                    record += ",  " + gyrox + "  ,  " + gyroy + " ,   " + gyroz;
                    record += ",  " + curAngleX +"  ,  " + curAngleY + " ,   " + curAngleZ + "\\r\\n";
                    data = data + record;
                    prevRecordTime = System.currentTimeMillis();
                }
            }
        };
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, linearAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    void unregisterSensor() {
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        //clean up and release memory.
        sensorManager = null;
        sensorEventListener = null;
    }


    private float getMagnitude(float x,float y,float z){
        return (float) Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2)+Math.pow(z, 2));
    }

    private float getNewCurrentAngle(float curAngle , float eventAngle , long dt){
        float currentAngle = (curAngle + (eventAngle *dt))%360;
        return currentAngle;
    }

}
