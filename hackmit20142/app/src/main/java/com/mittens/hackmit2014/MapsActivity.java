package com.mittens.hackmit2014;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ArrayList<LatLng> bathrooms = new ArrayList<LatLng>();
    private ArrayList<String> names = new ArrayList<String>();
    GPSTracker gps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bathrooms.add(new LatLng(42.358383, -71.096413));
        bathrooms.add( new LatLng(42.358482, -71.096110));
        bathrooms.add(new LatLng(42.361617, -71.090856));
        bathrooms.add(new LatLng(42.359185, -71.093136));
        names.add("Johnson Ice Rink");
        names.add("Johnson Bathroom");
        names.add("Ray and Maria Stata Center");
        names.add("MIT School of Architecture");
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        mMap.setOnInfoWindowClickListener(this);
        mMap.setMyLocationEnabled(true);
        float[] results;
        gps = new GPSTracker(this);
        //MIT... nigga.
        for (int i = 0; i < bathrooms.size(); i++) {
            results = new float[1];
            Location.distanceBetween(gps.latitude, gps.longitude,
                    bathrooms.get(i).latitude, bathrooms.get(i).longitude, results);
            mMap.addMarker(new MarkerOptions().position(bathrooms.get(i)).title(names.get(i)).snippet("Distance: " + results[0] + "/n(Click for path)"));

        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bathrooms.get(0), 15));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        gps = new GPSTracker(this);
        float[] results = new float[1];
        String url = makeURL(gps.getLatitude(), gps.getLongitude(), marker.getPosition().latitude, marker.getPosition().longitude);
        long timeSecs = System.currentTimeMillis() * 1000;
        Location.distanceBetween(gps.latitude, gps.longitude,
                marker.getPosition().latitude, marker.getPosition().longitude, results);


        new connectAsyncTask(url).execute();
    }

    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        String urlString = "";
        urlString += "http://maps.googleapis.com/maps/api/directions/json";
        urlString += "?origin=";// from
        urlString += Double.toString(sourcelat);
        urlString += ",";
        urlString += (Double.toString( sourcelog));
        urlString += ("&destination=");// to
        urlString += (Double.toString( destlat));
        urlString += (",");
        urlString += (Double.toString( destlog));
        urlString += ("&sensor=false&mode=walking&alternatives=true");
        return urlString.toString();
    }

    private class connectAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        String url;
        connectAsyncTask(String urlPass){
            url = urlPass;
        }
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(MapsActivity.this);
            progressDialog.setMessage("Fetching route, Please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.hide();
            if(result!=null){
                drawPath(result);
            }
        }
    }

    public void drawPath(String  result) {
        Log.d("got to drawPath", "");
        try {
            Log.d("inside draw", "");
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);
                Polyline line = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                        .width(9)
                        .color(Color.RED).geodesic(true));
            }

        }
        catch (JSONException e) {

        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }
}
