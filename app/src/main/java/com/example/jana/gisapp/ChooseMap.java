package com.example.jana.gisapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Jana on 2/3/2016.
 */
public class ChooseMap extends Activity {

    Button btnName;
    Button btnNearest;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_map);

        btnName = (Button)findViewById(R.id.btnName);
        btnNearest = (Button)findViewById(R.id.btnNearest);
    }

    public void byName(View view) {
        Intent mainIntent = new Intent(ChooseMap.this,ByName.class);
        ChooseMap.this.startActivity(mainIntent);
        ChooseMap.this.finish();
    }

    public void byNearest(View view) {
        Intent mainIntent = new Intent(ChooseMap.this,ByNearest.class);
        ChooseMap.this.startActivity(mainIntent);
        ChooseMap.this.finish();
    }
}
