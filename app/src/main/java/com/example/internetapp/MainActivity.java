package com.example.internetapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.internetapp.databinding.ActivityMainBinding;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    BluetoothAdapter bluetoothAdapter;
    UUID SERVICE_UUID = null;
    UUID CHARACTERISTIC_UUID = null;
    UUID DESCRIPTOR = null;

    BluetoothLeScanner bluetoothLeScanner;
    boolean scanning;
    Handler handler = new Handler();

    BluetoothGatt bluetoothGatt;
    BluetoothDevice device;

    ActivityResultLauncher<String[]> launcher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (result.containsValue(false)) {
                    Log.d("info", "no permissions :(");
                } else {
                    Log.d("info", "all permissions granted");
                    scanLeDevice();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* wifi */
        //performGetProfile();
        Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> {
            saveData(5,6); //dummy data
        });

        /* ble */
        binding.buttonScan.setOnClickListener(view -> {
            startBT();
        });

        binding.buttonConnect.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        });

    }

    void startBT() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null) { // ca marche
            if (bluetoothAdapter.isEnabled()) { //bluetooth is on
                boolean bluetoothLEAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
                if (bluetoothLEAvailable) {
                    scanLeDevice();
                } else {
                    Toast.makeText(MainActivity.this, "bluetooth LE no compatible", Toast.LENGTH_SHORT).show();
                }
            } else { // bluetooth is off
                Toast.makeText(MainActivity.this, "bluetooth off, please turn on", Toast.LENGTH_SHORT).show();
            }
        } else { //no bluetooth on the device
            Toast.makeText(MainActivity.this, "No bluetooth on device", Toast.LENGTH_SHORT).show();
        }
    }

    public void scanLeDevice() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT});
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        }

        if (!scanning) {
            //start scanning :D
            bluetoothLeScanner.startScan(leScanCallback);
            scanning = true;

            handler.postDelayed(() -> {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
            }, 20000);

        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }

    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (result.getDevice().getName() != null && result.getDevice().getName().contains("ESP32")) {
                device = result.getDevice();
                Log.d("info", "device name: " + device.getName());
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }
    };

    public void performGetProfile() {
        TypicodeService typicodeService = new Retrofit.Builder()
                .baseUrl("https://my-json-server.typicode.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TypicodeService.class);

        typicodeService.getProfile().enqueue(new Callback<Profile>() {
            @Override
            public void onResponse(Call<Profile> call, Response<Profile> response) {
                if (response.isSuccessful()) {
                    Profile profile = response.body();
                    binding.textview.setText(profile.getName());
                }
            }

            @Override
            public void onFailure(Call<Profile> call, Throwable t) {
            }
        });
    }



    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) { // successfully connected to the GATT Server
                Log.d("info", "connected to the server");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Log.d("info", "disconnected to the server");
                closeGatt();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("info", "success: " + status);

                mainFor:
                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            Log.d("info", service.getUuid() + "|" + characteristic.getUuid() + "|" + descriptor.getUuid());
                            if (service.getUuid().toString().contains("4fa")) {
                                SERVICE_UUID = service.getUuid();
                                CHARACTERISTIC_UUID = characteristic.getUuid();
                                DESCRIPTOR = descriptor.getUuid();
                                break mainFor;
                            }
                        }
                    }
                }
                if (SERVICE_UUID != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID);
                    bluetoothGatt.setCharacteristicNotification(characteristic, true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR); //UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(descriptor);
                    runOnUiThread(() -> {
                        binding.buttonSend.setEnabled(true);
                    });
                }

            } else {
                Log.w("info", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //for 1 int
            int x = java.nio.ByteBuffer.wrap(characteristic.getValue()).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            Log.d("info", String.format("xx: %d", x));
            saveData(x,x);
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.disconnect();
    }

    private void closeGatt() {
        if (bluetoothGatt == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void saveData(float latitude, float longitude) {
        IotService service = new Retrofit.Builder()
                .baseUrl("http://172.20.10.6:3000")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(IotService.class);

        service.saveData(latitude, longitude).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d("info", "data sent correctly");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("info", "something bad happened :(");
                t.printStackTrace();
            }
        });
    }
}