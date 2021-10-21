package com.example.android.sqlitekod_dev_test;

import static com.example.android.sqlitekod_dev_test.R.color.valmis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener {
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    EditText etName, etChageStringg, etNameOf_hide;
    Button btnSave, btnReadFile, btnDeleteAll, btnJaak, btnChange, btnSearch, btnBackSearch;
    ArrayList<String> listFromSharedPreference = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Main
    ArrayList<String> ListForTryingCatchPossition = new ArrayList<>(); //для показа примерной позиции , берёт начало от mainList в setOnItemCL (и чистица в Delete общем и одиночном)
    ArrayList<String> listForSearch = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Search
    ArrayList<String> backSearchlist = new ArrayList<>();
    ArrayList<String> downloadList = new ArrayList<>();  // - Куда считываеться значально тексты с файла а потом с него загружаем в Базу
    ArrayList<Integer> positionofIndex = new ArrayList<>(); // Используеться в LoadCheckedItems для обозначения позиций.

    //ниже 3 листа ипользуемые в методе onQueryTextSubmit для поиска.
    //Примечание: mainlist и foundlist основные.В list_of_View отбражаться всё что есть в mainlist или foundlist с помощью  adapter1.
    ArrayList<String> mainList; //основной лист
    ArrayList<String> found_List = new ArrayList<>();
    ArrayList<String> foundAccurateList = new ArrayList<>();// Используеться в более точном поиске
    volatile ListView list_of_View;   //Лист куда закидываеться основной или поисковые листы при помощи адаптера (adapter1). Для отображения.
    volatile ArrayAdapter adapter1;   //Главный Адаптер Он закидывает значения с mainList, found_List, foundAccurateList во ViewList тоесть list_of_View.

    String nameOf_etname = ""; // переменая реализуеться в  btnChange.onclick, после изменения сразу перекидывает на тот Лист, который нашёл поиск. берёт значение из метода поиска.
    public static String model, hideName, name = "";
    HashSet<String> HashSetMainCollectorItems = new HashSet<>(); // главный подсчёт выделяемых item elements в setOnItemCLick.
    int backcounter = 0;  //backcounter - работает с backSearchlist
    int requestCode1 = 1;


    public static volatile String fileName;
    volatile ProgressBar progressBar;
    static volatile boolean fileOfNameReady, fileNotChoosed, isSaved, permisionGranted = false;
    //Выше созданная permisionGranted специальная переменная для удаленя файла на носителе.
    //isSaved переменная служит для сохранения остатка , что бы удаление не произошло раньше чем не сохраниться остаток.
    //fileNotChoosed переменная служит для остановки потока который хочет считать имя файла работает в паре fileOfNameReady. Приобретает свойсва true  в методе onRestart()

    volatile static int mProgresscounter = 0;
    //2,3,9 - зарезирвированы в DialogClass
    final static int hSetToastErrorOfFileReading = 1;
    final static int hSetbtnReadFileEnabledFalse = 4;
    final static int hSetbtnReadFileEnabledTrue = 5;
    final static int hSetProgressBarVisible = 6;
    final static int hSetProgressBarGone = 7;
    final static int hSetLoadingListOfView_fromAdapter1 = 11;
    final static int hSetToastErrorfromReadingAdditionalLoad = 12;


    Toast toast;
    SharedPreferences sPref;
    final String TAG = "mylogs";
    public static String choosen_ItemInClickmethod = ""; // Выделяемые View преобразуються в String в setOnItemCL. После checked_Items работает с HashSetMainCollectorItems


    Cursor cursor;
    ContentValues contentValues = new ContentValues();
    static Handler handler;
/*    работа с simple cursor adapter
    SimpleCursorAdapter scAdapter;
    String[] from = new String[] {DBHelper.KEY_NAME,DBHelper.KEY_MODEL,DBHelper.KEY_DATA};
    int[] to  = new int[] {R.id.textFirstName,R.id.textModel,R.id.textData};*/

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list_of_View = findViewById(R.id.list_item_model);
        list_of_View.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mainList = new ArrayList<>();

        /*иницализация кнопок*/
        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        btnReadFile = findViewById(R.id.loadtext);
        btnReadFile.setOnClickListener(this);

        btnJaak = findViewById(R.id.btnJaak);
        btnJaak.setOnClickListener(this);


        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(this);

        btnChange = findViewById(R.id.btnChange);
        btnChange.setOnClickListener(this);

        btnBackSearch = findViewById(R.id.IdBackSearch);
        btnBackSearch.setOnClickListener(this);

        /*находим Добавление view*/
        etName = findViewById(R.id.etName);
        etNameOf_hide = findViewById(R.id.etNameOf_hide);
        etChageStringg = findViewById(R.id.etChageString);

        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);


        dbHelper = new DBHelper(this);
        viewData();
        registerForContextMenu(list_of_View);
        btnSave.callOnClick();
        progressBar = findViewById(R.id.downloadBar);

//Нажатие на Item
        list_of_View.setOnItemClickListener(this);
        onHandlerCreate();
    }

    public void onHandlerCreate() {
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case 1:
                        toast = Toast.makeText(MainActivity.this, "Ошибка при чтении файла!", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
                        break;
                    case 2:
                        createRestWithSave();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                while (!isSaved) {
                                    SystemClock.sleep(1000);
                                    Log.i(TAG, "run: :Ждём пока isSave , будет true " + isSaved);
                                }
                                Log.i(TAG, "isSave = " + isSaved + "      run: пошло удаление");
                                btnDeleteAll.callOnClick();
                            }
                        });
                        break;
                    case 3:
                        deleteRestMemory();
                        btnDeleteAll.callOnClick();
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
                    case 9:
                        etName.setText("");
                        break;
                    case 11:
                        list_of_View.setAdapter(adapter1);
                        break;
                    case 12:
                        toast = Toast.makeText(MainActivity.this, "Ошибка при чтении доп-загрузки", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
                        break;
                    case 13:
                        toast = Toast.makeText(MainActivity.this, "Works", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 0, 330);//250
                        toast.show();
                        break;

                }
            }
        };
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        int green = getResources().getColor(valmis);
        if (ListForTryingCatchPossition.isEmpty()) { // используеться когда мы в основном списке
            ListForTryingCatchPossition.addAll(mainList);
        }
        choosen_ItemInClickmethod = ((TextView) view).getText().toString(); // Кликнутая строка в данный момент

        //поиск и отображение id
        try {
            if (!mainList.isEmpty()) { // функция для нахождения позиции кликнутого элемента в основном листе
                cursor = dbHelper.viewData();
                cursor.moveToPosition(position);
                toast = Toast.makeText(MainActivity.this, "Position from DataBase " + cursor.getInt(3), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 330);//250
                toast.show();
                cursor.close();
            }
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, "Error with Cursor " + cursor.getInt(3), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 330);//250
            toast.show();
        }
        //закончен поиск id

        if (HashSetMainCollectorItems.contains(choosen_ItemInClickmethod)) { // Основной Список Выбранных Элементов
            HashSetMainCollectorItems.remove(choosen_ItemInClickmethod);
        } else {
            HashSetMainCollectorItems.add(choosen_ItemInClickmethod);
        }

        /*Save stats in sharedPreferences*/

        ArrayList<String> gap = new ArrayList<>(HashSetMainCollectorItems);
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        sPref.edit().clear().apply();                             // чистим SharedPreferences

        for (int i = 0; i < HashSetMainCollectorItems.size(); i++) {
            editor.putString("Keyg" + i, gap.get(i));
        }
        editor.putInt("Kolichesvo", HashSetMainCollectorItems.size());// Заливаем новые данные в SharedPreferences
        editor.apply();
        gap.clear();
    }

    private void viewDataForDownloading() {
        cursor = dbHelper.viewData(); // Курсор в данном этапе дейсвует как список в котором храняться все строки с Базы данных
        if (cursor.getCount() == 0) {
            toast = Toast.makeText(MainActivity.this, "Not data to show" + cursor.getInt(3), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 330);
            toast.show();
        } else {
            while (cursor.moveToNext()) { // тут мы его считываем
                mainList.add(cursor.getString(0));
                mProgresscounter++;
                try {
                    TimeUnit.MILLISECONDS.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "viewDataForDownloading: mProgresscounter = " + mProgresscounter);
                handler.post(incrementProgressbar);

            }
            adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, mainList);
            handler.sendEmptyMessage(hSetLoadingListOfView_fromAdapter1);
            /*list_of_View.setAdapter(adapter1);*/
            cursor.close();
        }
    }

    //вывод в ListView отображения
    private void viewData() {
        cursor = dbHelper.viewData(); // Курсор в данном этапе дейсвует как список в котором храняться все строки с Базы данных
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "Not data to show", Toast.LENGTH_SHORT).show();
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
            fileOfNameReady = true;

            sPref = getSharedPreferences("FILENAME", MODE_PRIVATE);
            SharedPreferences.Editor editor1 = sPref.edit();
            editor1.putString("keyFileName", fileName);
            editor1.apply();


        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("permission", permisionGranted);
        outState.putBoolean("val1", fileOfNameReady);
        outState.putBoolean("val2", fileNotChoosed);
        outState.putBoolean("val3", isSaved);

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        permisionGranted = savedInstanceState.getBoolean("permission");
        fileOfNameReady = savedInstanceState.getBoolean("val1");
        fileNotChoosed = savedInstanceState.getBoolean("val2");
        isSaved = savedInstanceState.getBoolean("val3");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: Вызвана пауза  permisionGranted: " + permisionGranted);
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
        if (!fileOfNameReady) {
            fileNotChoosed = true;
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

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        name = etName.getText().toString(); //Сохранение в базу шаг 1
        model = etChageStringg.getText().toString();
        hideName = etNameOf_hide.getText().toString();
        sqLiteDatabase = dbHelper.getWritableDatabase();
        //ContentValues contentValues = new ContentValues(); //шаг 2
        switch (v.getId()) {
//Обновить
            case R.id.btnSave:
                //contentValues.put(DBHelper.KEY_NAME, hideName); //шаг 3
                if (etName.length() == 0 && etChageStringg.length() == 0 && etNameOf_hide.length() == 0) {
                    //Если все значения были пустыми то чистим все листы и обновляем основной с проставлением checked
                    found_List.clear();
                    foundAccurateList.clear();
                    mainList.clear();
                    viewData(); // - В этом методе востанавливаться mainList

                    LoadCheckedItems(mainList); // Загрузка

                    if (HashSetMainCollectorItems.isEmpty()) {
                        //востановления hashset листа
                        HashSetMainCollectorItems.addAll(listFromSharedPreference); // сохранённые значения которые были актуальны в момент нажатия обновить, передаём hset.
                    }

                    choosen_ItemInClickmethod = "";
                    showListIsReadyPercent();
                } else {
                    etChageStringg.setText("");
                    etName.setText("");
                    nameOf_etname = "";
                    Toast.makeText(this, "Clear Object", Toast.LENGTH_SHORT).show();
                }
                break;

//Удаление
            case R.id.btnDeleteAll:
                String del = etName.getText().toString();
                String delete = "Delete";
                if (del.equals(delete)) { // Если Ввёл в строку Delete
                    //permisionGranted = true;
                    try {
                       /* ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, null, null);*/

                        File file = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser");
                        File file2 = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser-journal");
                        Log.i(TAG, "onClick: Попытка удалить файлы Data base. Файл сущестует?" + file.exists());
                        file.delete();
                        file2.delete(); //data/data/com.example.android.sqlitekod_dev_test/shared_prefs/SAVE.xml
                        Log.i(TAG, "onClick: После удаления Data base. Файл сущестует?" + file.exists());

                    } catch (Exception e) {
                        Log.d(TAG, "Попытка удалить внутренние файлы DBNeiser и DBNeiser-journal " + e.getMessage());
                        Toast.makeText(MainActivity.this, "File is not", Toast.LENGTH_SHORT).show();
                    }

                    try {
                        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        sqLiteDatabase.close();
                        dbHelper.close();
                        cursor.close();
                        listFromSharedPreference.clear();
                        listForSearch.clear();
                        mainList.clear();
                        found_List.clear();
                        HashSetMainCollectorItems.clear();
                        choosen_ItemInClickmethod = null;
                        adapter1.clear();
                        nameOf_etname = null;
                        name = null;
                        ListForTryingCatchPossition.clear();
                        etName.setText("");
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(MainActivity.this, "Deleted is successfully", Toast.LENGTH_SHORT).show();
                }
                //Начинаеться метод если хотим удалить какой то отдельный элемент.
                else if (!del.isEmpty()) { // Если Строка не пустая , а с каким то значением
                    try {
                        int upadateCount1 = sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_ID + "= ?", new String[]{name});
                        System.out.println("Строк удаленно " + upadateCount1);
                        ListForTryingCatchPossition.clear();
                        etName.setText("");
                        Toast.makeText(MainActivity.this, "Row is deleted " + upadateCount1, Toast.LENGTH_SHORT).show();
                        btnSave.callOnClick();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error deleted", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {

                    DialogClass dialogClass = new DialogClass(MainActivity.this,
                            "We can put unselected items in memory. And load them with the next loading Data.",
                            "How Delete",
                            "Delete",
                            "Delete with Rest",
                            "Cancel");
                    dialogClass.createStandartNewDialogOKCancelNetral();
                    dialogClass.dialog.show();
                    etName.setText("Delete");
                    //Toast.makeText(this, "Press Delete last one", Toast.LENGTH_SHORT).show();
                }
                break;
//Загрузка
            case R.id.loadtext:
                if (!fileOfNameReady) {
                    fileNotChoosed = false;
                }
                if (!mainList.isEmpty() || !found_List.isEmpty()) {
                    Toast.makeText(MainActivity.this, "List is already full", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    setResult(RESULT_OK, intent);
                    startActivityForResult(intent, requestCode1);
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!fileOfNameReady || Thread.currentThread().isInterrupted()) {
                                if (fileNotChoosed) {
                                    //if функция служит если файл не был выбран, включаться автоматом в onRestart
                                    break;
                                }
                                try {
                                    Log.i(TAG, "run: Зашли в режим сна, fileOfnameReady:  " + fileOfNameReady);
                                    Thread.sleep(3300);
                                } catch (InterruptedException e) {
                                    Log.i(TAG, "run: ошибка в новой нити: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            if (!fileNotChoosed && fileOfNameReady) { // если файл
                                Log.i(TAG, "run: Получили имя файла идём в загрузку,fileOfNameReady: " + fileOfNameReady);
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            }else {
                                toast = Toast.makeText(MainActivity.this, "Error fileNotChoosed: " + fileNotChoosed + ", fileOfNameReady: "+ fileOfNameReady, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.TOP, 0, 330);//250
                                toast.show();

                            }
                        }
                    });
                    t.start();
                }
                break;
//Остаток
            case R.id.btnJaak:
                if (name.equals("Rest")) { //Что бы сработал нужно ввести
                    /* createRestWithSave();*/
                    createUncheckedViewList();
                } else {
                    Toast.makeText(MainActivity.this, "Put in name ' Rest ' ", Toast.LENGTH_SHORT).show();
                }
                break;
//Изменение
            case R.id.btnChange:
                LayoutInflater inflater = this.getLayoutInflater();
                DialogClass dialogClass = new DialogClass(MainActivity.this,
                        "Lets do change",
                        inflater
                        );
                dialogClass.createCustomNewDialogChageitem();
                dialogClass.dialog.show();



/*                boolean a = false;
                boolean b = false;
                if (etChageStringg.length() != 0 && !choosen_ItemInClickmethod.isEmpty()) {
                    String changed = " Changed";
                    if (choosen_ItemInClickmethod.contains(changed)) { // Если строке уже было присвоенно Changed то оно не добавляеться сново.
                        contentValues.put(DBHelper.KEY_NAME, model);
                        if (!found_List.isEmpty()) {
                            sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});
                            Toast.makeText(MainActivity.this, "If was changed  ", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "If was changed");
                            a = true;
                            b = true;
                            Log.d(TAG, "Change   etname " + nameOf_etname);
                            etChageStringg.setText("");
                            btnSave.callOnClick();
                            etName.setText(nameOf_etname); //сюда закидываються слова которые были в поиске
                            btnSearch.callOnClick();
                        }
                    } else if (!b) {
                        contentValues.put(DBHelper.KEY_NAME, model + changed);
                        if (!found_List.isEmpty()) {    //Если Пойсковый лист не пустой то данные заменяються и запускаеться обновление
                            int upadateCount = sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});

                            System.out.println("Строк обновленно " + upadateCount);
                            Toast.makeText(MainActivity.this, "If was not changed ", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "If was not changed");
                            a = true;
                            etChageStringg.setText("");
                            btnSave.callOnClick();
                            etName.setText(nameOf_etname); //сюда закидываються слова которые были в поиске
                            btnSearch.callOnClick();
                        } else {
                            sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});
                            Toast.makeText(MainActivity.this, "String was changed", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Странный");
                            a = true;
                            etChageStringg.setText("");
                            btnSave.callOnClick();
                        }
                    }
                } else if (choosen_ItemInClickmethod.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Choose item", Toast.LENGTH_SHORT).show();
                } else if (!a) {// если ничего из изменений не выполнялось тогда просто закидывает строку. Для того что бы начать менять её.
                    etChageStringg.setText(choosen_ItemInClickmethod);
                }*/
                break;
//поиск
            case R.id.btnSearch:
                if (name == null || name.length() == 0) {
                    Toast.makeText(this, "Write something in the place \"Type item\" ", Toast.LENGTH_LONG).show();
                } else {
                    String et = etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                    onmyQueryTextSubmit(et);

                    //Далее Логика для истории поиска, сохранения 4 последних поисковых запросов
                    backcounter = 0;
                    if (!backSearchlist.contains(et)) { // 1) Если в списке элемент не содержиться
                        if (backSearchlist.size() >= 4) {  // 3) Если лист больше 4 значений то пересорировываем
                            backSearchlist.add(0, et);
                            backSearchlist.remove(4);
                        } else {
                            backSearchlist.add(et); // 2) Просто добавляем
                        }
                    }
                }
                break;

//История поиска
            case R.id.IdBackSearch:
                btnSave.callOnClick();
                if (backSearchlist.isEmpty()) { //Если чист то прерываем сеанс
                    break;
                }
                if (backcounter >= 4) {
                    backcounter = 0; // что бы получаемый контент не ушёл за рамки возможного
                }

                try {
                    etName.setText(backSearchlist.get(backcounter));// тут применяем
                } catch (Exception e) {
                    Log.d(TAG, "onClick: ERROR - " + e.getMessage());
                }

                if (backSearchlist.size() > ++backcounter) {
                    //Ничего не делаем тк переменная backcounter уже увеличилась в if позиции.
                    //Тут идёт очередная проверка что бы переменная далеко не ушла, иначе скидываем на ноль.
                    Log.d(TAG, "onClick: btnbackSearch зашли в увеличение Counter. " +
                            "List size: " + backSearchlist.size() +
                            "  backcounterValue is: " + backcounter);
                } else {
                    backcounter = 0;
                }
                break;


        }
        dbHelper.close();

    }

    public void createRestWithSave() {
        mainList.clear();
        viewData();
        restCreating();
        etName.setText("");
    }

    public void createUncheckedViewList() {
        ArrayList<String> jaak = new ArrayList<>(HashSetMainCollectorItems);
        for (int i = 0; i < jaak.size(); i++) {
            for (int g = 0; g < mainList.size(); g++) {
                if (jaak.get(i).equals(mainList.get(g))) {
                    mainList.remove(mainList.get(g));// удаляем с основного листа все item которые были checked
                }
            }
        }
    }

    private void restCreating() {
        //В этом методе main лист становиться меньше поэтому мы должны его востанавливать. что бы сново применять этот метод. Что бы работал корректно.
        sPref = getSharedPreferences("Jaak", MODE_PRIVATE);// удаляем старую версию остатка
        sPref.edit().clear().apply();
        // От сюда востанавливаем наш main

        ArrayList<String> jaak = new ArrayList<>(HashSetMainCollectorItems);
        for (int i = 0; i < jaak.size(); i++) {
            for (int g = 0; g < mainList.size(); g++) {
                if (jaak.get(i).equals(mainList.get(g))) {
                    mainList.remove(mainList.get(g));// удаляем с основного листа все item которые были checked
                }
            }
        }
        sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
        SharedPreferences.Editor editor1 = sPref.edit();
        for (int i = 0; i < mainList.size(); i++) {
            editor1.putString("Rest" + i, mainList.get(i));

        }
        editor1.putInt("Kol-jaak", mainList.size());
        editor1.apply();
        jaak.clear();
        isSaved = true;
    }

    @Override //после получения доступа Загружаем базу данных
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult: Переменная для удаления файла permisionGranted: " + permisionGranted);
        if (permisionGranted) { // permisionGranted специальная переменная для удаления файла на носителе.
            Log.i(TAG, "onRequestPermissionsResult:  Зашли в метод удаления файла.");
            sPref = getSharedPreferences("FILENAME", MODE_PRIVATE);
            fileName = sPref.getString("keyFileName", "");
            if (fileName == null || fileName.equals("")) {
                fileName = "Plan.txt";
            }
            switch (requestCode) {
                case 1: // подготовка к удалению файла
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(sdCard, fileName);
                        try {
                            if (file.delete()) {
                                Log.d(TAG, "onRequestPermissionsResult:  deleted");
                                sPref.edit().clear().apply();
                            } else {
                                Log.d(TAG, "onRequestPermissionsResult:  file doesn't deleted");
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onRequestPermissionsResult: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "File doesn't Deleted", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }

            }
        }//Логика удаления файла на носителе закончена.
        //далее начинаеться логика загрузки файла
        else if (!permisionGranted) {
            Log.i(TAG, "onRequestPermissionsResult: Загрузка файла : " + fileName);
            if (fileName == null) {
                return;
            }
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (fileName.substring(fileName.length() - 3, fileName.length()).equals("txt")) {
                        switch (requestCode) {
                            case 1:
                                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                    if (!mainList.isEmpty()) {
                                        Toast.makeText(MainActivity.this, "DataBase is full", Toast.LENGTH_SHORT).show();
                                    } else {
                                        handler.sendEmptyMessage(hSetbtnReadFileEnabledFalse);
                                        handler.sendEmptyMessage(hSetProgressBarVisible);
                                        loadingRest();
                                        mainloading();
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                                }
                        }
                    } else if (fileName.substring(fileName.length() - 3, fileName.length()).equals("xls")) {
                        Toast.makeText(MainActivity.this, "In works", Toast.LENGTH_LONG).show();
                    }
                }
            });
            thread.start();
        }
        permisionGranted = false;//для удаления файла на устройстве
        fileOfNameReady = false;
    }

    Runnable incrementProgressbar = new Runnable() {
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
            String line = "";
            int j = 1;

            while ((line = reader.readLine()) != null) { //Считываем список с файла в Лист.
                if (downloadList.contains(line)) {
                    downloadList.add(line + j);
                    j++;
                } else {
                    downloadList.add(line);
                }
            }
            progressBar.setMax(downloadList.size() + mProgresscounter); // так как переменная volantile все изменения будут видны в любом потоке.
            Log.i(TAG, "mainloading: progress bar MAX = " + (downloadList.size() + mProgresscounter));
            for (int i = 0; i < downloadList.size(); i++) {
                //начал добавлять сразу в базу что бы не нагружать main.
                dbHelper.insertData(downloadList.get(i));
            }
            reader.close();
            fileReader.close();
            viewDataForDownloading(); //образуем показ списка после того как он полностью загрузиться в Базу
            downloadList.clear();
            handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
            handler.sendEmptyMessage(hSetProgressBarGone);
            mProgresscounter = 0;

        } catch (IOException e) {
            handler.sendEmptyMessage(hSetToastErrorOfFileReading);
            handler.sendEmptyMessage(hSetbtnReadFileEnabledTrue);
            handler.sendEmptyMessage(hSetProgressBarGone);
            e.printStackTrace();
        }

    }

    private void loadingRest() {
        try {
            sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
            Log.i(TAG, "loadingRest: sPref = " + sPref);
            int luk = sPref.getInt("Kol-jaak", 0);
            if (luk > 0) {
                for (int i = 0; i < luk; i++) {
                    String s = sPref.getString("Rest" + i, "");
                    downloadList.add(s + "The_Rest");
                }
                sPref.edit().clear().apply();
                for (int i = 0; i < downloadList.size(); i++) {
                    dbHelper.insertData(downloadList.get(i));
                }
                mProgresscounter = downloadList.size();
                downloadList.clear();
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

    public void showListIsReadyPercent() {
        try {
            double countMain = mainList.size();
            double countReady = HashSetMainCollectorItems.size();
            double persent = countReady * 100 / countMain;
            String persentString = String.valueOf(persent);
            toast = Toast.makeText(MainActivity.this, persentString.substring(0, 4) + " done", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 400);
            toast.show();
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, "0.0 done", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 350);
            toast.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        }
        return super.onOptionsItemSelected(item);
    }


    public void deleteRestMemory() {
        try {
            sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
            sPref.edit().clear().apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //на рассмотрении

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        //на рассмотрении
        return super.onContextItemSelected(item);
    }

    //для поиска
    public void onmyQueryTextSubmit(String s) { //метод для поиска слова (присвоил к кнопке)
        try {
            nameOf_etname = s;
            //Тут начинаеться прыжки между двумя if else, если поиск продолжаться с уже найденного листа
            if (!found_List.isEmpty()) {
                ToAppointSearchList(found_List, foundAccurateList, s);
                //ToAppointSearchList(C какого листа идёт выборка, в какой лист переносяться найденные строки,Поисковое слово)
                Log.d(TAG, "Sessia - !found_List " + found_List.size());
            } else if (!foundAccurateList.isEmpty()) {
                ToAppointSearchList(foundAccurateList, found_List, s);
                Log.d(TAG, "Sessia - !foundAccurateList " + found_List.size());
            } else {
                ToAppointSearchList(mainList, found_List, s);
                // ToAppointSearchList(C какого листа идёт выборка, в какой лист переносяться найденные строки,Поисковое слово)
                Log.d(TAG, "Sessia - !mainList " + mainList.size());
            }
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, "Error onmyQueryTextSubmit", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 340);//250
            toast.show();
            e.printStackTrace();
        }
    }

    public void ToAppointSearchList(ArrayList<String> fromWhereSearchlist, ArrayList<String> toWhereList, String searchWord) {

        try {
            for (String name : fromWhereSearchlist) {
                if (name.toLowerCase().contains(searchWord.toLowerCase())) { // загружаем слова которые нашли в лист
                    toWhereList.add(name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_checked, toWhereList);
            list_of_View.setAdapter(adapter);
            fromWhereSearchlist.clear();

            LoadCheckedItems(toWhereList);
            etName.setText("");
        } catch (Exception e) {
            toast = Toast.makeText(MainActivity.this, "Error ToAppointSearchList", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 340);//250
            toast.show();
            e.printStackTrace();
        }

    }// для поиска

    public void LoadCheckedItems(ArrayList<String> list) {
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
            toast = Toast.makeText(MainActivity.this, "Error LoadCheckedItems", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 340);//250
            toast.show();
            e.printStackTrace();
        }


    }


}