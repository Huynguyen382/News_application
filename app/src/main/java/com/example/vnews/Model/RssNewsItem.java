package com.example.vnews.Model;

public class RssNewsItem {
    private String title;
    private String description;
    private String pubDate;
    private String link;
    private String imageUrl;
    private boolean isFromRss;

    public RssNewsItem() {
        this.isFromRss = false;
    }

    public RssNewsItem(String title, String description, String pubDate, String link, String imageUrl) {
        this.title = title;
        this.description = description;
        this.pubDate = pubDate;
        this.link = link;
        this.imageUrl = imageUrl;
        this.isFromRss = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isFromRss() {
        return isFromRss;
    }

    public void setIsFromRss(boolean isFromRss) {
        this.isFromRss = isFromRss;
    }

    // Extract clean description (remove HTML tags)
    public String getCleanDescription() {
        if (description == null) return "";
        // Remove any HTML tags and extract only text
        return description.replaceAll("\\<.*?\\>", "").trim();
    }
} 