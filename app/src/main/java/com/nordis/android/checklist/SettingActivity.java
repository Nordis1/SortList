package com.nordis.android.checklist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

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
        binding.checkBoxEnableAdBanner.setOnCheckedChangeListener(this);

        sPref = getSharedPreferences("Settings", MODE_PRIVATE);
        binding.checkBox.setChecked(sPref.getBoolean("autoCompletetext", false));

        sPref = getSharedPreferences("SettingsADBanner", MODE_PRIVATE);
        binding.checkBoxEnableAdBanner.setChecked(sPref.getBoolean("AdBanner", false));
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        if (v.getId() == binding.btnAcceptSettings.getId()) {
            sPref = getSharedPreferences("Settings", MODE_PRIVATE);
            sPref.edit().putBoolean("autoCompletetext", binding.checkBox.isChecked()).apply();

            sPref = getSharedPreferences("SettingsADBanner", MODE_PRIVATE);
            sPref.edit().putBoolean("AdBanner", binding.checkBoxEnableAdBanner.isChecked()).apply();

            intent = new Intent();
            intent.putExtra("AutoCompleteText", binding.checkBox.isChecked());
            intent.putExtra("AdBannerEnable", binding.checkBoxEnableAdBanner.isChecked());
            setResult(2, intent);
            finish();
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }
}