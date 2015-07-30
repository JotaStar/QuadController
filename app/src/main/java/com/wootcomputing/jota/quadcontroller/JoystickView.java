package com.wootcomputing.jota.quadcontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Jota on 7/2/2015.
 */
public class JoystickView extends View implements Runnable
{
    // Constants
    private final double RAD = 57.2957795;
    public final static long DEFAULT_LOOP_INTERVAL = 50; // 100 ms
    // Variables
    private Paint _mainCircle;
    private Paint _secondaryCircle;
    private Paint _button;

    private OnJoystickMoveListener _onJoystickMoveListener; // Listener
    private Thread thread = new Thread(this);
    private long loopInterval = DEFAULT_LOOP_INTERVAL;

    private int _xPosition = 0; // Touch x position
    private int _yPosition = 0; // Touch y position
    private int _centerX = 0; // Center view x position
    private int _centerY = 0; // Center view y position

    private float _diameter  = 0;
    private float _radius = 0;
    private float _joystickRadius;
    private int _buttonRadius;
    private int _lastAngle = 0;
    private int _lastPower = 0;

    private boolean _returnToCenterX = true;
    private boolean _returnToCenterY = true;
    private boolean _fixToCenterX = false;
    private boolean _fixToCenterY = false;

    public JoystickView(Context context)
    {
        super(context);
    }

    public JoystickView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs, int defaultStyle)
    {
        super(context, attrs, defaultStyle);
        initJoystickView();
    }

    public void setFixToCenterX(boolean value)
    {
        _fixToCenterX = value;

            _xPosition = _centerX;
            onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100, MotionEvent.ACTION_UP, _xPosition, _yPosition, 0));

    }

    public boolean getFixToCenterX()
    {
        return _fixToCenterX;
    }

    public void setFixToCenterY(boolean value)
    {
        _fixToCenterY = value;

            _yPosition = _centerY;
            onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100, MotionEvent.ACTION_UP, _xPosition, _yPosition, 0));

    }

    public boolean getFixToCenterY()
    {
        return _fixToCenterY;
    }

    public void setReturnToCenterX(boolean value, boolean move)
    {
        _returnToCenterX = value;
        if(move)
        {
            onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100, MotionEvent.ACTION_UP, 0, 0, 0));
        }
    }

    public boolean getReturnToCenterX()
    {
        return _returnToCenterX;
    }

    public void setReturnToCenterY(boolean value, boolean move)
    {
        _returnToCenterY = value;
        if(move)
        {
            onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis() + 100, MotionEvent.ACTION_UP, 0, 0, 0));
        }

    }

    public boolean getReturnToCenterY ()
    {
        return _returnToCenterY;
    }

    protected void initJoystickView()
    {
        _mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        _mainCircle.setColor(Color.GRAY);
        _mainCircle.setStyle(Paint.Style.FILL_AND_STROKE);

        _secondaryCircle = new Paint();
        _secondaryCircle.setColor(Color.BLACK);
        _secondaryCircle.setStyle(Paint.Style.STROKE);

        _button = new Paint(Paint.ANTI_ALIAS_FLAG);
        _button.setColor(Color.DKGRAY);
        _button.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onFinishInflate()
    {
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld)
    {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        // before measure, get the center of view
        _xPosition = (int) getWidth() / 2;
        _yPosition = (int) getWidth() / 2;

        _diameter = Math.min(xNew, yNew);
        _radius = _diameter / 2;

        _buttonRadius = (int) (_diameter / 2 * 0.40);
        _joystickRadius = (int) (_diameter / 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // setting the measured values to resize the view to a certain width and height
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));

        setMeasuredDimension(d, d);
    }

    private int measure(int measureSpec)
    {
        int result = 0;

        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED)
        {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        }
        else
        {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        // super.onDraw(canvas);
        _centerX = (getWidth()) / 2;
        _centerY = (getHeight()) / 2;

        // painting the main circle
        canvas.drawCircle((int) _centerX, (int) _centerY, _joystickRadius, _mainCircle);

        // painting the secondary circle
        canvas.drawCircle((int) _centerX, (int) _centerY, _joystickRadius / 4, _secondaryCircle);

        // painting the move _button
        canvas.drawCircle(_xPosition, _yPosition, _buttonRadius, _button);
    }

    private void valueChanged()
    {
        if (_onJoystickMoveListener != null)
        {
            int xValue = (int)  map(_xPosition, 0, (long) _diameter, 1024, -6);
            int yValue =  (int)  map(_yPosition, 0, (long) _diameter, 1024, -6);

            _onJoystickMoveListener.onValueChanged(xValue, yValue);
            //_onJoystickMoveListener.onValueChanged(_xPosition, _yPosition);
        }
    }

    private long map(long x, long in_min, long in_max, long out_min, long out_max)
    {
        return (long) ((x - in_min) * (out_max - out_min + ((short) 1)) / (in_max - in_min + ((short) 1)) + out_min);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(!_fixToCenterX) _xPosition = (int) event.getX();
        if(!_fixToCenterY) _yPosition = (int) event.getY();

        double diffX = (_xPosition - _centerX);
        double diffY = (_yPosition - _centerY);

        double radial = Math.sqrt(diffX * diffX + diffY * diffY);
        if (radial > _joystickRadius)
        {
            _xPosition = (int) ((diffX) / (radial) * _joystickRadius + _centerX);
            _yPosition = (int) ((diffY) / (radial) * _joystickRadius + _centerY);
        }

        //_xPosition = (int) Math.max(Math.min((_xPosition), _joystickRadius + _centerX), -_joystickRadius + _centerX);
        //_yPosition = (int) Math.max(Math.min((_yPosition), _joystickRadius + _centerY), -_joystickRadius + _centerY);


        invalidate();
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if(_returnToCenterX) _xPosition = _centerX;
            if(_returnToCenterY) _yPosition = _centerY;

            thread.interrupt();
            valueChanged();
        }
        if (_onJoystickMoveListener != null && event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if (thread != null && thread.isAlive())
            {
                thread.interrupt();
            }
            thread = new Thread(this);
            thread.start();
            valueChanged();
        }

        return true;
    }
    public int getAngle()
    {
        if (_xPosition > _centerX)
        {
            if (_yPosition < _centerY)
            {
                return _lastAngle = (int) (Math.atan((_yPosition - _centerY) / (_xPosition - _centerX)) * RAD + 90);
            }
            else if (_yPosition > _centerY)
            {
                return _lastAngle = (int) (Math.atan((_yPosition - _centerY) / (_xPosition - _centerX)) * RAD) + 90;
            }
            else
            {
                return _lastAngle = 90;
            }
        }
        else if (_xPosition < _centerX)
        {
            if (_yPosition < _centerY)
            {
                return _lastAngle = (int) (Math.atan((_yPosition - _centerY) / (_xPosition - _centerX)) * RAD - 90);
            }
            else if (_yPosition > _centerY)
            {
                return _lastAngle = (int) (Math.atan((_yPosition - _centerY) / (_xPosition - _centerX)) * RAD) - 90;
            }
            else
            {
                return _lastAngle = -90;
            }
        }
        else
        {
            if (_yPosition <= _centerY)
            {
                return _lastAngle = 0;
            }
            else
            {
                if (_lastAngle < 0)
                {
                    return _lastAngle = -180;
                }
                else
                {
                    return _lastAngle = 180;
                }
            }
        }
    }

    public int getPower()
    {
        return (int) (100 * Math.sqrt((_xPosition - _centerX) * (_xPosition - _centerX) + (_yPosition - _centerY) * (_yPosition - _centerY)) / _joystickRadius);
    }

    public int getYAxi()
    {
        return (int)_yPosition;
    }

    public int getXAxi()
    {
        return (int)_xPosition;
    }

    public int getDirection()
    {
        if (_lastPower == 0 && _lastAngle == 0)
        {
            return 0;
        }
        int a = 0;
        if (_lastAngle <= 0)
        {
            a = (_lastAngle * -1) + 90;
        }
        else if (_lastAngle > 0)
        {
            if (_lastAngle <= 90)
            {
                a = 90 - _lastAngle;
            }
            else
            {
                a = 360 - (_lastAngle - 90);
            }
        }

        int direction = (int) (((a + 22) / 45) + 1);

        if (direction > 8)
        {
            direction = 1;
        }
        return direction;
    }

    public void setOnJoystickMoveListener(OnJoystickMoveListener listener, long repeatInterval)
    {
        this._onJoystickMoveListener = listener;
        this.loopInterval = repeatInterval;
    }


    public static interface OnJoystickMoveListener
    {
        public void onValueChanged(int x, int y);
    }

    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            post(new Runnable()
            {
                public void run()
                {
                    valueChanged();
                }
            });
            try
            {
                Thread.sleep(loopInterval);
            }
            catch (InterruptedException e)
            {
                break;
            }
        }
    }
}