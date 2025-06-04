package com.tencent.yoloNcnn;

import java.util.HashMap;
import java.util.Map;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;

/**
 * http请求
 */
public class HttpUtil {

    /**
     * get请求
     *
     * @param url         地址
     * @param queryParams 参数
     * @param headers     头参数
     * @return 返回字符串
     */
    public static String get(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        String data = null;
        try {
            if (null == queryParams) queryParams = new HashMap<>();
            if (null == headers) headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            data = HttpRequest.get(url).form(queryParams).addHeaders(headers).execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * post请求
     *
     * @param url         地址
     * @param queryParams 参数
     * @param headers     头参数
     * @return 返回字符串
     */
    public static String post(String url, String queryParams, Map<String, String> headers) {
        String data = null;
        try {
            if (null == headers) headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            System.out.println("请求地址：" + url);
            System.out.println("请求头：" + JSONUtil.toJsonStr(headers));
            System.out.println("请求参数：" + queryParams);
            data = HttpRequest.post(url).addHeaders(headers).body(queryParams).execute().body();
            System.out.println("请求结果："+data);
        } catch (Exception e) {
            System.out.println("请求异常："+e);
        }
        return data;
    }

    /**
     * put请求
     *
     * @param url         地址
     * @param queryParams 参数
     * @param headers     头参数
     * @return 返回字符串
     */
    public static String put(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        String data = null;
        try {
            if (null == queryParams) queryParams = new HashMap<>();
            if (null == headers) headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            data = HttpRequest.put(url).addHeaders(headers).body(JSONUtil.toJsonStr(queryParams)).execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * del请求
     *
     * @param url         地址
     * @param queryParams 参数
     * @param headers     头参数
     * @return 返回字符串
     */
    public static String del(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        String data = null;
        try {
            if (null == queryParams) queryParams = new HashMap<>();
            if (null == headers) headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            data = HttpRequest.delete(url).addHeaders(headers).body(JSONUtil.toJsonStr(queryParams)).execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}
