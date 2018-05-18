package com.example.marcinlewczyk.mobilesystem;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.marcinlewczyk.mobilesystem.DeliveryModule.DeliverPackageActivity;
import com.example.marcinlewczyk.mobilesystem.DeliveryModule.OrderInfoActivity;
import com.example.marcinlewczyk.mobilesystem.DeliveryModule.OrderProductsInfoActivity;
import com.example.marcinlewczyk.mobilesystem.DeliveryModule.ShipPackageActivity;

public class DeliverySubMenuActivity extends AppCompatActivity {
    private Button orderInfoActivity, shipPackageActivity, deliverPackageActivity, orderProductsInfoActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_sub_menu);
        bindControls();
        setListeners();
        setTitle("Delivery module");
    }

    private void bindControls() {
        orderInfoActivity = findViewById(R.id.orderInfoActivityNavigationButton);
        shipPackageActivity = findViewById(R.id.shipPackageActivityNavigationButton);
        deliverPackageActivity = findViewById(R.id.deliverPackageActivityNavigationButton);
        orderProductsInfoActivity = findViewById(R.id.orderProductsActivityNavigationButton);
    }

    private void setListeners() {

        orderInfoActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), OrderInfoActivity.class);
                startActivity(intent);
            }
        });

        shipPackageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ShipPackageActivity.class);
                startActivity(intent);
            }
        });

        deliverPackageActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DeliverPackageActivity.class);
                startActivity(intent);
            }
        });

        orderProductsInfoActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), OrderProductsInfoActivity.class);
                startActivity(intent);
            }
        });
    }
}