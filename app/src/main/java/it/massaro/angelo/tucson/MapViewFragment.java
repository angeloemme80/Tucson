package it.massaro.angelo.tucson;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import it.massaro.angelo.tucson.map.MyItem;
import it.massaro.angelo.tucson.map.OwnIconRendered;

import static android.content.Context.MODE_PRIVATE;
import static com.facebook.FacebookSdk.getApplicationContext;
import static it.massaro.angelo.tucson.MainActivity.MY_PREFS_NAME;
import static it.massaro.angelo.tucson.MainActivity.MY_PREFS_SETTINGS;
import static it.massaro.angelo.tucson.MainActivity.URL_SERVIZI;

/**
 * Created by Angelo on 17/09/2016.
 */
public class MapViewFragment extends Fragment {

    MapView mMapView;
    private GoogleMap googleMap;
    private LatLng myPosition = null;
    private String menuClick = "";
    // Declare a variable for the cluster manager.
    private ClusterManager<MyItem> mClusterManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.location_fragment, container, false);

        //INIZIO Controllo se ha il gps e chiedo all'utente di attivarlo
        PackageManager pm = getActivity().getPackageManager();
        boolean hasGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        LocationManager manager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //Ask the user to enable GPS
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle( getResources().getString(R.string.gps_activation) );
            builder.setMessage( getResources().getString(R.string.enable_gps) );
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Launch settings, allowing user to make a change
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //No location service, no Activity
                    return;
                }
            });
            builder.create().show();
        }
        //FINE Controllo se ha il gps e chiedo all'utente di attivarlo




        //Recupero dal bundle il parametro menuClick per capire quale servizio lanciare, se quello che visualizza le posizioni di tutti o quello che visualizza lo storico delle mie posizioni
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            setMenuClick(bundle.getString("menuClick", "mappa"));
        }
        //Cambio il titolo
        if (getMenuClick().equals("mappa")){
            getActivity().setTitle(getResources().getString(R.string.mappa) + " " + getResources().getString(R.string.app_name));
        }else if (getMenuClick().equals("storico_posizioni")){
            getActivity().setTitle(getResources().getString(R.string.historical_positions));
        }

        //Carico lo SharedPreferences
        final SharedPreferences preferences = getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        //Il FloatingActionButton è il bottone rotondo in basso a destra che apre il menu di invio posizione
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        FloatingActionButton fabDel = (FloatingActionButton) getActivity().findViewById(R.id.fabDel);
        if(preferences.getString("facebookId","").equals("")){
            fab.setVisibility(View.INVISIBLE);
            fabDel.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fabDel.setVisibility(View.VISIBLE);
        }

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately



        mMapView.getMapAsync(new OnMapReadyCallback() {

            private LocationManager locationManager;
            private String provider;

            @Override
            public void onMapReady(GoogleMap mMap) {
                Log.d("onMapReady", "MAPPA pronta");
                googleMap = mMap;

                // Mostra il bottone my location
                //googleMap.setMyLocationEnabled(true);
                googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener(){
                    @Override
                    public boolean onMyLocationButtonClick()
                    {
                        return false;
                    }
                });

                // For dropping a marker at a point on the Map
                /*
                LatLng sydney = new LatLng(-34, 151);
                googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker Title").snippet("Marker Description"));
                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder().target(sydney).zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                */
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                provider = locationManager.getBestProvider(criteria, false);

                if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                //Serve a refreshare la posizione
                locationManager.requestLocationUpdates( provider, 0, 0, new LocationListener() {
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {
                            }
                            @Override
                            public void onProviderEnabled(String provider) {
                            }
                            @Override
                            public void onProviderDisabled(String provider) {
                            }
                            @Override
                            public void onLocationChanged(final Location location) {
                                /*LatLng nuovaPosizione = new LatLng( location.getLatitude(), location.getLongitude() );
                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(nuovaPosizione));
                                */
                            }
                        });
                //Fine refresh posizione



                Location location = locationManager.getLastKnownLocation(provider);
                myPosition = null;
                if (location != null) {
                    //Passo all'activity longitudine e latitudine
                    ((MainActivity)getActivity()).setLatitude( location.getLatitude() );
                    ((MainActivity)getActivity()).setLongitude( location.getLongitude() );
                    //Configuro il marker con la mia posizione attuale
                    myPosition = new LatLng(location.getLatitude(), location.getLongitude());//POSIZIONE CORRENTE
                    //googleMap.addMarker(new MarkerOptions().position(myPosition).title("myPosition"));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
                } else {//se il gps non riesce ad ottenere la posizione apro la mappa su ROMA con zoom sull'Italia
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng( new LatLng(41.900780, 12.483198) ));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(5), 2000, null);
                }

                // Initialize the manager with the context and the map.
                // (Activity extends context, so we can pass 'this' in the constructor.)
                mClusterManager = new ClusterManager<MyItem>(getActivity().getApplicationContext(), googleMap);
                // Point the map's listeners at the listeners implemented by the cluster
                // manager.
                googleMap.setOnCameraIdleListener(mClusterManager);
                googleMap.setOnMarkerClickListener(mClusterManager);


                //googleMap.animateCamera(CameraUpdateFactory.zoomIn());
                // Zoom out to zoom level 10, animating with a duration of 2 seconds.
                //googleMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);

                //Carico lo SharedPreferences
                final SharedPreferences preferences = getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
                final SharedPreferences preferencesImpostazioni = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, MODE_PRIVATE);
                //AsyncTask<String, Void, String> execute = new HttpCalls().execute(URL_SERVIZI + "?id=" + preferences.getString("facebookId","") + "&token=" + preferences.getString("accessToken", ""), "GET", null);
                HttpCalls httpCalls = new HttpCalls(googleMap, myPosition);

                boolean connessione = ((MainActivity)getActivity()).isNetworkAvailable();

                if (getMenuClick().equals("mappa") && connessione){
                    httpCalls.execute(URL_SERVIZI + "?id=" + preferences.getString("facebookId","") + "&token=" + preferences.getString("accessToken", ""), "GET", null);
                } else if (getMenuClick().equals("storico_posizioni") && connessione){
                    httpCalls.execute(URL_SERVIZI + preferences.getString("facebookId","") + "?token=" + preferences.getString("accessToken", "") + "&limite=" + preferencesImpostazioni.getInt("seekBarValue",999), "GET", null);
                } else if (connessione==false){
                    Toast toast = Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.no_internet), Toast.LENGTH_LONG);
                    toast.show();
                }



            }




        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public String getMenuClick() {
        return menuClick;
    }

    public void setMenuClick(String menuClick) {
        this.menuClick = menuClick;
    }


    private class HttpCalls extends AsyncTask<String, Void, String> {

        HttpCalls(GoogleMap map, LatLng position) {
            googleMap = map;
            myPosition = position;
        }

        @Override
        protected String doInBackground(String... params) {

            String sUrl = params[0];
            String method = params[1];
            String urlParameters = params[2];

            return requestUrl(sUrl, method, urlParameters);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("json", s);

            googleMap.clear();
            if(myPosition!=null){
                /*
                googleMap.addMarker(new MarkerOptions()
                        .title( getResources().getString(R.string.my_position) )
                        .snippet( getResources().getString(R.string.current_position) )
                        .position(myPosition))
                        .setDraggable(true);
                */
                MyItem offsetItem = new MyItem(myPosition.latitude,
                        myPosition.longitude,
                        getResources().getString(R.string.my_position),
                        getResources().getString(R.string.current_position),
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mClusterManager.addItem(offsetItem);
                mClusterManager.setRenderer(new OwnIconRendered(getActivity().getApplicationContext(), googleMap, mClusterManager));
            }

            JSONObject mainObject = null;
            try {
                mainObject = new JSONObject( s );
                int statusCode = mainObject.getInt("status_code");
                if(statusCode == 1210){//Se NON è loggato con facebook
                    ((MainActivity)getActivity()).apriFragmentFacebook();
                }

                JSONArray array = mainObject.getJSONArray("data");
                for(int i=0; i<array.length(); i++){
                    JSONObject objectInArray = array.getJSONObject(i);
                    //Log.d("name:", objectInArray.getString("NAME")); Log.d("name:", objectInArray.getString("LONGITUDE"));
                    //Imposto un marker per ogni posizione restituita nel json
                    //Utilita.getReadableDate("2016-10-06 20:54:27");
                    String title = getResources().getString(R.string.sent_on);
                    if(objectInArray.has("NAME")){//Se ha il nome nel json allora è il servizio getPositions quindi metto il nome come titolo
                        title = objectInArray.getString("NAME");
                    }
                    final SharedPreferences preferencesImpostazioni = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, MODE_PRIVATE);
                    if( objectInArray.has("EMAIL") && objectInArray.has("VISUALIZZA_MAIL") && objectInArray.getString("VISUALIZZA_MAIL").equals("1") ){//Se ha EMAIL nel json e l'utente aveva consentito la visualizzazione allora è il servizio getPositions quindi aggiungo EMAIL al titolo
                        title += " - " + objectInArray.getString("EMAIL");
                    }
                    /*
                    googleMap.addMarker(new MarkerOptions()
                            .title( title )
                            .snippet( Utilita.getReadableDate(objectInArray.getString("POSITION_DATE")) )
                            .position(new LatLng( Double.parseDouble(objectInArray.getString("LATITUDE")), Double.parseDouble(objectInArray.getString("LONGITUDE")) ))
                            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.mymarker))
                            )
                            .setDraggable(true)
                            ;
                    */
                    MyItem offsetItem = new MyItem( Double.parseDouble(objectInArray.getString("LATITUDE")),
                            Double.parseDouble(objectInArray.getString("LONGITUDE")),
                            title,
                            Utilita.getReadableDate(objectInArray.getString("POSITION_DATE")),
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    mClusterManager.addItem(offsetItem);
                    mClusterManager.setRenderer(new OwnIconRendered(getActivity().getApplicationContext(), googleMap, mClusterManager));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }




        }


        public String requestUrl(String url, String method, String postParameters)
        {
            if (Log.isLoggable("TAG_Url", Log.INFO)) {
                Log.i("TAG_Url", "Requesting service: " + url);
            }

            HttpURLConnection urlConnection = null;
            try {
                // create connection
                URL urlToRequest = new URL(url);
                urlConnection = (HttpURLConnection) urlToRequest.openConnection();
                //urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                //urlConnection.setReadTimeout(DATARETRIEVAL_TIMEOUT);

                // handle POST parameters
                if (postParameters != null) {

                    if (Log.isLoggable("TAG_Parameters", Log.INFO)) {
                        Log.i("TAG_Parameters", "parameters: " + postParameters);
                    }

                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod(method);
                    urlConnection.setFixedLengthStreamingMode(postParameters.getBytes().length);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    //send the POST out
                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                    out.print(postParameters);
                    out.close();
                }

                // handle issues
                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    // throw some exception
                }

                // read output (only for GET)
                if (urlConnection.getRequestMethod().equalsIgnoreCase("GET") && postParameters != null) {
                    return null;
                } else {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    return getResponseText(in);
                }


            } catch (MalformedURLException e) {
                // handle invalid URL
            } catch (SocketTimeoutException e) {
                // hadle timeout
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }

        private String getResponseText(InputStream inStream) throws IOException {
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try{
                rd = new BufferedReader(new InputStreamReader(inStream));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            }finally {
                if (rd != null) {
                    rd.close();
                }
            }

            return sb.toString();
        }


    }


}
