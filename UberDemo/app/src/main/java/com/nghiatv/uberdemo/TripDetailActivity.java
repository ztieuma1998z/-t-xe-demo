package com.nghiatv.uberdemo;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nghiatv.uberdemo.common.Common;

import java.util.Calendar;

public class TripDetailActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "TripDetailActivity";

    private GoogleMap mMap;
    private TextView txtDate, txtFee, txtBaseFare, txtTime, txtDistance, txtEstimatedPayout, txtFrom, txtTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initializeComponents();
    }

    private void initializeComponents() {
        txtDate = findViewById(R.id.txtDate);
        txtFee = findViewById(R.id.txtFee);
        txtBaseFare = findViewById(R.id.txtBaseFare);
        txtTime = findViewById(R.id.txtTime);
        txtDistance = findViewById(R.id.txtDistance);
        txtEstimatedPayout = findViewById(R.id.txtEstimatedPayout);
        txtFrom = findViewById(R.id.txtFrom);
        txtTo = findViewById(R.id.txtTo);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        settingInformation();
    }

    private void settingInformation() {
        if (getIntent() != null) {
            Calendar calendar = Calendar.getInstance();
            String date = String.format("%s, %d/%d", convertToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
                    , calendar.get(Calendar.DAY_OF_MONTH)
                    , calendar.get(Calendar.DAY_OF_MONTH));
            txtDate.setText(date);
            txtFee.setText(String.format("$ %.2f", getIntent().getDoubleExtra("total", 0.0)));
            txtEstimatedPayout.setText(String.format("$ %.2f", getIntent().getDoubleExtra("total", 0.0)));
            txtBaseFare.setText(String.format("$ %.2f", Common.base_fare));
            txtTime.setText(String.format("%s min", getIntent().getStringExtra("time")));
            txtDistance.setText(String.format("%s km", getIntent().getStringExtra("distance")));
            txtFrom.setText(getIntent().getStringExtra("start_address"));
            txtTo.setText(getIntent().getStringExtra("end_address"));

            String[] locationEnd = getIntent().getStringExtra("location_end").split(",");
            LatLng dropOff = new LatLng(Double.parseDouble(locationEnd[0]), Double.parseDouble(locationEnd[1]));

            mMap.addMarker(new MarkerOptions().position(dropOff)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropOff, 12.0f));
        }
    }

    private String convertToDayOfWeek(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return "Thứ hai";

            case Calendar.TUESDAY:
                return "Thứ ba";

            case Calendar.WEDNESDAY:
                return "Thứ tư";

            case Calendar.THURSDAY:
                return "Thứ năm";

            case Calendar.FRIDAY:
                return "Thứ sáu";

            case Calendar.SATURDAY:
                return "Thứ bảy";

            case Calendar.SUNDAY:
                return "Chủ nhật";

            default:
                return "UNK";
        }
    }
}
