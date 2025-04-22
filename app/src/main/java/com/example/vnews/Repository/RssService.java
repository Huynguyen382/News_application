package com.example.vnews.Repository;

import android.os.AsyncTask;
import android.util.Log;

import com.example.vnews.Model.RssNewsItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RssService {

    private static final String TAG = "RssService";
    
    public interface OnFetchDataListener {
        void onFetchDataSuccess(List<RssNewsItem> newsList);
        void onFetchDataFailure(Exception e);
    }
    
    public void fetchNewsData(String url, OnFetchDataListener listener) {
        new FetchRssDataTask(listener).execute(url);
    }
    
    private static class FetchRssDataTask extends AsyncTask<String, Void, List<RssNewsItem>> {
        
        private final OnFetchDataListener listener;
        private Exception exception;
        
        FetchRssDataTask(OnFetchDataListener listener) {
            this.listener = listener;
        }
        
        @Override
        protected List<RssNewsItem> doInBackground(String... urls) {
            if (urls.length == 0) return null;
            
            try {
                return parseRssFeed(urls[0]);
            } catch (IOException e) {
                exception = e;
                Log.e(TAG, "Error fetching RSS data", e);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<RssNewsItem> newsList) {
            if (exception != null) {
                listener.onFetchDataFailure(exception);
            } else if (newsList != null) {
                listener.onFetchDataSuccess(newsList);
            } else {
                listener.onFetchDataFailure(new Exception("Unknown error occurred"));
            }
        }
        
        private List<RssNewsItem> parseRssFeed(String url) throws IOException {
            List<RssNewsItem> newsList = new ArrayList<>();
            
            Document document = Jsoup.connect(url).parser(Parser.xmlParser()).get();
            Elements items = document.select("item");
            
            for (Element item : items) {
                RssNewsItem news = new RssNewsItem();
                
                // Extract basic info
                news.setTitle(item.select("title").text());
                news.setPubDate(item.select("pubDate").text());
                news.setLink(item.select("link").text());
                
                // Extract guid if available
                String guid = item.select("guid").text();
                if (guid == null || guid.isEmpty()) {
                    // If no guid, use link as fallback
                    guid = news.getLink();
                }
                news.setGuid(guid);
                
                // Extract description which contains HTML
                String description = item.select("description").text();
                news.setDescription(description);
                
                // Extract image URL from description or enclosure tag
                String imageUrl = "";
                
                // First try to get from enclosure tag if exists
                Element enclosure = item.selectFirst("enclosure");
                if (enclosure != null && enclosure.hasAttr("url")) {
                    imageUrl = enclosure.attr("url");
                } 
                // Otherwise try to parse from description which may contain image url in HTML
                else if (description != null && !description.isEmpty()) {
                    // Look for image URL pattern in the description
                    Pattern pattern = Pattern.compile("src=\"(.*?)\"");
                    Matcher matcher = pattern.matcher(description);
                    if (matcher.find()) {
                        imageUrl = matcher.group(1);
                    }
                }
                
                news.setImageUrl(imageUrl);
                
                // Set this item as coming from RSS
                news.setFromRss(true);
                
                newsList.add(news);
            }
            
            return newsList;
        }
    }
} 