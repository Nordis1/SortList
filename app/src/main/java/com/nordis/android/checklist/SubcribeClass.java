package com.nordis.android.checklist;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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

public class SubcribeClass extends AppCompatActivity implements View.OnClickListener {
    Button btnSubcribeButton, btnYeardApplySubscribe, btnMonthApplySubscribe, btnSixMonthApplySubscribe;
    private BillingClient billingClient;
    private static final String TAG = "My_SubClass";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_layout);

        btnSubcribeButton = findViewById(R.id.btntryForFree);
        btnYeardApplySubscribe = findViewById(R.id.btnYeardBuy);
        btnMonthApplySubscribe = findViewById(R.id.btn_Buy_month);
        btnSixMonthApplySubscribe = findViewById(R.id.btn_buy_sixmonth);

        btnSixMonthApplySubscribe.setOnClickListener(this);
        btnYeardApplySubscribe.setOnClickListener(this);
        btnMonthApplySubscribe.setOnClickListener(this);
        btnSubcribeButton.setOnClickListener(this);

        billingClient = BillingClient.newBuilder(this)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

                    }
                })
                .enablePendingPurchases()
                .build();


    }



    //использовать firebase cloud function as backend server. And firestore as the database.
    //Note: we use acknowledge purchase to sell product one-time. And we use consume async if the item purchase multiple times.
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_Buy_month) {
            connectToGooglePlayBilling(0);
        } else if (v.getId() == R.id.btn_buy_sixmonth) {
            connectToGooglePlayBilling(1);
        }else if (v.getId() == R.id.btnYeardBuy) {
            connectToGooglePlayBilling(2);
        }

    }

    private void connectToGooglePlayBilling(Integer whichSubType) {

        billingClient.startConnection(new BillingClientStateListener() {

            //Establish a connection to Google Play

            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    List<String> skuList = new ArrayList<>();
                    skuList.add("checklist_app");
                    skuList.add("checklist_app_six_month");
                    skuList.add("checklist_app_yeard");
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);

                    Activity activity = SubcribeClass.this;

                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                            .setSkuDetails(skuDetailsList.get(whichSubType))
                                            .build();
                                    SkuDetails iteminfo = skuDetailsList.get(whichSubType);
                                    Log.d(TAG, "onSkuDetailsResponse: iteminfo price: " + iteminfo.getPrice());
                                    int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
                                    Toast.makeText(activity, "responceCode: " + responseCode, Toast.LENGTH_LONG).show();
                                    // Process the result.
                                }
                            });

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                connectToGooglePlayBilling(whichSubType);
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });

    }
}
