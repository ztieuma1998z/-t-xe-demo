package com.nghiatv.uberdemo.common;

import android.location.Location;

import com.nghiatv.uberdemo.model.Driver;
import com.nghiatv.uberdemo.remote.FCMClient;
import com.nghiatv.uberdemo.remote.IFCMService;
import com.nghiatv.uberdemo.remote.IGoogleApi;
import com.nghiatv.uberdemo.remote.RetrofitClient;

public class Common {
    public static final String DRIVER_TBL = "Drivers";
    public static final String USER_DRIVER_TBL = "DriversInformation";
    public static final String USER_RIDER_TBL = "RidersInformation";
    public static final String PICKUP_REQUEST_TBL = "PickupRequests";
    public static final String TOKEN_TBL = "Tokens";

    public static final String ACCEPTED_MESSAGE = "Accepted";
    public static final String DECLINED_MESSAGE = "Declined";
    public static final String ARRIVED_MESSAGE = "Arrived";
    public static final String DROP_OFF_MESSAGE = "DropOff";

    public static final String baseUrl = "https://maps.googleapis.com";
    public static final String fcmUrl = "https://fcm.googleapis.com";

    public static final int PICK_IMAGE_REQUEST = 1000;

    public static double base_fare = 25000;
    private static double time_rate = 300;
    private static double distance_rate = 9000;
    public static double formulaPrice(double km, double min) {
        return base_fare + (distance_rate * km) + (time_rate * min);
    }

    public static Driver currentDriver;
    public static Location mLastLocation = null;

    public static IGoogleApi getGoogleApi() {
        return RetrofitClient.getClient(baseUrl).create(IGoogleApi.class);
    }

    public static IFCMService getFCMService() {
        return FCMClient.getClient(fcmUrl).create(IFCMService.class);
    }
}
