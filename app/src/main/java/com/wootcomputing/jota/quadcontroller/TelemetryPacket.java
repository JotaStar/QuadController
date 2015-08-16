package com.wootcomputing.jota.quadcontroller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by Jota on 7/28/2015.
 */
public class TelemetryPacket
{
    private static final int PACKET_DATA_SIZE = 80;
    private static final int PACKET_SIZE = 85;
    private TelemetryStruct _telemetryStruct;

    public byte header;
    public byte commandId;
    public byte dataLength;
    public byte[] data;
    public byte checkSum;
    public byte endFrame;

    public TelemetryPacket(byte[] bytes)
    {
        try
        {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            header = bb.get(0);
            commandId = bb.get(1);
            dataLength = bb.get(2);

            _telemetryStruct = new TelemetryStruct(Arrays.copyOfRange(bytes, 3, dataLength + 3));
            data = _telemetryStruct.getBytes();

            checkSum = bb.get(63);
            endFrame = bb.get(64);
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }

    public TelemetryPacket(byte commandId, TelemetryStruct telemetryStruct)
    {
        this.header = 36;
        this.commandId = commandId;
        this.data = telemetryStruct.getBytes();

        byte[] dataFilled = new byte[PACKET_DATA_SIZE];
        dataLength = (byte) data.length;
        System.arraycopy(data, 0, dataFilled, 0, dataLength);

        this.checkSum = TelemetryPacket.calculateCheckSum(dataFilled);
        this.endFrame = 13;
    }

    public TelemetryPacket(byte commandId, byte[] data)
    {
        this.header = 36;
        this.commandId = commandId;
        this.data = data;
        this.dataLength = (byte) data.length;

        this.checkSum = TelemetryPacket.calculateCheckSum(data);
        this.endFrame = 13;
    }

    public byte[] getBytes()
    {

        byte[] packet = new byte[dataLength + 5];

        //Copy Header
        System.arraycopy(new byte[]{header}, 0, packet, 0, 1);

        //Copy CommandId
        System.arraycopy(new byte[]{commandId}, 0, packet, 1, 1);

        //Copy DataLength
        System.arraycopy(new byte[]{dataLength}, 0, packet, 2, 1);

        //Copy Data
        System.arraycopy(data, 0, packet, 3, dataLength);

        //Copy CheckSum
        System.arraycopy(new byte[]{checkSum}, 0, packet, dataLength + 3, 1);

        //Copy EndOfFrame
        System.arraycopy(new byte[]{endFrame}, 0, packet, dataLength + 4, 1);

        return packet;
    }

    public TelemetryStruct getTelemetryStruct()
    {
        return _telemetryStruct;
    }

    public static byte calculateCheckSum(byte[] data)
    {
        byte checkSum = 0;

        for(byte i = 0; i < data.length; i++)
        {
            checkSum ^= data[i];
        }

        return checkSum;
    }
}


