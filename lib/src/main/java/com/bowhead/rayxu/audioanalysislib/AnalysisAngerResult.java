package com.bowhead.rayxu.audioanalysislib;

/*
{"status":"success","result":{"duration":"125947.13","sessionStatus":"Processing","analysisSegments":
[{"offset":100000,"duration":10000,"end":110000,"analysis":{"Anger/Dislike":{"Label":1,"Score":84}}},
{"offset":105000,"duration":10000,"end":115000,"analysis":{"Anger/Dislike":{"Label":0,"Score":69}}},
{"offset":110000,"duration":10000,"end":120000,"analysis":{"Anger/Dislike":{"Label":0,"Score":65}}},
{"offset":115000,"duration":10000,"end":125000,"analysis":{"Anger/Dislike":{"Label":0,"Score":70}}}]}}

 */

/*
{u'status': u'success', u'recordingId': u'e8d7d534-dff5-4fc6-9fa7-47c6374761bb', u'result':
{u'duration': u'140000.00', u'analysisSegments':
[{u'duration': 10000, u'end': 10000, u'analysis': {u'Anger/Dislike': {u'Score': 77, u'Label': 0}}, u'offset': 0},
 {u'duration': 10000, u'end': 15000, u'analysis': {u'Anger/Dislike': {u'Score': 82, u'Label': 1}}, u'offset': 5000},
 {u'duration': 10000, u'end': 20000, u'analysis': {u'Anger/Dislike': {u'Score': 77, u'Label': 0}}, u'offset': 10000},
 ...
 {u'duration': 10000, u'end': 135000, u'analysis': {u'Anger/Dislike': {u'Score': 87, u'Label': 1}}, u'offset': 125000},
 {u'duration': 10000, u'end': 140000, u'analysis': {u'Anger/Dislike': {u'Score': 58, u'Label': 0}}, u'offset': 130000}],
  u'analysisSummary': {u'AnalysisResult': {u'Anger/Dislike': {u'Mode': 0, u'ModePct': u'74.07'}}}, u'sessionStatus': u'Done'}}
 */

import android.util.Log;
import org.json.JSONObject;

import java.util.ArrayList;

public class AnalysisAngerResult {
    final String TAG = "AnalysisAngerResult";
    float duration;
    String status;
    public AnalysisAngerResult() {
        this.duration = 0;
        this.status = "200";
        this.analysisSegments = new ArrayList<AnalysisSegment>();
    }

    public AnalysisAngerResult(float duration, String status, AnalysisSummary analysisSummary, ArrayList<AnalysisSegment> analysisSegments) {
        this.duration = duration;
        this.status = status;
        this.analysisSummary = analysisSummary;
        this.analysisSegments = analysisSegments;
    }

    public  class AnalysisSegment {
        public float offset;
        public float duration;
        public float end;
        public Score AngerScore;
        public float  getScore() {return AngerScore.Score;}

        public AnalysisSegment(float offset, float duration, float end, Score anger) {
            this.offset = offset;
            this.duration = duration;
            this.end = end;
            this.AngerScore = anger;
        }
    }

    class Score {
        public float Score;
        public float Label;
        public Score (){
            this.Score = 0;
            this.Label = 0;
        }
        public Score (float score, float label){
            this.Score = score;
            this.Label = label;
        }

        public Score(JSONObject score) {
            try {
                this.Score = Float.parseFloat(score.getString("Score"));
                this.Label = Float.parseFloat(score.getString("Label"));
            }catch (Exception e) {
                Log.i(TAG, "field not found for AnalysisResult...");
            }
        }
    }
    //  u'analysisSummary': {u'AnalysisResult': {u'Anger/Dislike': {u'Mode': 0, u'ModePct': u'74.07'}}}, u'sessionStatus': u'Done'}}
    class AnalysisSummary {
        String AngerDislike;
        float Mode;
        float ModePct;
        public AnalysisSummary (String AngerDislike, float Mode, float ModePct){
            this.AngerDislike = AngerDislike;
            this.Mode = Mode;
            this.ModePct = ModePct;
        }
    }
    public AnalysisSummary analysisSummary;
    public ArrayList<AnalysisSegment> analysisSegments;

}
