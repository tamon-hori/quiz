package com.hori_t.ble_communication_lib;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;

class Constants {
//    static class Uuids {
//        static final UUID ADVERTISE = UUID.fromString("b5998ea7-407c-4827-8a18-b446d9a763da");
//        static final UUID SERVICE = UUID.fromString("a5972f44-94f6-4369-be64-d595ba7c47a2");
//        static final UUID CHARACTERISTIC = UUID.fromString("ea6c6a8c-8a3d-4993-b7f8-4a68091bffe9");
//        static final UUID CCCD = UUID.fromString("0x2902");
//    }

    static class Uuids {
        static final String ADVERTISE = "b5998ea7-407c-4827-8a18-b446d9a763da";
        static final String  SERVICE = "a5972f44-94f6-4369-be64-d595ba7c47a2";
        static final String CHARACTERISTIC = "ea6c6a8c-8a3d-4993-b7f8-4a68091bffe9";
        static final String CCCD = "000002902-0000-1000-8000-00805f9b34fb";
    }

    final static int SIZE_OF_INT = 4;

    final static int SIZE_OF_PACKET_HEAD = 3;
    final static int DEFAULT_BLOCK_SIZE = 20;

    final static int NUM_OF_CONNECTIBLE_DEVICE = 3;
    final static String CONNECTED_SIGN = "012345";

    @NonNull
    static LinkedList<byte[]> splitData(@NonNull byte[] data, int blockSize) {
        LinkedList<byte[]> splitData = new LinkedList<>();
        int dataLen = data.length;
        ByteBuffer buffer = ByteBuffer.allocate(Constants.SIZE_OF_INT + dataLen);
        buffer.put(ByteBuffer.allocate(Constants.SIZE_OF_INT).putInt(dataLen).array());
        buffer.put(data);
        data = buffer.array();
        int writeData = 0;
        int cnt = 0;
        while (data.length > writeData) {
            byte[] block = (blockSize >= (data.length - writeData)) ?
                    Arrays.copyOfRange(data, blockSize * cnt, data.length) :
                    Arrays.copyOfRange(data, blockSize * cnt, blockSize * (cnt + 1));
            splitData.add(block);
            writeData += block.length;
            cnt++;
        }
        return splitData;
    }
}
