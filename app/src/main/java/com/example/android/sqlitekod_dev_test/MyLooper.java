package com.example.android.sqlitekod_dev_test;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

public class MyLooper extends Thread {


    private static final String TAG = MyLooper.class.getSimpleName();
    Handler h;

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        h = new Handler(Looper.myLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {

                Log.i(TAG, "handleMessage: " + Thread.currentThread().getId());
            }
        };
        Looper.loop();
    }
}
