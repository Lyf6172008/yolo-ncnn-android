package com.tencent.yoloNcnn;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtil {
    public static String imageToBase64(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("Image must be non-null.");
        }

        // 将 RGBA_8888 转换为 Bitmap
        Bitmap bitmap = imageToBitmap(image);
        image.close(); // 关闭 Image，避免资源泄露

        if (bitmap == null) {
            throw new RuntimeException("Failed to convert Image to Bitmap");
        }

        // **压缩尺寸**（缩小为原来的一半）
        int newWidth = bitmap.getWidth();
        int newHeight = bitmap.getHeight();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        // 压缩 Bitmap 为 JPEG 并转换为 Base64
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] jpegBytes = outputStream.toByteArray();

        return Base64.encodeToString(jpegBytes, Base64.DEFAULT);
    }

    // 将 Image (RGBA_8888) 转换为 Bitmap
    private static Bitmap imageToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();

        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height); // 裁剪掉 rowPadding
    }
}
