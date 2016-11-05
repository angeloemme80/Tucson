package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by Angelo on 05/11/2016.
 */

public class TucsonItaliaFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tucson_italia_fragment, container, false);

        getActivity().setTitle(getResources().getString(R.string.hyundai_tucson_italia));
        //rendo INvisibile il FloatingActionButton che è il bottone rotondo in basso a destra che apre il menu di invio posizione
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        FloatingActionButton fabDel = (FloatingActionButton) getActivity().findViewById(R.id.fabDel);
        fabDel.setVisibility(View.INVISIBLE);

        final Button buttonJoin = (Button) rootView.findViewById(R.id.button_join);
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
                String facebookUrl = getFacebookPageURL(getActivity());
                facebookIntent.setData(Uri.parse(facebookUrl));
                startActivity(facebookIntent);
            }
        });

        return rootView;
    }

    public static String FACEBOOK_URL = "https://www.facebook.com/groups/hyundai.tucson.club.italia";
    public static String FACEBOOK_PAGE_ID = "Hyundai Tucson Italia";

    //method to get the right URL to use in the intent
    public String getFacebookPageURL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            int versionCode = packageManager.getPackageInfo("com.facebook.katana", 0).versionCode;
            if (versionCode >= 3002850) { //newer versions of fb app
                return "fb://facewebmodal/f?href=" + FACEBOOK_URL;
            } else { //older versions of fb app
                return "fb://page/" + FACEBOOK_PAGE_ID;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return FACEBOOK_URL; //normal web url
        }
    }

}
