package com.example.marcinlewczyk.mobilesystem.DeliveryModule;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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
import com.example.marcinlewczyk.mobilesystem.POJO.OrderAddressInfo;
import com.example.marcinlewczyk.mobilesystem.R;
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

public class OrderInfoActivity extends AppCompatActivity {
    private SurfaceView cameraPreview;
    private TextView clientNameTxt, address1Txt, address2Txt;
    private Button readCodeInboundButton;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private final int RequestCameraPermissionID = 1001;
    private long orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_info);
        prepareGUI();
        buildQRScanner();
    }

    private void prepareGUI(){
        initControls();
        setTitleBarContent();
    }

    private void initControls() {
        cameraPreview = findViewById(R.id.cameraPreview);
        clientNameTxt = findViewById(R.id.clientName);
        address1Txt = findViewById(R.id.clientAddress1);
        address2Txt = findViewById(R.id.clientAddress2);
        readCodeInboundButton = findViewById(R.id.readCodeButton);
    }

    private void setTitleBarContent(){
        setTitle("Order Info");
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
                    ActivityCompat.requestPermissions(OrderInfoActivity.this, new String[]{android.Manifest.permission.CAMERA}, RequestCameraPermissionID);
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

            @Override
            public void onClick(View v) {
                barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                    @Override
                    public void release() {

                    }

                    @Override
                    public void receiveDetections(Detector.Detections<Barcode> detections) {
                        SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                        orderId = 0;
                        if(qrCodes.size() != 0){
                            String qrCodeString = qrCodes.valueAt(0).displayValue;
                            String[] results = qrCodeString.split(";");
                            orderId = Long.valueOf(results[0]);
                        }
                    }
                });
                if(orderId > 0){ //change if needed
                    callOrderInfoWebService();
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

    private void callOrderInfoWebService(){
        new OrderInfoWebServiceHandler().execute(WebServiceConfig.address + "/address/" + orderId);
    }

    private class OrderInfoWebServiceHandler extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog = new ProgressDialog(OrderInfoActivity.this);

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
                OrderAddressInfo orderAddressInfo = new OrderAddressInfo();
                orderAddressInfo.setOrderId(object.optLong("orderId"));
                orderAddressInfo.setCustomerId(object.optLong("customerId"));
                orderAddressInfo.setCustomerName(object.optString("customerName"));
                orderAddressInfo.setCustomerLastName(object.optString("customerLastName"));
                orderAddressInfo.setCity(object.optString("city"));
                orderAddressInfo.setPostcode(object.optString("postcode"));
                orderAddressInfo.setBuildingNumber(object.optString("buildingNumber"));
                orderAddressInfo.setStreet(object.optString("street"));
                orderAddressInfo.setFlatnumber(object.optString("flatumber"));
                updateGUI(orderAddressInfo.getCustomerName() + " " + orderAddressInfo.getCustomerLastName(),
                        orderAddressInfo.getCity() + " " + orderAddressInfo.getPostcode(),
                        orderAddressInfo.getStreet() + " " + orderAddressInfo.getBuildingNumber() + "/" + orderAddressInfo.getFlatnumber());
            } catch (Exception e) {
                new AlertDialog.Builder(OrderInfoActivity.this)
                        .setTitle("Error")
                        .setMessage("Check Internet connection")
                        .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                callOrderInfoWebService();
                            }
                        }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
            }
        }
    }

    private void updateGUI(String clientName, String address1, String address2) {
        clientNameTxt.setText("Client: " + clientName);
        address1Txt.setText("Address: " + address1);
        address2Txt.setText(address2);
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