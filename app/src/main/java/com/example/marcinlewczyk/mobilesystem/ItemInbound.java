package com.example.marcinlewczyk.mobilesystem;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ItemInbound extends AppCompatActivity {
    String currentLocation;
    SurfaceView cameraPreview;
    TextView itemIdAndFromTxt, manufacturerTxt, itemQtyTxt;
    Button commitInboundButton, readCodeInboundButton;
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_inbound);
        initControls();
        setTitleBarContent();
        buildQRScanner();
        connectCommitButtonLogic();
    }

    private void initControls() {
        cameraPreview = findViewById(R.id.cameraPreview);
        itemIdAndFromTxt = findViewById(R.id.itemIdAndFrom);
        manufacturerTxt = findViewById(R.id.manufacturer);
        itemQtyTxt = findViewById(R.id.itemQty);
        commitInboundButton = findViewById(R.id.commitButton);
        readCodeInboundButton = findViewById(R.id.readCodeButton);
        commitInboundButton.setEnabled(false);
    }

    private void setTitleBarContent(){
        getCurrentLocation();
        setTitle("Your location: " + currentLocation);
    }

    private void getCurrentLocation(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("location", "Nope");
        currentLocation = name;
    }

    private void buildQRScanner() {
        initBarcodeDetector();
        buildCameraSource();
        bindReadButtonWithScanning();
    }

    private void bindReadButtonWithScanning() {
        readCodeInboundButton.setOnClickListener(new View.OnClickListener() {
            int howMuchScannedData = 0;
            @Override
            public void onClick(View v) {

                barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                    @Override
                    public void release() {

                    }

                    @Override
                    public void receiveDetections(Detector.Detections<Barcode> detections) {
                        SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                        if(qrCodes.size() != 0){
                            String qrCodeString = qrCodes.valueAt(0).displayValue;
                            String[] results = qrCodeString.split(";");
                            itemIdAndFromTxt.setText("Item: " + results[0] + "   From: " +  results[1]);
                            manufacturerTxt.setText("Manufacturer: " +  results[2]);
                            itemQtyTxt.setText("Qty: " + results[3]);
                            howMuchScannedData = results.length;
                        }
                    }
                });
                if(howMuchScannedData != 0){
                    commitInboundButton.setEnabled(true);
                }
            }
        });
    }

    private void initBarcodeDetector() {
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
    }

    private void buildCameraSource() {
        initCameraSource();
        addCallbacks();
    }

    private void initCameraSource() {
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(640, 480)
                .build();
    }

    private void addCallbacks() {
        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ItemInbound.this, new String[]{android.Manifest.permission.CAMERA}, RequestCameraPermissionID);
                    return;
                }
                try {
                    cameraSource.start(cameraPreview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }
                    try {
                        cameraSource.start(cameraPreview.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
    }

    private void connectCommitButtonLogic(){
        commitInboundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo communication with server
            }
        });
    }
}