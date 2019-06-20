package com.example.bleservice;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BluetoothGattCharacteristic characteristicRead;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBlueToothAdapter;
    private BluetoothGattServer mGattServer;
    //   private String UUID="95f4a5a0-8434-4d94-aefc-71e018a4364e";
    private static final java.util.UUID UUID_SERVICE = java.util.UUID.fromString("0783B03E-8535-B5A0-7140-A304D2495CB7");//蓝牙串口的通用UUID,UUID是什么东西
    private static final java.util.UUID UUID_CHARACTERISTIC_READ = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FC");//
    private static final java.util.UUID UUID_CHARACTERISTIC_WRITE = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FD");
    private static final java.util.UUID UUID_DESCRIPTOR = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FE");
    private static final java.util.UUID UUID_DESCRIPTOR_NOTIFY = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FF");
    private BluetoothGattCharacteristic characteristicWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      //  BluetoothGattCharacteristic
       //         BluetoothGattCallback
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBlueToothAdapter = mBluetoothManager.getAdapter();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {//如果该权限没有获得权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        initGATTServer();
    }

    /**
     * 1.初始化BLE蓝牙广播Advertiser，配置指定UUID的服务
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initGATTServer() {

        AdvertiseSettings settings = new AdvertiseSettings.Builder()//BLE广播设置
                .setConnectable(true)//设置广播是否可连接,广播分为可连接和不可连接的
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)// AdvertiseSettings#ADVERTISE_MODE_LOW_POWER},低功耗
                //AdvertiseSettings#ADVERTISE_MODE_BALANCED},平衡
                //AdvertiseSettings#ADVERTISE_MODE_LOW_LATENCY}.低延迟 设置广播模式
                .setTimeout(0)//设置广播超时时间,默认是0具体不是很理解什么意思
                // .setTxPowerLevel()//设置广播的发送功率级别,极低,低,中,高,具体看源码
                .build();
        //设置广播数据,广播启动就会发送这个数据
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)//设置的数据是否包含蓝牙名称
                .setIncludeTxPowerLevel(true)//设置的数据数据是否包含功率级别
//                .addServiceUuid(true)//设置的数据是否包含UUID
//                .addServiceData()//????
//                .addManufacturerData()设置自定义的数据
                .build();

        //设置广播相应数据,客户端连接的时候才会发送
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID_SERVICE))//设置服务ServiceUUID
                .setIncludeTxPowerLevel(true)//设置的数据数据是否包含功率级别
                .build();

        //广播创建成功之后的回调
        AdvertiseCallback callback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "BLE advertisement added successfully");
                //showText("1. initGATTServer success");
                //println("1. initGATTServer success");
                //初始化服务
                initServices(MainActivity.this);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.d(TAG, "Failed to add BLE advertisement, reason: " + errorCode);
                //showText("1. initGATTServer failure");
            }
        };

        //部分设备不支持Ble中心
        BluetoothLeAdvertiser bluetoothLeAdvertiser = mBlueToothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null) {
            Log.d(TAG, "initGATTServer:BluetoothLeAdvertiser为null ");
        }

        //开始广播
        if (bluetoothLeAdvertiser != null) {//todo 需要停止广播
            Log.d(TAG, "initGATTServer: 开始广播");
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback);//开发其他设备搜索
        }
    }

    /**
     * 初始化Gatt服务，主要是配置Gatt服务各种UUID
     *
     * @param context
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void initServices(Context context) {
        //创建GattServer服务器
        mGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);//开启GATTService其他设备才能发现BLE服务

        //这个指定的创建指定UUID的服务
        BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

//        //添加指定UUID的可读characteristic
//        characteristicRead = new BluetoothGattCharacteristic(
//                UUID_CHARACTERISTIC_READ,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ);
//
//        //添加可读characteristic的descriptor
////        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
////        characteristicRead.addDescriptor(descriptor);
//        service.addCharacteristic(characteristicRead);


        //添加可读可写可通知的characteristic
        characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARACTERISTIC_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE|
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);//todo PERMISSION_WRITE 和PROPERTY_NOTIFY如何使用

        //
        BluetoothGattDescriptor descriptorNotify = new BluetoothGattDescriptor(UUID_DESCRIPTOR_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicWrite.addDescriptor(descriptorNotify);


        service.addCharacteristic(characteristicWrite);

        mGattServer.addService(service);
        Log.e(TAG, "2. initServices ok1");
        //showText("2. initServices ok");
    }

    /**
     * 服务事件的回调
     */
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {

        /**
         * 1.连接状态发生变化时
         * @param device
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, String.format("1.onConnectionStateChange：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.d(TAG, String.format("1.onConnectionStateChange：status = %s, newState =%s ", status, newState));
            super.onConnectionStateChange(device, status, newState);
        }

        /**
         * 服务添加的时候回调
         * @param status
         * @param service
         */
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d(TAG, String.format("onServiceAdded：status = %s", status));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, String.format("onCharacteristicReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.d(TAG, String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset));

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
//            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        /**
         * 3. onCharacteristicWriteRequest,接收具体的字节
         * @param device
         * @param requestId
         * @param characteristic
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param requestBytes
         */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            Log.d(TAG, String.format("3.onCharacteristicWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.d(TAG, String.format("3.onCharacteristicWriteRequest：requestId = %s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite, responseNeeded, offset, requestBytes.toString()));
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);
            //4.处理响应内容
            onResponseToClient(requestBytes, device, requestId, characteristic);
        }

        /**
         * 2.描述被写入时，在这里执行 bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS...  收，触发 onCharacteristicWriteRequest
         * @param device
         * @param requestId
         * @param descriptor
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param value
         */
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, String.format("2.onDescriptorWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.d(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, value.toString()));

            // now tell the connected device that this was all successfull
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);


            final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            String response = "CHAR_" + (int) (Math.random() * 100); //模拟数据
            characteristic.setValue(response);
            mGattServer.notifyCharacteristicChanged(device, characteristic, false);

        }

        /**
         * 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
         * @param device
         * @param requestId
         * @param offset
         * @param descriptor
         */
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, String.format("onDescriptorReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.d(TAG, String.format("onDescriptorReadRequest：requestId = %s", requestId));
//            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.d(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.d(TAG, String.format("5.onNotificationSent：status = %s", status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.d(TAG, String.format("onMtuChanged：mtu = %s", mtu));
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            Log.d(TAG, String.format("onExecuteWrite：requestId = %s", requestId));
        }
    };

    /**
     * 4.处理onCharacteristicWriteRequest响应内容
     *
     * @param reqeustBytes
     * @param device
     * @param requestId
     * @param characteristic
     */
    private void onResponseToClient(byte[] reqeustBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, String.format("4.onResponseToClient：device name = %s, address = %s", device.getName(), device.getAddress()));
        Log.d(TAG, String.format("4.onResponseToClient：requestId = %s", requestId));
//        String msg = OutputStringUtil.transferForPrint(reqeustBytes);
        Log.d(TAG, "4.收到：reqeustBytes1=" + reqeustBytes[0]);
        //println("4.收到:" + msg);
        //showText("4.收到:" + msg);

//        String str = new String(reqeustBytes) + " hello BLE Recevie Your Data";
//        characteristicRead.setValue(str.getBytes());
//        mGattServer.notifyCharacteristicChanged(device, characteristicRead, false);//通知客户端服务器收到消息

//        String strTemp = new String(reqeustBytes) + " hello BLE Recevie Your Data";
//        characteristicWrite.setValue(strTemp.getBytes());
//        characteristic
//        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();

        //       mGattServer.notifyCharacteristicChanged(device, characteristicWrite, false);//通知客户端服务器收到消息


      //  Log.i(TAG, "4.响应：" + str);
        //   MainActivity.handler.obtainMessage(MainActivity.DEVICE, new String(reqeustBytes)).sendToTarget();
        //println("4.响应:" + str);
        //showText("4.响应:" + str);
    }


}
