package com.tencent.yoloNcnn;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;

import java.nio.ByteBuffer;

public class MediaProjectionHelper {
    public static MediaProjection mediaProjection;
    public static VirtualDisplay virtualDisplay;
    public static ImageReader imageReader;
    public static Bitmap capturedBitmap;

    private static int density;
    private static int width;
    private static int height;

    private static Context context;
    private static android.os.Handler handler;

    public static void initialize(MediaProjection projection, Context ctx, android.os.Handler handlerThread) {
        mediaProjection = projection;
        context = ctx;
        handler = handlerThread;

        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        density = metrics.densityDpi;
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        createVirtualDisplay();
    }

    public static void reloadVirtualDisplay(int newWidth, int newHeight) {
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }

        width = newWidth;
        height = newHeight;

        createVirtualDisplay();
    }

    @SuppressLint("WrongConstant")
    private static void createVirtualDisplay() {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null
        );

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                capturedBitmap = processImage(image);
                image.close();
            }
        }, handler);
    }

    public static Bitmap processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(), Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    public static Bitmap processImage(Image image, int captureWidth, int captureHeight) {
        if (-1 == captureWidth || -1 == captureHeight) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * image.getWidth();
            Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            return bitmap;
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        // 计算中心位置和截图区域
        int startX = (imageWidth - captureWidth) / 2;
        int startY = (imageHeight - captureHeight) / 2;
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        // 仅处理中心区域或整个图像
        Bitmap bitmap = Bitmap.createBitmap(captureWidth, captureHeight, Bitmap.Config.ARGB_8888);
        // 创建临时ByteBuffer以处理中心区域
        ByteBuffer tempBuffer = ByteBuffer.allocateDirect(captureWidth * captureHeight * pixelStride);
        for (int row = startY; row < startY + captureHeight; row++) {
            // 设置缓冲区的位置为当前行的起点
            buffer.position(row * rowStride + startX * pixelStride);
            // 将当前行的像素数据放入临时缓冲区
            buffer.get(tempBuffer.array(), (row - startY) * captureWidth * pixelStride, captureWidth * pixelStride);
        }
        // 将缓冲区的数据写入Bitmap
        bitmap.copyPixelsFromBuffer(tempBuffer);
        return bitmap;
    }


    public static void stop() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }
}
