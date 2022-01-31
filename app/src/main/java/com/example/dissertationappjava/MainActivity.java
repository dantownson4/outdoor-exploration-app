package com.example.dissertationappjava;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.dissertationappjava.databinding.ActivityMainBinding;
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
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.maps.plugin.locationcomponent.*;
import com.mapbox.mapboxsdk.location.LocationComponent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionsListener, OnMapReadyCallback, MapboxMap.OnMapClickListener {

    private ActivityMainBinding binding;

    private MapView mapView;
    private PermissionsManager pManager;
    private MapboxMap map;
    private LocationComponent location;
    private SymbolManager symbolManager;

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
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


    }

    //Suppresses missing permission warning as permissions are handled through Mapbox
    //@SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(Style loadedStyle) {
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

        } else {
            pManager = new PermissionsManager(this);
            pManager.requestLocationPermissions(this);
        }

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

   /* @Override
    protected void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }*/

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

        mapboxMap.setStyle(Style.MAPBOX_STREETS,
                style -> enableLocationComponent(style));

        mapboxMap.addOnMapClickListener(point -> {
            onMapClick(point);
            return false;
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

        //Adds the laoded GeoJSON as a source to the current map style
        loadedMapStyle.addSource(gjsonsource);

        //Creates a Bitmap object using the specified image file in the drawables folder
        //Then adds that bitmap to the style assigning a name
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.red_marker);
        loadedMapStyle.addImage("red_marker", icon);

        //Creates a new SymbolLayer to display the GeoJSON information markers on, assigning the above image file to all points
        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "geojsonsource");
        symbolLayer.setProperties(iconImage("red_marker"));

        //Adds this SymbolLayer to the style to display on top of the map
        loadedMapStyle.addLayer(symbolLayer);


    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        //Creates a list of features at the clicked location on the map using Mapbox Data (from OSM)
        List<Feature> mapFeatures = map.queryRenderedFeatures((map.getProjection().toScreenLocation(point)), "marker-layer");

        final TextView textViewToChange = (TextView) findViewById(R.id.testText);
        textViewToChange.setText("MAP CLICKED");

        if (mapFeatures.isEmpty()){
            textViewToChange.setText("No features - feature list empty");
        }

        for (Feature f : mapFeatures){

            if (f.getStringProperty("name") != null){
                textViewToChange.setText(f.getStringProperty("name"));
                break;
            }else{
                textViewToChange.setText("No features - no type tag");
            }

        }

        return true;
    }
}