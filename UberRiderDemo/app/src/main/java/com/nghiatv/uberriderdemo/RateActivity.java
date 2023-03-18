package com.nghiatv.uberriderdemo;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nghiatv.uberriderdemo.common.Common;
import com.nghiatv.uberriderdemo.model.Rate;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class RateActivity extends AppCompatActivity {
    private static final String TAG = "RateActivity";

    private Button btnSubmit;
    private MaterialRatingBar ratingBar;
    private MaterialEditText edtComment;

    private FirebaseDatabase database;
    private DatabaseReference rateDetailRef, driverInformationRef;

    private double ratingStars = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        rateDetailRef = database.getReference(Common.RATE_DETAIL_TBL);
        driverInformationRef = database.getReference(Common.USER_DRIVER_TBL);

        initializeComponents();
    }

    private void initializeComponents() {
        btnSubmit = findViewById(R.id.btnSubmit);
        ratingBar = findViewById(R.id.ratingBar);
        edtComment = findViewById(R.id.edtComment);

        ratingBar.setOnRatingChangeListener(new MaterialRatingBar.OnRatingChangeListener() {
            @Override
            public void onRatingChanged(MaterialRatingBar ratingBar, float rating) {
                ratingStars = rating;
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitRateDetails(Common.driverId);
            }
        });
    }

    private void submitRateDetails(final String driverId) {
        final AlertDialog waitingDialog = new SpotsDialog.Builder()
                .setContext(this)
                .build();
        waitingDialog.show();

        Rate rate = new Rate();
        rate.setRates(String.valueOf(ratingStars));
        rate.setComments(edtComment.getText().toString());

        // Update new value to Firebase
        rateDetailRef.child(driverId)
                .push()
                .setValue(rate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        rateDetailRef.child(driverId)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        double averageStars = 0.0;
                                        int count = 0;
                                        for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                                            Rate rate1 = postSnapShot.getValue(Rate.class);
                                            averageStars += Double.parseDouble(rate1.getRates());
                                            count++;
                                        }
                                        double finalAverage = averageStars / count;
                                        DecimalFormat df = new DecimalFormat("#.#");
                                        String valueUpdate = df.format(finalAverage);

                                        Map<String, Object> driverUpdateRate =  new HashMap<>();
                                        driverUpdateRate.put("rates", valueUpdate);

                                        driverInformationRef.child(Common.driverId)
                                                .updateChildren(driverUpdateRate)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        waitingDialog.dismiss();
                                                        Toast.makeText(RateActivity.this, "Cảm ơn bạn đã cho đánh giá", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        waitingDialog.dismiss();
                                                        Toast.makeText(RateActivity.this, "Rate không được cập nhật", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingDialog.dismiss();
                        Toast.makeText(RateActivity.this, "Rate thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
