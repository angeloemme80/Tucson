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
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

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
import java.util.Iterator;

import it.massaro.angelo.tucson.map.MyItem;
import it.massaro.angelo.tucson.map.OwnIconRendered;

import static android.content.Context.MODE_PRIVATE;
import static com.facebook.FacebookSdk.getApplicationContext;
import static com.google.android.gms.wearable.DataMap.TAG;
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
    MyItem miaPosizioneItem = null;
    Marker markerMiaPosizione = null;
    int contatoreDistanzaMinoreDi = 0;
    LocationListener mListener = null;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //  TODO: Prompt with explanation!

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.location_fragment, container, false);

        final SharedPreferences preferencesImpostazioni = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, MODE_PRIVATE);
        final SharedPreferences.Editor editor = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, getActivity().MODE_PRIVATE).edit();

        //INIZIO Controllo se ha un gps o un dispositivo di rete che puo dare la posizione, in caso negativo, lo mando sulla view di info mandandogli un messaggio
        PackageManager pm = getActivity().getPackageManager();
        boolean hasGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION);
        if (hasGps == false) {
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.no_gps), Toast.LENGTH_LONG);
            toast.show();
            ((MainActivity) getActivity()).apriFragmentInfo();
            return null;
        }
        //FINE Controllo se ha un gps o un dispositivo di rete che puo dare la posizione, in caso negativo, lo mando sulla view di info mandandogli un messaggio

        //Controllo se esiste una connessione ad internet e lo avviso solamente
        boolean connessione = ((MainActivity) getActivity()).isNetworkAvailable();
        if (connessione == false) {
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.no_internet), Toast.LENGTH_LONG);
            toast.show();
        }

        //Controllo la versione di android che se è dalla 6 in poi deve concedere i permessi di localizzazione
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //INIZIO Controllo se ha il gps attivo e in caso negativo chiedo all'utente di attivarlo
        LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if ((!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)  )
                && preferencesImpostazioni.getBoolean("activate_gps", true)) {
            //Ask the user to enable GPS
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(R.string.gps_activation));
            builder.setMessage(getResources().getString(R.string.enable_gps));
            builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Launch settings, allowing user to make a change
                    if (isAdded()) {
                        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(i);
                    }
                }
            });
            builder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //No location service, no Activity
                    editor.putBoolean("activate_gps", false);
                    editor.commit();
                    return;
                }
            });
            builder.create().show();
        }
        //FINE Controllo se ha il gps attivo e in caso negativo chiedo all'utente di attivarlo


        //Recupero dal bundle il parametro menuClick per capire quale servizio lanciare, se quello che visualizza le posizioni di tutti o quello che visualizza lo storico delle mie posizioni
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            setMenuClick(bundle.getString("menuClick", "mappa"));
        }
        //Cambio il titolo
        if (getMenuClick().equals("mappa")) {
            getActivity().setTitle(getResources().getString(R.string.mappa) + " " + getResources().getString(R.string.app_name));
        } else if (getMenuClick().equals("storico_posizioni")) {
            getActivity().setTitle(getResources().getString(R.string.historical_positions));
        }

        //Carico lo SharedPreferences
        final SharedPreferences preferences = getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        //Il FloatingActionButton è il bottone rotondo in basso a destra che apre il menu di invio posizione
        final FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        final FloatingActionButton fabDel = (FloatingActionButton) getActivity().findViewById(R.id.fabDel);
        /*
        if (preferences.getString("facebookId", "").equals("")) {
            fab.setVisibility(View.INVISIBLE);
            fabDel.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
            //TODO abilitare appena la funzione di cancellazione è complate e funzionante fabDel.setVisibility(View.VISIBLE);
        }*/

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
            Location mLocation;
            private String provider;

            @Override
            public void onMapReady(GoogleMap mMap) {
                Log.d("onMapReady", "MAPPA pronta");
                googleMap = mMap;



                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //User has previously accepted this permission
                    if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    //Not in api-23, no need to prompt
                    googleMap.setMyLocationEnabled(true);
                }


                //googleMap.setMyLocationEnabled(true);
                googleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        if(mLocation!=null) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLocation.getLatitude(), mLocation.getLongitude())));
                        }
                        return false;
                    }
                });


                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_LOW); // Chose your desired power consumption level.
                criteria.setAccuracy(Criteria.ACCURACY_FINE); // Choose your accuracy requirement.
                criteria.setSpeedRequired(true); // Chose if speed for first location fix is required.
                criteria.setAltitudeRequired(false); // Choose if you use altitude.
                criteria.setBearingRequired(false); // Choose if you use bearing.
                criteria.setCostAllowed(false); // Choose if this provider can waste money :-)
                provider = locationManager.getBestProvider(criteria, true);
                //Toast.makeText(getActivity().getApplicationContext(), "BESTPROVIDER: " + provider, Toast.LENGTH_SHORT).show();


                if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                //Serve a refreshare la posizione
                //locationManager.requestLocationUpdates( provider, 0, 0,
                mListener = new LocationListener() {
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        //if (getActivity() != null) {
                        //    Toast.makeText(getActivity().getApplicationContext(), "onStatusChanged: " + provider + " - STATUS:" + status, Toast.LENGTH_SHORT).show();
                        //}
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        //if (getActivity() != null) {
                        //    Toast.makeText(getActivity().getApplicationContext(), "onProviderEnabled: " + provider, Toast.LENGTH_SHORT).show();
                        //}
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        //if (getActivity() != null) {
                            //Toast.makeText(getActivity().getApplicationContext(), "onProviderDisabled: " + provider, Toast.LENGTH_SHORT).show();
                        //}
                    }

                    @Override
                    public void onLocationChanged(final Location location) {

                        //TODO Mentre il listener "ascolta" nuove posizioni, quando ne trova una nuova ne calcolo la distanza dall'ultima,
                        // se supera i 5 metri faccio ripartire il listener. Stoppo il listener se ha trovato la posizione almeno 5 volte
                        if (getActivity() != null && location != null && mLocation != null) {
                            float distanzaTra2Punti = location.distanceTo(mLocation);
                            if (distanzaTra2Punti < 5.0) {//Aggiorno il contatore, se per 5 volte la distanza è minore di 5 metri allora stoppo il listener, altrimenti faccio ripartire il contatore
                                contatoreDistanzaMinoreDi += 1;
                            } else {
                                contatoreDistanzaMinoreDi = 0;
                            }
                            if (contatoreDistanzaMinoreDi >= 5 && locationManager != null) {
                                if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                locationManager.removeUpdates(mListener);
                            }

                            //Toast toast = Toast.makeText(getActivity().getApplicationContext(), " DISTANZA: " + distanzaTra2Punti, Toast.LENGTH_SHORT);
                            //toast.show();
                        }

                        mLocation = location;


                        //Verifica la posizione migliore: DA TESTARE
                        //boolean migliore = isBetterLocation(location, mLocation);
                        if(getActivity()!=null) {

                            //Toast toast = Toast.makeText(getActivity().getApplicationContext(), " onLocationChanged: " + location.getProvider() + " - long:" + location.getLongitude() + " - lat:" + location.getLatitude(), Toast.LENGTH_LONG);
                            //toast.show();

                            ((MainActivity)getActivity()).setLatitude( mLocation.getLatitude() );
                            ((MainActivity)getActivity()).setLongitude( mLocation.getLongitude() );

                            //Visualizzo il pulsante per inviarla
                            if (preferences.getString("facebookId", "").equals("")) {
                                fab.setVisibility(View.INVISIBLE);
                                fabDel.setVisibility(View.INVISIBLE);
                            } else {
                                fab.setVisibility(View.VISIBLE);
                                //TODO abilitare appena la funzione di cancellazione è complate e funzionante fabDel.setVisibility(View.VISIBLE);
                            }

                            if(markerMiaPosizione!=null) {
                                markerMiaPosizione.remove();
                            }
                            markerMiaPosizione = googleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()))
                                    .title(getResources().getString(R.string.my_position))
                                    .snippet(getResources().getString(R.string.current_position))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

                            //googleMap.moveCamera(CameraUpdateFactory.newLatLng( new LatLng(mLocation.getLatitude(),mLocation.getLongitude())));


                            /*
                            if (mClusterManager != null) {
                                if (miaPosizioneItem != null) {
                                    mClusterManager.removeItem(miaPosizioneItem);
                                }
                                miaPosizioneItem = new MyItem(mLocation.getLatitude(),
                                        mLocation.getLongitude(),
                                        getResources().getString(R.string.my_position),
                                        getResources().getString(R.string.current_position),
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                                mClusterManager.addItem(miaPosizioneItem);
                                mClusterManager.setRenderer(new OwnIconRendered(getActivity().getApplicationContext(), googleMap, mClusterManager));
                            }
                            */
                        }

                    }
                };
                //);



                //Lancio il listener
                locationManager.requestLocationUpdates(provider, 0, 0, mListener);




                // Initialize the manager with the context and the map.
                // (Activity extends context, so we can pass 'this' in the constructor.)
                mClusterManager = new ClusterManager<MyItem>(getActivity().getApplicationContext(), googleMap);


                mLocation = locationManager.getLastKnownLocation(provider);
                myPosition = null;
                if (mLocation != null) {
                    //Passo all'activity longitudine e latitudine
                    ((MainActivity)getActivity()).setLatitude( mLocation.getLatitude() );
                    ((MainActivity)getActivity()).setLongitude( mLocation.getLongitude() );

                    //Visualizzo il pulsante per inviarla
                    if (preferences.getString("facebookId", "").equals("")) {
                        fab.setVisibility(View.INVISIBLE);
                        fabDel.setVisibility(View.INVISIBLE);
                    } else {
                        if(getActivity()!=null) {
                            //Toast toast = Toast.makeText(getActivity().getApplicationContext(), " POSIZIONE DISPONIBILE", Toast.LENGTH_SHORT);
                            //toast.show();
                        }
                        fab.setVisibility(View.VISIBLE);
                        //TODO abilitare appena la funzione di cancellazione è complate e funzionante fabDel.setVisibility(View.VISIBLE);
                    }

                    //Configuro il marker con la mia posizione attuale
                    myPosition = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());//POSIZIONE CORRENTE

                    if(markerMiaPosizione!=null) {
                        markerMiaPosizione.remove();
                    }
                    markerMiaPosizione = googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()))
                            .title(getResources().getString(R.string.my_position))
                            .snippet(getResources().getString(R.string.current_position))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

/*
                    if(mClusterManager!=null) {
                        if(miaPosizioneItem!=null) {
                            mClusterManager.removeItem(miaPosizioneItem);
                        }
                        miaPosizioneItem = new MyItem(
                                myPosition.latitude,
                                myPosition.longitude,
                                getResources().getString(R.string.my_position),
                                getResources().getString(R.string.current_position),
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                        mClusterManager.addItem(miaPosizioneItem);
                        mClusterManager.setRenderer(new OwnIconRendered(getActivity().getApplicationContext(), googleMap, mClusterManager));
                    }
*/

                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
                } else {//se il gps non riesce ad ottenere la posizione apro la mappa su ROMA con zoom sull'Italia
                    if(getActivity()!=null) {
                        //Toast toast = Toast.makeText(getActivity().getApplicationContext(), " POSIZIONE VUOTA", Toast.LENGTH_SHORT);
                        //toast.show();
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng( new LatLng(41.900780, 12.483198) ));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(5), 2000, null);
                    fab.setVisibility(View.INVISIBLE);
                    fabDel.setVisibility(View.INVISIBLE);
                }


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

                if (getMenuClick().equals("mappa") && connessione) {
                    httpCalls.execute(URL_SERVIZI + "?id=" + preferences.getString("facebookId", "") + "&token=" + preferences.getString("accessToken", ""), "GET", null);
                } else if (getMenuClick().equals("storico_posizioni") && connessione) {
                    httpCalls.execute(URL_SERVIZI + preferences.getString("facebookId", "") + "?token=" + preferences.getString("accessToken", "") + "&limite=" + preferencesImpostazioni.getInt("seekBarValue", 999), "GET", null);
                } else if (connessione == false) {
                    Toast toast = Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.no_internet), Toast.LENGTH_LONG);
                    toast.show();
                }

                //INIZIO Gestione del click sull'Infowindows del marker
                googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(final Marker marker) {
                        if(marker.getTag()!=null && ((String)marker.getTag()).equalsIgnoreCase("STORICO") ){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(getResources().getString(R.string.delete_position_title));
                            builder.setMessage(getResources().getString(R.string.delete_position));
                            builder.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Toast.makeText(getActivity().getApplicationContext(), marker.getSnippet(), Toast.LENGTH_LONG).show();
                                    //Nello snippet c'è la data e ora della posizione da cancellare
                                    HttpCalls httpCalls = new HttpCalls(googleMap, myPosition);
                                    httpCalls.execute(URL_SERVIZI + preferences.getString("facebookId", "") + "/DELETE", "POST", "positionDate=" + Utilita.getTimestampDate(marker.getSnippet()) + "&token=" + preferences.getString("accessToken", "") + "&limite=" + preferencesImpostazioni.getInt("seekBarValue", 999) );
                                }
                            });
                            builder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    return;
                                }
                            });
                            builder.create().show();
                        }
                    }
                });
                //FINE Gestione del click sull'Infowindows del marker

            }




        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Toast toast = Toast.makeText(getActivity().getApplicationContext(), "onResume", Toast.LENGTH_SHORT);
        //toast.show();
        if(mMapView!=null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //Toast toast = Toast.makeText(getActivity().getApplicationContext(), "onPause", Toast.LENGTH_SHORT);
        //toast.show();
        if(mMapView!=null) {
            mMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast toast = Toast.makeText(getActivity().getApplicationContext(), "onDestroy", Toast.LENGTH_SHORT);
        //toast.show();
        if(mMapView!=null) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //Toast toast = Toast.makeText(getActivity().getApplicationContext(), "onLowMemory", Toast.LENGTH_SHORT);
        //toast.show();
        if(mMapView!=null) {
            mMapView.onLowMemory();
        }
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

            mClusterManager.clearItems();
            JSONObject mainObject = null;
            try {
                mainObject = new JSONObject( s );
                int statusCode = mainObject.getInt("status_code");
                if(statusCode == 1210){//Se NON è loggato con facebook
                    ((MainActivity)getActivity()).apriFragmentFacebook();
                }

                if(mainObject.has("data")==false){
                    return;
                }
                JSONArray array = mainObject.getJSONArray("data");
                for(int i=0; i<array.length(); i++){
                    JSONObject objectInArray = array.getJSONObject(i);
                    //Log.d("name:", objectInArray.getString("NAME")); Log.d("name:", objectInArray.getString("LONGITUDE"));
                    //Imposto un marker per ogni posizione restituita nel json

                    String title = getResources().getString(R.string.sent_on);
                    String tipoMarker = "STORICO";//Tipo marker è impostato a "STORICO" oppure a "MAPPA" a seconda se si tratta di un marker della funzione storico o mappa, mi serve per la funzione di cancellazione che puo avvenire solo sullo storico
                    if(objectInArray.has("NAME")){//Se ha il nome nel json allora è il servizio getPositions quindi metto il nome come titolo
                        title = objectInArray.getString("NAME");
                        tipoMarker = "MAPPA";
                    }
                    final SharedPreferences preferencesImpostazioni = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, MODE_PRIVATE);
                    if( objectInArray.has("EMAIL") && objectInArray.has("VISUALIZZA_MAIL") && objectInArray.getString("VISUALIZZA_MAIL").equals("1") ){//Se ha EMAIL nel json e l'utente aveva consentito la visualizzazione allora è il servizio getPositions quindi aggiungo EMAIL al titolo
                        title += " - " + objectInArray.getString("EMAIL");
                    }

                    MyItem offsetItem = new MyItem( Double.parseDouble(objectInArray.getString("LATITUDE")),
                            Double.parseDouble(objectInArray.getString("LONGITUDE")),
                            title,
                            Utilita.getReadableDate(objectInArray.getString("POSITION_DATE")),
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            tipoMarker);

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
