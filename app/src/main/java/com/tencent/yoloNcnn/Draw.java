package com.tencent.yoloNcnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Map;


@RequiresApi(api = Build.VERSION_CODES.O)
public class Draw extends View {

    public Draw(Context context) {
        super(context);
    }

    public Draw(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            画笔属性();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int[][] JOINT_PAIRS = {
            {0, 1}, {1, 3}, {0, 2}, {2, 4}, {5, 6}, {5, 7}, {7, 9}, {6, 8},
            {8, 10}, {5, 11}, {6, 12}, {11, 12}, {11, 13}, {12, 14}, {13, 15}, {14, 16}
    };

    private static final int[] BONE_COLORS = {
            Color.RED, Color.RED, Color.RED, Color.RED,
            Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN,
            Color.GREEN, Color.YELLOW, Color.YELLOW, Color.YELLOW,
            Color.MAGENTA, Color.MAGENTA, Color.MAGENTA, Color.MAGENTA
    };

    public void 清除画布(Canvas 绘制) {
        绘制.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    private static Paint 文本, 目标文本, 目标背景, 方框, 背景, 类名, 类名统计, 统计背景, 统计边框, 骨骼点,台球;

    public void 画笔属性() {
        文本 = new Paint(Paint.ANTI_ALIAS_FLAG);
        文本.setTextSize(30);
        文本.setColor(Color.WHITE);
        目标文本 = new Paint(Paint.ANTI_ALIAS_FLAG);
        目标文本.setTextSize(30);
        目标文本.setColor(Color.WHITE);
        目标文本.setFakeBoldText(true);
        方框 = new Paint();
        方框.setAntiAlias(true);
        方框.setStyle(Paint.Style.STROKE);
        方框.setColor(Color.YELLOW);
        方框.setStrokeWidth(3);
        背景 = new Paint();
        背景.setColor(0x95000000);
        目标背景 = new Paint();
        目标背景.setColor(Color.GREEN);

        类名 = new Paint(Paint.ANTI_ALIAS_FLAG);
        类名.setTextSize(30);
        类名.setColor(0xFF23292e);
        类名统计 = new Paint(Paint.ANTI_ALIAS_FLAG);
        类名统计.setTextSize(30);
        类名统计.setColor(0xFFff5722);
        统计背景 = new Paint();
        统计背景.setColor(Color.WHITE);
        统计边框 = new Paint();
        统计边框.setStyle(Paint.Style.STROKE);
        统计边框.setStrokeWidth(3);
        统计边框.setColor(0xFFcccccc);

        骨骼点 = new Paint();

        台球 = new Paint();
        台球.setAntiAlias(true);
        台球.setStyle(Paint.Style.STROKE);
        台球.setColor(Color.YELLOW);
        台球.setStrokeWidth(3);
    }

    public void 绘制骨骼点(Canvas canvas, YoloV5Ncnn.Obj obj) {
        // 绘制骨骼连接线
        if (obj.keypoints != null && obj.keypoints.size() > 16) {
            for (int i = 0; i < JOINT_PAIRS.length; i++) {
                YoloV5Ncnn.KeyPoint p1 = obj.keypoints.get(JOINT_PAIRS[i][0]);
                YoloV5Ncnn.KeyPoint p2 = obj.keypoints.get(JOINT_PAIRS[i][1]);
                if (p1.prob > 0.2f && p2.prob > 0.2f) {
                    骨骼点.setColor(BONE_COLORS[i]);
                    骨骼点.setStrokeWidth(5);
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, 骨骼点);
                }
            }
            // 绘制关键点
            骨骼点.setColor(Color.CYAN);
            骨骼点.setStyle(Paint.Style.FILL);
            for (YoloV5Ncnn.KeyPoint kp : obj.keypoints) {
                if (kp.prob > 0.2f) {
                    canvas.drawCircle(kp.x, kp.y, 6, 骨骼点);
                }
            }
        }
    }

    public static void 绘制轮廓(Canvas canvas, YoloV5Ncnn.Obj obj) {
        if (obj.mask == null || obj.mask.length == 0) {
            return;
        }

        Paint paint = new Paint();
        paint.setColor(Color.GREEN); // 轮廓线颜色
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3); // 线宽
        paint.setAntiAlias(true);

        Path path = new Path();

        int height = obj.mask.length;
        int width = obj.mask[0].length;

        boolean[][] visited = new boolean[height][width];

        // 遍历所有像素，找到边界像素
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (obj.mask[y][x] && isEdge(obj.mask, x, y)) {
                    // 遍历找到完整的轮廓路径
                    traceContour(obj.mask, x, y, obj.x, obj.y, path, visited);
                }
            }
        }

        // 画出轮廓线
        canvas.drawPath(path, paint);
    }

    /**
     * 判断当前点是否是轮廓点（即其相邻某个方向上是false）
     */
    private static boolean isEdge(boolean[][] mask, int x, int y) {
        int height = mask.length;
        int width = mask[0].length;

        // 方向数组，上下左右
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            // 只要相邻的点是false，当前点就是边界点
            if (nx < 0 || ny < 0 || nx >= width || ny >= height || !mask[ny][nx]) {
                return true;
            }
        }
        return false;
    }

    /**
     * 追踪轮廓，确保轮廓线是连续的
     */
    private static void traceContour(boolean[][] mask, int startX, int startY, float offsetX, float offsetY, Path path, boolean[][] visited) {
        int height = mask.length;
        int width = mask[0].length;

        // 8个方向：上、右上、右、右下、下、左下、左、左上
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};

        int x = startX;
        int y = startY;

        path.moveTo(offsetX + x, offsetY + y);
        visited[y][x] = true;

        while (true) {
            boolean foundNext = false;
            for (int i = 0; i < 8; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (nx >= 0 && ny >= 0 && nx < width && ny < height && mask[ny][nx] && isEdge(mask, nx, ny) && !visited[ny][nx]) {
                    path.lineTo(offsetX + nx, offsetY + ny);
                    visited[ny][nx] = true;
                    x = nx;
                    y = ny;
                    foundNext = true;
                    break;
                }
            }
            if (!foundNext) {
                break; // 轮廓闭合
            }
        }
    }

    public static void 显示推理结果(Canvas 绘制, String text, int screenWidth, float startX, float startY) {
        // 使用Paint来获取文本的Bounds
        Rect textBounds = new Rect();

        float lineHeight = 文本.getTextSize() ; // 计算行高，1.5倍文本大小
        float totalHeight = 0; // 用于计算矩形的总高度

        // 计算总高度
        String[] lines = text.split("\n");
        for (String line : lines) {
            文本.getTextBounds(line, 0, line.length(), textBounds);
            totalHeight += textBounds.height(); // 累加每行的高度
        }

        // 设置矩形的宽度和高度
        int rectWidth = screenWidth; // 矩形宽度固定为屏幕宽度
        int rectHeight = (int) totalHeight;

        // 绘制圆角矩形
        绘制.drawRoundRect(startX, startY - 40, startX + rectWidth, startY + rectHeight, 10, 10, 背景);

        // 绘制文本
        float currentY = startY; // 文本绘制的起始Y位置
        for (String line : lines) {
            // 获取每一行的文本宽度
            文本.getTextBounds(line, 0, line.length(), textBounds);
            // 绘制文本，确保每行不会超出矩形的宽度
            绘制.drawText(line, startX + 10, currentY, 文本); // 每行文本从矩形的左边开始
            currentY += lineHeight; // 换行
        }
    }


    public void 绘制配置(Canvas 绘制) {
        try {
            int startX = 50;  // 起始X坐标
            int startY = 100;  // 起始Y坐标
            if(AiUtil.开启大模型){
                if(null!=AiUtil.推理结果&&0<AiUtil.推理结果.length()){
                    显示推理结果(绘制, AiUtil.推理结果, WindowBean.screenWidth - 150, startX, startY);
                }
            }else {
                if (MainActivity.objects != null && !Param.保存图片) {
                    for (YoloV5Ncnn.Obj obj : MainActivity.objects) {
                        if (obj != null && Param.checkedStates.get(Param.yolo类名集合.indexOf(obj.label))) {
                            if (Param.显示方框) {
                                int x = (int) obj.x, y = (int) obj.y;
                                int w = (int) obj.w, h = (int) obj.h;
                                绘制.drawRect(x, y, x + w, y + h, 方框);
                                if (Param.显示类名) {
                                    String 匹配度 = obj.label + " " + String.format("%.1f", obj.prob * 100) + "%";
                                    绘制.drawText(匹配度, x + 5, y + 30, 目标文本);
                                }
                            }
                            if (Param.显示骨骼) {
                                绘制骨骼点(绘制, obj);
                            }
                            if (Param.显示轮廓) {
                                绘制轮廓(绘制, obj);
                            }
                        }
                    }
                    if (Param.显示记牌器)
                        CardCounter.processRecognizedCards(Arrays.asList(MainActivity.objects));
                }
                if (Param.显示基础信息) {
                    // 构建置信度和速度文本
                    String 绘制文本 = "最低置信：" + String.format("%.1f", Param.probThreshold * 100)
                            + "% 置信：" + String.format("%.1f", Param.识别合格率 * 100)
                            + "% 去重叠：" + String.format("%.1f", Param.nmsThreshold * 100) + "%"
                            + " 输入尺寸：" + Param.targetSize + "px";
                    // 根据文本宽度自适应圆角矩形
                    Rect textBounds = new Rect();
                    文本.getTextBounds(绘制文本, 0, 绘制文本.length(), textBounds);
                    int textWidth = textBounds.width();
                    // 绘制自适应圆角矩形
                    绘制.drawRoundRect(startX, startY, 100 + textWidth, Param.保存图片 ? 190 : 150, 10, 10, 背景);
                    // 绘制置信度文本
                    绘制.drawText(绘制文本, startX + 10, 105, 文本);
                    绘制文本 = String.format("耗时：%.1fms 当前帧数：%.1fFPS 平均帧数：%.1fFPS", Param.耗时, 1000 / Param.耗时, Param.平均帧率);
                    绘制.drawText(绘制文本, startX + 10, 140, 文本);
                    startY = 150;
                    if (Param.保存图片) {
                        绘制文本 = "保存路径：" + Param.保存图片路径 + " 保存数量：" + Param.保存图片数量;
                        绘制.drawText(绘制文本, startX + 10, 175, 文本);
                        startY = 185;
                    }
                }
                if (!Param.识别全屏) {
                    int 中心X = WindowBean.screenWidth / 2;
                    int 中心Y = WindowBean.screenHeight / 2;
                    int 半径X = 0;
                    int 半径Y = 0;
                    if (Param.游戏类型 == 0) {
                        半径X = Param.识别范围X / 2;
                        半径Y = Param.识别范围X / 2;
                    } else {
                        半径X = Param.识别范围X / 2;
                        半径Y = Param.识别范围Y / 2;
                    }
                    绘制.drawRect(中心X - 半径X, 中心Y - 半径Y, 中心X + 半径X, 中心Y + 半径Y, 方框);
                }
                if (Param.显示记牌器) {
                    int cardWidth = 50;  // 牌的宽度Z
                    int cardHeight = 80;  // 牌的高度
                    int spacing = -5;  // 牌之间的间距
                    for (int i = 0; i < Param.sortedLabels.size(); i++) {
                        String label = Param.sortedLabels.get(i);
                        int count = CardCounter.cardCount.getOrDefault(label, 0);
                        int cardX = startX + i * (cardWidth + spacing);
                        int cardY = startY;
                        Path path = new Path();
                        float[] radii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
                        if (i == 0) {
                            radii = new float[]{10, 10, 0, 0, 0, 0, 10, 10};
                        } else if (i == Param.sortedLabels.size() - 1) {
                            radii = new float[]{0, 0, 10, 10, 10, 10, 0, 0};
                            cardWidth -= 5;
                        }
                        RectF cardRect = new RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight);
                        path.addRoundRect(cardRect, radii, Path.Direction.CW);
                        绘制.drawPath(path, 统计背景);
                        绘制.drawPath(path, 统计边框);
                        int textX = cardX + 10;  // 类名的X位置
                        int textY = cardY + cardHeight / 3 + 5;  // 类名的Y位置
                        绘制.drawText(label, textX, textY, 类名);
                        textX = cardX + cardWidth / 4;  // 数量的X位置
                        textY = (int) (cardY + 2 * cardHeight / 2.3);  // 数量的Y位置
                        绘制.drawText(String.valueOf(count), textX, textY, 类名统计);
                    }
                }
                if (Param.显示玩家区域 && null != Param.玩家区域 && !Param.玩家区域.isEmpty()) {
                    for (Map.Entry<String, Integer[]> entry : Param.玩家区域.entrySet()) {
                        Integer[] bounds = entry.getValue();
                        RectF rect = new RectF(bounds[0], bounds[2], bounds[1], bounds[3]);
                        绘制.drawRoundRect(rect, 10, 10, 背景);
                    }
                }
            }
        } catch (Exception e) {
            Log.d("YOLO", "异常：" + e);
        }
    }

    @Override
    public void onDraw(Canvas 画布) {
        super.onDraw(画布);
        清除画布(画布);
        绘制配置(画布);
        postInvalidate();
    }

}
