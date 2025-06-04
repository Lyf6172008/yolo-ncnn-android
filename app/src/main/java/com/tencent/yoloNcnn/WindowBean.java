package com.tencent.yoloNcnn;


import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.RequiresApi;

public class WindowBean {
    private static final String TAG = "YOLO-V5";
    public static String screenFilePath = "/sdcard/screen";
    //图标的xy坐标
    public static int floatx = 0, floaty = 200;
    //屏幕宽高
    public static int screenWidth = 1080, screenHeight = 2340;
    //图标悬浮窗是否运行
    public static boolean floatWindwosStatus = false;
    //菜单悬浮窗是否运行
    public static boolean menuWindwosStatus = false;
    //悬浮窗朝向，0-左向右、1-右向左
    public static int windowDirection = 1;
    //图标是否吸附屏幕边缘，0-吸附、1-不吸附
    public static int restoreFloating = 0;
    //下一次切换呼出方式的时间
    public static long time = 0;

    public static int[] getScreenResolution(Context context) {
        int[] resolution = new int[2]; // 数组用于存放宽度和高度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) 及以上使用 WindowMetrics
            WindowManager windowManager = context.getSystemService(WindowManager.class);
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            resolution[0] = windowMetrics.getBounds().width();
            resolution[1] = windowMetrics.getBounds().height();
        } else {
            // Android 11 以下使用 DisplayMetrics
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            resolution[0] = displayMetrics.widthPixels;
            resolution[1] = displayMetrics.heightPixels;
        }
        WindowBean.screenWidth = resolution[0];
        WindowBean.screenHeight = resolution[1];
        return resolution;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean isLandscape(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            Param.横屏 = true;
        } else {
            Param.横屏 = false;
        }
        String screen = FileUtils.readFileContent(WindowBean.screenFilePath);
        if (screen != null && screen.length() > 6) {
            String[] screenArr = screen.split(",");
            int newScreenWidth = Integer.valueOf(screenArr[Param.横屏 ? 1 : 0]);
            int newScreenHeight = Integer.valueOf(screenArr[Param.横屏 ? 0 : 1]);
            // 只有当屏幕宽度或高度发生变化时，才执行后续的操作
            if (newScreenWidth != WindowBean.screenWidth || newScreenHeight != WindowBean.screenHeight) {
                // 更新屏幕宽度和高度
                WindowBean.screenWidth = newScreenWidth;
                WindowBean.screenHeight = newScreenHeight;
                // 执行操作
                MainActivity.yolov5ncnn.setScreen(WindowBean.screenWidth, WindowBean.screenHeight);
                MainActivity.createVirtualDisplay();
                FloatingWindowService.restoreFloatingView();

//                CardCounter.initialize();
            }
        }
        return Param.横屏;
    }

}
