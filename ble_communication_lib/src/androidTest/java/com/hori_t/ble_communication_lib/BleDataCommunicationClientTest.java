package com.hori_t.ble_communication_lib;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import org.junit.Test;

public class BleDataCommunicationClientTest {
    private final BleDataCommunicationClient.Callback mCallback =
            new BleDataCommunicationClient.Callback() {
        @Override
        public void onDiscovered(@NonNull BluetoothDevice device) {
            Log.d("通信Lib", "[onDiscovered]" + device);
            Thread thread = new Thread(() -> mClient.connect(device));
            thread.start();
        }

        @Override
        public void onDiscoveryStopped() {

        }

        @Override
        public void onConnected() {
            Log.d("通信Lib", "------------Connected.-------------");
        }

        @Override
        public void onDisconnected() {

        }

        @Override
        public void onDataReceived(@NonNull byte[] data) {
            Log.d("通信Lib", new String(data));
        }

        @Override
        public void onError(@NonNull BleDataCommunicationException exception) {

        }
    };

    private final BleDataCommunicationClient mClient = new BleDataCommunicationClient(
            InstrumentationRegistry.getContext(),
            null,
            mCallback
    );

    @Test
    public void start() {
        Mutex mutex = new Mutex(10 * 60 * 1000);
        mClient.startDiscovery();
        mutex.lock();
    }

}