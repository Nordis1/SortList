package com.nordis.android.checklist;

import static com.nordis.android.checklist.MainActivity.countDownLatch;
import static com.nordis.android.checklist.MainActivity.handler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.example.android.checklist.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SubcribeClass extends AppCompatActivity implements View.OnClickListener {
    Button btnYeardApplySubscribe, btnMonthApplySubscribe, btnSixMonthApplySubscribe;
    private volatile BillingClient billingClient;
    private BillingFlowParams billingFlowParams;
    private ArrayList<SkuDetails> skuDetalsList123 = new ArrayList<>();
    public static final String TAG = "My_SubClass";
    DatabaseReference myRef;
    FirebaseDatabase database;
    String currentPrice;
    static Handler handlerForSubscribtionClass;
    SharedPreferences sPref;
    String whatSubChosen;
    Activity activity = SubcribeClass.this;
    ExecutorService executorServiceSubClass = Executors.newFixedThreadPool(2);


    String currentPurchaseToken;
    final int hSubISActivated = 2;
    final int hRecordInSPref = 1;
    final int hSubscribtionActiveYet = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_layout);
        handlerForSubscribtionClass = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                String gettingPrice = msg.getData().getString("PurchaseHasBeenSold");
                String getException = msg.getData().getString("Exception");
                if (gettingPrice != null) {
                    Toast.makeText(SubcribeClass.this, "Подписка дейсвует за " + gettingPrice, Toast.LENGTH_LONG).show();
                }
                if (getException != null) {
                    Toast.makeText(SubcribeClass.this, "Ошибка " + getException, Toast.LENGTH_LONG).show();

                }

                switch (msg.what) {
                    case hSubISActivated:
                        Toast.makeText(SubcribeClass.this, "Подписка активированна!", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(SubcribeClass.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        Log.d(TAG, "handleMessage: запускаем mainActiviy по новой");
                        startActivity(i);

                        break;
                    case hSubscribtionActiveYet:
                        Toast.makeText(SubcribeClass.this, "Подписка ещё дейсвует", Toast.LENGTH_LONG).show();
                        break;
                    case hRecordInSPref:
                        sPref = getSharedPreferences("Tokens", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        Log.d(TAG, "handleMessage: Удаление старого токена и запись нового");
                        SharedPreferences.Editor editor = sPref.edit();
                        editor.putString("Token", currentPurchaseToken);
                        editor.putString("Period&SubTime", whatSubChosen);
                        editor.apply();
                        sPref = getSharedPreferences("SEARCH_REMAIN",MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        Log.d(TAG, "handleMessage: Так как была подписка Search remain очищается");
                        break;
                }
            }

        };



        /*final String databaseUrl = "https://checklist-nordis-default-rtdb.europe-west1.firebasedatabase.app";
        database = FirebaseDatabase.getInstance(databaseUrl);*/

        btnYeardApplySubscribe = findViewById(R.id.btnYeardBuy);
        btnMonthApplySubscribe = findViewById(R.id.btn_Buy_month);
        btnSixMonthApplySubscribe = findViewById(R.id.btn_buy_sixmonth);

        btnSixMonthApplySubscribe.setOnClickListener(this);
        btnYeardApplySubscribe.setOnClickListener(this);
        btnMonthApplySubscribe.setOnClickListener(this);

        //Initialize a BillingClient
        //Первое что нужно это. Инициализировать клиента, второе Подконектиться к Google Play.
        initializeBillingClient(this);

        connectToGooglePlayBilling(false);


    }

    public void initializeBillingClient(Context context) {

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

        billingClient = BillingClient.newBuilder(context)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
                        // This listener receives updates for all purchases in your app.
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {

                            //To verify the purchases is real it requires two steps.
                            //1. Make sure if the item is in purchase stay and it not acknowledged before
                            //2. The second step to verify purchase in backend server, we will need to get to purchase token for each purchase and send it to
                            // backend server and then check the token is never use. If the token is never use it means it is a valid token. We can store the purchase info
                            // in online database.

                            for (Purchase purchase : list) {
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED &&
                                        !purchase.isAcknowledged()) {
                                    Log.d(TAG, "onPurchasesUpdated: Начинаем попытку подтверждения подписки!");
/*
                                    Log.d(TAG, "onPurchasesUpdated:  Зашли в метод Токен:" + purchase.getPurchaseToken() );
                                    Log.d(TAG, "onPurchasesUpdated:  Зашли в метод Order ID:" + purchase.getOrderId() );
                                    Log.d(TAG, "onPurchasesUpdated:  Зашли в метод purchase.toString:" + purchase.toString() );*/

                                    String orderId = purchase.getOrderId().replaceAll("[.]", "-");
                                    String purchaseTime = format.format(new Date(purchase.getPurchaseTime()));
                                    currentPurchaseToken = purchase.getPurchaseToken();
                                    String purchInfo = orderId + " " + purchaseTime;
                                    whatSubChosen = whatSubChosen + "/" + purchaseTime;


                                    //UserConsumSub User = new UserConsumSub(purchaseTime,orderId,currentPurchaseToken,currentPrice);
                                    //myRef = database.getReference(orderId);// В getReference нельзя указывать '.' поскольку не будет вписанно. И '/' поскольку будет создавать автоматом новые child.
                                    //myRef.setValue(currentPurchaseToken);

                                    AcknowledgePurchaseParams purchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(currentPurchaseToken).build();
                                    billingClient.acknowledgePurchase(purchaseParams, new AcknowledgePurchaseResponseListener() {
                                        @Override
                                        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                handlerForSubscribtionClass.sendEmptyMessage(hRecordInSPref);
                                                handlerForSubscribtionClass.sendEmptyMessage(hSubISActivated);
                                            }
                                        }
                                    });

                                }


                            }

                        }

                    }
                })
                .enablePendingPurchases()
                .build();

        Log.d(TAG, "initializeBillingClient: Initialize was success!");
    }
    public Boolean checkConnections(){
        if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTED){
            Log.d(TAG, "checkConnections: true");
            return true;
        }else {
            Log.d(TAG, "checkConnections: false");
            return false;
        }
    }

    @Override
    protected void onResume() {
        //https://developer.android.com/google/play/billing/integrate#fetch
        super.onResume();
        connectToGooglePlayBilling(false);
    }


    //использовать firebase cloud function as backend server. And firestore as the database.
    @Override
    public void onClick(View v) {
        int responseCode;
        if (v.getId() == R.id.btn_Buy_month) {
            initAndCheckPurchase(skuDetalsList123.get(0));

        } else if (v.getId() == R.id.btn_buy_sixmonth) {
            initAndCheckPurchase(skuDetalsList123.get(1));


        } else if (v.getId() == R.id.btnYeardBuy) {
            initAndCheckPurchase(skuDetalsList123.get(2));

        }

    }

    public void initAndCheckPurchase(SkuDetails skuDetails) {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                if (!list.isEmpty()) {
                    for (Purchase purchase : list) {
                        connectionStateGeneration(billingClient.getConnectionState());
                        purchaseStateGeneration(purchase.getPurchaseState());
                        billingClientResponseCodeGenerated(billingResult.getResponseCode());

                        handlerForSubscribtionClass.sendEmptyMessage(hSubscribtionActiveYet);
                        Log.d(TAG, "onQueryPurchasesResponse: подписка есть! " + purchase.toString());
                    }
                } else {
                    whatSubChosen = skuDetails.getSubscriptionPeriod();// whatSubChosen окончательно отылаеться в onAcknowledgePurchaseResponse. Для проверки в main.
                    Log.d(TAG, "onQueryPurchasesResponse: period is: " + whatSubChosen);
                    currentPrice = skuDetails.getPrice();
                    billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build();
                    billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();

                }

            }
        });


    }
    public static void connectionStateGeneration(int state){
        switch (state){
            case 0:
                Log.i(TAG, "connectionStateGeneration: Disconnected");
                break;
            case 1:
                Log.i(TAG, "connectionStateGeneration: Connecting...");
                break;
            case 2:
                Log.d(TAG, "connectionStateGeneration: Connected");
                break;
            case 3:
                Log.d(TAG, "connectionStateGeneration: Closed");
                break;
            default:
                Log.d(TAG, "connectionStateGeneration: Not definitely");
                break;

        }
    }
    public static void purchaseStateGeneration(int state){
        switch (state){
            case 0:
                Log.d(TAG, "purchaseStateGeneration: UNSPECIFIED_STATE");
                break;
            case 1:
                Log.d(TAG, "purchaseStateGeneration: PURCHASED");
                break;
            case 2:
                Log.d(TAG, "purchaseStateGeneration: PENDING");
            default:
                Log.d(TAG, "purchaseStateGeneration: Not definitely");
                break;
        }
    }
    public static void billingClientResponseCodeGenerated(int state){
        switch (state){
            case 0:
                Log.d(TAG, "billingClientResponseCodeGenerated: OK");
                break;
            case 1:
                Log.d(TAG, "billingClientResponseCodeGenerated: USER_CANCELED");
                break;
            case 2:
                Log.d(TAG, "billingClientResponseCodeGenerated: SERVICE_UNAVAILABLE");
                break;
            case 3:
                Log.d(TAG, "billingClientResponseCodeGenerated: BILLING_UNAVAILABLE");
                break;
            case 4:
                Log.d(TAG, "billingClientResponseCodeGenerated: ITEM_UNAVAILABLE");
                break;
            case 5:
                Log.d(TAG, "billingClientResponseCodeGenerated: DEVELOPER_ERROR");
                break;
            case 6:
                Log.d(TAG, "billingClientResponseCodeGenerated: ERROR");
                break;
            case 7:
                Log.d(TAG, "billingClientResponseCodeGenerated: ITEM_ALREADY_OWNED");
                break;
            case 8:
                Log.d(TAG, "billingClientResponseCodeGenerated: ITEM_NOT_OWNED");
                break;
            case -1:
                Log.d(TAG, "billingClientResponseCodeGenerated: SERVICE_DISCONNECTED");
                break;
            case -2:
                Log.d(TAG, "billingClientResponseCodeGenerated: FEATURE_NOT_SUPPORTED");
                break;
            case -3:
                Log.d(TAG, "billingClientResponseCodeGenerated: SERVICE_TIMEOUT");
                break;
            default:
                Log.d(TAG, "billingClientResponseCodeGenerated: Not definitely");
                break;
        }

    }


    public void checkThePurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {

                if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTING){
                    try {
                        Log.d(TAG, "onQueryPurchasesResponse: State Connecting... wait a sec");
                        TimeUnit.SECONDS.sleep(1);
                        checkThePurchases();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (billingClient.getConnectionState() != BillingClient.ConnectionState.CONNECTED){
                    //Если статус not connected то пробуем делать connect.
                    Log.d(TAG, "onQueryPurchasesResponse: Нет соединения, попытка повторного коннекта");
                    executorServiceSubClass.execute(()->{
                        connectToGooglePlayBilling(true);
                    });
                }else {
                    Log.d(TAG, "onQueryPurchasesResponse: Соединение успешно, идём далее.");
                    if (!list.isEmpty()) {
                        Log.d(TAG, "onQueryPurchasesResponse: Лист не пустой");
                        for (Purchase purchase: list){
                            if (purchase.getPurchaseState() == 1){
                                
                                Log.d(TAG, "onQueryPurchasesResponse: Есть элемент со статусом купленно поэтому True");
                                MainActivity.handler.sendEmptyMessage(MainActivity.hSetIsSubscribeTrue);
                            }else {
                                Log.d(TAG, "onQueryPurchasesResponse: Лист не пустой, но ниже строка скажет почему");
                                purchaseStateGeneration(purchase.getPurchaseState());
                                MainActivity.handler.sendEmptyMessage(MainActivity.gethSetIsSubscribeFalse);
                            }
                        }
                    } else {
                        Log.d(TAG, "onQueryPurchasesResponse: Лист пуст поэтому делаем False");
                        MainActivity.handler.sendEmptyMessage(MainActivity.gethSetIsSubscribeFalse);
/*                    MainActivity.isSubscribed = false;
                    SharedPreferences sharedPreferences = getSharedPreferences("Tokens",MODE_PRIVATE);
                    sharedPreferences.edit().clear().apply();*/
                    }

                }
            }
        });

    }
    public void connectToGooglePlayBilling(Boolean check) {
        Log.d(TAG, "connectToGooglePlayBilling: We come in connectToGooglePlayBilling");
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
                                }
                            });

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                if (check){
                    connectToGooglePlayBilling(check);
                }else {
                    connectToGooglePlayBilling(false);
                }
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });

        if (check){
            checkThePurchases();
        }
        Log.d(TAG, "connectToGooglePlayBilling: We go out from connectToGooglePlayBilling");
    }
}
