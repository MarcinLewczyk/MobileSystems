package com.example.marcinlewczyk.mobilesystem;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenuActivity extends AppCompatActivity {
    private Button warehouseActivityButton, deliveryActivityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        bindControls();
        setListeners();
        setTitle("Main Menu");
    }

    private void bindControls() {
        warehouseActivityButton = findViewById(R.id.warehouseModuleNavigationButton);
        deliveryActivityButton = findViewById(R.id.deliveryModuleNavigationButton);
    }

    private void setListeners() {
        warehouseActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), WarehouseSubMenuActivity.class);
                startActivity(intent);
            }
        });

        deliveryActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DeliverySubMenuActivity.class);
                startActivity(intent);
            }
        });
    }
}