package com.spacescout.spacescout_android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

import static com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm.MAX_DISTANCE_AT_ZOOM;
/**
 *
 * Created by ajay alfred on 11/5/13.
 */
public class SpaceMapActivity extends Fragment{

    int dist = MAX_DISTANCE_AT_ZOOM;

    public ClusterManager<MyItem> mClusterManager;
    public void setUpClusterer() {

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(this.getActivity(), map);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        map.setOnCameraChangeListener(mClusterManager);
        map.setOnMarkerClickListener(mClusterManager);

    }

    public SpaceMapActivity() {
        ///empty constructor required for fragment subclasses
    }

    static final LatLng UnivWashington = new LatLng(47.655263166697765, -122.30669233862307);
    private GoogleMap map;
    private MapFragment mapFragment;
    private FragmentManager fm;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle bundle) {
        super.onCreate(bundle);
        if(view == null)
            view = inflater.inflate(R.layout.fragment_space_map, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMap();
    }


    private void setUpMap() {
        if (map != null)
            return;
        map = mapFragment.getMap();
        if(map == null)
            return;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        fm = getFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        if(mapFragment == null)
        {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        map = mapFragment.getMap();

//        map.setMyLocationEnabled(true);
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);


        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(UnivWashington, 17.2f));

        new JSONParse().execute();

    }

    public class JSONParse extends AsyncTask<String, String, JSONArray> {
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected JSONArray doInBackground(String... args){
            JSONParser jParser = new JSONParser();
            JSONArray json = new JSONArray();
            System.out.println("trying");
            // Getting JSON from URL
             json = jParser.getJSONFromUrl("http://skor.cac.washington.edu:9001/api/v1/spot/all");

            return json;
        }
        @Override
        protected void onPostExecute(JSONArray json) {
            HashSet<String> buildings = new HashSet<String>();

            try {

                System.out.println("test3");
                setUpClusterer();
                mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<MyItem>(new ClusterByBuilding<MyItem>()));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                for(int i = 0; i < json.length(); i++){
                    JSONObject curr = json.getJSONObject(i);
                    JSONObject info = curr.getJSONObject("extended_info");

                    JSONObject location = curr.getJSONObject("location");
                    double lng = Double.parseDouble(location.getString("longitude"));
                    double lat = Double.parseDouble(location.getString("latitude"));
                    String name = curr.getString("name");
                    String building_name = location.getString("building_name");
                    String campus = info.getString("campus");

                    if(campus.equals("seattle") && (!buildings.contains(building_name))){
                        buildings.add(building_name);
                        mClusterManager.addItem(new MyItem(lat, lng, building_name));
                        LatLng currLoc = new LatLng(lat, lng);
                        builder.include(currLoc);
//                        map.addMarker(new MarkerOptions()
//                                .position(currLoc)
//                                .title(name)
//                                .snippet("Students: 1234")
//                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                    }
                }
                System.out.println("Zoom Level: "+map.getMaxZoomLevel());
                System.out.println("Zoom Level: "+map.getCameraPosition().zoom);
               LatLngBounds bounds = builder.build();
               map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 70));

                System.out.println("test4");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
}
