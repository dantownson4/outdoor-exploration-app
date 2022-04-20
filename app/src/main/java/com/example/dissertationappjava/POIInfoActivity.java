package com.example.dissertationappjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class POIInfoActivity extends AppCompatActivity {

    DatabaseReference POIDatabaseRef;
    String POIdata;
    String poiID;
    Double userDistance;
    String userID;
    private int POIscore;
    int scoreUpdate;
    boolean alreadyVisited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poiinfo);

        Bundle extras = getIntent().getExtras();

        userDistance = extras.getDouble("userDistance");
        userID = extras.getString("userID");

        POIDatabaseRef = FirebaseDatabase.getInstance("https://dissertation-androidstudio-default-rtdb.europe-west1.firebasedatabase.app/").getReference().child("POIs");

        //Declares objects allowing text in TextView objects to be changed
        final TextView textViewID = (TextView) findViewById(R.id.POIInfo);
        final TextView textViewName = (TextView) findViewById(R.id.POIname);
        final TextView textViewHistoric = (TextView) findViewById(R.id.POIhistorictype);
        final TextView textViewEleData = (TextView) findViewById(R.id.elevationData);
        final TextView textViewEleTitle = (TextView) findViewById(R.id.elevationTitle);
        final TextView textViewScore = (TextView) findViewById(R.id.scoreField);
        final TextView textViewDistance = (TextView) findViewById(R.id.playerDistanceValue);
        final TextView textViewVisited = (TextView) findViewById(R.id.playerVisitedValue);


        //If no POI data was passed in, displays "no data found" message
        //Else sets the input POI JSON data to the string POIdata
        if(extras == null){
            textViewID.setText("NO DATA FOUND");
        }else{
            POIdata = extras.getString("ID");
        }

        //Sets the display of the users distance to POI to the value with only 2 digits after decimal point (not rounded)
        textViewDistance.setText(String.format("%.2f", userDistance) + "m");

        //Creates map object to store the tags and their values
        Map<String, String> information = new HashMap<String, String>();

        try {
            //Calls parseJson function to receive fully parsed POI data
            information = parseJson(POIdata);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("JSON PARSE FAILED in onCreate");
        }

        poiID = information.get("@id");

        //Temporary version of information Map used in ValueEventListener function
        Map<String, String> finalInformation = information;

        //Checks if the current POI ID is already in database
        POIDatabaseRef.child(poiID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //If POI ID already exists, gets the score value stored in the database
                    POIDatabaseRef.child(poiID).child("score").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            POIscore = Integer.parseInt(snapshot.getValue().toString());
                            textViewScore.setText(Integer.toString(POIscore));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }else{
                    //If POI is not in the database, called function to add it and calculate the score
                    addNewPOIToDatabase(finalInformation);
                    textViewScore.setText(Integer.toString(POIscore));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference userDatabaseRef = FirebaseDatabase.getInstance("https://dissertation-androidstudio-default-rtdb.europe-west1.firebasedatabase.app/").getReference().child("users");

        //Checks if the POI ID exists under current user's "visited" list
        //If it does, sets alreadyVisited boolean to True to prevent adding score again
        userDatabaseRef.child(userID).child("visited").child(poiID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    alreadyVisited = true;
                    textViewVisited.setText("Yes");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        //Sets the name and ID TextViews to their corresponding values
        //(Every POI that is valid contains both of these values so no check is needed)
        textViewName.setText(information.get("name"));
        textViewID.setText(information.get("@id"));

        //Checks for other tags, setting their TextViews to the corresponding values
        if(information.containsKey("type")){
            textViewHistoric.setText(information.get("type"));
        }

        if(information.containsKey("ele")){
            textViewEleData.setText(information.get("ele") + "m");
            textViewEleTitle.setText("Height: ");
        }else{
            textViewEleTitle.setText("");
        }


    }

    private Map<String, String> parseJson(String s) throws JSONException {

        JSONObject wholeJson = null;
        JSONObject propertiesJson = null;
        Map<String, String> POIDataMap = new HashMap<String, String>();

        //Attempts to set the input string to a JSONObject, throws exception if not valid Json
        try {
            wholeJson = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("JSON PARSE FAILED in parseJson()");
            return POIDataMap;
        }


        //Gets the properties section of the input POI data Json, containing all required data tags
        if (wholeJson.has("properties")){
            propertiesJson = wholeJson.getJSONObject("properties");
        }else{
            System.out.println("No properties in POI data");
            return POIDataMap;
        }

        //Checks for tags, retrieving their values if exist
        if (propertiesJson.has("@id")){
            POIDataMap.put("@id", propertiesJson.getString("@id"));
        }

        if (propertiesJson.has("name")){
            POIDataMap.put("name", propertiesJson.getString("name"));
        }

        //POI type display uses value from several possible OSM tags, ordered in if else statement by priority
        if(propertiesJson.has("man_made")){
            POIDataMap.put("type", propertiesJson.getString("man_made"));
            POIDataMap.put("typeMain", "man_made");
        }else if (propertiesJson.has("leisure")){
            POIDataMap.put("type", propertiesJson.getString("leisure"));
            POIDataMap.put("typeMain", "leisure");
        }else if (propertiesJson.has("historic")){
            POIDataMap.put("type", propertiesJson.getString("historic"));
            POIDataMap.put("typeMain", "historic");
        }else if (propertiesJson.has("natural")){
            POIDataMap.put("type", propertiesJson.getString("natural"));
            POIDataMap.put("typeMain", "natural");
        }else if (propertiesJson.has("tourism")){
            POIDataMap.put("type", propertiesJson.getString("tourism"));
            POIDataMap.put("typeMain", "tourism");
        }

        if (propertiesJson.has("ele")){
            POIDataMap.put("ele", propertiesJson.getString("ele"));
        }



        return POIDataMap;


    }

    private void addNewPOIToDatabase(Map<String, String> POIInfo){

        //Creates new entry in database with the POI ID and name values
        POIDatabaseRef.child(poiID).child("name").setValue(POIInfo.get("name"));

        //Adds the type value to database if exists
        if (POIInfo.containsKey("type")){
            POIDatabaseRef.child(poiID).child("type").setValue(POIInfo.get("type"));
        }

        //switch statement for assigning a score to the new POI based on POI type
        switch(POIInfo.get("typeMain")){

            case "natural":

                //If the POI has an elevation value, calculates score based on this
                if (POIInfo.containsKey("ele")){
                    int elevation = Integer.parseInt(POIInfo.get("ele"));

                    // Calculates scores for peaks using an exponential function,
                    // with a 100 base score added to 2 to the power of the elevation divided by 125
                    // This produces a final score which increases exponentially depending on the peak elevation
                    // e.g Arthurs seats adds +4, Ben Nevis adds +1734
                    POIscore = (int) (100 + (Math.pow(2, (elevation/125))));
                }else{

                    //If no elevation value, sets score to 75
                    POIscore = 75;
                }

                break;

            case "man-made":
                POIscore = 40;
            case "leisure":
            case "tourism":
                POIscore = 30;
                break;
            case "historic":
                POIscore = 50;
                break;

        }

        //Sets score value in database to the newly calculated score
        POIDatabaseRef.child(poiID).child("score").setValue(Integer.toString(POIscore));


    }


    public void onClose(View v){
        finish();
    }

    //Called when the check in button is pressed
    public void onCheckIn(View v){

        DatabaseReference userDatabaseRef = FirebaseDatabase.getInstance("https://dissertation-androidstudio-default-rtdb.europe-west1.firebasedatabase.app/").getReference().child("users");

        final TextView textViewCheckin = (TextView) findViewById(R.id.checkInStatus);
        final TextView textViewVisited = (TextView) findViewById(R.id.playerVisitedValue);

        //If the user is within the specified range (metres) and has not visited the POI previously
        if (userDistance < 500 && !alreadyVisited){

            scoreUpdate = POIscore;

            //Performs actions on the score value contained in the user's database entry
            userDatabaseRef.child(userID).child("score").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //Calculates user's new score based on their existing score + current POI score value
                    int currentScore = Integer.parseInt(snapshot.getValue().toString());
                    scoreUpdate = scoreUpdate + currentScore;

                    //Updates database entries for user score and visited list
                    userDatabaseRef.child(userID).child("score").setValue(Integer.toString(scoreUpdate));
                    userDatabaseRef.child(userID).child("visited").child(poiID).setValue(true);

                    //Updates UI elements
                    textViewCheckin.setText("Congratulations! You have earned " + POIscore + " points and now have a total of " + scoreUpdate + " points!");
                    textViewVisited.setText("Yes");

                    //Gets the current visited count of user and adds 1
                    userDatabaseRef.child(userID).child("visitedCount").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int currentCount = Integer.parseInt(snapshot.getValue().toString());
                            userDatabaseRef.child(userID).child("visitedCount").setValue(Integer.toString(currentCount+1));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });


            //Adds message to UI based on fail reason
        }else if (alreadyVisited){
            textViewCheckin.setText("Sorry, you have already visited this location.");

        }else{
            textViewCheckin.setText("You are too far away from this location. Please move closer to check-in.");
        }

    }

}