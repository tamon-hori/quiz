package com.hori_t.ble_communication_lib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sky.hori_t.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;

/**
 * BLEのGATTを用いて{@link BleDataCommunicationClient}とデータの送受信を行うクラスです。サーバ側となる場合に用います。
 */
@SuppressWarnings({"unused","WeakerAccess"})
public class BleDataCommunicationServer {
    /**
     * {@link BleDataCommunicationServer}からの通知を受け取るためのコールバックインターフェースです。
     */
    public interface Callback {
        /**
         * 接続待ち開始通知
         */
        void onWaitingStarted();

        /**
         * 接続待ち終了通知
         */
        void onWaitingStopped();

        /**
         * クライアント側端末からの通信接続通知
         * @param device  接続したクライアント側端末の端末情報
         */
        void onConnected(@NonNull BluetoothDevice device);

        /**
         * クライアント側端末との通信切断通知
         * @param device    切断したクライアント側端末の端末情報
         */
        void onDisconnected(@NonNull BluetoothDevice device);

        /**
         * データ受信通知
         * @param device    送信元クライアント側端末の端末情報
         * @param data      受信したデータ
         */
        void onDataReceived(@NonNull BluetoothDevice device, @NonNull byte[] data);

        /**
         * エラー発生による停止通知
         * @param exception    エラー理由
         */
        void onError(@NonNull BleDataCommunicationException exception);
    }

    private enum State {
        Stopped,
        Starting,
        ConnectWaiting,
        Stopping,
    }

    @NonNull
    private State mState = State.Stopped;

    @NonNull
    private final Context mContext;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final CallbackExecutor mCallback;

    @NonNull
    private final BluetoothManager mManager;
    @NonNull
    private final BluetoothAdapter mAdapter;
    @Nullable
    private BluetoothGattServer mServer;
    @Nullable
    private BluetoothGattCharacteristic mCharacteristic;
    @NonNull
    private LinkedHashMap<BluetoothDevice, ReceivePacketLinker> mDeviceMap =
            new LinkedHashMap<>();
    @NonNull
    private final BluetoothGattServerCallback mGattCallback =
            new BluetoothGattServerCallback() {
                @Override
                public void onConnectionStateChange(
                        BluetoothDevice device, int status, int newState) {
                    Log.vMethodIn("newState:" + newState);
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED: {
                            mHandler.post(() -> _onConnected(device));
                            break;
                        }
                        case BluetoothProfile.STATE_DISCONNECTED: {
                            mHandler.post(() -> _onDisconnected(device));
                            break;
                        }
                    }
                    Log.vMethodOut();
                }

                @Override
                public void onCharacteristicWriteRequest(
                        BluetoothDevice device, int requestId,
                        BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                        boolean responseNeeded, int offset, byte[] value) {
                    Log.vMethodIn();
                    mHandler.post(() -> _onCharacteristicWriteRequest(
                            device, characteristic, requestId, offset, value));
                    Log.vMethodOut();
                }

                @Override
                public void onDescriptorWriteRequest(
                        BluetoothDevice device, int requestId,
                        BluetoothGattDescriptor descriptor, boolean preparedWrite,
                        boolean responseNeeded, int offset, byte[] value) {
                    Log.vMethodIn();
                    mHandler.post(() -> _onDescriptorWriteRequest(
                            device, requestId, offset, value));
                    Log.vMethodOut();
                }

//                @Override
//                public void onMtuChanged(BluetoothDevice device, int mtu) {
//                    mHandler.post(() -> _onMtuChanged(device, mtu));
//                }
            };

    /**
     * コンストラクタ
     * @param context                   コンテキスト
     * @param dispatchQueueLooper     コールバックを実行するスレッドのルーパー
     * @param callback                  通知を受け取るためのコールバック
     */
    public BleDataCommunicationServer(
            @NonNull Context context,
            @Nullable Looper dispatchQueueLooper,
            @NonNull Callback callback) throws UnsupportedOperationException {
        Log.vMethodIn();
        mContext = context;
        BluetoothManager manager = (BluetoothManager) context.getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (null == manager) {
            throw new UnsupportedOperationException("This device does not support BLE.");
        }
        mManager = manager;
        mAdapter = manager.getAdapter();

        // 自スレッド生成
        HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        // コールバックインスタンス生成
        Handler callbackHandler;
        if (dispatchQueueLooper == null) {
            HandlerThread callbackThread =
                    new HandlerThread(getClass().getSimpleName() + "Callback");
            callbackThread.start();
            callbackHandler = new Handler(callbackThread.getLooper());
        } else {
            callbackHandler = new Handler(dispatchQueueLooper);
        }
        mCallback = new CallbackExecutor(callback, callbackHandler);
        Log.vMethodOut();
    }

    /**
     * クライアント側端末からの接続待ち状態に移ります。<br>
     * 接続待ちに移ると{@link Callback#onWaitingStarted()}が通知されます。
     */
    public void startWaiting() {
        Log.vMethodIn();
        if (State.Stopped != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        _startWaiting();
        Log.vMethodOut();
    }

    /**
     * 接続待ち状態を終了します。<br>
     * クライアント側端末からの接続がある場合は切断してから閉じます。<br>
     * 終了すると{@link Callback#onWaitingStopped()}が通知されます。
     */
    public void stopWaiting() {
        Log.vMethodIn();
        if (State.ConnectWaiting != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        mHandler.post(this::_stopWaiting);
        Log.vMethodOut();
    }

    /**
     * 引数で指定したクライアント側端末に任意データを送信します。
     * @param device    送信先クライアント側端末の端末情報
     * @param data      送信するデータ
     * @throws IllegalStateException    接続が確立していない段階で呼び出した場合に投げられます。
     */
    public void sendData(@NonNull BluetoothDevice device, @NonNull byte[] data)
            throws IllegalStateException {
        Log.vMethodIn("Device: " + device + " Data: " + new String(data));
        if (State.ConnectWaiting != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        if (!isConnected(device)) {
            throw new IllegalStateException(
                    "The device name \"" + device.getName() + "\" has not been connected.");
        }
        mHandler.post(() -> _sendData(device, data));
        Log.vMethodOut();
    }

    /**
     * 引数で指定したクライアント側端末からの接続を切断します。<br>
     * 切断が完了すると{@link Callback#onDisconnected(BluetoothDevice)}が通知されます。
     * @param device  切断するクライアント側端末の端末情報
     */
    public void disconnect(BluetoothDevice device) throws IllegalStateException {
        Log.vMethodIn();
        if (State.ConnectWaiting != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        if (!isConnected(device)) {
            throw new IllegalStateException(
                    "The device name \"" + device.getName() + "\" has not been connected.");
        }
        mHandler.post(() -> _disconnect(device));
        Log.vMethodOut();
    }

    /**
     * 現在接続している全ての端末の端末情報を返します。
     * @return  接続している全ての端末情報
     */
    @NonNull
    public ArrayList<BluetoothDevice> getAllDeviceInfo() {
        Log.vMethodIn();
        Mutex<ArrayList<BluetoothDevice>> mutex = new Mutex<>();
        mHandler.post(() -> {
            ArrayList<BluetoothDevice> deviceList = new ArrayList<>(mDeviceMap.keySet());
            mutex.unlock(deviceList);
        });
        Log.vMethodOut();
        return mutex.lock();
    }

    private State getState() {
        Log.vMethodIn();
        State state;
        if (Thread.currentThread() == mHandler.getLooper().getThread()) {
            state = mState;
        } else {
            Mutex<State> mutex = new Mutex<>();
            mHandler.post(() -> mutex.unlock(mState));
            state = mutex.lock();
        }
        Log.vMethodOut(state.toString());
        return state;
    }

    private boolean isConnected(@NonNull BluetoothDevice device) {
        Log.vMethodIn();
        Mutex<Boolean> mutex = new Mutex<>();
        mHandler.post(() -> mutex.unlock(mDeviceMap.containsKey(device)));
        Log.vMethodOut();
        return mutex.lock();
    }

    private void _startWaiting() {
        Log.vMethodIn();
        mState = State.Starting;
        BluetoothGattServer server = mManager.openGattServer(mContext, mGattCallback);
        if (null == server) {
            return;
            // TODO
        }

        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(Constants.Uuids.SERVICE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mCharacteristic = new BluetoothGattCharacteristic(
                UUID.fromString(Constants.Uuids.CHARACTERISTIC),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattDescriptor.PERMISSION_WRITE |
                        BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(mCharacteristic);

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                UUID.fromString(Constants.Uuids.CCCD),
                BluetoothGattDescriptor.PERMISSION_WRITE |
                        BluetoothGattDescriptor.PERMISSION_READ);
        mCharacteristic.addDescriptor(descriptor);

        mServer = mManager.openGattServer(mContext, mGattCallback);
        if (null == mServer) {
            return;
            // TODO
        }
        mServer.addService(service);

        BluetoothLeAdvertiser advertiser = mAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID.fromString(Constants.Uuids.ADVERTISE)))
                .build();

        advertiser.startAdvertising(settings, data, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.vMethodIn();
                mHandler.post(() -> {
                    mState = State.ConnectWaiting;
                    mCallback.onWaitingStarted();
                });
                Log.vMethodOut();
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.vMethodIn();
                // TODO
                Log.vMethodOut();
            }
        });
        Log.vMethodOut();
    }

    private void _stopWaiting() {
        Log.vMethodIn();
        for (BluetoothDevice device : mDeviceMap.keySet()) {
            if (null != device) {
                mServer.cancelConnection(device);
            }
        }
        if (null != mServer) {
            mServer.close();
            mServer = null;
        }
        mCallback.onWaitingStopped();
        Log.vMethodOut();
    }

    private void  _sendData(
            @NonNull BluetoothDevice device, @NonNull byte[] data) {
        Log.vMethodIn("Device: " + device + " Data: " + new String(data));
        Objects.requireNonNull(mServer);
        Objects.requireNonNull(mCharacteristic);
        // 1回の送信で送れるデータサイズのデフォルト値。
        int mBlockSize = Constants.DEFAULT_BLOCK_SIZE;
        LinkedList<byte[]> splitData = Constants.splitData(data, mBlockSize);
        for (byte[] block : splitData) {
            mCharacteristic.setValue(block);
            boolean result = mServer.notifyCharacteristicChanged(
                    device, mCharacteristic, false);
            if (!result) {
                // TODO エラー
            }
        }
        Log.vMethodOut();
    }

    private void _disconnect(@NonNull BluetoothDevice device) {
        Log.vMethodIn("Device: " + device);
        Objects.requireNonNull(mServer);
        mServer.cancelConnection(device);
        Log.vMethodOut();
    }

    private void _onConnected(@NonNull BluetoothDevice device) {
        Log.vMethodIn("Device: " + device);
        Objects.requireNonNull(mServer);
        if (!(getState() == State.ConnectWaiting) ||
                (Constants.NUM_OF_CONNECTIBLE_DEVICE <= mDeviceMap.size())) {
            mServer.cancelConnection(device);
            return;
        }
        mDeviceMap.put(device, new ReceivePacketLinker());
        Log.vMethodOut();
    }

    private void _onDisconnected(@NonNull BluetoothDevice device) {
        Log.vMethodIn("Device: " + device);
        if (!mDeviceMap.containsKey(device)) {
            return;
        }
        mDeviceMap.remove(device);
        mCallback.onDisconnected(device);
        Log.vMethodOut();
    }

//    private void _onMtuChanged(@NonNull BluetoothDevice device, int mtu) {
//
//    }

    private void _onDescriptorWriteRequest(
            @NonNull BluetoothDevice device, int requestId, int offset, @NonNull byte[] value) {
        Log.vMethodIn("Device: " + device);
        Objects.requireNonNull(mServer);
        mServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        Log.vMethodOut();
    }

    private void _onCharacteristicWriteRequest(
            @NonNull BluetoothDevice device,
            @NonNull BluetoothGattCharacteristic characteristic,
            int requestId,
            int offset,
            @NonNull byte[] value) {
        Log.vMethodIn("Device: " + device);
        Objects.requireNonNull(mServer);
        // Characteristicの確認
//        if (checkCharacteristicInvalidity(characteristic)) {
//            mServer.sendResponse(
//                    device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
//            return;
//        }
        // 接続済み機器かどうか確認
        ReceivePacketLinker linker = mDeviceMap.get(device);
        if (null == linker) {
            mServer.sendResponse(
                    device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
            Log.vMethodOut("null == linker");
            return;
        }
        // データ結合
        try {
            linker.link(value);
        } catch (IOException e) {
            e.printStackTrace();
            mDeviceMap.put(device, new ReceivePacketLinker());
            mServer.sendResponse(
                    device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
            return;
        }
        // Client側に成功通知
        mServer.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        byte[] receivedPacket = linker.getLinkedPacket();
        // 結合が完了していたら通知
        if (null == receivedPacket) {
            Log.vMethodOut("Packet has not linked.");
            return;
        }
        mDeviceMap.put(device, new ReceivePacketLinker());
        if (Constants.CONNECTED_SIGN.equals(new String(receivedPacket))) {
            mCallback.onConnected(device);
            Log.vMethodOut("Connected sign: " + device);
            return;
        }
        mCallback.onDataReceived(device, receivedPacket);
        Log.vMethodOut("Data received: " + device
                + " Packet : " + new String(receivedPacket));
    }

    /**
     * このクラスで用いているCharacteristic以外かどうかを確認する
     * @param characteristic    確認するCharacteristic
     * @return  true:  このクラスで用いているCharacteristic以外
     *           false: このクラスで用いているCharacteristic
     */
    private boolean checkCharacteristicInvalidity(
            @NonNull BluetoothGattCharacteristic characteristic) {
        Log.vMethodIn();
        UUID serviceUuid = characteristic.getService().getUuid();
        UUID characteristicUuid = characteristic.getUuid();
        Log.vMethodOut();
        return !serviceUuid.equals(Constants.Uuids.SERVICE) ||
                !characteristicUuid.equals(Constants.Uuids.CHARACTERISTIC);
    }

    private class CallbackExecutor implements Callback {
        @NonNull
        private final Callback mCallback;
        @NonNull
        private final Handler mHandler;

        CallbackExecutor(@NonNull Callback callback, @NonNull Handler handler) {
            Log.vMethodIn();
            mCallback = callback;
            mHandler = handler;
            Log.vMethodOut();
        }

        @Override
        public void onWaitingStarted() {
            Log.vMethodIn();
            mHandler.post(mCallback::onWaitingStarted);
            Log.vMethodOut();
        }

        @Override
        public void onWaitingStopped() {
            Log.vMethodIn();
            mHandler.post(mCallback::onWaitingStopped);
            Log.vMethodOut();
        }

        @Override
        public void onConnected(@NonNull final BluetoothDevice device) {
            Log.vMethodIn("Device: " + device);
            mHandler.post(() -> mCallback.onConnected(device));
            Log.vMethodOut();
        }

        @Override
        public void onDisconnected(@NonNull final BluetoothDevice device) {
            Log.vMethodIn("Device: " + device);
            mHandler.post(() -> mCallback.onDisconnected(device));
            Log.vMethodOut();
        }

        @Override
        public void onDataReceived(
                @NonNull final BluetoothDevice device,
                @NonNull final byte[] data) {
            Log.vMethodIn("Device: " + device + " Data: " + new String(data));
            mHandler.post(() -> mCallback.onDataReceived(device, data));
            Log.vMethodOut();
        }

        @Override
        public void onError(@NonNull BleDataCommunicationException exception) {
            Log.vMethodIn();
            mHandler.post(() -> mCallback.onError(exception));
            Log.vMethodOut();
        }
    }

    private static class ReceivePacketLinker {
        @Nullable
        private byte[] mLinkedPacket;
        @NonNull
        private final ByteArrayOutputStream mStream = new ByteArrayOutputStream();
        private int mDataLen;
        private boolean mIsReceiving = false;

        private static final int DATA_OFFSET = 4;

        void link(@NonNull byte[] receivePacket) throws IOException {
            Log.vMethodIn("Data: " + receivePacket);
            if (!mIsReceiving) {
                mIsReceiving = true;
                byte[] dataLenByte = Arrays.copyOfRange(receivePacket, 0, DATA_OFFSET);
                mDataLen = ByteBuffer.wrap(dataLenByte).getInt();
                Log.d("Data length: " + mDataLen);
                receivePacket = Arrays.copyOfRange(
                        receivePacket, DATA_OFFSET, receivePacket.length);
            }
            mStream.write(receivePacket);
            if (mDataLen == mStream.size()) {
                mLinkedPacket = mStream.toByteArray();
            }
            Log.vMethodOut();
        }

        @Nullable
        byte[] getLinkedPacket() {
            Log.vMethodIn("isPacketNull: " + String.valueOf(null == mLinkedPacket));
            return mLinkedPacket;
        }
    }
}
