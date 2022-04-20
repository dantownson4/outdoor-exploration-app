package com.example.dissertationappjava;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.dissertationappjava.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.location.LocationComponent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionsListener, OnMapReadyCallback, MapboxMap.OnMapClickListener, BottomNavigationView.OnNavigationItemSelectedListener {

    public String userID;
    public String userScore;
    public String userVisited;

    private ActivityMainBinding binding;

    private MapView mapView;
    private PermissionsManager pManager;
    private MapboxMap map;
    private LocationComponent location;
    private Feature currentPOI;
    private LatLng currentSelectedPoint;


    //Creates fragments for bottom menu navigation
    FirstFragment homeFrag = new FirstFragment();
    DashboardFragment profileFrag = new DashboardFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, "pk.eyJ1IjoiZGFuaWVsdG93bnNvbjQiLCJhIjoiY2t5cTBkcWFrMDhvbjJ1dGd5OXBkNGtzcyJ9.gyreeDfI-Is7PM3T9uvpug");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        navView.setOnNavigationItemSelectedListener(this);
        navView.setSelectedItemId(R.id.navigation_home);


        Bundle bundle = getIntent().getExtras();

        DatabaseReference db = FirebaseDatabase.getInstance("https://dissertation-androidstudio-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("users");

        if (bundle.getString("userID") != null){

            //Gets user ID passed in from the login activity
            userID = bundle.getString("userID");

            //Single event listener for
            db.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){

                        //Retrieves current user score from database
                        db.child(userID).child("score").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                userScore = snapshot.getValue().toString();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        //Retrieves current user visited count from database
                        db.child(userID).child("visitedCount").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                userVisited = snapshot.getValue().toString();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });



                    }else{
                        db.child(userID).child("score").setValue("0");
                        db.child(userID).child("visitedCount").setValue("0");
                        userScore = "0";
                        userVisited = "0";
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


            userID = bundle.getString("userID");
        }else{
            System.out.println("USERID NOT FOUND");
        }


    }


    private void enableLocationComponent(Style loadedStyle) {

        //If location permissions have not been granted, requests permissions from user
        if (!PermissionsManager.areLocationPermissionsGranted(this)){
            //Creates Mapbox PermissionsManager to handle permissions request, then requests location permissions from user
            pManager = new PermissionsManager(this);
            pManager.requestLocationPermissions(this);
        }

        //Checks if location permissions have been granted by the user
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            location = map.getLocationComponent();

            location.activateLocationComponent(LocationComponentActivationOptions.builder(this, loadedStyle).build());


            //Auto generated permissions check
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            location.setLocationComponentEnabled(true);

            location.setCameraMode(CameraMode.TRACKING);
            location.setRenderMode(RenderMode.COMPASS);

        }

        //Calls GeoJSONLoad method using specified geojson file
        //This file can be changed for different POIs, locations etc.
        GeoJSONLoad(loadedStyle, "asset://POIdatapoints.geojson");

    }


    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onExplanationNeeded(List<String> list) {

    }

    @Override
    public void onPermissionResult(boolean b) {

        if(b){
            map.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(Style style) {
                    enableLocationComponent(style);

                }
            });
        }
    }


    @Override
    public void onMapReady(MapboxMap mapboxMap) {

        this.map = mapboxMap;

        //Sets the map style (from included Mapbox default styles)
        //Then gets callback from enableLocationComponent finalising the map loading, using lambda function
        mapboxMap.setStyle(Style.SATELLITE_STREETS,
                style -> enableLocationComponent(style));
        //Adds click listener for the map, calling the onMapClick function as a result of a click
        mapboxMap.addOnMapClickListener(point -> {
            onMapClick(point);
            return false;
        });

        DatabaseReference db = FirebaseDatabase.getInstance("https://dissertation-androidstudio-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("users").child(userID).child("score");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                TextView textViewToChangeScore = (TextView) findViewById(R.id.userscoreValue);
                textViewToChangeScore.setText(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void GeoJSONLoad(Style loadedMapStyle, String assetID) {

        GeoJsonSource gjsonsource = null;

        //Attempts to retrieve GeoJSON data from the specified location storing it in a GeoJsonSource object
        try {
            gjsonsource = new GeoJsonSource("geojsonsource", new URI(assetID));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        //Adds the loaded GeoJSON as a source to the current map style
        loadedMapStyle.addSource(gjsonsource);

        //Creates a Bitmap object using the specified image file in the drawables folder
        //Then adds that bitmap to the style assigning a name
        Bitmap markerIcon = BitmapFactory.decodeResource(getResources(), R.drawable.red_marker);
        loadedMapStyle.addImage("red_marker", markerIcon);

        //Creates a new SymbolLayer to display the GeoJSON information markers on, assigning the above image file to all points
        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "geojsonsource");
        symbolLayer.setProperties(iconImage("red_marker"));

        //Adds this SymbolLayer to the style to display on top of the map
        loadedMapStyle.addLayer(symbolLayer);


    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        currentSelectedPoint = point;

        //Creates a list of features at the clicked location on the map using Mapbox Data (from OSM)
        List<Feature> mapFeatures = map.queryRenderedFeatures((map.getProjection().toScreenLocation(point)), "marker-layer");

        final TextView textViewToChange = (TextView)findViewById(R.id.poiname);

        textViewToChange.setText("TESTING");

        //If user clicks on location without a POI
        if (mapFeatures.isEmpty()){
            textViewToChange.setText("No features - feature list empty");
            currentPOI = null;
        }


        for (Feature f : mapFeatures){

            //If the current Feature object contains a "name" tag, displays the value and sets currentPOI to this Feature
            if (f.getStringProperty("name") != null){
                textViewToChange.setText(f.getStringProperty("name"));
                currentPOI = f;
                break;
            }else{
                textViewToChange.setText("No features - no type tag");
            }

        }

        return true;
    }

    public void onInfoClick(View v){

        final TextView textViewToChange = (TextView) findViewById(R.id.poiname);


        if (currentPOI != null){

            textViewToChange.setText(" ");
            Bundle bundle = new Bundle();

            LatLng userLocation = new LatLng(location.getLastKnownLocation().getLatitude(), location.getLastKnownLocation().getLongitude());

            //Adds the currentPOI Feature object to the bundle to be passed to the info activity, in JSON format
            bundle.putString("ID", currentPOI.toJson());
            bundle.putString("userID", userID);
            bundle.putDouble("userDistance", currentSelectedPoint.distanceTo(userLocation));

            //Creates and starts an Intent containing the above POI JSON, without closing the main map
            Intent i = new Intent(MainActivity.this, POIInfoActivity.class);
            i.putExtras(bundle);
            startActivity(i);


        }else{
            textViewToChange.setText("No feature selected");
        }


    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        //Loads corresponding fragment when user clicks on a navigation button
        switch (item.getItemId()) {
            case R.id.navigation_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.container, homeFrag).commit();
                return true;

            case R.id.navigation_profile:
                getSupportFragmentManager().beginTransaction().replace(R.id.container, profileFrag).commit();
                return true;

        }
        return false;
    }
}