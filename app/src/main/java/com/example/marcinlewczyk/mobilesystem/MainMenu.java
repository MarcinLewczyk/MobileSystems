package com.example.marcinlewczyk.mobilesystem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainMenu extends Activity {
    EditText currentLocation;
    Button inboundActivity, outboundActivity, cycleCountActivity, setLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        bindControls();
        setListeners();

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
                editor.commit();
            }
        });

        inboundActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ItemInbound.class);
                startActivity(intent);
            }
        });

        outboundActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ItemOutbound.class);
                startActivity(intent);
            }
        });

        cycleCountActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CycleCount.class);
                startActivity(intent);
            }
        });
    }
}