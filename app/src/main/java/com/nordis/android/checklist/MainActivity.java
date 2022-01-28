package com.nordis.android.checklist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.example.android.checklist.R;
import com.example.android.checklist.databinding.ActivityMainBinding;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
//Планы на завтра  Поправить Layout для телефонов для планшетов вроде норм.
//Реализовать фунционал в контекстном меню delete row
// Добавить возможность чтения xls файлов.
// Не забыть включить удаление файла при удалении базы данных. По возможности сделать это изберательно.
//Попробовать сделать огромный список на 600+ элементов, посмотреть как будет вести себя Listview.Будет ли лагать.
        AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
    //Inner DataBase SQL variables
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    //UI - variables
    EditText etName;
    Button btnSave, btnReadFile, btnDeleteAll, btnSearch, btnBackSearch;

    //Arraylist variables
    //Примечание: mainlist и foundlist и foundAccurateList основные.В list_of_View отбражаться всё что есть в mainlist или foundlist с помощью  adapter1.
    ArrayList<String> mainList; //основной лист
    ArrayList<String> found_List = new ArrayList<>();
    ArrayList<String> foundAccurateList = new ArrayList<>();// Используеться в более точном поиске
    ArrayList<String> supportRequestHistoryForChangeStrings = new ArrayList<>();// Для изменения строки
    ArrayList<String> firstWordSearchingList = new ArrayList<>();// Для изменения строки
    ArrayList<String> menuList = new ArrayList<String>();// Для отображения в спинере главного меню.
    ArrayList<String> listFromSharedPreference = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Main
    ArrayList<String> listForSearch = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Search
    volatile ArrayList<String> downloadList = new ArrayList<>();  // - Куда считываеться изначально тексты с файла а потом с него загружаем в Базу
    ArrayList<String> loadhistory = new ArrayList<>(); // Лист работает в история поиска.
    volatile HashSet<String> hashSetMainCollectorItems = new HashSet<>(); // главный подсчёт выделяемых item elements в setOnItemCLick.

    volatile ListView list_of_View;   //Лист куда закидываеться основной или поисковые листы при помощи адаптера (adapter1). Для отображения.
    volatile ArrayAdapter adapter1;   //Главный Адаптер Он закидывает значения с mainList, found_List, foundAccurateList во ViewList тоесть list_of_View.
    ConstraintLayout constraintLayoutManual;
    Thread thread;
    volatile ProgressBar progressBar;


    //String variables
    public static volatile String fileName;
    public static String name = "";
    final String TAG = "Main_Activity";
    String chosenCharset; // получаем значение в OnActivityResult когда выбрали кодировку.
    public static volatile String choosen_ItemInClickmethod = ""; // Выделяемые View преобразуються в String в setOnItemCL. После checked_Items работает с HashSetMainCollectorItems

    //Boolean variables
    static boolean isSubscribed = false;// для того что бы из subscribtion class получить инфу о подписке.
    static volatile boolean bool_fileOfNameReady, bool_fileNotChosen, bool_isSaved,
            bool_deleteFile_checkBox_isActivated, bool_prepereDeleteRow, bool_onSaveReady, bool_xlsColumnsWasChosen,
            bool_xlsExecutorCanceled, bool_haveDeletingRight, bool_billingInitializeOk = false;
    //bool_fileOfNameReady используеться в Загрузке и onRestart и ActivityResult
    //bool_prepereDeleteRow - для контекстной функции Delete row.
    //bool_isSaved - используется в RestCreating. Что бы прога не удалила план пока не завершится сохранение остатка.
    //bool_onSaveReady - для контекстной функции Delete All checked.
    //bool_deleteFile_checkBox_isActivated специальная переменная для удаленя файла на носителе.
    //bool_isSaved переменная служит для сохранения остатка , что бы удаление не произошло раньше чем не сохраниться остаток.

    //Integer variables
    static int batteryLvl;
    int backcounter = 0;  //backcounter - работает с backSearchlist
    final int requestCode1 = 1;
    volatile static int mProgresscounter = 0;
    volatile static int mColumnmax = 0;
    volatile static int mColumnmin = 0;
    static int firstWordCounter = -1; //переменная вторичная
    final int menuSize = 4;

    final int hSetToastErrorOfFileReading = 1;
    final int hsetdelete_WithOut_rest = 3;
    final int hSetbtnReadFileEnabledFalse = 4;
    final int hSetbtnReadFileEnabledTrue = 5;
    final int hSetProgressBarVisible = 6;
    final int hSetProgressBarGone = 7;
    final int hSetTurnManualOn = 8;
    final int hsetdelete_IsCanceled = 9;
    final int hsetlistView_Onvisible = 10;
    final int hSetLoadingListOfView_fromAdapter1 = 11;
    final int hSetToastErrorfromReadingAdditionalLoad = 12;
    final int hSetCreateDialogError = 13;
    final int hSetDoRest = 14;
    final int hSetDeleteRest = 15;
    final int hSetCreateDialogFromWhichToWhich = 16;
    final int hSetDeleteChekedPositions = 17;
    final static int hSetSubscribeTrue = 18;
    final static int hSetSubscribeFalse = 19;
    final static int hBillingClientInitializeIsCorrect = 20;
    final static int hShowAd = 22;
    final static int hSetSubscribePending = 23;
    final static int hSetSubscribeUNSPECIFIED = 24;
    final int hBatteryOn = 25;
    final int hsetLostConnectionsWithGooglePlay = 26;

    static volatile SharedPreferences sPref;
    Executor executor;
    Cursor cursor;
    ContentValues contentValues = new ContentValues();
    static Handler handler;
    final static CountDownLatch countDownLatch = new CountDownLatch(1);
    ActivityMainBinding binding;
    private RewardedAd mRewardedAd;
    DialogClass newdialog;
    LocalDateTime localDateChecked;
    Uri uri; // Получаем нахождение файла в OnActivityResult

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        onAdCreate();
        menuList.add(getString(R.string.manual));
        menuList.add(getString(R.string.charset_determinations));
        menuList.add(getString(R.string.getSubscribe));
        menuList.add(getString(R.string.toSeeAds));
        menuList.add("");
        //final int menuSize = menuList.size()-1;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, menuList) {
            @Override
            public int getCount() {
                return menuSize;// Этот метод влияет на отображение в самом spinner
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.menuSpiner.setAdapter(adapter);
        binding.menuSpiner.setSelection(menuSize);
        binding.menuSpiner.setOnItemSelectedListener(this);

        binding.menuViewBattery.setOnClickListener(this);


        executor = Executors.newCachedThreadPool(); // With this method, the thread lives 60 sec if it done.


        constraintLayoutManual = findViewById(R.id.ID_ConstraintManual);

        list_of_View = findViewById(R.id.list_item_model);
        list_of_View.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mainList = new ArrayList<>();

        /*иницализация кнопок*/
        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        btnReadFile = findViewById(R.id.loadtext);
        btnReadFile.setOnClickListener(this);

        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(this);

        btnBackSearch = findViewById(R.id.IdBackSearch);
        btnBackSearch.setOnClickListener(this);

        /*находим Добавление view*/
        etName = findViewById(R.id.etName);

        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);


        dbHelper = new DBHelper(this);
        viewData();
        registerForContextMenu(list_of_View);
        progressBar = findViewById(R.id.downloadBar);

//Нажатие на Item
        list_of_View.setOnItemClickListener(this);
        onHandlerCreate();
        if (!mainList.isEmpty() || !found_List.isEmpty() || !foundAccurateList.isEmpty()) {
            constraintLayoutManual.setVisibility(View.GONE);
            list_of_View.setVisibility(View.VISIBLE);

        } else {
            list_of_View.setVisibility(View.GONE);
            constraintLayoutManual.setVisibility(View.VISIBLE);
        }
        btnSave.callOnClick();

        checkSub();

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkSub() {
        //Логика дейсвий:
        //1 Проверяеться connection to PlayStore, если ошибка тогда оповещаем клиента что ошибка!
        //2 Если connecting in correct. Then we have check subscription.
        //3 If subscribe is correct: isSubscribe = true , else isSubscribe = false;
        SubcribeClass subcribeClass = new SubcribeClass();
        try {
            Log.d(SubcribeClass.TAG, "From Main: BillingClient Initialization begins... ");
            subcribeClass.initializeBillingClient(this);
            executor.execute(() -> {
                Log.d(SubcribeClass.TAG, "From Main: We launch a new thread and connectToGooglePlayBilling method ");
                subcribeClass.connectToGooglePlayBilling(false);// false так как это обычное подсоединение.
                try {
                    int i = 4;
                    while (!subcribeClass.checkConnections()) {
                        if (i == 0) {
                            throw new InterruptedException();
                        }
                        i--;
                        countDownLatch.await(2, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(hsetLostConnectionsWithGooglePlay);
                    return;
                }
                Log.d(SubcribeClass.TAG, "From Main: we have got connections and checkThePurchases method begins");
                subcribeClass.checkThePurchases();
            });
        } catch (Exception e) {
            //Далее методика Если инициализация не прошла, то идёт проверка на существующий токен, А если есть то сколько он ещё дейсвует.
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.intializegettingFall, Toast.LENGTH_LONG).show();
            Log.d(TAG, "checkSub: error: " + e.getMessage());
        }


    }


    public void onAdCreate() {
        AdRequest adRequest = new AdRequest.Builder().build();


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });

        //ca-app-pub-6564886494367745/7174186976 - my
        //ca-app-pub-3940256099942544/5224354917 - test
        RewardedAd.load(this, "ca-app-pub-6564886494367745/7174186976",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, "Ad was not loaded " + loadAdError.getMessage());
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        Log.d(TAG, "Ad was loaded.");
                    }
                });

    }

    public void onHandlerCreate() {
        handler = new Handler(getMainLooper()) {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void handleMessage(@NonNull Message msg) {
                String s = msg.getData().getString("changeString");
                if (s != null) {
                    Log.d(TAG, "handleMessage: получили строку " + s);
                    prepareTochange(s);
                }
                switch (msg.what) {
                    case 1:
                        Toast.makeText(MainActivity.this, (R.string.File_reading_Error), Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        createRestMemory();
                        handler.post(() -> {
                            while (!bool_isSaved) {
                                SystemClock.sleep(1000);
                                Log.i(TAG, "run: :Ждём пока isSave , будет true " + bool_isSaved);
                            }
                            Log.i(TAG, "isSave = " + bool_isSaved + "      run: пошло удаление");
                            btnDeleteAll.callOnClick();
                        });
                        break;
                    case 3:
                        btnDeleteAll.callOnClick();
                        Toast.makeText(MainActivity.this, R.string.deleted_success, Toast.LENGTH_LONG).show();
                        break;
                    case 4:
                        btnReadFile.setEnabled(false);
                        break;
                    case 5:
                        btnReadFile.setEnabled(true);
                        break;
                    case 6:
                        progressBar.setVisibility(View.VISIBLE);
                        break;
                    case 7:
                        progressBar.setVisibility(View.GONE);
                        break;
                    case 8:
                        constraintLayoutManual.setVisibility(View.VISIBLE);
                        break;
                    case 9:
                        etName.setText("");
                        Toast.makeText(MainActivity.this, getString(R.string.cancel), Toast.LENGTH_LONG).show();
                        break;
                    case 10:
                        list_of_View.setVisibility(View.VISIBLE);
                        break;
                    case 11:
                        list_of_View.setAdapter(adapter1);
                        break;
                    case 12:
                        Toast.makeText(MainActivity.this, R.string.Reading_rest_data_Error, Toast.LENGTH_LONG).show();
                        break;
                    case 13:
                        Toast.makeText(MainActivity.this, R.string.Create_dialog_Error, Toast.LENGTH_LONG).show();
                        break;
                    case 14:
                        createRestMemory();
                        break;
                    case 15:
                        deleteRestMemory();
                        break;
                    case 16:
                        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                        newdialog = new DialogClass(MainActivity.this,
                                null,
                                inflater,
                                null
                        );
                        newdialog.createCustomNewDialogFromWhichTowhich();
                        newdialog.dialog.show();
                        break;
                    case 17:
                        deleteCheckedItems();
                        Toast.makeText(MainActivity.this, R.string.DeleteCheked, Toast.LENGTH_LONG).show();
                        break;
                    case hSetSubscribeTrue:
                        Toast.makeText(MainActivity.this, R.string.subscribe_is_valid, Toast.LENGTH_LONG).show();
                        isSubscribed = true;
                        regSubElements(isSubscribed);
                        localDateChecked = LocalDateTime.now();
                        break;
                    case hSetSubscribeFalse:
                        Toast.makeText(MainActivity.this, R.string.subscribe_out, Toast.LENGTH_LONG).show();
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("BATTERY", MODE_PRIVATE);
                        batteryLvl = sPref.getInt("KeyBatterylvl", 35);
                        binding.menuViewBattery.getBackground().setLevel((batteryLvl * 100));
                        break;
                    case hBillingClientInitializeIsCorrect:
                        bool_billingInitializeOk = true;
                        break;
                    case hShowAd:
                        showAdExecute();
                        break;
                    case hSetSubscribePending:
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("BATTERY", MODE_PRIVATE);
                        batteryLvl = sPref.getInt("KeyBatterylvl", 35);
                        binding.menuViewBattery.getBackground().setLevel((batteryLvl * 100));

                        newdialog = new DialogClass(MainActivity.this,
                                getString(R.string.pending),
                                getString(R.string.pendingtitle),
                                getString(R.string.understand),
                                null, null);
                        newdialog.createDialogPendingState();
                        newdialog.dialog.show();
                        break;
                    case hBatteryOn:
                        regSubElements(false);
                        break;
                    case hSetSubscribeUNSPECIFIED:
                        Toast.makeText(MainActivity.this, R.string.unspecified, Toast.LENGTH_LONG).show();
                        break;
                    case hsetLostConnectionsWithGooglePlay:
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("BATTERY", MODE_PRIVATE);
                        batteryLvl = sPref.getInt("KeyBatterylvl", 35);
                        binding.menuViewBattery.getBackground().setLevel((batteryLvl * 100));

                        newdialog = new DialogClass(MainActivity.this,
                                getString(R.string.lostConnection),
                                getString(R.string.connectionIssue),
                                getString(R.string.understand),
                                null, null);
                        newdialog.createDialogPendingState();
                        newdialog.dialog.show();
                        break;


                }
            }
        };
    }

    public void regSubElements(Boolean subcribtionY) {
        if (subcribtionY) {
            binding.menuViewBattery.setVisibility(View.GONE);
            binding.idsubscriptionText.setVisibility(View.VISIBLE);

        } else {
            binding.idsubscriptionText.setVisibility(View.GONE);
            binding.menuViewBattery.setVisibility(View.VISIBLE);
        }
    }

    public void showAdExecute() {
        if (mRewardedAd != null) {
            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad was shown.");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when ad fails to show.
                    Log.d(TAG, "Ad failed to show.");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad was dismissed.");
                    mRewardedAd = null;
                }
            });
            Activity activityContext = MainActivity.this;
            mRewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();
                    //получаем количество очков и загружаем в батарею и сохраняем в sPref
                    batteryLvl += rewardAmount;
                    if (batteryLvl > 100) {
                        batteryLvl = 100;
                    }

                    sPref = getSharedPreferences("BATTERY", MODE_PRIVATE);
                    sPref.edit().putInt("KeyBatterylvl", batteryLvl).apply();

                    binding.menuViewBattery.getBackground().setLevel((batteryLvl * 100));
                    Log.d(TAG, "onUserEarnedReward!!!!!!!!!: rewardAmount - " + rewardAmount);

                }
            });
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
        }
    }

    public void prepareTochange(String changeAbleString) {
        Log.i(TAG, "prepareTochange: перешли в метод изменения строки");
        sqLiteDatabase = dbHelper.getWritableDatabase();

        contentValues.put(DBHelper.KEY_NAME, changeAbleString);
        if (!found_List.isEmpty() || !foundAccurateList.isEmpty()) {    //Если Пойсковый лист не пустой то данные заменяються и запускаеться обновление
            sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});
            Log.i(TAG, "prepareTochange: Изменение с сторией поиска");
            btnSave.callOnClick();
            try {
                ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
                for (int i = 0; i < loadhistory.size(); i++) {
                    etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                    btnSearch.callOnClick();
                }
                loadhistory.clear();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

        } else {
            Log.i(TAG, "prepareTochange: простое изменение");
            sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});
            btnSave.callOnClick();
        }
        dbHelper.close();

    }

    public void prepareToDelete(String becomeDeleteString) {
        sqLiteDatabase = dbHelper.getWritableDatabase();
        if (!found_List.isEmpty() || !foundAccurateList.isEmpty()) {
            sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_NAME + "= ?", new String[]{becomeDeleteString});
            btnSave.callOnClick();
            try {
                ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
                for (int i = 0; i < loadhistory.size(); i++) {
                    etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                    btnSearch.callOnClick();
                }
                loadhistory.clear();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            try {
                sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_NAME + "= ?", new String[]{becomeDeleteString});
                btnSave.callOnClick();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.Delete_Error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        choosen_ItemInClickmethod = null;
        dbHelper.close();

    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        //int green = getResources().getColor(valmis);
        choosen_ItemInClickmethod = ((TextView) view).getText().toString(); // Кликнутая строка в данный момент
        Thread thread = new Thread(this::checkedItemsReloadInfo);
        thread.start();
        //checkedItemsReloadInfo();

    }

    public void checkedItemsReloadInfo() {
        if (hashSetMainCollectorItems.contains(choosen_ItemInClickmethod)) { // Основной Список Выбранных Элементов
            hashSetMainCollectorItems.remove(choosen_ItemInClickmethod);
        } else {
            hashSetMainCollectorItems.add(choosen_ItemInClickmethod);
        }

        /*Save stats in sharedPreferences*/

        ArrayList<String> gap = new ArrayList<>(hashSetMainCollectorItems);
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        sPref.edit().clear().apply();                             // чистим SharedPreferences

        for (int i = 0; i < hashSetMainCollectorItems.size(); i++) {
            editor.putString("Keyg" + i, gap.get(i));
        }
        editor.putInt("Kolichesvo", hashSetMainCollectorItems.size());// Заливаем новые данные в SharedPreferences
        editor.apply();
        gap.clear();
        bool_prepereDeleteRow = true;
    }

    private void viewDataForDownloading() {
        cursor = dbHelper.viewData(); // Курсор в данном этапе дейсвует как список в котором храняться все строки с Базы данных
        if (cursor.getCount() == 0) {
            Toast.makeText(MainActivity.this, getString(R.string.Not_data_to_show) + cursor.getInt(3), Toast.LENGTH_LONG).show();
        } else {
            while (cursor.moveToNext()) { // тут мы его считываем
                mainList.add(cursor.getString(0));
                mProgresscounter++;
                if (progressBar.getMax() <= 40) {// В зависимости от объёма данных увеличиваем скорость загрузки
                    try {
                        TimeUnit.MILLISECONDS.sleep(70); //0.7 - 2.8 sec
                        //Log.i(TAG, "Sleep 50");
                        handler.post(runnableIncrementProgressbar);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (progressBar.getMax() > 40 && progressBar.getMax() <= 100) { //1.4 - 3.5 sec
                    try {
                        TimeUnit.MILLISECONDS.sleep(35);
                        //Log.i(TAG, "Sleep 35");
                        if (mProgresscounter % 4 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (progressBar.getMax() > 100 && progressBar.getMax() <= 350) { //1.3 - 4.5 sec
                    try {
                        TimeUnit.MILLISECONDS.sleep(13);
                        //Log.i(TAG, "Sleep 30");
                        if (mProgresscounter % 5 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (progressBar.getMax() > 350 && progressBar.getMax() <= 600) { // 2.4 - 4.2
                    try {
                        TimeUnit.MILLISECONDS.sleep(7);
                        //Log.i(TAG, "Sleep 12");
                        if (mProgresscounter % 8 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (progressBar.getMax() > 600) { // 3 sec
                    try {
                        TimeUnit.MILLISECONDS.sleep(5);
                        //Log.i(TAG, "Sleep 5");
                        if (mProgresscounter % 10 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
            adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, mainList);
            handler.sendEmptyMessage(hSetLoadingListOfView_fromAdapter1);
            cursor.close();
            chosenCharset = null;
            uri = null;

        }
    }

    //вывод в ListView отображения
    private void viewData() {
        cursor = dbHelper.viewData(); // Курсор в данном этапе дейсвует как список в котором храняться все строки с Базы данных
        if (cursor.getCount() == 0) {
            Toast.makeText(this, getString(R.string.Not_data_to_show), Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) { // тут мы его считываем
                mainList.add(cursor.getString(0));

            }
            adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, mainList);
            list_of_View.setAdapter(adapter1);
            cursor.close();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Сюда приходим из Загрузки, и ищём наш файл на устройстве. А так же инициализируем его имя.
        if (requestCode == requestCode1 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            uri = data.getData();
            fileName = BrowseTheFile.getRealPath(MainActivity.this, uri);
            //String path23 = BrowseTheFile.getRealPath(this, uri);
            Log.i(TAG, "onActivityResult: data String: " + fileName);
            String[] massiveString = fileName.split("/");
            fileName = massiveString[massiveString.length - 1];
            Log.i(TAG, "onActivityResult: data String: " + massiveString[massiveString.length - 1]);
            bool_fileOfNameReady = true;

            sPref = getSharedPreferences("FILENAME", MODE_PRIVATE);
            SharedPreferences.Editor editor1 = sPref.edit();
            editor1.putString("keyFileName", fileName);
            editor1.apply();

        }
        if (requestCode == 2 && resultCode == RESULT_OK) {
            if (data == null) {
                return;
            }
            chosenCharset = data.getStringExtra("nameOfCharset");
            Log.d(TAG, "onActivityResult: Данные полученны: " + data.getStringExtra("nameOfCharset"));
        }
        if (requestCode == 2 && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "onActivityResult: Была отмена!");
        }

    }


    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        name = etName.getText().toString(); //Сохранение в базу шаг 1
        sqLiteDatabase = dbHelper.getWritableDatabase();
        //ContentValues contentValues = new ContentValues(); //шаг 2
        switch (v.getId()) {
//Обновить
            case R.id.btnSave:
                Log.d(TAG, "onClick: Status subscribe : " + isSubscribed);
                if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                    int kol = sPref.getInt("Kolichesvo", 0);
                    if (kol > 0) {
                        viewData();
                        loadCheckedItems(mainList);
                        if (hashSetMainCollectorItems.isEmpty()) {
                            hashSetMainCollectorItems.addAll(listFromSharedPreference); // сохранённые значения которые были актуальны в момент нажатия обновить, передаём hset.
                        }
                        choosen_ItemInClickmethod = "";
                        showListIsReadyPercent();
                    } else
                        Toast.makeText(MainActivity.this, R.string.First_download_the_file, Toast.LENGTH_SHORT).show();
                    break;
                }
                bool_onSaveReady = false;
                //contentValues.put(DBHelper.KEY_NAME, hideName); //шаг 3
                if (etName.length() == 0 || bool_haveDeletingRight) {
                    //Если все значения были пустыми то чистим все листы и обновляем основной с проставлением checked
                    found_List.clear();
                    foundAccurateList.clear();
                    mainList.clear();
                    hashSetMainCollectorItems.clear();
                    viewData(); // - В этом методе востанавливаться mainList

                    loadCheckedItems(mainList); // Загрузка

                    if (hashSetMainCollectorItems.isEmpty()) {
                        hashSetMainCollectorItems.addAll(listFromSharedPreference); // сохранённые значения которые были актуальны в момент нажатия обновить, передаём hset.
                    }

                    showListIsReadyPercent();
                    choosen_ItemInClickmethod = "";
                    bool_onSaveReady = true;
                } else {
                    etName.setText("");
                    Toast.makeText(this, R.string.clear_object, Toast.LENGTH_SHORT).show();
                }
                break;

//Удаление
            case R.id.btnDeleteAll:
                if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.First_download_the_file), Toast.LENGTH_SHORT).show();
                    break;
                }
                Log.i(TAG, "onClick: Была нажата кнопка удалить");
                String del = etName.getText().toString();
                String delete = "Delete";
                if (del.equals(delete)) { // Если Ввёл в строку Delete
                    try {
                        if (bool_deleteFile_checkBox_isActivated) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                        sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, null, null);
                        File file = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser");
                        File file2 = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser-journal");
                        Log.i(TAG, "onClick: Попытка удалить файлы Data base. Файл сущестует?" + file.exists());
                        file.delete();
                        file2.delete(); //data/data/com.example.android.sqlitekod_dev_test/shared_prefs/SAVE.xml
                        Log.i(TAG, "onClick: После удаления Data base. Файл сущестует?" + file.exists());

                    } catch (Exception e) {
                        Log.d(TAG, "Попытка удалить внутренние файлы DBNeiser и DBNeiser-journal " + e.getMessage());
                        e.printStackTrace();
                    }

                    try {
                        Log.i(TAG, "onClick: Зашли в чистку всех елементов");
                        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                        Log.i(TAG, "onClick: sPref:" + sPref);
                        sPref.edit().clear().apply();
                        Log.i(TAG, "onClick: sPref после удалениея:" + sPref);
                        cursor.close();
                        listFromSharedPreference.clear();
                        listForSearch.clear();
                        list_of_View.setAdapter(null);
                        mainList.clear();
                        found_List.clear();
                        downloadList.clear();
                        foundAccurateList.clear();
                        hashSetMainCollectorItems.clear();
                        choosen_ItemInClickmethod = null;
                        adapter1.clear();
                        backcounter = 0;
                        mColumnmax = 0;
                        mColumnmin = 0;
                        name = null;
                        etName.setText("");
                        bool_haveDeletingRight = false;
                        constraintLayoutManual.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(MainActivity.this, R.string.deleted_successfully, Toast.LENGTH_SHORT).show();
                } else {
                    bool_haveDeletingRight = true;
                    LayoutInflater inflater = this.getLayoutInflater();
                    DialogClass dialogClass = new DialogClass(MainActivity.this,
                            null,
                            inflater,
                            null
                    );
                    dialogClass.createCustomNewDialogDeleteFile();
                    dialogClass.dialog.show();
                    etName.setText("Delete");
                }
                break;
//Загрузка
            case R.id.loadtext:
                if (!bool_fileOfNameReady) {
                    bool_fileNotChosen = false;
                }
                if (!mainList.isEmpty() || !found_List.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.list_is_already_full, Toast.LENGTH_LONG).show();

                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    setResult(RESULT_OK, intent);
                    startActivityForResult(intent, requestCode1);
                    Thread t = new Thread(() -> {
                        while (!bool_fileOfNameReady || Thread.currentThread().isInterrupted()) {
                            if (bool_fileNotChosen) {
                                //if функция служит если файл не был выбран, включаться автоматом в onRestart
                                break;
                            }
                            try {
                                Log.i(TAG, "run: Зашли в режим сна, fileOfnameReady:  " + bool_fileOfNameReady);
                                TimeUnit.SECONDS.sleep(3);
                                //Thread.sleep(3300);
                            } catch (InterruptedException e) {
                                Log.i(TAG, "run: ошибка в новой нити: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        if (!bool_fileNotChosen && bool_fileOfNameReady) { // если файл
                            Log.i(TAG, "run: Получили имя файла идём в загрузку,fileOfNameReady: " + bool_fileOfNameReady);
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        } else {
                            handler.sendEmptyMessage(hSetToastErrorOfFileReading);
                        }
                    });
                    t.start();
                }
                break;
//поиск
            case R.id.btnSearch:
                //Если все листы пусты то...
                if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.First_download_the_file), Toast.LENGTH_SHORT).show();
                    return;
                }
                //Если поисковый запрос не был написан, поле пустое то.
                if (name == null || name.length() == 0) {
                    Toast.makeText(this, R.string.write_something_in_the_place, Toast.LENGTH_LONG).show();
                } else {
                    if (!isSubscribed) {
                        if (batteryLvl == 0) {
                            Toast.makeText(this, getString(R.string.checkBatteryLoading), Toast.LENGTH_LONG).show();
                        } else {
                            String searchWord = etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                            onmyQueryTextSubmit(searchWord);
                        }
                    } else {

                        if (LocalDateTime.now().isAfter(localDateChecked.plusDays(1))) {
                            Log.d(TAG, "onClick: проходим дополнительную проверку.");
                            checkSub();
                        }
                        String searchWord = etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                        onmyQueryTextSubmit(searchWord);
                    }
                }
                break;

//История поиска
            case R.id.IdBackSearch:
                if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.First_download_the_file), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (supportRequestHistoryForChangeStrings.size() >= 2) {
                    Log.d(TAG, "onClick: зашли в историю поиска пунк 1");
                    try {
                        loadhistory.addAll(supportRequestHistoryForChangeStrings);
                        btnSave.callOnClick();
                        String howDeepWeGo = "";
                        // supportRequestHistoryForChangeStrings чистить не нужно так как чистица автоматом в поиске.
                        for (int i = 0; i < loadhistory.size() - 1; i++) {
                            if (i == loadhistory.size() - 2) {
                                howDeepWeGo = howDeepWeGo + loadhistory.get(i);
                                Log.d(TAG, "onClick: №1 " + howDeepWeGo);
                            } else {
                                howDeepWeGo = howDeepWeGo + loadhistory.get(i);
                                howDeepWeGo = howDeepWeGo + "->";
                                Log.d(TAG, "onClick: №2 " + howDeepWeGo);
                            }

                            etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                            btnSearch.callOnClick();
                        }
                        loadhistory.clear();
                        Log.d(TAG, "onClick: вызываем тост");
                        Toast.makeText(this, howDeepWeGo, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else if (!firstWordSearchingList.isEmpty()) {
                    //firstWordCounter = firstWordSearchingList.size()-1;
                    btnSave.callOnClick();
                    if (firstWordCounter <= -1)
                        firstWordCounter = firstWordSearchingList.size() - 1;
                    Log.i(TAG, "onClick: firstWordSearchingList.size = " + firstWordSearchingList.size());
                    Log.i(TAG, "onClick: firstWordCounter = " + firstWordCounter);
                    etName.setText(firstWordSearchingList.get(firstWordCounter));
                    firstWordCounter--;
                    btnSearch.callOnClick();
                } else {
                    Log.d(TAG, "onClick: зашли в историю поиска пунк 2");
                    btnSave.callOnClick();
                }
                break;


        }
        if (binding.menuViewBattery.equals(v)) {
            Toast.makeText(this, binding.menuViewBattery.getTransitionName(), Toast.LENGTH_SHORT).show();
        }
        dbHelper.close();

    }

    public void createRestMemory() {
        //mainList.clear();
        //viewData(); // тут мы востанавливаем main
        btnSave.callOnClick();
        restCreating(); // тут мы сравниваем разность main и hashSetMain
    }

    public void createUncheckedViewList() {
        if (!mainList.isEmpty()) {
            createUncheckedViewListSupport(mainList);//В методе поддержка вычисляется с какого листа будет происходить показ Unchecked items/
        } else if (!found_List.isEmpty()) {
            createUncheckedViewListSupport(found_List);
        } else if (!foundAccurateList.isEmpty()) {
            createUncheckedViewListSupport(foundAccurateList);
        }
    }

    public void createUncheckedViewListSupport(ArrayList<String> list) {
        ArrayList<String> jaak = new ArrayList<>(hashSetMainCollectorItems);
        for (int i = 0; i < jaak.size(); i++) {
            for (int g = 0; g < list.size(); g++) {
                if (jaak.get(i).equals(list.get(g))) {
                    list.remove(list.get(g));// удаляем с основного листа все item которые были checked
                }
            }
        }
        adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, list);
        list_of_View.setAdapter(adapter1);
    }


    private void restCreating() {
        //В этом методе main лист становиться меньше поэтому мы должны его востанавливать. что бы сново применять этот метод. Что бы работал корректно.
        sPref = getSharedPreferences("Jaak", MODE_PRIVATE);// удаляем старую версию остатка
        sPref.edit().clear().apply();
        // От сюда востанавливаем наш main


        ArrayList<String> dublicateMainList = new ArrayList<>(mainList);
        ArrayList<String> jaakList = new ArrayList<>(hashSetMainCollectorItems);
        for (int i = 0; i < jaakList.size(); i++) {
            for (int g = 0; g < dublicateMainList.size(); g++) {
                if (jaakList.get(i).equals(dublicateMainList.get(g))) {
                    dublicateMainList.remove(dublicateMainList.get(g));// удаляем с основного листа все item которые были checked
                }
            }
        }
        sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
        SharedPreferences.Editor editor1 = sPref.edit();
        for (int i = 0; i < dublicateMainList.size(); i++) {
            editor1.putString("Rest" + i, dublicateMainList.get(i));

        }
        editor1.putInt("Kol-jaak", dublicateMainList.size());
        editor1.apply();
        jaakList.clear();
        dublicateMainList.clear();
        bool_isSaved = true;
    }

    @Override //после получения доступа Загружаем базу данных
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult: Переменная для удаления файла bool_deleteFile_checkBox_isActivated: " + bool_deleteFile_checkBox_isActivated);
        if (bool_deleteFile_checkBox_isActivated) { // bool_deleteFile_checkBox_isActivated специальная переменная для удаления файла на носителе.
            Log.i(TAG, "onRequestPermissionsResult:  Зашли в метод удаления файла.");

            sPref = getSharedPreferences("FILENAME", MODE_PRIVATE); // получаем имя файла, которое сохранили при загрузке
            fileName = sPref.getString("keyFileName", "");

            try {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = pInfo.versionName;
                Log.i(TAG, "onRequestPermissionsResult: versionName: " + version);
                if (version.equals("1.0")) {
                    fileName = "Plan.txt";
                }
            } catch (PackageManager.NameNotFoundException e) {
                Toast.makeText(MainActivity.this, R.string.file_version_error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            if (requestCode == 1) { // подготовка к удалению файла
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(sdCard, fileName);

                        if (file.delete()) {
                            Log.d(TAG, "onRequestPermissionsResult:  deleted");
                            Toast.makeText(MainActivity.this, getString(R.string.file_deleted_successfully), Toast.LENGTH_SHORT).show();
                            sPref.edit().clear().apply();
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.file_not_found) + cursor.getInt(3), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "onRequestPermissionsResult:  file doesn't deleted");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "onRequestPermissionsResult: " + e.getMessage());
                        Toast.makeText(MainActivity.this, R.string.file_deleting_error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
        }//Логика удаления файла на носителе закончена.
        //далее начинаеться логика загрузки файла
        else if (!bool_deleteFile_checkBox_isActivated) {
            constraintLayoutManual.setVisibility(View.GONE);
            Log.i(TAG, "onRequestPermissionsResult: Загрузка файла : " + fileName);
            if (fileName == null) {
                return;
            }
            thread = new Thread(() -> {
                Log.i(TAG, "run: Поток закачки был запущен");
                //if (fileName.substring(fileName.length() - 3, fileName.length()).equals("txt")) {
                if (fileName.contains(".txt")) {
                    Log.i(TAG, "run: File был распознан как txt");
                    if (requestCode == 1) {
                        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            if (!mainList.isEmpty()) {
                                Toast.makeText(MainActivity.this, R.string.data_base_is_full, Toast.LENGTH_SHORT).show();
                            } else {
                                handler.sendEmptyMessage(hSetbtnReadFileEnabledFalse);
                                handler.sendEmptyMessage(hSetProgressBarVisible);
                                loadingRest();
                                mainloading();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                        }
                    }
                    handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                    handler.sendEmptyMessage(hSetProgressBarGone);
                    handler.sendEmptyMessage(hsetlistView_Onvisible);
                    //} else if (fileName.substring(fileName.length() - 3, fileName.length()).equals("xls")) {
                } else if (fileName.contains(".xls")) {
                    Log.i(TAG, "run: File был распознан как xls");
                    try {
                        handler.sendEmptyMessage(hSetbtnReadFileEnabledFalse);
                        handler.sendEmptyMessage(hSetProgressBarVisible); // Диактивируем кнопку и Активируем прогресс бар

                        Log.i(TAG, "run: Передаём имя файла, файлу который считывает");

                        File_XLS_Reader file_xls_reader = new File_XLS_Reader(fileName); // передаём имя файла в наш Класс, который читает его.
                        downloadList = file_xls_reader.readingXLS();
                        Log.i(TAG, "run: Получили считанный лист. Его размер: " + downloadList.size());
                        loadingRest(); // запускаем метод что бы получить остаток если он есть + получить переменную mProgresscounter.

                        int j = downloadList.size() + mProgresscounter;
                        progressBar.setMax(j); // так как переменная volantile все изменения будут видны в любом потоке.
                        Log.i(TAG, "mainloading: progress bar MAX = " + j);

                        for (int i = 0; i < downloadList.size(); i++) {
                            //начал добавлять сразу в базу что бы не нагружать main.
                            dbHelper.insertData(downloadList.get(i));
                        }
                        viewDataForDownloading(); //образуем показ Загрузки и списка после того как он полностью загрузиться в Базу
                        downloadList.clear();
                        //Востанавливаем кнопку и убирает прогресс бар.
                        handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                        handler.sendEmptyMessage(hSetProgressBarGone);
                        handler.sendEmptyMessage(hsetlistView_Onvisible);
                        mProgresscounter = 0;

                    } catch (Exception e) {
                        Log.i(TAG, "onRequestPermissionsResult: Поток был прерван в main");
                        handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                        handler.sendEmptyMessage(hSetProgressBarGone);
                        handler.sendEmptyMessage(hSetTurnManualOn);
                        mProgresscounter = 0;
                        bool_xlsExecutorCanceled = false;
                        bool_xlsColumnsWasChosen = false;
                        e.printStackTrace();
                    }
                }
            });
            thread.start();

        }
        bool_deleteFile_checkBox_isActivated = false;//для удаления файла на устройстве
        progressBar.setProgress(0);
        mProgresscounter = 0;
        bool_fileOfNameReady = false;
    }

    Runnable runnableIncrementProgressbar = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "run: зашли в в увеличение progressbar mProgresscounter = " + mProgresscounter);
            progressBar.setProgress(mProgresscounter);

        }
    };


    private void mainloading() {
        try {
            // Open a specific media item using ParcelFileDescriptor.
            ContentResolver resolver = getApplicationContext().getContentResolver();
            String readOnlyMode = "r";
            ParcelFileDescriptor parcelFile = resolver.openFileDescriptor(uri, readOnlyMode);

            BufferedReader reader = null;
            FileReader fileReader = null;
            FileInputStream fileInputStream = null;
            InputStreamReader isr = null;

            if (chosenCharset != null && !chosenCharset.isEmpty()) {
                fileInputStream = new FileInputStream(parcelFile.getFileDescriptor());
                isr = new InputStreamReader(fileInputStream, chosenCharset);
                reader = new BufferedReader(isr);

            } else {
                fileReader = new FileReader(parcelFile.getFileDescriptor());
                reader = new BufferedReader(fileReader);
            }

            // Начало основной загрузки
            String line;
            int j = 1;
            downloadList.add(fileName.substring(0, fileName.length() - 4));

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty() || line.equals(" ")) { // если строка пустая , то пропускаем её.
                    continue;
                }
                if (downloadList.contains(line)) {
                    downloadList.add(line + j);
                    j++;
                } else {
                    downloadList.add(line);
                }
            }
            j = (downloadList.size() + mProgresscounter);
            mProgresscounter = 0;
            progressBar.setMax(j); // так как переменная volantile все изменения будут видны в любом потоке.
            Log.i(TAG, "mainloading: progress bar MAX = " + j);
            for (int i = 0; i < downloadList.size(); i++) {
                //начал добавлять сразу в базу что бы не нагружать main.
                dbHelper.insertData(downloadList.get(i));
            }

            if (isr != null) {
                isr.close();
                fileInputStream.close();
            }
            if (fileReader != null) {
                reader.close();
                fileReader.close();
            }
            viewDataForDownloading(); //образуем показ Загрузки и списка после того как он полностью загрузиться в Базу
            downloadList.clear();
            handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
            handler.sendEmptyMessage(hSetProgressBarGone);
        } catch (IOException e) {
            // Если ошибка, то все востанавливаем кнопки. и убираем видимость progressBar.
            handler.sendEmptyMessage(hSetToastErrorOfFileReading);
            handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
            handler.sendEmptyMessage(hSetProgressBarGone);
            e.printStackTrace();
        }

    }

    private void loadingRest() {
        try {
            // 1 - считываем всё что было сохраненно
            sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
            Log.i(TAG, "loadingRest: sPref = " + sPref);
            int luk = sPref.getInt("Kol-jaak", 0);
            if (luk > 0) {
                for (int i = 0; i < luk; i++) {
                    String s = sPref.getString("Rest" + i, "");
                    if (s.contains(" - *")) {
                        downloadList.add(s);
                    } else downloadList.add(s + " - *");
                }
                sPref.edit().clear().apply(); // Чистим наше сохранения

                for (int i = 0; i < downloadList.size(); i++) {
                    dbHelper.insertData(downloadList.get(i));// загружаем в базу всё что считали
                }
                mProgresscounter = downloadList.size();
                downloadList.clear(); // Чистим лист, он будет нужен в считывании main list.
                Log.i(TAG, "Считал данные с доп загрузки");
            } else sPref = null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Ошибка при чтении доп-загрузки");
            handler.sendEmptyMessage(hSetToastErrorfromReadingAdditionalLoad);

        }

    }

    /*Создаём меню и регистрируем там поиск*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menuxml, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuID_USER_GUIDE) {
            Intent intent = new Intent(MainActivity.this, UserGuideActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.menuID_ToSubscribe) {
            Intent intent = new Intent(MainActivity.this, SubcribeClass.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menuID_Determination_Charset) {
            Intent intent = new Intent(MainActivity.this, EncodingActivity.class);
            startActivityForResult(intent, 2);
        }

        return super.onOptionsItemSelected(item);
    }


    public void showListIsReadyPercent() {
        try {
            double countMain = mainList.size();
            double countReady = hashSetMainCollectorItems.size();
            double persent = countReady * 100 / countMain;
            String persentString = String.valueOf(persent);
            Toast.makeText(MainActivity.this, persentString.substring(0, 4) + "% " + getString(R.string.done), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "0.0 % " + getString(R.string.done), Toast.LENGTH_LONG).show();
        }
    }


    public void deleteRestMemory() {
        Log.i(TAG, "deleteRestMemory: зашли удалить Остаток");
        try {
            sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
            sPref.edit().clear().apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list_item_model) {
            getMenuInflater().inflate(R.menu.menuitem, menu);
        }

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position1 = info.position; // находи позицию

        int itemId = item.getItemId();
//Изменить строку
        if (itemId == R.id.menuitemChageRow) {
            choosen_ItemInClickmethod = list_of_View.getItemAtPosition(position1).toString();
            //Находим строку и присваеваем её к переменной , которая взаимодейсвует с методом checkedItemsReloadInfo();
            if (list_of_View.isItemChecked(position1)) {
                list_of_View.setItemChecked(position1, false);
                checkedItemsReloadInfo();
            }
            //Создаём диалог
            LayoutInflater inflater = this.getLayoutInflater();
            DialogClass dialogClass = new DialogClass(MainActivity.this,
                    "Lets change row",
                    inflater,
                    choosen_ItemInClickmethod
            );
            dialogClass.createCustomNewDialogChageItem();
            dialogClass.dialog.show();
        }
//Удаляем строку
        else if (itemId == R.id.menuitemDeleteRow) {
            bool_prepereDeleteRow = false;
            choosen_ItemInClickmethod = list_of_View.getItemAtPosition(position1).toString();
            //Находим строку и присваеваем её к переменной , которая взаимодейсвует с методом checkedItemsReloadInfo();
            if (list_of_View.isItemChecked(position1)) {
                list_of_View.setItemChecked(position1, false);
                checkedItemsReloadInfo();// в конце метода checkedItemsReloadInfo(), bool_prepereDeleteRow должна стать true.
            } else {
                bool_prepereDeleteRow = true;
            }
            Thread thread = new Thread(() -> {
                while (!bool_prepereDeleteRow) { //Если до сих пор false то спим.
                    try {
                        Log.i(TAG, "run: Данные не подготовленны, спим");
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.post(runnableToDelete);

            });
            thread.start();
            // handler запуститься через пол секунды
            //handler.postAtTime(runnableToDelete,(SystemClock.uptimeMillis()+300));
        }
//Выводим на экран неотмеченные items
        else if (itemId == R.id.menuUncheckedItems) {
            createUncheckedViewList();
        }
//Отметить все элементы в листе
        else if (itemId == R.id.menuCheckAllPositions) { // Меню отмечаем все елементы.
            if (!mainList.isEmpty()) {
                btnSave.callOnClick();
                sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                SharedPreferences.Editor editor = sPref.edit();
                sPref.edit().clear().apply();

                hashSetMainCollectorItems.clear();
                hashSetMainCollectorItems.addAll(mainList);
                ArrayList<String> gap = new ArrayList<>(hashSetMainCollectorItems);
                for (int i = 0; i < hashSetMainCollectorItems.size(); i++) {
                    editor.putString("Keyg" + i, gap.get(i));
                }
                editor.putInt("Kolichesvo", hashSetMainCollectorItems.size());// Заливаем новые данные в SharedPreferences
                editor.apply();
                gap.clear();
                hashSetMainCollectorItems.clear();// Чистим его тут что бы он обновился с новыми данными в btnSave.
                btnSave.callOnClick();
            } else if (!found_List.isEmpty()) {
                getAllListItemsIsCheched(found_List);
            } else if (!foundAccurateList.isEmpty()) {
                getAllListItemsIsCheched(foundAccurateList);
            }

        } else if (itemId == R.id.menuUnCheckAllPositions) {
            if (!mainList.isEmpty()) {
                btnSave.callOnClick();
                sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                sPref.edit().clear().apply();
                hashSetMainCollectorItems.clear();
                btnSave.callOnClick();
            } else if (!found_List.isEmpty()) {
                getAllListItemsIsUnCheched(found_List);
            } else if (!foundAccurateList.isEmpty()) {
                getAllListItemsIsUnCheched(foundAccurateList);
            }

        }

        return super.onContextItemSelected(item);
    }

    public void getAllListItemsIsUnCheched(ArrayList<String> arrayList) {
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        sPref.edit().clear().apply();

        hashSetMainCollectorItems.removeAll(arrayList);
        ArrayList<String> gap = new ArrayList<>(hashSetMainCollectorItems);
        for (int i = 0; i < hashSetMainCollectorItems.size(); i++) {
            editor.putString("Keyg" + i, gap.get(i));
        }
        editor.putInt("Kolichesvo", hashSetMainCollectorItems.size());// Заливаем новые данные в SharedPreferences
        editor.apply();
        gap.clear();
        hashSetMainCollectorItems.clear();// Чистим его тут что бы он обновился с новыми данными в btnSave.
        btnSave.callOnClick();
        try {
            ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
            for (int i = 0; i < loadhistory.size(); i++) {
                etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                btnSearch.callOnClick();
            }
            loadhistory.clear();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }


    public void getAllListItemsIsCheched(ArrayList<String> arrayList) {
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        sPref.edit().clear().apply();

        hashSetMainCollectorItems.addAll(arrayList);
        ArrayList<String> gap = new ArrayList<>(hashSetMainCollectorItems);
        for (int i = 0; i < hashSetMainCollectorItems.size(); i++) {
            editor.putString("Keyg" + i, gap.get(i));
        }
        editor.putInt("Kolichesvo", hashSetMainCollectorItems.size());// Заливаем новые данные в SharedPreferences
        editor.apply();
        gap.clear();
        hashSetMainCollectorItems.clear();// Чистим его тут что бы он обновился с новыми данными в btnSave.
        btnSave.callOnClick();
        try {
            ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
            for (int i = 0; i < loadhistory.size(); i++) {
                etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                btnSearch.callOnClick();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }

    public void deleteCheckedItems() {
               /*  sparseBooleanArray.keyAt(i));//получение позиции
             list_of_View.getItemAtPosition( sparseBooleanArray.keyAt(i))); // получение имени по позиции.
            */
        bool_haveDeletingRight = false;
        btnSave.callOnClick();
        if (hashSetMainCollectorItems.size() == mainList.size()) {
            etName.setText("Delete");
            btnDeleteAll.callOnClick();
        }
        sqLiteDatabase = dbHelper.getWritableDatabase();
        SparseBooleanArray sparseBooleanArray = list_of_View.getCheckedItemPositions();//получаем все cheked elements
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            Log.i(TAG, "onContextItemSelected: " + list_of_View.getItemAtPosition(sparseBooleanArray.keyAt(i)));
            String toDeleteString = (String) list_of_View.getItemAtPosition(sparseBooleanArray.keyAt(i));// получаем строку
            //list_of_View.setItemChecked(sparseBooleanArray.keyAt(i),false);
            sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_NAME + "= ?", new String[]{toDeleteString});
            //dbHelper.uninsertData(toDeleteString);
        }
        dbHelper.close();
        hashSetMainCollectorItems.clear();
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        sPref.edit().clear().apply();
        btnSave.callOnClick();

    }


    Runnable runnableToDelete = () -> prepareToDelete(choosen_ItemInClickmethod);


    //для поиска
    public void onmyQueryTextSubmit(String s) { //метод для поиска слова (присвоил к кнопке)
        try {
            //Тут начинаеться прыжки между двумя if else, если поиск продолжаться с уже найденного листа
            if (!found_List.isEmpty()) {
                toAppointSearchList(found_List, foundAccurateList, s);
                supportRequestHistoryForChangeStrings.add(s);
                Log.d(TAG, "Из found_List в foundAccurateList");
                //ToAppointSearchList(C какого листа идёт выборка, в какой лист переносяться найденные строки,Поисковое слово)
            } else if (!foundAccurateList.isEmpty()) {
                toAppointSearchList(foundAccurateList, found_List, s);
                supportRequestHistoryForChangeStrings.add(s);
                Log.d(TAG, "Из foundAccurateList в found_List");
            } else {
                toAppointSearchList(mainList, found_List, s);
                if (firstWordSearchingList.size() >= 5 && !firstWordSearchingList.contains(s)) {
                    //для поискового листа первого слово(Постредственный)
                    firstWordSearchingList.remove(0);
                    firstWordSearchingList.add(s);
                } else {
                    if (!firstWordSearchingList.contains(s)) firstWordSearchingList.add(s);
                }

                if (!supportRequestHistoryForChangeStrings.isEmpty()) {
                    //Для поскового листа, Если копаем глубже в found листы.(Главный)
                    supportRequestHistoryForChangeStrings.clear();
                    supportRequestHistoryForChangeStrings.add(s);
                    Log.d(TAG, "Из mainList в found_List с очисткой supportRequestHistoryForChangeStrings");
                } else {
                    Log.d(TAG, "Из mainList в found_List");
                    supportRequestHistoryForChangeStrings.add(s);
                }
                // ToAppointSearchList(C какого листа идёт выборка, в какой лист переносяться найденные строки,Поисковое слово)
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.search_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void toAppointSearchList(ArrayList<String> fromWhereSearchlist, ArrayList<String> toWhereList, String searchWord) {
        try {
            while (true) {
                //Note: В этом цикле пытаемся найти слово по запросу.Если слово не найденно то уменьшаем его длинну.
                for (String name : fromWhereSearchlist) {
                    if (name.toLowerCase().contains(searchWord.toLowerCase())) { // загружаем слова которые нашли в лист
                        toWhereList.add(name);
                    }
                }
                if (!toWhereList.isEmpty()) {
                    //Log.i(TAG, "toAppointSearchList: list не пустой");
                    break;
                } else if (searchWord.length() <= 2) {
                    Log.i(TAG, "toAppointSearchList: Слово уже слишком короткое уходим от сюда");
                    etName.setText("");
                    Toast.makeText(MainActivity.this, R.string.word_not_exist, Toast.LENGTH_LONG).show();
                    return;
                } else if (toWhereList.isEmpty()) {
                    Log.i(TAG, "toAppointSearchList: Уменьшаем слово");
                    searchWord = searchWord.substring(0, searchWord.length() - 1);
                }


            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_checked, toWhereList);
            list_of_View.setAdapter(adapter);
            fromWhereSearchlist.clear();

            loadCheckedItems(toWhereList);
            etName.setText("");
            //вычитаем point c батареи

            if (!isSubscribed) {
                Log.d(TAG, "toAppointSearchList: Подписки нет, уменьшаем батарею");
                batteryLvl--;

                binding.menuViewBattery.getBackground().setLevel((batteryLvl * 100));

                sPref = getSharedPreferences("BATTERY", MODE_PRIVATE);
                sPref.edit().putInt("KeyBatterylvl", batteryLvl).apply();
            }


        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.toappoint_search_list_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

    }// для поиска

    public void loadCheckedItems(ArrayList<String> list) {
        //метод используеться в btn Save\Update
        // И когда обновляет данные checked позиции в found листах.
        try {
            listFromSharedPreference.clear();
            //UP-date Main
            sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
            int kol = sPref.getInt("Kolichesvo", 0);
            if (listFromSharedPreference.isEmpty()) {
                for (int i = 0; i < kol; i++) {
                    listFromSharedPreference.add(sPref.getString("Keyg" + i, "")); //загрузка с Preferences всех сохранёных отмеченных итемов
                }
            }
            // Экспериментальный, опробован, подтверждён!
            for (int i = 0; i < list.size(); i++) {             //Теперь из основного Списка находим отмеченные
                for (int g = 0; g < listFromSharedPreference.size(); g++) {
                    if (list.get(i).contains(listFromSharedPreference.get(g))) {
                        list_of_View.setItemChecked(list.indexOf(list.get(i)), true);//Если имена совпали, в list.get(i) - получаем индекс, и сразу пихаем его в list.indexOf().
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.load_checked_items_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }


    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // View элементы у которых есть ID они сами востанавливают своё значение. Так же как Элементы Final.
        // А все другие нужно сохранять тут. А востанавливать в onRestoreInstanceState.
        outState.putBoolean("permission", bool_deleteFile_checkBox_isActivated);
        outState.putBoolean("val1", bool_fileOfNameReady);
        outState.putBoolean("val2", bool_fileNotChosen);
        outState.putBoolean("val3", bool_isSaved);
        outState.putBoolean("val4", bool_xlsExecutorCanceled);
        outState.putBoolean("val5", bool_xlsColumnsWasChosen);
        outState.putBoolean("val6", isSubscribed);
        if (localDateChecked != null) outState.putString("val7", localDateChecked.toString());

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        bool_deleteFile_checkBox_isActivated = savedInstanceState.getBoolean("permission");
        bool_fileOfNameReady = savedInstanceState.getBoolean("val1");
        bool_fileNotChosen = savedInstanceState.getBoolean("val2");
        bool_isSaved = savedInstanceState.getBoolean("val3");
        bool_xlsExecutorCanceled = savedInstanceState.getBoolean("val4");
        bool_xlsColumnsWasChosen = savedInstanceState.getBoolean("val5");
        isSubscribed = savedInstanceState.getBoolean("val6");
        if (dbHelper == null) dbHelper = new DBHelper(this);
        if (contentValues == null) contentValues = new ContentValues();
        if (handler == null) onHandlerCreate();
        if (localDateChecked == null){
            String date = savedInstanceState.getString("val7");
            localDateChecked = LocalDateTime.parse(date);
        }
        regSubElements(isSubscribed);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.i(TAG, "onPostCreate");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //isSaved = false;
        if (!bool_fileOfNameReady) {
            bool_fileNotChosen = true;
        }
        Log.i(TAG, "onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.i(TAG, "onPostResume");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: Активити умерло.");
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected: " + parent.getAdapter().getItem(position).toString());
        if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.manual))) {
            binding.menuSpiner.setSelection(menuSize);
            Intent intent = new Intent(MainActivity.this, UserGuideActivity.class);
            startActivity(intent);

        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.getSubscribe))) {
            binding.menuSpiner.setSelection(menuSize);
            Intent intent = new Intent(MainActivity.this, SubcribeClass.class);
            startActivity(intent);
        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.charset_determinations))) {
            binding.menuSpiner.setSelection(menuSize);
            Intent intent = new Intent(MainActivity.this, EncodingActivity.class);
            startActivityForResult(intent, 2);
        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.toSeeAds))) {
            Log.d(TAG, "onItemSelected: lol");
            if (mRewardedAd != null) {
                binding.menuSpiner.setSelection(menuSize);
                DialogClass dialogClass = new DialogClass(MainActivity.this,
                        getString(R.string.toGetBatteryEnergy),
                        getString(R.string.ad),
                        getString(R.string.towatch),
                        getString(R.string.cancel),
                        null
                );
                dialogClass.createStandartNewDialogShowAd();
                dialogClass.dialog.show();
            } else {
                binding.menuSpiner.setSelection(menuSize);
                Toast.makeText(this, R.string.rewardedAdsIsNull, Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}