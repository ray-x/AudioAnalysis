package com.bowhead.rayxu.audioanalysislib;
import android.util.Log;

import com.bowhead.rayxu.audioanalysislib.AnalysisAngerResult;
import com.bowhead.rayxu.audioanalysislib.AnalysisJsonAngerResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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


public class AnalysisJsonAngerResult {
    public String httpResult;
    public JSONObject jsonobj;
    final String TAG = "JSON ERROR";

    public AnalysisJsonAngerResult(String s) {
        httpResult = s;
        try {
            jsonobj = new JSONObject(s);
        }catch (JSONException e) {
            Log.i("JSON",httpResult);
        }
    }
    public final String[] TVA =  {"Temper", "Valence", "Arousal"};

    public JSONObject getJsonObj() {
        return jsonobj;
    }
    public AnalysisAngerResult convert() {
        AnalysisAngerResult analysisResult=new AnalysisAngerResult();
        if (! (jsonobj.has("status") && jsonobj.has("result"))){
            Log.i(TAG, "field not found for result");
            return analysisResult;
        }
        JSONObject result;
        try {
            result = jsonobj.getJSONObject("result");
        }catch (Exception e) {
            Log.i(TAG, "field not found for result...");
            return analysisResult;
        }

        try {
            //{u'duration': u'140000.00', u'analysisSegments':
            analysisResult.duration = Float.parseFloat(result.getString("duration"));
            JSONArray segments = result.getJSONArray("analysisSegments");
            //{u'duration': 10000, u'end': 15000, u'analysis': {u'Anger/Dislike': {u'Score': 82, u'Label': 1}}, u'offset': 5000},
            for (int i = 0; i < segments.length(); i++) {
                JSONObject jsonSegment = segments.getJSONObject(i);
                JSONObject analysisJson = jsonSegment.getJSONObject("analysis").getJSONObject("Anger/Dislike");
                AnalysisAngerResult.Score score = analysisResult.new Score(jsonSegment.getJSONObject("analysis").getJSONObject("Anger/Dislike"));
                AnalysisAngerResult.AnalysisSegment analysisSegment = analysisResult.new AnalysisSegment(Float.parseFloat(jsonSegment.getString("offset")),
                        jsonSegment.getInt("duration"),
                        jsonSegment.getInt("end"),
                        score);
                analysisResult.analysisSegments.add(analysisSegment);

            }

        }catch (Exception e) {
            Log.i(TAG, "field not found for analysisSegments...");
        }
        //Result summary

        //"status":"success","result":{"duration":"140000.00","sessionStatus":"Done","analysisSummary":{"AnalysisResult":{"Anger/Dislike":{"Mode":0,"ModePct":"74.07"}}}}}
        if (result.has("analysisSummary"))
        {
            try {
                JSONObject analysisSummary = result.getJSONObject("analysisSummary").getJSONObject("AnalysisResult");
                JSONObject scoreTJson = analysisSummary.getJSONObject("Anger/Dislike");
                analysisResult.analysisSummary= analysisResult.new AnalysisSummary("Anger/Dislike", scoreTJson.getInt("Mode"),
                        Float.parseFloat(scoreTJson.getString("ModePct")));


            }catch (Exception e) {
                Log.i(TAG, "field not found for AnalysisResult...");
            }
        }


        return analysisResult;
    }



}
