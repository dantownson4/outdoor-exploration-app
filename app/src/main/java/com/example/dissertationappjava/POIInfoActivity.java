package com.example.dissertationappjava;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class POIInfoActivity extends AppCompatActivity {

    String POIdata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poiinfo);

        Bundle extras = getIntent().getExtras();

        final TextView textViewID = (TextView) findViewById(R.id.POIInfo);
        final TextView textViewName = (TextView) findViewById(R.id.POIname);
        final TextView textViewHistoric = (TextView) findViewById(R.id.POIhistorictype);

        if(extras == null){
            textViewID.setText("NO DATA FOUND");
        }else{
            POIdata = extras.getString("ID");
        }

        Map<String, String> information = new HashMap<String, String>();
        try {
            information = parseJson(POIdata);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("JSON PARSE FAILED in onCreate");
        }

        textViewName.setText(information.get("name"));
        textViewID.setText(information.get("@id"));

        if(information.containsKey("historic")){
            textViewHistoric.setText(information.get("historic"));
        }


    }

    private Map<String, String> parseJson(String s) throws JSONException {

        JSONObject wholeJson = null;
        JSONObject propertiesJson = null;
        Map<String, String> data = new HashMap<String, String>();

        try {
            wholeJson = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("JSON PARSE FAILED in parseJson()");
            return data;
        }



        if (wholeJson.has("properties")){
            propertiesJson = wholeJson.getJSONObject("properties");
        }else{
            System.out.println("No properties in POI data");
            return data;
        }

        if (propertiesJson.has("@id")){
            data.put("@id", propertiesJson.getString("@id"));
        }

        if (propertiesJson.has("name")){
            data.put("name", propertiesJson.getString("name"));
        }

        if (propertiesJson.has("historic")){
            data.put("historic", propertiesJson.getString("historic"));
        }



        return data;


    }

    public void onClose(View v){
        finish();

    }

}