package com.tencent.yoloNcnn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class CanvasWindow extends Service implements SurfaceHolder.Callback{

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    int 屏幕高;
    int 屏幕宽;
    public static CanvasWindow DrawOc;
    public static WindowManager windowManager;
    public static LinearLayout mFloatLayout;
    public static WindowManager.LayoutParams wmParams;
    public static boolean runGameThread = false;

    @SuppressLint({"RtlHardcoded", "WrongConstant"})
    @Override
    public void onCreate() {
        super.onCreate();
        DrawOc = this;
        WindowManager windowManage = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metric = new DisplayMetrics();
        windowManage.getDefaultDisplay().getRealMetrics(metric);
        if (metric.widthPixels > metric.heightPixels) {
            屏幕宽 = metric.widthPixels;
            屏幕高 = metric.heightPixels;
        } else {
            屏幕高 = metric.widthPixels;
            屏幕宽 = metric.heightPixels;
        }
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.draw, null);
        wmParams = new WindowManager.LayoutParams();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (Integer.parseInt(Build.VERSION.SDK) >= 26) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = 屏幕宽;
        wmParams.height = -1;
        wmParams.alpha = 0.8f;
        windowManager.addView(mFloatLayout, wmParams);
    }
    private Notification createNotification() {
        String channelId = "绘图任务";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "识图",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("自动识别中")
                .setContentText("服务正在运行")
                .setSmallIcon(R.mipmap.widget_app_logo) // 确保该图标有效
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // 设置为持续显示
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        if (intent != null) {
            MainActivity.mediaProjection = MainActivity.mediaProjectionManager.getMediaProjection(
                    Activity.RESULT_OK,
                    (Intent) intent.getParcelableExtra("data")
            );
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }
}
