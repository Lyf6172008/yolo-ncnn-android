package com.tencent.yoloNcnn;

/**
 * 知识库实体
 */
public class KnowledgeBo {
    /**
     * id
     */
    private String id;
    /**
     * 知识库名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;

    /**
     * 知识库绑定的向量化模型，目前仅支持embedding-2。
     * 3:表示为embedding-2
     */
    private int embeddingId = 3;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(int embeddingId) {
        this.embeddingId = embeddingId;
    }
}
