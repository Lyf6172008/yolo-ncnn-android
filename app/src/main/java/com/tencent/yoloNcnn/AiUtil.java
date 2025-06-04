package com.tencent.yoloNcnn;


import java.util.HashMap;
import java.util.Map;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class AiUtil {
    public static boolean 开启大模型 = false;
    public static boolean 推理状态 = false;
    public static boolean 知识库状态 = false;
    public static String ai配置文件路径 = "/sdcard/yolo/ai_config";
    public static String URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    //知识库查询地址
    public static String QUERY_KNOWLEDGE = "https://open.bigmodel.cn/api/paas/v4/knowledge";
    //知识库创建地址
    public static String ADD_KNOWLEDGE = "https://open.bigmodel.cn/api/paas/v4/knowledge";
    //知识库修改地址
    public static String EDIT_KNOWLEDGE = "https://open.bigmodel.cn/api/paas/v4/knowledge/{id}";
    //知识库删除地址
    public static String DEL_KNOWLEDGE = "https://open.bigmodel.cn/api/paas/v4/knowledge/{id}";
    //知识库文件上传
    public static String QUERY_KNOWLEDGE_FILE = "https://open.bigmodel.cn/api/paas/v4/files";
    //知识库文件上传
    public static String ADD_KNOWLEDGE_FILE = "https://open.bigmodel.cn/api/paas/v4/files";
    //知识库文件上传
    public static String DEL_KNOWLEDGE_FILE = "https://open.bigmodel.cn/api/paas/v4/files/{id}";
    //使用的模型名称
    public static String 语言模型 = "glm-4-plus";
    public static String 知识库ID = "1905080363315879936";
    public static String MODEL = "glm-4v-plus-0111";
    public static String APP_KEY = "9251afd2b6b344c5b6e4d66ec18d32ce.qUfcM3BPXM2Xp87f";
    public static String 多模态提示词 = "提取图中的题目和选项，只提取单选题和多选题" +
            "如果图中没有题目就输出‘未识别到题目内容’，如果图中不是选择题就输出‘目前只支持选择题’。";
    public static String 提示词 = "请根据以下选择题内容，从知识库中检索相关信息，并判断正确选项。\n" +
            "仅返回正确答案，不需要解释或分析过程。严格按照以下规则和格式输出：\n\n" +
            "1. 如果选项前缀为 A、B、C、D，则答案格式：正确答案：A,B,C\n" +
            "2. 如果选项前缀为 1、2、3、4，则答案格式：正确答案：1,2,3\n" +
            "3. 如果选项没有字母或数字标记，则按照顺序自动编号 1,2,3,4，并返回格式：正确答案：1,2,3\n\n" +
            "错误处理：\n" +
            "- 如果题目内容为空，返回：'未识别到题目内容'\n" +
            "- 如果题目不是选择题，返回：'目前只支持选择题'";

    public static String 知识库提示词 = "请根据以下选择题内容，从文档中检索相关信息，并判断正确选项。\n"
            + "知识库内容：'{{knowledge}}'"
            + "题目内容：'{{question}}'"
            + "请严格按照以下格式返回正确答案：\n"
            + "- 字母选项格式：正确答案：A,B,C\n"
            + "- 数字选项格式：正确答案：选项1,选项2\n\n"
            + "如果题目内容为空，返回：'未识别到题目内容'\n"
            + "如果题目不是选择题，返回：'目前只支持选择题'";

    public static String 推理结果;

    public static String 推理(String 图片Base64) {
        try {
            推理状态 = true;
            long startTime = System.currentTimeMillis();
            ImageDescriptionRequest messages = new ImageDescriptionRequest("图片Base64", 知识库状态?多模态提示词:提示词);
            Map<String, String> headers = new HashMap<>();
            Map<String, Object> paramMap = new HashMap<>();
            headers.put("Authorization", APP_KEY);
            paramMap.put("model", MODEL);
            paramMap.put("messages", messages.toString());
            String params = JSONUtil.toJsonStr(paramMap).replaceAll("\\\\", "")
                    .replaceAll("\"\\[", "[").replaceAll("\\]\"", "]")
                    .replaceAll("\"\\[\\{", "[{").replaceAll("\\}\\]\"", "\\}\\]")
                    .replaceAll("图片Base64", 图片Base64).replaceAll("\r", "").replaceAll("\n", "");
            推理结果 = "提取题目中，请稍等...";
            推理结果 = HttpUtil.post(URL, params, headers);
            if (null != 推理结果) {
                if (推理结果.indexOf("{error") == -1) {
                    JSONObject jsonObject = JSONUtil.parseObj(推理结果);
                    String content = jsonObject.getByPath("choices[0].message.content", String.class);
                    int totalTokens = jsonObject.getByPath("usage.total_tokens", Integer.class);
                    if (content.indexOf("未识别到题目内容") == -1 && content.indexOf("目前只支持选择题") == -1 && null != 知识库ID && 知识库状态) {
                        推理结果 = "分析中，请稍等...";
                        Map<String, Object> returnData = InvokeKnowledgeIdReturnString(提示词 + "\n当前题目内容:\n" + content, 知识库ID, 知识库提示词);
                        if (!returnData.isEmpty()) {
                            推理结果 = "耗时：" + (System.currentTimeMillis() - startTime) + "ms 消耗tokens：" + (totalTokens + (int) returnData.get("totalTokens"))
                                    + "  本次费用：" + (calculateCost(totalTokens, 0.004) + calculateCost((int) returnData.get("totalTokens"), 0.05)) + "元\n"
                                    + String.valueOf(returnData.get("content")).replaceAll("\n\n", "\n");
                        } else {
                            推理结果 = String.valueOf(returnData.get("content"));
                        }
                    } else {
                        推理结果 = "耗时：" + (System.currentTimeMillis() - startTime) + "ms 消耗tokens：" + totalTokens + "  本次费用：" + calculateCost(totalTokens, 0.004) + "元\n" + content.replaceAll("\n\n", "\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            推理状态 = false;
            return 推理结果;
        }
    }

    public static double calculateCost(int totalTokens, double costPerThousand) {
        return (totalTokens / 1000.0) * costPerThousand;
    }

    public static void 保存配置() {
        FileUtils.saveFile(ai配置文件路径, MODEL + "," + APP_KEY + "," + 提示词 + "," + 知识库ID);
    }

    public static void 刷新配置() {
        try {
            String ai_config = FileUtils.readFileContent(ai配置文件路径);
            if (null != ai_config && 10 < ai_config.length()) {
                String[] ai_config_arr = ai_config.split(",");
                MODEL = ai_config_arr[0];
                APP_KEY = ai_config_arr[1];
                提示词 = ai_config_arr[2];
                知识库ID = ai_config_arr[3];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 同步调用带知识库id，只返回结果
     */
    public static Map<String, Object> InvokeKnowledgeIdReturnString(String message, String KnowledgeId, String 知识库提示词) {
        Map<String, Object> returnData = new HashMap<>();
        try {
            if (null == message || null == KnowledgeId)
                return returnData;
            ChatRequest request = ChatRequest.create(语言模型)
                    .addUserMessage(message)
                    .addRetrievalTool(KnowledgeId, 知识库提示词);
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", APP_KEY);
            String params = request.toString().replaceAll("\\\\", "")
                    .replaceAll("\"\\[", "[").replaceAll("\\]\"", "]")
                    .replaceAll("\"\\[\\{", "[{").replaceAll("\\}\\]\"", "\\}\\]")
                    .replaceAll("\r", "").replaceAll("\n", "");
            String data = HttpUtil.post(URL, params, headers);
            if (null != data) {
                if (data.indexOf("error") == -1) {
                    JSONObject jsonObject = JSONUtil.parseObj(data);
                    String content = jsonObject.getByPath("choices[0].message.content", String.class);
                    int totalTokens = jsonObject.getByPath("usage.total_tokens", Integer.class);
                    returnData.put("content", content);
                    returnData.put("totalTokens", totalTokens);
                    returnData.put("cost", calculateCost(totalTokens, 0.004));
                }
            } else {
                returnData.put("content", data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return returnData;
        }
    }

    /**
     * 查询知识库
     */
    public Map<String, Object> queryKnowledge(Integer pageNum, Integer pageSize) {
        String data;
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> paramMap = new HashMap<>();
        headers.put("Authorization", APP_KEY);
        paramMap.put("page", pageNum);
        paramMap.put("size", pageSize);
        data = HttpUtil.get(QUERY_KNOWLEDGE, paramMap, headers);
        Map<String, Object> dataMap = JSONUtil.toBean(data, Map.class);
        return dataMap;
    }

    /**
     * 创建知识库
     */
    public String addKnowledge(KnowledgeBo bo) {
        Map<String, Object> dataMap = new HashMap<>();
        String name = bo.getName();
        String description = bo.getDescription();
        String data;
        if (null == name)
            return "知识库名称不能为空";
        if (null != name && 20 < name.length())
            return "知识库名称长度不能大于20字";
        if (null != description && 100 < description.length())
            return "知识库描述长度不能大于100字";
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> paramMap = new HashMap<>();
        headers.put("Authorization", APP_KEY);
        paramMap.put("embedding_id", 3);
        paramMap.put("name", name);
        paramMap.put("description", description);
        data = HttpUtil.post(ADD_KNOWLEDGE, JSONUtil.toJsonStr(paramMap), headers);
        return data;
    }

    /**
     * 编辑知识库
     */
    public String editKnowledge(KnowledgeBo bo) {
        String knowledge_id = bo.getId();
        String name = bo.getName();
        String description = bo.getDescription();
        Map<String, Object> dataMap = new HashMap<>();
        String data;
        if (null == knowledge_id)
            return "知识库ID不能为空";
        if (null != name && 20 < name.length())
            return "知识库名称长度不能大于20字";
        if (null != description && 100 < description.length())
            return "知识库描述长度不能大于100字";
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> paramMap = new HashMap<>();
        headers.put("Authorization", APP_KEY);
        paramMap.put("knowledge_id", knowledge_id);
        paramMap.put("embedding_id", 3);
        paramMap.put("name", name);
        paramMap.put("description", description);
        data = HttpUtil.put(EDIT_KNOWLEDGE.replaceAll("\\{id\\}", knowledge_id), paramMap, headers);
        if (null == data || "".equals(data) || 0 == data.length()) {
            return "编辑成功";
        } else {
            dataMap = JSONUtil.toBean(data, Map.class);
            dataMap = JSONUtil.toBean(String.valueOf(dataMap.get("error")), Map.class);
            return "编辑失败" + String.valueOf(dataMap.get("message"));
        }
    }

    /**
     * 删除知识库
     */
    public String delKnowledge(String knowledge_id) {
        String data;
        if (null == knowledge_id)
            return "知识库ID不能为空";
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> paramMap = new HashMap<>();
        headers.put("Authorization", APP_KEY);
        paramMap.put("knowledge_id", knowledge_id);
        data = HttpUtil.del(DEL_KNOWLEDGE.replaceAll("\\{id\\}", knowledge_id), paramMap, headers);
        Map<String, Object> dataMap = new HashMap<>();
        if (null == data || "".equals(data) || 0 == data.length()) {
            return "删除成功";
        } else {
            dataMap = JSONUtil.toBean(data, Map.class);
            dataMap = JSONUtil.toBean(String.valueOf(dataMap.get("error")), Map.class);
            return "删除失败" + String.valueOf(dataMap.get("message"));
        }
    }

}
