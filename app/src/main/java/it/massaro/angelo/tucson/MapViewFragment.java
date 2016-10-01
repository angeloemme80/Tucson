package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Angelo on 17/09/2016.
 */
public class MapViewFragment extends Fragment {

    MapView mMapView;
    private GoogleMap googleMap;


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
                LatLng myPosition = null;
                LatLng positionCasale = null;
                if (location != null) {
                    //Passo all'activity longitudine e latitudine
                    ((MainActivity)getActivity()).setLatitude( location.getLatitude() );
                    ((MainActivity)getActivity()).setLongitude( location.getLongitude() );
                    //Configuro il marker con la mia posizione attuale
                    myPosition = new LatLng(location.getLatitude(), location.getLongitude());//POSIZIONE CORRENTE
                    positionCasale = new LatLng(41.00905027182723, 14.123783111572276);//CASAL DI PRINCIPE

                    googleMap.addMarker(new MarkerOptions().position(myPosition).title("myPosition"));
                    googleMap.addMarker(new MarkerOptions().position(positionCasale).title("positionCasale"));

                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
                }



                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
                // Zoom out to zoom level 10, animating with a duration of 2 seconds.
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

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
