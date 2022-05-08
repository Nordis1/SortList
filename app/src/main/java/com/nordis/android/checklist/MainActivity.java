package com.nordis.android.checklist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.nordis.android.checklist.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemSelectedListener {
    final static CountDownLatch countDownLatch = new CountDownLatch(1);
    final static int hSetCreateDialogFromWhichToWhich = 16;
    final static int hSetSubscribeTrue = 18;
    final static int hSetSubscribeFalse = 19;
    final static int hBillingClientInitializeIsCorrect = 20;
    final static int hShowAd = 22;
    final static int hSetSubscribePending = 23;
    final static int hSetSubscribeUNSPECIFIED = 24;
    //final String TAG = "Main_Activity";
    private static final String TAG = "Main_Activity";
    //String variables
    public static volatile String fileName; //Нужен что бы распозновать какой тип файла мы читаем xml,txt
    public static String name = "";
    public static volatile String choosen_ItemInClickmethod = ""; // Выделяемые View преобразуються в String в setOnItemCL. После checked_Items работает с HashSetMainCollectorItems
    //Boolean variables
    static boolean isSubscribed = false;// для того что бы из subscribtion class получить инфу о подписке.
    static volatile boolean
            bool_isSaved, ///bool_isSaved переменная служит для сохранения остатка , что бы удаление не произошло раньше чем не сохраниться остаток.
            bool_prepereDeleteRow, //bool_prepereDeleteRow - для контекстной функции Delete row.
            bool_onSaveReady, //bool_onSaveReady - для контекстной функции Delete All checked.
            bool_xlsColumnsWasChosen,
            bool_xlsExecutorCanceled,
            bool_accessToDeleteAllWithOutDialog,
            bool_haveDeletingRight,
            bool_billingInitializeOk,
            bool_owner,
            bool_neiser,
            boolPlayStoreISSigned = false;
    //Integer variables
    int diamondValue = 0;
    volatile static int mProgresscounter = 0;
    volatile static int mColumnmax = 0;
    volatile static int mColumnmin = 0;
    static int firstWordCounter = -1; //переменная вторичная
    static volatile SharedPreferences sPref;
    static Handler handler;
    static Uri uri; // Получаем нахождение файла в OnActivityResult
    static int menuSize = 4;
    final int hSetToastErrorOfFileReading = 1;
    final int hsetdelete_WithOut_rest = 3;
    final int hSetbtnReadFileEnabledFalse = 4;
    final int hSetbtnReadFileEnabledTrue = 5;
    final int hSetProgressBarVisible = 6;
    final int hSetProgressBarGone = 7;
    final int hSetMainInnerUserGuideOnVISIBLE = 8;
    final int hsetdelete_IsCanceled = 9;
    final int hsetlistView_Onvisible = 10;
    final int hSetLoadingListOfView_fromAdapter1 = 11;
    final int hSetToastErrorfromReadingAdditionalLoad = 12;
    final int hSetCreateDialogError = 13;
    final int hSetDoRest = 14;
    final int hSetDeleteRest = 15;
    final int hSetDeleteChekedPositions = 17;
    final int hBatteryOn = 25;
    final int hsetLostConnectionsWithGooglePlay = 26;
    final int hNewXLSReader = 27;
    final int hSetWhatIsListVisible = 28;
    final int hSetWhatIsListNotVisible = 29;
    final private int requestCodePermissionResult_ToReadFile = 2;
    //Inner DataBase SQL variables
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    //Arraylist variables
    //Примечание: mainlist и foundlist и foundAccurateList основные.В list_of_View отбражаться всё что есть в mainlist или foundlist с помощью  adapter1.
    ArrayList<String> mainList = new ArrayList<>(); //основной лист
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
    volatile ArrayAdapter adapter1;   //Главный Адаптер Он закидывает значения с mainList, found_List, foundAccurateList во ViewList тоесть list_of_View.
    Thread thread;
    volatile File_XLS_Reader file_xls_reader;
    String chosenCharset; // получаем значение в OnActivityResult когда выбрали кодировку.
    Executor executor, checkExecutor;
    Cursor cursor;
    Intent intent;
    ContentValues contentValues = new ContentValues();
    ActivityMainBinding binding;
    ActivityResultLauncher<String> mGetContentResult;
    ActivityResultLauncher<Intent> mGetActivityResult;
    DialogClass newdialog;
    LocalDateTime localDateChecked;
    Runnable runnableIncrementProgressbar = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "run: зашли в в увеличение progressbar mProgresscounter = " + mProgresscounter);
            binding.downloadBar.setProgress(mProgresscounter);

        }
    };
    Runnable runnableToDelete = () -> prepareToDelete(choosen_ItemInClickmethod);
    private RewardedAd mRewardedAd;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        executor = Executors.newCachedThreadPool(); // With this method, the thread lives 60 sec if it done.
        checkExecutor = Executors.newFixedThreadPool(3);

        //Лист куда закидываеться основной или поисковые листы при помощи адаптера (adapter1)
        binding.listItemModel.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        registerForContextMenu(binding.listItemModel);
        binding.listItemModel.setOnItemClickListener(this);

        /*иницализация кнопок*/
        binding.btnSave.setOnClickListener(this);
        binding.btnAddCustomLine.setOnClickListener(this);
        binding.btnLoadFile.setOnClickListener(this);
        binding.btnDeleteAll.setOnClickListener(this);
        binding.idBackSearch.setOnClickListener(this);
        binding.btnSearch.setOnClickListener(this);

        dbHelper = new DBHelper(this);

        viewData();// загружаем наш список
        checkInnerPreview(); // отображаем наш список
        onHandlerCreate(); // Создаём хендлер
        methodsRegisterForActivity(); //For ActivityResult
        checkSub(); // Проверяем подписку
        onMenuCreate(); // Создаём меню
        // onAdCreate(); Создаётся в методе  regSubElements();


    }

    private void methodsRegisterForActivity() {

        //Для  выбора charset
        mGetActivityResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {


                if (result.getResultCode() == RESULT_OK) {
                    assert result.getData() != null;
                    chosenCharset = result.getData().getStringExtra("nameOfCharset");
                    Log.d(TAG, "onActivityResult: Данные полученны: " + chosenCharset);
                }
                if (result.getResultCode() == RESULT_CANCELED) {
                    Log.d(TAG, "onActivityResult: Была отмена!");
                }

            }
        });


        //Для открытия и чтения файла.
        mGetContentResult = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                uri = result;
                fileName = getFileName(uri);
                Log.d(TAG, "onActivityResult: fileName is 1 : " + fileName);
                if (fileName != null && !fileName.isEmpty()) {
                    binding.IDMainInnerUserGuide.setVisibility(View.GONE);
                    creatingThreadToReadingFile();

                }
            }
        });


    }

    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void checkInnerPreview() {
        if (!mainList.isEmpty() || !found_List.isEmpty() || !foundAccurateList.isEmpty()) {
            /**Если Какой либо из листов не пуст тогда...
             * Убираем Гайд, врубаем видимость Листа, и название листа*/

            binding.IDMainInnerUserGuide.setVisibility(View.GONE);
            binding.idWhatIsList.setVisibility(View.VISIBLE);
            binding.listItemModel.setVisibility(View.VISIBLE);

        } else {
            /**Если все листы пусты тогда вырубаем всё , подключаем ЮзерГайд*/
            binding.listItemModel.setVisibility(View.GONE);
            binding.idWhatIsList.setVisibility(View.GONE);
            binding.IDMainInnerUserGuide.setVisibility(View.VISIBLE);
        }
        binding.btnSave.callOnClick();
    }

    //меню
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected: " + parent.getAdapter().getItem(position).toString());
        if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.manual))) {
            //Прочесть Руководсво пользователя
            binding.menuSpiner.setSelection(menuSize);
            intent = new Intent(MainActivity.this, UserGuideActivity.class);
            startActivity(intent);

        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.getSubscribe))) {
            //получить подписку
            if (!boolPlayStoreISSigned) {
                DialogClass dialogClass = new DialogClass(MainActivity.this,
                        getString(R.string.connectionIssue),
                        getString(R.string.playMarket),
                        null,
                        null,
                        getString(R.string.understand)
                );
                dialogClass.createStandartNewDialogShowAd();
                dialogClass.dialog.show();
            } else {
                binding.menuSpiner.setSelection(menuSize);
                intent = new Intent(MainActivity.this, SubcribeClass.class);
                startActivity(intent);
            }

            binding.menuSpiner.setSelection(menuSize);
        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.charset_determinations))) {
            // Изменить чтение charset
            binding.menuSpiner.setSelection(menuSize);
            intent = new Intent(MainActivity.this, EncodingActivity.class);
            mGetActivityResult.launch(intent);
        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.toSeeAds))) {
            //Посмотреть Рекламу
            if (!bool_owner) {
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
            binding.menuSpiner.setSelection(menuSize);
        } else if (parent.getAdapter().getItem(position).toString().equals(getString(R.string.evaluateUs))) {
            binding.menuSpiner.setSelection(menuSize);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.nordis.android.checklist"));
            startActivity(intent);

        }

    }

    private void onMenuCreate() {
        if (!menuList.isEmpty()){
            menuList.clear();
        }
        if (bool_owner) {
            menuSize = 3;
            menuList.add(getString(R.string.manual));
            menuList.add(getString(R.string.charset_determinations));
            menuList.add(getString(R.string.evaluateUs));
            menuList.add("");
        } else {
            menuSize = 5;
            menuList.add(getString(R.string.manual));
            menuList.add(getString(R.string.charset_determinations));
            menuList.add(getString(R.string.getSubscribe));
            menuList.add(getString(R.string.toSeeAds));
            menuList.add(getString(R.string.evaluateUs));
            menuList.add("");
        }
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

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkSub() {
        Log.d(TAG, "checkSub: Зашли в checkSub ");
        sPref = getSharedPreferences("OWNER", MODE_PRIVATE);
        bool_owner = sPref.getBoolean("keyown", false);

        if (bool_owner) {
            Log.d(TAG, "checkSub: зашли в проверку хозяин или Россиянен.");
            isSubscribed = true;
            regSubElements(isSubscribed);
        } else {
            Log.d(TAG, "checkSub: зашли в проверку подписки");
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
                        int i = 3;
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
                    boolPlayStoreISSigned = true;
                    Log.d(SubcribeClass.TAG, "From Main: we have got connections and checkThePurchases method begins");
                    subcribeClass.checkThePurchases();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, R.string.intializegettingFall, Toast.LENGTH_LONG).show();
                Log.d(TAG, "checkSub: error: " + e.getMessage());
            }

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
                        Log.d(TAG, "onAdFailedToLoad: " + loadAdError);
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
                            binding.btnDeleteAll.callOnClick();
                        });
                        break;
                    case 3:
                        binding.btnDeleteAll.callOnClick();
                        Toast.makeText(MainActivity.this, R.string.deleted_success, Toast.LENGTH_LONG).show();
                        break;
                    case 4:
                        binding.btnLoadFile.setEnabled(false);
                        break;
                    case 5:
                        binding.btnLoadFile.setEnabled(true);
                        break;
                    case 6:
                        binding.downloadBar.setVisibility(View.VISIBLE);
                        break;
                    case 7:
                        binding.downloadBar.setVisibility(View.GONE);
                        break;
                    case 8:
                        binding.IDMainInnerUserGuide.setVisibility(View.VISIBLE);
                        break;
                    case 9:
                        binding.etName.setText("");
                        Toast.makeText(MainActivity.this, getString(R.string.cancel), Toast.LENGTH_LONG).show();
                        break;
                    case 10:
                        binding.listItemModel.setVisibility(View.VISIBLE);
                        break;
                    case 11:
                        binding.listItemModel.setAdapter(adapter1);
                        binding.idWhatIsList.setText(R.string.main);
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
                        Log.d(TAG, "handleMessage: зашли в создание диалога");
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
                        //Подписка есть
                        Toast.makeText(MainActivity.this, R.string.subscribe_is_valid, Toast.LENGTH_LONG).show();
                        isSubscribed = true;
                        regSubElements(isSubscribed);
                        localDateChecked = LocalDateTime.now();
                        break;
                    case hSetSubscribeFalse:
                        //Подписки нет
                        Toast.makeText(MainActivity.this, R.string.subscribe_out, Toast.LENGTH_LONG).show();
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("DIAMOND", MODE_PRIVATE);
                        diamondValue = sPref.getInt("KeyDiamond", 50);
                        binding.idDiamondNumber.setText(String.valueOf(diamondValue));
                        break;
                    case hBillingClientInitializeIsCorrect:
                        bool_billingInitializeOk = true;
                        break;
                    case hShowAd:
                        showAdExecute();
                        break;
                    case hSetSubscribePending:
                        //Подписка в Ожидании
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("DIAMOND", MODE_PRIVATE);
                        diamondValue = sPref.getInt("KeyDiamond", 50);
                        binding.idDiamondNumber.setText(String.valueOf(diamondValue));

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
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("DIAMOND", MODE_PRIVATE);
                        diamondValue = sPref.getInt("KeyDiamond", 50);
                        binding.idDiamondNumber.setText(String.valueOf(diamondValue));
                        Toast.makeText(MainActivity.this, R.string.unspecified, Toast.LENGTH_LONG).show();
                        break;
                    case hsetLostConnectionsWithGooglePlay:
                        isSubscribed = false;
                        regSubElements(isSubscribed);
                        sPref = getSharedPreferences("DIAMOND", MODE_PRIVATE);
                        diamondValue = sPref.getInt("KeyDiamond", 50);
                        binding.idDiamondNumber.setText(String.valueOf(diamondValue));

                        newdialog = new DialogClass(MainActivity.this,
                                getString(R.string.lostConnection),
                                getString(R.string.connectionIssue),
                                getString(R.string.understand),
                                null, null);
                        newdialog.createDialogPendingState();
                        newdialog.dialog.show();
                        break;
                    case hNewXLSReader:
                        file_xls_reader = new File_XLS_Reader(fileName);
                        break;
                    case hSetWhatIsListVisible:
                        binding.idWhatIsList.setVisibility(View.VISIBLE);
                        break;
                    case hSetWhatIsListNotVisible:
                        binding.idWhatIsList.setVisibility(View.GONE);
                        break;

                }
            }
        };
    }

    public void regSubElements(Boolean subcribtionY) {
        binding.idDiamondNumber.setVisibility(View.GONE);
        binding.IdDiamondIcon.setVisibility(View.GONE);
        binding.IDX.setVisibility(View.GONE);
        binding.idsubscriptionText.setVisibility(View.GONE);
        binding.idTextOwner.setVisibility(View.GONE);
        if (subcribtionY) {
            //Убираем батарею и текст о подписке
            if (bool_owner) {
                //Если сюда , значит Хозяин.
                Log.d(TAG, "regSubElements: Зашли в оформление Хозяина");
                binding.idTextOwner.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, R.string.developer_mode, Toast.LENGTH_SHORT).show();
            } else {
                //Если сюда, значит подписчик.
                binding.idsubscriptionText.setVisibility(View.VISIBLE);
            }

        } else {
            //Сюда если Халявщик
            binding.IdDiamondIcon.setVisibility(View.VISIBLE);
            binding.IDX.setVisibility(View.VISIBLE);
            binding.idDiamondNumber.setVisibility(View.VISIBLE);
            onAdCreate();
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
                    onAdCreate();
                    //mRewardedAd = null;
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
                    diamondValue += rewardAmount;

                    sPref = getSharedPreferences("DIAMOND", MODE_PRIVATE);
                    sPref.edit().putInt("KeyDiamond", diamondValue).apply();

                    assert binding.idDiamondNumber != null;
                    binding.idDiamondNumber.setText(String.valueOf(diamondValue));

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
            binding.btnSave.callOnClick();
            try {
                ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
                for (int i = 0; i < loadhistory.size(); i++) {
                    binding.etName.setText(loadhistory.get(i));//сюда закидываються слова которые были в поиске
                    binding.btnSearch.callOnClick();
                }
                loadhistory.clear();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

        } else {
            Log.i(TAG, "prepareTochange: простое изменение");
            sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});
            binding.btnSave.callOnClick();
        }
        dbHelper.close();

    }

    public void prepareToDelete(String becomeDeleteString) {
        sqLiteDatabase = dbHelper.getWritableDatabase();
        if (!found_List.isEmpty() || !foundAccurateList.isEmpty()) {
            sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_NAME + "= ?", new String[]{becomeDeleteString});
            binding.btnSave.callOnClick();
            try {
                ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
                for (int i = 0; i < loadhistory.size(); i++) {
                    binding.etName.setText(loadhistory.get(i));//сюда закидываються слова которые были в поиске
                    binding.btnSearch.callOnClick();
                }
                loadhistory.clear();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            try {
                sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_NAME + "= ?", new String[]{becomeDeleteString});
                binding.btnSave.callOnClick();
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
        choosen_ItemInClickmethod = ((TextView) view).getText().toString(); // Кликнутая строка в данный момент
        checkExecutor.execute(() -> checkedItemsReloadInfo());

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
                if (binding.downloadBar.getMax() <= 40) {// В зависимости от объёма данных увеличиваем скорость загрузки
                    try {
                        TimeUnit.MILLISECONDS.sleep(70); //0.7 - 2.8 sec
                        //Log.i(TAG, "Sleep 50");
                        handler.post(runnableIncrementProgressbar);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (binding.downloadBar.getMax() > 40 && binding.downloadBar.getMax() <= 100) { //1.4 - 3.5 sec
                    try {
                        TimeUnit.MILLISECONDS.sleep(35);
                        //Log.i(TAG, "Sleep 35");
                        if (mProgresscounter % 4 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (binding.downloadBar.getMax() > 100 && binding.downloadBar.getMax() <= 350) { //1.3 - 4.5 sec
                    try {
                        TimeUnit.MILLISECONDS.sleep(13);
                        //Log.i(TAG, "Sleep 30");
                        if (mProgresscounter % 5 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (binding.downloadBar.getMax() > 350 && binding.downloadBar.getMax() <= 600) { // 2.4 - 4.2
                    try {
                        TimeUnit.MILLISECONDS.sleep(7);
                        //Log.i(TAG, "Sleep 12");
                        if (mProgresscounter % 8 == 0) {
                            handler.post(runnableIncrementProgressbar);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (binding.downloadBar.getMax() > 600) { // 3 sec
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
            mProgresscounter = 0;
            handler.post(runnableIncrementProgressbar);
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
            binding.idWhatIsList.setText(R.string.main);
            adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, mainList);
            binding.listItemModel.setAdapter(adapter1);
            cursor.close();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        name = binding.etName.getText().toString();
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
                if (binding.etName.length() == 0 || bool_haveDeletingRight) {
                    Log.d(TAG, "onClick: зашли в обновление данных");
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
                    binding.etName.setText("");
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
                String del = binding.etName.getText().toString();
                String delete = "Delete";
                if (del.equals(delete) || bool_accessToDeleteAllWithOutDialog) { // Если Ввёл в строку Delete
                    try {

                        sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, null, null);
                        File file = new File("data/data/com.nordis.android.checklist/databases/DBNeiser");
                        File file2 = new File("data/data/com.nordis.android.checklist/databases/DBNeiser-journal");
                        if (file.exists() || file2.exists()) {
                            file.delete();
                            file2.delete();
                            Log.d(TAG, "Файлы базы данных были обнаружины и удалены! Существуют ли файлы баз данных после удаления? " + file.exists());
                        } else {
                            Log.d(TAG, "onClick: Файлы базы данных не были обнаруженны.");
                        }


                    } catch (Exception e) {
                        Log.d(TAG, "Ошибка удаления внутренних файлов DBNeiser и DBNeiser-journal " + e.getMessage());
                        e.printStackTrace();
                    }

                    try {
                        Log.i(TAG, "onClick: Зашли в чистку всех елементов");
                        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        cursor.close();
                        fileName = null;
                        listFromSharedPreference.clear();
                        listForSearch.clear();
                        mainList.clear();
                        found_List.clear();
                        downloadList.clear();
                        foundAccurateList.clear();
                        hashSetMainCollectorItems.clear();
                        choosen_ItemInClickmethod = null;
                        bool_accessToDeleteAllWithOutDialog = false;
                        adapter1.clear();
                        mProgresscounter = 0;
                        mColumnmax = 0;
                        mColumnmin = 0;
                        name = null;
                        binding.etName.setText("");
                        bool_haveDeletingRight = false;
                        binding.idWhatIsList.setVisibility(View.GONE);
                        binding.listItemModel.setVisibility(View.GONE);
                        binding.IDMainInnerUserGuide.setVisibility(View.VISIBLE);
                        file_xls_reader = null;

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
                    binding.etName.setText("Delete");
                }
                break;
//Загрузка
            case R.id.btnLoadFile:
                if (!mainList.isEmpty() || !found_List.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.list_is_already_full, Toast.LENGTH_LONG).show();

                } else {
                    //Проверяем доступ к файлам.
                    int result1 = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    //Если нет доступа то запрашиваем и в onPermissionResult осуществляем открытие документа.
                    if (result1 != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCodePermissionResult_ToReadFile);
                    } else {
                        mGetContentResult.launch("*/*");
                    }
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
                    if (name.equals("Nordis-picker=null")) {
                        sPref = getSharedPreferences("OWNER", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        bool_owner = false;
                        isSubscribed = false;
                        binding.etName.setText("");
                        onMenuCreate();
                        checkSub();
                    }
                    if (name.equals("Nordis-picker")) {
                        Log.d(TAG, "onClick: зашли в Nordis-picker");
                        bool_owner = true;
                        isSubscribed = true;
                        sPref = getSharedPreferences("OWNER", MODE_PRIVATE);
                        sPref.edit().putBoolean("keyown", bool_owner).apply();
                        binding.etName.setText("");
                        onMenuCreate();
                        checkSub();


                    } else if (!isSubscribed) {//Если нет подписки то...
                        if (diamondValue == 0) {
                            Toast.makeText(this, getString(R.string.checkBatteryLoading), Toast.LENGTH_LONG).show();
                        } else {
                            String searchWord = binding.etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                            onmyQueryTextSubmit(searchWord);
                        }
                    } else {
                        if (!bool_owner) { // Если обычный подписчик, проверяем каждые 3 дня
                            try {
                                if (LocalDateTime.now().isAfter(localDateChecked.plusDays(3))) {
                                    // Если прошёло 3 дня с момента последней проверки подписки, то проверяем снова.
                                    Log.d(TAG, "onClick: проходим дополнительную проверку.");
                                    checkSub();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, getString(R.string.DateError), Toast.LENGTH_LONG).show();
                                intent = new Intent(this,MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                        String searchWord = binding.etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                        onmyQueryTextSubmit(searchWord);
                    }
                }
                break;

//История поиска
            case R.id.idBackSearch:

                try {
                    if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                        Toast.makeText(MainActivity.this, getString(R.string.First_download_the_file), Toast.LENGTH_SHORT).show();
                        break;
                    }
                    if (supportRequestHistoryForChangeStrings.size() >= 2) {
                        Log.d(TAG, "onClick: зашли в историю поиска пунк 1");
                        try {
                            loadhistory.addAll(supportRequestHistoryForChangeStrings);
                            binding.btnSave.callOnClick();
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

                                binding.etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                                binding.btnSearch.callOnClick();
                            }
                            loadhistory.clear();
                            Log.d(TAG, "onClick: вызываем тост");
                            Toast.makeText(this, howDeepWeGo, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    } else if (!firstWordSearchingList.isEmpty()) {
                        binding.btnSave.callOnClick();
                        if (firstWordCounter <= -1)
                            firstWordCounter = firstWordSearchingList.size() - 1;
                        Log.i(TAG, "onClick: firstWordSearchingList.size = " + firstWordSearchingList.size());
                        Log.i(TAG, "onClick: firstWordCounter = " + firstWordCounter);
                        binding.etName.setText(firstWordSearchingList.get(firstWordCounter));
                        firstWordCounter--;
                        binding.btnSearch.callOnClick();
                    } else {
                        Log.d(TAG, "onClick: зашли в историю поиска пунк 2");
                        binding.btnSave.callOnClick();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, R.string.backSearchError + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                break;
//Добавляемая строка
            case R.id.btnAddCustomLine:
                if (!name.isEmpty() && mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    Log.d(TAG, "onClick: Зашли в создание первого итема");
                    dbHelper.insertData(name);
                    viewData();
                    checkInnerPreview();
                    binding.etName.setText("");
                } else if (!name.isEmpty() && !mainList.isEmpty() && !mainList.contains(name)) {
                    Log.d(TAG, "onClick: Зашли в создание второстепенного итема");
                    dbHelper.insertData(name);
                    mainList.clear();
                    viewData();
                    binding.etName.setText("");
                    binding.btnSave.callOnClick();
                } else if (mainList.contains(name)) {
                    Toast.makeText(this, R.string.element_isExist, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.write_something_in_the_place, Toast.LENGTH_LONG).show();
                }
                break;


        }
        dbHelper.close();

    }

    public void createRestMemory() {
        //mainList.clear();
        //viewData(); // тут мы востанавливаем main
        binding.btnSave.callOnClick();
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
        binding.listItemModel.setAdapter(adapter1);
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
        if (requestCode == requestCodePermissionResult_ToReadFile &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mGetContentResult.launch("*/*");
            //Toast.makeText(this,getString(R.string.permission_granted),Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
        }
    }


    private void creatingThreadToReadingFile() {
        thread = new Thread(() -> {
            Log.i(TAG, "run: Поток закачки был запущен");
            if (fileName.contains(".txt")) {
                Log.i(TAG, "run: File был распознан как txt");

                handler.sendEmptyMessage(hSetbtnReadFileEnabledFalse);
                handler.sendEmptyMessage(hSetProgressBarVisible);
                loadingRest();
                mainloading();

                handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                handler.sendEmptyMessage(hSetProgressBarGone);
                handler.sendEmptyMessage(hSetWhatIsListVisible);
                handler.sendEmptyMessage(hsetlistView_Onvisible);
            } else if (fileName.contains(".xls")) {
                if (fileName.toLowerCase(Locale.ROOT).contains("neiser")) {
                    Log.d(TAG, "Содержит в названии Neiser");
                    bool_neiser = true;
                }
                Log.i(TAG, "run: File был распознан как xls");
                try {
                    handler.sendEmptyMessage(hSetbtnReadFileEnabledFalse);
                    handler.sendEmptyMessage(hSetProgressBarVisible); // Диактивируем кнопку и Активируем прогресс бар

                    Log.i(TAG, "run: Передаём имя файла, файлу который считывает");
                    Log.i(TAG, "onRequestPermissionsResult: имя файла " + fileName);
                    handler.sendEmptyMessage(hNewXLSReader);// передаём имя файла в наш Класс, который читает его.
                    while (file_xls_reader == null) {
                        TimeUnit.SECONDS.sleep(1);
                        Log.d(TAG, "ждём  инициальзации file_xls_reader");
                    }
                    Log.d(TAG, "Инициализация успешна!");
                    loadingRest(); // запускаем метод что бы получить остаток если он есть + получить переменную mProgresscounter.
                    downloadList = file_xls_reader.readingXLS(MainActivity.this);
                    Log.i(TAG, "run: Получили считанный лист. Его размер: " + downloadList.size());

                    int j = downloadList.size() + mProgresscounter;
                    binding.downloadBar.setMax(j); // так как переменная volantile все изменения будут видны в любом потоке.
                    Log.i(TAG, "mainloading: progress bar MAX = " + j);

                    for (int i = 0; i < downloadList.size(); i++) {
                        //начал добавлять сразу в базу что бы не нагружать main.
                        dbHelper.insertData(downloadList.get(i));
                    }
                    viewDataForDownloading(); //образуем показ Загрузки и списка после того как он полностью загрузиться в Базу
                    downloadList.clear();
                    file_xls_reader = null;
                    mProgresscounter = 0;
                    bool_neiser = false;
                    //Востанавливаем кнопку и убирает прогресс бар.
                    handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                    handler.sendEmptyMessage(hSetProgressBarGone);
                    handler.sendEmptyMessage(hSetWhatIsListVisible);
                    handler.sendEmptyMessage(hsetlistView_Onvisible);

                } catch (Exception e) {
                    Log.i(TAG, "onRequestPermissionsResult: Поток был прерван в main");
                    Log.d(TAG, "onRequestPermissionsResult: " + e.getMessage());
                    handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
                    handler.sendEmptyMessage(hSetProgressBarGone);
                    handler.sendEmptyMessage(hSetMainInnerUserGuideOnVISIBLE);
                    file_xls_reader = null;
                    mProgresscounter = 0;
                    bool_neiser = false;
                    bool_xlsExecutorCanceled = false;
                    bool_xlsColumnsWasChosen = false;
                    bool_neiser = false;
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

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

            //readTextFromUri(uri);

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
            binding.downloadBar.setMax(downloadList.size()); // так как переменная volantile все изменения будут видны в любом потоке.
            //Log.i(TAG, "mainloading: progress bar MAX = " + j);
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
            if (!mainList.isEmpty()) {
                handler.sendEmptyMessage(hSetWhatIsListVisible);
            }
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
                    if (s.contains(" - Old \uD83D\uDCDC")) {
                        downloadList.add(s);
                    } else downloadList.add(s + " - Old \uD83D\uDCDC");
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
            choosen_ItemInClickmethod = binding.listItemModel.getItemAtPosition(position1).toString();
            //Находим строку и присваеваем её к переменной , которая взаимодейсвует с методом checkedItemsReloadInfo();
            if (binding.listItemModel.isItemChecked(position1)) {
                binding.listItemModel.setItemChecked(position1, false);
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
            if (mainList.size() == 1) {
                bool_accessToDeleteAllWithOutDialog = true;
                binding.btnDeleteAll.callOnClick();
            } else {
                bool_prepereDeleteRow = false;
                choosen_ItemInClickmethod = binding.listItemModel.getItemAtPosition(position1).toString();
                //Находим строку и присваеваем её к переменной , которая взаимодейсвует с методом checkedItemsReloadInfo();
                if (binding.listItemModel.isItemChecked(position1)) {
                    binding.listItemModel.setItemChecked(position1, false);
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
            }
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
                binding.btnSave.callOnClick();
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
                binding.btnSave.callOnClick();
            } else if (!found_List.isEmpty()) {
                getAllListItemsIsCheched(found_List);
            } else if (!foundAccurateList.isEmpty()) {
                getAllListItemsIsCheched(foundAccurateList);
            }

        } else if (itemId == R.id.menuUnCheckAllPositions) {
            if (!mainList.isEmpty()) {
                binding.btnSave.callOnClick();
                sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                sPref.edit().clear().apply();
                hashSetMainCollectorItems.clear();
                binding.btnSave.callOnClick();
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
        binding.btnSave.callOnClick();
        try {
            ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
            for (int i = 0; i < loadhistory.size(); i++) {
                binding.etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                binding.btnSearch.callOnClick();
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
        binding.btnSave.callOnClick();
        try {
            ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
            for (int i = 0; i < loadhistory.size(); i++) {
                binding.etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                binding.btnSearch.callOnClick();
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
        binding.btnSave.callOnClick();
        if (hashSetMainCollectorItems.size() == mainList.size()) {
            binding.etName.setText("Delete");
            binding.btnDeleteAll.callOnClick();
        }
        sqLiteDatabase = dbHelper.getWritableDatabase();
        SparseBooleanArray sparseBooleanArray = binding.listItemModel.getCheckedItemPositions();//получаем все cheked elements
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            Log.i(TAG, "onContextItemSelected: " + binding.listItemModel.getItemAtPosition(sparseBooleanArray.keyAt(i)));
            String toDeleteString = (String) binding.listItemModel.getItemAtPosition(sparseBooleanArray.keyAt(i));// получаем строку
            //list_of_View.setItemChecked(sparseBooleanArray.keyAt(i),false);
            sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_NAME + "= ?", new String[]{toDeleteString});
            //dbHelper.uninsertData(toDeleteString);
        }
        dbHelper.close();
        hashSetMainCollectorItems.clear();
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        sPref.edit().clear().apply();
        binding.btnSave.callOnClick();

    }

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
                    binding.etName.setText("");
                    Toast.makeText(MainActivity.this, R.string.word_not_exist, Toast.LENGTH_LONG).show();
                    return;
                } else if (toWhereList.isEmpty()) {
                    Log.i(TAG, "toAppointSearchList: Уменьшаем слово");
                    searchWord = searchWord.substring(0, searchWord.length() - 1);
                }


            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_checked, toWhereList);
            binding.listItemModel.setAdapter(adapter);
            fromWhereSearchlist.clear();

            if (!mainList.isEmpty()) {
                binding.idWhatIsList.setText(R.string.main);
            } else {
                binding.idWhatIsList.setText(R.string.searchList);
            }

            loadCheckedItems(toWhereList);
            binding.etName.setText("");
            //вычитаем point c батареи

            if (!isSubscribed) {
                Log.d(TAG, "toAppointSearchList: Подписки нет, уменьшаем батарею");
                diamondValue--;

                assert binding.idDiamondNumber != null;
                binding.idDiamondNumber.setText(String.valueOf(diamondValue));


                sPref = getSharedPreferences("DIAMOND", MODE_PRIVATE);
                sPref.edit().putInt("KeyDiamond", diamondValue).apply();
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
            Log.d(TAG, "loadCheckedItems: кол-во сохранёных в spref " + kol);
            for (int i = 0; i < kol; i++) {
                listFromSharedPreference.add(sPref.getString("Keyg" + i, "")); //загрузка с Preferences всех сохранёных отмеченных итемов
            }

            // Экспериментальный, опробован, подтверждён!
            for (int i = 0; i < list.size(); i++) {             //Теперь из основного Списка находим отмеченные
                for (int g = 0; g < listFromSharedPreference.size(); g++) {
                    if (list.get(i).contains(listFromSharedPreference.get(g))) {
                        binding.listItemModel.setItemChecked(list.indexOf(list.get(i)), true);
                        //Если имена совпали, в list.get(i) - получаем индекс, и сразу пихаем его в list.indexOf().
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
        if (localDateChecked != null) outState.putString("val", localDateChecked.toString());
        // View элементы у которых есть ID они сами востанавливают своё значение. Так же как Элементы Final.
        // А все другие нужно сохранять тут. А востанавливать в onRestoreInstanceState.
        //outState.putBoolean("val3", bool_isSaved);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (localDateChecked == null) {
            String date = savedInstanceState.getString("val");
            localDateChecked = LocalDateTime.parse(date);
        }
        //Всё что создаёться в on Create тут востанавливать не нужно.
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
    public void onNothingSelected(AdapterView<?> parent) {

    }
}