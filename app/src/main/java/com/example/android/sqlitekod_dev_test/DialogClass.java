package com.example.android.sqlitekod_dev_test;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.Tag;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class DialogClass extends MainActivity {
    AlertDialog dialog;
    AlertDialog.Builder alertBuilder;
    Context context;
    final String TAG = "dialogclassTag";

    public DialogClass(Context context) {
        this.context = context;


    }

    public void createNewDialog(){

        try {
            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_Hello)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok_accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            createjaak();
                            btnDeleteAll.callOnClick();

                            Toast toast = Toast.makeText(context, "Deleted successfully, the rest is waiting for a new load.", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.TOP, 0, 0);
                            toast.show();
                            //finish(); use finish if you want the app  to be closed.
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialog.cancel();
                    Toast toast = Toast.makeText(context, "Press delete one more", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.TOP, 0, 0);
                            toast.show();
                }
            });
            dialog = alertBuilder.create();
        } catch (Exception e) {
            Log.i(TAG, "createNewDialog: "+ e.getMessage());
            e.printStackTrace();
        }

    }

}
