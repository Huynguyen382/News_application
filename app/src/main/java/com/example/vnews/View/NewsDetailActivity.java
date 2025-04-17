package com.example.vnews.View;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.example.vnews.R;
import com.example.vnews.Utils.ArticleScraper;
import com.example.vnews.databinding.ActivityNewsDetailBinding;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "NewsDetailActivity";
    private ActivityNewsDetailBinding binding;
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_detail);
        
        // Enable back button in action bar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("article_title");
            String url = intent.getStringExtra("article_url");
            String imageUrl = intent.getStringExtra("article_image");
            String description = intent.getStringExtra("article_description");
            String pubDate = intent.getStringExtra("article_pubDate");
            
            // Set data to views
            if (title != null) {
                binding.newsTitle.setText(title);
                // Also set as action bar title
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Tin tức");
                }
            }
            
            // Format and display the publication date
            if (pubDate != null) {
                binding.newsDate.setText(formatPublishedDate(pubDate));
            } else {
                binding.newsDate.setVisibility(View.GONE);
            }
            
            // Load image
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.newsImage);
            } else {
                binding.newsImage.setVisibility(View.GONE);
            }
            
            // Show loading indicator
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.newsContent.setVisibility(View.GONE);
            
            // Fetch full article content if URL is available
            if (url != null && !url.isEmpty()) {
                // Load full article content using JSoup in background thread
                loadFullArticleContent(url);
                
                // Set up button to open full article in browser
                binding.readMoreButton.setOnClickListener(v -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                });
            } else {
                // If URL is not available, just display the description from RSS
                binding.progressBar.setVisibility(View.GONE);
                binding.newsContent.setVisibility(View.VISIBLE);
                
                if (description != null) {
                    // Extract content from CDATA if present
                    String content = extractContentFromCDATA(description);
                    
                    // Display the content
                    Spanned htmlContent = fromHtml(content);
                    binding.newsContent.setText(htmlContent);
                    binding.newsContent.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    binding.newsContent.setVisibility(View.GONE);
                }
                
                binding.readMoreButton.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Load full article content from the URL using JSoup
     */
    private void loadFullArticleContent(String url) {
        executor.execute(() -> {
            try {
                // Get article content using ArticleScraper
                final String htmlContent = ArticleScraper.getArticleContent(url);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.newsContent.setVisibility(View.VISIBLE);
                    
                    // Display the content
                    Spanned content = fromHtml(htmlContent);
                    binding.newsContent.setText(content);
                    binding.newsContent.setMovementMethod(LinkMovementMethod.getInstance());
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading full article content", e);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.newsContent.setVisibility(View.VISIBLE);
                    
                    // Display error message
                    binding.newsContent.setText(R.string.error_loading_content);
                    
                    // Show toast message
                    Toast.makeText(NewsDetailActivity.this, 
                            R.string.error_loading_content, 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Format the published date from RSS format to a more readable format
     */
    private String formatPublishedDate(String pubDateStr) {
        try {
            // RSS date format: EEE, dd MMM yyyy HH:mm:ss Z
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            
            Date date = inputFormat.parse(pubDateStr);
            return date != null ? outputFormat.format(date) : pubDateStr;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + pubDateStr, e);
            return pubDateStr;
        }
    }
    
    /**
     * Extract content from CDATA section
     */
    private String extractContentFromCDATA(String description) {
        // If the description contains CDATA, extract content between CDATA tags
        if (description.contains("CDATA[")) {
            Pattern pattern = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>");
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return description;
    }
    
    /**
     * Extract image URL from HTML content
     */
    private String extractImageUrlFromContent(String content) {
        Pattern pattern = Pattern.compile("<img[^>]+src=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Convert HTML to displayable text
     */
    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle back button click
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
