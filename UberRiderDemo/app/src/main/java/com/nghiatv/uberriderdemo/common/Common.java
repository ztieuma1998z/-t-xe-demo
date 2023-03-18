package com.nghiatv.uberriderdemo.common;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.nghiatv.uberriderdemo.model.DataMessage;
import com.nghiatv.uberriderdemo.model.FCMResponse;
import com.nghiatv.uberriderdemo.model.Rider;
import com.nghiatv.uberriderdemo.model.Token;
import com.nghiatv.uberriderdemo.remote.FCMClient;
import com.nghiatv.uberriderdemo.remote.RetrofitClient;
import com.nghiatv.uberriderdemo.remote.IFCMService;
import com.nghiatv.uberriderdemo.remote.IGoogleApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Common {
    private static final String TAG = "Common";

    public static final String DRIVER_TBL = "Drivers";
    public static final String USER_DRIVER_TBL = "DriversInformation";
    public static final String USER_RIDER_TBL = "RidersInformation";
    public static final String PICKUP_REQUEST_TBL = "PickupRequests";
    public static final String TOKEN_TBL = "Tokens";
    public static final String RATE_DETAIL_TBL = "RateDetails";

    public static final String ACCEPTED_MESSAGE = "Accepted";
    public static final String DECLINED_MESSAGE = "Declined";
    public static final String ARRIVED_MESSAGE = "Arrived";
    public static final String DROP_OFF_MESSAGE = "DropOff";


    public static final String BROADCAST_DROP_OFF = "Arrived";
    public static final String CANCEL_BROADCAST_STRING = "cancel_pickup";

    public static final String fcmUrl = "https://fcm.googleapis.com";
    public static final String googleAPIUrl = "https://maps.googleapis.com";

    public static final int PICK_IMAGE_REQUEST = 2000;

    public static Rider currentUser;
    public static Location mLastLocation;
    public static boolean isDriverFound = false;
    public static String driverId = "";

    private static double baseFare = 25000;
    private static double timeFare = 300;
    private static double distanceFare = 9000;

    public static double getPrice(double km, int min) {
        return baseFare + (timeFare * min) + (distanceFare * km);
    }

    public static IFCMService getFCMService() {
        return FCMClient.getClient(fcmUrl).create(IFCMService.class);
    }

    public static IGoogleApi getGoogleApi() {
        return RetrofitClient.getClient(googleAPIUrl).create(IGoogleApi.class);
    }

    public static void sendRequestToDriver(String driverId, final IFCMService mService, final Context context, final Location currentLocation) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.TOKEN_TBL);
        tokens.orderByKey().equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            Token token = postSnapshot.getValue(Token.class);

                            // Make raw payload - convert LatLng to Json
                            String riderToken = FirebaseInstanceId.getInstance().getToken();

                            Map<String, String> content = new HashMap<>();
                            content.put("customer", riderToken);
                            content.put("lat", String.valueOf(currentLocation.getLatitude()));
                            content.put("lng", String.valueOf(currentLocation.getLongitude()));

                            DataMessage dataMessage = new DataMessage(token.getToken(), content);

                            mService.sendMessage(dataMessage)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body() != null) {
                                                Toast.makeText(context, "Đã gửi yêu cầu", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(context, "Gửi yêu cầu thất bại", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                                            Log.d(TAG, "onFailure: " + t.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}
