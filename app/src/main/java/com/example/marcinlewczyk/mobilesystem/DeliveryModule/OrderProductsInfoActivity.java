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
import android.widget.ListView;

import com.example.marcinlewczyk.mobilesystem.Adapters.OrderProductsAdapter;
import com.example.marcinlewczyk.mobilesystem.Config.WebServiceConfig;
import com.example.marcinlewczyk.mobilesystem.POJO.DishInfo;
import com.example.marcinlewczyk.mobilesystem.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class OrderProductsInfoActivity extends AppCompatActivity {
    private SurfaceView cameraPreview;
    private ListView orderProductsListView;
    private Button readCodeInboundButton;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private final int RequestCameraPermissionID = 1001;
    private long orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_products_info);
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
        orderProductsListView = findViewById(R.id.orderProductsListView);
    }

    private void setTitleBarContent(){
        setTitle("Check dishes in order");
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
                    ActivityCompat.requestPermissions(OrderProductsInfoActivity.this, new String[]{android.Manifest.permission.CAMERA}, RequestCameraPermissionID);
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
                    callOrderProductsInfoWebService();
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

    private void callOrderProductsInfoWebService(){
        new OrderProductsWebServiceHandler().execute(WebServiceConfig.address + "/order/" + orderId);
    }

    private class OrderProductsWebServiceHandler extends AsyncTask<String, Void, String> {

        private ProgressDialog dialog = new ProgressDialog(OrderProductsInfoActivity.this);

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
            ArrayList<DishInfo> dishesInfo = new ArrayList<>();
            try {
                JSONObject container = new JSONObject(result);
                JSONArray jsonArray = container.optJSONArray("dishes");
                for(int i = 0; i < jsonArray.length(); i++){
                    JSONObject dish = jsonArray.getJSONObject(i);
                    dishesInfo.add(new DishInfo(dish.optString("dishName"), dish.optInt("quantity")));
                }

                setListViewContent(dishesInfo);
            } catch (Exception e) {
                new AlertDialog.Builder(OrderProductsInfoActivity.this)
                        .setTitle("Error")
                        .setMessage("Check Internet connection")
                        .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                callOrderProductsInfoWebService();
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

    private void setListViewContent(ArrayList content){
        OrderProductsAdapter adapter = new OrderProductsAdapter(getApplicationContext(), content);
        orderProductsListView.setAdapter(adapter);
    }
}