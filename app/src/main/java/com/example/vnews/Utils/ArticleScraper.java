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

            // Get article title
            Element titleElement = doc.selectFirst("h1.title-detail");
            String title = titleElement != null ? titleElement.text() : "";
            Log.d(TAG, "Article title: " + title);

            // Get article content
            Elements contentElements = doc.select("div.fck_detail > p, div.fck_detail > figure, div.fck_detail > table, article.fck_detail > p, article.fck_detail > figure");
            
            if (contentElements.isEmpty()) {
                // Try alternative selectors if the first ones didn't work
                contentElements = doc.select(".content-detail p, .content-detail figure");
            }

            StringBuilder content = new StringBuilder();
            
            // We don't add the title because we already display it separately in the UI
            
            for (Element el : contentElements) {
                if (el.tagName().equals("p")) {
                    // Add paragraph
                    content.append("<p>").append(el.html()).append("</p>");
                } else if (el.tagName().equals("figure")) {
                    // Add image
                    Element img = el.selectFirst("img");
                    if (img != null) {
                        String imgSrc = img.attr("src");
                        String imgDesc = img.attr("alt");
                        
                        content.append("<figure>");
                        content.append("<img src=\"").append(imgSrc).append("\" style=\"width:100%;\" />");
                        
                        // Add image caption if available
                        Element figCaption = el.selectFirst("figcaption");
                        if (figCaption != null) {
                            content.append("<figcaption style=\"font-style:italic;color:#666;font-size:14px;margin-top:5px;\">")
                                   .append(figCaption.text())
                                   .append("</figcaption>");
                        }
                        
                        content.append("</figure>");
                    }
                } else if (el.tagName().equals("table")) {
                    // Add table
                    content.append(el.outerHtml());
                }
            }

            String result = content.toString();
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