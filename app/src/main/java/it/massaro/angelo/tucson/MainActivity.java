package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static it.massaro.angelo.tucson.MapViewFragment.MY_PERMISSIONS_REQUEST_LOCATION;
import static it.massaro.angelo.tucson.R.id.start;
import static it.massaro.angelo.tucson.R.menu.main;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private double longitude;
    private double latitude;
    public static final String URL_SERVIZI = "http://russoangela.altervista.org/TucsonREST_11/";
    public static final String MY_PREFS_NAME = "MyPrefsFile";//Salvo le informazioni sul login di facebook
    public static final String MY_PREFS_SETTINGS = "MyPrefsSettings";//Salvo le informazioni del fragment delle impostazioni



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Carico lo SharedPreferences
        final SharedPreferences preferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        final SharedPreferences preferencesImpostazioni = getSharedPreferences(MY_PREFS_SETTINGS, MODE_PRIVATE);

        //Il FloatingActionButton è il bottone rotondo in basso a destra che apre il menu di invio posizione
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                apriFragmentMappa("mappa");

                //Rimane aperto per 15 secondi
                Snackbar.make(view, "", 15000)
                        .setAction(getResources().getString(R.string.send_position), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(isNetworkAvailable() ){
                                    if (getLongitude()!=0.0 && getLatitude()!=0.0) {
                                        new HttpCalls().execute(URL_SERVIZI + preferences.getString("facebookId", ""), "POST", "longitude=" + getLongitude() + "&latitude=" + getLatitude() + "&token=" + preferences.getString("accessToken", "") + "&visualizza_mail=" + preferencesImpostazioni.getBoolean("switch_allow", false) + "&anonimo=" + preferencesImpostazioni.getBoolean("switch_anonimo", false) );
                                    }
                                } else {
                                    Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_internet), Toast.LENGTH_LONG);
                                    toast.show();
                                }
                            }
                        }).show();
            }
        });

        //Il fabDel è il bottone rotondo in basso a sinistra che apre il menu di cancella posizione
        FloatingActionButton fabDel = (FloatingActionButton) findViewById(R.id.fabDel);
        fabDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Rimane aperto per 10 secondi
                Snackbar.make(view, "", 10000)
                        .setAction(getResources().getString(R.string.del_position), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(isNetworkAvailable()){
                                    //TODO servizio di cancellazione
                                    new HttpCalls().execute( URL_SERVIZI + preferences.getString("facebookId","") + "/DELETE", "POST", "token=" + preferences.getString("accessToken","") );
                                } else {
                                    Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_internet), Toast.LENGTH_LONG);
                                    toast.show();
                                }
                            }
                        }).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Parte il fragment con la mappa "MapViewFragment"
        apriFragmentMappa("mappa");
        //

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        //googleMap.setMyLocationEnabled(true);
                        apriFragmentMappa("mappa");
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, getResources().getString(R.string.no_permission), Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            //Parte il fragment con il login di facebook
            apriFragmentFacebook();
        } else if (id == R.id.nav_map) {
            //Parte il fragment con la mappa "MapViewFragment"
            apriFragmentMappa("mappa");
        } else if (id == R.id.nav_all_map_positions) {
            apriFragmentMappa("storico_posizioni");
        } else if (id == R.id.nav_settings) {
            apriFragmentImpostazioni();
        } else if (id == R.id.nav_info) {
            apriFragmentInfo();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public void apriFragmentMappa(String menuClick){
        Fragment fragment = Fragment.instantiate(getApplicationContext(), MapViewFragment.class.getName());
        //passo il parametro menuClick che contiene la stringa relativa al menu dove lutente ha clikkato
        Bundle bundle = new Bundle();
        bundle.putString("menuClick", menuClick);
        fragment.setArguments(bundle);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_main, fragment);
        ft.commit();
    }

    public void apriFragmentFacebook(){
        Fragment fragment = Fragment.instantiate(this, FacebookLogin.class.getName());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_main, fragment);
        ft.commit();
    }

    public void apriFragmentImpostazioni(){
        Fragment fragment = Fragment.instantiate(this, ImpostazioniFragment.class.getName());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_main, fragment);
        ft.commit();
    }

    public void apriFragmentInfo(){
        Fragment fragment = Fragment.instantiate(this, InfoFragment.class.getName());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_main, fragment);
        ft.commit();
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    private class HttpCalls extends AsyncTask<String, Void, String> {

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
            String messaggioRisposta = getResources().getString(R.string.error_send_position);
            //Log.i("json", s);

            JSONObject mainObject = null;
            try {
                mainObject = new JSONObject( s );
                String affectedRows = mainObject.getString("affected_rows");
                if(affectedRows!=null && affectedRows.equals("1")){
                    messaggioRisposta = getResources().getString(R.string.position_send);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast toast = Toast.makeText(getApplicationContext(), messaggioRisposta, Toast.LENGTH_LONG);
            toast.show();

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


    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}
