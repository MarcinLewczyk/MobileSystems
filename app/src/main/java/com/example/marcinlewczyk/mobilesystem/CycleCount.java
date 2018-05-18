package com.example.marcinlewczyk.mobilesystem;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class CycleCount extends AppCompatActivity {
    private String currentLocation;
    private SurfaceView cameraPreview;
    private TextView itemIdTxt, itemQtyTxt, itemQtyTotalScanned;
    private Button commitCycleCountButton, readCodeCycleCountButton;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private long lastScan, timeParameter = 2000, itemId, lastItemId;
    private final int RequestCameraPermissionID = 1001;
    private int lastScannedQty, totalScannedQty;
    private boolean firstScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_count);
        initVariables();
        prepareGUI();
        buildQRScanner();
        connectCommitButtonLogic();
    }

    private void initVariables() {
        totalScannedQty = 0;
        firstScan = true;
    }

    private void prepareGUI() {
        initControls();
        setTitleBarContent();
    }

    private void initControls() {
        cameraPreview = findViewById(R.id.cameraPreview);
        itemIdTxt = findViewById(R.id.itemId);
        itemQtyTxt = findViewById(R.id.itemQty);
        itemQtyTotalScanned = findViewById(R.id.itemQtyTotal);
        commitCycleCountButton = findViewById(R.id.commitButton);
        readCodeCycleCountButton = findViewById(R.id.readCodeButton);
        commitCycleCountButton.setEnabled(false);
    }

    private void setTitleBarContent(){
        getCurrentLocation();
        setTitle("Cycle count - your location: " + currentLocation);
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
                    ActivityCompat.requestPermissions(CycleCount.this, new String[]{android.Manifest.permission.CAMERA}, RequestCameraPermissionID);
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
        readCodeCycleCountButton.setOnClickListener(new View.OnClickListener() {
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
                            long currentTime = SystemClock.uptimeMillis();
                            if(currentTime - lastScan >= timeParameter){
                                itemId = Long.valueOf(results[0]);
                                lastScannedQty = Integer.valueOf(results[2]);
                                howMuchScannedData = results.length;
                                lastScan = currentTime;
                            }
                        }
                    }
                });
                if(howMuchScannedData == 3){
                    if(firstScan){
                        totalScannedQty += lastScannedQty;
                        updateGUI("" + itemId, "" + lastScannedQty, "" + totalScannedQty);
                        callItemInfoWebService();
                        commitCycleCountButton.setEnabled(true);
                        lastItemId = itemId;
                        firstScan = false;
                    } else if (lastItemId == itemId) {
                        totalScannedQty += lastScannedQty;
                        updateGUI("", "" + lastScannedQty, "" + totalScannedQty);
                    } else {
                        new AlertDialog.Builder(CycleCount.this)
                                .setTitle("Error")
                                .setMessage("This item is different than previous one, cannot count different items at once")
                                .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).setCancelable(false)
                                .show();
                    }
                }
            }
        });
    }

    private void callItemInfoWebService(){
        new ItemInfoWebServiceHandler().execute(WebServiceConfig.address + "/item/" + itemId);
    }

    private class ItemInfoWebServiceHandler extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog = new ProgressDialog(CycleCount.this);

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
                        lastScannedQty + "",
                        totalScannedQty + "");
            } catch (Exception e) {
                new AlertDialog.Builder(CycleCount.this)
                        .setTitle("Error")
                        .setMessage("Check Internet connection")
                        .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                callItemInfoWebService();
                            }
                        }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
            }
        }
    }

    private void updateGUI(String itemInfo, String scannedQty, String totalScannedQty) {
        if(itemInfo != ""){
            itemIdTxt.setText("Item: " + itemInfo);
        }
        itemQtyTotalScanned.setText("Last scanned qty: " + scannedQty);
        itemQtyTxt.setText("Total scanned: " + totalScannedQty);
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
        commitCycleCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callCycleCountWebService();
            }
        });
    }

    private void callCycleCountWebService(){
        new CycleCountWebServiceHandler().execute(WebServiceConfig.address + "/item/" + itemId +"/" + totalScannedQty + "/" + currentLocation);
    }

    private class CycleCountWebServiceHandler extends AsyncTask<String, Void, String>{

        private ProgressDialog dialog = new ProgressDialog(CycleCount.this);

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
                if(result.contains("true")){
                    new AlertDialog.Builder(CycleCount.this)
                            .setTitle("Processed")
                            .setMessage("Correct qty")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    clearScreen();
                                }
                            }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
                } else {
                    new AlertDialog.Builder(CycleCount.this)
                            .setTitle("Error")
                            .setMessage("Qty different than system state, count again or contact supervisor!")
                            .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    clearScreen();
                                }
                            }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
                }
            } catch (Exception e) {
                new AlertDialog.Builder(CycleCount.this)
                        .setTitle("Error")
                        .setMessage("Check Internet connection")
                        .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                callCycleCountWebService();
                            }
                        }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
            }
        }
    }

    private void clearScreen(){
        itemIdTxt.setText("");
        itemQtyTotalScanned.setText("");
        itemQtyTxt.setText("");
        itemId = -1;
        totalScannedQty = 0;
        lastScannedQty = 0;
        firstScan = true;
        commitCycleCountButton.setEnabled(false);
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