package it.massaro.angelo.tucson.map;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Created by Angelo on 09/10/2016.
 */

public class OwnIconRendered extends DefaultClusterRenderer<MyItem> {

    public OwnIconRendered(Context context, GoogleMap map, ClusterManager<MyItem> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(MyItem item, MarkerOptions markerOptions) {
        markerOptions.icon(item.getIcon());
        markerOptions.snippet(item.getSnippet());
        markerOptions.title(item.getTitle());
        super.onBeforeClusterItemRendered(item, markerOptions);
    }

    @Override
    protected void onClusterItemRendered(MyItem item, Marker marker) {
        marker.setTag(item.getmTipoMarker());//Imposto nella proprietà TAG del marker il tipo per capire se si tratta di storico oppure no
        super.onClusterItemRendered(item, marker);
    }




}
