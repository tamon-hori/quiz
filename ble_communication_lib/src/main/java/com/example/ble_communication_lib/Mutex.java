package com.example.ble_communication_lib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings({"unused", "WeakerAccess"})
class Mutex<T> {
    private final static long DEFAULT_TIMEOUT = 5 * 1000;
    private final long mTimeout;
    @NonNull
    private final CountDownLatch mLock;
    @Nullable
    private T mData;

    Mutex() {
        this(DEFAULT_TIMEOUT);
    }

    Mutex(long timeout) {
        mTimeout = timeout;
        mLock = new CountDownLatch(1);
    }

    T lock() {
        T ret = null;
        try {
            lock(mTimeout);
            ret = mData;
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void lock(long timeout) throws InterruptedException, TimeoutException {
        mLock.await(timeout, TimeUnit.MILLISECONDS);
        if (0 < mLock.getCount()) {
            throw new TimeoutException("Timeout CountDownLatch.await()");
        }
    }

    void unlock(@Nullable T data) {
        mData = data;
        mLock.countDown();
    }
}
