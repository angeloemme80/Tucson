package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;

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
import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;
import static com.facebook.FacebookSdk.getApplicationContext;
import static it.massaro.angelo.tucson.MainActivity.MY_PREFS_NAME;
import static it.massaro.angelo.tucson.MainActivity.URL_SERVIZI;

/**
 * Created by Angelo on 18/09/2016.
 */
public class FacebookLogin extends Fragment {


    CallbackManager callbackManager;
    //public static final String MY_PREFS_NAME = "MyPrefsFile";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        String keyHash = FacebookSdk.getApplicationSignature(getActivity().getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //faccio il logout da facebook prima di mostrare il bottone login, perchè potrebbe esserci qualche connessione facebook aperta precedentemente
        //LoginManager.getInstance().logOut();
        View rootView = inflater.inflate(R.layout.facebook_login, container, false);
        getActivity().setTitle(getResources().getString(R.string.login));
        //rendo INvisibile il FloatingActionButton che è il bottone rotondo in basso a destra che apre il menu di invio posizione
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);

        LoginButton loginButton = (LoginButton) rootView.findViewById(R.id.login_button);

        loginButton.setReadPermissions(Arrays.asList("email", "public_profile","user_friends"));

        // If using in a fragment
        loginButton.setFragment(this);
        // Other app specific specialization

        //Intercetta il LOGOUT di facebook, resetto lo shared preferences
        AccessTokenTracker fbTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken accessToken, AccessToken accessToken2) {
                if (accessToken2 == null) {
                    Log.d("FB", "User Logged Out.");
                    if(getActivity()!=null){
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                        editor.clear().commit();
                    }
                }
            }
        };
        //Fine Intercetta il LOGOUT di facebook

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            private ProfileTracker mProfileTracker;

            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("ONSUCCESS FACEBOOK", loginResult.toString());
                String accessToken = loginResult.getAccessToken().getToken();

                //Salvo l'accesstoken nello SharedPreferences
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(MY_PREFS_NAME, getActivity().MODE_PRIVATE).edit();
                editor.putString("accessToken", accessToken);
                editor.putString("facebookId", loginResult.getAccessToken().getUserId());
                editor.commit();

                //rendo visibile il FloatingActionButton che è il bottone rotondo in basso a destra che apre il menu di invio posizione
                FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
                fab.setVisibility(View.VISIBLE);

                Bundle bundle = new Bundle();
                bundle.putString("fields", "email, id, name, first_name, last_name, age_range, link, gender, locale, picture, timezone, updated_time, verified");

                new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "/me",
                        bundle,
                        HttpMethod.GET,
                        new GraphRequest.Callback() {
                            public void onCompleted(GraphResponse response) {
                                //Prendo i valori che mi ritornano da facebook
                                JSONObject me = response.getJSONObject();
                                String id = me.optString("id");
                                String email = me.optString("email");
                                String name = me.optString("name");
                                String picture = me.optString("picture");
                                String link = me.optString("link");

                                //chiamo il servizio per la verifica del login di facebook e relativo invio della posizione
                                new HttpCalls().execute( URL_SERVIZI + id, "POST", "longitude=" + ((MainActivity)getActivity()).getLongitude() + "&latitude=" + ((MainActivity)getActivity()).getLatitude() + "&token=" + AccessToken.getCurrentAccessToken().getToken() );
                                /*
                                progressDialog = ProgressDialog.show(FacebookLoginFragment.this.getContext(), "", getResources().getString(R.string.progress_dialog_message));
                                */


                            }
                        }
                ).executeAsync();



            }

            @Override
            public void onCancel() {
                Log.d("ONCANCEL FACEBOOK", "ONCANCEL FACEBOOK");
            }
            @Override
            public void onError(FacebookException exception) {
                Log.d("ONERROR FACEBOOK", exception.getMessage());
            }



        });


        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
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
            //TODO gestire la risposta
            Log.i("json", s);
            ((MainActivity)getActivity()).apriFragmentMappa("mappa");
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
