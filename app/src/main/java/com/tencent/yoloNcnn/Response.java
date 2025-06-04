package com.tencent.yoloNcnn;

import java.util.List;

public class Response {
    private String status;          // 表示响应的状态（例如 "success"）
    private String end_marker;          // 表示响应的状态（例如 "success"）
    private List<Detection> data;   // 包含检测结果的列表

    public String getEnd_marker() {
        return end_marker;
    }

    public void setEnd_marker(String end_marker) {
        this.end_marker = end_marker;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Detection> getData() {
        return data;
    }

    public void setData(List<Detection> data) {
        this.data = data;
    }
}
