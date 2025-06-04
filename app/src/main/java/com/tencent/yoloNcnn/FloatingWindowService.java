package com.tencent.yoloNcnn;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class FloatingWindowService extends Service implements View.OnTouchListener {

    private static WindowManager windowManager;
    private static WindowManager.LayoutParams layoutParams;
    private static View floatingIcon;
    private static Button 识别;
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private long touchStartTime;
    private boolean move = false;
    private static final int TOUCH_SLOP = 150; // 滑动阈值单位像素
    private static final int TOUCH_TIME = 200; // 手指长按的阈值
    private Handler mHandler = new Handler();
    private Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            move = true;
            setFloatViewSize(120, 120);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!WindowBean.floatWindwosStatus) {
            super.onStartCommand(intent, flags, startId);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            floatingIcon = LayoutInflater.from(this).inflate(R.layout.floating_icon, null);
            floatingIcon.setOnTouchListener(this);
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = WindowBean.floatx;
            layoutParams.y = WindowBean.floaty;
            setFloatingIcon();
            windowManager.addView(floatingIcon, layoutParams);
            WindowBean.floatWindwosStatus = true;
            if (!AiUtil.开启大模型) {
                startService(new Intent(MainActivity.context, MenuWindowService.class));
                hide();
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = motionEvent.getRawX();
                initialTouchY = motionEvent.getRawY();
                touchStartTime = System.currentTimeMillis();
                mHandler.postDelayed(mLongPressRunnable, TOUCH_TIME);
                break;
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mLongPressRunnable);
                move = false;
                restoreFloatingView();
                break;
            case MotionEvent.ACTION_UP:
                if (!move) {
                    openMenu();
                    setFloatViewSize(80, 80);
                }
                mHandler.removeCallbacks(mLongPressRunnable);
                move = false;
                restoreFloatingView();
                break;
            case MotionEvent.ACTION_MOVE:
                if (move) {
                    float dx = motionEvent.getRawX() - initialTouchX;
                    float dy = motionEvent.getRawY() - initialTouchY;
                    layoutParams.x = (int) (initialX + dx);
                    layoutParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(floatingIcon, layoutParams);
                } else {
                    float distanceX = Math.abs(motionEvent.getRawX() - initialX);
                    if (distanceX > TOUCH_SLOP) {
                        openMenu();
                    }
                }
                break;
        }
        return true;
    }

    public static void setFloatingIcon() {
        layoutParams.width = 80;
        layoutParams.height = 80;
        floatingIcon.setBackgroundResource(AiUtil.开启大模型?R.drawable.ai_icon:R.drawable.floating_icon);
        if (WindowBean.floatWindwosStatus) {
            windowManager.updateViewLayout(floatingIcon, layoutParams);
        }
    }

    public static void show() {
        floatingIcon.setVisibility(View.VISIBLE);
    }

    public static void hide() {
        floatingIcon.setVisibility(View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void openMenu() {
        if (AiUtil.开启大模型) {
            if(AiUtil.推理状态){
                Toast.makeText(MainActivity.context, "上一次推理还没完成", Toast.LENGTH_LONG).show();
                return;
            }
            if (MainActivity.imageReader != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AiUtil.推理结果 = "获取内容中...";
                        Image image = MainActivity.imageReader.acquireLatestImage();
                        if (null != image) {
                            AiUtil.推理(ImageUtil.imageToBase64(image));
                        } else {
                            Toast.makeText(MainActivity.context, "图像获取失败", Toast.LENGTH_LONG).show();
                        }
                        if (image != null) image.close();
                    }
                }).start();
            } else {
                Toast.makeText(MainActivity.context, "imageReader获取失败", Toast.LENGTH_LONG).show();
            }
        } else {
            if (!WindowBean.menuWindwosStatus) {
                startService(new Intent(MainActivity.context, MenuWindowService.class));
            } else {
                MenuWindowService.show();
            }
        }
    }

    public static void setFloatViewSize(int width, int height) {
        layoutParams.width = width == 0 ? 20 : width;
        layoutParams.height = height == 0 ? 150 : height;
        windowManager.updateViewLayout(floatingIcon, layoutParams);
    }

    public static void restoreFloatingView() {
        if (floatingIcon == null || 1 == WindowBean.restoreFloating)
            return;
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                setFloatViewSize(80, 80);
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) floatingIcon.getLayoutParams();
                layoutParams.x = WindowBean.screenWidth - floatingIcon.getWidth();
                layoutParams.y = Math.min(layoutParams.y, WindowBean.screenHeight - floatingIcon.getHeight());
                windowManager.updateViewLayout(floatingIcon, layoutParams);
                WindowBean.floatx = layoutParams.x;
                WindowBean.floaty = layoutParams.y;
            }
        });
    }

}

