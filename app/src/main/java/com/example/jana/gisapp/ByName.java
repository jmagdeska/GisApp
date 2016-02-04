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

public class ByName extends Activity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.by_name);

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

    public void searchClicked(View view) {
        EditText stationName = (EditText)findViewById(R.id.stationName);
        Toast.makeText(getApplicationContext(),stationName.getText(),Toast.LENGTH_LONG).show();
    }
    public void backBtnClicked(View view) {
        Intent mainIntent = new Intent(ByName.this,ChooseMap.class);
        ByName.this.startActivity(mainIntent);
        ByName.this.finish();
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    public void onStop() {
        locationManager.removeUpdates(locationListener);
        super.onStop();
        finish();
    }

}
