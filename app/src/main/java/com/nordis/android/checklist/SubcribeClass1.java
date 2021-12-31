package com.nordis.android.checklist;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.example.android.checklist.R;

import java.util.ArrayList;
import java.util.List;

public class SubcribeClass1 extends AppCompatActivity implements View.OnClickListener {
    Button btnSubcribeButton,btnYeardApplySubscribe,btnMonthApplySubscribe,btnSixMonthApplySubscribe;
    private BillingClient billingClient;
    //private SkuDetails iteminfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_layout);

        btnYeardApplySubscribe = findViewById(R.id.btnYeardBuy);
        btnMonthApplySubscribe = findViewById(R.id.btn_Buy_month);
        btnSixMonthApplySubscribe = findViewById(R.id.btn_buy_sixmonth);

        btnSixMonthApplySubscribe.setOnClickListener(this);
        btnYeardApplySubscribe.setOnClickListener(this);
        btnMonthApplySubscribe.setOnClickListener(this);

        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {


            }
        }).build();
        connectToGooglePlayBilling();


    }
    private void connectToGooglePlayBilling(){

        billingClient.startConnection(
                new BillingClientStateListener() {
                    @Override
                    public void onBillingServiceDisconnected() {
                        connectToGooglePlayBilling();
                    }

                    @Override
                    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){

                            getProductDetals();
                        }
                    }
                }
        );
    }
    private void getProductDetals(){


        List<String> productID = new ArrayList<>();
        productID.add("checklist_app_six_month");
        SkuDetailsParams getProductDetalsQuery = SkuDetailsParams
                .newBuilder()
                .setSkusList(productID)
                .setType(BillingClient.SkuType.SUBS)
                .build();

        Activity activity = SubcribeClass1.this;
        billingClient.querySkuDetailsAsync(
                getProductDetalsQuery, new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> list) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
                             SkuDetails iteminfo = list.get(0);
                             btnSubcribeButton.setText(iteminfo.getPrice());

                            btnSubcribeButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setSkuDetails(iteminfo).build());
                                }
                            });
                            Toast.makeText(activity,"iteminfo = " + iteminfo,Toast.LENGTH_LONG).show();

                        }
                    }
                }
        );
    }



     //использовать firebase cloud function as backend server. And firestore as the database.
    //Note: we use acknowledge purchase to sell product one-time. And we use consume async if the item purchase multiple times.
    @Override
    public void onClick(View v) {
        Activity activity = this;
        if (v.getId() == R.id.btn_Buy_month){
            Toast.makeText(this,"Hi",Toast.LENGTH_LONG).show();
        }
/*        if (v.getId() == R.id.btntryForFree) {
            if (iteminfo == null){
                Toast.makeText(this,"iteminfo = null",Toast.LENGTH_LONG).show();
            }else {
                billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setSkuDetails(iteminfo).build());
            }


        }else if (v.getId() == R.id.btn_Buy_month){


        }*/

    }


}
