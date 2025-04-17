package com.example.vnews.Utils;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Utility class for scraping article content from VnExpress
 */
public class ArticleScraper {
    
    private static final String TAG = "ArticleScraper";
    
    /**
     * Get full article content from a VnExpress URL
     * 
     * @param url VnExpress article URL
     * @return HTML formatted content of the article
     */
    public static String getArticleContent(String url) {
        try {
            Log.d(TAG, "Fetching article content from: " + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            // Get article title for debugging
            Element titleElement = doc.selectFirst("h1.title-detail");
            String title = titleElement != null ? titleElement.text() : "";
            Log.d(TAG, "Article title: " + title);

            // Try different selectors for article content based on VnExpress layout patterns
            Elements contentElements = doc.select("article.fck_detail > *");
            
            if (contentElements.isEmpty()) {
                // Try alternative selector structures
                contentElements = doc.select("div.fck_detail > *");
            }
            
            if (contentElements.isEmpty()) {
                // Try more generic selectors
                contentElements = doc.select(".content-detail > p, .content-detail > figure");
            }
            
            Log.d(TAG, "Found " + contentElements.size() + " content elements");

            StringBuilder content = new StringBuilder();
            
            for (Element el : contentElements) {
                String tagName = el.tagName().toLowerCase();
                
                // Skip related news sections
                if (el.hasClass("box-relate") || el.hasClass("box-topping") || 
                    el.hasClass("related-news") || el.hasClass("social-box")) {
                    continue;
                }
                
                switch (tagName) {
                    case "p":
                        // Process paragraphs
                        content.append("<p>").append(el.html()).append("</p>");
                        break;
                        
                    case "figure":
                        // Process images
                        Element img = el.selectFirst("img[data-src]");
                        if (img == null) {
                            img = el.selectFirst("img[src]");
                        }
                        
                        if (img != null) {
                            String imgSrc = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
                            
                            // Make sure URL is absolute
                            if (imgSrc.startsWith("//")) {
                                imgSrc = "https:" + imgSrc;
                            }
                            
                            content.append("<img src=\"").append(imgSrc).append("\" />");
                            
                            // Add image caption if available
                            Element figCaption = el.selectFirst("figcaption");
                            if (figCaption != null && !figCaption.text().isEmpty()) {
                                content.append("<div style=\"color:#666;font-style:italic;text-align:center;margin-bottom:12px;font-size:14px;\">")
                                      .append(figCaption.text())
                                      .append("</div>");
                            }
                        }
                        break;
                        
                    case "table":
                        // Process tables
                        content.append(el.outerHtml());
                        break;
                        
                    case "h2":
                    case "h3":
                        // Process headings
                        content.append("<").append(tagName).append(">")
                               .append(el.text())
                               .append("</").append(tagName).append(">");
                        break;
                        
                    case "ul":
                    case "ol":
                        // Process lists
                        content.append(el.outerHtml());
                        break;
                }
            }

            // Add fallback message if no content was scraped
            String result = content.toString();
            if (result.isEmpty()) {
                Log.e(TAG, "No content extracted from article");
                return "<p>Không thể trích xuất nội dung bài viết. Vui lòng nhấn nút 'Đọc bài đầy đủ' để đọc trên VnExpress.</p>";
            }
            
            Log.d(TAG, "Successfully scraped article content");
            return result;

        } catch (IOException e) {
            Log.e(TAG, "Error fetching article content", e);
            return "<p>Không thể tải nội dung bài viết. Vui lòng kiểm tra lại kết nối mạng hoặc nhấn nút 'Đọc bài đầy đủ' để đọc trên VnExpress.</p>";
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while parsing article content", e);
            return "<p>Đã xảy ra lỗi khi phân tích nội dung bài viết. Vui lòng nhấn nút 'Đọc bài đầy đủ' để đọc trên VnExpress.</p>";
        }
    }
} 