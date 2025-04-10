package com.example.vnnews.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "news")
public class News {
    @PrimaryKey
    private long id;
    private String title;
    private String description;
    private String content;
    private String imageUrl;
    private String category;
    private Date publishedDate;
    private String source;
    private String author;
    private int views;
    private boolean featured;

    public News(long id, String title, String description, String content, String imageUrl,
                String category, Date publishedDate, String source, String author, int views,
                boolean featured) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.imageUrl = imageUrl;
        this.category = category;
        this.publishedDate = publishedDate;
        this.source = source;
        this.author = author;
        this.views = views;
        this.featured = featured;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getPublishedDate() { return publishedDate; }
    public void setPublishedDate(Date publishedDate) { this.publishedDate = publishedDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
} 