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
    Button btnYeardApplySubscribe, btnMonthApplySubscribe, btnSixMonthApplySubscribe;
    private BillingClient billingClient;
    private BillingFlowParams billingFlowParams;
    private ArrayList<SkuDetails> skuDetalsList123 = new ArrayList<>();
    private static final String TAG = "My_SubClass";

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

        //Initialize a BillingClient
        billingClient = BillingClient.newBuilder(this)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
                        // This listener receives updates for all purchases in your app.
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null){
                            //To verify the purchases is real it requires two steps.
                            //1. Make sure if the item is in purchase stay and it not acknowledged before
                            //2. The second step to verify purchase in backend server, we will need to get to purchase token for each purchase and send it to
                            // backend server and then check the token is never use. If the token is never use it means it is a valid token. We can store the purchase info
                            // in online database.
                            for (Purchase purchase : list){
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED &&
                                        !purchase.isAcknowledged()){
                                    String token = purchase.getPurchaseToken();

                                }


                            }

                        }

                    }
                })
                .enablePendingPurchases()
                .build();

        connectToGooglePlayBilling();


    }

    @Override
    protected void onResume() {
        //https://developer.android.com/google/play/billing/integrate#fetch
        super.onResume();
        connectToGooglePlayBilling();
    }


    //использовать firebase cloud function as backend server. And firestore as the database.
    //Note: we use acknowledge purchase to sell product one-time. And we use consume async if the item purchase multiple times.
    @Override
    public void onClick(View v) {
        int responseCode;
        Activity activity = SubcribeClass.this;
        if (v.getId() == R.id.btn_Buy_month) {
            billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetalsList123.get(0))
                    .build();
            responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
        } else if (v.getId() == R.id.btn_buy_sixmonth) {

            billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetalsList123.get(1))
                    .build();
            responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
        } else if (v.getId() == R.id.btnYeardBuy) {

            billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetalsList123.get(2))
                    .build();
            responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
        }

    }

/*    public void areSubscriptionsSupported() {
        String s = BillingClient.FeatureType.SUBSCRIPTIONS;
        if (billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS) == BillingClient.FeatureType.SUBSCRIPTIONS) {

        }


        int responseCode = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        if (responseCode != BillingClient.BillingResponse.OK) {
            Timber.w("Got an error response: " + responseCode);
        }
        return responseCode == BillingClient.BillingResponse.OK;
    }*/


        private void connectToGooglePlayBilling () {
            //To connect to Google Play, call startConnection().
            billingClient.startConnection(new BillingClientStateListener() {

                //Establish a connection to Google Play
                //Подключение к Google Play
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

                        //To query Google Play for in-app product details, call querySkuDetailsAsync()
                        billingClient.querySkuDetailsAsync(params.build(),
                                new SkuDetailsResponseListener() {
                                    @Override
                                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                                        SkuDetails iteminfo = skuDetailsList.get(0);
                                        SkuDetails iteminfo1 = skuDetailsList.get(1);
                                        SkuDetails iteminfo2 = skuDetailsList.get(2);
                                        btnMonthApplySubscribe.setText(iteminfo.getPrice());
                                        btnSixMonthApplySubscribe.setText(iteminfo1.getPrice());
                                        btnYeardApplySubscribe.setText(iteminfo2.getPrice());
                                        skuDetalsList123.addAll(skuDetailsList);

                                        //Log.d(TAG, "onSkuDetailsResponse: iteminfo price: " + iteminfo.getPrice());
                                        /*int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();*/
                                        //Toast.makeText(activity, "responceCode: " + responseCode, Toast.LENGTH_LONG).show();
                                        // Process the result.
                                    }
                                });

                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    connectToGooglePlayBilling();
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            });

        }


    }
