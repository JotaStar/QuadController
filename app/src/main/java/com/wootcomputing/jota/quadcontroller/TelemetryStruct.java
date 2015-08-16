package com.wootcomputing.jota.quadcontroller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by Jota on 7/28/2015.
 */

public class TelemetryStruct
{
    public MotorTelemetry channel;
    public MotorTelemetry velocity;
    public YprTelemetry ypr;
    public YprTelemetry constantP;
    public YprTelemetry constantI;
    public YprTelemetry constantD;
    public YprTelemetry computedPID;

    public TelemetryStruct(byte[] bytes)
    {
        if(bytes.length >= 8)
        {
            channel = new MotorTelemetry(Arrays.copyOfRange(bytes, 0, 8));
        }

        if (bytes.length >= 16)
        {
            velocity = new MotorTelemetry(Arrays.copyOfRange(bytes, 8, 16));
        }

        if (bytes.length >= 28)
        {
            ypr = new YprTelemetry(Arrays.copyOfRange(bytes, 16, 28));
        }

        if (bytes.length >= 40)
        {
            constantP = new YprTelemetry(Arrays.copyOfRange(bytes, 28, 40));
        }

        if (bytes.length >= 52)
        {
            constantI = new YprTelemetry(Arrays.copyOfRange(bytes, 40, 52));
        }

        if (bytes.length >= 64)
        {
            constantD = new YprTelemetry(Arrays.copyOfRange(bytes, 52, 64));
        }

        if (bytes.length >= 76)
        {
            computedPID = new YprTelemetry(Arrays.copyOfRange(bytes, 64, 76));
        }
    }

    public class YprTelemetry
    {
        public float ypr[] = new float[3];

        public YprTelemetry(byte[] bytes)
        {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            ypr[0] = bb.getFloat();
            ypr[1] = bb.getFloat();
            ypr[2] = bb.getFloat();
        }
    }

    public class MotorTelemetry
    {
        public short values[] = new short[4];

        public MotorTelemetry(byte[] bytes)
        {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            values[0] = bb.getShort();
            values[1] = bb.getShort();
            values[2] = bb.getShort();
            values[3] = bb.getShort();
        }
    }

    public byte[] getBytes()
    {
        byte bytes[] = new byte[52];
        ByteUtil byteUtil = new ByteUtil();

        if (channel != null)
        {
            System.arraycopy(byteUtil.shortToBytes(channel.values[0]), 0, bytes, 0, 2);
            System.arraycopy(byteUtil.shortToBytes(channel.values[1]), 0, bytes, 2, 2);
            System.arraycopy(byteUtil.shortToBytes(channel.values[2]), 0, bytes, 4, 2);
            System.arraycopy(byteUtil.shortToBytes(channel.values[3]), 0, bytes, 6, 2);
        }
        if (velocity != null)
        {
            System.arraycopy(byteUtil.shortToBytes(velocity.values[0]), 0, bytes, 8, 2);
            System.arraycopy(byteUtil.shortToBytes(velocity.values[1]), 0, bytes, 10, 2);
            System.arraycopy(byteUtil.shortToBytes(velocity.values[2]), 0, bytes, 12, 2);
            System.arraycopy(byteUtil.shortToBytes(velocity.values[3]), 0, bytes, 14, 2);
        }

        if (ypr != null)
        {
            System.arraycopy(byteUtil.floatToBytes(ypr.ypr[0]), 0, bytes, 16, 4);
            System.arraycopy(byteUtil.floatToBytes(ypr.ypr[1]), 0, bytes, 20, 4);
            System.arraycopy(byteUtil.floatToBytes(ypr.ypr[2]), 0, bytes, 24, 4);
        }

        if (constantP != null)
        {
            System.arraycopy(byteUtil.floatToBytes(constantP.ypr[0]), 0, bytes, 28, 4);
            System.arraycopy(byteUtil.floatToBytes(constantP.ypr[1]), 0, bytes, 32, 4);
            System.arraycopy(byteUtil.floatToBytes(constantP.ypr[2]), 0, bytes, 36, 4);
        }

        if (computedPID != null)
        {
            System.arraycopy(byteUtil.floatToBytes(computedPID.ypr[0]), 0, bytes, 40, 4);
            System.arraycopy(byteUtil.floatToBytes(computedPID.ypr[1]), 0, bytes, 44, 4);
            System.arraycopy(byteUtil.floatToBytes(computedPID.ypr[2]), 0, bytes, 48, 4);
        }

        return bytes;
    }
}
