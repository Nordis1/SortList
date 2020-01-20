package com.example.android.sqlitekod_dev_test;

import android.Manifest;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import static com.example.android.sqlitekod_dev_test.DBHelper.KEY_MODEL;
import static com.example.android.sqlitekod_dev_test.R.color.valmis;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {
    DBHelper dbHelper;
    SQLiteDatabase db;
    EditText etName, etModel, etsecond,etNameOf_hide;
    Button btnSave, loadtext, btnDeleteAll, btnJaak, btnChange, btnsearch;
    ListView list_of_View;   //Лист куда закидываеться Arlismodel при помощи адаптера (adapter1)
    ArrayList<String> mainList;     //основной лист, в прошлом был Arlistmodel
    ArrayList<String> listFromPreferenses = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Main
    ArrayList<String> JustList = new ArrayList<>(); //для показа примерной позиции , берёт начало от mainList в setOnItemCL (и чистица в Delete общем и одиночном)
    ArrayList<String> listForSearch = new ArrayList<>(); // лист куда закидываться инфа с SharedPreferences up Search

    /*test*/
    ArrayList<String> found_List = new ArrayList<>();
    String nameOf_etname = "";
    String nameOf_etsecond = "";
    public static String model,hideName = "";
    public static String name = "";
    HashSet<String> hset = new HashSet<>(); // главный подсчёт выделяемых itemov в setOnItemCL
    int abra = 0; //значение рабоает с hset
    int lan = 0; // используеться в Остатке btnJaak. значение работает с mainList

    SharedPreferences sPref;
    final String LOG_TAG = "mylogs";
    public static String checked_Items = ""; // Выделяемые View преобразуються в String в setOnItemCL. После checked_Items работает с hset
    /*test*/

    ArrayAdapter adapter1;   //главный Адаптер Он закидывает значения с mainList во ViewList тоесть list_of_View.
    Cursor cursor;
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

        loadtext = findViewById(R.id.loadtext);
        loadtext.setOnClickListener(this);

        btnJaak = findViewById(R.id.btnJaak);
        btnJaak.setOnClickListener(this);


        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(this);

        btnChange = findViewById(R.id.btnChange);
        btnChange.setOnClickListener(this);

        /*находим Добавление view*/
        etName = findViewById(R.id.etName);
        etNameOf_hide = findViewById(R.id.etNameOf_hide);
        etModel = findViewById(R.id.etModel);
        etsecond = findViewById(R.id.secondSearch);

        btnsearch = findViewById(R.id.btnSearch);
        btnsearch.setOnClickListener(this);


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
        if (JustList.isEmpty()) {
            for (int i = 0; i < mainList.size(); i++) {
                JustList.add(mainList.get(i));
            }
        }
        checked_Items = ((TextView) view).getText().toString();

        try {
            if (mainList.isEmpty()) {
                for (int i = 0; i < JustList.size(); i++) {
                    String gg = JustList.get(i);
                    if (gg.equals(checked_Items)) {
                        int gif = JustList.size();
                        Toast.makeText(MainActivity.this, " Pos Approximately = " + (i + 1) + "; List 1 - " + gif, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error with JustList ", Toast.LENGTH_SHORT).show();
        }

        //поиск и отображение id
        try {
            if (!mainList.isEmpty()) {
                cursor = dbHelper.viewData();
                cursor.moveToPosition(position);
                Toast.makeText(MainActivity.this, "Position from DataBase " + cursor.getInt(3), Toast.LENGTH_SHORT).show();
                cursor.close();
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error with Cursor ", Toast.LENGTH_SHORT).show();
        }
        //закончен поиск id

        if (hset.contains(checked_Items)) {
            hset.remove(checked_Items);
        } else {
            hset.add(checked_Items);
        }
        abra = hset.size();


        /*Save stats in sharedPreferences*/
        Log.d(LOG_TAG, "запись в SharedPreferences");
        ArrayList<String> gap = new ArrayList<>(hset);
        sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        sPref.edit().clear().apply();
        for (int i = 0; i < hset.size(); i++) {
            editor.putString("Keyg" + i, gap.get(i));
        }
        editor.putInt("Kolichesvo", abra);

        editor.commit();
        gap.clear();

    }

    //вывод в ListView отображения
    private void viewData() {
        cursor = dbHelper.viewData();
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "Not data to show", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                mainList.add(cursor.getString(0));

            }
            adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, mainList);
            list_of_View.setAdapter(adapter1);
            cursor.close();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        name = etName.getText().toString();
        model = etModel.getText().toString();
        hideName = etNameOf_hide.getText().toString();


        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        switch (v.getId()) {

//Обновить
            case R.id.btnSave:
                contentValues.put(DBHelper.KEY_NAME, name);
                contentValues.put(DBHelper.KEY_NAME, hideName);
                //contentValues.put(KEY_MODEL, model);

                if (etName.length() == 0 && etModel.length() == 0 && etsecond.length() == 0 && etNameOf_hide.length() == 0) {
                    mainList.clear();
                    viewData();

                    //UP-date Main
                    sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                    int kol = sPref.getInt("Kolichesvo", abra);
                    if (listFromPreferenses.isEmpty()) {
                        for (int i = 0; i < kol; i++) {
                            listFromPreferenses.add(sPref.getString("Keyg" + i, "")); //загрузка с Preferences
                        }
                    }
                    System.out.println(listFromPreferenses.size() + " UP - size");
                    ArrayList<Integer> positionofIndex = new ArrayList<>();
                    for (int i = 0; i < mainList.size(); i++) {             //Поиск выбранных(загруженных) елементов в основном листе
                        for (int g = 0; g < listFromPreferenses.size(); g++) {
                            if (mainList.get(i).contains(listFromPreferenses.get(g))) {
                                String o = mainList.get(i);                //Найденный елемент приобразуем в строку и передаём переменной
                                positionofIndex.add(mainList.indexOf(o));   // в Лист integer заполняем  индексами.
                                break;
                            }
                        }
                    }
                    for (int i = 0; i < positionofIndex.size(); i++) {
                        list_of_View.setItemChecked(positionofIndex.get(i), true); //Чек итемов которые были полученны.
                    }

                    if (hset.isEmpty()) {
                        for (String nn : listFromPreferenses) { //востановления Аррай листа
                            hset.add(nn);
                        }
                    }
                    positionofIndex.clear();
                    listFromPreferenses.clear();
                    found_List.clear();
                    checked_Items = "";
                    Log.d(LOG_TAG, "Только что зашли в save/update при условии что все поля пусты  etname" + nameOf_etname + " etSecond "+ nameOf_etsecond);
                    Toast.makeText(MainActivity.this, "UpDate is successfully", Toast.LENGTH_SHORT).show();
                } else {
                    etModel.setText("");
                    etsecond.setText("");
                    etName.setText("");

                    dbHelper.insertData(hideName);
                    mainList.clear();
                    viewData();

                    nameOf_etname = "";
                    nameOf_etsecond = "";
                    Toast.makeText(this, "Clear Object", Toast.LENGTH_SHORT).show();
                }
                break;

//Удаление
            case R.id.btnDeleteAll:
                String del = etName.getText().toString();
                String delete = "Delete";
                if (del.equals(delete)) { // Если Ввёл в строку Delete
                    db.delete(DBHelper.TABLE_CONTACT, null, null);
                    try {
                        for (int i = 0; i < 2; i++) {
                            File file = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser");
                            File file2 = new File("data/data/com.example.android.sqlitekod_dev_test/databases/DBNeiser-journal");
                            file.delete();
                            file2.delete();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "File is not", Toast.LENGTH_SHORT).show();
                    }
                    sPref = getSharedPreferences("SAVE", MODE_PRIVATE);
                    sPref.edit().clear().apply();
                    db.close();
                    dbHelper.close();
                    cursor.close();
                    listFromPreferenses.clear();
                    listForSearch.clear();
                    mainList.clear();
                    found_List.clear();
                    hset.clear();
                    checked_Items = null;
                    adapter1.clear();
                    abra = 0;
                    lan = 0;
                    nameOf_etname = null;
                    name = null;
                    JustList.clear();
                    etName.setText("");
                    Toast.makeText(MainActivity.this, "Deleted is successfully", Toast.LENGTH_SHORT).show();
                } else if (!del.isEmpty()) { // Если Строка не пустая , а с каким то значением
                    try {
                        int upadateCount1 = db.delete(DBHelper.TABLE_CONTACT, DBHelper.KEY_ID + "= ?", new String[]{name});
                        System.out.println("Строк удаленно " + upadateCount1);
                        JustList.clear();
                        etName.setText("");
                        Toast.makeText(MainActivity.this, "Row is deleted " + upadateCount1, Toast.LENGTH_SHORT).show();
                        btnSave.callOnClick();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error deleted", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "Add in name 'Delete' or number", Toast.LENGTH_SHORT).show();
                }
                break;

//Загрузка
            case R.id.loadtext:
                if (!mainList.isEmpty() || !found_List.isEmpty()) {
                    Toast.makeText(MainActivity.this, "DataBase is full", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }//продолжение кода в onRequestPermissionsResult (Код ниже)
                break;
//Остаток
            case R.id.btnJaak:
                if (name.equals("Rest")) { //Что бы сработал нужно ввести
                    sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
                    sPref.edit().clear().apply();

                    mainList.clear();
                    viewData();

                    ArrayList<String> jaak = new ArrayList<>(hset);
                    for (int i = 0; i < jaak.size(); i++) {
                        for (int g = 0; g < mainList.size(); g++) {
                            if (jaak.get(i).equals(mainList.get(g))) {
                                mainList.remove(mainList.get(g));
                            }
                        }
                    }
                    sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
                    SharedPreferences.Editor editor1 = sPref.edit();
                    lan = mainList.size();
                    for (int i = 0; i < mainList.size(); i++) {
                        editor1.putString("naidis" + i, mainList.get(i));

                    }
                    editor1.putInt("Kol-jaak", lan);
                    editor1.commit();
                    jaak.clear();
                    etName.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "Put in name ' Rest ' ", Toast.LENGTH_SHORT).show();
                }
                break;

//Изменение
            case R.id.btnChange:
                boolean a = false;
                boolean b = false;
                if (etModel.length() != 0 && !checked_Items.isEmpty()) {
                    String changed = " Changed";
                    if (checked_Items.contains(changed)){ // Если строке уже было присвоенно Changed то оно не добавляеться сново.
                        contentValues.put(DBHelper.KEY_NAME, model);
                        if (!found_List.isEmpty()) {
                            int upadateCount = db.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{checked_Items});

                            System.out.println("Строк обновленно " + upadateCount);
                            Toast.makeText(MainActivity.this, "String was changed " + upadateCount, Toast.LENGTH_SHORT).show();
                            a = true;
                            b = true;
                            Log.d(LOG_TAG, "Change   etname " + nameOf_etname + " etSecond "+ nameOf_etsecond);
                            etModel.setText("");
                            btnSave.callOnClick();
                            etName.setText(nameOf_etname); //сюда закидываються слова которые были в поиске
                            etsecond.setText(nameOf_etsecond);// как для Имени  так и для второго значиния.
                            btnsearch.callOnClick();
                        }
                    }else if (!b){
                    contentValues.put(DBHelper.KEY_NAME, model + changed);
                    if (!found_List.isEmpty()) {    //Если Пойсковый лист не пустой то данные заменяються и запускаеться обновление
                        int upadateCount = db.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{checked_Items});

                        System.out.println("Строк обновленно " + upadateCount);
                        Toast.makeText(MainActivity.this, "String was changed " + upadateCount, Toast.LENGTH_SHORT).show();
                        a = true;
                        etModel.setText("");
                        btnSave.callOnClick();
                        etName.setText(nameOf_etname); //сюда закидываються слова которые были в поиске
                        etsecond.setText(nameOf_etsecond);// как для Имени  так и для второго значиния.
                        btnsearch.callOnClick();
                    }}else {
                        int upadateCount = db.update(DBHelper.TABLE_CONTACT, contentValues, DBHelper.KEY_NAME + "= ?", new String[]{checked_Items});

                        System.out.println("Строк обновленно " + upadateCount);
                        Toast.makeText(MainActivity.this, "String was changed999 " + upadateCount, Toast.LENGTH_SHORT).show();
                        a = true;
                        etModel.setText("");
                        btnSave.callOnClick();
                    }
                } else if (checked_Items.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Choose item", Toast.LENGTH_SHORT).show();
                } else if (a == false) {// если ничего из изменений не выполнялось тогда просто закидывает строку. Для того что бы начать менять её.
                    etModel.setText(checked_Items);
                }
                break;
            case R.id.btnSearch:
                onQueryTextSubmit(etName.getText().toString());
                break;
        }
        dbHelper.close();

    }

    @Override //после получения доступа Загружаем базу данных
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!mainList.isEmpty()) {
                        Toast.makeText(MainActivity.this, "DataBase is full", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            Log.d(LOG_TAG, sdCard.getAbsolutePath());
                            File file = new File(sdCard, "Plan.txt");
                            Log.d(LOG_TAG, file.getAbsolutePath());

                            FileReader fileReader = new FileReader(file);
                            BufferedReader reader = new BufferedReader(fileReader);

                            ArrayList<String> gap = new ArrayList<>();


                            //Дополнительная подгрузка остатка (код ниже)
                            try {
                                sPref = getSharedPreferences("Jaak", MODE_PRIVATE);
                                int luk = sPref.getInt("Kol-jaak", lan);
                                for (int i = 0; i < luk; i++) {
                                    String s = sPref.getString("naidis" + i, "");
                                    gap.add(s + "The_Rest");
                                }
                                sPref.edit().clear().apply();
                                for (int i = 0; i < gap.size(); i++) {
                                    etNameOf_hide.setText(gap.get(i));
                                    btnSave.callOnClick();
                                    etNameOf_hide.setText("");
                                }
                                gap.clear();
                                Log.d(LOG_TAG,"Считал данные с доп загрузки");
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(LOG_TAG,"Ошибка при чтении доп-загрузки");
                                Toast.makeText(MainActivity.this, "Ошибка при чтении доп-загрузки", Toast.LENGTH_SHORT).show();

                            }
                            //доп загрузка закончена


                            String g = "";
                            int j = 1;
                            while ((g = reader.readLine()) != null) {
                                if (gap.contains(g)) {
                                    gap.add(g + j);
                                    j++;
                                } else {
                                    gap.add(g);
                                }
                            }
                            int kokku = gap.size();
                            for (int i = 0; i < kokku; i++) {
                                etNameOf_hide.setText(gap.get(i));
                                btnSave.callOnClick();
                                etNameOf_hide.setText("");
                            }
                            reader.close();
                            gap.clear();
                            Log.d(LOG_TAG,"Считала новый план");

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
            case R.id.item_ready:
                int countMain = mainList.size();
                int countReady = hset.size();
                double persent = countReady * 100 / countMain;
                Toast.makeText(MainActivity.this, "Persent is done " + persent, Toast.LENGTH_SHORT).show();

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
                sPref = getSharedPreferences("Jaak",MODE_PRIVATE);
                sPref.edit().clear().apply();
                Toast.makeText(MainActivity.this,"The rest memory was Deleted " ,Toast.LENGTH_SHORT ).show();
                break;
            case R.id.item2:
                Toast.makeText(MainActivity.this,"In works... " ,Toast.LENGTH_SHORT ).show();
                break;
                }

                return super.onContextItemSelected(item);
        }


    @Override//для поиска
    public boolean onQueryTextSubmit(String s) { //метод для поиска слова (присвоил к кнопке)
        Log.d(LOG_TAG, "Только что зашли в search  etname " + nameOf_etname + " etSecond "+ nameOf_etsecond);
        nameOf_etsecond = etsecond.getText().toString();
        nameOf_etname = s;


        for (String name : mainList) {
            if (name.toLowerCase().contains(s.toLowerCase())) {
                found_List.add(name);
            }
        }
        if (nameOf_etsecond.length() > 0) { // если в поле что то было переделываю found_List
            ArrayList<String> temporaryList = new ArrayList<>();
            for (String name : found_List) {
                if (name.toLowerCase().contains(nameOf_etsecond.toLowerCase())) {
                    temporaryList.add(name);
                }
            }
            found_List.clear();
            found_List.addAll(temporaryList);
        }
        System.out.println(found_List.size() + "кол-во в поисковом Аррэй ");


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_checked, found_List);
        list_of_View.setAdapter(adapter);
        mainList.clear();
        if (listForSearch.isEmpty()) {
            for (String L : found_List) {
                listForSearch.add(L);
            }
        }
        System.out.println(listForSearch.size() + "Кол-во копированных элементов с поиска");
        etName.setText("");
        etsecond.setText("");

        //UP-date Search list
        sPref = getSharedPreferences("SAVE",MODE_PRIVATE);
        int koli = sPref.getInt("Kolichesvo",abra);
        if (listFromPreferenses.isEmpty()) {
            for (int i = 0; i < koli; i++) {
                listFromPreferenses.add(sPref.getString("Keyg" + i, ""));
            }
        }
        System.out.println(listFromPreferenses.size() + " UP_2 - size Кол-во отмеченных всего!");

        ArrayList<Integer> position_ofIndex1 = new ArrayList<>();
        System.out.println(listForSearch.size() + " Кол-во которое видит поик");

        for (int i = 0; i < listForSearch.size();i++) {
            System.out.println(listForSearch.get(i));
            for (int g = 0; g < listFromPreferenses.size(); g++) {
                if (listForSearch.get(i).contains(listFromPreferenses.get(g))){
                    String o = listForSearch.get(i);
                    position_ofIndex1.add(listForSearch.indexOf(o));
                    break;
                }
            }
        }
        System.out.println(position_ofIndex1.size() + "Кол-во позиций добавленных в новый стринг");
        for (int i = 0; i<position_ofIndex1.size();i++){
            list_of_View.setItemChecked(position_ofIndex1.get(i),true);
        }

        if (hset.isEmpty()) {
            for (String nn : listFromPreferenses) {
                hset.add(nn);            //востановления Аррай листа
            }
        }
        listForSearch.clear();
        position_ofIndex1.clear();
        listFromPreferenses.clear();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }
}