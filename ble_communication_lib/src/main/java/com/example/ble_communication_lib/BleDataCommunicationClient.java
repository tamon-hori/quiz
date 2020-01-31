package com.example.ble_communication_lib;

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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * BLEのGATTを用いて{@link BleDataCommunicationServer}とデータの送受信を行うクラスです。クライアント側となる場合に用います。
 */
//@SuppressWarnings({"unused"})
public class BleDataCommunicationClient {
    /**
     *{@link BleDataCommunicationClient}からの通知を受け取るためのコールバックインターフェースです。<br>
     */
    public interface Callback {
        /**
         * サーバ側端末発見通知
         * @param device    発見したサーバ側端末の端末情報
         */
        void onDiscovered(@NonNull BluetoothDevice device);

        /**
         * サーバ側端末探索終了通知
         */
        void onDiscoveryStopped();

        /**
         * サーバ側端末との通信接続通知
         */
        void onConnected();

        /**
         * サーバ側端末との通信切断通知
         */
        void onDisconnected();

        /**
         * データ受信通知
         * @param data  受信したデータ
         */
        void onDataReceived(@NonNull byte[] data);

        /**
         * エラー発生による停止通知
         * @param exception    エラー理由
         */
        void onError(@NonNull BleDataCommunicationException exception);
    }

    private enum State {
        Stopped,
        Scanning,
        Connecting,
        Connected,
        Stopping,
    }

    @NonNull
    private State mState = State.Stopped;
    @Nullable
    private BleDataCommunicationException mException;

    @NonNull
    private final Context mContext;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final CallbackExecutor mCallback;
    @NonNull
    private final BluetoothLeScanner mScanner;
    @NonNull
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mHandler.post(() -> _onScanResult(result.getDevice()));
        }

        @Override
        public void onScanFailed(int errorCode) {
            // TODO
        }
    };
    @NonNull
    private final List<BluetoothDevice> mScannedDevice = new ArrayList<>();
    @Nullable
    private BluetoothGatt mGatt;
    @NonNull
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED :
                    mHandler.post(() -> onConnected(gatt));
                    break;
                case BluetoothProfile.STATE_DISCONNECTED :
                    mHandler.post(() -> onDisconnected());
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (BluetoothGatt.GATT_SUCCESS == status) {
                mHandler.post(() -> _onServiceDiscovered());
            } else {
                // TODO
            }
        }

        @Override
        public void onDescriptorWrite(
                BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (BluetoothGatt.GATT_SUCCESS == status) {
                mHandler.post(() -> _onDescriptorWrite());
            } else {
                // TODO
            }
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (BluetoothGatt.GATT_SUCCESS == status) {
                mHandler.post(() -> _onCharacteristicWrite());
            } else {

            }
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            mHandler.post(() -> _onCharacteristicChanged(data));
        }

//        @Override
//        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
//            if (BluetoothGatt.GATT_SUCCESS == status) {
//
//            } else {
//
//            }
//        }
    };
    @Nullable
    private BluetoothGattCharacteristic mCharacteristic;
    @NonNull
    private final LinkedList<byte[]> mSendQueue = new LinkedList<>();
    // 1回の送信で送れるデータサイズのデフォルト値。MTU変更により設定可能。
    private int mBlockSize = Constants.DEFAULT_BLOCK_SIZE;
    private boolean isWriting = false;
    @Nullable
    private ReceivePacketLinker mLinker = new ReceivePacketLinker();

    /**
     * コンストラクタ
     * @param context   コンテキスト
     * @param dispatchQueueLooper   コールバックイベント処理に使うルーパー
     * @param callback  通知を受け取るためのコールバック
     * @throws UnsupportedOperationException    端末がBLEに対応していない場合に投げられます。
     */
    public BleDataCommunicationClient(
            @NonNull Context context,
            @Nullable Looper dispatchQueueLooper,
            @NonNull Callback callback) throws UnsupportedOperationException {
        mContext = context;
        BluetoothManager manager = (BluetoothManager) context.getApplicationContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);
        if (null == manager) {
            throw new UnsupportedOperationException("This device does not support BLE.");
        }

        mScanner = manager.getAdapter().getBluetoothLeScanner();

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
    }

    /**
     * サーバ側端末の探索を開始します。<br>
     * サーバ側端末を検出すると{@link Callback#onDiscovered(BluetoothDevice)}が通知されます。
     * @throws IllegalStateException    既に検索を開始しているか、サーバ側端末との接続が
     *                                   確立している場合に投げられます。
     */
    public void startDiscovery() throws IllegalStateException {
        if (State.Stopped != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        mHandler.post(this::_startDiscovery);
    }

    /**
     * サーバ側端末の探索を終了します。<br>
     * 終了すると{@link Callback#onDiscoveryStopped()}が通知されます。
     */
    public void stopDiscovery() {
        if (State.Scanning != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        mHandler.post(this::_stopDiscovery);
    }

    /**
     * 引数で指定したサーバ側端末へ通信を接続します。<br>
     * 接続が完了すると{@link Callback#onConnected()}が通知されます。
     * @param device    接続するサーバ側端末の端末情報
     * @throws IllegalStateException    サーバ側端末の探索が開始していない場合に投げられます。
     * @throws IllegalArgumentException 引数で指定した端末が未検出の場合に投げられます。
     */
    public void connect(@NonNull BluetoothDevice device)
            throws IllegalStateException, IllegalArgumentException {
        if (State.Scanning != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        Mutex<IllegalArgumentException> mutex = new Mutex<>();
        mHandler.post(() -> _connect(device, mutex));
        IllegalArgumentException exception = mutex.lock();
        if (null != exception) {
            throw exception;
        }
    }

    /**
     * サーバ側端末にデータを送信します。<br>
     *
     * @param data  送信するデータ
     * @throws IllegalStateException    接続が確立していない段階で呼び出した場合に投げられます。
     */
    public void sendData(@NonNull byte[] data) throws IllegalStateException {
        if (State.Connected != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        mHandler.post(() -> _sendData(data));
    }

    /**
     * サーバ側端末との通信接続を切断します。<br>
     * 切断が完了すると{@link Callback#onDisconnected()}が通知されます。
     */
    public void disconnect() throws IllegalStateException {
        if (State.Connected != getState()) {
            throw new IllegalStateException("Bad state.");
        }
        mHandler.post(this::_disconnect);
    }

//    /**
//     * 自身の端末情報を返します。
//     * @return  自身の端末情報
//     */
//    @Nullable
//    public BluetoothDevice getMyDeviceInfo() {}

    private State getState() {
        State state;
//        if (Thread.currentThread() == mHandler.getLooper().getThread()) {
            state = mState;
//        } else {
//            Mutex<State> mutex = new Mutex<>();
//            mHandler.post(() -> mutex.unlock(mState));
//            state = mutex.lock();
//        }
        return state;
    }

    private void _startDiscovery() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(UUID.fromString(Constants.Uuids.ADVERTISE))).build());
        ScanSettings scanSettings = new ScanSettings.Builder().build();
        mScanner.startScan(scanFilters, scanSettings, mScanCallback);
        mState = State.Scanning;
    }

    private void _stopDiscovery() {
        mScanner.stopScan(mScanCallback);
        mState = State.Stopped;
        mCallback.onDiscoveryStopped();
    }

    private void _onScanResult(@NonNull BluetoothDevice device) {
        if ((State.Scanning != getState()) || (mScannedDevice.contains(device))) {
            return;
        }
        mScannedDevice.add(device);
        mCallback.onDiscovered(device);
    }

    private void _connect(
            @NonNull BluetoothDevice device, @NonNull Mutex<IllegalArgumentException> mutex) {
        if (!mScannedDevice.contains(device)) {
            mutex.unlock(new IllegalArgumentException("The device name \"" + device.getName() +
                            "\" has not been discovered."));
            return;
        }
        mutex.unlock(null);
        mScanner.stopScan(mScanCallback);
        device.connectGatt(mContext, false, mGattCallback);
    }

    private void onConnected(@NonNull BluetoothGatt gatt) {
        if (State.Scanning != getState()) {
            return;
        }
        mState = State.Connecting;
        mGatt = gatt;
        boolean result = gatt.discoverServices();
        if (!result) {
            // TODO
            Log.e(getClass().getSimpleName(), "Failed to discover services.");
        }
    }

    private void _onServiceDiscovered() {
        if (State.Connecting != getState()) {
            return;
        }
        Objects.requireNonNull(mGatt);
        BluetoothGattService service = mGatt.getService(UUID.fromString(Constants.Uuids.SERVICE));
        if (null == service) {
            // TODO
            return;
        }
        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(UUID.fromString(Constants.Uuids.CHARACTERISTIC));
        if (null == characteristic) {
            // TODO
            return;
        }
        mCharacteristic = characteristic;
        boolean setResult = mGatt.setCharacteristicNotification(mCharacteristic, true);
        if (!setResult) {
            // TODO
            return;
        }
        BluetoothGattDescriptor descriptor =
                mCharacteristic.getDescriptor(UUID.fromString(Constants.Uuids.CCCD));
        boolean writeResult =
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!writeResult) {
            // TODO
        }
        mGatt.writeDescriptor(descriptor);
    }

    private void _onDescriptorWrite() {
        mState = State.Connected;
        sendData(Constants.CONNECTED_SIGN.getBytes());
        mCallback.onConnected();
    }

    private void _sendData(@NonNull byte[] data) {
        mSendQueue.addAll(Constants.splitData(data, mBlockSize));
        if (isWriting) {
            // TODO ログ
            return;
        }
        isWriting = true;
        sendRequest();
    }

    private void sendRequest() {
        Objects.requireNonNull(mCharacteristic);
        Objects.requireNonNull(mGatt);
        boolean setResult = mCharacteristic.setValue(mSendQueue.peek());
        if (!setResult) {
            // TODO
        }
        boolean writeResult = mGatt.writeCharacteristic(mCharacteristic);
        if (!writeResult) {
            // TODO
        }
    }

    private void _onCharacteristicWrite() {
        if (State.Connected != getState()) {
            return;
        }
        mSendQueue.remove();
        if (mSendQueue.isEmpty()) {
            // TODO ログ
            isWriting = false;
            return;
        }
        sendRequest();
    }

    private void _onCharacteristicChanged(@NonNull byte[] data) {
        if (State.Connected != getState()) {
            return;
        }
        if (null == mLinker) {
            mLinker = new ReceivePacketLinker();
        }
        try {
            mLinker.link(data);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO
        }
        if (null != mLinker.getLinkedPacket()) {
            byte[] receivedData = mLinker.getLinkedPacket();
            mLinker = null;
            mCallback.onDataReceived(receivedData);
        }
    }

    private void _disconnect() {
        mState = State.Stopping;
        if (null != mGatt) {
            mGatt.disconnect();
            return;
        }
        mGatt = null;
    }

    private void onDisconnected() {
        Objects.requireNonNull(mGatt);
        mGatt.close();
        mGatt = null;
        mScannedDevice.clear();
        mSendQueue.clear();
        mLinker = null;
        mCharacteristic = null;
        if (State.Stopping == getState()) {
            mCallback.onDisconnected();
        } else {
            errorOccurred(new BleDataCommunicationException(
                    BleDataCommunicationException.ErrorCode.Disconnect));
        }
    }

    private void errorOccurred(@NonNull BleDataCommunicationException exception) {
        mException = exception;
        mCallback.onError(mException);
    }

    private static class CallbackExecutor implements Callback {
        @NonNull
        private final Callback mCallback;
        @NonNull
        private final Handler mHandler;

        CallbackExecutor(@NonNull Callback callback, @NonNull Handler handler) {
            mCallback = callback;
            mHandler = handler;
        }

        @Override
        public void onDiscovered(@NonNull BluetoothDevice device) {
            mHandler.post(() -> mCallback.onDiscovered(device));
        }

        @Override
        public void onDiscoveryStopped() {
            mHandler.post(mCallback::onDiscoveryStopped);
        }

        @Override
        public void onConnected() {
            mHandler.post(mCallback::onConnected);
        }

        @Override
        public void onDisconnected() {
            mHandler.post(mCallback::onDisconnected);
        }

        @Override
        public void onDataReceived(@NonNull byte[] data) {
            mHandler.post(() -> mCallback.onDataReceived(data));
        }

        @Override
        public void onError(@NonNull BleDataCommunicationException exception) {
            mHandler.post(() -> mCallback.onError(exception));
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
            if (!mIsReceiving) {
                mIsReceiving = true;
                byte[] dataLenByte = Arrays.copyOfRange(receivePacket, 0, DATA_OFFSET);
                mDataLen = ByteBuffer.wrap(dataLenByte).getInt();
                receivePacket = Arrays.copyOfRange(
                        receivePacket, DATA_OFFSET, receivePacket.length);
            }
            mStream.write(receivePacket);
            if (mDataLen == mStream.size()) {
                mLinkedPacket = mStream.toByteArray();
            }
        }

        @Nullable
        byte[] getLinkedPacket() {
            return mLinkedPacket;
        }
    }
}
