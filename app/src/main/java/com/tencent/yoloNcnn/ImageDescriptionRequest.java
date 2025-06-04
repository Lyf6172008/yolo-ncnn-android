package com.tencent.yoloNcnn;

import java.util.ArrayList;
import java.util.List;

public class ImageDescriptionRequest {
    private String role = "user";
    private List<Content> content;

    public ImageDescriptionRequest(String imageBase64) {
        this(imageBase64, "分析图中的题目和选项给出每个选项的正确率");
    }

    public ImageDescriptionRequest(String imageBase64, String promptText) {
        this.content = new ArrayList<>();
        this.content.add(new TextContent(promptText));
        this.content.add(new ImageContent(imageBase64));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[{");
        sb.append("\"role\":\"").append(role).append("\",");
        sb.append("\"content\":[");

        for (int i = 0; i < content.size(); i++) {
            sb.append(content.get(i));
            if (i < content.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]}]");
        return sb.toString();
    }

    public static abstract class Content {
        protected String type;

        public Content(String type) {
            this.type = type;
        }
    }

    public static class ImageContent extends Content {
        private ImageUrl image_url;

        public ImageContent(String imageBase64) {
            super("image_url");
            this.image_url = new ImageUrl(imageBase64);
        }

        @Override
        public String toString() {
            return "{\"type\":\"" + type + "\",\"image_url\":{\"url\":\"" + image_url.url + "\"}}";
        }

        private static class ImageUrl {
            String url;

            public ImageUrl(String imageBase64) {
//                this.url = "data:image/jpeg;base64," + imageBase64;
                this.url = imageBase64;
            }
        }
    }

    public static class TextContent extends Content {
        private String text;

        public TextContent(String text) {
            super("text");
            this.text = text;
        }

        @Override
        public String toString() {
            return "{\"type\":\"" + type + "\",\"text\":\"" + text + "\"}";
        }
    }
}