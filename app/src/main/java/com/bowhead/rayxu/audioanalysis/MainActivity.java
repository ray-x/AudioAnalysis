package com.bowhead.rayxu.audioanalysis;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
//import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import com.google.gson.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;
import com.bowhead.rayxu.audioanalysislib.AnalysisAngerResult;
import com.bowhead.rayxu.audioanalysislib.AnalysisJsonAngerResult;
import java.io.*;
import java.util.concurrent.TimeUnit;

import okhttp3.*;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO = 0;
    private static final String AUDIO_FILE_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/recorded_audio.wav";

    private final String api_key = "7b1d9ca4-d170-4f48-b398-7f8232a9d2ec";
    private OkHttpClient httpClient;
    private Request request;
    private final String TAG = "MainActivity_Log";
    private final String url = "https://token.beyondverbal.com/token";
    private final String recordingurl="https://angdetector.beyondverbal.com/v4/recording/";
    private final String startRecording = recordingurl+"start";
    private String token;
    private String sessionID;
    private static final MediaType MEDIA_TYPE_AUDIO = MediaType.parse("audio/wav");



    public void showDialog(String warning, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(String.format("Result: %s", warning));
        alertDialog.setMessage(String.format("Alert Http connection failure%s", message));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void getToken() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();

        RequestBody requestBody = new FormBody.Builder().add("grant_type","client_credentials")
                .add("apiKey",api_key).build();
        request = new Request.Builder().url(url)
                .post(requestBody).build();
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(5);

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String js = response.body().string();
                Log.i(TAG, js);
                try {
                    JSONObject jsonObject = new JSONObject(js);
                    if (jsonObject.has("access_token")) {
                        token = jsonObject.getString("access_token");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ProgressBar progressBar = findViewById(R.id.progressBar);
                                progressBar.setProgress(50);
                                getCallID();
                            }
                        });


                    }else{
                        Log.i(TAG, "Invalid Key");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("error:", "Invalid Key");
                            }
                        });
                    }
                    // Do something here
                } catch (JSONException e) {
                    Log.i(TAG, e.getMessage());
                    final String errorMsg = e.getMessage();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("error:", errorMsg);
                        }
                    });
                }
            }
        });
    }

    public void getCallID() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();

        JsonObject json = new JsonObject();
        JsonObject jsonType = new JsonObject();
        jsonType.addProperty("type", "WAV");
        String jsons = "{'dataFormat':{'type':'WAV'}}";
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, jsons);


        //token = "vGde6auI6zFkzU1F8G0jxBorsHcTMXSL7prcxZewPtq-rVEb1C7_LsgqS4g6-g7A1EUwMatWmPdiMcuLYpQXeSioWfxrJ9JRHUHaKZkbRCzQMo4fUluCmmwtvV7ZM_tO6BNBKqAZhKyxNPvcSQwxo66uXVv_Xl5vZv-tHoYOzRww8fwXAS7GSQexRQz3zcwVfjOVfj4PL1irXAltz-LuN5dJaZTBs24znqbapnqGp1wGHxIKOC-OtD6VMvqywXsweRfgTqPwfc2CAb6JJ5KF2acUuYuZV0DmMprRJH-PZ97PPO2xElraRtJUhTjwxi1vKxdwQ2pqnJmlTqs3f_-oj8f1jJ4bXwEhNaIS4LeXlyQsI5DC2hENlotwRvSFPVxq_tjigOc-LVQ1OUwdWnXy42p7l5S_oMVLt4K3z-c64JhfVuPGj8xRgV2UQslhHPHr3bLsqtGcDH-QQEpiTJN-GbfV2nHoTCNbsvHvcPInWahaTznpQtjYrQLoYYsXMYuSK-HlFL448N_LpyNnR8zlx2JGmvM8BWDd03oxkFwbDn24T24NGQB7JR6egw9TVlncEh60A7Yk8SxJRYyUW-P6K5JHQWUKnYCU_h-K7S9X5-nKAMewl4Pia_VQUP1BE7ZUTQWvf7wfF-tzfO8Aw4-MKXZBG02EW5PEGzBrvE8d-FYAgU5R9Caf4fI2npjUOgJ5KeR7GkqCA5clOH0yUp-wiv5sRb-nr-djbNaFldD5WfH7D7hg";
        if (token.isEmpty()) {
            Log.i(TAG, "no valide token");
            return;
        }
        final String  headers = "";

        request = new Request.Builder().url(startRecording)
                .addHeader("Authorization", "Bearer " + token)
                .post(body).build();


        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {

                String js = response.body().string();
                int code = response.code();

                Log.i(TAG, String.format("Status code %d %s", code, js));
                if (code>204){
                    final String resp =  response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("httperror", resp);
                        }
                    });
                }

                try {
                    JSONObject jsonObject = new JSONObject(js);
                    if (jsonObject.has("recordingId")) {
                        sessionID = jsonObject.getString("recordingId");
                        Log.i(TAG, "session id " + sessionID);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ProgressBar progressBar = findViewById(R.id.progressBar);
                                progressBar.setProgress(100);
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                    else {
                        Log.i(TAG, "invalid token");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialog("error:", "invalid token");
                            }
                        });
                    }

                    // Do something here
                } catch (JSONException e) {
                    Log.i(TAG, e.getMessage());
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog("error:", "Json error");
                        }
                    });
                }
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(getColor(R.color.colorPrimaryDark)));
        }
        getToken();
        //getCallID();
        cafe.adriel.androidaudiorecorder.example.Util.requestPermission(this, Manifest.permission.RECORD_AUDIO);
        cafe.adriel.androidaudiorecorder.example.Util.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);



        ProgressBar progressBar = findViewById(R.id.progressBar);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        progressBar.setVisibility(View.VISIBLE);  //To show ProgressBar
        //progressBar.setVisibility(View.GONE);     // To Hide ProgressBar

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Audio recorded successfully!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Audio was not recorded", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //    public File getEntityForSampleFile1() {
//        InputStream raw = getResources().openRawResource(R.raw.sample);
//        //InputStreamEntity reqEntity = new InputStreamEntity(raw, -1);
//        return  raw;
//
//    }
//    public File getEntityForSampleFile1() {
//        File file = new File();
//    }
    public void Sample1(View v) {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();


        try{
            InputStream is = getResources().openRawResource(R.raw.audio1);
            final File file = new File(MainActivity.this.getExternalCacheDir(), "audio1.wav");
            try {
                OutputStream os = new FileOutputStream(file);
                int bytesRead = 0;
                byte[]  buffer = new byte[2048];
                while((bytesRead = is.read(buffer,0,2048))!= -1){
                    os.write(buffer,0,bytesRead);
                }
                os.close();
                is.close();
                // upload file
                OkHttpClient httpClient = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(300, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(MEDIA_TYPE_AUDIO, file);
                Request request = new Request.Builder().url(recordingurl+sessionID).addHeader("Authorization", "Bearer " + token).post(body).build();
                //Call call = httpClient.newCall(request);
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        file.delete();
                        Log.i(TAG, "status_error:");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //file.delete();
                        final int code = response.code();
                        final String resp = response.body().string();
                        Log.i(TAG, String.format("status_code: %d %s", response.code() , resp));
                        AnalysisJsonAngerResult apiResult=new AnalysisJsonAngerResult(resp);
                        AnalysisAngerResult jsonResult =  apiResult.convert();

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Handle UI here
                                showDialog("Result:", resp);
                                //findViewById(R.id.loading).setVisibility(View.GONE);
                            }
                        });
                    }
                });


            }catch (Exception e){
                Log.i(TAG, "file not found");
            }

        }catch (Resources.NotFoundException e) {
            Log.i(TAG, "resource not found");
        }
        getCallID();

    }

    private int fileuploaded;
    private int filesize;


    public void Sample2(View v) {
        InputStream is = getResources().openRawResource(R.raw.audio1);

        String filename=MainActivity.this.getExternalCacheDir().getAbsolutePath() + "/audio1.wav";
        final File file = new File(filename);
        if (file.exists()) {
            file.delete();
        }

        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[2048];
            while ((bytesRead = is.read(buffer, 0, 2048)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            is.close();
            // upload file
            //Call call = httpClient.newCall(request);
        } catch (Exception e) {
            Log.i(TAG, "audio not found");
            return;
        }

        if (file.exists()) {
            Log.i(TAG, String.format("file size %d", file.length()));
        }
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(filename)
                .setColor(getColor(R.color.colorPrimary))
                .setRequestCode(REQUEST_RECORD_AUDIO)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.MONO)
                .setSampleRate(AudioSampleRate.HZ_16000)
                .setAutoStart(false)
                .setKeepDisplayOn(true)
                .setApiToken(token)
                .setSessionID(sessionID)

                // Start recording
                .play();
        getCallID();
        //file.delete();
    }

    public void recordAudio(View v) {
        File file = new File(AUDIO_FILE_PATH);
        if (file.exists())
            file.delete();
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(AUDIO_FILE_PATH)
                .setColor(getColor(R.color.colorPrimary))
                .setRequestCode(REQUEST_RECORD_AUDIO)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.MONO)
                .setSampleRate(AudioSampleRate.HZ_16000)
                .setAutoStart(true)
                .setKeepDisplayOn(true)
                .setApiToken(token)
                .setSessionID(sessionID)
                // Start recording
                .record();
        //getCallID();
    }
}
