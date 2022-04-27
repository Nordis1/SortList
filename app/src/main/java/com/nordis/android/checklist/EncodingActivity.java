package com.nordis.android.checklist;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.nordis.android.checklist.databinding.ActivityEncodingBinding;

import java.util.ArrayList;
import java.util.Arrays;


public class EncodingActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "EncodingActivity";
    private ActivityEncodingBinding binding;
    String result;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_encoding);
        binding = ActivityEncodingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.btnOk.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(this);

        ArrayList<CharsetsEnum> adapterList = new ArrayList<>();
        adapterList.addAll(Arrays.asList(CharsetsEnum.values()));
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < adapterList.size(); i++) {
            list.add(adapterList.get(i).getKey());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, list);

        binding.IDSpinner.setAdapter(adapter);
        binding.IDSpinner.setOnItemSelectedListener(this);


    }

    @Override
    public void onClick(View v) {
        Intent intent;
        if (binding.btnOk.equals(v)) {
            intent = new Intent();
            intent.putExtra("nameOfCharset", result);
            setResult(RESULT_OK, intent);
            finish();
        } else if (binding.btnCancel.equals(v)) {
            setResult(RESULT_CANCELED);
            finish();
        }

    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                result = parent.getAdapter().getItem(position).toString();
                //Log.d(TAG, "onItemSelected: "+ parent.getAdapter().getItem(position).toString());
                break;
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}