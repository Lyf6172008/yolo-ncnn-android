project(yolov5ncnn)

set(CMAKE_CXX_STANDARD 17)  # 使用 C++17 标准
set(CMAKE_CXX_STANDARD_REQUIRED YES)  # 强制使用指定的标准
set(CMAKE_CXX_EXTENSIONS NO)  # 禁止编译器使用非标准扩展

cmake_minimum_required(VERSION 3.4.1)

set(OpenCV_DIR D:/AndroidWork/OpenCV-android-sdk/sdk/native/jni)
find_package(OpenCV REQUIRED core imgproc imgcodecs)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(yolov5ncnn SHARED yolov5ncnn_jni.cpp yolov8.cpp yolov8_det.cpp yolov8_seg.cpp yolov8_pose.cpp ${TOUCH_SOURCES})

target_link_libraries(yolov5ncnn
        ncnn
        ${OpenCV_LIBS}
        jnigraphics
)
