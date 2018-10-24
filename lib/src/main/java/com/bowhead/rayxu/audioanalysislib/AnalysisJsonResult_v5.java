package com.bowhead.rayxu.audioanalysislib;

import android.util.Log;
//import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*

{"status":"success","result":
{"duration":"49168.50","sessionStatus":"Done","analysisSegments":
[{"offset":0,"duration":10000,"end":10000,"analysis":{"Temper":{"Value":91,"Group":"high","Score":87},"Valence":{"Value":9,"Group":"negative","Score":87},"Arousal":{"Value":97,"Group":"high","Score":96},"Emotion_group":{"Group":"Anger/Dislike/Stress","Score":90},"Anger/Dislike":{"Label":1,"Score":87}}},
{"offset":5000,"duration":10000,"end":15000,"analysis":{"Temper":{"Value":90,"Group":"high","Score":85},"Valence":{"Value":10,"Group":"negative","Score":85},"Arousal":{"Value":97,"Group":"high","Score":95},"Emotion_group":{"Group":"Anger/Dislike/Stress","Score":88},"Anger/Dislike":{"Label":1,"Score":85}}}
]
"analysisSummary":
{"AnalysisResult":
{"Temper":{"Mean":"89.50","Mode":"high","ModePct":"87.50"},
"Valence":{"Mean":"15.75","Mode":"negative","ModePct":"87.50"},
"Arousal":{"Mean":"95.13","Mode":"high","ModePct":"100.00"},
"Emotion_group":{"Mode":"Anger/Dislike/Stress","ModePct":"87.50"},
"Anger/Dislike":{"Mode":1,"ModePct":"87.50"}}}},"recordingId":"7c4c19d3-a09f-4109-b4f9-0450fa794b3f"}

{u'status': u'success', u'recordingId': u'82284f81-e871-4813-8143-8d8a16b97d25', u'result': {u'duration': u'140000.00', u'analysisSegments':
{u'duration': 10000, u'end': 10000, u'analysis': {u'Anger/Dislike': {u'Score': 77, u'Label': 0}}, u'offset': 0},
{u'duration': 10000, u'end': 15000, u'analysis': {u'Anger/Dislike': {u'Score': 82, u'Label': 1}}, u'offset': 5000},
{u'duration': 10000, u'end': 20000, u'analysis': {u'Anger/Dislike': {u'Score': 77, u'Label': 0}}, u'offset': 10000},
{u'duration': 10000, u'end': 25000, u'analysis': {u'Anger/Dislike': {u'Score': 75, u'Label': 0}}, u'offset': 15000}],
u'analysisSummary': {u'AnalysisResult': {u'Anger/Dislike': {u'Mode': 0, u'ModePct': u'74.07'}}}, u'sessionStatus': u'Done'}}

{u'status': u'success', u'recordingId': u'578b5ad1-1076-4d0c-bc76-5ffd4083c5de', u'result':
{u'duration': u'16379.13', u'analysisSegments': [
{u'duration': 10000, u'end': 10000, u'analysis': {u'Anger/Dislike': {u'Score': 77, u'Label': 0}}, u'offset': 0},
{u'duration': 10000, u'end': 15000, u'analysis': {u'Anger/Dislike': {u'Score': 82, u'Label': 1}}, u'offset': 5000}],
u'analysisSummary': {u'AnalysisResult': {u'Anger/Dislike': {u'Mode': 0, u'ModePct': u'50.00'}}}, u'sessionStatus': u'Done'}}

 */

public class AnalysisJsonResult_v5 {
    public String httpResult;
    public JSONObject jsonobj;
    final String TAG = "JSON ERROR";

    public AnalysisJsonResult_v5(String s) {
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
    public AnalysisResult_v5 convert() {
        AnalysisResult_v5 analysisResult=new AnalysisResult_v5();
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
            analysisResult.duration = Float.parseFloat(result.getString("duration"));
            analysisResult.status = result.getString("sessionStatus");
            JSONArray segments = jsonobj.getJSONArray("analysisSegments");
            for (int i = 0; i < segments.length(); i++) {
                JSONObject jsonSegment = segments.getJSONObject(i);
                int offset = jsonSegment.getInt("offset");
                int duration = jsonSegment.getInt("duration");
                int end = jsonSegment.getInt("end");

                AnalysisResult_v5.AnalysisSegment analysisSegment = analysisResult.new AnalysisSegment(jsonSegment.getInt("offset"),jsonSegment.getInt("duration"), jsonSegment.getInt("end"));
                JSONObject analysis = jsonSegment.getJSONObject("analysis");
                for (int j = 0; j < TVA.length; j++){
                    JSONObject jo = analysis.getJSONObject(TVA[j]);
                    AnalysisResult_v5.TVAScore score = analysisResult.new TVAScore(jo.getInt("Value"), jo.getString("Group"), jo.getInt("Score"));
                    analysisSegment.TvaScores[j] = score;
                }
                analysisResult.analysisSgements.add(analysisSegment);
            }

        }catch (Exception e) {
            Log.i(TAG, "field not found for analysisSegments...");
        }
        //Result summary


        try {
            JSONObject analysisSummary = result.getJSONObject("analysisSummary").getJSONObject("AnalysisResult");
            JSONObject scoreTJson = analysisSummary.getJSONObject("Temper");
            analysisResult.analysisSummary.temper = analysisResult.new Score(analysisSummary.getJSONObject("Temper"));
            analysisResult.analysisSummary.valence= analysisResult.new Score(analysisSummary.getJSONObject("Valence"));
            analysisResult.analysisSummary.arousal = analysisResult.new Score(analysisSummary.getJSONObject("Arousal"));
            analysisResult.analysisSummary.group = analysisSummary.getJSONObject("Emotion_group").getString("Mode");
            analysisResult.analysisSummary.AngerDislike = analysisSummary.getJSONObject("Anger/Dislike").getInt("Mode");
            analysisResult.analysisSummary.AngerDislikePct = Float.parseFloat(analysisSummary.getJSONObject("Anger/Dislike").getString("ModePct"));

        }catch (Exception e) {
            Log.i(TAG, "field not found for AnalysisResult...");
        }

        return analysisResult;
    }


}

