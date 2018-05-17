package com.example.marcinlewczyk.mobilesystem;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.marcinlewczyk.mobilesystem.Config.WebServiceConfig;
import com.example.marcinlewczyk.mobilesystem.POJO.ItemInboundInfo;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class ItemInbound extends AppCompatActivity {
    private String currentLocation;
    private SurfaceView cameraPreview;
    private TextView itemIdTxt, fromTxt, itemQtyTxt;
    private Button commitInboundButton, readCodeInboundButton;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private final int RequestCameraPermissionID = 1001;
    private long itemId, locationId;
    private int itemQty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_inbound);
        prepareGUI();
        buildQRScanner();
        connectCommitButtonLogic();
    }

    private void prepareGUI(){
        initControls();
        setTitleBarContent();
    }

    private void initControls() {
        cameraPreview = findViewById(R.id.cameraPreview);
        itemIdTxt = findViewById(R.id.itemIdAndFrom);
        fromTxt = findViewById(R.id.manufacturer);
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
        currentLocation = preferences.getString("location", "Nope");
    }

    private void buildQRScanner() {
        initBarcodeDetector();
        buildCameraSource();
        bindReadButtonWithScanning();
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
                            itemId = Long.valueOf(results[0]);
                            locationId = Long.valueOf(results[1]);
                            itemQty = Integer.valueOf(results[2]);
                            howMuchScannedData = results.length;
                        }
                    }
                });
                if(howMuchScannedData == 3){ //change if needed
                    updateGUI("" + itemId, "" + locationId, "" + itemQty);
                    callItemInfoWebService();
                    commitInboundButton.setEnabled(true);
                }
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

    private void callItemInfoWebService(){
        new ItemInfoWebServiceHandler().execute(WebServiceConfig.address + "/item/" + itemId);
    }

    private class ItemInfoWebServiceHandler extends AsyncTask<String, Void, String>{

        private ProgressDialog dialog = new ProgressDialog(ItemInbound.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Connecting with server...");
            dialog.show();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                URLConnection connection = url.openConnection();
                connection.setReadTimeout(10000);
                connection.setConnectTimeout(10000);
                InputStream in = new BufferedInputStream(connection.getInputStream());

                return convertInputStreamToString(in);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {

            dialog.dismiss();
            try {
                JSONObject object = new JSONObject(result);
                ItemInboundInfo itemInboundInfo = new ItemInboundInfo();
                itemInboundInfo.setId(object.optLong("id"));
                itemInboundInfo.setName(object.optString("name"));
                itemInboundInfo.setLocationId(object.optLong("locationId"));
                itemInboundInfo.setDescription(object.optString("description"));
                itemInboundInfo.setLocationName(object.optString("locationName"));
                itemInboundInfo.setQuantity(object.optInt("quantity"));
                updateGUI(itemInboundInfo.getId() + "  Name: " + itemInboundInfo.getName(),
                            itemInboundInfo.getId() + "  Location: " + itemInboundInfo.getLocationName(),
                            itemQty + "");
            } catch (Exception e) {
                Log.d("ItemInbound", e.toString());
                new AlertDialog.Builder(ItemInbound.this)
                        .setTitle("Error")
                        .setMessage("Check internet connection")
                        .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                new ItemInbound.ItemInfoWebServiceHandler().execute(WebServiceConfig.address);
                            }
                        }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
            }
        }
    }

    private void connectCommitButtonLogic(){
        commitInboundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWebService();
            }
        });
    }

    private void startWebService(){

    }

    private void updateGUI(String itemInfo, String fromInfo, String qtyInfo) {
        itemIdTxt.setText("Item: " + itemInfo);
        fromTxt.setText("From: " + fromInfo);
        itemQtyTxt.setText("Qty: " + qtyInfo);
    }


    public static String convertInputStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}