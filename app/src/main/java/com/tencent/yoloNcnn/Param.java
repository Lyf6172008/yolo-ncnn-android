package com.tencent.yoloNcnn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Param {
    //    public static String 目标包名 = "com.tencent.tmgp.cf";
    public static String 目标包名 = "com.bilibili.app.in";

    public static boolean 使用远程服务器 = false;
    public static boolean 服务器连接状态 = false;
    public static String 服务器地址 = "wss://u310863-b225-2776c3e2.westc.gpuhub.com:8443";
    //    public static String 服务器地址 = "u310863-a86d-2005451a.westb.seetacloud.com";
    public static int 服务器端口 = 8443;
    public static boolean JNI初始化 = false;
    public static boolean YOLO初始化 = false;
    public static boolean 使用GPU = true;
    public static String 模型路径 = "/sdcard/yolo/ncnn/";
    public static String ncnn文件地址 = "/sdcard/yolo/ncnn/";
    public static String tflite文件地址 = "/sdcard/yolo/tflite/";
    public static String 推理框架="ncnn";//支持ncnn 和 tflite
    public static String yolo版本="v5";//支持v5,v8
    public static String 模型地址;
    public static String 模型参数地址;
    public static String 模型类名地址;
    public static String 斗地主类名 = "K,4,9,8,J,3,6,Q,7,A,5,10,小,大,2";
    public static String yolo类名 = "人类,自行车,汽车,摩托车,飞机,公共汽车,火车,卡车,船,交通灯,消防栓,停车标志,停车计时器,长椅,鸟,猫,狗,马,羊,牛,大象,熊,斑马,长颈鹿,背包,雨伞,手提包,领带,手提箱,飞盘,滑雪板,滑雪板,运动球,风筝,棒球棒,棒球手套,滑板,冲浪板,网球拍,瓶子,酒杯,杯子,叉子,刀,勺子,碗,香蕉,苹果,三明治,橙子,西兰花,胡萝卜,热狗,披萨,甜甜圈,蛋糕,椅子,沙发,盆栽,床,餐桌,卫生间,电视,笔记本电脑,鼠标,遥控器,键盘,手机,微波炉,烤箱,烤面包机,水槽,冰箱,书,钟,花瓶,剪刀,泰迪熊,吹风机,牙刷";
    public static List<String> yolo类名集合 = new ArrayList<>();
    public static List<Boolean> checkedStates = new ArrayList<>();
    public static Map<String, Integer> labelCountMap = new HashMap<>();
    public static List<String> FPS类型 = Arrays.asList("fps", "FPS", "cfm", "CFM", "cs", "CS");
    public static List<String> 棋牌类型 = Arrays.asList("棋牌", "斗地主");
    public static List<String> 台球类型 = Arrays.asList("台球");
    public static List<String> sortedLabels = Arrays.asList("大", "小", "2", "A", "K", "Q", "J", "10", "9", "8", "7", "6", "5", "4", "3");
    public static double 识别合格率 = 0.5;
    public static int targetSize = 320;
    public static double probThreshold = 0.1;
    public static double nmsThreshold = 0.1;
    public static List<Double> 帧率存储 = new ArrayList<>();
    public static double 平均帧率 = 0;
    public static int 平均帧率计算次数 = 50;

    // 0 - fps  1-斗地主  2-台球
    public static int 游戏类型 = 0;
    public static boolean 是否有ROOT = false;
    public static boolean 自动打开游戏 = false;
    public static boolean 开启触摸 = false;
    public static boolean 开始识别 = false;
    public static boolean 横屏 = false;
    public static boolean 显示方框 = true;
    public static boolean 显示骨骼 = true;
    public static boolean 显示轮廓 = true;
    public static boolean 保存图片 = false;
    public static String 保存图片路径 = "/sdcard/yoloSave/";
    public static int 保存图片数量 = 0;
    public static int 识别速度 = 1;
    public static boolean 识别全屏 = true;
    public static boolean 显示类名 = true;
    public static int 识别范围X = 500;
    public static int 识别范围Y = 400;
    public static int 棋牌识别范围X = 950;
    public static int 棋牌识别范围Y = 400;
    public static int 信息刷新速度 = 800;
    public static float 耗时 = 0;
    public static float 截图耗时 = 0;
    public static float 裁剪耗时 = 0;
    public static float 推理耗时 = 0;
    public static boolean 显示基础信息 = true;
    public static boolean 显示记牌器 = false;
    public static boolean 显示玩家区域 = false;
    public static Map<String, Integer[]> 玩家区域 = new HashMap<>();


    public static String 使用说明 = "输入限制in0,输出out0,1,2\n" +
            "模型精度说明：n < s < m < l\n" +
            "模型速度说明：n > s > m > l\n" +
            "手机处理器越好识别的越快\n" +
            "性能消耗巨大发热发烫属于正常现象";

    public static void 更新平均帧率(double fps) {
        if (平均帧率计算次数 < 帧率存储.size())
            帧率存储.remove(0);
        帧率存储.add(fps);
        double allFPS = 0;
        for (Double f : 帧率存储) {
            allFPS += f;
        }
        平均帧率 = allFPS / 帧率存储.size();
    }
}