package com.example.android.sqlitekod_dev_test;

import static com.example.android.sqlitekod_dev_test.R.color.valmis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;
    EditText etName, etChageStringg,etNameOf_hide;
    Button btnSave, loadText, btnDeleteAll, btnJaak, btnChange, btnSearch, btnBackSearch;
    ListView list_of_View;   //Лист куда закидываеться основной при помощи адаптера (adapter1)
    ArrayList<String> mainList; //основной лист
    ArrayList<String> ListFromSharedPreference = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Main
    ArrayList<String> ListForTryingCatchPossition = new ArrayList<>(); //для показа примерной позиции , берёт начало от mainList в setOnItemCL (и чистица в Delete общем и одиночном)
    ArrayList<String> listForSearch = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Search
    ArrayList<String> backSearchlist = new ArrayList<>();
    ArrayList<String> downloadList = new ArrayList<>();  // - Куда считываеться значально тексты с файла а потом с него загружаем в Базу
    ArrayList<String> foundAccurateList = new ArrayList<>();// Используеться в более точном поиске
    ArrayList<Integer> positionofIndex = new ArrayList<>(); // Используеться в LoadCheckedItems для обозначения позиций.

    /*test*/
    ArrayList<String> found_List = new ArrayList<>();
    String nameOf_etname = "";
    public static String model,hideName = "";
    public static String name = "";
    HashSet<String> HashSetMainCollectorItems = new HashSet<>(); // главный подсчёт выделяемых itemov в setOnItemCL
    int backcounter = 0;  //backcounter - работает с backSearchlist
    int lan = 0; // используеться в Остатке btnJaak. значение работает с mainList
    int counter = 0; //для повторений запроса в первом элементе etName.
    boolean permisionGranted = false; //permisionGranted специальная переменная для удаленя файла на носителе.


    //Примечание: mainlist и foundlist основные.В list_of_View отбражаться всё что есть в mainlist или foundlist с помощью  adapter1.

    SharedPreferences sPref;
    final String TAG = "mylogs";
    public static String choosen_ItemInClickmethod = ""; // Выделяемые View преобразуються в String в setOnItemCL. После checked_Items работает с hset
    /*test*/

    ArrayAdapter adapter1;   //главный Адаптер Он закидывает значения с mainList во ViewList тоесть list_of_View.
    Cursor cursor;
    ContentValues contentValues = new ContentValues();
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

        loadText = findViewById(R.id.loadtext);
        loadText.setOnClickListener(this);

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

//Нажатие на Item
        list_of_View.setOnItemClickListener(this);
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
                Toast.makeText(MainActivity.this, "Position from DataBase " + cursor.getInt(3), Toast.LENGTH_SHORT).show();
                cursor.close();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error with Cursor ", Toast.LENGTH_SHORT).show();
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

                if (etName.length() == 0 && etChageStringg.length() == 0  && etNameOf_hide.length() == 0) {
                    found_List.clear();
                    foundAccurateList.clear();
                    //Если все значения были пустыми то чистим лист и обновляем его  с проставлением checked
                    mainList.clear();
                    viewData(); // - В этом методе востанавливаться mainList

                    LoadCheckedItems(mainList); // Загрузка

                    if (HashSetMainCollectorItems.isEmpty()) {
                        //востановления Аррай листа
                        HashSetMainCollectorItems.addAll(ListFromSharedPreference); // сохранённые значения которые были актуальны в момент нажатия обновить, передаём hset.
                    }
                    choosen_ItemInClickmethod = "";
                    if (counter >= 2){ //эта функция для того что бы часто не забивать в поиск одно и тоже слово
                        etName.setText(nameOf_etname);
                    }
                    Toast.makeText(MainActivity.this, "UpDate is successfully", Toast.LENGTH_SHORT).show();
                } else {
                    etChageStringg.setText("");
                    etName.setText("");
                    if (hideName.length() > 0) {// Если в скрытом edit text что то было то  создаём новый Item.
                        dbHelper.insertData(hideName);// шаг 4
                    }
                    nameOf_etname = "";
                    Toast.makeText(this, "Clear Object", Toast.LENGTH_SHORT).show();
                }
                break;

//Удаление
            case R.id.btnDeleteAll:
                String del = etName.getText().toString();
                String delete = "Delete";
                if (del.equals(delete)) { // Если Ввёл в строку Delete
                    try {
                        permisionGranted = true;
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        sqLiteDatabase.delete(DBHelper.TABLE_CONTACT, null, null);

                        for (int i = 0; i < 2; i++) {
                            File file = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser");
                            File file2 = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser-journal");

                            file.delete();
                            file2.delete(); //data/data/com.example.android.sqlitekod_dev_test/shared_prefs/SAVE.xml
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Строка 292 Попытка удалить внутренние файлы DBNeiser и DBNeiser-journal " + e.getMessage());
                        Toast.makeText(MainActivity.this, "File is not", Toast.LENGTH_SHORT).show();
                    }

                    try {
                        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                        sPref.edit().clear().apply();
                        sqLiteDatabase.close();
                        dbHelper.close();
                        cursor.close();
                        ListFromSharedPreference.clear();
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
                        Toast.makeText(MainActivity.this, "Something gone wrong", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(MainActivity.this, "Deleted is successfully", Toast.LENGTH_SHORT).show();
                } else if (!del.isEmpty()) { // Если Строка не пустая , а с каким то значением
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
                    etName.setText("Delete");
                    Toast.makeText(this, "Add in name 'Delete' or number", Toast.LENGTH_SHORT).show();
                }
                break;

//Загрузка
            case R.id.loadtext:
                if (!mainList.isEmpty() || !found_List.isEmpty()) {
                    Toast.makeText(MainActivity.this, "For loading a new Base is need to delete old", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }//продолжение кода в onRequestPermissionsResult (Код ниже)
                break;
//Остаток
            case R.id.btnJaak:
                if (name.equals("Rest")) { //Что бы сработал нужно ввести
                    mainList.clear();
                    viewData();
                    restCreating();
                    etName.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "Put in name ' Rest ' ", Toast.LENGTH_SHORT).show();
                }
                break;

//Изменение
            case R.id.btnChange:
                boolean a = false;
                boolean b = false;
                if (etChageStringg.length() != 0 && !choosen_ItemInClickmethod.isEmpty()) {
                    String changed = " Changed";
                    if (choosen_ItemInClickmethod.contains(changed)){ // Если строке уже было присвоенно Changed то оно не добавляеться сново.
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
                    }else if (!b){
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
                    }else {
                        sqLiteDatabase.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{choosen_ItemInClickmethod});
                        Toast.makeText(MainActivity.this, "String was changed", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Странный");
                        a = true;
                        etChageStringg.setText("");
                        btnSave.callOnClick();
                    }}
                } else if (choosen_ItemInClickmethod.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Choose item", Toast.LENGTH_SHORT).show();
                } else if (!a) {// если ничего из изменений не выполнялось тогда просто закидывает строку. Для того что бы начать менять её.
                    etChageStringg.setText(choosen_ItemInClickmethod);

                    // Пробую изменять вид кнопки
                    /*DisplayMetrics metrics = new DisplayMetrics();
                    MainActivity.this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int height = metrics.heightPixels;
                    ViewGroup.LayoutParams params = btnJaak.getLayoutParams();
                    params.width = metrics.widthPixels;

                    Log.d(TAG, "onClick: params width = " + params.width);
                    btnJaak.setLayoutParams();
                    try {
                        double ratio = 0.5;
                        params.height = Double.valueOf(ratio * height).intValue();
                        yourButton.setLayoutParams(params);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/


                }
                break;
//поиск
            case R.id.btnSearch:
                if (name == null || name.length() == 0){
                    Toast.makeText(this,"Write something in the place \"Type item\" ",Toast.LENGTH_LONG).show();
                }else {
                    String et = etName.getText().toString(); // Берём в переменную тк  в onQueryTextSubmit значение сбивается.
                    onQueryTextSubmit(et);

                     //Далее Логика для истории поиска, сохранения 4 последних поисковых запросов
                    backcounter = 0;
                    if (!backSearchlist.contains(et)){ // 1) Если в списке элемент не содержиться
                        if (backSearchlist.size() >= 4){  // 3) Если лист больше 4 значений то пересорировываем
                            backSearchlist.add(0,et);
                            backSearchlist.remove(4);
                        }else {
                            backSearchlist.add(et); // 2) Просто добавляем
                        }
                    }
                }
                break;
 //История поиска
            case R.id.IdBackSearch:
                btnSave.callOnClick();
                if (backSearchlist.isEmpty()){ //Если чист то прерываем сеанс
                    break;
                }
                if (backcounter >= 4){
                    backcounter = 0; // что бы получаемый контент не ушёл за рамки возможного
                }

                try {
                    etName.setText(backSearchlist.get(backcounter));
                }catch (Exception e){
                    Log.d(TAG, "onClick: ERROR - " + e.getMessage());
                }

                if (backSearchlist.size() > ++backcounter ){
                    //Ничего не делаем тк переменная backcounter уже увеличилась в if позиции.
                    Log.d(TAG, "onClick: btnbackSearch зашли в увеличение Counter. " +
                            "List size: "+ backSearchlist.size()+
                            "  backcounterValue is: " + backcounter);
                }else {
                    backcounter = 0;
                }
                break;


        }
        dbHelper.close();

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
    }

    @Override //после получения доступа Загружаем базу данных
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permisionGranted){ // permisionGranted специальная переменная для удаления файла на носителе.
            switch (requestCode){
                case 1: // подготовка к удалению файла
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(sdCard, "Plan.txt");
                        try {
                            if (file.delete()){
                                Log.d(TAG, "onRequestPermissionsResult:  deleted");
                            }else {
                                Log.d(TAG, "onRequestPermissionsResult:  file doesn't deleted");
                            }
                        }catch (Exception e){
                            Log.d(TAG, "onRequestPermissionsResult: " + e.getMessage() );
                            Toast.makeText(MainActivity.this, "File doesn't Deleted", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }

            }
        }
        if (!permisionGranted) {
            switch (requestCode) {
                case 1:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (!mainList.isEmpty()) {
                            Toast.makeText(MainActivity.this, "DataBase is full", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                Log.d(TAG, sdCard.getAbsolutePath());
                                File file = new File(sdCard, "Plan.txt");
                                Log.d(TAG, file.getAbsolutePath());

                                FileReader fileReader = new FileReader(file);
                                BufferedReader reader = new BufferedReader(fileReader);

                                //Дополнительная подгрузка остатка (код ниже)
                                try {
                                    sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
                                        int luk = sPref.getInt("Kol-jaak", 0);
                                        if (luk > 0) {
                                            for (int i = 0; i < luk; i++) {
                                                String s = sPref.getString("Rest" + i, "");
                                                downloadList.add(s + "The_Rest");
                                            }
                                            sPref.edit().clear().apply();
                                            for (int i = 0; i < downloadList.size(); i++) {
                                                etNameOf_hide.setText(downloadList.get(i));
                                                btnSave.callOnClick();
                                                etNameOf_hide.setText("");
                                            }
                                            downloadList.clear();
                                            Log.d(TAG, "Считал данные с доп загрузки");
                                        }else sPref = null;
                                    } catch(Exception e){
                                        e.printStackTrace();
                                        Log.d(TAG, "Ошибка при чтении доп-загрузки");
                                        Toast.makeText(MainActivity.this, "Ошибка при чтении доп-загрузки", Toast.LENGTH_SHORT).show();

                                    }
                                //доп загрузка закончена


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
                                for (int i = 0; i < downloadList.size(); i++) {
                                    etNameOf_hide.setText(downloadList.get(i));
                                    btnSave.callOnClick();
                                    etNameOf_hide.setText("");
                                }
                                reader.close();
                                fileReader.close();
                                downloadList.clear();
                                viewData(); //образуем показ списка после того как он полностью загрузиться в Базу
                                Log.d(TAG, "Считала новый план");

                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, "Ошибка при чтении файла!", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
            }
        }
        permisionGranted = false;
    }

    /*Создаём меню и регистрируем там поиск*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menuxml, menu);

      /*  MenuItem searchItem = menu.findItem(R.id.item_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);*/

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_ready: //смотреть процент готовности плана
                try {
                    double countMain = mainList.size();
                    double countReady = HashSetMainCollectorItems.size();
                    double persent = countReady * 100 / countMain;
                    String persentString = String.valueOf(persent);
                    Toast.makeText(MainActivity.this, "Persent is done " + persentString.substring(0,4), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this,"Persent is done  0.0",Toast.LENGTH_LONG).show();
                }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        switch (v.getId()) {
            case R.id.list_item_model:
                getMenuInflater().inflate(R.menu.menuitem, menu);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }



    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
                sPref.edit().clear().apply();
                Toast.makeText(MainActivity.this, "The rest memory was Deleted ", Toast.LENGTH_SHORT).show();
                break;
        }

                return super.onContextItemSelected(item);
        }


    @Override//для поиска
    public boolean onQueryTextSubmit(String s) { //метод для поиска слова (присвоил к кнопке)
        if (nameOf_etname.equals(s)){ // эта функция для частых вводных слов в поиск.
            counter++;
        }else{
            counter = 0;
        }
        nameOf_etname = s;
        //Тут начинаеться прыжки между двумя if else, если поиск продолжаться с уже найденного листа
        if (!found_List.isEmpty()){
            ToAppointSearchList(found_List,foundAccurateList,s);
            //ToAppointSearchList(C какого листа идёт выборка, в какой лист переносяться найденные строки,Поисковое слово)
            Log.d(TAG, "Sessia - !found_List " + found_List.size() );
        }else if (!foundAccurateList.isEmpty()){
            ToAppointSearchList(foundAccurateList,found_List,s);
            Log.d(TAG, "Sessia - !foundAccurateList " + found_List.size() );
        }else {
            ToAppointSearchList(mainList,found_List,s);
            // ToAppointSearchList(C какого листа идёт выборка, в какой лист переносяться найденные строки,Поисковое слово)
            Log.d(TAG, "Sessia - !mainList " + mainList.size() );
        }
        return true;
    }
    public void ToAppointSearchList(ArrayList<String> fromWhereSearchlist, ArrayList<String> toWhereList, String searchWord){

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

    }// для поиска
    public void LoadCheckedItems(ArrayList<String> list){

        //UP-date Main
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        int kol = sPref.getInt("Kolichesvo", 0);
        if (ListFromSharedPreference.isEmpty()) {
            for (int i = 0; i < kol; i++) {
                ListFromSharedPreference.add(sPref.getString("Keyg" + i, "")); //загрузка с Preferences
            }
        }

        String mainline = "";
        for (int i = 0; i < list.size(); i++) {             //Поиск выбранных(загруженных) елементов в основном листе
            for (int g = 0; g < ListFromSharedPreference.size(); g++) {
                if (list.get(i).contains(ListFromSharedPreference.get(g))) {
                     mainline = list.get(i);                //Найденный елемент приобразуем в строку и передаём переменной
                    positionofIndex.add(list.indexOf(mainline));   // в Лист integer заполняем  индексами.
                    break;
                }
            }
        }


        for (int i = 0; i < positionofIndex.size(); i++) {
            list_of_View.setItemChecked(positionofIndex.get(i), true); //Чек итемов которые были полученны.
        }
        ListFromSharedPreference.clear();
        positionofIndex.clear();
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }
}