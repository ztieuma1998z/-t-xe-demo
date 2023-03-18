package com.nghiatv.uberdemo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nghiatv.uberdemo.common.Common;
import com.nghiatv.uberdemo.model.DataMessage;
import com.nghiatv.uberdemo.model.FCMResponse;
import com.nghiatv.uberdemo.model.Token;
import com.nghiatv.uberdemo.remote.IFCMService;
import com.nghiatv.uberdemo.remote.IGoogleApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerCallActivity extends AppCompatActivity {
    private static final String TAG = "CustomerCallActivity";

    private TextView txtTime, txtDistance, txtAddress, txtCountDown;
    private Button btnAccept, btnDecline;

    private MediaPlayer mediaPlayer;
    private IGoogleApi mService;
    private IFCMService mFCMService;

    private String customerId;
    private String lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_call);

        mService = Common.getGoogleApi();
        mFCMService = Common.getFCMService();

        // Init view
        initializeComponents();
    }

    private void initializeComponents() {
        txtTime = findViewById(R.id.txtTime);
        txtDistance = findViewById(R.id.txtDistance);
        txtAddress = findViewById(R.id.txtAddress);
        txtCountDown = findViewById(R.id.txtCountDown);

        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(customerId)) {
                    declineBooking(customerId);
                }
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerCallActivity.this, DriverTrackingActivity.class);
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("customerId", customerId);

                startActivity(intent);
                finish();
            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if (getIntent() != null) {
            lat = getIntent().getStringExtra("lat");
            lng = getIntent().getStringExtra("lng");
            customerId = getIntent().getStringExtra("customer");

            getDirection(lat, lng);
        }
        
        startTimer();
    }

    private void startTimer() {
        CountDownTimer countDownTimer = new CountDownTimer(31000, 1000) {
            @Override
            public void onTick(long l) {
                txtCountDown.setText(String.valueOf(l / 1000));
            }

            @Override
            public void onFinish() {
                if (!TextUtils.isEmpty(customerId)) {
                    declineBooking(customerId);
                } else {
                    Toast.makeText(CustomerCallActivity.this, "ID khách hàng không được null", Toast.LENGTH_SHORT).show();
                }
            }
        }.start();
    }

    private void declineBooking(String customerId) {
        Token token = new Token(customerId);
        //Notification notification = new Notification(Common.DECLINED_MESSAGE, "Driver has declined your request");
        //Sender sender = new Sender(token.getToken(), notification);

        Map<String, String> content = new HashMap<>();
        content.put("title", Common.DECLINED_MESSAGE);
        content.put("message", "Lái xe đã từ chối yêu cầu của bạn");
        DataMessage dataMessage = new DataMessage(token.getToken(), content);

        mFCMService.sendMessage(dataMessage)
                .enqueue(new Callback<FCMResponse>() {
                    @Override
                    public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                        if (response.body().getSuccess() == 1) {
                            Toast.makeText(CustomerCallActivity.this, "Từ chối", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<FCMResponse> call, Throwable t) {

                    }
                });
    }

    private void getDirection(String lat, String lng) {
        String requestApi = null;

        try {
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"
                    + "mode=driving&"
                    + "transit_routing_preference=less_driving&"
                    + "origin=" + Common.mLastLocation.getLatitude() + "," + Common.mLastLocation.getLongitude() + "&"
                    + "destination=" + lat + "," + lng + "&"
                    + "key=" + getResources().getString(R.string.google_direction_api);

            Log.d(TAG, "getDirection: " + requestApi);

            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray routes = jsonObject.getJSONArray("routes");

                                // After get routes, just get first element of routes
                                JSONObject object = routes.getJSONObject(0);

                                // After get first element, we need get array with name "legs"
                                JSONArray legs = object.getJSONArray("legs");

                                // Add get first element of  legs array
                                JSONObject legsObject = legs.getJSONObject(0);

                                // Now get distance
                                JSONObject distance = legsObject.getJSONObject("distance");
                                txtDistance.setText(distance.getString("text"));

                                // Get time
                                JSONObject time = legsObject.getJSONObject("duration");
                                txtTime.setText(time.getString("text"));

                                // Get address
                                String address = legsObject.getString("end_address");
                                txtAddress.setText(address);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(CustomerCallActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onStop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.release();
        }
        super.onStop();
    }
}
