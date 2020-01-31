package com.example.ble_communication_lib;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import com.sky.hori_t.util.Log;

import org.junit.Test;

public class BleDataCommunicationServerTest {
    private final BleDataCommunicationServer.Callback mCallback =
            new BleDataCommunicationServer.Callback() {
        @Override
        public void onWaitingStarted() {
            Log.d("");
        }

        @Override
        public void onWaitingStopped() {
            Log.d("");
        }

        @Override
        public void onConnected(@NonNull BluetoothDevice device) {
            Log.d("Device: " + device);
            mServer.sendData(device, "1234".getBytes());
        }

        @Override
        public void onDisconnected(@NonNull BluetoothDevice device) {
            Log.d("Device: " + device);
        }

        @Override
        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull byte[] data) {
            Log.d("Device: " + device + " Data: " + new String(data));
            if (new String(data).equals("disconnect")) {
                mServer.disconnect(device);
            }
        }

        @Override
        public void onError(@NonNull BleDataCommunicationException exception) {
            exception.printStackTrace();
        }
    };

    private final BleDataCommunicationServer mServer = new BleDataCommunicationServer(
            InstrumentationRegistry.getContext(),
            null,
            mCallback
    );

    @Test
    public void start() {
        Mutex mutex = new Mutex(10 * 60 * 1000);
        mServer.startWaiting();
        mutex.lock();
    }
}