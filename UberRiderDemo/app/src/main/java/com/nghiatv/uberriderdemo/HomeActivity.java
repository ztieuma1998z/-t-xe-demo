package com.nghiatv.uberriderdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arsy.maps_library.MapRipple;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.SphericalUtil;
import com.nghiatv.uberriderdemo.common.Common;
import com.nghiatv.uberriderdemo.helper.CustomInfoWindow;
import com.nghiatv.uberriderdemo.model.Rider;
import com.nghiatv.uberriderdemo.model.Token;
import com.nghiatv.uberriderdemo.remote.IFCMService;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, ValueEventListener {

    private static final String TAG = "HomeActivity";
    private static final int MY_PERMISSION_REQUEST_CODE = 2000;
    private static final int PLAY_SERVICE_RES_REQUEST = 2001;
    private static final int UPDATE_INTERVAL = 5000;
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private DatabaseReference ref;
    private GeoFire geoFire;
    private Marker mUserMarker, mDestinationMarker;

    private SupportMapFragment mapFragment;

    // Bottom sheet
    private Button btnPickupRequest;

    private int radius = 1;
    private int distance = 1;
    private static final int LIMIT = 3;

    // Send alert
    private IFCMService mService;

    //Presense system
    private DatabaseReference driverAvailable;

    private PlaceAutocompleteFragment placeLocation, placeDestination;
    private AutocompleteFilter typeFilter;
    private String mPlaceLocation, mPlaceDestination;

    // Vehicle Type
    private ImageView imgSelectUberX, imgSelectUberBlack;
    private boolean isUberX = true;

    private CircleImageView imgAvatar;
    private TextView txtRiderName, txtRiderStars;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // Broadcast
    private BroadcastReceiver mCancelBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Common.isDriverFound = false;
            Common.driverId = "";

            btnPickupRequest.setText("TÌM XE");
            btnPickupRequest.setEnabled(true);

            mUserMarker.hideInfoWindow();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Register for Cancel Request
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCancelBroadcast, new IntentFilter(Common.CANCEL_BROADCAST_STRING));
        // Register for Arrived Request
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCancelBroadcast, new IntentFilter(Common.BROADCAST_DROP_OFF));

        mService = Common.getFCMService();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Init Firebase Storage
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = navigationView.getHeaderView(0);
        txtRiderName = navigationHeaderView.findViewById(R.id.txtRiderName);
        txtRiderStars = navigationHeaderView.findViewById(R.id.txtRiderStars);
        imgAvatar = navigationHeaderView.findViewById(R.id.imgAvatar);

        txtRiderName.setText(Common.currentUser.getName());
        txtRiderStars.setText(Common.currentUser.getRate());
        if (Common.currentUser.getAvatarUrl() != null
                && !TextUtils.isEmpty(Common.currentUser.getAvatarUrl())) {
            Picasso.with(this)
                    .load(Common.currentUser.getAvatarUrl())
                    .into(imgAvatar);
        }

        // Maps
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Geo Fire
        ref = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL);
        geoFire = new GeoFire(ref);

        // Init view
        initializeComponents();

        setUpLocation();

        updateTokenToSerVer();

    }

    private void updateTokenToSerVer() {
        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
            @Override
            public void onSuccess(final Account account) {
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                final DatabaseReference tokens = db.getReference(Common.TOKEN_TBL);

                FirebaseInstanceId.getInstance()
                        .getInstanceId()
                        .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                            @Override
                            public void onSuccess(InstanceIdResult instanceIdResult) {
                                Token token = new Token(instanceIdResult.getToken());
                                tokens.child(account.getId())
                                        .setValue(token);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(AccountKitError accountKitError) {

            }
        });
    }

    private void initializeComponents() {
        btnPickupRequest = findViewById(R.id.btnPickupRequest);
        imgSelectUberX = findViewById(R.id.imgSelectUberX);
        imgSelectUberBlack = findViewById(R.id.imgSelectUberBlack);

        imgSelectUberX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isUberX = true;
                if (isUberX) {
                    imgSelectUberX.setImageResource(R.drawable.car_cui_select);
                    imgSelectUberBlack.setImageResource(R.drawable.car_vip);
                } else {
                    imgSelectUberX.setImageResource(R.drawable.car_cui);
                    imgSelectUberBlack.setImageResource(R.drawable.car_vip_select);
                }

                mMap.clear();

                if (driverAvailable != null) {
                    driverAvailable.removeEventListener(HomeActivity.this);
                }
                driverAvailable = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child(isUberX ? "UberX" : "Uber Black");
                driverAvailable.addValueEventListener(HomeActivity.this);

                loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }
        });

        imgSelectUberBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isUberX = false;
                if (isUberX) {
                    imgSelectUberX.setImageResource(R.drawable.car_cui_select);
                    imgSelectUberBlack.setImageResource(R.drawable.car_vip);
                } else {
                    imgSelectUberX.setImageResource(R.drawable.car_cui);
                    imgSelectUberBlack.setImageResource(R.drawable.car_vip_select);
                }

                mMap.clear();

                if (driverAvailable != null) {
                    driverAvailable.removeEventListener(HomeActivity.this);
                }
                driverAvailable = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child(isUberX ? "UberX" : "Uber Black");
                driverAvailable.addValueEventListener(HomeActivity.this);

                loadAllAvailableDriver(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            }
        });

        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Common.isDriverFound) {
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(Account account) {
                            requestPickupHere(account.getId());
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                        }
                    });
                } else {
                    btnPickupRequest.setEnabled(false);
                    Common.sendRequestToDriver(Common.driverId, mService, getBaseContext(), Common.mLastLocation);
                }
            }
        });

        placeLocation = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.placeLocation);
        placeDestination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.placeDestination);
        typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .setTypeFilter(3)
                .build();

        placeLocation.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceLocation = place.getAddress().toString();

                // Remove old marker
                mMap.clear();

                // Add marker at new location
                mUserMarker = mMap.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                        .title("Pickup here"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));

            }

            @Override
            public void onError(Status status) {

            }
        });

        placeDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceDestination = place.getAddress().toString();

                mMap.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                        .title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));

                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstance(mPlaceLocation, mPlaceDestination, false);
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    private void requestPickupHere(String uid) {
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.PICKUP_REQUEST_TBL);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude())
                , new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

        if (mUserMarker.isVisible()) {
            mUserMarker.remove();
        }

        // Add new marker
        mUserMarker = mMap.addMarker(new MarkerOptions()
                .title("Đón xe tại đây")
                .snippet("")
                .position(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));
        mUserMarker.showInfoWindow();

        btnPickupRequest.setText("ĐANG TÌM XE...");

        findDriver();
    }

    private void findDriver() {
        DatabaseReference driverLocation;
        if (isUberX) {
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child("UberX");
        } else {
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child("Uber Black");
        }

        GeoFire gf = new GeoFire(driverLocation);
        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // if found
                if (!Common.isDriverFound) {
                    Common.isDriverFound = true;
                    Common.driverId = key;
                    btnPickupRequest.setText("GỌI XE");
                    //Toast.makeText(HomeActivity.this, "" + key, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // If still not found driver, increase distance
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                if (!Common.isDriverFound && radius < LIMIT) {
                    radius++;
                    findDriver();
                } else {
                    Toast.makeText(HomeActivity.this, "Không có xe nào gần vị trí của bạn", Toast.LENGTH_SHORT).show();
                    btnPickupRequest.setText("TÌM XE");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request runtime permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION
                            , Manifest.permission.ACCESS_FINE_LOCATION
                            , Manifest.permission.CALL_PHONE},
                    MY_PERMISSION_REQUEST_CODE);
        } else {
            buildLocationCallBack();
            buildLocationRequest();
            displayLocation();
        }
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLastLocation = locationResult.getLastLocation();
                Common.mLastLocation = locationResult.getLocations().get(locationResult.getLocations().size() - 1);
                displayLocation();
            }
        };
    }

    private void buildLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Common.mLastLocation = location;
                        if (Common.mLastLocation != null) {
                            // Create LatLng from mLastLocation and this is center point
                            LatLng center = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
                            //distance in metter
                            //heading 0 is north, 90 is east, 180 is south, 270 is west
                            LatLng northSide = SphericalUtil.computeOffset(center, 100000, 0);
                            LatLng southSide = SphericalUtil.computeOffset(center, 100000, 180);
                            LatLngBounds bounds = LatLngBounds.builder()
                                    .include(northSide)
                                    .include(southSide)
                                    .build();

                            placeLocation.setBoundsBias(bounds);
                            placeLocation.setFilter(typeFilter);

                            placeDestination.setBoundsBias(bounds);
                            placeDestination.setFilter(typeFilter);

                            // Presense system
                            driverAvailable = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child(isUberX ? "UberX" : "Uber Black");
                            driverAvailable.addValueEventListener(HomeActivity.this);

                            final double latitude = Common.mLastLocation.getLatitude();
                            final double longitude = Common.mLastLocation.getLongitude();

                            loadAllAvailableDriver(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));

                            Log.d(TAG, "displayLocation: " + String.format("Your location was changed: %f / %f", latitude, longitude));

                        } else {
                            Log.d(TAG, "displayLocation: Cannot get your loaction");
                        }
                    }
                });
    }

    private void loadAllAvailableDriver(final LatLng location) {
        // Update to FireBase
        mMap.clear();

        mUserMarker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .title("Bạn"));

        // Move camera to this position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f));

        // Load all available Driver in distance 3 km
        DatabaseReference driverLocation;

        if (isUberX) {
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child("UberX");
        } else {
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.DRIVER_TBL).child("Uber Black");
        }

        GeoFire gf = new GeoFire(driverLocation);
        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.latitude, location.longitude), distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                // Use key to get email from table Users
                // Table Users is table when driver register account and update information
                FirebaseDatabase.getInstance().getReference(Common.USER_DRIVER_TBL)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                // Because Rider and User model is same properties
                                // So we can use Rider model to get User here
                                Rider rider = dataSnapshot.getValue(Rider.class);

                                if (isUberX) {
                                    if (rider.getCarType().equals("UberX")) {
                                        // Add driver to map
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
                                                .flat(true)
                                                .title(rider.getName())
                                                .snippet("ID lái xe : " + dataSnapshot.getKey())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    }
                                } else {
                                    if (rider.getCarType().equals("Uber Black")) {
                                        // Add driver to map
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
                                                .flat(true)
                                                .title(rider.getName())
                                                .snippet("ID lái xe : " + dataSnapshot.getKey())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT) {
                    distance++;
                    loadAllAvailableDriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in .
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.navUpdateInformation) {
            showUpdateInformationDialog();
        } else if (id == R.id.navSignOut) {
            signOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showUpdateInformationDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View updateInformationDialog = inflater.inflate(R.layout.dialog_update_information, null);

        final MaterialEditText edtName = updateInformationDialog.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = updateInformationDialog.findViewById(R.id.edtPhone);
        final ImageView imgUpload = updateInformationDialog.findViewById(R.id.imgUpload);

        imgUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(updateInformationDialog)
                .setTitle("THAY ĐỔI THÔNG TIN CÁ NHÂN")
                .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        final android.app.AlertDialog waitingDialog = new SpotsDialog.Builder()
                                .setContext(HomeActivity.this)
                                .build();
                        waitingDialog.show();

                        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                            @Override
                            public void onSuccess(Account account) {
                                String name = edtName.getText().toString();
                                String phone = edtPhone.getText().toString();
                                Map<String, Object> updateInfo = new HashMap<>();

                                if (!TextUtils.isEmpty(name)) {
                                    updateInfo.put("name", name);
                                }
                                if (!TextUtils.isEmpty(phone)) {
                                    updateInfo.put("phone", phone);
                                }

                                DatabaseReference driverInformation = FirebaseDatabase.getInstance().getReference(Common.USER_RIDER_TBL);
                                driverInformation.child(account.getId())
                                        .updateChildren(updateInfo)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(HomeActivity.this, "Thông tin cá nhân đã được cập nhật", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(HomeActivity.this, "Thông tin cá nhân cập nhật thất bại", Toast.LENGTH_SHORT).show();
                                                }

                                                waitingDialog.dismiss();
                                            }
                                        });
                            }

                            @Override
                            public void onError(AccountKitError accountKitError) {

                            }
                        });

                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture: "), Common.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Common.PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            Uri saveUri = data.getData();
            if (saveUri != null) {
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setMessage("Đang tải ảnh...");
                mDialog.show();

                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("images/" + imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                mDialog.dismiss();
                                imageFolder.getDownloadUrl()
                                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(final Uri uri) {
                                                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                                                    @Override
                                                    public void onSuccess(Account account) {
                                                        Map<String, Object> avatarUpdate = new HashMap<>();
                                                        avatarUpdate.put("avatarUrl", uri.toString());

                                                        DatabaseReference driverInformation = FirebaseDatabase.getInstance().getReference(Common.USER_RIDER_TBL);
                                                        driverInformation.child(account.getId())
                                                                .updateChildren(avatarUpdate)
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Toast.makeText(HomeActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                                                                        } else {
                                                                            Toast.makeText(HomeActivity.this, "Upload error", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                    }

                                                    @Override
                                                    public void onError(AccountKitError accountKitError) {

                                                    }
                                                });
                                            }
                                        });
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                mDialog.setMessage("Đã tải được " + progress + "%");
                            }
                        });
            }
        }
    }

    private void signOut() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("ĐĂNG XUẤT")
                .setMessage("Bạn có muốn đăng xuất?")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AccountKit.logOut();
                        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        builder.show();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            boolean isSuccess = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map));
            if (!isSuccess) {
                Log.e(TAG, "Map style load failed ");
            }

        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mDestinationMarker != null) {
                    mDestinationMarker.remove();
                }
                mDestinationMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                        .title("Điểm đến"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstance(String.format("%f,%f", mLastLocation.getLatitude(), mLastLocation.getLongitude())
                        , String.format("%f,%f", latLng.latitude, latLng.longitude)
                        , true);
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
            }
        });

        mMap.setOnInfoWindowClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (!marker.getTitle().equals("Bạn")) {
            Intent intent = new Intent(this, CallDriverActivity.class);
            intent.putExtra("driverId", marker.getSnippet().replaceAll("\\D+", ""));
            intent.putExtra("lat", Common.mLastLocation.getLatitude());
            intent.putExtra("lng", Common.mLastLocation.getLongitude());
            startActivity(intent);
        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        loadAllAvailableDriver(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCancelBroadcast);

        super.onDestroy();
    }
}
