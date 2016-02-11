package com.example.jana.gisapp;

/**
 * Created by computer on 11.2.2016.
 */
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;

import java.util.HashMap;
import java.util.Map;

/** Since this class attaches the dialog fragment "AlertDialogRadio",
 *  it is suppose to implement the interface "AlertPositiveListener"
 */
public class MainActivity extends Activity implements  AlertDialogRadio.AlertPositiveListener {
    /** Stores the selected item's position */
    int position = 0;
    private LocationManager locationManager;
    private LocationListener locationListener;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final MapView mapView = (MapView)findViewById(R.id.mapView);

        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                /** Getting the fragment manager */
                FragmentManager manager = getFragmentManager();

                /** Instantiating the DialogFragment class */
                AlertDialogRadio alert = new AlertDialogRadio();

                /** Creating a bundle object to store the selected item's index */
                Bundle b  = new Bundle();

                /** Storing the selected item's index in the bundle object */
                b.putInt("position", position);

                /** Setting the bundle object to the dialog fragment object */
                alert.setArguments(b);

                /** Creating the dialog fragment object, which will in turn open the alert dialog window */
                alert.show(manager, "alert_dialog_radio");
            }
        };

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                Point mapPt = mapView.toMapPoint((float)location.getLatitude(), (float)location.getLongitude());
//
//                Map<String,Object> attributes = new HashMap<String,Object>();
//                attributes.put("Type", "Location");
//                attributes.put("Description", "Curent Location");
//
//                Graphic g = new Graphic(location.getLatitude(), location.getLongitude(), new SimpleMarkerSymbol(Color.RED, 10, SimpleMarkerSymbol.STYLE.CIRCLE), attributes, 0);
//                Toast.makeText(getApplicationContext(), "Your current location: \n" + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_LONG).show();
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



        /** Getting the reference of the button from the main layout */
        Button btn = (Button) findViewById(R.id.btn_choose);

        /** Setting a button click listener for the choose button */
        btn.setOnClickListener(listener);
    }

    /** Defining button click listener for the OK button of the alert dialog window */
    @Override
    public void onPositiveClick(int position) {
        this.position = position;
        MapView mapView = (MapView)findViewById(R.id.mapView);
        Layer layers[] = mapView.getLayers();

        for(int i = 1; i < layers.length; i++) {
            layers[i].setVisible(false);
        }

        switch(this.position) {
            case 0:
            case 5:     layers[1].setVisible(true);
                        layers[2].setVisible(true);
                        layers[3].setVisible(true);
                        layers[4].setVisible(true);
                        break;
            case 1:     layers[1].setVisible(true); break;
            case 2:     layers[2].setVisible(true); break;
            case 3:     layers[3].setVisible(true); break;
            case 4:     layers[4].setVisible(true); break;
        }
    }
}
