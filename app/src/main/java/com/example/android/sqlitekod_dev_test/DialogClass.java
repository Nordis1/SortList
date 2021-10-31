package com.example.android.sqlitekod_dev_test;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.edmodo.rangebar.RangeBar;

import java.util.ArrayList;
import java.util.zip.Inflater;

public class DialogClass extends MainActivity implements View.OnClickListener, RangeBar.OnRangeBarChangeListener {
    @Override
    public void onIndexChangeListener(RangeBar rangeBar, int i, int i1) {
        minValue.setText(String.valueOf(i));
        maxValue.setText(String.valueOf(i1));
    }

    // Переменные для  createCustomNewDialogFromWhichTowhich
    TextView minValue;
    TextView maxValue;
    Button btnAccept;
    Button btnCancel;

    AlertDialog dialog;
    AlertDialog.Builder alertBuilder;
    Context context;
    String dialog_message, dialog_title, btnPositive, btnNegative, btnNetral;
    LayoutInflater inflater;

    final int deleteWithOut_rest = 3;
    final int deleteWith_rest = 2;
    final int deleteIsCanceled = 9;


    Button btnDialogCancel, btnDialogChange1;
    EditText editText;
    CheckBox checkBox;
    String stringPrepareToChange;
    final String TAG = "dialogclassTag";

    public DialogClass(Context context, @Nullable String dialog_message, LayoutInflater inflater, @Nullable String stringPrepareToChange) {
        this.context = context;
        this.dialog_message = dialog_message;
        this.inflater = inflater;
        this.stringPrepareToChange = stringPrepareToChange;
    }


    public DialogClass(Context context, @Nullable String dialog_message, @Nullable String title, @Nullable String btn_positive, @Nullable String btn_Negative, @Nullable String btn_Netral) {
        this.context = context;
        this.dialog_message = dialog_message;
        this.dialog_title = title;
        this.btnPositive = btn_positive;
        this.btnNegative = btn_Negative;
        this.btnNetral = btn_Netral;
    }

    @Override
    public void onClick(View v) {
        Toast toast;
        switch (v.getId()) {
            case R.id.ID_btn_DeleteFile_Dialog_Cancel:
                handler.sendEmptyMessage(deleteIsCanceled);
                toast = Toast.makeText(context, "Cancel", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 330);//250 //y - чем выше значение тем ниже элемент
                toast.show();
                dialog.cancel();
                break;
            case R.id.ID_btn_DeleteFile_Dialog_DeleteWithRest:
                handler.sendEmptyMessage(deleteWith_rest);
                toast = Toast.makeText(context, "Deleted successfully, the rest is waiting for a new load.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 330);
                toast.show();
                dialog.cancel();
                break;
            case R.id.ID_btn_DeleteFile_Dialog_Delete:
                handler.sendEmptyMessage(deleteWithOut_rest);
                toast = Toast.makeText(context, "Deleted successfully", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 330);//250
                toast.show();
                dialog.cancel();
                break;
            case R.id.ID_checkBoxDeleteFile:
                if (checkBox.isChecked()) {
                    handler.sendEmptyMessage(hSetDeleteFileCheckBoxIsActivated);
                } else {
                    handler.sendEmptyMessage(hSetDeleteFileCheckBoxDiactivated);
                }
                break;
            case R.id.ID_resize_btnAccept:
                String max = (String) maxValue.getText();
                String min = (String) minValue.getText();
                mColumnmax = Integer.valueOf(max);
                mColumnmin = Integer.valueOf(min);
                bool_xlsColumnsWasChosen = true;
                dialog.cancel();
                break;
            case R.id.ID_resize_btnCancel:
                bool_xlsExecutorCanceled = true;
                dialog.cancel();
                break;
            default:
                handler.sendEmptyMessage(hSetCreateDialogError);
                dialog.cancel();
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    public void createCustomNewDialogFromWhichTowhich() {
        try {
            alertBuilder = new AlertDialog.Builder(context);
            View layout = inflater.inflate(R.layout.resize_xls_reader, null);
            alertBuilder.setView(layout);
            TextView textView = layout.findViewById(R.id.ID_resizeText);//1
            ImageView imageView = layout.findViewById(R.id.ID_resize_xls_image);//2
            minValue = layout.findViewById(R.id.ID_resize_minValue);//3
            maxValue = layout.findViewById(R.id.ID_resize_maxValue);//4
            minValue.setText(String.valueOf(mColumnmin));
            int max = mColumnmax;
            maxValue.setText(String.valueOf(--max)); // тут вставил так, потому что в setTickCount значения считываються с нуля.
            RangeBar rangeBar = layout.findViewById(R.id.ID_resize_RangeBar);//5
            Log.i(TAG, "createCustomNewDialogFromWhichTowhich: mColumbmax = " + mColumnmax);
            rangeBar.setTickCount(mColumnmax);
            rangeBar.setOnRangeBarChangeListener(this);
            btnAccept = layout.findViewById(R.id.ID_resize_btnAccept);//6
            btnCancel = layout.findViewById(R.id.ID_resize_btnCancel);//7
            btnAccept.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
            dialog = alertBuilder.create();
        } catch (Exception e) {
            handler.sendEmptyMessage(hSetCreateDialogError);
            e.printStackTrace();
        }

    }

    public void createCustomNewDialogDeleteFile() {
        try {
            alertBuilder = new AlertDialog.Builder(context);
            View layout = inflater.inflate(R.layout.activity_delete_file, null);
            alertBuilder.setView(layout);
            checkBox = layout.findViewById(R.id.ID_checkBoxDeleteFile);
            Button btnCancel = layout.findViewById(R.id.ID_btn_DeleteFile_Dialog_Cancel);
            Button btnDeleteWithRest = layout.findViewById(R.id.ID_btn_DeleteFile_Dialog_DeleteWithRest);
            Button btnDelete = layout.findViewById(R.id.ID_btn_DeleteFile_Dialog_Delete);
            btnCancel.setOnClickListener(this);
            btnDeleteWithRest.setOnClickListener(this);
            btnDelete.setOnClickListener(this);
            checkBox.setOnClickListener(this);
            dialog = alertBuilder.create();
        } catch (Exception e) {
            handler.sendEmptyMessage(hSetCreateDialogError);
            e.printStackTrace();
        }


    }

    public void createCustomNewDialogChageItem() { //
        //Work Example from: https://stackoverflow.com/questions/22655599/alertdialog-builder-with-custom-layout-and-edittext-cannot-access-view
        try {
            alertBuilder = new AlertDialog.Builder(context);
            View layout = inflater.inflate(R.layout.activitychangeitem2, null);
            alertBuilder.setView(layout);
            btnDialogChange1 = layout.findViewById(R.id.ID_btnChange_Dialog);
            btnDialogCancel = layout.findViewById(R.id.ID_btnCancel_Dialog);
            editText = layout.findViewById(R.id.ID_Edit_text_Dialog);
            editText.setText(stringPrepareToChange);

            btnDialogChange1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context, "Yes", Toast.LENGTH_SHORT).show();
                    String s = editText.getText().toString();
                    if (!s.isEmpty() && !s.equals(stringPrepareToChange)) {
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();

                        bundle.putString("changeString", s);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                        dialog.cancel();
                    } else {
                        dialog.cancel();
                    }
                }
            });
            btnDialogCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context, "Cancel", Toast.LENGTH_SHORT).show();
                    dialog.cancel();
                }
            });

            dialog = alertBuilder.create();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void createStandartNewDialogOKCancelNetral() {
        try {
            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setMessage(dialog_message).setTitle(dialog_title)
                    .setCancelable(true)
                    .setPositiveButton(btnPositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //positive
                            Log.i(TAG, "Dialog onClick: Была нажата кнопка Позитив");
                            handler.sendEmptyMessage(deleteWithOut_rest);
                            Toast toast = Toast.makeText(context, "Deleted successfully", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.TOP, 0, 330);//250
                            toast.show();
                            //finish(); use finish if you want the app  to be closed.
                        }
                    }).setNegativeButton(btnNegative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Негатив
                    Log.i(TAG, "Dialog onClick: Была нажата кнопка Негатив");
                    handler.sendEmptyMessage(deleteWith_rest);
                    Toast toast = Toast.makeText(context, "Deleted successfully, the rest is waiting for a new load.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 330);
                    toast.show();
                }
            }).setNeutralButton(btnNetral, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.i(TAG, "Dialog onClick: Была нажата кнопка нетрал");
                    handler.sendEmptyMessage(deleteIsCanceled);
                    Toast toast = Toast.makeText(context, "Cancel", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 330);//250 //y - чем выше значение тем ниже элемент
                    toast.show();
                    //Нетрал
                }
            });
            dialog = alertBuilder.create();
        } catch (Exception e) {
            Log.i(TAG, "createNewDialog: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
