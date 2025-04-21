package com.example.vnews.View;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vnews.Adapter.NewsAdapter;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.Model.articles;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Utils.EyeProtectionManager;
import com.example.vnews.databinding.ActivitySavedArticlesBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SavedArticlesActivity extends AppCompatActivity {

    private static final String TAG = "SavedArticlesActivity";
    private static final String PREF_NAME = "saved_articles_cache";
    private static final String KEY_ARTICLES_CACHE = "cached_articles";
    private static final long CACHE_VALIDITY_MS = 30 * 60 * 1000; // 30 phút
    private static final int LOADING_TIMEOUT_MS = 6000; // 6 giây

    private ActivitySavedArticlesBinding binding;
    private FirebaseRepository repository;
    private NewsAdapter newsAdapter;
    private List<RssNewsItem> savedArticlesList;
    private SharedPreferences prefs;
    private Handler timeoutHandler;
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_saved_articles);
        
        // Khởi tạo
        repository = new FirebaseRepository();
        savedArticlesList = new ArrayList<>();
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        timeoutHandler = new Handler(Looper.getMainLooper());
        
        // Áp dụng chế độ bảo vệ mắt nếu đã bật
        EyeProtectionManager.applyEyeProtectionIfEnabled(this);
        
        // Thiết lập toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.saved_articles);
        }
        
        // Thiết lập RecyclerView và Adapter
        setupRecyclerView();
        
        // Hiển thị dữ liệu từ cache ngay lập tức (nếu có)
        displayCachedArticles();
        
        // Thiết lập SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadSavedArticles);
        
        // Thiết lập BottomNavigationView
        BottomNavMenu.setup(this, binding.bottomNavigationView, R.id.navigation_profile);
        
        // Tải bài viết đã lưu (chỉ khi không có cache hoặc cache hết hạn)
        if (!isDataLoaded || isCacheExpired()) {
            loadSavedArticles();
        }
    }
    
    private void setupRecyclerView() {
        // Khởi tạo adapter với danh sách rỗng
        newsAdapter = new NewsAdapter(this, new ArrayList<>());
        newsAdapter.setFromSavedArticles(true); // Đánh dấu là từ màn hình bài viết đã lưu
        
        // Thiết lập RecyclerView
        binding.savedArticlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.savedArticlesRecyclerView.setAdapter(newsAdapter);
        binding.savedArticlesRecyclerView.setHasFixedSize(true); // Tối ưu hiệu suất
        
        // Thêm ItemTouchHelper để xử lý vuốt xóa
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        itemTouchHelper.attachToRecyclerView(binding.savedArticlesRecyclerView);
    }
    
    private void displayCachedArticles() {
        String cachedArticlesJson = prefs.getString(KEY_ARTICLES_CACHE, null);
        
        if (cachedArticlesJson != null && !isCacheExpired()) {
            try {
                Gson gson = new GsonBuilder()
                    .setLenient()
                    .disableHtmlEscaping() // Không escape HTML và ký tự đặc biệt
                    .create();
                
                Type type = new TypeToken<List<RssNewsItem>>(){}.getType();
                savedArticlesList = gson.fromJson(cachedArticlesJson, type);
                
                // Xác nhận đã tải dữ liệu từ cache
                if (savedArticlesList != null && !savedArticlesList.isEmpty()) {
                    // Xử lý tiêu đề tiếng Việt cho các mục từ cache
                    for (RssNewsItem item : savedArticlesList) {
                        // Xử lý tiêu đề
                        String title = item.getTitle();
                        if (title != null) {
                            // Loại bỏ số ở cuối tiêu đề nếu có
                            title = title.replaceAll("\\s*\\d+$", "").trim();
                            
                            // Kiểm tra và chuyển đổi mã hóa nếu cần
                            if (!containsVietnameseCharacters(title)) {
                                try {
                                    // Kiểm tra nếu title có chứa byte không hợp lệ trong UTF-8
                                    byte[] bytesUTF8 = title.getBytes(StandardCharsets.UTF_8);
                                    String utf8Check = new String(bytesUTF8, StandardCharsets.UTF_8);
                                    
                                    if (!title.equals(utf8Check)) {
                                        // Thử ISO-8859-1 to UTF-8
                                        byte[] bytes = title.getBytes(StandardCharsets.ISO_8859_1);
                                        String utf8Title = new String(bytes, StandardCharsets.UTF_8);
                                        
                                        if (containsVietnameseCharacters(utf8Title)) {
                                            title = utf8Title;
                                        } else {
                                            // Thử Windows-1252 to UTF-8
                                            byte[] bytesWin = title.getBytes("windows-1252");
                                            String winTitle = new String(bytesWin, StandardCharsets.UTF_8);
                                            if (containsVietnameseCharacters(winTitle)) {
                                                title = winTitle;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Lỗi khi chuyển đổi mã hóa tiêu đề từ cache: " + title, e);
                                }
                            }
                            item.setTitle(title);
                        }
                        
                        // Xử lý URL hình ảnh
                        String imageUrl = item.getImageUrl();
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            // Đảm bảo không để URL null
                            imageUrl = "https://placeholder.com/wp-content/uploads/2018/10/placeholder.png";
                            item.setImageUrl(imageUrl);
                            Log.d(TAG, "Đã thiết lập URL hình ảnh mặc định cho item có tiêu đề: " + item.getTitle());
                        } else {
                            try {
                                // Log URL gốc để debug
                                Log.d(TAG, "URL hình ảnh gốc từ cache: " + imageUrl);
                                
                                // Đảm bảo URL bắt đầu đúng
                                if (!imageUrl.startsWith("http")) {
                                    imageUrl = "https:" + imageUrl;
                                }
                                
                                // Xử lý khoảng trắng và ký tự đặc biệt
                                imageUrl = imageUrl.replaceAll(" ", "%20");
                                
                                Log.d(TAG, "URL hình ảnh từ cache sau xử lý: " + imageUrl);
                                item.setImageUrl(imageUrl);
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi xử lý URL hình ảnh từ cache", e);
                                // Đặt URL hình ảnh mặc định nếu có lỗi
                                item.setImageUrl("https://placeholder.com/wp-content/uploads/2018/10/placeholder.png");
                            }
                        }
                    }
                    
                    // Cập nhật adapter và hiển thị
                    newsAdapter.updateNewsList(savedArticlesList);
                    binding.emptyStateTextView.setVisibility(View.GONE);
                    binding.savedArticlesRecyclerView.setVisibility(View.VISIBLE);
                    
                    isDataLoaded = true;
                    Log.d(TAG, "Đã tải " + savedArticlesList.size() + " bài viết từ cache");
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi phân tích dữ liệu cache", e);
            }
        }
    }
    
    private boolean isCacheExpired() {
        long lastCacheTime = prefs.getLong("last_cache_time", 0);
        return System.currentTimeMillis() - lastCacheTime > CACHE_VALIDITY_MS;
    }
    
    private void cacheArticles(List<RssNewsItem> articles) {
        if (articles == null) return;
        
        try {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .disableHtmlEscaping() // Không escape HTML và ký tự đặc biệt
                    .create();
            
            String articlesJson = gson.toJson(articles);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_ARTICLES_CACHE, articlesJson);
            editor.putLong("last_cache_time", System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "Cached " + articles.size() + " articles");
        } catch (Exception e) {
            Log.e(TAG, "Error caching articles", e);
        }
    }
    
    private void loadSavedArticles() {
        // Hiển thị indicator loading
        binding.swipeRefreshLayout.setRefreshing(true);
        
        // Clear any pending timeout callbacks
        timeoutHandler.removeCallbacksAndMessages(null);
        
        if (repository.isUserLoggedIn()) {
            String userId = repository.getCurrentUserId();
            
            // Set a flag để theo dõi callback
            final AtomicBoolean callbackCalled = new AtomicBoolean(false);
            
            // Set timeout cho Firebase operation
            timeoutHandler.postDelayed(() -> {
                // Chỉ hiển thị timeout nếu callback chưa được gọi
                if (callbackCalled.compareAndSet(false, true)) {
                    Log.d(TAG, "Firebase operation timed out");
                    binding.swipeRefreshLayout.setRefreshing(false);
                    
                    // Nếu không có dữ liệu, hiển thị thông báo
                    if (!isDataLoaded) {
                        binding.emptyStateTextView.setVisibility(View.VISIBLE);
                        binding.savedArticlesRecyclerView.setVisibility(View.GONE);
                        binding.emptyStateTextView.setText("Không thể tải dữ liệu. Vui lòng kiểm tra kết nối mạng.");
                    } else {
                        // Hiển thị thông báo toast nếu đã có dữ liệu cache
                        Toast.makeText(SavedArticlesActivity.this, 
                                "Không thể tải dữ liệu mới. Đang hiển thị dữ liệu đã lưu.", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }, LOADING_TIMEOUT_MS);
            
            repository.getSavedArticles(userId, new FirebaseRepository.FirestoreCallback<List<articles>>() {
                @Override
                public void onCallback(List<articles> articles) {
                    // Chỉ xử lý nếu đây là callback đầu tiên
                    if (callbackCalled.compareAndSet(false, true)) {
                        // Hủy timeout
                        timeoutHandler.removeCallbacksAndMessages(null);
                        
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        if (articles != null && !articles.isEmpty()) {
                            // Chuyển đổi từ articles sang RssNewsItem
                            savedArticlesList = convertToRssNewsItems(articles);
                            
                            // Hiển thị danh sách bài viết
                            binding.emptyStateTextView.setVisibility(View.GONE);
                            binding.savedArticlesRecyclerView.setVisibility(View.VISIBLE);
                            
                            // Cập nhật adapter
                            newsAdapter.updateNewsList(savedArticlesList);
                            
                            // Lưu cache
                            cacheArticles(savedArticlesList);
                            isDataLoaded = true;
                        } else {
                            // Hiển thị thông báo không có bài viết đã lưu
                            binding.emptyStateTextView.setVisibility(View.VISIBLE);
                            binding.savedArticlesRecyclerView.setVisibility(View.GONE);
                            binding.emptyStateTextView.setText(R.string.no_saved_articles);
                            
                            // Xóa cache khi không có bài viết
                            clearArticlesCache();
                            isDataLoaded = false;
                        }
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    // Chỉ xử lý nếu đây là callback đầu tiên
                    if (callbackCalled.compareAndSet(false, true)) {
                        // Hủy timeout
                        timeoutHandler.removeCallbacksAndMessages(null);
                        
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        if (!isDataLoaded) {
                            binding.emptyStateTextView.setVisibility(View.VISIBLE);
                            binding.savedArticlesRecyclerView.setVisibility(View.GONE);
                            binding.emptyStateTextView.setText("Lỗi: " + e.getMessage());
                        } else {
                            // Nếu đã có dữ liệu từ cache, chỉ hiển thị thông báo lỗi
                            Toast.makeText(SavedArticlesActivity.this, 
                                    "Lỗi khi tải: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        }
                        
                        Log.e(TAG, "Error loading saved articles", e);
                    }
                }
            });
        } else {
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.emptyStateTextView.setVisibility(View.VISIBLE);
            binding.savedArticlesRecyclerView.setVisibility(View.GONE);
            binding.emptyStateTextView.setText("Vui lòng đăng nhập để xem bài viết đã lưu");
            
            // Xóa cache khi không đăng nhập
            clearArticlesCache();
            isDataLoaded = false;
        }
    }
    
    private void clearArticlesCache() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_ARTICLES_CACHE);
        editor.apply();
    }
    
    /**
     * Chuyển đổi articles từ Firebase sang RssNewsItem để hiển thị
     */
    private List<RssNewsItem> convertToRssNewsItems(List<articles> articlesList) {
        List<RssNewsItem> result = new ArrayList<>();
        
        for (articles article : articlesList) {
            RssNewsItem item = new RssNewsItem();
            
            // Xử lý tiêu đề để hiển thị đúng dấu tiếng Việt
            String title = article.getTitle();
            if (title != null) {
                // Loại bỏ số ở cuối tiêu đề
                title = title.replaceAll("\\s*\\d+$", "").trim();
                
                // Giải mã URL nếu cần
                if (title.contains("%")) {
                    try {
                        title = URLDecoder.decode(title, StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi giải mã tiêu đề: " + title, e);
                    }
                }
                
                // Thử các phương pháp chuyển đổi mã hóa khác nhau
                if (!containsVietnameseCharacters(title)) {
                    try {
                        // Kiểm tra nếu title có chứa byte không hợp lệ trong UTF-8
                        byte[] bytesUTF8 = title.getBytes(StandardCharsets.UTF_8);
                        String utf8Check = new String(bytesUTF8, StandardCharsets.UTF_8);
                        
                        if (!title.equals(utf8Check)) {
                            // Phương pháp 1: ISO-8859-1 to UTF-8
                            byte[] bytesISO = title.getBytes(StandardCharsets.ISO_8859_1);
                            String utf8Title = new String(bytesISO, StandardCharsets.UTF_8);
                            
                            if (containsVietnameseCharacters(utf8Title)) {
                                title = utf8Title;
                            } else {
                                // Phương pháp 2: Windows-1252 to UTF-8
                                byte[] bytesWin = title.getBytes("windows-1252");
                                String winTitle = new String(bytesWin, StandardCharsets.UTF_8);
                                if (containsVietnameseCharacters(winTitle)) {
                                    title = winTitle;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi chuyển đổi mã hóa tiêu đề: " + title, e);
                    }
                }
            }
            
            item.setTitle(title);
            
            // Xử lý nội dung bài viết
            String content = article.getContent();
            if (content != null) {
                // Áp dụng các phương pháp xử lý tương tự như tiêu đề
                if (content.contains("%")) {
                    try {
                        content = URLDecoder.decode(content, StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi giải mã nội dung", e);
                    }
                }
                
                if (!containsVietnameseCharacters(content)) {
                    try {
                        // Kiểm tra nếu content có chứa byte không hợp lệ trong UTF-8
                        byte[] bytesUTF8 = content.getBytes(StandardCharsets.UTF_8);
                        String utf8Check = new String(bytesUTF8, StandardCharsets.UTF_8);
                        
                        if (!content.equals(utf8Check)) {
                            byte[] bytesISO = content.getBytes(StandardCharsets.ISO_8859_1);
                            String utf8Content = new String(bytesISO, StandardCharsets.UTF_8);
                            
                            if (containsVietnameseCharacters(utf8Content)) {
                                content = utf8Content;
                            } else {
                                byte[] bytesWin = content.getBytes("windows-1252");
                                String winContent = new String(bytesWin, StandardCharsets.UTF_8);
                                if (containsVietnameseCharacters(winContent)) {
                                    content = winContent;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi chuyển đổi mã hóa nội dung", e);
                    }
                }
            }
            
            item.setDescription(content);
            
            // Xử lý URL hình ảnh để tránh lỗi "Failed to create image decoder"
            String imageUrl = article.getImgUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Log URL gốc để debug
                Log.d(TAG, "URL hình ảnh gốc: " + imageUrl);
                
                // Kiểm tra và chuẩn hóa URL
                try {
                    // Đảm bảo URL bắt đầu bằng http hoặc https
                    if (!imageUrl.startsWith("http")) {
                        imageUrl = "https:" + imageUrl;
                    }
                    
                    // Loại bỏ các ký tự không hợp lệ trong URL
                    imageUrl = imageUrl.replaceAll(" ", "%20");
                    
                    Log.d(TAG, "URL hình ảnh sau khi xử lý: " + imageUrl);
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi khi xử lý URL hình ảnh: " + imageUrl, e);
                }
                
                item.setImageUrl(imageUrl);
            } else {
                // Đặt hình ảnh mặc định - quan trọng: không để null
                item.setImageUrl("https://placeholder.com/wp-content/uploads/2018/10/placeholder.png");
            }
            
            // Xử lý link và ngày đăng
            item.setLink(article.getId());
            
            // Định dạng lại ngày đăng nếu là timestamp
            String pubDate = article.getPublishedAt();
            if (pubDate != null && pubDate.matches("\\d+")) {
                try {
                    long timestamp = Long.parseLong(pubDate);
                    java.util.Date date = new java.util.Date(timestamp);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                    pubDate = sdf.format(date);
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi khi định dạng ngày đăng: " + pubDate, e);
                }
            }
            item.setPubDate(pubDate);
            
            result.add(item);
        }
        
        return result;
    }
    
    /**
     * Kiểm tra chuỗi có chứa ký tự tiếng Việt không
     */
    private boolean containsVietnameseCharacters(String text) {
        if (text == null) return false;
        // Mở rộng regex để bắt nhiều ký tự tiếng Việt hơn, bao gồm cả chữ hoa
        return text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ].*");
    }
    
    /**
     * Xóa bài viết đã lưu
     */
    private void unsaveArticle(RssNewsItem item, int position) {
        if (!repository.isUserLoggedIn() || item == null) {
            return;
        }
        
        String userId = repository.getCurrentUserId();
        String articleId = item.getLink();
        
        // Tạm thời xóa khỏi adapter trước để UI phản hồi nhanh
        final RssNewsItem removedItem = item;
        newsAdapter.removeItem(position);
        
        // Cập nhật cache ngay sau khi xóa khỏi UI
        savedArticlesList.remove(position);
        cacheArticles(savedArticlesList);
        
        // Kiểm tra nếu không còn bài viết nào
        if (newsAdapter.getItemCount() == 0) {
            binding.emptyStateTextView.setVisibility(View.VISIBLE);
            binding.savedArticlesRecyclerView.setVisibility(View.GONE);
            binding.emptyStateTextView.setText(R.string.no_saved_articles);
        }
        
        // Hiển thị Snackbar với tùy chọn hoàn tác
        Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                "Đã xóa bài viết khỏi danh sách đã lưu",
                Snackbar.LENGTH_LONG
        );
        
        snackbar.setAction("Hoàn tác", v -> {
            // Thêm lại vào adapter ngay lập tức để UI phản hồi nhanh
            savedArticlesList.add(position, removedItem);
            newsAdapter.updateNewsList(savedArticlesList);
            
            if (binding.emptyStateTextView.getVisibility() == View.VISIBLE) {
                binding.emptyStateTextView.setVisibility(View.GONE);
                binding.savedArticlesRecyclerView.setVisibility(View.VISIBLE);
            }
            
            // Cập nhật cache
            cacheArticles(savedArticlesList);
            
            // Lưu lại bài viết trên Firebase
            repository.saveArticle(userId, articleId, new FirebaseRepository.FirestoreCallback<String>() {
                @Override
                public void onCallback(String result) {
                    // Đã thêm vào UI rồi, không cần làm gì thêm
                }
                
                @Override
                public void onError(Exception e) {
                    Toast.makeText(SavedArticlesActivity.this, 
                            "Lỗi khi hoàn tác: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        snackbar.show();
        
        // Thực hiện unsave trên Firebase (ngay cả khi đã cập nhật UI trước)
        repository.unsaveArticle(userId, articleId, new FirebaseRepository.FirestoreCallback<Void>() {
            @Override
            public void onCallback(Void unused) {
                // Đã cập nhật UI rồi, không cần làm gì thêm
            }
            
            @Override
            public void onError(Exception e) {
                Toast.makeText(SavedArticlesActivity.this, 
                        "Cảnh báo: Có thể xóa không thành công trên máy chủ. Vui lòng thử lại.", 
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error unsaving article", e);
            }
        });
    }
    
    /**
     * Class xử lý vuốt để xóa
     */
    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        
        SwipeToDeleteCallback() {
            super(0, ItemTouchHelper.RIGHT); // Chỉ cho phép vuốt sang phải
        }
        
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, 
                             @NonNull RecyclerView.ViewHolder viewHolder, 
                             @NonNull RecyclerView.ViewHolder target) {
            return false; // Không hỗ trợ kéo thả
        }
        
        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            RssNewsItem item = newsAdapter.getItemAtPosition(position);
            
            if (item != null) {
                unsaveArticle(item, position);
            }
        }
        
        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, 
                               @NonNull RecyclerView.ViewHolder viewHolder, 
                               float dX, float dY, int actionState, boolean isCurrentlyActive) {
            
            View itemView = viewHolder.itemView;
            
            if (dX > 0) {
                // Nếu vuốt sang phải, vẽ nền màu đỏ
                itemView.setBackgroundColor(ContextCompat.getColor(SavedArticlesActivity.this, R.color.colorDelete));
            } else {
                // Đặt lại màu nền khi không vuốt
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }
            
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Xóa callbacks đang chờ xử lý
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 