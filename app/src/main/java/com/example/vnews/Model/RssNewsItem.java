package com.example.vnews.Model;

public class RssNewsItem {
    private String title;
    private String description;
    private String pubDate;
    private String link;
    private String imageUrl;
    private boolean isFromRss;
    private String guid;

    public RssNewsItem() {
        this.isFromRss = false;
    }

    public RssNewsItem(String title, String description, String pubDate, String link, String imageUrl, String guid) {
        this.title = title;
        this.description = description;
        this.pubDate = pubDate;
        this.link = link;
        this.imageUrl = imageUrl;
        this.guid = guid;
        this.isFromRss = true;
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

    public void setFromRss(boolean fromRss) {
        this.isFromRss = fromRss;
    }
    
    public String getGuid() {
        return guid;
    }
    
    public void setGuid(String guid) {
        this.guid = guid;
    }

    // Extract clean description (remove HTML tags and CDATA)
    public String getCleanDescription() {
        if (description == null) return "";
        // Remove CDATA markers
        String clean = description.replaceAll("<!\\[CDATA\\[", "")
                                   .replaceAll("\\]\\]>", "");
        // Remove any HTML tags and extract only text
        return clean.replaceAll("\\<.*?\\>", "").trim();
    }
    
    // Extract image URL from description if not already set
    public String getEffectiveImageUrl() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }
        
        // Try to extract from description if it contains an img tag
        if (description != null && description.contains("<img")) {
            try {
                // First attempt with standard regex
                int srcIndex = description.indexOf("src=\"");
                if (srcIndex >= 0) {
                    int startIndex = srcIndex + 5;
                    int endIndex = description.indexOf("\"", startIndex);
                    if (endIndex > startIndex) {
                        String extractedUrl = description.substring(startIndex, endIndex);
                        if (!extractedUrl.isEmpty()) {
                            return extractedUrl;
                        }
                    }
                }
                
                // Second attempt with Jsoup for better HTML parsing
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(description);
                org.jsoup.nodes.Element imgElement = doc.select("img").first();
                if (imgElement != null) {
                    String jsoupImageUrl = imgElement.attr("src");
                    if (jsoupImageUrl != null && !jsoupImageUrl.isEmpty()) {
                        return jsoupImageUrl;
                    }
                }
                
                // Third attempt for data-src or data-original attributes (lazy loading)
                org.jsoup.nodes.Element imgWithDataSrc = doc.select("img[data-src]").first();
                if (imgWithDataSrc != null) {
                    String dataSrc = imgWithDataSrc.attr("data-src");
                    if (dataSrc != null && !dataSrc.isEmpty()) {
                        return dataSrc;
                    }
                }
                
                // Fourth attempt for VnExpress specific picture tag
                org.jsoup.nodes.Element pictureElement = doc.select("picture source").first();
                if (pictureElement != null) {
                    String srcset = pictureElement.attr("srcset");
                    if (srcset != null && !srcset.isEmpty()) {
                        // Get the first URL from srcset (format might be "url size, url size, ...")
                        int spaceIndex = srcset.indexOf(" ");
                        if (spaceIndex > 0) {
                            return srcset.substring(0, spaceIndex);
                        }
                        return srcset;
                    }
                }
            } catch (Exception e) {
                // If any exception occurs, return empty string
                android.util.Log.e("RssNewsItem", "Error extracting image URL", e);
            }
        }
        
        return "";
    }
} 