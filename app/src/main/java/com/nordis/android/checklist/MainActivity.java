package com.nordis.android.checklist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
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
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.Gravity;
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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.example.android.checklist.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
//Планы на завтра  Поправить Layout для телефонов для планшетов вроде норм.
//Реализовать фунционал в контекстном меню delete row
// Добавить возможность чтения xls файлов.
// Не забыть включить удаление файла при удалении базы данных. По возможности сделать это изберательно.
//Попробовать сделать огромный список на 600+ элементов, посмотреть как будет вести себя Listview.Будет ли лагать.
        AdapterView.OnItemClickListener {
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    EditText etName;
    Button btnSave, btnReadFile, btnDeleteAll, btnSearch, btnBackSearch;
    ArrayList<String> listFromSharedPreference = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Main
    ArrayList<String> listForSearch = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Search
    volatile ArrayList<String> downloadList = new ArrayList<>();  // - Куда считываеться изначально тексты с файла а потом с него загружаем в Базу

    //ниже 3 листа ипользуемые в методе onQueryTextSubmit для поиска.
    //Примечание: mainlist и foundlist и foundAccurateList основные.В list_of_View отбражаться всё что есть в mainlist или foundlist с помощью  adapter1.
    ArrayList<String> mainList; //основной лист
    ArrayList<String> found_List = new ArrayList<>();
    ArrayList<String> foundAccurateList = new ArrayList<>();// Используеться в более точном поиске
    ArrayList<String> supportRequestHistoryForChangeStrings = new ArrayList<>();// Для изменения строки
    ArrayList<String> firstWordSearchingList = new ArrayList<>();// Для изменения строки
    volatile ListView list_of_View;   //Лист куда закидываеться основной или поисковые листы при помощи адаптера (adapter1). Для отображения.
    volatile ArrayAdapter adapter1;   //Главный Адаптер Он закидывает значения с mainList, found_List, foundAccurateList во ViewList тоесть list_of_View.


    public static String name = "";
    volatile HashSet<String> hashSetMainCollectorItems = new HashSet<>(); // главный подсчёт выделяемых item elements в setOnItemCLick.
    int backcounter = 0;  //backcounter - работает с backSearchlist
    final int requestCode1 = 1;
    ConstraintLayout constraintLayoutManual;
    Thread thread;
    static Boolean isSubscribed = null;// для того что бы из subscribtion class получить инфу о подписке.


    public static volatile String fileName;
    volatile ProgressBar progressBar;
    static volatile boolean bool_fileOfNameReady, bool_fileNotChosen, bool_isSaved,
            bool_deleteFile_checkBox_isActivated, bool_prepereDeleteRow, bool_onSaveReady, bool_xlsColumnsWasChosen,
            bool_xlsExecutorCanceled, bool_haveDeletingRight,bool_billingInitializeOk = false;
    //bool_fileOfNameReady используеться в Загрузке и onRestart и ActivityResult
    //fileNotChoosed переменная служит для остановки потока который хочет считать имя файла работает в паре fileOfNameReady. Приобретает свойсва true  в методе onRestart()
    //bool_prepereDeleteRow - для контекстной функции Delete row.
    //bool_isSaved - используется в RestCreating. Что бы прога не удалила план пока не завершится сохранение остатка.
    //bool_onSaveReady - для контекстной функции Delete All checked.
    //bool_deleteFile_checkBox_isActivated специальная переменная для удаленя файла на носителе.
    //isSaved переменная служит для сохранения остатка , что бы удаление не произошло раньше чем не сохраниться остаток.
    volatile static int mProgresscounter = 0;
    volatile static int mColumnmax = 0;
    volatile static int mColumnmin = 0;
    static int firstWordCounter = -1;
    int searchRemain = 3;


    final int hSetToastErrorOfFileReading = 1;
    final int hsetdelete_With_rest = 2;
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
    final static int hSetIsSubscribeTrue = 18;
    final static int gethSetIsSubscribeFalse = 19;
    final static int hBillingClientInitializeIsCorrect = 20;
    final int hcheckSubscribtionWithOutNet = 21;

    Toast toast;
    static volatile SharedPreferences sPref;
    final String TAG = "Main_Activity";
    public static volatile String choosen_ItemInClickmethod = ""; // Выделяемые View преобразуються в String в setOnItemCL. После checked_Items работает с HashSetMainCollectorItems
    Executor executor;
    Cursor cursor;
    ContentValues contentValues = new ContentValues();
    static Handler handler;
    final static CountDownLatch countDownLatch = new CountDownLatch(1);
/*    работа с simple cursor adapter
    SimpleCursorAdapter scAdapter;
    String[] from = new String[] {DBHelper.KEY_NAME,DBHelper.KEY_MODEL,DBHelper.KEY_DATA};
    int[] to  = new int[] {R.id.textFirstName,R.id.textModel,R.id.textData};*/

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executor = Executors.newCachedThreadPool();


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

    public void checkSub() {
        if (!checkSubscribtionWithOutNet()) {
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
                        //countDownLatch.await(1,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "checkSub: Запуск checkSubscribtionWithOutNet()");
                        handler.sendEmptyMessage(hcheckSubscribtionWithOutNet);
                        e.printStackTrace();
                        return;
                    }
                    Log.d(SubcribeClass.TAG, "From Main: we have got connections and checkThePurchases method begins");
                    subcribeClass.checkThePurchases();
                });

                if (isSubscribed == null) {
                    checkSubscribtionWithOutNet();
                }
            } catch (Exception e) {
                //Далее методика Если инициализация не прошла, то идёт проверка на существующий токен, А если есть то сколько он ещё дейсвует.
                e.printStackTrace();
                Log.d(TAG, "checkSub: error: " + e.getMessage());
                checkSubscribtionWithOutNet();
                return;
            }
        }

    }
    public boolean checkSubscribtionWithOutNet(){
        Log.d(TAG, "checkSubscribtionWithOutNet: Launch token initializations witOut Internet!");
        sPref = getSharedPreferences("Tokens", MODE_PRIVATE);
        String token = sPref.getString("Token", "");
        String isValidTime = sPref.getString("Period&SubTime","");
        Log.d(TAG, "checkSub: We got data from Spref: Purchase Time is : "+ isValidTime);

        //Если токена нет, то загружаем сколько осталось поисковых кликов.
        if (token == null || token.isEmpty()) {
            sPref = getSharedPreferences("SEARCH_REMAIN",MODE_PRIVATE);
            if (sPref != null){
                searchRemain = sPref.getInt("isRemain",3);
                Log.d(TAG, "checkSub: токен не получен, загрузка цифр подсчёта: "+ searchRemain);
            }
            isSubscribed = false;
        } else {
            Log.d(TAG, "checkSubscribtionWithOutNet: Токен получен, начинаем просмотр его Валидности");
            DateTimeFormatter ldFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate localDateNow = LocalDate.now();
            if (isValidTime != null || !isValidTime.isEmpty()){
                Log.d(TAG, "checkSubscribtionWithOutNet: Заходим в  проверку");
                String [] ob = isValidTime.split("/");
                String subType = ob[0];
                String subDateStarts = ob[1];
                LocalDate ldSubDateStarts = LocalDate.parse(subDateStarts,ldFormatter);
                Log.d(TAG, "checkSubscribtionWithOutNet: SybType: "+ subType);
                if (subType.equals("P1M")){
                    Log.d(TAG, "checkSubscribtionWithOutNet: подписка была 1 месяц, проверка годности.");
                    LocalDate ldSubDateExpire = ldSubDateStarts.plusMonths(1);
                    if (!checkDateValid(localDateNow,ldSubDateExpire)){
                        isSubscribed = false;
                        return isSubscribed;
                    }
                }else if (subType.equals("P6M")){
                    Log.d(TAG, "checkSubscribtionWithOutNet: Подписка была на 6 месяцов, проверка годности.");
                    LocalDate ldSubDateExpire = ldSubDateStarts.plusMonths(6);
                    if (!checkDateValid(localDateNow,ldSubDateExpire)){
                        isSubscribed = false;
                        return isSubscribed;
                    }
                }else if (subType.equals("P1Y")){
                    Log.d(TAG, "checkSubscribtionWithOutNet: Подписка была на год,проверка годности.");
                    LocalDate ldSubDateExpire = ldSubDateStarts.plusYears(1);
                    if (!checkDateValid(localDateNow,ldSubDateExpire)){
                        isSubscribed = false;
                        return isSubscribed;
                    }
                }
            }

            Log.d(TAG, "checkSub: token получен всё хорошо. Подписка действительна");
            toast = Toast.makeText(MainActivity.this, R.string.subscribe_is_valid, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 330);//250
            toast.show();
            isSubscribed = true;
        }

        return isSubscribed;
    }


    public Boolean checkDateValid(LocalDate now,LocalDate mustExpire){
        Boolean isvalid = true;
        Log.d(TAG, "checkDateValid: зашли в проверку валидности даты");

        if (now.isAfter(mustExpire)){
            toast = Toast.makeText(MainActivity.this, R.string.subscribe_out, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 330);
            toast.show();
            Log.d(TAG, "checkSubscribtionWithOutNet: Подписка истекла");
            isSubscribed = false;
            isvalid = false;
            return isvalid;
        }

        return isvalid;
    }


    public void onHandlerCreate() {
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String s = msg.getData().getString("changeString");
                if (s != null) {
                    Log.d(TAG, "handleMessage: получили строку " + s);
                    prepareTochange(s);
                }
                switch (msg.what) {
                    case 1:
                        toast = Toast.makeText(MainActivity.this, (R.string.File_reading_Error), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
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
                        toast = Toast.makeText(MainActivity.this, R.string.deleted_success, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
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
                        toast = Toast.makeText(MainActivity.this, getString(R.string.cancel), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250 //y - чем выше значение тем ниже элемент
                        toast.show();
                        break;
                    case 10:
                        list_of_View.setVisibility(View.VISIBLE);
                        break;
                    case 11:
                        list_of_View.setAdapter(adapter1);
                        break;
                    case 12:
                        toast = Toast.makeText(MainActivity.this, R.string.Reading_rest_data_Error, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
                        break;
                    case 13:
                        toast = Toast.makeText(MainActivity.this, R.string.Create_dialog_Error, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
                        break;
                    case 14:
                        createRestMemory();
                        break;
                    case 15:
                        deleteRestMemory();
                        break;
                    case 16:
                        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                        DialogClass dialogClass = new DialogClass(MainActivity.this,
                                null,
                                inflater,
                                null
                        );
                        dialogClass.createCustomNewDialogFromWhichTowhich();
                        dialogClass.dialog.show();
                        break;
                    case 17:
                        deleteCheckedItems();
                        toast = Toast.makeText(MainActivity.this, R.string.DeleteCheked, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);
                        toast.show();
                        break;
                    case hSetIsSubscribeTrue:
                        isSubscribed = true;
                        break;
                    case gethSetIsSubscribeFalse:
                        sPref = getSharedPreferences("Tokens", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        isSubscribed = false;
                        break;
                    case hBillingClientInitializeIsCorrect:
                        bool_billingInitializeOk = true;
                        break;
                    case hcheckSubscribtionWithOutNet:
                        checkSubscribtionWithOutNet();
                        break;

                }
            }
        };
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
                toast = Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 330);//250
                toast.show();
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
                toast = Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 330);//250
                toast.show();
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
            toast = Toast.makeText(MainActivity.this, getString(R.string.Not_data_to_show) + cursor.getInt(3), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 330);
            toast.show();
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

    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Сюда приходим из Загрузки, и ищём наш файл на устройстве. А так же инициализируем его имя.
        if (requestCode == requestCode1 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            Uri uri = data.getData();
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

    }


    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        name = etName.getText().toString(); //Сохранение в базу шаг 1
        sqLiteDatabase = dbHelper.getWritableDatabase();
        //ContentValues contentValues = new ContentValues(); //шаг 2
        switch (v.getId()) {
//Обновить
            case R.id.btnSave:
                Log.d(TAG, "onClick: Status subscribe : "+ isSubscribed);
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

                    choosen_ItemInClickmethod = "";
                    showListIsReadyPercent();
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
                    toast = Toast.makeText(MainActivity.this, R.string.list_is_already_full, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 330);
                    toast.show();
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
                if (!isSubscribed){
                    if (searchRemain <= 0) {
                        Toast.makeText(this, R.string.please_subscribe, Toast.LENGTH_LONG).show();
                        return;
                    }
                    searchRemain--;
                    Log.d(TAG, "onClick: Поисковых попыток осталось: "+ searchRemain);
                    sPref = getSharedPreferences("SEARCH_REMAIN",MODE_PRIVATE);
                    sPref.edit().putInt("isRemain",searchRemain).apply();

                }


                if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.First_download_the_file), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (name == null || name.length() == 0) {
                    Toast.makeText(this, R.string.write_something_in_the_place, Toast.LENGTH_LONG).show();
                } else {
                    String searchWord = etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                    onmyQueryTextSubmit(searchWord);
                }
                break;

//История поиска
            case R.id.IdBackSearch:
                if (mainList.isEmpty() && found_List.isEmpty() && foundAccurateList.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.First_download_the_file), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (supportRequestHistoryForChangeStrings.size() >= 2) {
                    btnSave.callOnClick();
                    try {
                        ArrayList<String> loadhistory = new ArrayList<>(supportRequestHistoryForChangeStrings);
                        // supportRequestHistoryForChangeStrings чистить не нужно так как чистица автоматом в поиске.
                        for (int i = 0; i < loadhistory.size() - 1; i++) {
                            etName.setText(loadhistory.get(i)); //сюда закидываються слова которые были в поиске
                            btnSearch.callOnClick();
                        }
                        loadhistory.clear();
                    } catch (Exception e) {
                        toast = Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
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
                    Log.i(TAG, "onClick: firstWordSearchingList.size = " + firstWordSearchingList.size());
                    btnSave.callOnClick();
                }
                break;


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
                            toast = Toast.makeText(MainActivity.this, getString(R.string.file_deleted_successfully), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.TOP, 0, 330);
                            toast.show();
                            sPref.edit().clear().apply();
                        } else {
                            toast = Toast.makeText(MainActivity.this, getString(R.string.file_not_found) + cursor.getInt(3), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.TOP, 0, 330);
                            toast.show();
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
            File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(sdCard, fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);

            // Начало основной загрузки
            String line;
            int j = 1;
            downloadList.add(fileName.substring(0, fileName.length() - 4));

            while ((line = reader.readLine()) != null) { //Считываем список с файла в Лист.
                byte[] bytes = line.getBytes();
                line = new String(bytes, StandardCharsets.UTF_8);
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
            reader.close();
            fileReader.close();
            viewDataForDownloading(); //образуем показ Загрузки и списка после того как он полностью загрузиться в Базу
            downloadList.clear();
            handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
            handler.sendEmptyMessage(hSetProgressBarGone);
            mProgresscounter = 0;

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
        } else if (item.getItemId() == R.id.menuID_ToSubscribe1) {
            Intent intent = new Intent(MainActivity.this, SubcribeClass1.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    public void showListIsReadyPercent() {
        try {
            double countMain = mainList.size();
            double countReady = hashSetMainCollectorItems.size();
            double persent = countReady * 100 / countMain;
            String persentString = String.valueOf(persent);
            toast = Toast.makeText(MainActivity.this, persentString.substring(0, 4) + "% " + getString(R.string.done), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 400);
            toast.show();
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, "0.0 % " + getString(R.string.done), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 350);
            toast.show();
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

        }

        return super.onContextItemSelected(item);
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
            loadhistory.clear();
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, R.string.fail_to_restore_previous_searching, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 330);//250
            toast.show();
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
            toast = Toast.makeText(MainActivity.this, R.string.search_error, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 340);//250
            toast.show();
            e.printStackTrace();
        }
    }

    public void toAppointSearchList(ArrayList<String> fromWhereSearchlist, ArrayList<String> toWhereList, String searchWord) {
        try {
            while (true) {
                for (String name : fromWhereSearchlist) {
                    if (name.toLowerCase().contains(searchWord.toLowerCase())) { // загружаем слова которые нашли в лист
                        toWhereList.add(name);
                    }
                }
                if (!toWhereList.isEmpty()) {
                    Log.i(TAG, "toAppointSearchList: list не пустой");
                    break;
                } else if (searchWord.length() <= 2) {
                    Log.i(TAG, "toAppointSearchList: Слово слишкок мало уходим от сюда");
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
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, R.string.toappoint_search_list_error, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 340);//250
            toast.show();
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
            toast = Toast.makeText(MainActivity.this, R.string.load_checked_items_error, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 340);//250
            toast.show();
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
        if (dbHelper == null) dbHelper = new DBHelper(this);
        if (contentValues == null) contentValues = new ContentValues();
        if (handler == null) onHandlerCreate();

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


}