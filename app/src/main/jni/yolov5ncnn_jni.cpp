#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <thread>
#include <linux/input.h>
#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <string.h>
#include <cstring>
#include <sys/stat.h>
#include <fcntl.h>
#include <jni.h>

// ncnn
#include "layer.h"
#include "net.h"
#include "benchmark.h"

#include "yolov8.h"
#include "layer.h"
#include "net.h"

#if defined(USE_NCNN_SIMPLEOCV)
#include "simpleocv.h"
#else

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#endif

#include <algorithm>
#include <float.h>
#include <stdio.h>
#include <dirent.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <ctype.h>
#include <sys/socket.h>
#include <mutex>

// 全局互斥量
std::mutex screen_config_mutex;
using namespace std;

#define TAG "YOLO-V5" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;
static ncnn::Net yolov5;
static YOLOv8 *g_yolov8 = 0;

class YoloV5Focus : public ncnn::Layer {
public:
    YoloV5Focus() {
        one_blob_only = true;
    }

    virtual int
    forward(const ncnn::Mat &bottom_blob, ncnn::Mat &top_blob, const ncnn::Option &opt) const {
        int w = bottom_blob.w;
        int h = bottom_blob.h;
        int channels = bottom_blob.c;

        int outw = w / 2;
        int outh = h / 2;
        int outc = channels * 4;

        top_blob.create(outw, outh, outc, 4u, 1, opt.blob_allocator);
        if (top_blob.empty())
            return -100;

#pragma omp parallel for num_threads(opt.num_threads)
        for (int p = 0; p < outc; p++) {
            const float *ptr = bottom_blob.channel(p % channels).row((p / channels) % 2) +
                               ((p / channels) / 2);
            float *outptr = top_blob.channel(p);

            for (int i = 0; i < outh; i++) {
                for (int j = 0; j < outw; j++) {
                    *outptr = *ptr;

                    outptr += 1;
                    ptr += 2;
                }

                ptr += w;
            }
        }

        return 0;
    }
};

DEFINE_LAYER_CREATOR(YoloV5Focus)

//struct Object {
//    cv::Rect_<float> rect;
//    int label;
//    float prob;
//    cv::Mat mask;   // 分割掩码
//};

static inline float intersection_area(const Object &a, const Object &b) {
    cv::Rect_<float> inter = a.rect & b.rect;
    return inter.area();
}

static void qsort_descent_inplace(std::vector<Object> &faceobjects, int left, int right) {
    int i = left;
    int j = right;
    float p = faceobjects[(left + right) / 2].prob;
    while (i <= j) {
        while (faceobjects[i].prob > p)
            i++;
        while (faceobjects[j].prob < p)
            j--;
        if (i <= j) {
            std::swap(faceobjects[i], faceobjects[j]);
            i++;
            j--;
        }
    }
#pragma omp parallel sections
    {
#pragma omp section
        {
            if (left < j) qsort_descent_inplace(faceobjects, left, j);
        }
#pragma omp section
        {
            if (i < right) qsort_descent_inplace(faceobjects, i, right);
        }
    }
}

static void qsort_descent_inplace(std::vector<Object> &faceobjects) {
    if (faceobjects.empty())
        return;
    qsort_descent_inplace(faceobjects, 0, faceobjects.size() - 1);
}

static void nms_sorted_bboxes(const std::vector<Object> &faceobjects, std::vector<int> &picked,
                              float nms_threshold, bool agnostic = false) {
    picked.clear();
    const int n = faceobjects.size();
    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) {
        areas[i] = faceobjects[i].rect.area();
    }
    for (int i = 0; i < n; i++) {
        const Object &a = faceobjects[i];
        int keep = 1;
        for (int j = 0; j < (int) picked.size(); j++) {
            const Object &b = faceobjects[picked[j]];
            if (!agnostic && a.label != b.label)
                continue;
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            if (inter_area / union_area > nms_threshold)
                keep = 0;
        }
        if (keep)
            picked.push_back(i);
    }
}

static inline float sigmoid(float x) {
    return static_cast<float>(1.f / (1.f + exp(-x)));
}

static void generate_proposals(const ncnn::Mat &anchors, int stride, const ncnn::Mat &in_pad,
                               const ncnn::Mat &feat_blob, float prob_threshold,
                               std::vector<Object> &objects) {
    const int num_grid_x = feat_blob.w;
    const int num_grid_y = feat_blob.h;
    const int num_anchors = anchors.w / 2;
    const int num_class = feat_blob.c / num_anchors - 5;
    const int feat_offset = num_class + 5;
    for (int q = 0; q < num_anchors; q++) {
        const float anchor_w = anchors[q * 2];
        const float anchor_h = anchors[q * 2 + 1];
        for (int i = 0; i < num_grid_y; i++) {
            for (int j = 0; j < num_grid_x; j++) {
                int class_index = 0;
                float class_score = -FLT_MAX;
                for (int k = 0; k < num_class; k++) {
                    float score = feat_blob.channel(q * feat_offset + 5 + k).row(i)[j];
                    if (score > class_score) {
                        class_index = k;
                        class_score = score;
                    }
                }
                float box_score = feat_blob.channel(q * feat_offset + 4).row(i)[j];
                float confidence = sigmoid(box_score) * sigmoid(class_score);
                if (confidence >= prob_threshold) {
                    float dx = sigmoid(feat_blob.channel(q * feat_offset + 0).row(i)[j]);
                    float dy = sigmoid(feat_blob.channel(q * feat_offset + 1).row(i)[j]);
                    float dw = sigmoid(feat_blob.channel(q * feat_offset + 2).row(i)[j]);
                    float dh = sigmoid(feat_blob.channel(q * feat_offset + 3).row(i)[j]);
                    float pb_cx = (dx * 2.f - 0.5f + j) * stride;
                    float pb_cy = (dy * 2.f - 0.5f + i) * stride;
                    float pb_w = pow(dw * 2.f, 2) * anchor_w;
                    float pb_h = pow(dh * 2.f, 2) * anchor_h;
                    float x0 = pb_cx - pb_w * 0.5f;
                    float y0 = pb_cy - pb_h * 0.5f;
                    float x1 = pb_cx + pb_w * 0.5f;
                    float y1 = pb_cy + pb_h * 0.5f;
                    Object obj;
                    obj.rect.x = x0;
                    obj.rect.y = y0;
                    obj.rect.width = x1 - x0;
                    obj.rect.height = y1 - y0;
                    obj.label = class_index;
                    obj.prob = confidence;
                    objects.push_back(obj);
                }
            }
        }
    }
}

extern "C" {

static jclass objCls = NULL;
static jmethodID constructortorId;
static jfieldID xId;
static jfieldID yId;
static jfieldID wId;
static jfieldID hId;
static jfieldID labelId;
static jfieldID probId;
static jfieldID maskId;
static jfieldID keypointsId; // 存储 keypoints (List<KeyPoint>)
// KeyPoint 相关
static jclass keyPointCls = NULL;
static jmethodID keyPointConstructor; // KeyPoint(float x, float y, float prob)
// ArrayList 相关
static jclass arrayListCls = NULL;
static jmethodID arrayListInit;  // 构造方法 ArrayList()
static jmethodID arrayListAdd;   // boolean add(Object)


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "YoloV5Ncnn", "JNI_OnLoad");
    ncnn::create_gpu_instance();
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "YoloV5Ncnn", "JNI_OnUnload");
    ncnn::destroy_gpu_instance();
}

void 数据分割(const std::string &分割数据, std::vector<std::string> &存储数组,
              const std::string &分隔符) {
    size_t start = 0, end = 0;
    while ((end = 分割数据.find(分隔符, start)) != std::string::npos) {
        存储数组.push_back(分割数据.substr(start, end - start));
        start = end + 分隔符.length();
    }
    存储数组.push_back(分割数据.substr(start));
}


int socketRet = -1;
int server_fd;
struct sockaddr_in serveraddr;
std::string IP = "127.0.0.1";

void createClientSocket() {
    LOGI("Socket连接中");
    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        LOGI("Socket创建错误");
        exit(-1);
    }
    if (server_fd < 0) {
        LOGI("Socket连接失败1");
        exit(-1);
    }
    serveraddr.sin_family = AF_INET;
    serveraddr.sin_addr.s_addr = inet_addr((char *) IP.data());
    serveraddr.sin_port = htons(16384);
    socketRet = connect(server_fd, (struct sockaddr *) &serveraddr, sizeof(struct sockaddr));
    if (socketRet < 0) {
        LOGI("Socket连接失败2");
        exit(-1);
    }
    LOGI("Socket连接成功");
}

//客户端发送消息
void sendMsg(char *msg) {
    send(server_fd, msg, strlen(msg), 0);
}

struct Vector2A {
    int X;
    int Y;

    Vector2A() {
        this->X = 0;
        this->Y = 0;
    }
};
struct Screen {
    float ScreenX;
    float ScreenY;
};
Screen full_screen;
Vector2A vpvp;
int Orientation = 0;
static int screen_x = 0, screen_y = 0;
bool aimStatusInit = false;
bool aimStatus = false;
int 触摸点X = 1800;
int 触摸点Y = 350;
float 触摸点范围 = 300;
float 瞄准力度 = 10;
float 瞄准范围 = 1000;
float 瞄准速度 = 10;
int MaxPlayerCount = 0;
int AimCount = 0;
int Gmin = -1;
struct AimStruct {
    Vector2A ObjAim;
    float ScreenDistance = 0;
} Aim[100];

int findminat() {
    float min = 999999;
    int minAt = 999;
    for (int i = 0; i < MaxPlayerCount; i++) {
        if (Aim[i].ScreenDistance < min && Aim[i].ScreenDistance != 0) {
            minAt = i;
            min = Aim[i].ScreenDistance;
        }
    }
    if (minAt == 999) {
        Gmin = -1;
        return -1;
    }
    Gmin = minAt;
    return minAt;
}

void TouchReleaseIfDown(bool &isDown, int &tx, int &ty) {
    if (isDown) {
        tx = 触摸点X, ty = 触摸点Y;
        std::string touchData = std::to_string(tx) + "," + std::to_string(ty) + ",8,3\n";
        send(server_fd, touchData.c_str(), strlen(touchData.c_str()), 0);
        isDown = false;
    }
}
void AimBotAuto() {
    bool isDown = false;
    int maxAdjustment = 5;
    static int TargetX = 0;
    static int TargetY = 0;
    int tx = 触摸点X, ty = 触摸点Y;
    while (aimStatus) {
        int ScrXH = screen_x / 2;
        int ScrYH = screen_y / 2;
        findminat();
        int ToReticleDistance = static_cast<int>(Aim[Gmin].ScreenDistance);
        if (ToReticleDistance > static_cast<int>(瞄准范围) || !aimStatus || Gmin == -1 ||
            AimCount <= 0) {
            TouchReleaseIfDown(isDown, tx, ty);
            isDown = false;
            usleep(static_cast<useconds_t>(瞄准力度 * 1000));
            continue;
        }
        vpvp.X = std::clamp(static_cast<int>(Aim[Gmin].ObjAim.X), 0, screen_x);
        vpvp.Y = std::clamp(static_cast<int>(Aim[Gmin].ObjAim.Y), 0, screen_y);
        if (!isDown) {
            std::string touchData = std::to_string(tx) + "," + std::to_string(ty) + ",8,1\n";
            send(server_fd, touchData.c_str(), strlen(touchData.c_str()), 0);
            isDown = true;
        }
        TargetX = (vpvp.X > ScrXH) ? -(ScrXH - vpvp.X) : (vpvp.X - ScrXH);
        TargetY = (vpvp.Y > ScrYH) ? -(ScrYH - vpvp.Y) : (vpvp.Y - ScrYH);
        float distance = std::sqrt(TargetX * TargetX + TargetY * TargetY);
        int moveStepX = 0;
        int moveStepY = 0;
        if (distance > 0) {
            moveStepX = static_cast<int>(maxAdjustment * (TargetX / distance));
            moveStepY = static_cast<int>(maxAdjustment * (TargetY / distance));
            tx += moveStepX;
            ty += moveStepY;
        }
        tx = std::clamp(tx, 0, screen_x);
        ty = std::clamp(ty, 0, screen_y);
        if (tx <= 触摸点X + static_cast<int>(触摸点范围) &&
            tx >= 触摸点X - static_cast<int>(触摸点范围) &&
            ty <= 触摸点Y + static_cast<int>(触摸点范围) &&
            ty >= 触摸点Y - static_cast<int>(触摸点范围) &&
            tx != 触摸点X && ty != 触摸点Y) {
            std::string touchData = std::to_string(tx) + "," + std::to_string(ty) + ",8,1\n";
            send(server_fd, touchData.c_str(), strlen(touchData.c_str()), 0);
//            Aim[Gmin].ObjAim.X -= moveStepX;
//            Aim[Gmin].ObjAim.Y -= moveStepY;
        }
        usleep(static_cast<useconds_t>(瞄准力度 * 1000));
    }
}

JNIEXPORT void JNICALL Java_com_tencent_yoloNcnn_YoloV5Ncnn_setScreen(
        JNIEnv *env, jobject thiz, jint width, jint height) {
    screen_x = width;
    screen_y = height;
    full_screen.ScreenX = screen_x;
    full_screen.ScreenY = screen_y;
}
JNIEXPORT void JNICALL Java_com_tencent_yoloNcnn_YoloV5Ncnn_setAim(
        JNIEnv *env, jobject thiz, jboolean onOrOff) {
    if (system("su -c 'id'") != 0) {
        printf("无法获取root权限\n");
    } else if (-1 == socketRet) {
        createClientSocket();
        LOGI("初始化服务链接完成");
        aimStatus = onOrOff;
        if (!aimStatusInit && aimStatus) {
            aimStatusInit = true;
            new std::thread(AimBotAuto);
        }
    }
}

std::vector<std::string> class_names;
JNIEXPORT jboolean JNICALL Java_com_tencent_yoloNcnn_YoloV5Ncnn_Init(JNIEnv *env, jobject thiz) {
    LOGI("初始化JNI配置");

    // 获取 Obj 类并创建全局引用
    jclass localObjCls = env->FindClass("com/tencent/yoloNcnn/YoloV5Ncnn$Obj");
    objCls = reinterpret_cast<jclass>(env->NewGlobalRef(localObjCls));

    // 获取 Obj 的构造方法和字段 ID
    constructortorId = env->GetMethodID(objCls, "<init>", "(Lcom/tencent/yoloNcnn/YoloV5Ncnn;)V");
    xId = env->GetFieldID(objCls, "x", "F");
    yId = env->GetFieldID(objCls, "y", "F");
    wId = env->GetFieldID(objCls, "w", "F");
    hId = env->GetFieldID(objCls, "h", "F");
    labelId = env->GetFieldID(objCls, "label", "Ljava/lang/String;");
    probId = env->GetFieldID(objCls, "prob", "F");
    maskId = env->GetFieldID(objCls, "mask", "[[Z");  // 获取 mask 的字段 ID
    keypointsId = env->GetFieldID(objCls, "keypoints", "Ljava/util/List;");

    // 获取 KeyPoint 类并创建全局引用
    jclass localKeyPointCls = env->FindClass("com/tencent/yoloNcnn/YoloV5Ncnn$KeyPoint");
    keyPointCls = reinterpret_cast<jclass>(env->NewGlobalRef(localKeyPointCls));

    // 获取 KeyPoint 构造方法
    keyPointConstructor = env->GetMethodID(keyPointCls, "<init>", "(FFF)V");

    // 获取 ArrayList 类，并创建全局引用
    jclass localArrayListCls = env->FindClass("java/util/ArrayList");
    arrayListCls = reinterpret_cast<jclass>(env->NewGlobalRef(localArrayListCls));

    // 获取 ArrayList 方法 ID
    arrayListInit = env->GetMethodID(arrayListCls, "<init>", "()V");
    arrayListAdd = env->GetMethodID(arrayListCls, "add", "(Ljava/lang/Object;)Z");

    LOGI("初始化YOLO配置完成");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_tencent_yoloNcnn_YoloV5Ncnn_setModel(
        JNIEnv *env, jobject thiz, jstring model_param_path, jstring model_bin_path,
        jstring jni_class_names,jstring jni_model_tyle) {
    LOGI("初始化YOLO配置");
    ncnn::Option opt;
    opt.lightmode = true;
    opt.num_threads = 4;
    opt.blob_allocator = &g_blob_pool_allocator;
    opt.workspace_allocator = &g_workspace_pool_allocator;
    opt.use_packing_layout = true;
    if (ncnn::get_gpu_count() != 0) {
        opt.use_vulkan_compute = true;
        LOGI("Vulkan加速推理");
    }
    LOGI("yolo版本：%s",env->GetStringUTFChars(jni_model_tyle, (jboolean *) false));
    if (strcmp(env->GetStringUTFChars(jni_model_tyle, (jboolean *) false), "v5") == 0) {
        LOGI("加载v5");
        yolov5.clear();
        yolov5.opt = opt;
        int ret = yolov5.load_param(env->GetStringUTFChars(model_param_path, (jboolean *) false));
        if (ret != 0) {
            LOGI("加载参数失败");
            return JNI_FALSE;
        }
        LOGI("加载参数完成");
        ret = yolov5.load_model(env->GetStringUTFChars(model_bin_path, (jboolean *) false));
        if (ret != 0) {
            LOGI("加载模型失败");
            return JNI_FALSE;
        }
    } else if (strcmp(env->GetStringUTFChars(jni_model_tyle, (jboolean *) false), "v8") == 0) {
        const char* model_param_path_cstr = env->GetStringUTFChars(model_param_path, NULL);  // 获取字符串指针
        if (model_param_path_cstr == NULL) return JNI_FALSE;  // 失败则返回
        LOGI("加载v8");
        if (strstr(model_param_path_cstr, "pose") != NULL) {
            LOGI("加载pose");
            g_yolov8 = new YOLOv8_pose;
        } else if (strstr(model_param_path_cstr, "seg") != NULL) {
            LOGI("加载seg");
            g_yolov8 = new YOLOv8_seg;
        } else {
            LOGI("加载默认 det");
            g_yolov8 = new YOLOv8_det;
        }
        g_yolov8->load(env->GetStringUTFChars(model_param_path, (jboolean *) false),
                       env->GetStringUTFChars(model_bin_path, (jboolean *) false),
                       true);
    } else {
        LOGI("加载失败，退出程序");
        exit(0);
    }
    class_names.clear();
    const char *str = env->GetStringUTFChars(jni_class_names, nullptr);
    std::string 类名(str);
    数据分割(类名, class_names, ",");
    env->ReleaseStringUTFChars(jni_class_names, str);
    LOGI("加载模型完成");
    return JNI_TRUE;
}
JNIEXPORT jobjectArray JNICALL
Java_com_tencent_yoloNcnn_YoloV5Ncnn_DetectV8(JNIEnv *env, jobject thiz, jobject surface, jobject bitmap,
                                              jboolean use_gpu, jfloat prob, jint target_size,
                                              jfloat prob_threshold, jfloat nms_threshold) {
    if (use_gpu) {
        ncnn::create_gpu_instance();
    } else {
        ncnn::create_gpu_instance("libvulkan_freedreno.so");
    }
    if (use_gpu == JNI_TRUE && ncnn::get_gpu_count() == 0)
        return NULL;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return NULL;

    int width = info.width;
    int height = info.height;

    float scale = (width > height) ? (float)target_size / width : (float)target_size / height;
    int w = width * scale;
    int h = height * scale;
    int wpad = (w + 31) / 32 * 32 - w;
    int hpad = (h + 31) / 32 * 32 - h;

    // 加载并预处理图像
    ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGB, w, h);
    std::vector<Object> objects = g_yolov8->detect(in, width, height, wpad, hpad, scale, screen_x, screen_y, target_size, prob_threshold, nms_threshold);

    jobjectArray jObjArray = env->NewObjectArray(objects.size(), objCls, NULL);
    for (size_t i = 0; i < objects.size(); i++) {
        if(prob >= objects[i].prob)continue;
        jobject jObj = env->NewObject(objCls, constructortorId, thiz);

        if (class_names.size() <= objects[i].label) {
            env->SetObjectField(jObj, labelId, env->NewStringUTF(std::to_string(objects[i].label).data()));
        } else {
            env->SetObjectField(jObj, labelId, env->NewStringUTF(class_names[objects[i].label].data()));
        }

        env->SetFloatField(jObj, xId, objects[i].rect.x);
        env->SetFloatField(jObj, yId, objects[i].rect.y);
        env->SetFloatField(jObj, wId, objects[i].rect.width);
        env->SetFloatField(jObj, hId, objects[i].rect.height);
        env->SetFloatField(jObj, probId, objects[i].prob);

        // 处理 mask 转换
        int mask_height = objects[i].mask.rows;
        int mask_width = objects[i].mask.cols;

        jobjectArray jMaskArray = env->NewObjectArray(mask_height, env->FindClass("[Z"), NULL);
        for (int y = 0; y < mask_height; y++) {
            jbooleanArray row = env->NewBooleanArray(mask_width);
            std::vector<jboolean> rowData(mask_width);
            for (int x = 0; x < mask_width; x++) {
                rowData[x] = objects[i].mask.at<uchar>(y, x) > 0 ? JNI_TRUE : JNI_FALSE;
            }
            env->SetBooleanArrayRegion(row, 0, mask_width, rowData.data());
            env->SetObjectArrayElement(jMaskArray, y, row);
            env->DeleteLocalRef(row);
        }
        env->SetObjectField(jObj, maskId, jMaskArray);
        env->DeleteLocalRef(jMaskArray);

        // 处理 KeyPoint 转换
        jobject jKeypointsList = env->NewObject(arrayListCls, arrayListInit);
        for (const KeyPoint &keypoint : objects[i].keypoints) {
            jobject jKeyPoint = env->NewObject(keyPointCls, keyPointConstructor, keypoint.p.x, keypoint.p.y, keypoint.prob);
            env->CallBooleanMethod(jKeypointsList, arrayListAdd, jKeyPoint);
            env->DeleteLocalRef(jKeyPoint);
        }
        env->SetObjectField(jObj, keypointsId, jKeypointsList);
        env->DeleteLocalRef(jKeypointsList);

        env->SetObjectArrayElement(jObjArray, i, jObj);
        env->DeleteLocalRef(jObj);
    }

    return jObjArray;
}

JNIEXPORT jobjectArray JNICALL
Java_com_tencent_yoloNcnn_YoloV5Ncnn_DetectV5(JNIEnv *env, jobject thiz, jobject surface, jobject bitmap,
                                              jboolean use_gpu,
                                              jfloat prob, jint target_size,
                                              jfloat prob_threshold, jfloat nms_threshold) {
    if (use_gpu == JNI_TRUE && ncnn::get_gpu_count() == 0)
        return NULL;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
        return NULL;

    int width = info.width;
    int height = info.height;

    // 计算缩放比例和填充
    float scale = (width > height) ? (float) target_size / width : (float) target_size / height;
    int w = width * scale;
    int h = height * scale;
    int wpad = (w + 31) / 32 * 32 - w;
    int hpad = (h + 31) / 32 * 32 - h;

    // 加载并预处理图像
    ncnn::Mat in = ncnn::Mat::from_android_bitmap_resize(env, bitmap, ncnn::Mat::PIXEL_RGB, w, h);
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2,
                           wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 114.f);
    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = yolov5.create_extractor();
    ex.set_vulkan_compute(use_gpu);
    ex.input("in0", in_pad);
    std::vector<Object> proposals;
    const std::array<int, 3> strides = {8, 16, 32};
    const float anchors_data[3][6] = {
            {10.f,  13.f, 16.f,  30.f,  33.f,  23.f},
            {30.f,  61.f, 62.f,  45.f,  59.f,  119.f},
            {116.f, 90.f, 156.f, 198.f, 373.f, 326.f}
    };
    for (int i = 0; i < 3; ++i) {
        ncnn::Mat out;
        ex.extract(("out" + std::to_string(i)).c_str(), out);
        ncnn::Mat anchors(6);
        for (int j = 0; j < 6; j++) {
            anchors[j] = anchors_data[i][j];
        }
        std::vector<Object> objects;
        generate_proposals(anchors, strides[i], in_pad, out, prob_threshold, objects);
        proposals.insert(proposals.end(), objects.begin(), objects.end());
    }

    // 非极大值抑制
    qsort_descent_inplace(proposals);
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, nms_threshold);

    // 过滤并构建返回结果
    std::vector<Object> objects(picked.size());
    int left = static_cast<int>((screen_x - static_cast<float>(width)) / 2.0f);
    int top = static_cast<int>((screen_y - static_cast<float>(height)) / 2.0f);

    // 创建 Java 对象数组
    jobjectArray jObjArray = env->NewObjectArray(objects.size(), objCls, NULL);
    MaxPlayerCount = objects.size();
    AimCount = 0;

    for (size_t i = 0; i < objects.size(); i++) {
        // 获取 proposal
        objects[i] = proposals[picked[i]];
        if(prob >= objects[i].prob)continue;

        // 坐标转换
        float x0 = (objects[i].rect.x - (wpad / 2)) / scale + left;
        float y0 = (objects[i].rect.y - (hpad / 2)) / scale + top;
        float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale + left;
        float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale + top;

        // 更新坐标和尺寸
        objects[i].rect.x = x0;
        objects[i].rect.y = y0;
        objects[i].rect.width = x1 - x0;
        objects[i].rect.height = y1 - y0;

        // 创建 Java 对象
        jobject jObj = env->NewObject(objCls, constructortorId, thiz);

        // 计算瞄准点
        if (aimStatus) {
            Vector2A D;
            D.X = objects[i].rect.x + objects[i].rect.width / 2;
            D.Y = objects[i].rect.y + objects[i].rect.height / 2;
            Aim[AimCount].ObjAim = D;
            Aim[AimCount].ScreenDistance = sqrt(pow(screen_x / 2 - D.X, 2)
                                                + pow(screen_y / 2 - D.Y, 2));
            AimCount++;
        }

        // 设置 Java 对象的字段
        if (class_names.size() <= objects[i].label){
            env->SetObjectField(jObj, labelId, env->NewStringUTF(std::to_string(objects[i].label).data()));
        }else{
            env->SetObjectField(jObj, labelId, env->NewStringUTF(class_names[objects[i].label].data()));
        }
        env->SetFloatField(jObj, xId, objects[i].rect.x);
        env->SetFloatField(jObj, yId, objects[i].rect.y);
        env->SetFloatField(jObj, wId, objects[i].rect.width);
        env->SetFloatField(jObj, hId, objects[i].rect.height);
        env->SetFloatField(jObj, probId, objects[i].prob);

        // 将 Java 对象添加到数组
        env->SetObjectArrayElement(jObjArray, i, jObj);
    }

    return jObjArray;
}

}
