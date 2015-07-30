package com.wootcomputing.jota.quadcontroller;

/**
 * Created by Jota on 7/28/2015.
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtil
{
    private ByteBuffer bufferLong = ByteBuffer.allocate(12);
    private ByteBuffer bufferShort = ByteBuffer.allocate(2);
    private ByteBuffer bufferFloat = ByteBuffer.allocate(4);

    public ByteUtil()
    {
        bufferLong.order(ByteOrder.LITTLE_ENDIAN);
        bufferShort.order(ByteOrder.LITTLE_ENDIAN);
        bufferFloat.order(ByteOrder.LITTLE_ENDIAN);
    }

    public byte[] longToBytes(long x)
    {
        bufferLong.putLong(0, x);
        return bufferLong.array();
    }

    public long bytesToLong(byte[] bytes)
    {
        bufferLong.put(bytes, 0, bytes.length);
        bufferLong.flip();
        return bufferLong.getLong();
    }

    public byte[] shortToBytes(short x)
    {
        bufferShort.putShort(0, x);
        return bufferShort.array();
    }

    public long bytesToShort(byte[] bytes)
    {
        bufferShort.put(bytes, 0, bytes.length);
        bufferShort.flip();
        return bufferShort.getShort();
    }

    public byte[] floatToBytes(float x)
    {
        bufferFloat.putFloat(0, x);
        return bufferFloat.array();
    }

    public float bytesToFloat(byte[] bytes)
    {
        bufferFloat.put(bytes, 0, bytes.length);
        bufferFloat.flip();
        return bufferFloat.getFloat();
    }

    private byte[] shortToByteArray(short value)
    {
        return new byte[]{(byte) (value & 0xff), (byte) ((value >> 8) & 0xff)};
    }
}