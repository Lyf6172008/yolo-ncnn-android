package com.tencent.yoloNcnn;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ZhiPuMessageContent {
    private final List<ContentItem> contents;

    public ZhiPuMessageContent() {
        this.contents = new ArrayList<>();
    }

    // 添加内容的方法（保持原子性）
    public void addImageContent(String imageUrl) {
        if (imageUrl == null) throw new IllegalArgumentException("Image URL cannot be null");
        contents.add(new ContentItem("image_url", new ImageUrl(imageUrl), null));
    }

    public void addTextContent(String text) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null");
        contents.add(new ContentItem("text", null, text));
    }

    // 返回不可变列表防止外部修改
    public List<ContentItem> getContents() {
        return Collections.unmodifiableList(contents);
    }

    // 静态内部类 + 不可变设计（可选）
    public static class ContentItem {
        @SerializedName("type")
        private final String type;

        @SerializedName("image_url")
        private final ImageUrl imageUrl;

        @SerializedName("text")
        private final String text;

        // 全参构造 + 参数校验
        public ContentItem(String type, ImageUrl imageUrl, String text) {
            this.type = Objects.requireNonNull(type, "Type cannot be null");
            this.imageUrl = imageUrl; // 允许为null
            this.text = text;         // 允许为null
        }

        // Getter方法（无Setter保证不可变）
        public String getType() { return type; }
        public ImageUrl getImageUrl() { return imageUrl != null ? imageUrl : new ImageUrl(""); }
        public String getText() { return text != null ? text : ""; }
    }

    // 静态内部类 + 值对象模式
    public static class ImageUrl {
        @SerializedName("url")
        private final String url;

        public ImageUrl(String url) {
            this.url = Objects.requireNonNull(url, "URL cannot be null");
        }

        public String getUrl() { return url; }

        // 可重写equals/hashCode（如需比较对象）
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageUrl imageUrl = (ImageUrl) o;
            return url.equals(imageUrl.url);
        }
    }
}