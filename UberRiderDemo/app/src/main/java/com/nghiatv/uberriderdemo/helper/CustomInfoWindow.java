package com.nghiatv.uberriderdemo.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.nghiatv.uberriderdemo.R;

public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {
    private View myView;

    public CustomInfoWindow(Context context) {
        myView = LayoutInflater.from(context).inflate(R.layout.custom_rider_info_window, null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtPickupInfo = myView.findViewById(R.id.txtPickupInfo);
        txtPickupInfo.setText(marker.getTitle());

        TextView txtPickupSnippet = myView.findViewById(R.id.txtPickupSnippet);
        txtPickupSnippet.setText(marker.getSnippet());

        return myView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
