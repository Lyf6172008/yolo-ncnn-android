#include "yolov8.h"

YOLOv8::~YOLOv8() {}

int YOLOv8::load(const char *parampath, const char *modelpath, bool use_gpu) {
    yolov8.clear();
    yolov8.opt = ncnn::Option();
#if NCNN_VULKAN
    yolov8.opt.use_vulkan_compute = use_gpu;
#endif
    yolov8.load_param(parampath);
    yolov8.load_model(modelpath);
    return 0;
}
