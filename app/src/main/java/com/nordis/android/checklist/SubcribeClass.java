package com.nordis.android.checklist;

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
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.api.services.androidpublisher.model.Price;
import com.google.api.services.androidpublisher.model.SubscriptionPriceChange;
import com.nordis.android.checklist.databinding.SubscribeLayoutBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SubcribeClass extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "My_SubClass";
    public static String whatSubChosen;
    static Handler handlerForSubscribtionClass;
    static String currentPurchaseToken;
    final int hSubISActivated = 2;
    final int hSubscribtionActiveYet = 4;
    Button btnYeardApplySubscribe, btnMonthApplySubscribe, btnSixMonthApplySubscribe;
    String currentPrice;
    SharedPreferences sPref;
    Activity activity = SubcribeClass.this;
    ExecutorService executorServiceSubClass = Executors.newCachedThreadPool();
    SubscribeLayoutBinding subscribeLayoutBinding;
    private BillingClient billingClient;
    private QueryProductDetailsParams detailsParams;
    private ArrayList<ProductDetails> productDetailsArrayList = new ArrayList<>();
    private ArrayList<String> myProductsList = new ArrayList<>();
    
    SubscriptionPriceChange subscriptionPriceChange = new SubscriptionPriceChange();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscribeLayoutBinding = SubscribeLayoutBinding.inflate(getLayoutInflater());
        setContentView(subscribeLayoutBinding.getRoot());

        myProductsList.add("checklist_app");
        myProductsList.add("checklist_app_six_month");
        myProductsList.add("checklist_app_yeard");
        handlerForSubscribtionClass = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                String gettingPrice = msg.getData().getString("PurchaseHasBeenSold");
                String getException = msg.getData().getString("Exception");
                String getOfferToken = msg.getData().getString("offetTokenIs");
                if (gettingPrice != null) {
                    Toast.makeText(SubcribeClass.this, "Подписка дейсвует за " + gettingPrice, Toast.LENGTH_LONG).show();
                }
                if (getException != null) {
                    Toast.makeText(SubcribeClass.this, "Ошибка " + getException, Toast.LENGTH_LONG).show();

                }
                if (getOfferToken != null){
                    Log.d(TAG, "handleMessage: offerTokenIs: "+ getOfferToken);
                }

                switch (msg.what) {
                    case hSubISActivated:
                        //1:При Подтверждении подписки методом  onAcknowledgePurchaseResponse
                        sPref = getSharedPreferences("Tokens", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        Log.d(TAG, "handleMessage: Удаление старого токена и запись нового");
                        SharedPreferences.Editor editor = sPref.edit();
                        editor.putString("Token", currentPurchaseToken);
                        editor.putString("Period&SubTime", whatSubChosen);
                        editor.apply();
                        Log.d(TAG, "handleMessage:sub begins in  " + whatSubChosen);
                        //подписка Активирована
                        Intent i = new Intent(SubcribeClass.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        Log.d(TAG, "handleMessage: запускаем mainActiviy по новой");
                        startActivity(i);
                        break;
                    case hSubscribtionActiveYet:
                        Toast.makeText(SubcribeClass.this, "Подписка ещё дейсвует", Toast.LENGTH_LONG).show();
                        break;
                }
            }

        };

        subscribeLayoutBinding.btnSubRemoveGotoread.setOnClickListener(this);
        subscribeLayoutBinding.btnSubDetailGotoread.setOnClickListener(this);
        subscribeLayoutBinding.btnClose.setOnClickListener(this);

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

        //SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

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
                                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                        AcknowledgePurchaseParams purchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(currentPurchaseToken).build();
                                        billingClient.acknowledgePurchase(purchaseParams, new AcknowledgePurchaseResponseListener() {
                                            @Override
                                            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                    handlerForSubscribtionClass.sendEmptyMessage(hSubISActivated);
                                                }
                                            }
                                        });
                                    }

                                }


                            }

                        }

                    }
                })
                .enablePendingPurchases()
                .build();

        Log.d(TAG, "initializeBillingClient: Initialize was success!");
    }

    public void connectToGooglePlayBilling(Boolean check) {
        Log.d(TAG, "ConnectToGooglePlayBilling stars...");
        //To connect to Google Play, call startConnection().
        billingClient.startConnection(new BillingClientStateListener() {

            //Establish a connection to Google Play
            //Подключение к Google Play
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.

                    ArrayList<QueryProductDetailsParams.Product> products = new ArrayList<>();

                    for (int i = 0; i < myProductsList.size(); i++) {
                        products.add(QueryProductDetailsParams.Product.newBuilder().setProductId(myProductsList.get(i))
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build());
                    }

                    detailsParams = QueryProductDetailsParams.newBuilder()
                            .setProductList(products)
                            .build();

                    billingClient.queryProductDetailsAsync(
                            detailsParams,
                            new ProductDetailsResponseListener() {
                                public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList) {
                                    for (ProductDetails productDetails : productDetailsList) {
                                        Log.d(TAG, "productDetails: " + productDetails.getName());

                                        ArrayList<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = (ArrayList<ProductDetails.SubscriptionOfferDetails>) productDetails.getSubscriptionOfferDetails();
                                        Log.d(TAG, "subscriptionOfferDetails Size: " + subscriptionOfferDetails.size());

                                        for (int i = 0; i < subscriptionOfferDetails.size(); i++) {
                                            Log.d(TAG, "subscriptionOfferDetails in Cycle: " + subscriptionOfferDetails.get(i).getOfferToken());
                                        }

                                    }

                                    // Process the result
                                    productDetailsArrayList.addAll(productDetailsList);
                                    Log.d(TAG, "onProductDetailsResponse11111111: " + productDetailsArrayList.size());

                                }
                            }
                    );

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                if (check) {
                    connectToGooglePlayBilling(check);
                } else {
                    connectToGooglePlayBilling(false);
                }
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });

        if (check) {
            checkThePurchases();
        }
        Log.d(TAG, "ConnectToGooglePlayBilling end");
    }
    //использовать firebase cloud function as backend server. And firestore as the database.
    @Override
    public void onClick(View v) {
        Intent intent;
        int responseCode;
        if (v.getId() == R.id.btn_Buy_month) {
            initAndCheckPurchase(productDetailsArrayList.get(0));
            Log.d(TAG, "onClick: " + productDetailsArrayList.get(0).getProductId());

        } else if (v.getId() == R.id.btn_buy_sixmonth) {
            initAndCheckPurchase(productDetailsArrayList.get(1));
            Log.d(TAG, "onClick: " + productDetailsArrayList.get(1).getProductId());


        } else if (v.getId() == R.id.btnYeardBuy) {
            initAndCheckPurchase(productDetailsArrayList.get(2));
            Log.d(TAG, "onClick: " + productDetailsArrayList.get(2).getProductId());

        }
        if (subscribeLayoutBinding.btnSubRemoveGotoread.equals(v)) {
            intent = new Intent(SubcribeClass.this, ActivitySubRemove.class);
            startActivity(intent);
        }
        if (subscribeLayoutBinding.btnSubDetailGotoread.equals(v)) {
            intent = new Intent(SubcribeClass.this, SubcribeDetailsActivity.class);
            startActivity(intent);
        }
        if (subscribeLayoutBinding.btnClose.equals(v)) {
            intent = new Intent(SubcribeClass.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

    }

    public Boolean checkConnections() {
        if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTED) {
            Log.d(TAG, "checkConnections: true");
            return true;
        } else {
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

    public void initAndCheckPurchase(ProductDetails productDetails) {

        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                if (!list.isEmpty()) {
                    for (Purchase purchase : list) {
                        connectionStateGeneration(billingClient.getConnectionState());
                        billingClientResponseCodeGenerated(billingResult.getResponseCode());
                        handlerForSubscribtionClass.sendEmptyMessage(hSubscribtionActiveYet);
                        Log.d(TAG, "onQueryPurchasesResponse: подписка есть! " + purchase.toString());
                    }

            }else {
                    Log.d(TAG, "onProductDetailsResponse: подписки нет!");
                    // Retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    // Get the offerToken of the selected offer


                    assert productDetails.
                            getSubscriptionOfferDetails() != null;
                    String offerToken = productDetails.
                            getSubscriptionOfferDetails().get(0)
                            .getOfferToken();

                    Log.d(TAG, "onProductDetailsResponse: offerToken is: " + offerToken);
                    // Set the parameters for the offer that will be presented
                    // in the billing flow creating separate productDetailsParamsList variable

                    ArrayList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
                    productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken).build());
 /*                   ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                            ImmutableList.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(offerToken)
                                            .build()
                            );*/

                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build();

                    // Launch the billing flow
                    billingClient.launchBillingFlow(SubcribeClass.this, billingFlowParams);


                }
        }

        });


    }

    public void checkThePurchases() {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {

                if (billingClient.getConnectionState() == BillingClient.ConnectionState.CONNECTING) {
                    try {
                        Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: State Connecting... wait a sec");
                        TimeUnit.SECONDS.sleep(1);
                        checkThePurchases();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (billingClient.getConnectionState() != BillingClient.ConnectionState.CONNECTED) {
                    //Если статус not connected то пробуем делать connect.
                    Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: Нет соединения, попытка повторного коннекта");
                    executorServiceSubClass.execute(() -> {
                        connectToGooglePlayBilling(true);
                    });
                } else {
                    Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: Соединение успешно, идём далее.");
                    if (!list.isEmpty()) {
                        Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: Лист не пустой");
                        for (Purchase purchase : list) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: Есть элемент со статусом купленно поэтому True");
                                MainActivity.handler.sendEmptyMessage(MainActivity.hSetSubscribeTrue);
                            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                                //This for Pending and Unspecified state.
                                Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: Лист не пустой, но в состоянии ожидания");
                                MainActivity.handler.sendEmptyMessage(MainActivity.hSetSubscribePending);
                            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                                MainActivity.handler.sendEmptyMessage(MainActivity.hSetSubscribeUNSPECIFIED);
                            }
                        }
                    } else {
                        Log.d(TAG, "checkThePurchases onQueryPurchasesResponse: Лист пуст поэтому делаем False");
                        MainActivity.handler.sendEmptyMessage(MainActivity.hSetSubscribeFalse);
                    }

                }
            }
        });

    }


    public static void connectionStateGeneration(int state) {
        switch (state) {
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

    public static void purchaseStateGeneration(int state) {
        switch (state) {
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

    public static void billingClientResponseCodeGenerated(int state) {
        switch (state) {
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
}
