package com.tencent.yoloNcnn;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.lang.reflect.Method;

public class SingleTouchSimulator {

    private InputManager inputManager;
    private Method injectInputEventMethod;
    private long downTime;

    public SingleTouchSimulator(Context context) {
        try {
            inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            injectInputEventMethod = InputManager.class.getMethod("injectInputEvent", MotionEvent.class, int.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 模拟触摸点按下
    public void touchDown(int pointerId, float x, float y) {
        try {
            downTime = SystemClock.uptimeMillis();
            long eventTime = downTime;

            MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
            properties.id = pointerId;
            properties.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.x = x;
            coords.y = y;
            coords.pressure = 1;
            coords.size = 1;

            MotionEvent downEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_DOWN,
                    1,
                    new MotionEvent.PointerProperties[]{properties},
                    new MotionEvent.PointerCoords[]{coords},
                    0,
                    0,
                    1.0f,
                    1.0f,
                    0,
                    0,
                    InputDevice.SOURCE_TOUCHSCREEN,
                    0
            );
            injectInputEventMethod.invoke(inputManager, downEvent, 0);

            downEvent.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 模拟触摸点抬起
    public void touchUp(int pointerId) {
        try {
            long eventTime = SystemClock.uptimeMillis();

            MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
            properties.id = pointerId;
            properties.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.pressure = 1;
            coords.size = 1;

            MotionEvent upEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_UP,
                    1,
                    new MotionEvent.PointerProperties[]{properties},
                    new MotionEvent.PointerCoords[]{coords},
                    0,
                    0,
                    1.0f,
                    1.0f,
                    0,
                    0,
                    InputDevice.SOURCE_TOUCHSCREEN,
                    0
            );
            injectInputEventMethod.invoke(inputManager, upEvent, 0);

            upEvent.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
