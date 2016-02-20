package com.example.jana.gisapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
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
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.map.popup.PopupInfo;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.ags.query.QueryTask;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.LangUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity {
    private MapView map;
    private PopupContainer popupContainer;
    private PopupDialog popupDialog;
    private ProgressDialog progressDialog;
    private AtomicInteger count;
    public LocationDisplayManager ls;
    public LocationManager locationManager;
    public LocationListener locationListener;

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        setTitle("FuelUp");

        // Load a webmap.
        map = new MapView(this, "http://jmagdeska.maps.arcgis.com/home/item.html?id=d58cdc0ea56f4a34ac2c1a3dfb15a704", "", "");
        setContentView(map);

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

        map.setOnStatusChangedListener(new OnStatusChangedListener() {
            public void onStatusChanged(Object source, STATUS status) {
                if (source == map && status == STATUS.INITIALIZED) {
                    ls = map.getLocationDisplayManager();
                    ls.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
                    ls.start();

                }
            }
        });
        // Tap on the map and show popups for selected features.
        map.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;

            public void onSingleTap(float x, float y) {
                if (map.isLoaded()) {
                    // Instantiate a PopupContainer
                    popupContainer = new PopupContainer(map);
                    int id = popupContainer.hashCode();
                    popupDialog = null;
                    // Display spinner.
                    if (progressDialog == null || !progressDialog.isShowing())
                        progressDialog = ProgressDialog.show(map.getContext(), "", "Querying...");

                    // Loop through each layer in the webmap
                    int tolerance = 50;
                    Envelope env = new Envelope(map.toMapPoint(x, y), 20 * map.getResolution(), 20 * map.getResolution());
                    Layer[] layers = map.getLayers();
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
                        }
                        else if (layer instanceof ArcGISDynamicMapServiceLayer) {
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
                                if (popupInfo == null)
                                    continue;

                                // Check if a sub-layer is visible.
                                ArcGISLayerInfo info = layerInfo;
                                while ( info != null && info.isVisible() ) {
                                    info = info.getParentLayer();
                                }
                                // Skip invisible sub-layer
                                if ( info != null && ! info.isVisible() ) {
                                    continue;
                                };

                                // Check if the sub-layer is within the scale range
                                double maxScale = (layerInfo.getMaxScale() != 0) ? layerInfo.getMaxScale():popupInfo.getMaxScale();
                                double minScale = (layerInfo.getMinScale() != 0) ? layerInfo.getMinScale():popupInfo.getMinScale();

                                if ((maxScale == 0 || map.getScale() > maxScale) && (minScale == 0 || map.getScale() < minScale)) {
                                    // Query sub-layer which is associated with a popup definition and is visible and in scale range.
                                    count.incrementAndGet();
                                    new RunQueryDynamicLayerTask(env, layer, layerInfo.getId(), dynamicLayer.getSpatialReference(), id).execute(dynamicLayer.getUrl() + "/" + layerInfo.getId());
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Layer layers[] = map.getLayers();

        for(int i = 1; i < layers.length; i++) {
            layers[i].setVisible(false);
        }

        switch(item.getItemId()) {
            case R.id.menuitem1:    setTitle("All");
                layers[1].setVisible(true);
                layers[2].setVisible(true);
                layers[3].setVisible(true);
                layers[4].setVisible(true);
                break;

            case R.id.menuitem2:    setTitle("Okta");
                layers[3].setVisible(true); break;

            case R.id.menuitem3:    setTitle("Makpetrol");
                layers[2].setVisible(true); break;

            case R.id.menuitem4:    setTitle("Lukoil");
                layers[1].setVisible(true); break;

            case R.id.menuitem5:    setTitle("Others");
                layers[4].setVisible(true); break;

            case R.id.menuitem6:    setTitle("Nearby");
                layers[1].setVisible(true);
                layers[2].setVisible(true);
                layers[3].setVisible(true);
                layers[4].setVisible(true);
                map.centerAt(ls.getPoint(), true);
                map.setScale(100000);
                break;
        }
        return true;
    }

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
            popupDialog = new PopupDialog(map.getContext(), popupContainer);
            popupDialog.show();
        }
    }

    // Query feature layer by hit test
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
                Popup popup = featureLayer.createPopup(map, 0, fr);
                popupContainer.addPopup(popup);
            }
            createPopupViews(features, id);
        }

    }

    // Query dynamic map service layer by QueryTask
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
                Popup popup = layer.createPopup(map, subLayerId, fr);
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
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            LinearLayout layout = new LinearLayout(getContext());
            layout.addView(popupContainer.getPopupContainerView(), android.widget.LinearLayout.LayoutParams.FILL_PARENT, android.widget.LinearLayout.LayoutParams.FILL_PARENT);
            setContentView(layout, params);
        }
    }

    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        finish();
    }

}