package com.example.jana.gisapp;

import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.esri.android.map.Layer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISLayerInfo;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.popup.Popup;
import com.esri.android.map.popup.PopupContainer;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.popup.PopupInfo;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.ags.query.QueryTask;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends ActionBarActivity implements  AlertDialogRadio.AlertPositiveListener {
    int position = 0;
    public MapView mapView;
    public LocationDisplayManager ls;
    public LocationManager locationManager;
    public LocationListener locationListener;
    public PopupContainer popupContainer;
    private PopupDialog popupDialog;
    private ProgressDialog progressDialog;
    private AtomicInteger count;
    public String mFeatureServiceLukoil;
    public String mFeatureServiceOkta;
    public String mFeatureServiceMakpetrol;
    public String mFeatureServiceOthers;
    public ArcGISFeatureLayer mFeatureLayer1;
    public ArcGISFeatureLayer mFeatureLayer2;
    public ArcGISFeatureLayer mFeatureLayer3;
    public ArcGISFeatureLayer mFeatureLayer4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mapView = (MapView)findViewById(R.id.mapView);
        mapView.centerAt(41.995379, 21.432899, true);
        mapView.setScale(100000);

        mFeatureServiceLukoil = this.getResources().getString(R.string.featureServiceLukoil);
        mFeatureServiceMakpetrol = this.getResources().getString(R.string.featureServiceMakpetrol);
        mFeatureServiceOkta = this.getResources().getString(R.string.featureServiceOkta);
        mFeatureServiceOthers = this.getResources().getString(R.string.featureServiceOthers);

        // Add Feature layer to the MapView
        mFeatureLayer1 = new ArcGISFeatureLayer(mFeatureServiceLukoil, ArcGISFeatureLayer.MODE.ONDEMAND);
        mFeatureLayer2 = new ArcGISFeatureLayer(mFeatureServiceMakpetrol, ArcGISFeatureLayer.MODE.ONDEMAND);
        mFeatureLayer3 = new ArcGISFeatureLayer(mFeatureServiceOkta, ArcGISFeatureLayer.MODE.ONDEMAND);
        mFeatureLayer4 = new ArcGISFeatureLayer(mFeatureServiceOthers, ArcGISFeatureLayer.MODE.ONDEMAND);

        mapView.addLayer(mFeatureLayer1);
        mapView.addLayer(mFeatureLayer2);
        mapView.addLayer(mFeatureLayer3);
        mapView.addLayer(mFeatureLayer4);

        mFeatureLayer1.setVisible(true);
        mFeatureLayer2.setVisible(true);
        mFeatureLayer3.setVisible(true);
        mFeatureLayer4.setVisible(true);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
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
        Button btn = (Button) findViewById(R.id.btn_choose);
        btn.setOnClickListener(listener);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {}
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}
            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(getApplicationContext(), "Please enable GPS", Toast.LENGTH_LONG).show();
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

        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;

            public void onSingleTap(float x, float y) {
                if (mapView.isLoaded()) {
                    // Instantiate a PopupContainer
                    popupContainer = new PopupContainer(mapView);
                    int id = popupContainer.hashCode();
                    popupDialog = null;
                    // Display spinner.
                    if (progressDialog == null || !progressDialog.isShowing())
                        progressDialog = ProgressDialog.show(mapView.getContext(), "", "Querying...");

                    // Loop through each layer in the webmap
                    int tolerance = 20;
                    Envelope env = new Envelope(mapView.toMapPoint(x, y), 20 * mapView.getResolution(), 20 * mapView.getResolution());

                    Layer[] layers = mapView.getLayers();
                    count = new AtomicInteger();
                    for (Layer layer : layers) {
                        // If the layer has not been initialized or is invisible, do nothing.
                        if (!layer.isInitialized() || !layer.isVisible())
                            continue;

                        if (layer instanceof ArcGISFeatureLayer) {
                            // Query feature layer and display popups
                            ArcGISFeatureLayer featureLayer = (ArcGISFeatureLayer) layer;
                            if (featureLayer.getPopupInfo() != null) {
                                // Query feature layer which is associated with a popup definition.
                                count.incrementAndGet();
                                new RunQueryFeatureLayerTask(x, y, tolerance, id).execute(featureLayer);
                            }
                        } else if (layer instanceof ArcGISDynamicMapServiceLayer) {
                            // Query dynamic map service layer and display popups.
                            ArcGISDynamicMapServiceLayer dynamicLayer = (ArcGISDynamicMapServiceLayer) layer;
                            // Retrieve layer info for each sub-layer of the dynamic map service layer.
                            ArcGISLayerInfo[] layerinfos = dynamicLayer.getAllLayers();
                            if (layerinfos == null)
                                continue;

                            // Loop through each sub-layer
                            for (ArcGISLayerInfo layerInfo : layerinfos) {
                                // Obtain PopupInfo for sub-layer.
                                PopupInfo popupInfo = dynamicLayer.getPopupInfo(layerInfo.getId());
                                // Skip sub-layer which is without a popup definition.
                                if (popupInfo == null) {
                                    continue;
                                }
                                // Check if a sub-layer is visible.
                                ArcGISLayerInfo info = layerInfo;
                                while (info != null && info.isVisible()) {
                                    info = info.getParentLayer();
                                }
                                // Skip invisible sub-layer
                                if (info != null && !info.isVisible()) {
                                    continue;
                                }
                                ;

                                // Check if the sub-layer is within the scale range
                                double maxScale = (layerInfo.getMaxScale() != 0) ? layerInfo.getMaxScale() : popupInfo.getMaxScale();
                                double minScale = (layerInfo.getMinScale() != 0) ? layerInfo.getMinScale() : popupInfo.getMinScale();

                                if ((maxScale == 0 || mapView.getScale() > maxScale) && (minScale == 0 || mapView.getScale() < minScale)) {
                                    // Query sub-layer which is associated with a popup definition and is visible and in scale range.
                                    count.incrementAndGet();
                                    new RunQueryDynamicLayerTask(env, layer, layerInfo.getId(), dynamicLayer.getSpatialReference(), id).execute(dynamicLayer.getUrl() + "/" + layerInfo.getId());
                                }
                            }
                        }
                    }
                }
            }
        });}


    private void createPopupViews(Feature[] features, final int id) {
        if ( id != popupContainer.hashCode() ) {
            if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                progressDialog.dismiss();

            return;
        }

        if (popupDialog == null) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            // Create a dialog for the popups and display it.
            popupDialog = new PopupDialog(mapView.getContext(), popupContainer);
            popupDialog.show();
        }
    }

    private class RunQueryFeatureLayerTask extends AsyncTask<ArcGISFeatureLayer, Void, Feature[]> {

        private int tolerance;
        private float x;
        private float y;
        private ArcGISFeatureLayer featureLayer;
        private int id;

        public RunQueryFeatureLayerTask(float x, float y, int tolerance, int id) {
            super();
            this.x = x;
            this.y = y;
            this.tolerance = tolerance;
            this.id = id;
        }

        @Override
        protected Feature[] doInBackground(ArcGISFeatureLayer... params) {
            for (ArcGISFeatureLayer featureLayer : params) {
                this.featureLayer = featureLayer;
                // Retrieve feature ids near the point.
                int[] ids = featureLayer.getGraphicIDs(x, y, tolerance);
                if (ids != null && ids.length > 0) {
                    ArrayList<Feature> features = new ArrayList<Feature>();
                    for (int id : ids) {
                        // Obtain feature based on the id.
                        Feature f = featureLayer.getGraphic(id);
                        if (f == null)
                            continue;
                        features.add(f);
                    }
                    // Return an array of features near the point.
                    return features.toArray(new Feature[0]);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Feature[] features) {
            count.decrementAndGet();
            if (features == null || features.length == 0) {
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }
            // Check if the requested PopupContainer id is the same as the current PopupContainer.
            // Otherwise, abandon the obsoleted query result.
            if ( id != popupContainer.hashCode() ) {
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }

            for (Feature fr : features) {
                Popup popup = featureLayer.createPopup(mapView, 0, fr);
                popupContainer.addPopup(popup);
            }
            createPopupViews(features, id);
        }

    }

    private class RunQueryDynamicLayerTask extends AsyncTask<String, Void, FeatureSet> {
        private Envelope env;
        private SpatialReference sr;
        private int id;
        private Layer layer;
        private int subLayerId;

        public RunQueryDynamicLayerTask(Envelope env, Layer layer, int subLayerId, SpatialReference sr, int id) {
            super();
            this.env = env;
            this.sr = sr;
            this.id = id;
            this.layer = layer;
            this.subLayerId = subLayerId;
        }

        @Override
        protected FeatureSet doInBackground(String... urls) {
            for (String url : urls) {
                // Retrieve features within the envelope.
                Query query = new Query();
                query.setInSpatialReference(sr);
                query.setOutSpatialReference(sr);
                query.setGeometry(env);
                query.setMaxFeatures(10);
                query.setOutFields(new String[] { "*" });

                QueryTask queryTask = new QueryTask(url);
                try {
                    FeatureSet results = queryTask.execute(query);
                    return results;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final FeatureSet result) {
            count.decrementAndGet();
            if (result == null) {
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }

            Feature[] features = result.getGraphics();
            if (features == null || features.length == 0) {
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }
            // Check if the requested PopupContainer id is the same as the current PopupContainer.
            // Otherwise, abandon the obsoleted query result.
            if (id != popupContainer.hashCode()) {
                // Dismiss spinner
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }
            PopupInfo popupInfo = layer.getPopupInfo(subLayerId);
            if (popupInfo == null) {
                // Dismiss spinner
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }

            for (Feature fr : features) {
                Popup popup = layer.createPopup(mapView, subLayerId, fr);
                popupContainer.addPopup(popup);
            }
            createPopupViews(features, id);

        }
    }

    // A customize full screen dialog.
    private class PopupDialog extends Dialog {
        private PopupContainer popupContainer;

        public PopupDialog(Context context, PopupContainer popupContainer) {
            super(context, android.R.style.Theme);
            this.popupContainer = popupContainer;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout layout = new LinearLayout(getContext());
            layout.addView(popupContainer.getPopupContainerView(), android.widget.LinearLayout.LayoutParams.FILL_PARENT, android.widget.LinearLayout.LayoutParams.FILL_PARENT);
            setContentView(layout, params);
        }
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
                layers[1].setVisible(true);
                layers[2].setVisible(true);
                layers[3].setVisible(true);
                layers[4].setVisible(true);
                break;

            case 1:     setTitle("Okta");
                        layers[3].setVisible(true); break;

            case 2:     setTitle("Makpetrol");
                        layers[2].setVisible(true); break;

            case 3:     setTitle("Lukoil");
                        layers[5].setVisible(true); break;

            case 4:     setTitle("Others");
                        layers[1].setVisible(true); break;

            case 5:     setTitle("Nearby");
                        layers[1].setVisible(true);
                        layers[2].setVisible(true);
                        layers[3].setVisible(true);
                        layers[4].setVisible(true);
                        mapView.centerAt(ls.getPoint(), true);
                        mapView.setScale(100000);
                        break;
        }
    }

    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        finish();
    }
}

