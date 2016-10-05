package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static it.massaro.angelo.tucson.MainActivity.MY_PREFS_NAME;
import static it.massaro.angelo.tucson.MainActivity.URL_SERVIZI;

/**
 * Created by Angelo on 17/09/2016.
 */
public class MapViewFragment extends Fragment {

    MapView mMapView;
    private GoogleMap googleMap;
    private LatLng myPosition = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.location_fragment, container, false);

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
                Log.d("onMapReady", "MAPPA prontaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
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
                                LatLng nuovaPosizione = new LatLng( location.getLatitude(), location.getLongitude() );
                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(nuovaPosizione));
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
                }

                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
                // Zoom out to zoom level 10, animating with a duration of 2 seconds.
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

                //Carico lo SharedPreferences
                final SharedPreferences preferences = getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
                //AsyncTask<String, Void, String> execute = new HttpCalls().execute(URL_SERVIZI + "?id=" + preferences.getString("facebookId","") + "&token=" + preferences.getString("accessToken", ""), "GET", null);
                HttpCalls httpCalls = new HttpCalls(googleMap, myPosition);
                httpCalls.execute(URL_SERVIZI + "?id=" + preferences.getString("facebookId","") + "&token=" + preferences.getString("accessToken", ""), "GET", null);

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
            //TODO gestire la risposta
            Log.i("json", s);

            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .title("Mia posizione")
                    .snippet("Is this the right location?")
                    .position(myPosition))
                    .setDraggable(true);

            JSONObject mainObject = null;
            try {
                mainObject = new JSONObject( s );
                JSONArray array = mainObject.getJSONArray("data");
                for(int i=0; i<array.length(); i++){

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


            //TODO fare il ciclo for con tutte le posizioni ricavate nel json
            googleMap.addMarker(new MarkerOptions()
                    .title("2222222222")
                    .snippet("22222222Is this the right location?")
                    .position(new LatLng(11.47,-9)))
                    .setDraggable(true);

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

    /**
     * Created by admwks on 27/09/2016.
     */
/*
    public static class Utilita {

        private static final char PARAMETER_DELIMITER = '&';
        private static final char PARAMETER_EQUALS_CHAR = '=';
        public static StringBuilder createQueryStringForParameters(Map<String, String> parameters) {
            StringBuilder parametersAsQueryString = new StringBuilder();
            if (parameters != null) {
                boolean firstParameter = true;

                for (String parameterName : parameters.keySet()) {
                    if (!firstParameter) {
                        parametersAsQueryString.append(PARAMETER_DELIMITER);
                    }

                    try {
                        parametersAsQueryString.append(parameterName)
                                .append(PARAMETER_EQUALS_CHAR)
                                .append( URLEncoder.encode(parameters.get(parameterName),"UTF8") );
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    firstParameter = false;
                }
            }
            return parametersAsQueryString;
        }
    }
    */
}
