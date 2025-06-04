package com.tencent.yoloNcnn;

import android.graphics.Bitmap;

import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;

public class ImageWebSocketClient extends WebSocketClient {

    public static  Gson gson = new Gson();
    private static final String END_OF_IMAGE = "END_OF_IMAGE";

    public ImageWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("服务器连接成功");
        Param.服务器连接状态 = true;
        Param.YOLO初始化 = true;
    }

    @Override
    public void onMessage(String message) {
        if (null != message && 0 < message.length()) {
            // 处理服务器返回的结果
            Response response = gson.fromJson(message, Response.class);
            if (response != null && "success".equals(response.getStatus())) {
                List<Detection> detections = response.getData();
                YoloV5Ncnn.Obj[] objects = new YoloV5Ncnn.Obj[detections.size()];
                //计算偏移
                int left = 0;
                int top = 0;
                if (!Param.识别全屏) {
                    left = (WindowBean.screenWidth - Param.识别范围X) / 2;
                    top = (WindowBean.screenHeight - Param.识别范围Y) / 2;
                }
                for (int i = 0; i < detections.size(); i++) {
                    YoloV5Ncnn.Obj obj = MainActivity.yolov5ncnn.new Obj();
                    obj.x = (float) detections.get(i).getX() + left;
                    obj.y = (float) detections.get(i).getY() + top;
                    obj.w = (float) detections.get(i).getW();
                    obj.h = (float) detections.get(i).getH();
                    obj.prob = (float) detections.get(i).getProd();
                    obj.label = Param.yolo类名集合.get(detections.get(i).getClassId());
                    objects[i] = obj;
                }
                MainActivity.objects = objects;
            } else {
                System.out.println("推理状态不为success");
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("连接关闭: " + reason);
        Param.服务器连接状态 = false;
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public void sendImage(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();
            byte[] endOfImageData = END_OF_IMAGE.getBytes();
            byte[] completeData = new byte[imageData.length + endOfImageData.length];
            System.arraycopy(imageData, 0, completeData, 0, imageData.length);
            System.arraycopy(endOfImageData, 0, completeData, imageData.length, endOfImageData.length);
            ByteBuffer completeBuffer = ByteBuffer.wrap(completeData);
            this.send(completeBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
