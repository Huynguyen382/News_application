package com.example.vnnews.model;

public class NewsItem {
    private String title;
    private String description;
    private String imageUrl;
    private String link;
    private String pubDate;

    public NewsItem(String title, String description, String imageUrl, String link, String pubDate) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.link = link;
        this.pubDate = pubDate;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }
} 