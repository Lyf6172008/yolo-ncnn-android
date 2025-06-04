#include "yolov8.h"

#include "layer.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <float.h>
#include <stdio.h>
#include <vector>

static inline float intersection_area(const Object &a, const Object &b) {
    cv::Rect_<float> inter = a.rect & b.rect;
    return inter.area();
}

static void qsort_descent_inplace(std::vector<Object> &objects, int left, int right) {
    int i = left;
    int j = right;
    float p = objects[(left + right) / 2].prob;

    while (i <= j) {
        while (objects[i].prob > p)
            i++;

        while (objects[j].prob < p)
            j--;

        if (i <= j) {
            // swap
            std::swap(objects[i], objects[j]);

            i++;
            j--;
        }
    }

    // #pragma omp parallel sections
    {
        // #pragma omp section
        {
            if (left < j) qsort_descent_inplace(objects, left, j);
        }
        // #pragma omp section
        {
            if (i < right) qsort_descent_inplace(objects, i, right);
        }
    }
}

static void qsort_descent_inplace(std::vector<Object> &objects) {
    if (objects.empty())
        return;

    qsort_descent_inplace(objects, 0, objects.size() - 1);
}

static void
nms_sorted_bboxes(const std::vector<Object> &objects, std::vector<int> &picked, float nms_threshold,
                  bool agnostic = false) {
    picked.clear();

    const int n = objects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) {
        areas[i] = objects[i].rect.area();
    }

    for (int i = 0; i < n; i++) {
        const Object &a = objects[i];

        int keep = 1;
        for (int j = 0; j < (int) picked.size(); j++) {
            const Object &b = objects[picked[j]];

            if (!agnostic && a.label != b.label)
                continue;

            // intersection over union
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            // float IoU = inter_area / union_area
            if (inter_area / union_area > nms_threshold)
                keep = 0;
        }

        if (keep)
            picked.push_back(i);
    }
}

static inline float sigmoid(float x) {
    return 1.0f / (1.0f + expf(-x));
}

static void
generate_proposals(const ncnn::Mat &pred, int stride, const ncnn::Mat &in_pad, float prob_threshold,
                   std::vector<Object> &objects) {
    const int w = in_pad.w;
    const int h = in_pad.h;

    const int num_grid_x = w / stride;
    const int num_grid_y = h / stride;

    const int reg_max_1 = 16;
    const int num_class = pred.w - reg_max_1 * 4; // number of classes. 80 for COCO

    for (int y = 0; y < num_grid_y; y++) {
        for (int x = 0; x < num_grid_x; x++) {
            const ncnn::Mat pred_grid = pred.row_range(y * num_grid_x + x, 1);

            // find label with max score
            int label = -1;
            float score = -FLT_MAX;
            {
                const ncnn::Mat pred_score = pred_grid.range(reg_max_1 * 4, num_class);

                for (int k = 0; k < num_class; k++) {
                    float s = pred_score[k];
                    if (s > score) {
                        label = k;
                        score = s;
                    }
                }

                score = sigmoid(score);
            }

            if (score >= prob_threshold) {
                ncnn::Mat pred_bbox = pred_grid.range(0, reg_max_1 * 4).reshape(reg_max_1,
                                                                                4).clone();

                {
                    ncnn::Layer *softmax = ncnn::create_layer("Softmax");

                    ncnn::ParamDict pd;
                    pd.set(0, 1); // axis
                    pd.set(1, 1);
                    softmax->load_param(pd);

                    ncnn::Option opt;
                    opt.num_threads = 1;
                    opt.use_packing_layout = false;

                    softmax->create_pipeline(opt);

                    softmax->forward_inplace(pred_bbox, opt);

                    softmax->destroy_pipeline(opt);

                    delete softmax;
                }

                float pred_ltrb[4];
                for (int k = 0; k < 4; k++) {
                    float dis = 0.f;
                    const float *dis_after_sm = pred_bbox.row(k);
                    for (int l = 0; l < reg_max_1; l++) {
                        dis += l * dis_after_sm[l];
                    }

                    pred_ltrb[k] = dis * stride;
                }

                float pb_cx = (x + 0.5f) * stride;
                float pb_cy = (y + 0.5f) * stride;

                float x0 = pb_cx - pred_ltrb[0];
                float y0 = pb_cy - pred_ltrb[1];
                float x1 = pb_cx + pred_ltrb[2];
                float y1 = pb_cy + pred_ltrb[3];

                Object obj;
                obj.rect.x = x0;
                obj.rect.y = y0;
                obj.rect.width = x1 - x0;
                obj.rect.height = y1 - y0;
                obj.label = label;
                obj.prob = score;
                obj.gindex = y * num_grid_x + x;

                objects.push_back(obj);
            }
        }
    }
}

static void
generate_proposals(const ncnn::Mat &pred, const std::vector<int> &strides, const ncnn::Mat &in_pad,
                   float prob_threshold, std::vector<Object> &objects) {
    const int w = in_pad.w;
    const int h = in_pad.h;

    int pred_row_offset = 0;
    for (size_t i = 0; i < strides.size(); i++) {
        const int stride = strides[i];

        const int num_grid_x = w / stride;
        const int num_grid_y = h / stride;
        const int num_grid = num_grid_x * num_grid_y;

        std::vector<Object> objects_stride;
        generate_proposals(pred.row_range(pred_row_offset, num_grid), stride, in_pad,
                           prob_threshold, objects_stride);

        for (size_t j = 0; j < objects_stride.size(); j++) {
            Object obj = objects_stride[j];
            obj.gindex += pred_row_offset;
            objects.push_back(obj);
        }

        pred_row_offset += num_grid;
    }
}

std::vector<Object> YOLOv8_seg::detect(ncnn::Mat in, int img_w, int img_h, int wpad, int hpad, float scale,
                                       float screen_x,float screen_y, int target_size, float prob_threshold, float nms_threshold) {
    std::vector<Object> objects;
    const float mask_threshold = 0.5f;
    std::vector<int> strides(3);
    strides[0] = 8;
    strides[1] = 16;
    strides[2] = 32;
    const int max_stride = 32;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2,
                           ncnn::BORDER_CONSTANT, 114.f);
    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in_pad.substract_mean_normalize(0, norm_vals);
    ncnn::Extractor ex = yolov8.create_extractor();
    ex.input("in0", in_pad);
    ncnn::Mat out;
    ex.extract("out0", out);
    std::vector<Object> proposals;
    generate_proposals(out, strides, in_pad, prob_threshold, proposals);
    qsort_descent_inplace(proposals);
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, nms_threshold);
    int count = picked.size();
    if (count == 0)
        return objects;

    ncnn::Mat mask_feat;
    ex.extract("out1", mask_feat);
    ncnn::Mat mask_protos;
    ex.extract("out2", mask_protos);
    ncnn::Mat objects_mask_feat(mask_feat.w, 1, count);
    objects.resize(count);
    for (int i = 0; i < count; i++) {
        objects[i] = proposals[picked[i]];
        int left = static_cast<int>((screen_x - static_cast<float>(img_w)) / 2.0f);
        int top = static_cast<int>((screen_y - static_cast<float>(img_h)) / 2.0f);
        // adjust offset to original unpadded
        float x0 = (objects[i].rect.x - (wpad / 2)) / scale + left;
        float y0 = (objects[i].rect.y - (hpad / 2)) / scale + top;
        float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale + left;
        float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale + top;
        // clip
        x0 = std::max(std::min(x0, (float) (img_w - 1)), 0.f);
        y0 = std::max(std::min(y0, (float) (img_h - 1)), 0.f);
        x1 = std::max(std::min(x1, (float) (img_w - 1)), 0.f);
        y1 = std::max(std::min(y1, (float) (img_h - 1)), 0.f);
        objects[i].rect.x = x0;
        objects[i].rect.y = y0;
        objects[i].rect.width = x1 - x0;
        objects[i].rect.height = y1 - y0;
        // pick mask feat
        memcpy(objects_mask_feat.channel(i), mask_feat.row(objects[i].gindex),
               mask_feat.w * sizeof(float));
    }

    // process mask
    ncnn::Mat objects_mask;
    {
        ncnn::Layer *gemm = ncnn::create_layer("Gemm");

        ncnn::ParamDict pd;
        pd.set(6, 1); // constantC
        pd.set(7, count); // constantM
        pd.set(8, mask_protos.w * mask_protos.h); // constantN
        pd.set(9, mask_feat.w); // constantK
        pd.set(10, -1); // constant_broadcast_type_C
        pd.set(11, 1); // output_N1M
        gemm->load_param(pd);

        ncnn::Option opt;
        opt.num_threads = 1;
        opt.use_packing_layout = false;

        gemm->create_pipeline(opt);

        std::vector<ncnn::Mat> gemm_inputs(2);
        gemm_inputs[0] = objects_mask_feat;
        gemm_inputs[1] = mask_protos.reshape(mask_protos.w * mask_protos.h, 1, mask_protos.c);
        std::vector<ncnn::Mat> gemm_outputs(1);
        gemm->forward(gemm_inputs, gemm_outputs, opt);
        objects_mask = gemm_outputs[0].reshape(mask_protos.w, mask_protos.h, count);

        gemm->destroy_pipeline(opt);

        delete gemm;
    }
    {
        ncnn::Layer *sigmoid = ncnn::create_layer("Sigmoid");

        ncnn::Option opt;
        opt.num_threads = 1;
        opt.use_packing_layout = false;
        sigmoid->create_pipeline(opt);
        sigmoid->forward_inplace(objects_mask, opt);
        sigmoid->destroy_pipeline(opt);
        delete sigmoid;
    }

    // resize mask map
    {
        ncnn::Mat objects_mask_resized;
        ncnn::resize_bilinear(objects_mask, objects_mask_resized, in_pad.w / scale,
                              in_pad.h / scale);
        objects_mask = objects_mask_resized;
    }

    // create per-object mask
    for (int i = 0; i < count; i++) {
        Object &obj = objects[i];
        const ncnn::Mat mm = objects_mask.channel(i);
        obj.mask = cv::Mat((int) obj.rect.height, (int) obj.rect.width, CV_8UC1);
        // adjust offset to original unpadded and clip inside object box
        for (int y = 0; y < (int) obj.rect.height; y++) {
            const float *pmm = mm.row((int) (hpad / 2 / scale + obj.rect.y + y)) +
                               (int) (wpad / 2 / scale + obj.rect.x);
            uchar *pmask = obj.mask.ptr<uchar>(y);
            for (int x = 0; x < (int) obj.rect.width; x++) {
                pmask[x] = pmm[x] > mask_threshold ? 1 : 0;
            }
        }
    }

    // sort objects by area
    struct {
        bool operator()(const Object &a, const Object &b) const {
            return a.rect.area() > b.rect.area();
        }
    } objects_area_greater;
    std::sort(objects.begin(), objects.end(), objects_area_greater);

    return objects;
}

