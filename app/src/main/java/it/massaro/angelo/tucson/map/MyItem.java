package it.massaro.angelo.tucson.map;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;


/**
 * Created by Angelo on 09/10/2016.
 */

public class MyItem implements ClusterItem {
    private final LatLng mPosition;
    private final String mTitle;
    private final String mSnippet;
    private final BitmapDescriptor mIcon;
    private final String mTipoMarker;

    public MyItem(double lat, double lng, String title, String snippet, BitmapDescriptor icon, String tipoMarker) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mSnippet = snippet;
        mIcon = icon;
        mTipoMarker = tipoMarker;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public String getTitle(){
        return mTitle;
    }

    public String getSnippet(){
        return mSnippet;
    }


    public BitmapDescriptor getIcon() {
        return mIcon;
    }

    public String getmTipoMarker() {
        return mTipoMarker;
    }
}
