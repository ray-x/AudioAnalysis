package com.bowhead.rayxu.audioanalysislib;

import android.util.Log;
import org.json.JSONObject;

import java.util.ArrayList;

/*

{"status":"success","result":
{"duration":"49168.50","sessionStatus":"Done","analysisSegments":
[{"offset":0,"duration":10000,"end":10000,"analysis":{"Temper":{"Value":91,"Group":"high","Score":87},"Valence":{"Value":9,"Group":"negative","Score":87},"Arousal":{"Value":97,"Group":"high","Score":96},"Emotion_group":{"Group":"Anger/Dislike/Stress","Score":90},"Anger/Dislike":{"Label":1,"Score":87}}},
{"offset":5000,"duration":10000,"end":15000,"analysis":{"Temper":{"Value":90,"Group":"high","Score":85},"Valence":{"Value":10,"Group":"negative","Score":85},"Arousal":{"Value":97,"Group":"high","Score":95},"Emotion_group":{"Group":"Anger/Dislike/Stress","Score":88},"Anger/Dislike":{"Label":1,"Score":85}}}
]
"analysisSummary":{"AnalysisResult":{"Temper":{"Mean":"89.50","Mode":"high","ModePct":"87.50"},
"Valence":{"Mean":"15.75","Mode":"negative","ModePct":"87.50"},
"Arousal":{"Mean":"95.13","Mode":"high","ModePct":"100.00"},
"Emotion_group":{"Mode":"Anger/Dislike/Stress","ModePct":"87.50"},
"Anger/Dislike":{"Mode":1,"ModePct":"87.50"}}}},"recordingId":"7c4c19d3-a09f-4109-b4f9-0450fa794b3f"}
 */

/* Temper reflects a speaker’s temperament or emotional state ranging from gloomy or depressive at the low-end, embracive and friendly in the mid-range, and confrontational or aggressive at the high-end of the scale.
High temper occur when the speaker experiences and expresses aggressive emotions, such as active resistance, anger, hatred, hostility, aggressiveness, forceful commandment and/or arrogance.
Medium temper occur when the speaker experience and expresses the following three types of emotions:
Low temper occur when the speaker experiences and expresses depressive emotions in an inhibited fashion, such as sadness, pain, suffering, insult, inferiority, self-blame, self-criticism, regret, fear, anxiety and concern (can also be interpreted as fatigued). It is as though the speaker is waning, growing smaller or pulling back.
 */

/*
Valence is an output which measures speaker’s level of negativity / positivity.
Negative Valence. The speaker’s voice conveys emotional pain and weakness or aggressive and antagonistic emotions.
Neutral Valence. The speaker’s voice conveys no preference and comes across as self-control or neutral.
Positive Valence. The speaker’s voice conveys affection, love, acceptance and openness.
 */

/*
Arousal is an output that measures a speaker’s degree of energy ranging from tranquil, bored or sleepy to excited and highly energetic. Arousal can also correspond to similar concepts such as involvement and stimulation.
The Arousal output is divided into two distinct measurements:

Continuous Scale ranging from 0 to 100, representing a shift from tranquil at the lower part of the scale to excited at the higher part of the same scale.
Arousal groups which consist of three distinct groups: Low, Mid and High.
There are three possible and distinct Arousal groups:

Low Arousal, conveys low levels of alertness and can be registered in cases of sadness, comfort, relief or sleepiness.
Mid Arousal, conveys a medium level of alertness and can be registered in cases of normal conduct, indifference or self-control.
High Arousal, conveys a high level of alertness such as excitement, surprise, passionate communication, extreme happiness or anger.

 */

public class AnalysisResult_v5 {
    final String TAG = "JSON ERROR";
    float duration;
    String status;
    class Score {
        float mean;
        String mode;
        float modepct;
        public Score (float mean, String mode, float modepct){
            this.mean = mean;
            this.mode = mode;
            this.modepct = modepct;
        }
        public Score(JSONObject score) {
            try {
                this.mean = Float.parseFloat(score.getString("Mean"));
                this.mode = score.getString("Mode");
                this.modepct = Float.parseFloat(score.getString("ModePct"));
            }catch (Exception e) {
                Log.i(TAG, "field not found for AnalysisResult...");
            }

        }
    }

    public class TVAScore {
        int value;
        String Group;
        int scrore;

        public TVAScore(int value, String group, int scrore) {
            this.value = value;
            Group = group;
            this.scrore = scrore;
        }
    }
    public  class AnalysisSegment {
        int offset;
        int duration;
        int end;

        public TVAScore[] TvaScores;

        public AnalysisSegment(int offset, int duration, int end) {
            this.offset = offset;
            this.duration = duration;
            this.end = end;
            this.TvaScores = new TVAScore[3];
        }
    }
    class AnalysisSummary {
        Score temper;
        Score valence;
        Score arousal;
        String group;
        float modePct;
        float angerPct;
        int AngerDislike;
        float AngerDislikePct;

        public AnalysisSummary (Score Temper, Score Valence, Score Arousal, String group, float ModePct, float AngerPct){
            this.temper = Temper;
            this.valence = Valence;
            this.arousal = Arousal;
            this.group = group;
            this.modePct = ModePct;
            this.angerPct = AngerPct;
        }
    }
    ArrayList<AnalysisSegment> analysisSgements;
    AnalysisSummary analysisSummary;



    void AddDetail(AnalysisSegment chunk){
        analysisSgements.add(chunk);
    }

    public AnalysisResult_v5(){
        this.duration = 0;
        status = "";
    }
//
//    public AnalysisResult (String s) {
//        httpResult = s;
//        try {
//            jsonobj = new JSONObject(s);
//
//        }catch (JSONException e) {
//            Log.i("JSON",httpResult);
//        }
//    }
//
//    public JSONObject getJsonObj() {
//        return jsonobj;
//    }


}
