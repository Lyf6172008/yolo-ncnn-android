package com.tencent.yoloNcnn;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

public class WirelessDebuggingHelper {

    private static final String TAG = "WirelessDebuggingHelper";

    /**
     * 启用无线调试（通过反射调用系统 API）
     */
    public static void enableWirelessDebugging(Context context) {
        try {
            // 获取 ServiceManager 类
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);

            // 获取 IAdbManager 实例
            Object adbManager = getServiceMethod.invoke(null, "adb");

            if (adbManager == null) {
                Log.e(TAG, "ADB service not available");
                return;
            }

            // 获取 enableWirelessDebugging 方法
            Class<?> adbManagerClass = Class.forName("android.debug.IAdbManager");
            Method enableWirelessMethod = adbManagerClass.getMethod("enableWirelessDebugging", boolean.class);

            // 启用无线调试
            enableWirelessMethod.invoke(adbManager, true);
            Log.d(TAG, "Wireless debugging enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error enabling wireless debugging", e);
        }
    }

    /**
     * 使用配对码配对设备（通过反射调用系统 API）
     *
     * @param pairingCode 配对码
     */
    public static void pairDevice(String pairingCode) {
        try {
            // 获取 ServiceManager 类
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);

            // 获取 IAdbManager 实例
            Object adbManager = getServiceMethod.invoke(null, "adb");

            if (adbManager == null) {
                Log.e(TAG, "ADB service not available");
                return;
            }

            // 获取 pairDevice 方法
            Class<?> adbManagerClass = Class.forName("android.debug.IAdbManager");
            Method pairDeviceMethod = adbManagerClass.getMethod("pairDevice", String.class);

            // 使用配对码配对设备
            pairDeviceMethod.invoke(adbManager, pairingCode);
            Log.d(TAG, "Device paired successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error pairing device", e);
        }
    }

    /**
     * 通过无线调试执行 ADB 命令
     *
     * @param command ADB 命令
     * @return 命令输出结果
     */
    public static String executeAdbCommand(String command) {
        try {
            // 连接到无线调试服务
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", 5555)); // 默认端口为 5555

            // 发送命令
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(command.getBytes());
            outputStream.flush();

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            // 关闭连接
            socket.close();
            return response.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error executing ADB command", e);
            return null;
        }
    }
}