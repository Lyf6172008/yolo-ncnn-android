#ifndef YOLOV8_H
#define YOLOV8_H
#pragma once

#include <opencv2/core/core.hpp>

#include <net.h>

struct KeyPoint {
    cv::Point2f p;
    float prob;
};

struct Object {
    cv::Rect_<float> rect;
    cv::RotatedRect rrect;
    int label;
    float prob;
    int gindex;
    cv::Mat mask;
    std::vector<KeyPoint> keypoints;
};

class YOLOv8 {
public:
    virtual ~YOLOv8();

    int load(const char *parampath, const char *modelpath, bool use_gpu = false);

    virtual std::vector<Object>
    detect(ncnn::Mat in, int img_w, int img_h, int wpad, int hpad, float scale,
           float screen_x, float screen_y, int target_size, float prob_threshold,
           float nms_threshold) = 0;


protected:
    ncnn::Net yolov8;
};

class YOLOv8_det : public YOLOv8 {
public:
    virtual std::vector<Object>
    detect(ncnn::Mat in, int img_w, int img_h, int wpad, int hpad, float scale,
           float screen_x, float screen_y,
           int target_size, float prob_threshold, float nms_threshold);
};

class YOLOv8_seg : public YOLOv8 {
public:
    virtual std::vector<Object>
    detect(ncnn::Mat in, int img_w, int img_h, int wpad, int hpad, float scale,
           float screen_x, float screen_y,
           int target_size, float prob_threshold, float nms_threshold);
};

class YOLOv8_pose : public YOLOv8 {
public:
    virtual std::vector<Object>
    detect(ncnn::Mat in, int img_w, int img_h, int wpad, int hpad, float scale,
           float screen_x, float screen_y,
           int target_size, float prob_threshold, float nms_threshold);
};

#endif
