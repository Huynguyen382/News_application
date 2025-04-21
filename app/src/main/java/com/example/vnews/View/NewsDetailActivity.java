package com.example.vnews.View;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Utils.ArticleScraper;
import com.example.vnews.Utils.EyeProtectionManager;
import com.example.vnews.databinding.ActivityNewsDetailBinding;

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
    private FirebaseRepository repository;
    private String articleUrl;
    private String articleTitle;
    private String articleImageUrl;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_detail);
        
        // Khởi tạo repository
        repository = new FirebaseRepository();
        
        // Apply eye protection if it's enabled
        EyeProtectionManager.applyEyeProtectionIfEnabled(this);
        
        // Thiết lập WebView
        setupWebView();
        
        // Thiết lập thanh công cụ
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null) {
            articleTitle = intent.getStringExtra("article_title");
            articleUrl = intent.getStringExtra("article_url");
            articleImageUrl = intent.getStringExtra("article_image");
            String description = intent.getStringExtra("article_description");
            String pubDate = intent.getStringExtra("article_pubDate");
            
            // Hiển thị tiêu đề
            if (articleTitle != null) {
                binding.newsTitle.setText(articleTitle);
                // Đặt tiêu đề cho thanh công cụ
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Tin tức");
                }
            }
            
            // Định dạng và hiển thị ngày xuất bản
            if (pubDate != null) {
                binding.newsDate.setText(formatPublishedDate(pubDate));
            } else {
                binding.newsDate.setVisibility(View.GONE);
            }
            
            // Hiển thị hình ảnh chính
            if (articleImageUrl != null && !articleImageUrl.isEmpty()) {
                Glide.with(this)
                        .load(articleImageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.newsImage);
            } else {
                binding.newsImage.setVisibility(View.GONE);
            }
            
            // Hiển thị trạng thái đang tải
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.webView.setVisibility(View.GONE);
            
            // Tải nội dung đầy đủ nếu có URL
            if (articleUrl != null && !articleUrl.isEmpty()) {
                // Tải nội dung bằng JSoup trong luồng nền
                loadFullArticleContent(articleUrl);
                
                // Thiết lập nút để mở bài viết đầy đủ trong trình duyệt
                binding.readMoreButton.setOnClickListener(v -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                    startActivity(browserIntent);
                });
                
                // Thiết lập nút lưu bài viết
                binding.saveArticleButton.setOnClickListener(v -> saveArticle());
                
                // Kiểm tra xem bài viết đã được lưu hay chưa
                checkIfArticleIsSaved();
            } else {
                // Nếu không có URL, chỉ hiển thị mô tả từ RSS
                binding.progressBar.setVisibility(View.GONE);
                binding.webView.setVisibility(View.VISIBLE);
                
                if (description != null) {
                    // Trích xuất nội dung từ CDATA nếu có
                    String content = extractContentFromCDATA(description);
                    
                    // Hiển thị nội dung
                    loadHtmlContent(content);
                } else {
                    binding.webView.setVisibility(View.GONE);
                }
                
                binding.readMoreButton.setVisibility(View.GONE);
                binding.saveArticleButton.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Kiểm tra xem bài viết đã được lưu hay chưa
     */
    private void checkIfArticleIsSaved() {
        if (!repository.isUserLoggedIn() || articleUrl == null) {
            // Nếu người dùng chưa đăng nhập hoặc không có URL, không cần kiểm tra
            binding.saveArticleButton.setText(R.string.save_article);
            return;
        }
        
        String userId = repository.getCurrentUserId();
        repository.isArticleSaved(userId, articleUrl, new FirebaseRepository.FirestoreCallback<Boolean>() {
            @Override
            public void onCallback(Boolean isSaved) {
                runOnUiThread(() -> {
                    if (isSaved) {
                        binding.saveArticleButton.setText(R.string.article_saved);
                        binding.saveArticleButton.setEnabled(false);
                    } else {
                        binding.saveArticleButton.setText(R.string.save_article);
                        binding.saveArticleButton.setEnabled(true);
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    binding.saveArticleButton.setText(R.string.save_article);
                    Log.e(TAG, "Error checking if article is saved", e);
                });
            }
        });
    }
    
    /**
     * Lưu bài viết vào danh sách đã lưu
     */
    private void saveArticle() {
        if (!repository.isUserLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu bài viết", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (articleUrl == null || articleUrl.isEmpty()) {
            Toast.makeText(this, "Không thể lưu bài viết này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = repository.getCurrentUserId();
        repository.saveArticle(userId, articleUrl, new FirebaseRepository.FirestoreCallback<String>() {
            @Override
            public void onCallback(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsDetailActivity.this, "Đã lưu bài viết thành công", Toast.LENGTH_SHORT).show();
                    binding.saveArticleButton.setText(R.string.article_saved);
                    binding.saveArticleButton.setEnabled(false);
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewsDetailActivity.this, "Lỗi khi lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving article", e);
                });
            }
        });
    }
    
    /**
     * Thiết lập WebView
     */
    private void setupWebView() {
        WebView webView = binding.webView;
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Ẩn trạng thái đang tải khi trang đã tải xong
                binding.progressBar.setVisibility(View.GONE);
                binding.webView.setVisibility(View.VISIBLE);
            }
        });
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false); // Tắt JavaScript vì lý do bảo mật
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
    }
    
    /**
     * Tải nội dung HTML vào WebView
     */
    private void loadHtmlContent(String htmlContent) {
        // Tạo HTML đầy đủ với CSS cho phần hiển thị
        String styledHtml = 
                "<html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: 'Roboto', Arial, sans-serif; color: #333; line-height: 1.6; padding: 8px; }" +
                "img { max-width: 100%; height: auto; display: block; margin: 12px auto; }" +
                "figcaption { color: #666; font-size: 14px; font-style: italic; text-align: center; margin-bottom: 16px; }" +
                "p { margin-bottom: 16px; }" +
                "h2, h3 { margin-top: 20px; margin-bottom: 10px; }" +
                "</style></head><body>" +
                htmlContent +
                "</body></html>";
        
        binding.webView.loadDataWithBaseURL(null, styledHtml, "text/html", "utf-8", null);
    }
    
    /**
     * Tải nội dung đầy đủ của bài viết từ URL
     */
    private void loadFullArticleContent(String url) {
        // Hiển thị trạng thái đang tải
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.webView.setVisibility(View.GONE);
        
        executor.execute(() -> {
            try {
                // Lấy nội dung bài viết bằng ArticleScraper
                final String htmlContent = ArticleScraper.getArticleContent(url);
                
                // Cập nhật UI trên luồng chính
                runOnUiThread(() -> {
                    try {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.webView.setVisibility(View.VISIBLE);
                        
                        // Hiển thị nội dung
                        loadHtmlContent(htmlContent);
                        
                        // Ghi log thành công
                        Log.d(TAG, "Article content loaded successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error displaying HTML content", e);
                        displayErrorMessage();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading full article content", e);
                
                // Cập nhật UI trên luồng chính
                runOnUiThread(this::displayErrorMessage);
            }
        });
    }
    
    /**
     * Hiển thị thông báo lỗi khi nội dung không thể tải
     */
    private void displayErrorMessage() {
        binding.progressBar.setVisibility(View.GONE);
        binding.webView.setVisibility(View.VISIBLE);
        loadHtmlContent("<p>" + getString(R.string.error_loading_content) + "</p>");
        Toast.makeText(this, R.string.error_loading_content, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Định dạng ngày xuất bản từ định dạng RSS sang định dạng dễ đọc hơn
     */
    private String formatPublishedDate(String pubDateStr) {
        try {
            // Định dạng ngày RSS: EEE, dd MMM yyyy HH:mm:ss Z
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
     * Trích xuất nội dung từ phần CDATA
     */
    private String extractContentFromCDATA(String description) {
        // Nếu mô tả chứa CDATA, trích xuất nội dung giữa các thẻ CDATA
        if (description.contains("CDATA[")) {
            Pattern pattern = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>");
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return description;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Xử lý khi nhấn nút quay lại
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
