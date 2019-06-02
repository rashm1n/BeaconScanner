package com.example.beaconscanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private Handler hand;
    private HashMap<String, BLTE_Device> mBTDevicesHashMap;
    private ArrayList<BLTE_Device> mBTDevicesArrayList;
    ListAdapter_BTLE_Devices adapter;
    private Scanner_BLTE mBTLeScanner;
    Button button;
    private ArrayList<String> itrList = new ArrayList<>();
    private ArrayList<String> macList = new ArrayList<>();
    private ArrayList<String> allValues = new ArrayList<>();
    final String MAC_ADDRESS ="CA:92:D2:A5:41:2B";
    EditText macaddrees;
    public String data;
    public List<String[]> stringlist;
    private FileWriter mFileWriter;
    private static final int PERMISSION_REQUEST_CODE = 200;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stringlist = new ArrayList();

        String[] a = new String[2];
        a[0] = "distance";
        a[1] = "rssi";

        stringlist.add(a);

        if (!checkPermission()) {
            openActivity();
        } else {
            if (checkPermission()) {
                requestPermissionAndContinue();
            } else {
                openActivity();
            }
        }

        macaddrees = (EditText)findViewById(R.id.mac_adrs);
        EditText distance = (EditText)findViewById(R.id.distance_beacon);
        ListView listView = (ListView)findViewById(R.id.list);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            System.out.println("BLE NOT SUPPORTED");
            finish();
        }

        hand = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBTLeScanner = new Scanner_BLTE(this, 100000, -100);
        }

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();
        adapter = new ListAdapter_BTLE_Devices(MainActivity.this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        listView.setAdapter(adapter);

        button = (Button)findViewById(R.id.scan_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBTLeScanner.isScanning()) {
                    macList.clear();
                    itrList.clear();
                    allValues.clear();
                    startScan();
                }
                else {
                    stopScan();
                }
            }
        });
    }

    public synchronized void stopScan() {
        mBTLeScanner.stop();
        try {
            writeCSV(stringlist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addDevice(BluetoothDevice device, int rssi) {
        String address = device.getAddress();
        if (!mBTDevicesHashMap.containsKey(address)) {
            BLTE_Device btleDevice = new BLTE_Device(device);
            btleDevice.setRSSI(rssi);
            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);

//            data = editString(data,"23",Integer.toString(rssi));

            String[] a = new String[2];
            a[0] = "23";
            a[1] = Integer.toString(rssi);

            stringlist.add(a);

//            System.out.println(device.getAddress());
//            System.out.println(macaddrees.getText().toString());
        }
        else {
            System.out.println(device.getAddress());
            System.out.println(macaddrees.getText().toString());
            adapter.notifyDataSetChanged();
            mBTDevicesHashMap.get(address).setRSSI(rssi);

            String[] a = new String[2];
            a[0] = "23";
            a[1] = Integer.toString(rssi);
            stringlist.add(a);
        }
        adapter.notifyDataSetChanged();
    }

    public void startScan(){
        mBTLeScanner.start();
    }

    public void writeCSV(List<String[]> a) throws IOException {
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d("Write",baseDir);
        String fileName = "AnalysisDatafinal.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer;
        // File exist
        if(f.exists()&&!f.isDirectory())
        {
            mFileWriter = new FileWriter(filePath, true);
            writer = new CSVWriter(mFileWriter);
        }
        else
        {
            writer = new CSVWriter(new FileWriter(filePath));
        }

        for (String[] s:a){
            writer.writeNext(s);
        }

        writer.close();
        Log.d("Write","written");
        System.out.println("writeeeeeeeeeeeeeeeeee");
    }

    public String editString(String original,String value1, String value2){
        StringBuilder stringBuilder = new StringBuilder(original);
        stringBuilder.append(value1);
        stringBuilder.append(",");
        stringBuilder.append(value2);
        stringBuilder.append(System.lineSeparator());
        return stringBuilder.toString();
    }

    private boolean checkPermission() {

        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ;
    }

    private void requestPermissionAndContinue() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("permission_necessary");
                alertBuilder.setMessage("storage_permission_is_encessary_to_wrote_event");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE
                                , READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                Log.e("", "permission denied, show dialog");
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE,
                        READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            openActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0 && grantResults.length > 0) {

                boolean flag = true;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        flag = false;
                    }
                }
                if (flag) {
                    openActivity();
                } else {
                    finish();
                }

            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void openActivity() {
        //add your further process after giving permission or to download images from remote server.
    }
}
