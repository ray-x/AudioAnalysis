package cafe.adriel.androidaudiorecorder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.Fragment;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;

public class AndroidAudioPlayer extends AndroidAudioRecorder  {
    public void play() {
        Intent intent = new Intent(activity, AudioRecorderActivity.class);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_COLOR, color);
        intent.putExtra(EXTRA_SOURCE, source);
        intent.putExtra(EXTRA_CHANNEL, channel);
        intent.putExtra(EXTRA_SAMPLE_RATE, sampleRate);
        intent.putExtra(EXTRA_AUTO_START, autoStart);
        intent.putExtra(EXTRA_KEEP_DISPLAY_ON, keepDisplayOn);
        intent.putExtra(EXTRA_API_TOKEN, apiToken);
        intent.putExtra(EXTRA_API_SESSION_ID, sessionID);
        activity.startActivityForResult(intent, requestCode);
    }

    public void playFromFragment() {
        Intent intent = new Intent(fragment.getActivity(), AudioRecorderActivity.class);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_COLOR, color);
        intent.putExtra(EXTRA_SOURCE, source);
        intent.putExtra(EXTRA_CHANNEL, channel);
        intent.putExtra(EXTRA_SAMPLE_RATE, sampleRate);
        intent.putExtra(EXTRA_AUTO_START, autoStart);
        intent.putExtra(EXTRA_KEEP_DISPLAY_ON, keepDisplayOn);
        intent.putExtra(EXTRA_API_TOKEN, apiToken);
        intent.putExtra(EXTRA_API_SESSION_ID, sessionID);

        fragment.startActivityForResult(intent, requestCode);
    }
}
