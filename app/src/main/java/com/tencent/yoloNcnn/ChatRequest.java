package com.tencent.yoloNcnn;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.json.JSONUtil;

/**
 * 大模型对话请求参数（精简版，仅含 model/messages/tools）
 */
public class ChatRequest {

    private String model;
    private List<Message> messages = new ArrayList<>();
    private List<Tool> tools = new ArrayList<>();

    // 快捷创建方法
    public static ChatRequest create(String model) {
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        return request;
    }

    // 添加用户消息
    public ChatRequest addUserMessage(String content) {
        this.messages.add(new Message("user", content));
        return this;
    }

    // 添加知识库工具
    public ChatRequest addRetrievalTool(String knowledgeId, String promptTemplate) {
        this.tools.add(new Tool("retrieval", new Retrieval(knowledgeId, promptTemplate)));
        return this;
    }

    //================ 内部类 ================//
    public static class Message {
        private String role;  // "system"|"user"|"assistant"
        private String content;

        public Message() {}
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // Getter/Setter
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class Tool {
        private String type;  // "retrieval"|"function"|"web_search"
        private Retrieval retrieval;

        public Tool() {}
        public Tool(String type, Retrieval retrieval) {
            this.type = type;
            this.retrieval = retrieval;
        }

        // Getter/Setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Retrieval getRetrieval() { return retrieval; }
        public void setRetrieval(Retrieval retrieval) { this.retrieval = retrieval; }
    }

    public static class Retrieval {
        private String knowledgeId;
        private String promptTemplate;

        public Retrieval() {}
        public Retrieval(String knowledgeId, String promptTemplate) {
            this.knowledgeId = knowledgeId;
            this.promptTemplate = promptTemplate;
        }

        // Getter/Setter
        public String getKnowledgeId() { return knowledgeId; }
        public void setKnowledgeId(String knowledgeId) { this.knowledgeId = knowledgeId; }
        public String getPromptTemplate() { return promptTemplate; }
        public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }
    }

    //================ 主类Getter/Setter ================//
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public List<Tool> getTools() { return tools; }
    public void setTools(List<Tool> tools) { this.tools = tools; }

    @Override
    public String toString() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // 添加model字段
        json.append("\"model\":\"").append(escapeJson(model)).append("\",");

        // 添加messages数组
        json.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) json.append(",");
            json.append("{\"role\":\"").append(escapeJson(msg.role)).append("\",");
            json.append("\"content\":\"").append(escapeJson(msg.content)).append("\"}");
        }
        json.append("],");

        // 添加tools数组
        json.append("\"tools\":[");
        for (int i = 0; i < tools.size(); i++) {
            Tool tool = tools.get(i);
            if (i > 0) json.append(",");
            json.append("{\"type\":\"").append(escapeJson(tool.type)).append("\",");
            json.append("\"retrieval\":{");
            json.append("\"knowledge_id\":\"").append(escapeJson(tool.retrieval.knowledgeId)).append("\",");
            json.append("\"prompt_template\":\"").append(escapeJson(tool.retrieval.promptTemplate)).append("\"}}");
        }
        json.append("]");

        json.append("}");
        return json.toString();
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}