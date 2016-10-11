package it.massaro.angelo.tucson;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import static android.content.Context.MODE_PRIVATE;
import static it.massaro.angelo.tucson.MainActivity.MY_PREFS_SETTINGS;

/**
 * Created by Angelo on 08/10/2016.
 */

public class ImpostazioniFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.impostazioni_fragment, container, false);
        getActivity().setTitle(getResources().getString(R.string.action_settings));
        //rendo INvisibile il FloatingActionButton che è il bottone rotondo in basso a destra che apre il menu di invio posizione
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);

        SharedPreferences preferencesImpostazioni = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, MODE_PRIVATE);

        //Font personalizzato
        //Typeface typeFace = Typeface.createFromAsset(getActivity().getAssets(),"Dosis-Bold.ttf");
        //TextView tvMaxPositions = (TextView) rootView.findViewById(R.id.tvMaxPositions);
        //tvMaxPositions.setTypeface(typeFace);

        //Switch
        Switch switch_allow = (Switch) rootView.findViewById(R.id.switch_allow);
        switch_allow.setChecked( preferencesImpostazioni.getBoolean("switch_allow",false) );
        switch_allow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, getActivity().MODE_PRIVATE).edit();
                editor.putBoolean("switch_allow", ((Switch) v).isChecked() );
                editor.commit();
            }
        });

        //SeekBar
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.seekBar);
        final TextView tvSize = (TextView) rootView.findViewById(R.id.tvSize);
        seekBar.setProgress( preferencesImpostazioni.getInt("seekBarValue",999) );
        //tvSize.setTypeface(typeFace);
        tvSize.setText(String.valueOf(seekBar.getProgress()));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSize.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("onStartTrackingTouch", "onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Salvo l'accesstoken nello SharedPreferences
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(MY_PREFS_SETTINGS, getActivity().MODE_PRIVATE).edit();
                editor.putInt("seekBarValue", seekBar.getProgress() );
                editor.commit();
            }

        });



        return rootView;
    }



}
