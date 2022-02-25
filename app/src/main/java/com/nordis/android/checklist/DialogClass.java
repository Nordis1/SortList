package com.nordis.android.checklist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import androidx.annotation.Nullable;
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
import com.example.android.checklist.R;

import java.util.concurrent.TimeUnit;

public class DialogClass extends MainActivity implements View.OnClickListener, RangeBar.OnRangeBarChangeListener {

    // Общие переменные
    AlertDialog dialog;
    AlertDialog.Builder alertBuilder;
    Context context;
    LayoutInflater inflater;

    // Переменные для  createCustomNewDialogFromWhichTowhich
    TextView minValue;
    TextView maxValue;
    Button btnAccept;
    Button btnCancel;

    // Переменные для createCustomNewDialogChageItem()
    Button btn_ChageItem_Cancel;
    Button btn_ChageItem_Change;
    EditText ChageItem_editText;
    String stringPrepareToChange;


    // Переменные для createCustomNewDialogDeleteFile()
    CheckBox checkBox_DeleteFile;
    CheckBox checkbox_saveCheched;

    //Переменные для классического применения диалога.
    String dialog_message, dialog_title, btnPositive, btnNegative, btnNetral;


    Button btnDeleteAllChecked;


    final String TAG = "Dialog_class_Tag";

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
                handler.sendEmptyMessage(hsetdelete_IsCanceled);
                dialog.cancel();
                break;
            case R.id.ID_btn_DeleteFile_Dialog_DeleteAllChecked:
                handler.sendEmptyMessage(hSetDeleteRest);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(hSetDeleteChekedPositions);
                dialog.cancel();
                break;
            case R.id.ID_btn_DeleteFile_Dialog_Delete:
                handler.sendEmptyMessage(hsetdelete_WithOut_rest);
                dialog.cancel();
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
            case R.id.ID_saveUncheckedPositions:
                if (checkbox_saveCheched.isChecked()) {
                    btnDeleteAllChecked.setEnabled(false);
                    btnCancel.setEnabled(false);
                    handler.sendEmptyMessage(hSetDoRest);
                }else {
                    btnDeleteAllChecked.setEnabled(true);
                    btnCancel.setEnabled(true);
                    handler.sendEmptyMessage(hSetDeleteRest);
                }
                break;
            default:
                handler.sendEmptyMessage(hSetCreateDialogError);
                dialog.cancel();
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }

    @Override
    public void onIndexChangeListener(RangeBar rangeBar, int i, int i1) {
        // rangeBar как и seekbar  только range bar можно регулировать с обеих сторон.
        minValue.setText(String.valueOf(i));
        maxValue.setText(String.valueOf(i1));
    }

    public void createCustomNewDialogFromWhichTowhich() {
        // Используеться при загрузке xls файла, что бы понять с какой по какую колонку загружать.
        Log.d(TAG, "createCustomNewDialogFromWhichTowhich: Начало нового диалога");
        try {
            alertBuilder = new AlertDialog.Builder(context);
            View layout = inflater.inflate(R.layout.resize_xls_reader, null);
            alertBuilder.setView(layout);
            alertBuilder.setCancelable(false);
            TextView textView = layout.findViewById(R.id.ID_resizeText);//1
            ImageView imageView = layout.findViewById(R.id.ID_resize_xls_image);//2
            minValue = layout.findViewById(R.id.ID_resize_minValue);//3
            maxValue = layout.findViewById(R.id.ID_resize_maxValue);//4
            minValue.setText(String.valueOf(mColumnmin));
            int max = mColumnmax;
            maxValue.setText(String.valueOf(--max)); // тут вставил так, потому что в setTickCount значения считываються с нуля.
            RangeBar rangeBar = layout.findViewById(R.id.ID_resize_RangeBar);//5
            Log.i(TAG, "createCustomNewDialogFromWhichTowhich: mColumbmax = " + mColumnmax);
            rangeBar.setTickCount(mColumnmax);//Внимание устанавливать кол-во нужно до установки нажатия.Иначе ошибка.
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
        //Используеться при нажатии кнопки удаления
        try {
            alertBuilder = new AlertDialog.Builder(context);
            View layout = inflater.inflate(R.layout.activity_delete_file, null);
            alertBuilder.setView(layout);
            alertBuilder.setCancelable(false);
            checkbox_saveCheched = layout.findViewById(R.id.ID_saveUncheckedPositions);
            btnCancel = layout.findViewById(R.id.ID_btn_DeleteFile_Dialog_Cancel);
            btnDeleteAllChecked = layout.findViewById(R.id.ID_btn_DeleteFile_Dialog_DeleteAllChecked);
            Button btnDelete = layout.findViewById(R.id.ID_btn_DeleteFile_Dialog_Delete);
            checkbox_saveCheched.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
            btnDeleteAllChecked.setOnClickListener(this);
            btnDelete.setOnClickListener(this);
            dialog = alertBuilder.create();
        } catch (Exception e) {
            handler.sendEmptyMessage(hSetCreateDialogError);
            e.printStackTrace();
        }


    }

    public void createCustomNewDialogChageItem() {
        // Спользуеться через контексное меню , изменение строки.
        //Work Example from: https://stackoverflow.com/questions/22655599/alertdialog-builder-with-custom-layout-and-edittext-cannot-access-view
        try {
            alertBuilder = new AlertDialog.Builder(context);
            View layout = inflater.inflate(R.layout.activitychangeitem2, null);
            alertBuilder.setView(layout);
            alertBuilder.setCancelable(false);
            btn_ChageItem_Change = layout.findViewById(R.id.ID_btnChange_Dialog);
            btn_ChageItem_Cancel = layout.findViewById(R.id.ID_btnCancel_Dialog);
            ChageItem_editText = layout.findViewById(R.id.ID_Edit_text_Dialog);
            ChageItem_editText.setText(stringPrepareToChange);

            btn_ChageItem_Change.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String s = ChageItem_editText.getText().toString();
                    if (!s.isEmpty() && !s.equals(stringPrepareToChange)) {
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();

                        bundle.putString("changeString", s);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                        dialog.cancel();
                    } else {
                        Toast.makeText(context, "Nothing to change", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                }
            });
            btn_ChageItem_Cancel.setOnClickListener(new View.OnClickListener() {
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


    public void createStandartNewDialogShowAd() {
        try {
            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setMessage(dialog_message).setTitle(dialog_title)
                    .setCancelable(false)
                    .setPositiveButton(btnPositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //positive
                            Log.i(TAG, "Dialog onClick: Была нажата кнопка Позитив");
                            handler.sendEmptyMessage(hShowAd);
                            //finish(); use finish if you want the app  to be closed.
                        }
                    }).setNegativeButton(btnNegative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Негатив
                    Log.i(TAG, "Dialog onClick: Была нажата кнопка Негатив");
                    dialog.cancel();
                }
            }).setNeutralButton(btnNetral, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Log.i(TAG, "Dialog onClick: Была нажата кнопка нетрал");
                    //Нетрал
                }
            });
            dialog = alertBuilder.create();
        } catch (Exception e) {
            Log.i(TAG, "createNewDialog: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void createDialogPendingState() {
        try {
            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setMessage(dialog_message).setTitle(dialog_title)
                    .setCancelable(false)
                    .setPositiveButton(btnPositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //positive
                            dialog.cancel();
                            //finish(); use finish if you want the app  to be closed.
                        }
                    });
            dialog = alertBuilder.create();
        } catch (Exception e) {
            Log.i(TAG, "createNewDialog: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
