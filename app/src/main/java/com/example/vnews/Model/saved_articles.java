package com.example.vnews.Model;

public class saved_articles {
    private String id;
    private String articleId;
    private String userId;
    private long savedAt;
    
    // Các trường từ RSS
    private String title;
    private String description;
    private String pubDate;
    private String link;
    private String imageUrl;
    private String guid;

    public saved_articles() {
    }

    public saved_articles(String id, String articleId, String userId, long savedAt) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.savedAt = savedAt;
    }
    
    // Constructor đầy đủ với thông tin RSS
    public saved_articles(String id, String articleId, String userId, long savedAt,
                         String title, String description, String pubDate, 
                         String link, String imageUrl, String guid) {
        this.id = id;
        this.articleId = articleId;
        this.userId = userId;
        this.savedAt = savedAt;
        this.title = title;
        this.description = description;
        this.pubDate = pubDate;
        this.link = link;
        this.imageUrl = imageUrl;
        this.guid = guid;
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
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getPubDate() {
        return pubDate;
    }
    
    public String getLink() {
        return link;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public String getGuid() {
        return guid;
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
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }
    
    public void setLink(String link) {
        this.link = link;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public void setGuid(String guid) {
        this.guid = guid;
    }
    
    // Hàm tiện ích để trích xuất nội dung sạch từ CDATA description
    public String getCleanDescription() {
        if (description == null) return "";
        
        // Loại bỏ CDATA và các thẻ HTML
        String clean = description.replaceAll("<!\\[CDATA\\[", "")
                                  .replaceAll("\\]\\]>", "")
                                  .replaceAll("<[^>]*>", "");
        return clean.trim();
    }
    
    // Hàm tiện ích để trích xuất URL hình ảnh từ thẻ img trong description
    public String extractImageUrlFromDescription() {
        if (description == null) return "";
        
        // Tìm src= trong thẻ img
        if (description.contains("<img src=\"")) {
            int start = description.indexOf("<img src=\"") + 10;
            int end = description.indexOf("\"", start);
            if (start > 0 && end > start) {
                return description.substring(start, end);
            }
        }
        return "";
    }
}