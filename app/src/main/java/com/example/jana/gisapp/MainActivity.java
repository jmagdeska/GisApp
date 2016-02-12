package com.example.jana.gisapp;

/**
 * Created by computer on 11.2.2016.
 */
import android.app.ActionBar;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.esri.android.map.Layer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;

public class MainActivity extends ActionBarActivity implements  AlertDialogRadio.AlertPositiveListener {
    int position = 0;
    public MapView mapView;
    public LocationDisplayManager ls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mapView = (MapView)findViewById(R.id.mapView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        setTitle("FuelUp");

        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                AlertDialogRadio alert = new AlertDialogRadio();
                Bundle b  = new Bundle();
                b.putInt("position", position);
                alert.setArguments(b);
                alert.show(manager, "alert_dialog_radio");
            }
        };

        mapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mapView && status == STATUS.INITIALIZED) {
                    ls = mapView.getLocationDisplayManager();
                    ls.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
                    ls.start();

                }
            }
        });

        Button btn = (Button) findViewById(R.id.btn_choose);
        btn.setOnClickListener(listener);
    }

    @Override
    public void onPositiveClick(int position) {
        this.position = position;
        Layer layers[] = mapView.getLayers();

        for(int i = 1; i < layers.length; i++) {
            layers[i].setVisible(false);
        }

        switch(this.position) {
            case 0:     setTitle("All");
                        layers[2].setVisible(true);
                        layers[3].setVisible(true);
                layers[4].setVisible(true);
                        break;
            case 1:     setTitle("Okta");
                layers[1].setVisible(true); break;
            case 2:     setTitle("Makpetrol");
                layers[2].setVisible(true); break;
            case 3:     setTitle("Lukoil");
                layers[3].setVisible(true); break;
            case 4:     setTitle("Others");
                layers[4].setVisible(true); break;
            case 5:     setTitle("Nearby");
                        layers[2].setVisible(true);
                        layers[3].setVisible(true);
                        layers[4].setVisible(true);
                        mapView.centerAt(ls.getPoint(), true);
                        mapView.setScale(100000);layers[1].setVisible(true);
                        break;
        }
    }
}
