package com.example.vnews.Model;

public class saved_articles {
    private String id;
    private String articleId;
    private String userId;
    private long savedAt;

    public saved_articles() {
    }

    public saved_articles(String id, String articleId, String userId, long savedAt) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.savedAt = savedAt;
    }

    public String getId() {
        return id;
    }

    public String getArticleId() {
        return articleId;
    }

    public String getUserId() {
        return userId;
    }

    public long getSavedAt() {
        return savedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setSavedAt(long savedAt) {
        this.savedAt = savedAt;
    }
}