package com.example.shaban.readsensorsapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends WearableActivity {

    private String serverIp0;
    private String serverIp1;
    private String serverIp2;
    private String serverIp3;

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometerSensor;
    private Sensor gyroSensor;
    private Sensor linearAccelerometerSensor;

    private Spinner serverIpSpinner0;
    private Spinner serverIpSpinner1;
    private Spinner serverIpSpinner2;
    private Spinner serverIpSpinner3;
    private Spinner numOfSamplesSpinner;
    private Spinner fileNameSpinner;
    private Button startBtn;
    private Button storeBtn;

    private String header = "seconds,ax,ay,az,amag,lax,lay,laz,lamag,gyrox,gyroy,gyroz,rotaionx,rotaiony,rotaionz \r\n";
    private String data;
    private int counter;
    private int numOfSamples;
    private String fileName = "";
    private long prevRecordTime;
    private long currentTime;
    private long prevListenerTime;
    private boolean recordingEnable = false;
    float ax, ay, az, lax, lay, laz, gyrox, gyroy, gyroz;
    float curAngleX,curAngleY,curAngleZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileName = "a";
        numOfSamples = 100;

        Integer[] numbersOption = new Integer[256];
        for (int i = 0 ; i < 256 ; i++)
            numbersOption[i] = i;

        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        Character[] charactersOption = new Character[36];
        for (int i = 0 ; i < characters.length() ; i++)
            charactersOption[i] = characters.charAt(i);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        linearAccelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.start();

        serverIpSpinner0 = (Spinner)findViewById(R.id.serverIpSpinner0);
        serverIpSpinner1 = (Spinner)findViewById(R.id.serverIpSpinner1);
        serverIpSpinner2 = (Spinner)findViewById(R.id.serverIpSpinner2);
        serverIpSpinner3 = (Spinner)findViewById(R.id.serverIpSpinner3);
        numOfSamplesSpinner = (Spinner)findViewById(R.id.numOfSamplesSpinner);
        fileNameSpinner = (Spinner)findViewById(R.id.fileNameSpinner);

        ArrayAdapter<Integer> numbersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, numbersOption);
        serverIpSpinner0.setAdapter(numbersAdapter);
        serverIpSpinner1.setAdapter(numbersAdapter);
        serverIpSpinner2.setAdapter(numbersAdapter);
        serverIpSpinner3.setAdapter(numbersAdapter);
        numOfSamplesSpinner.setAdapter(numbersAdapter);
        ArrayAdapter<Character> charactersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, charactersOption);
        fileNameSpinner.setAdapter(charactersAdapter);

        serverIpSpinner0.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serverIp0 = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        serverIpSpinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serverIp1 = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        serverIpSpinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serverIp2 = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        serverIpSpinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serverIp3 = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        numOfSamplesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                numOfSamples = Integer.parseInt(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fileNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fileName = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        startBtn = (Button) findViewById(R.id.startBtn);
        storeBtn = (Button) findViewById(R.id.storeBtn);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBtn.setVisibility(View.INVISIBLE);
                recordingEnable = true;
                data = header;
                counter = 0;
                prevRecordTime = System.currentTimeMillis();
                prevListenerTime = System.currentTimeMillis();
                storeBtn.setVisibility(View.VISIBLE);
            }
        });

        storeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeBtn.setVisibility(View.INVISIBLE);
                String serverIpText = serverIp0 + "." + serverIp1 + "." + serverIp2 + "." + serverIp3;
                String url = "http://" + serverIpText + ":8081/store";
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
                if (recordingEnable) {
                    currentTime = System.currentTimeMillis();
                    String str = "";
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        ax = event.values[0];
                        ay = event.values[1];
                        az = event.values[2];
                    } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        gyrox = event.values[0];
                        gyroy = event.values[1];
                        gyroz = event.values[2];
                        curAngleX = getNewCurrentAngle(curAngleX, gyrox, currentTime - prevListenerTime);
                        curAngleX = getNewCurrentAngle(curAngleY, gyroy, currentTime - prevListenerTime);
                        curAngleX = getNewCurrentAngle(curAngleZ, gyroz, currentTime - prevListenerTime);
                        prevListenerTime = System.currentTimeMillis();
                    } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        lax = event.values[0];
                        lay = event.values[1];
                        laz = event.values[2];
                    }

                    if (counter <= numOfSamples && ((currentTime - prevRecordTime) >= 10)) {
                        String record = (10 * counter) + ",  " + ax + ",  " + ay + ",   " + az + "  ,  " + getMagnitude(ax, ay, az);
                        record += ",  " + lax + ",  " + lay + ",   " + laz + "  ,  " + getMagnitude(lax, lay, laz);
                        record += ",  " + gyrox + "  ,  " + gyroy + " ,   " + gyroz;
                        record += ",  " + curAngleX + "  ,  " + curAngleY + " ,   " + curAngleZ + "\r\n";
                        data = data + record;
                        prevRecordTime = System.currentTimeMillis();
                        counter++;
                    }
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


    private class SendDataToServer extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... params) {
            String JsonResponse = null;
            String JsonDATA = params[0];
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("http://192.168.0.53:8081/store");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                // is output buffer writter
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                //set headers and method
                Writer writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(JsonDATA);
                // json data
                writer.close();
                InputStream inputStream = urlConnection.getInputStream();
                //input stream
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String inputLine;
                while ((inputLine = reader.readLine()) != null)
                    buffer.append(inputLine + "\n");
                if (buffer.length() == 0) {
                    // Stream was empty. No point in parsing.
                    return null;
                }
                JsonResponse = buffer.toString();
                //response data
                Log.i("JsonResponse:",JsonResponse);
                try {
                    //send to post execute
                    return JsonResponse;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;



            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("Error", "Error closing stream", e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
        }

    }



}
