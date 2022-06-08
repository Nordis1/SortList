package com.nordis.android.checklist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nordis.android.checklist.databinding.ActivitySettingBinding;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
private ActivitySettingBinding binding;
private SharedPreferences sPref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnAcceptSettings.setOnClickListener(this);
        binding.checkBox.setOnCheckedChangeListener(this);

        sPref = getSharedPreferences("Settings",MODE_PRIVATE);
        binding.checkBox.setChecked(sPref.getBoolean("autoCompletetext",false));
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        if (v.getId() == binding.btnAcceptSettings.getId()){
           intent = new Intent();
           intent.putExtra("AutoCompleteText", binding.checkBox.isChecked());
           setResult(2, intent);
           finish();
       }
    }

    public void checkBoxListner(Boolean bool){
        sPref = getSharedPreferences("Settings",MODE_PRIVATE);
        if (bool){
            sPref.edit().putBoolean("autoCompletetext",true).apply();
        }else {
            sPref.edit().putBoolean("autoCompletetext",false).apply();
        }


    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == binding.checkBox.getId()){
            checkBoxListner(isChecked);
        }
    }
}