package com.tencent.yoloNcnn;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ImageSocketClient {

    private static final String TAG = "ImageSocketClient";
    private static final int RECONNECT_DELAY_MS = 5000; // 重连延迟时间（毫秒）
    private static final int READ_TIMEOUT_MS = 5000; // 读取超时（毫秒）
    private static final int WRITE_TIMEOUT_MS = 5000; // 写入超时（毫秒）

    private Gson gson = new Gson();
    private Socket socket;
    private BufferedOutputStream outputStream;
    private InputStream inputStream;
    private final String serverAddress;
    private final int port;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public ImageSocketClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
        connect(); // 初始化时建立连接
    }

    // 连接服务器
    private void connect() {
        executor.execute(() -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close(); // 确保之前的连接被关闭
                }
                // 创建 SSL 上下文
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new DefaultTrustManager()}, new java.security.SecureRandom());
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                // 创建 SSLSocket 实例，连接到指定的域名和端口
                socket = (SSLSocket) sslSocketFactory.createSocket(serverAddress, port);
                socket.setSoTimeout(READ_TIMEOUT_MS); // 设置读取超时

                // 获取输出流
                outputStream = new BufferedOutputStream(socket.getOutputStream());

                // 获取输入流
                inputStream = socket.getInputStream();
                Log.i(TAG, "连接成功");
                Param.服务器连接状态 = true;
            } catch (Exception e) {
                Log.e(TAG, "连接失败: " + e.getMessage());
                scheduleReconnect(); // 尝试重连
            }
        });
    }

    // 默认的 TrustManager 实现，信任所有证书（仅用于测试，不建议用于生产环境）
    private static class DefaultTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
        }
    }


    // 发送图像数据
    public void sendImage(Bitmap bitmap) {
        try {
            if (socket == null || socket.isClosed()) {
                Log.i(TAG, "连接被关闭");
                return;
            }
            // 将 Bitmap 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();
            // 发送图像数据
            outputStream.write(imageData);
            outputStream.flush();
            // 发送结束标志
            outputStream.write("END_OF_IMAGE".getBytes());
            outputStream.flush();
            // 接收服务器返回结果
            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder result = new StringBuilder();
            boolean endOfResult = false;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                result.append(new String(buffer, 0, bytesRead));
                // 检查是否包含结束标志
                if (result.indexOf("END_OF_RESULT") != -1) {
                    endOfResult = true;
                    break;
                }
            }
            // 处理接收到的结果
            if (endOfResult) {
                handler.post(() -> {
//                    Log.i(TAG, "Server response: " + result);
                    if (null != result && 0 < result.length()) {
                        // 处理服务器返回的结果
                        Response response = gson.fromJson(result.toString(), Response.class);
                        if (response != null && "success".equals(response.getStatus())) {
                            List<Detection> detections = response.getData();
                            YoloV5Ncnn.Obj[] objects = new YoloV5Ncnn.Obj[detections.size()];
                            //计算偏移
                            int left = (WindowBean.screenWidth - Param.识别范围X) / 2;
                            int top = (WindowBean.screenHeight - Param.识别范围Y) / 2;
                            for (int i = 0; i < detections.size(); i++) {
                                YoloV5Ncnn.Obj obj = MainActivity.yolov5ncnn.new Obj();
                                obj.x = (float) detections.get(i).getX() + left;
                                obj.y = (float) detections.get(i).getY() + top;
                                obj.w = (float) detections.get(i).getW();
                                obj.h = (float) detections.get(i).getH();
                                obj.prob = (float) detections.get(i).getProd();
                                obj.label = String.valueOf(detections.get(i).getClassId());
                                objects[i] = obj;
                            }
                            MainActivity.objects = objects;
                        } else {
                            System.out.println("推理状态不为success");
                        }
                    }
                });
            } else {
                handler.post(() -> {
                    Log.i(TAG, "没有接收到结束标志");
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 安排重连
    private void scheduleReconnect() {
        executor.execute(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS); // 等待一段时间后重连
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // 关闭连接
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket: " + e.getMessage());
        }
    }
}
