package com.wootcomputing.jota.quadcontroller;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.Arrays;


public class MainActivity extends Activity
{
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;

    // Message types sent from the BluetoothClientActivity Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothClientActivity Handler
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    public static final int PACKET_DATA_SIZE = 60;
    public static final int PACKET_SIZE = 65;

    //Joysticks
    private JoystickView _joystickLeft;
    private JoystickView _joystickRight;

    //Controls
    private TextView _statusTextView;
    private Button _motorFLButton;
    private Button _motorFRButton;
    private Button _motorBLButton;
    private Button _motorBRButton;

    private Button _yawButton;
    private Button _pitchButton;
    private Button _rollButton;

//    private Button _rcThroButton;
//    private Button _rcYawButton;
//    private Button _rcPitchButton;
//    private Button _rcRollButton;

    private TextView _pidYawTextView;
    private TextView _pidPitchTextView;
    private TextView _pidRollTextView;

    private CheckBox _joystickRightFixXCheckBox;
    private CheckBox _joystickRightFixYCheckBox;
    private CheckBox _joystickLeftFixXCheckBox;
    private CheckBox _joystickLeftFixYCheckBox;

    private  CheckBox joystickLeftFixToCenterXCheckBox;


    private Button _pidYawMoreButton;
    private Button _pidYawLessButton;

    private Button _pidPitchMoreButton;
    private Button _pidPitchLessButton;

    private Button _pidRollMoreButton;
    private Button _pidRollLessButton;


    //RC YPR VALUES
    private short _rcThroValue;
    private short _rcYawValue;
    private short _rcPitchValue;
    private short _rcRollValue;


    //Bluetooth
    private static String _readMessage;
    private static String _deviceName;
    private BluetoothClientActivity _btClientActivity;
    private BluetoothAdapter _btAdapter = null;
    private Thread _threadSend;

    private final Handler _handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MESSAGE_DEVICE_NAME:
                {
                    // save the connected device's name
                    _deviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " + _deviceName, Toast.LENGTH_SHORT).show();
                    break;
                }
                case MESSAGE_TOAST:
                {
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                }
                case MESSAGE_STATE_CHANGE:
                {
                    switch (msg.arg1)
                    {
                        case BluetoothClientActivity.STATE_CONNECTED:
                        {
                            _statusTextView.setText("Connected to: ");
                            _statusTextView.append(" " + _deviceName);

                            _threadSend = new Thread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    while (true)
                                    {
                                        sendMessageRX(_rcThroValue, _rcYawValue, _rcPitchValue, _rcRollValue);
                                        try
                                        {
                                            Thread.sleep(100);
                                        }
                                        catch (Exception ex)
                                        {
                                            //TODO
                                        }
                                    }
                                }
                            });

                            _threadSend.start();


                            break;
                        }
                        case BluetoothClientActivity.STATE_CONNECTING:
                        {
                            _statusTextView.setText("Connecting...");
                            break;
                        }
                        case BluetoothClientActivity.STATE_NONE:
                        {
                            _statusTextView.setText("Not connected.");

                            if (_threadSend != null)
                            {
                                _threadSend.interrupt();
                                _threadSend = null;
                            }

                            break;
                        }
                    }
                    break;
                }
                case MESSAGE_READ:
                {
                    try
                    {
                        //$[1](36)<CommandId[1]><dataLenght[1]><data[40]><checkSum[1]><endFrame[1](13)>


                        TelemetryPacket newPacket = (TelemetryPacket) msg.obj;
                        TelemetryStruct telemetryStruct = newPacket.getTelemetryStruct();

                        _yawButton.setText(String.format("%.2f", telemetryStruct.ypr.ypr[0]));
                        _pitchButton.setText(String.format("%.2f", telemetryStruct.ypr.ypr[1]));
                        _rollButton.setText(String.format("%.2f", telemetryStruct.ypr.ypr[2]));

                        _motorFLButton.setText(String.format("%d", telemetryStruct.channel.values[0]) + "\r\n" + String.format("%d", telemetryStruct.velocity.values[0]));
                        _motorFRButton.setText(String.format("%d", telemetryStruct.channel.values[1]) + "\r\n" + String.format("%d", telemetryStruct.velocity.values[1]));
                        _motorBLButton.setText(String.format("%d", telemetryStruct.channel.values[2]) + "\r\n" + String.format("%d", telemetryStruct.velocity.values[2]));
                        _motorBRButton.setText(String.format("%d", telemetryStruct.channel.values[3]) + "\r\n" + String.format("%d", telemetryStruct.velocity.values[3]));

                        //                        _rcThroButton.setText(items[9]);
                        //                        _rcYawButton.setText(items[10]);
                        //                        _rcPitchButton.setText(items[11]);
                        //                        _rcRollButton.setText(items[12]);

                        _pidYawTextView.setText(String.format("%.4f", telemetryStruct.constantPID.ypr[0]));
                        _pidPitchTextView.setText(String.format("%.4f", telemetryStruct.constantPID.ypr[1]));
                        _pidRollTextView.setText(String.format("%.4f", telemetryStruct.constantPID.ypr[2]));
                    }
                    catch (Exception ex)
                    {
                        _statusTextView.setText("Exception: MESSAGE_READ");
                    }
                }
            }
        }
    };

    private float roundFloat(float f, int decimal)
    {
        return BigDecimal.valueOf(f).setScale(decimal,BigDecimal.ROUND_HALF_UP).floatValue();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bluetooth
        _btClientActivity = new BluetoothClientActivity(this, _handler);
        _btAdapter = BluetoothAdapter.getDefaultAdapter();

        //Controls
        _statusTextView = (TextView) findViewById(R.id.statusTextView);
        _yawButton = (Button) findViewById(R.id.yawButton);
        _pitchButton = (Button) findViewById(R.id.pitchButton);
        _rollButton = (Button) findViewById(R.id.rollButton);

        _joystickLeftFixXCheckBox = (CheckBox) findViewById(R.id.joystickLeftFixXCheckBox);
        _joystickLeftFixYCheckBox = (CheckBox) findViewById(R.id.joystickLeftFixYCheckBox);
        _joystickRightFixXCheckBox = (CheckBox) findViewById(R.id.joystickRightFixXCheckBox);
        _joystickRightFixYCheckBox = (CheckBox) findViewById(R.id.joystickRightFixYCheckBox);

        joystickLeftFixToCenterXCheckBox = (CheckBox) findViewById(R.id.joystickLeftFixCenterXCheckBox);
        joystickLeftFixToCenterXCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _joystickLeft.setFixToCenterX(((CheckBox) v).isChecked());
            }
        });

        _joystickLeftFixXCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _joystickLeft.setReturnToCenterX(((CheckBox) v).isChecked(), _joystickLeft.getReturnToCenterX() && ((CheckBox) v).isChecked());
            }
        });

        _joystickLeftFixYCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _joystickLeft.setReturnToCenterY(((CheckBox) v).isChecked(), _joystickLeft.getReturnToCenterY() && ((CheckBox) v).isChecked());
            }
        });

        _joystickRightFixXCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _joystickRight.setReturnToCenterX(((CheckBox) v).isChecked(), _joystickRight.getReturnToCenterX() && ((CheckBox) v).isChecked());
            }
        });

        _joystickRightFixXCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _joystickRight.setReturnToCenterX(((CheckBox) v).isChecked(), _joystickRight.getReturnToCenterX() && ((CheckBox) v).isChecked());
            }
        });


        _joystickRightFixYCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _joystickRight.setReturnToCenterY(((CheckBox) v).isChecked(), _joystickRight.getReturnToCenterY() && ((CheckBox) v).isChecked());
            }
        });

        _motorFLButton = (Button) findViewById(R.id.motorFLButton);
        _motorFRButton = (Button) findViewById(R.id.motorFRButton);
        _motorBLButton = (Button) findViewById(R.id.motorBLButton);
        _motorBRButton = (Button) findViewById(R.id.motorBRButton);

//        _rcThroButton = (Button) findViewById(R.id.rcThroButton);
//        _rcYawButton = (Button) findViewById(R.id.rcYawButton);
//        _rcPitchButton = (Button) findViewById(R.id.rcPitchButton);
//        _rcRollButton = (Button) findViewById(R.id.rcRollButton);

        _pidYawMoreButton = (Button) findViewById(R.id.pidYawMoreButton);
        _pidYawLessButton = (Button) findViewById(R.id.pidYawLessButton);
        _pidYawMoreButton.setLongClickable(true);
        _pidYawLessButton.setLongClickable(true);

        _pidYawLessButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessageRX((byte) 0, (byte) 0);
            }
        });

        _pidYawMoreButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessageRX((byte) 0, (byte) 1);
            }
        });

        _pidYawLessButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                sendMessageRX((byte) 0, (byte) 2);
                return true;
            }
        });

        _pidYawMoreButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                sendMessageRX((byte) 0, (byte) 3);
                return true;
            }
        });

        _pidPitchMoreButton = (Button) findViewById(R.id.pidPitchMoreButton);
        _pidPitchLessButton = (Button) findViewById(R.id.pidPitchLessButton);
        _pidPitchMoreButton.setLongClickable(true);
        _pidPitchLessButton.setLongClickable(true);

        _pidPitchLessButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessageRX((byte) 1, (byte) 0);
            }
        });

        _pidPitchMoreButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessageRX((byte) 1, (byte) 1);
            }
        });

        _pidPitchLessButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                sendMessageRX((byte) 1, (byte) 2);
                return true;
            }
        });

        _pidPitchMoreButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                sendMessageRX((byte) 1, (byte) 3);
                return true;
            }
        });

        _pidRollMoreButton = (Button) findViewById(R.id.pidRollMoreButton);
        _pidRollLessButton = (Button) findViewById(R.id.pidRollLessButton);
        _pidRollMoreButton.setLongClickable(true);
        _pidRollLessButton.setLongClickable(true);

        _pidRollLessButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessageRX((byte) 2, (byte) 0);
            }
        });

        _pidRollMoreButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendMessageRX((byte) 2, (byte) 1);
            }
        });

        _pidRollLessButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                sendMessageRX((byte) 2, (byte) 2);
                return true;
            }
        });
        _pidRollMoreButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                sendMessageRX((byte) 2, (byte) 3);
                return true;
            }
        });

        _pidYawTextView = (TextView) findViewById(R.id.pidYawTextView);
        _pidPitchTextView = (TextView) findViewById(R.id.pidPitchTextView);
        _pidRollTextView = (TextView) findViewById(R.id.pidRollTextView);

        _joystickLeft = (JoystickView) findViewById(R.id.joystickLeft);
        _joystickLeft.setReturnToCenterX(false, true);
        _joystickLeft.setReturnToCenterY(false, true);
        _joystickLeftFixXCheckBox.setChecked(false);
        _joystickLeftFixYCheckBox.setChecked(false);

        _joystickLeft.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener()
        {
            @Override
            public void onValueChanged(int x, int y)
            {
                //Values are between 0 - 1024
                _rcYawValue = (short) x;
                _rcThroValue = (short) y;

                String s = Integer.toString(_rcYawValue) + " - " + Integer.toString(_rcThroValue);

                _statusTextView.setText(s);
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

        _joystickRight = (JoystickView) findViewById(R.id.joystickRight);
        _joystickRight.setReturnToCenterX(true, true);
        _joystickRight.setReturnToCenterY(true, true);
        _joystickRight.setFixToCenterY(false);
        _joystickRight.setFixToCenterX(false);

        _joystickRightFixXCheckBox.setChecked(true);
        _joystickRightFixYCheckBox.setChecked(true);

        _joystickRight.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener()
        {
            @Override
            public void onValueChanged(int x, int y)
            {
                //Values are between 0 - 1024
                _rcRollValue = (short) x;
                _rcPitchValue = (short) y;
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);


        //sendMessageRX((short)900,(short)1500,(short)1500,(short)1500);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menuConnect)
        {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_ENABLE_BT:
            {
                if (_btAdapter.isEnabled())
                {
                    Toast.makeText(getApplicationContext(), "BT Enabled", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_CONNECT_DEVICE:
            {
                if(resultCode== Activity.RESULT_OK)
                {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                    BluetoothDevice device = _btAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    _btClientActivity.connect(device);

                }
            }
        }
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();

        if (_btClientActivity != null)
        {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (_btClientActivity.getState() == BluetoothClientActivity.STATE_NONE)
            {
                // Start the Bluetooth
                _btClientActivity.start();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        // Stop the Bluetooth
        if (_btClientActivity != null)
        {
            _btClientActivity.stop();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        new AlertDialog.Builder(this).setTitle("QuadController").setMessage("Close?").setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
            }
        }).setNegativeButton("No", null).show();
    }

    private void sendMessageRX(short thro, short yaw, short pitch, short roll)
    {
        ByteUtil byteUtil = new ByteUtil();
        byte throValue[] = byteUtil.shortToBytes(thro).clone();
        byte yawValue[] = byteUtil.shortToBytes(yaw).clone();
        byte pitchValue[] = byteUtil.shortToBytes(pitch).clone();
        byte rollValue[] = byteUtil.shortToBytes(roll).clone();

        sendMessageRX((byte) 1, new byte[]{throValue[0], throValue[1], yawValue[0], yawValue[1], pitchValue[0], pitchValue[1], rollValue[0], rollValue[1]});
    }

    private void sendMessageRX(byte ypr, byte moreOrLess)
    {
        sendMessageRX((byte) 2, new byte[]{ ypr, moreOrLess });
    }

    private void sendMessageRX(byte commandId, byte data[])
    {
        // Check that we're actually connected before trying anything
        if (_btClientActivity.getState() != BluetoothClientActivity.STATE_CONNECTED)
        {
            return;
        }

        // Check that there's actually something to send
        if ((data.length) > 0)
        {
            TelemetryPacket newPacket = new TelemetryPacket(commandId, data);

            _btClientActivity.write(newPacket.getBytes());
        }
    }
}
