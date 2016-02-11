package com.example.jana.gisapp;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.portal.BaseMap;

import java.util.Map;

public class ByName extends Activity {

    private LocationManager locationManager;
    private LocationListener locationListener;
    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.by_name);
        MapView mapView = (MapView)findViewById(R.id.mapView);
        mapView.centerAndZoom(42.011052, 21.412204, 10);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Toast.makeText(getApplicationContext(), "Your current location: \n" + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(getApplicationContext(), "Please enable GPS", Toast.LENGTH_LONG).show();
            }
        };

        locationManager.requestLocationUpdates("gps", 5000, 0, locationListener);

    }

//    public void searchClicked(View view) {
//        MapView mapView = (MapView)findViewById(R.id.mapView);
//        Layer layers[] = mapView.getLayers();
//        for(int i = 1; i < layers.length; i++) {
//            layers[i].setVisible(false);
//        }
//        editText = (EditText)findViewById(R.id.editText);
//        String stationName = editText.getText().toString();
//
//        switch(stationName) {
//            case "Okta":        layers[1].setVisible(true); break;
//            case "Makpetrol":   layers[2].setVisible(true); break;
//            case "Lukoil":      layers[3].setVisible(true); break;
//            default:            layers[4].setVisible(true); break;
//        }
//    }


    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    public void onStop() {
        locationManager.removeUpdates(locationListener);
        super.onStop();
        finish();
    }

}
