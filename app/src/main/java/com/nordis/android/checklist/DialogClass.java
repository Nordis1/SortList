package com.nordis.android.checklist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.edmodo.rangebar.RangeBar;
import com.nordis.android.checklist.databinding.ActivityDeleteFileBinding;
import com.nordis.android.checklist.databinding.Activitychangeitem2Binding;
import com.nordis.android.checklist.databinding.ResizeXlsReaderBinding;

import java.util.concurrent.TimeUnit;

public class DialogClass extends MainActivity implements View.OnClickListener, RangeBar.OnRangeBarChangeListener, CompoundButton.OnCheckedChangeListener {

    final String TAG = "Dialog_class_Tag";
    private ActivityDeleteFileBinding activityDeleteFileBinding;
    private ResizeXlsReaderBinding resizeXlsReaderBinding;
    private Activitychangeitem2Binding activitychangeitem2Binding;
    // Общие переменные
    AlertDialog dialog;
    AlertDialog.Builder alertBuilder;
    Context context;
    LayoutInflater inflater;
    // Переменные для createCustomNewDialogChageItem()
    String stringPrepareToChange;
    //Переменные для классического применения диалога.
    String dialog_message, dialog_title, btnPositive, btnNegative, btnNetral;

    public DialogClass(Context context, @Nullable String dialog_message, @Nullable String stringPrepareToChange) {
        this.context = context;
        this.dialog_message = dialog_message;
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
    public void onIndexChangeListener(RangeBar rangeBar, int i, int i1) {
        // rangeBar как и seekbar  только range bar можно регулировать с обеих сторон.
        resizeXlsReaderBinding.IDResizeMinValue.setText(String.valueOf(i));
        resizeXlsReaderBinding.IDResizeMaxValue.setText(String.valueOf(i1));
    }

    public void createCustomNewDialogFromWhichTowhich() {
        // Используеться при загрузке xls файла, что бы понять с какой по какую колонку загружать.
        Log.d(TAG, "createCustomNewDialogFromWhichTowhich: Начало нового диалога");
        try {
            alertBuilder = new AlertDialog.Builder(context);
            inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            resizeXlsReaderBinding = ResizeXlsReaderBinding.inflate(inflater);
            resizeXlsReaderBinding.IDResizeBtnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mColumnmax = Integer.parseInt(resizeXlsReaderBinding.IDResizeMaxValue.getText().toString());
                    mColumnmin = Integer.parseInt(resizeXlsReaderBinding.IDResizeMinValue.getText().toString());
                    bool_xlsColumnsWasChosen = true;
                    dialog.cancel();
                }
            });
            resizeXlsReaderBinding.IDResizeBtnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bool_xlsExecutorCanceled = true;
                    dialog.cancel();
                }
            });
            resizeXlsReaderBinding.IDResizeRangeBar.setOnRangeBarChangeListener(this);
            resizeXlsReaderBinding.IDResizeMinValue.setText(String.valueOf(mColumnmin));
            resizeXlsReaderBinding.IDResizeMaxValue.setText(String.valueOf(--mColumnmax));
            resizeXlsReaderBinding.IDResizeRangeBar.setTickCount(++mColumnmax);//Внимание устанавливать кол-во нужно до установки нажатия.Иначе ошибка.
            alertBuilder.setView(resizeXlsReaderBinding.getRoot());
            alertBuilder.setCancelable(false);
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
            inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            activityDeleteFileBinding = ActivityDeleteFileBinding.inflate(inflater);
            activityDeleteFileBinding.IDBtnDeleteFileDialogDelete.setOnClickListener(new View.OnClickListener() {
                /** Удалие */
                @Override
                public void onClick(View v) {
              /*      if (activityDeleteFileBinding.IDSaveUncheckedPositions.isChecked()){
                        handler.sendEmptyMessage(2);
                    }else {*/
                    handler.sendEmptyMessage(hsetdelete_WithOut_rest);
                    dialog.cancel();
                }
            });
            activityDeleteFileBinding.IDBtnDeleteFileDialogCancel.setOnClickListener(new View.OnClickListener() {
                /** Отмена */
                @Override
                public void onClick(View v) {
                    handler.sendEmptyMessage(hsetdelete_IsCanceled);
                    dialog.cancel();
                }
            });

            activityDeleteFileBinding.IDBtnDeleteFileDialogDeleteAllChecked.setOnClickListener(new View.OnClickListener() {
                /** Удаляем все отмеченные */
                @Override
                public void onClick(View v) {
                    handler.sendEmptyMessage(hRemoveRestMemory);
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(hSetDeleteChekedPositions);
                    dialog.cancel();
                }
            });
            activityDeleteFileBinding.IDSaveUncheckedPositions.setOnCheckedChangeListener(this);
            alertBuilder.setView(activityDeleteFileBinding.getRoot());
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
            inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            activitychangeitem2Binding = Activitychangeitem2Binding.inflate(inflater);
            activitychangeitem2Binding.IDBtnCancelDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Cancel", Toast.LENGTH_SHORT).show();
                    dialog.cancel();
                }
            });
            activitychangeitem2Binding.IDBtnChangeDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String s = activitychangeitem2Binding.IDEditTextDialog.getText().toString();
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

            alertBuilder.setCancelable(false);
            alertBuilder.setView(activitychangeitem2Binding.getRoot());
            activitychangeitem2Binding.IDEditTextDialog.setText(stringPrepareToChange);
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            activityDeleteFileBinding.IDBtnDeleteFileDialogDeleteAllChecked.setEnabled(false);
            activityDeleteFileBinding.IDBtnDeleteFileDialogCancel.setEnabled(false);
            handler.sendEmptyMessage(hMakeRestMemory);
        } else {
            activityDeleteFileBinding.IDBtnDeleteFileDialogDeleteAllChecked.setEnabled(true);
            activityDeleteFileBinding.IDBtnDeleteFileDialogCancel.setEnabled(true);
            handler.sendEmptyMessage(hRemoveRestMemory);
        }
    }
    public void createDialogNoInternet() {
        try {
            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setMessage(dialog_message).setTitle(dialog_title)
                    .setCancelable(false)
                    .setPositiveButton(btnPositive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //positive
                            handler.sendEmptyMessage(hSetInternetReCheck);
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
