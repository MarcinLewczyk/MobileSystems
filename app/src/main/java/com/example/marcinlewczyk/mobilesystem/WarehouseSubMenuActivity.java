package com.example.marcinlewczyk.mobilesystem;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.marcinlewczyk.mobilesystem.MagModule.CycleCountActivity;
import com.example.marcinlewczyk.mobilesystem.MagModule.ItemInboundActivity;
import com.example.marcinlewczyk.mobilesystem.MagModule.ItemOutboundActivity;

public class WarehouseSubMenuActivity extends Activity {
    private EditText currentLocation;
    private Button inboundActivity, outboundActivity, cycleCountActivity, setLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_sub_menu);
        bindControls();
        setListeners();
        getLocation();
        setTitle("Warehouse module");
    }

    private void bindControls() {
        currentLocation = findViewById(R.id.locationEditText);
        inboundActivity = findViewById(R.id.inboundAct);
        outboundActivity = findViewById(R.id.outboundAct);
        cycleCountActivity = findViewById(R.id.cycleCountAct);
        setLocation = findViewById(R.id.setLocationButton);
    }

    private void setListeners() {
        setLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("location", currentLocation.getText().toString());
                editor.apply();
                Toast.makeText(WarehouseSubMenuActivity.this, "Location set!",
                        Toast.LENGTH_LONG).show();
            }
        });

        inboundActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ItemInboundActivity.class);
                startActivity(intent);
            }
        });

        outboundActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ItemOutboundActivity.class);
                startActivity(intent);
            }
        });

        cycleCountActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CycleCountActivity.class);
                startActivity(intent);
            }
        });
    }

    private void getLocation(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("location", "");
        currentLocation.setText(name);
    }
}