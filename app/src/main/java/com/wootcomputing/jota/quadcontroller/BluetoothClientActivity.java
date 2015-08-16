package com.wootcomputing.jota.quadcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Jota on 6/30/2015.
 */

public class BluetoothClientActivity extends Activity
{

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int PACKET_SIZE = 85;

    //Protocol
    private static byte nextByte = 0;
    private static byte buff[] = new byte[PACKET_SIZE];

    // Member fields
    private final BluetoothAdapter _adapter;
    private final android.os.Handler _handler;
    private ConnectThread _connectThread;
    private ConnectedThread _connectedThread;
    private int _state;

    // Constants that indicate the current connection _state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * - context - The UI Activity Context
     * - handler - A Handler to send messages back to the UI Activity
     */

    public BluetoothClientActivity(Context context, android.os.Handler handler)
    {
        _adapter = BluetoothAdapter.getDefaultAdapter();
        _state = STATE_NONE;
        _handler = handler;
    }

    /**
     * Set the current _state o
     */
    private synchronized void setState(int state)
    {
        _state = state;

        // Give the new _state to the Handler so the UI Activity can update
        _handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection _state.
     */
    public synchronized int getState()
    {
        return _state;
    }

    /**
     * Start the Rfcomm client service.
     */
    public synchronized void start()
    {
        // Cancel any thread attempting to make a connection
        if (_connectThread != null)
        {
            _connectThread.cancel();
            _connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (_connectedThread != null)
        {
            _connectedThread.cancel();
            _connectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * - device - The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device)
    {
        // Cancel any thread attempting to make a connection
        if (_state == STATE_CONNECTING || _state == STATE_CONNECTED)
        {
            if (_connectThread != null)
            {
                _connectThread.cancel();
                _connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (_connectedThread != null)
        {
            _connectedThread.cancel();
            _connectedThread = null;
        }

        // Start the thread to connect with the given device
        _connectThread = new ConnectThread(device);
        _connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * - socket - The BluetoothSocket on which the connection was made
     * - device - The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
        // Cancel the thread that completed the connection
        if (_connectThread != null)
        {
            _connectThread.cancel();
            _connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (_connectedThread != null)
        {
            _connectedThread.cancel();
            _connectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        _connectedThread = new ConnectedThread(socket);
        _connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = _handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        _handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop()
    {
        if (_connectThread != null)
        {
            _connectThread.cancel();
            _connectThread = null;
        }
        if (_connectedThread != null)
        {
            _connectedThread.cancel();
            _connectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * - out - The bytes to write - ConnectedThread#write(byte[])
     */
    public void write(byte[] out)
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this)
        {
            if (_state != STATE_CONNECTED)
            {
                return;
            }
            r = _connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);

    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed()
    {
        setState(STATE_NONE);
        // Send a failure message back to the Activity
        Message msg = _handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        _handler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost()
    {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = _handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        _handler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private final BluetoothSocket _socket;
        private final BluetoothDevice _device;

        public ConnectThread(BluetoothDevice device)
        {
            BluetoothSocket socketTemp = null;
            _device = device;


            try
            {
                socketTemp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e)
            {
                //
            }
            _socket = socketTemp;

        }

        public void run()
        {
            //Cancel discovery because it will slow down a connection
            _adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a  successful connection or an exception
                _socket.connect();
            }
            catch (IOException e)
            {
                connectionFailed();
                // Close the socket
                try
                {
                    _socket.close();
                }
                catch (IOException e2)
                {
                    //
                }
                // Start the service over to restart listening mode
                BluetoothClientActivity.this.start();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothClientActivity.this)
            {
                _connectThread = null;
            }
            // Start the connected thread
            connected(_socket, _device);
        }

        public void cancel()
        {
            try
            {
                _socket.close();
            }
            catch (IOException e)
            {
                //
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket _socket;
        private final InputStream _inStream;
        private final OutputStream _outStream;

        public ConnectedThread(BluetoothSocket socket)
        {

            _socket = socket;
            InputStream inputStreamTemp = null;
            OutputStream outputStreamTemp = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                inputStreamTemp = socket.getInputStream();
                outputStreamTemp = socket.getOutputStream();
            }
            catch (IOException ex)
            {

            }

            _inStream = inputStreamTemp;
            _outStream = outputStreamTemp;
        }

        public void run()
        {

            byte[] buffer = new byte[1];
            byte byteRecv = 0;
            int bytes;

            // Keep listening to the InputStream while connected
            while (true)
            {
                try
                {
                    // Read from the InputStream
                    bytes = _inStream.read(buffer);

                    if(bytes != 1) continue;

                    byteRecv = buffer[0];
                    buff[nextByte] = byteRecv;

                    if (byteRecv == 13)
                    {
                        if (buff[0] == 36) //Header = $ -> 36
                        {

                            byte dataFrame[] = Arrays.copyOfRange(buff, 3, buff[2] + 3);
                            byte checkSum = TelemetryPacket.calculateCheckSum(dataFrame);

                            if(buff[nextByte - 1] == checkSum)
                            {
                                TelemetryPacket newPacket = new TelemetryPacket(buff);
                                processMessage(newPacket);
                            }
                        }

                        Arrays.fill(buff, (byte) 0);
                        nextByte = 0;

                    }
                    else if (nextByte + 1 >= (PACKET_SIZE))
                    {
                        Arrays.fill(buff, (byte) 0);
                        nextByte = 0;
                    }
                    else
                    {
                        nextByte++;
                    }
                }
                catch (IOException e)
                {
                    connectionLost();
                    break;
                }
            }
        }

        private void processMessage(TelemetryPacket telemetryPacket)
        {
            _handler.obtainMessage(MainActivity.MESSAGE_READ, telemetryPacket).sendToTarget();
        }

        public void write(byte[] buffer)
        {
            try
            {
                _outStream.write(buffer);

                // Share the sent message back to the UI Activity
                // _handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }
            catch (IOException e)
            {
                //
            }
        }

        public void cancel()
        {
            try
            {
                _socket.close();
            }
            catch (IOException e)
            {
                //
            }
        }
    }



}