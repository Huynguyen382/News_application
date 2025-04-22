package com.example.vnews.View;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

public class NewsDetailActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static final String TAG = "NewsDetailActivity";
    private ActivityNewsDetailBinding binding;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private FirebaseRepository repository;
    private String articleUrl;
    private String articleTitle;
    private String articleImageUrl;
    private SharedPreferences preferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_detail);
        
        // Khởi tạo repository
        repository = new FirebaseRepository();
        
        // Khởi tạo SharedPreferences và đăng ký listener
        preferences = getSharedPreferences("VNNews", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        
        // Áp dụng chế độ bảo vệ mắt nếu được bật
        boolean eyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
        Log.d(TAG, "Eye protection enabled: " + eyeProtectionEnabled);
        EyeProtectionManager.applyEyeProtection(this, eyeProtectionEnabled);
        
        // Cập nhật giao diện dựa trên chế độ bảo vệ mắt
        updateUIForEyeProtection(eyeProtectionEnabled);
        
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
    
    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra và áp dụng lại chế độ bảo vệ mắt mỗi khi activity được khởi động lại
        boolean eyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
        Log.d(TAG, "onResume: Eye protection enabled: " + eyeProtectionEnabled);
        EyeProtectionManager.applyEyeProtection(this, eyeProtectionEnabled);
        
        // Cập nhật giao diện dựa trên chế độ bảo vệ mắt
        updateUIForEyeProtection(eyeProtectionEnabled);
        
        // Áp dụng chế độ bảo vệ mắt cho WebView nếu đang hiển thị nội dung
        updateWebViewWithEyeProtection();
    }
    
    @Override
    protected void onDestroy() {
        // Hủy đăng ký listener để tránh memory leak
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }
    
    /**
     * Cập nhật lại WebView với chế độ bảo vệ mắt nếu có thay đổi
     */
    private void updateWebViewWithEyeProtection() {
        if (binding.webView.getVisibility() == View.VISIBLE) {
            // Kiểm tra trạng thái chế độ bảo vệ mắt
            boolean eyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
            Log.d(TAG, "Updating WebView with eye protection: " + eyeProtectionEnabled);
            
            // Tải lại nội dung HTML với chế độ bảo vệ mắt (nếu được bật)
            String currentContent = (String) binding.webView.getTag();
            if (currentContent != null) {
                loadHtmlContent(currentContent);
            }
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Nếu thay đổi cài đặt chế độ bảo vệ mắt, cập nhật lại UI
        if (key != null && key.equals("eye_protection_enabled")) {
            // Lấy giá trị mới của chế độ bảo vệ mắt
            boolean eyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
            Log.d(TAG, "Preference changed - Eye protection: " + eyeProtectionEnabled);
            
            // Áp dụng chế độ bảo vệ mắt cho overlay
            EyeProtectionManager.applyEyeProtection(this, eyeProtectionEnabled);
            
            // Cập nhật giao diện dựa trên chế độ bảo vệ mắt
            updateUIForEyeProtection(eyeProtectionEnabled);
            
            // Cập nhật WebView với chế độ bảo vệ mắt mới
            updateWebViewWithEyeProtection();
        }
    }
    
    /**
     * Cập nhật giao diện dựa trên chế độ bảo vệ mắt
     * @param enabled trạng thái chế độ bảo vệ mắt
     */
    private void updateUIForEyeProtection(boolean enabled) {
        if (enabled) {
            // Màu sắc khi bật chế độ bảo vệ mắt
            int bgColor = Color.parseColor("#FCF6E8");  // Màu nền nhẹ nhàng
            int textColor = Color.parseColor("#624B3A"); // Màu chữ nâu nhẹ
            
            // Áp dụng màu cho các thành phần UI
            binding.newsTitle.setTextColor(textColor);
            binding.newsDate.setTextColor(Color.parseColor("#8A7054")); // Màu nâu nhạt cho ngày
            
            // Đặt màu nền cho layout
            ((View) binding.newsTitle.getParent()).setBackgroundColor(bgColor);
            
            // Áp dụng màu nhẹ cho hình ảnh (nếu có)
            if (binding.newsImage.getDrawable() != null) {
                binding.newsImage.setColorFilter(Color.parseColor("#17FFB65C"), android.graphics.PorterDuff.Mode.MULTIPLY);
            }
        } else {
            // Màu sắc mặc định
            int defaultTextColor = getResources().getColor(R.color.black);
            int dateColor = Color.GRAY;
            
            // Đặt lại màu mặc định
            binding.newsTitle.setTextColor(defaultTextColor);
            binding.newsDate.setTextColor(dateColor);
            
            // Đặt lại màu nền
            ((View) binding.newsTitle.getParent()).setBackgroundColor(Color.WHITE);
            
            // Xóa bộ lọc màu khỏi ảnh
            binding.newsImage.clearColorFilter();
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
        
        // Tăng kích thước font chữ mặc định để dễ đọc hơn
        webSettings.setDefaultFontSize(18);
        webSettings.setMinimumFontSize(14);
        
        // Thiết lập text scaling dựa vào chế độ bảo vệ mắt từ SharedPreferences
        boolean eyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
        if (eyeProtectionEnabled) {
            // Tăng khoảng cách dòng và điều chỉnh text size khi bật chế độ bảo vệ mắt
            webSettings.setTextZoom(105); // Tăng kích thước text thêm 5%
        } else {
            webSettings.setTextZoom(100); // Kích thước mặc định
        }
    }
    
    /**
     * Tải nội dung HTML vào WebView
     */
    private void loadHtmlContent(String htmlContent) {
        // Lưu nội dung HTML gốc để có thể tải lại khi cần
        binding.webView.setTag(htmlContent);
        
        // Kiểm tra xem chế độ bảo vệ mắt có được bật không từ SharedPreferences
        boolean eyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
        
        // Chọn background color và text color dựa trên chế độ bảo vệ mắt
        String backgroundColor = eyeProtectionEnabled ? "#FCF6E8" : "#FFFFFF";
        String textColor = eyeProtectionEnabled ? "#624B3A" : "#333333";
        String linkColor = eyeProtectionEnabled ? "#976526" : "#0066CC";
        String captionColor = eyeProtectionEnabled ? "#8A7054" : "#666666";
        
        // Chuẩn bị CSS bổ sung cho chế độ bảo vệ mắt
        String additionalEyeProtectionCss = eyeProtectionEnabled ? 
            "p, li { line-height: 1.8; letter-spacing: 0.01em; }" +
            "img { opacity: 0.95; filter: brightness(0.95); }" : 
            "";
        
        // Tạo HTML đầy đủ với CSS cho phần hiển thị
        String styledHtml = 
                "<html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: 'Roboto', Arial, sans-serif; color: " + textColor + "; line-height: 1.6; padding: 12px; background-color: " + backgroundColor + "; }" +
                "img { max-width: 100%; height: auto; display: block; margin: 16px auto; border-radius: 4px; }" +
                "figcaption { color: " + captionColor + "; font-size: 14px; font-style: italic; text-align: center; margin-bottom: 16px; }" +
                "p { margin-bottom: 16px; }" +
                "h2, h3 { margin-top: 24px; margin-bottom: 12px; }" +
                "a { color: " + linkColor + "; text-decoration: none; }" +
                additionalEyeProtectionCss +
                "</style></head><body>" +
                htmlContent +
                "</body></html>";
        
        binding.webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null);
        
        // Cập nhật text zoom trong WebSettings dựa vào chế độ bảo vệ mắt
        WebSettings webSettings = binding.webView.getSettings();
        if (eyeProtectionEnabled) {
            webSettings.setTextZoom(105); // Tăng kích thước text thêm 5%
        } else {
            webSettings.setTextZoom(100); // Kích thước mặc định
        }
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
