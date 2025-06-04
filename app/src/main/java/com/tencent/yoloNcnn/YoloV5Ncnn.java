package com.tencent.yoloNcnn;

import android.graphics.Bitmap;
import android.view.Surface;

import java.util.List;

public class YoloV5Ncnn {
    public static class KeyPoint {
        public float x;
        public float y;
        public float prob;
        KeyPoint(float x, float y, float prob) {
            this.x = x;
            this.y = y;
            this.prob = prob;
        }
    }

    public class Obj {
        public float x;
        public float y;
        public float w;
        public float h;
        public String label;
        public float prob;
        public List<KeyPoint> keypoints;
        public boolean[][] mask;
    }

    public native boolean Init();

    public native boolean setModel(String model_param_path, String model_bin_path, String class_names, String model_type);

    public native Obj[] DetectV5(Surface surface, Bitmap bitmap, boolean use_gpu,
                               float prob, int imgSize,
                               float probThreshold, float nmsThreshold);


    public native Obj[] DetectV8(Surface surface, Bitmap bitmap, boolean use_gpu,
                                            float prob, int imgSize,
                                            float probThreshold, float nmsThreshold);

    public native void setScreen(int width, int height);

    public native void setAim(boolean onOrOff);

    static {
        System.loadLibrary("yolov5ncnn");
    }

}
