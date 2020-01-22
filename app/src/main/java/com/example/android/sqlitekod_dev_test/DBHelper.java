package com.example.android.sqlitekod_dev_test;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "DBNeiser";
    public static final String TABLE_CONTACT = "contact";

    public static final String KEY_NAME = "name";
    public static final String KEY_MODEL = "model";
    public static final String KEY_DATA = "data";
    public static final String KEY_ID = "_id";
    public static final String DBCREATE = "create table " + TABLE_CONTACT + "(" + KEY_NAME + " ," + KEY_MODEL + " ," + KEY_DATA + "," + KEY_ID + " INTEGER PRIMARY KEY " + ")";
    //public static final String DBCREATE = "create table " + TABLE_CONTACT + "(" + KEY_NAME + "," + KEY_MODEL + "," + KEY_DATA + ")";
    SQLiteDatabase db;


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBCREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_CONTACT);
        onCreate(db);

    }

    public void insertData(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_NAME, name);
        //contentValues.put(KEY_MODEL, model);
        //contentValues.put(KEY_DATA, data);
        db.insert(TABLE_CONTACT, null, contentValues);
/*        long result = db.insert(TABLE_CONTACT, null, contentValues);
        return result != -1;*/
    }

    public void uninsertData(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
     /*   ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_NAME, name);*/
        db.delete(TABLE_CONTACT,KEY_NAME,new String[]{name});
        db.close();
    }

    public Cursor viewData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "Select * from " + TABLE_CONTACT;
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }

/*    public boolean updateData(String name,String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_NAME, name);
        //contentValues.put(KEY_MODEL, model);
        db.update(TABLE_CONTACT, contentValues, "_id = ?", new String[]{id});
        return true;
    }*/
}





