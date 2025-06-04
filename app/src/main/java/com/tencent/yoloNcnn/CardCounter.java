package com.tencent.yoloNcnn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardCounter {
    public static Map<String, Map<String, Integer>> cardCountByArea = new HashMap<>();
    public static Map<String, List<String>> previousCardsByArea = new HashMap<>();
    public static Map<String, Integer> cardCount = new HashMap<>();  // 用于显示合并后的总剩余牌数
    public static final int STABILITY_COUNT = 3;
    public static Map<String, List<List<String>>> recentCardGroupsByArea = new HashMap<>();

    // 初始化每个区域和总的牌数
    public static void initialize() {
        cardCountByArea.clear();
        previousCardsByArea.clear();
        recentCardGroupsByArea.clear();

        // 初始化玩家区域
        Param.玩家区域.put("player1", new Integer[]{700, 1125, 400, 600});
        Param.玩家区域.put("player2", new Integer[]{1225, 1650, 400, 600});
        Param.玩家区域.put("player3", new Integer[]{700, 1650, 600, 750});

        initializeArea("player1");
        initializeArea("player2");
        initializeArea("player3");

        initCardCount();
    }

    public static void initCardCount() {
        cardCount.clear();
        cardCount.put("大", 1);   // 大王
        cardCount.put("小", 1);   // 小王
        for (int i = 2; i < Param.sortedLabels.size(); i++) {
            cardCount.put(Param.sortedLabels.get(i), 4);
        }

    }

    // 初始化单个区域的牌数
    public static void initializeArea(String area) {
        Map<String, Integer> areaCardCount = new HashMap<>();
        areaCardCount.put("大", 1);
        areaCardCount.put("小", 1);
        for (int i = 2; i < Param.sortedLabels.size(); i++) {
            areaCardCount.put(Param.sortedLabels.get(i), 4);
        }
        cardCountByArea.put(area, areaCardCount);
        previousCardsByArea.put(area, new ArrayList<>());
        recentCardGroupsByArea.put(area, new ArrayList<>());
    }

    // 判断牌是否仍在该区域内
    private static boolean isCardInArea(String area, YoloV5Ncnn.Obj obj) {
        Integer[] bounds = Param.玩家区域.get(area);
        return obj.x >= bounds[0] && obj.x <= bounds[1] && obj.y >= bounds[2] && obj.y <= bounds[3];
    }

    // 更新 `processRecognizedCards` 方法，加入追踪
    public static void processRecognizedCards(List<YoloV5Ncnn.Obj> objects) {
        if (objects == null || objects.isEmpty()) return;

        Map<String, List<String>> cardsByArea = new HashMap<>();
        cardsByArea.put("player1", new ArrayList<>());
        cardsByArea.put("player2", new ArrayList<>());
        cardsByArea.put("player3", new ArrayList<>());

        // 根据牌的坐标划分区域，加入区域追踪
        for (YoloV5Ncnn.Obj obj : objects) {
            if (obj != null) {
                String area = determineArea(obj.x, obj.y);
                if (cardsByArea.containsKey(area) && isCardInArea(area, obj)) {
                    cardsByArea.get(area).add(obj.label);
                }
            }
        }

        for (String area : cardsByArea.keySet()) {
            List<String> areaCards = cardsByArea.get(area);
            areaCards.sort(Comparator.comparingInt(CardCounter::getCardOrder));
            processAreaCards(area, areaCards);
        }

        if (cardCount.values().stream().allMatch(count -> count == 0)) initialize(); // 重置所有区域的牌数量
    }

    // 判断牌属于哪个区域
    private static String determineArea(float x, float y) {
        for (Map.Entry<String, Integer[]> entry : Param.玩家区域.entrySet()) {
            String player = entry.getKey();
            Integer[] bounds = entry.getValue();
            // 判断 x 和 y 是否在该玩家的区域范围内
            if (x >= bounds[0] && x <= bounds[1] && y >= bounds[2] && y <= bounds[3])
                return player; // 返回玩家的区域标识
        }
        return "unknown"; // 如果不在任何玩家区域内
    }

    // 处理每个区域的牌组
    public static void processAreaCards(String area, List<String> recognizedCards) {
        List<List<String>> recentCards = recentCardGroupsByArea.get(area);
        List<String> previousCards = previousCardsByArea.get(area);

        // 添加当前识别结果到最近的识别组
        recentCards.add(new ArrayList<>(recognizedCards));
        if (recentCards.size() > STABILITY_COUNT)
            recentCards.remove(0);

        // 检查是否稳定
        if (isStable(recentCards) && hasCardListChanged(recognizedCards, previousCards)) {
            previousCards.clear();
            previousCards.addAll(recognizedCards);
            reduceCardCounts(area, recognizedCards);
        }
    }

    // 判断最近的识别结果是否稳定
    public static boolean isStable(List<List<String>> recentGroups) {
        for (List<String> group : recentGroups) {
            if (!group.equals(recentGroups.get(0)))
                return false;
        }
        return true;
    }

    // 判断牌组是否发生变化
    public static boolean hasCardListChanged(List<String> current, List<String> previous) {
        if (current.size() != previous.size())
            return true;
        for (String card : current) {
            if (Collections.frequency(current, card) != Collections.frequency(previous, card))
                return true;
        }
        return false;
    }

    // 减少指定区域的牌数量
    public static void reduceCardCounts(String area, List<String> recognizedCards) {
        Map<String, Integer> areaCardCount = cardCountByArea.get(area);
        for (String card : recognizedCards) {
            // 仅在区域内的牌数大于0时，减少牌数
            if (areaCardCount.containsKey(card) && areaCardCount.get(card) > 0) {
                areaCardCount.put(card, areaCardCount.get(card) - 1);
                // 更新全局牌计数，确保全局牌计数不会变为负数
                if (cardCount.containsKey(card) && cardCount.get(card) > 0) {
                    cardCount.put(card, cardCount.get(card) - 1);
                }
            }
        }
    }

    // 获取卡牌的排序顺序
    public static int getCardOrder(String card) {
        switch (card) {
            case "大":
                return 0;
            case "小":
                return 1;
            case "2":
                return 2;
            case "A":
                return 3;
            case "K":
                return 4;
            case "Q":
                return 5;
            case "J":
                return 6;
            case "10":
                return 7;
            case "9":
                return 8;
            case "8":
                return 9;
            case "7":
                return 10;
            case "6":
                return 11;
            case "5":
                return 12;
            case "4":
                return 13;
            case "3":
                return 14;
            default:
                return Integer.MAX_VALUE;
        }
    }

}
