package cafe.adriel.androidaudiorecorder;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

//import cafe.adriel.androidaudiorecorder.model.WavHeader;
import cafe.adriel.androidaudiorecorder.model.WavHeader;
import com.bowhead.rayxu.audioanalysislib.AnalysisAngerResult;
import com.bowhead.rayxu.audioanalysislib.AnalysisJsonAngerResult;
import com.cleveroad.audiovisualization.DbmHandler;
import com.cleveroad.audiovisualization.GLAudioVisualizationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;

import com.google.gson.JsonObject;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import okio.Pipe;
import okio.Sink;
import omrecorder.*;

import org.json.JSONException;
import org.json.JSONObject;


import java.util.concurrent.TimeUnit;



public class AudioRecorderActivity extends AppCompatActivity
        implements PullTransport.OnAudioChunkPulledListener, MediaPlayer.OnCompletionListener {

    private String filePath;
    private AudioSource source;
    private AudioChannel channel;
    private AudioSampleRate sampleRate;
    private int color;
    private boolean autoStart;
    private boolean keepDisplayOn;

    private MediaPlayer player;
    private Recorder recorder;
    private VisualizerHandler visualizerHandler;

    private Timer timer;
    private MenuItem saveMenuItem;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;
    private boolean isRecording;

    private RelativeLayout contentLayout;
    private GLAudioVisualizationView visualizerView;
    private TextView statusView;
    private TextView timerView;
    private TextView analysisResult;
    private ImageButton restartView;
    private ImageButton recordView;
    private ImageButton playView;
    private String token;
    private String sessionID;
    private final String TAG = "Recorder/Player";
    private final String url = "https://token.beyondverbal.com/token";
    private final String recordingurl="https://angdetector.beyondverbal.com/v4/recording/";
    private final String startRecording = recordingurl+"start";
    private static final MediaType MEDIA_TYPE_AUDIO = MediaType.parse("audio/wav");
    private int offset;
    private int curBolckSize;
    PipeBody pipeBody;
    Request request;

    static final class PipeBody extends RequestBody {
        private final Pipe pipe = new Pipe(128*1024);
        private final BufferedSink sink = Okio.buffer(pipe.sink());

        public BufferedSink sink() {
            return sink;
        }

        @Override public MediaType contentType() {
            return null;
        }

        @Override public void writeTo(BufferedSink sink) throws IOException {
            sink.writeAll(pipe.source());
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.aar_activity_audio_recorder);

        if(savedInstanceState != null) {
            filePath = savedInstanceState.getString(AndroidAudioRecorder.EXTRA_FILE_PATH);
            source = (AudioSource) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_SOURCE);
            channel = (AudioChannel) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_CHANNEL);
            sampleRate = (AudioSampleRate) savedInstanceState.getSerializable(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
            color = savedInstanceState.getInt(AndroidAudioRecorder.EXTRA_COLOR);
            autoStart = savedInstanceState.getBoolean(AndroidAudioRecorder.EXTRA_AUTO_START);
            keepDisplayOn = savedInstanceState.getBoolean(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON);
            sessionID = savedInstanceState.getString(AndroidAudioRecorder.EXTRA_API_SESSION_ID);
            token = savedInstanceState.getString(AndroidAudioRecorder.EXTRA_API_TOKEN);
        } else {
            filePath = getIntent().getStringExtra(AndroidAudioRecorder.EXTRA_FILE_PATH);
            source = (AudioSource) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SOURCE);
            channel = (AudioChannel) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_CHANNEL);
            sampleRate = (AudioSampleRate) getIntent().getSerializableExtra(AndroidAudioRecorder.EXTRA_SAMPLE_RATE);
            color = getIntent().getIntExtra(AndroidAudioRecorder.EXTRA_COLOR, Color.BLACK);
            autoStart = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_AUTO_START, false);
            keepDisplayOn = getIntent().getBooleanExtra(AndroidAudioRecorder.EXTRA_KEEP_DISPLAY_ON, false);
            sessionID = getIntent().getStringExtra(AndroidAudioRecorder.EXTRA_API_SESSION_ID);
            token = getIntent().getStringExtra(AndroidAudioRecorder.EXTRA_API_TOKEN);
        }

        if(keepDisplayOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(Util.getDarkerColor(color)));
            getSupportActionBar().setHomeAsUpIndicator(
                    ContextCompat.getDrawable(this, R.drawable.aar_ic_clear));
        }

        visualizerView = new GLAudioVisualizationView.Builder(this)
                .setLayersCount(1)
                .setWavesCount(8)
                .setWavesHeight(R.dimen.aar_wave_height)
                .setWavesFooterHeight(R.dimen.aar_footer_height)
                .setBubblesPerLayer(20)
                .setBubblesSize(R.dimen.aar_bubble_size)
                .setBubblesRandomizeSize(true)
                .setBackgroundColor(Util.getDarkerColor(color))
                .setLayerColors(new int[]{color})
                .build();

        contentLayout = (RelativeLayout) findViewById(R.id.content);
        statusView = (TextView) findViewById(R.id.status);
        timerView = (TextView) findViewById(R.id.timer);
        analysisResult = (TextView) findViewById(R.id.AnalysisResult);
        restartView = (ImageButton) findViewById(R.id.restart);
        recordView = (ImageButton) findViewById(R.id.record);
        playView = (ImageButton) findViewById(R.id.play);

        contentLayout.setBackgroundColor(Util.getDarkerColor(color));
        contentLayout.addView(visualizerView, 0);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);

        if(Util.isBrightColor(color)) {
            ContextCompat.getDrawable(this, R.drawable.aar_ic_clear)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            ContextCompat.getDrawable(this, R.drawable.aar_ic_check)
                    .setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
            statusView.setTextColor(Color.BLACK);
            timerView.setTextColor(Color.BLACK);
            analysisResult.setTextColor(Color.BLACK);
            restartView.setColorFilter(Color.BLACK);
            recordView.setColorFilter(Color.BLACK);
            playView.setColorFilter(Color.BLACK);
        }
        pipeBody = new PipeBody();
        Log.i(TAG, "oncreate");
    }

    public void updateCallID() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
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
                }

                try {
                    JSONObject jsonObject = new JSONObject(js);
                    if (jsonObject.has("recordingId")) {
                        sessionID = jsonObject.getString("recordingId");
                        Log.i(TAG, "session id " + sessionID);
                    }
                    else {
                        Log.i(TAG, "invalid token");
                    }

                    // Do something here
                } catch (JSONException e) {
                    Log.i(TAG, e.getMessage());
                }
            }
        });
    }


    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(autoStart && !isRecording){
            toggleRecording(null);
        }else{
            togglePlaying(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            visualizerView.onResume();
        } catch (Exception e){ }
    }

    @Override
    protected void onPause() {
        restartRecording(null);
        try {
            visualizerView.onPause();
        } catch (Exception e){ }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        restartRecording(null);
        setResult(RESULT_CANCELED);
        try {
            visualizerView.release();
        } catch (Exception e){ }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(AndroidAudioRecorder.EXTRA_FILE_PATH, filePath);
        outState.putInt(AndroidAudioRecorder.EXTRA_COLOR, color);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.aar_audio_recorder, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        saveMenuItem.setIcon(ContextCompat.getDrawable(this, R.drawable.aar_ic_check));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
        } else if (i == R.id.action_save) {
            selectAudio();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAudioChunkPulled(AudioChunk audioChunk) {
        float amplitude = isRecording ? (float) audioChunk.maxAmplitude() : 0f;
        visualizerHandler.onDataReceived(amplitude);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopPlaying();
    }

    private void selectAudio() {
        stopRecording();
        setResult(RESULT_OK);
        finish();
    }

    public void toggleRecording(View v) {
        stopPlaying();
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    pauseRecording();
                } else {
                    resumeRecording();
                }
            }
        });
    }

    public void togglePlaying(View v){
        pauseRecording();
        Util.wait(100, new Runnable() {
            @Override
            public void run() {
                if(isPlaying()){
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });
    }

    public void restartRecording(View v){
        offset = 0;
        curBolckSize = 0;
        if(isRecording) {
            stopRecording();
        } else if(isPlaying()) {
            stopPlaying();
        } else {
            visualizerHandler = new VisualizerHandler();
            visualizerView.linkTo(visualizerHandler);
            visualizerView.release();
            if(visualizerHandler != null) {
                visualizerHandler.stop();
            }
        }
        saveMenuItem.setVisible(false);
        statusView.setVisibility(View.INVISIBLE);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);
        recordView.setImageResource(R.drawable.aar_ic_rec);
        timerView.setText("00:00:00");
        analysisResult.setText("Waiting server response....");
        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;
    }


//    public PullableSource mic() {
//        return new PullableSource.Default(
//                new AudioRecordConfig.Default(
//                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
//                        AudioFormat.CHANNEL_IN_MONO, 44100
//                )
//        );
//    }
    private void resumeRecording() {
        isRecording = true;
        saveMenuItem.setVisible(false);
        statusView.setText(R.string.aar_recording);
        statusView.setVisibility(View.VISIBLE);
        restartView.setVisibility(View.INVISIBLE);
        playView.setVisibility(View.INVISIBLE);
        recordView.setImageResource(R.drawable.aar_ic_pause);
        playView.setImageResource(R.drawable.aar_ic_play);

        visualizerHandler = new VisualizerHandler();
        visualizerView.linkTo(visualizerHandler);

        if(recorder == null) {
            timerView.setText("00:00:00");
            analysisResult.setText("Waiting server response....");

            recorder = OmRecorder.wav(
                    new PullTransport.Default(Util.getMic(source, channel, sampleRate), AudioRecorderActivity.this),
                    new File(filePath));
//            recorder = OmRecorder.wav(
//                    new PullTransport.Default(mic(), new PullTransport.OnAudioChunkPulledListener() {
//                        @Override public void onAudioChunkPulled(AudioChunk audioChunk) {
//                            //animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
//                        }
//                    }), new File(filePath));

            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
            pipeBody = new PipeBody();
            request = new Request.Builder()
                    .url(recordingurl+sessionID)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(pipeBody)
                    .build();
            Log.i(TAG, "begin playing");

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

                        Log.e(TAG, resp);
                    }

                }
            });
        }
        recorder.resumeRecording();

        startTimer();
    }

    private void pauseRecording() {
        isRecording = false;
        if(!isFinishing() && saveMenuItem != null) {
            saveMenuItem.setVisible(true);
        }
        statusView.setText(R.string.aar_paused);
        statusView.setVisibility(View.VISIBLE);
        restartView.setVisibility(View.VISIBLE);
        playView.setVisibility(View.VISIBLE);
        recordView.setImageResource(R.drawable.aar_ic_rec);
        playView.setImageResource(R.drawable.aar_ic_play);

        visualizerView.release();
        if(visualizerHandler != null) {
            visualizerHandler.stop();
        }

        if (recorder != null) {
            recorder.pauseRecording();
        }

        stopTimer();
    }

    private void stopRecording(){
        visualizerView.release();
        if(visualizerHandler != null) {
            visualizerHandler.stop();
        }

        recorderSecondsElapsed = 0;
        if (recorder != null) {
            try{
                recorder.stopRecording();
            }catch (Exception e){
                Log.e(TAG, e.getLocalizedMessage());
            }

            recorder = null;
        }
        try {
            pipeBody.sink.close();
        }catch (Exception e){
            Log.e(TAG, e.getLocalizedMessage());
        }
        stopTimer();
    }

    private void startPlaying(){
        try {
            stopRecording();
            Log.i(TAG, "creating pipe");
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
            pipeBody = new PipeBody();
            request = new Request.Builder()
                    .url(recordingurl+sessionID)
                    .addHeader("Authorization", "Bearer " + token)
                    .post(pipeBody)
                    .build();
            Log.i(TAG, "beging playing");

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

                        Log.e(TAG, resp);
                    }

                }
            });
            offset = 0;
            curBolckSize = 0;
            player = new MediaPlayer();
            File file = new File(filePath);

            player.setDataSource(filePath);
            player.prepare();
            player.start();

            visualizerView.linkTo(DbmHandler.Factory.newVisualizerHandler(this, player));
            visualizerView.post(new Runnable() {
                @Override
                public void run() {
                    player.setOnCompletionListener(AudioRecorderActivity.this);
                }
            });

            timerView.setText("00:00:00");
            statusView.setText(R.string.aar_playing);
            analysisResult.setText("Waiting server response....");
            statusView.setVisibility(View.VISIBLE);
            playView.setImageResource(R.drawable.aar_ic_stop);

            playerSecondsElapsed = 0;
            startTimer();
        } catch (Exception e){
            //e.printStackTrace();
            Log.e(TAG, "startPlaying failed" + e.getMessage());
        }
    }

    private void stopPlaying(){
        statusView.setText("");
        statusView.setVisibility(View.INVISIBLE);
        playView.setImageResource(R.drawable.aar_ic_play);

        visualizerView.release();
        if(visualizerHandler != null) {
            visualizerHandler.stop();
        }

        if(player != null){
            try {
                player.stop();
                player.reset();
            } catch (Exception e){ }
        }
        try {
            pipeBody.sink.close();
        }catch (Exception e){
            Log.e(TAG, e.getLocalizedMessage());
        }

        stopTimer();
        //updateCallID();
    }

    private boolean isPlaying(){
        try {
            return player != null && player.isPlaying() && !isRecording;
        } catch (Exception e){
            return false;
        }
    }

    private void startTimer(){
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer(){
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void updateTimer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isRecording) {
                    recorderSecondsElapsed++;
                    timerView.setText(Util.formatSeconds(recorderSecondsElapsed));
                    Log.i(TAG, String.format("recording %d", recorderSecondsElapsed));
                    if (recorderSecondsElapsed>=2 && recorderSecondsElapsed<3)
                        writeSink();
                    if (recorderSecondsElapsed>=4 && recorderSecondsElapsed<5)
                         getAnalysisResult();

                } else if(isPlaying()){
                    playerSecondsElapsed++;
                    Log.i(TAG, String.format("playing %d", playerSecondsElapsed));
                    timerView.setText(Util.formatSeconds(playerSecondsElapsed));
                    if (playerSecondsElapsed>=1 && playerSecondsElapsed<2)
                        writeSink();
                    if (playerSecondsElapsed>=4 && playerSecondsElapsed<5)
                        getAnalysisResult();

                }
            }
        });
    }

    protected void getAnalysisResult() {
        class Result {
            private float offset;
            private float score;
            private float duration;
            private Boolean canStop;


            public void setDuration(float duration) {
                this.duration = duration;
            }

            public float getDuration() {
                return duration;
            }

            public void setOffset(float offset) {
                this.offset = offset;
            }

            public void setScore(float score) {
                this.score = score;
            }

            public float getOffset() {
                return offset;
            }

            public float getScore() {
                return score;
            }

            public void setCanStop(Boolean canStop) {
                this.canStop = canStop;
            }
            public Boolean getCanStop() {
                return canStop;
            }
            public Result() {
                this.offset = 0;
                this.score = -1;
                this.duration = 0;
                canStop = false;
            }
        }

        Thread thread = new Thread("reader") {
            final Result jsonresult = new Result();
            @Override public void run() {
                final OkHttpClient client = new OkHttpClient();
                //RequestBody body = RequestBody.create(null, blockData);
                if (sessionID == null || token == null)
                    return;

                //切割文件为32KB (4s)
                float offset = -1;
                while (true) {
                    {
                        String url = String.format("%s%s/analysis?fromMs=%d", recordingurl, sessionID, Math.round(jsonresult.offset));
                        Request getRequest = new Request.Builder().url(url)
                                .addHeader("Authorization", "Bearer " + token)
                                .build();
                        offset = jsonresult.offset;
                        if (offset < 1) {
                            try{
                                Thread.sleep(2000);
                            }catch (Exception e){
                                Log.e(TAG, e.getLocalizedMessage());
                            }
                        }
                        client.newCall(getRequest).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.e(TAG, e.getLocalizedMessage());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String resp = response.body().string();
                                float anger = 0;
                                AnalysisJsonAngerResult apiResult = new AnalysisJsonAngerResult(resp);
                                final AnalysisAngerResult jsonResult = apiResult.convert();
                                if (jsonResult.analysisSegments.size() == 0) {
                                    try{
                                        Thread.sleep(2000);
                                    }catch (Exception e){
                                        Log.e(TAG, e.getLocalizedMessage());
                                    }
                                }
                                for (int i = 0; i < jsonResult.analysisSegments.size(); i++) {
                                    jsonresult.offset = jsonResult.analysisSegments.get(i).offset + jsonResult.analysisSegments.get(i).duration;
                                    jsonresult.score = Math.max(jsonresult.score, jsonResult.analysisSegments.get(i).getScore());
                                }
                                if (jsonResult.analysisSegments.size() > 0) {
                                    final int _anger = Math.round(jsonresult.score);
                                    //"status":"success","result":{"duration":"140000.00","sessionStatus":"Done","analysisSummary":{"AnalysisResult":{"Anger/Dislike":{"Mode":0,"ModePct":"74.07"}}}}}
                                    try {
                                        JSONObject summaryJson = new JSONObject(resp);
                                        if (summaryJson.has("result")) {
                                            if (summaryJson.getJSONObject("result").has("analysisSummary")) {
                                                if (summaryJson.getJSONObject("result").getJSONObject("analysisSummary").has("AnalysisResult")) {
                                                    JSONObject result = summaryJson.getJSONObject("result").getJSONObject("analysisSummary").getJSONObject("AnalysisResult").getJSONObject("Anger/Dislike");
                                                    final int finalAnger = Math.round(Float.parseFloat(result.getString("ModePct")));
                                                    AudioRecorderActivity.this.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (finalAnger >= 0) {
                                                                if (finalAnger >= 80) {
                                                                    analysisResult.setText(String.format("Summary: Anger value: %d !!", finalAnger));
                                                                    analysisResult.setTextSize(24);
                                                                    //analysisResult.setAllCaps(true);
                                                                } else {
                                                                    analysisResult.setText(String.format("Summary: emotion value: %d", finalAnger));
                                                                    analysisResult.setAllCaps(false);
                                                                }
                                                                jsonresult.setScore(1);
                                                            }

                                                        }
                                                    });
                                                    jsonresult.setCanStop(true);


                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, e.getLocalizedMessage());
                                    }


                                    AudioRecorderActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (_anger >= 0) {

                                                if (_anger >= 80) {
                                                    analysisResult.setText(String.format("Anger value: %d !!", _anger));
                                                    analysisResult.setTextSize(24);
                                                    //analysisResult.setAllCaps(true);
                                                } else {
                                                    analysisResult.setText(String.format("emotion value: %d", _anger));
                                                    analysisResult.setAllCaps(false);
                                                }
                                                jsonresult.setScore(1);
                                            }

                                        }
                                    });
                                    Log.i(TAG, resp);
                                }
                            }
                        });
                    }
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                    if (jsonresult.canStop)
                        break;
                }
            }
        };

        thread.start();
    }


    protected Request generateRequest(String url) {
        File file=new File(filePath);

        final int CHUNK_SIZE = 128 * 1024;
        //切割文件为32KB (4s)
        byte[] blockData = FileUtils.getBlock(offset, filePath, CHUNK_SIZE);

        if (blockData == null) {
            throw new RuntimeException(
                    String.format("upload file get blockData faild，filePath:%s , offest:%d",
                            filePath, offset));
        }

        curBolckSize = blockData.length;
        offset = offset + blockData.length;
        // 分块上传，客户端和服务端约定，name字段传文件分块的始偏移量
        final BufferedSink sink=pipeBody.sink;
        try{
            sink.write(blockData);
        }catch (Exception e) {
            Log.i(TAG, e.toString());
        }


        RequestBody body = RequestBody.create(null, blockData);
        Request request = new Request.Builder().url(recordingurl+sessionID)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Transfer-Encoding", "chunked")
                .post(body)
                .build();

        return request;

    }

    protected void writeSink() {
        final WavHeader wavheader = new WavHeader(100000, AudioFormat.CHANNEL_IN_MONO, (byte)16, (long) sampleRate.getSampleRate());
        Thread thread = new Thread("writer") {
            @Override public void run() {
                if (sessionID == null || token == null || pipeBody == null)
                    return;

                BufferedSink sink = pipeBody.sink;
                try {
                    sink.write(wavheader.toBytes());
                }catch (Exception e){
                    Log.e(TAG, e.getLocalizedMessage());

                }

                while (true) {
                    final int CHUNK_SIZE = 128 * 1024;
                    //切割文件为32KB (4s)
                    byte[] blockData = FileUtils.getBlock(offset, filePath, CHUNK_SIZE);
                    if (null == blockData) {
                        break;
                    }

                    curBolckSize = blockData.length;
                    offset = offset + blockData.length;

                    try{
                        sink.write(blockData);
                        Log.i(TAG, String.format("sink wrote %d", offset));
                    }catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    sink.close();
                }catch (Exception e){
                    Log.e(TAG, e.getLocalizedMessage());
                }

            }
        };

        thread.start();

    }


}
