package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Angelo on 08/10/2016.
 */

public class ImpostazioniFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.impostazioni_fragment, container, false);
        getActivity().setTitle(getResources().getString(R.string.action_settings));
        //rendo INvisibile il FloatingActionButton che è il bottone rotondo in basso a destra che apre il menu di invio posizione
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);




        return rootView;
    }



}
