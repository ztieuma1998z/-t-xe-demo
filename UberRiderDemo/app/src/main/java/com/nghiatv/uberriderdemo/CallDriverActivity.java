package com.nghiatv.uberriderdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nghiatv.uberriderdemo.common.Common;
import com.nghiatv.uberriderdemo.model.Rider;
import com.nghiatv.uberriderdemo.remote.IFCMService;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CallDriverActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "CallDriverActivity";

    private CircleImageView imgDriverAvatar;
    private TextView txtDriverName, txtDriverPhone, txtDriverRate;
    private Button btnCallDriverByApp, btnCallDriverByPhone;

    private String driverId;
    private Location mLastLocation;

    private IFCMService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_driver);

        mService = Common.getFCMService();

        initializeComponents();

        if (getIntent() != null) {
            driverId = getIntent().getStringExtra("driverId");
            double lat = getIntent().getDoubleExtra("lat", -1.0);
            double lng = getIntent().getDoubleExtra("lng", -1.0);

            mLastLocation = new Location("");
            mLastLocation.setLatitude(lat);
            mLastLocation.setLongitude(lng);
            
            loadDriverInformation();
        }
    }

    private void loadDriverInformation() {
        FirebaseDatabase.getInstance().getReference(Common.USER_DRIVER_TBL)
                .child(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Rider driverUser = dataSnapshot.getValue(Rider.class);

                        if (!driverUser.getAvatarUrl().isEmpty()) {
                            Picasso.with(getBaseContext())
                                    .load(driverUser.getAvatarUrl())
                                    .into(imgDriverAvatar);
                        }
                        txtDriverName.setText(driverUser.getName());
                        txtDriverPhone.setText(driverUser.getPhone());
                        txtDriverRate.setText(driverUser.getRate());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void initializeComponents() {
        imgDriverAvatar = findViewById(R.id.imgDriverAvatar);

        txtDriverName = findViewById(R.id.txtDriverName);
        txtDriverPhone = findViewById(R.id.txtDriverPhone);
        txtDriverRate = findViewById(R.id.txtDriverRate);

        btnCallDriverByApp = findViewById(R.id.btnCallDriverByApp);
        btnCallDriverByPhone = findViewById(R.id.btnCallDriverByPhone);
        btnCallDriverByApp.setOnClickListener(this);
        btnCallDriverByPhone.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnCallDriverByApp:
                if (driverId != null && driverId.isEmpty()) {
                    Common.sendRequestToDriver(driverId, mService, getBaseContext(), mLastLocation);
                }
                break;

            case R.id.btnCallDriverByPhone:
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel: " + txtDriverPhone.getText().toString()));

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                startActivity(intent);
                break;

            default:
                break;
        }
    }
}
