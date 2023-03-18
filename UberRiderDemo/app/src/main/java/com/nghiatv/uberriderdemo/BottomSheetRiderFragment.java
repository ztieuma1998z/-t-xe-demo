package com.nghiatv.uberriderdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nghiatv.uberriderdemo.common.Common;
import com.nghiatv.uberriderdemo.remote.IGoogleApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BottomSheetRiderFragment extends BottomSheetDialogFragment {
    private static final String TAG = "BottomSheetRiderFragmen";

    private String mLocation, mDestination;
    private IGoogleApi mService;
    private TextView txtLocation, txtDestination, txtCalculate;

    private boolean isTapOnMap;

    public static BottomSheetRiderFragment newInstance(String location, String destination, boolean isTapOnMap) {
        BottomSheetRiderFragment f = new BottomSheetRiderFragment();
        Bundle args = new Bundle();
        args.putString("location", location);
        args.putString("destination", destination);
        args.putBoolean("isTapOnMap", isTapOnMap);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = getArguments().getString("location");
        mDestination = getArguments().getString("destination");
        isTapOnMap = getArguments().getBoolean("isTapOnMap");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_rider, container, false);

        txtLocation = view.findViewById(R.id.txtLocation);
        txtDestination = view.findViewById(R.id.txtDestination);
        txtCalculate = view.findViewById(R.id.txtCalculate);

        mService = Common.getGoogleApi();
        getPrice(mLocation, mDestination);

        if (!isTapOnMap) {
            txtLocation.setText(mLocation);
            txtDestination.setText(mDestination);
        }

        return view;
    }

    private void getPrice(String mLocation, String mDestination) {
        String requestUrl = null;
        try {
            requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "mode=driving&"
                    + "transit_routing_preference=less_driving&"
                    + "origin=" + mLocation + "&"
                    + "destination=" + mDestination + "&"
                    + "key=" + getResources().getString(R.string.google_direction_api);

            Log.d(TAG, requestUrl);

            mService.getPath(requestUrl)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray routes = jsonObject.getJSONArray("routes");

                                JSONObject object = routes.getJSONObject(0);
                                JSONArray legs = object.getJSONArray("legs");

                                JSONObject legsObject = legs.getJSONObject(0);

                                // Get distance
                                JSONObject distance = legsObject.getJSONObject("distance");
                                String distanceText = distance.getString("text");
                                Double distanceValue = Double.parseDouble(distanceText.replaceAll("[^0-9\\\\.]+", ""));

                                // Get time
                                JSONObject time = legsObject.getJSONObject("duration");
                                String timeText = distance.getString("text");
                                Integer timeValue = Integer.parseInt(timeText.replaceAll("\\D+", ""));

                                String finalCalculate = String.format("%s + %s = $%.2f", distanceText, timeText, Common.getPrice(distanceValue, timeValue));

                                txtCalculate.setText(finalCalculate);

                                if (isTapOnMap) {
                                    String startAddress = legsObject.getString("start_address");
                                    String endAddress = legsObject.getString("end_address");

                                    txtLocation.setText(startAddress);
                                    txtDestination.setText(endAddress);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Log.e(TAG, "onFailure: " + t.getMessage());
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
