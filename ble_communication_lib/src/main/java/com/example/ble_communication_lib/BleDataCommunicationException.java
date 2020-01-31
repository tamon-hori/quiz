package com.example.ble_communication_lib;

import android.support.annotation.NonNull;

@SuppressWarnings({"unused", "WeakerAccess"})
public class BleDataCommunicationException extends Exception {
    public enum ErrorCode {
        IOException("IOException"),
        Disconnect("Unexpected disconnection")
        ;
        @NonNull
        private final String message;

        ErrorCode(@NonNull String message)  {
            this.message = message;
        }
    }
    @NonNull
    private final ErrorCode mErrorCode;

    BleDataCommunicationException(@NonNull ErrorCode errorCode) {
        this(errorCode, errorCode.message);
    }

    BleDataCommunicationException(@NonNull ErrorCode errorCode, @NonNull String message) {
        super(message);
        mErrorCode = errorCode;
    }

    @NonNull
    public ErrorCode getmErrorCode() {
        return mErrorCode;
    }

    @NonNull
    @Override
    public String toString() {
        return mErrorCode.name() + " : " + getMessage();
    }
}
