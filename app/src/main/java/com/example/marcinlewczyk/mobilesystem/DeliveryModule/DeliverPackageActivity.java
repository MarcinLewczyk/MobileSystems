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

import com.example.marcinlewczyk.mobilesystem.Config.WebServiceConfig;
import com.example.marcinlewczyk.mobilesystem.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class DeliverPackageActivity extends AppCompatActivity {
    private SurfaceView cameraPreview;
    private Button readCodeInboundButton;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private final int RequestCameraPermissionID = 1001;
    private long orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliver_package);
        prepareGUI();
        buildQRScanner();
    }

    private void prepareGUI(){
        initControls();
        setTitleBarContent();
    }

    private void initControls() {
        cameraPreview = findViewById(R.id.cameraPreview);
        readCodeInboundButton = findViewById(R.id.readCodeButton);
    }

    private void setTitleBarContent(){
        setTitle("Deliver Package");
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
                    ActivityCompat.requestPermissions(DeliverPackageActivity.this, new String[]{android.Manifest.permission.CAMERA}, RequestCameraPermissionID);
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
                    callShipPackageWebService();
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

    private void callShipPackageWebService(){
        new DeliverPackageWebServiceHandler().execute(WebServiceConfig.address + "/status2/" + orderId);
    }

    private class DeliverPackageWebServiceHandler extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog = new ProgressDialog(DeliverPackageActivity.this);

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
                    new AlertDialog.Builder(DeliverPackageActivity.this)
                            .setTitle("Processed")
                            .setMessage("Status changed to - Delivered")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }).show();
                } else {
                    new AlertDialog.Builder(DeliverPackageActivity.this)
                            .setTitle("Error")
                            .setMessage("Package can be already shipped")
                            .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                }
            } catch (Exception e) {
                new AlertDialog.Builder(DeliverPackageActivity.this)
                        .setTitle("Error")
                        .setMessage("Check Internet connection")
                        .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                callShipPackageWebService();
                            }
                        }).setCancelable(false).setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
            }
        }
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