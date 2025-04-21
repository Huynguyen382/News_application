package com.example.vnews.View;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;


import com.bumptech.glide.Glide;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.R;
import com.example.vnews.Adapter.NewsAdapter;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Repository.RssService;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.databinding.DataBindingUtil;
import com.example.vnews.databinding.ActivityHomeBinding;
import com.example.vnews.Model.articles;
import com.example.vnews.Utils.EyeProtectionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FirebaseRepository repository;
    private SharedPreferences preferences;
    private NewsAdapter newsAdapter;
    private List<RssNewsItem> newsList;
    private RssService rssService;
    
    // VnExpress RSS feed URL
    private static final String RSS_FEED_URL = "https://vnexpress.net/rss/tin-moi-nhat.rss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        
        // Apply eye protection if it's enabled
        EyeProtectionManager.applyEyeProtectionIfEnabled(this);
        
        preferences = getSharedPreferences("VNNews", MODE_PRIVATE);
        repository = new FirebaseRepository();
        rssService = new RssService();
        newsList = new ArrayList<>();
        
        // Initialize adapter with empty list
        newsAdapter = new NewsAdapter(this, newsList);

        setupUI();
        setupListeners();
        setupTouchListenerToHideKeyboard();
        loadRssNews();
    }

    private void setupUI() {
        // Set welcome text with user's name
        String userFullName = "Khách";
        if (repository.isUserLoggedIn()) {
            userFullName = repository.getCurrentUserName();
        }
        binding.welcomeText.setText(getString(R.string.welcome_user, userFullName));

        // Set current date
        String date = DateFormat.format("EEEE, dd MMMM", Calendar.getInstance(Locale.getDefault())).toString();
        binding.dateText.setText(date);

        // Setup RecyclerView
        binding.latestNewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.latestNewsRecyclerView.setAdapter(newsAdapter);
        
        // Ẩn bàn phím khi người dùng tương tác với RecyclerView
        binding.latestNewsRecyclerView.setOnTouchListener((v, event) -> {
            // Kiểm tra xem ô tìm kiếm có đang có focus không
            if (binding.searchEditText.hasFocus()) {
                hideKeyboard();
                binding.searchEditText.clearFocus();
            }
            return false; // Để RecyclerView vẫn xử lý sự kiện scroll
        });

        // Load user avatar
        Glide.with(this)
                .load(R.drawable.default_avatar)
                .circleCrop()
                .into(binding.profileImage);
    }

    private void setupListeners() {
        // Tab selection listener
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String category = "";
                switch (tab.getPosition()) {
                    case 0:
                        category = "tin-moi-nhat";
                        break;
                    case 1:
                        category = "thoi-su";
                        break;
                    case 2:
                        category = "kinh-doanh";
                        break;
                    case 3:
                        category = "the-thao";
                        break;
                }
                if (!category.isEmpty()) {
                    loadRssNewsByCategory(category);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Swipe refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadRssNews);
        
        // Ẩn bàn phím khi vuốt để làm mới
        binding.swipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (binding.searchEditText.hasFocus()) {
                hideKeyboard();
                binding.searchEditText.clearFocus();
            }
        });

        // Search listener
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = v.getText().toString().trim();
            if (!query.isEmpty()) {
                searchNews(query);
                
                // Ẩn bàn phím sau khi tìm kiếm
                hideKeyboard();
                
                // Xóa focus khỏi EditText
                v.clearFocus();
            }
            return true;
        });

        // Bottom navigation - Sử dụng lớp BottomNavMenu
        BottomNavMenu.setup(this, binding.bottomNavigationView, R.id.navigation_home);
    }

    // Phương thức thiết lập sự kiện touch cho layout chính
    private void setupTouchListenerToHideKeyboard() {
        // Lấy layout chính của activity
        View mainLayout = findViewById(android.R.id.content);
        
        // Thêm sự kiện touch
        mainLayout.setOnTouchListener((v, event) -> {
            // Lấy view đang có focus
            View currentFocus = getCurrentFocus();
            
            // Chỉ ẩn bàn phím khi currentFocus không phải null và không phải là ô tìm kiếm
            if (currentFocus != null && currentFocus.getId() != binding.searchEditText.getId()) {
                // Ẩn bàn phím
                hideKeyboard();
                // Xóa focus
                currentFocus.clearFocus();
            }
            
            return false; // Cho phép các sự kiện touch khác tiếp tục xử lý
        });
    }
    
    // Phương thức tiện ích để ẩn bàn phím
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocusView = getCurrentFocus();
        if (currentFocusView != null) {
            imm.hideSoftInputFromWindow(currentFocusView.getWindowToken(), 0);
        }
    }

    // Load news from VnExpress RSS feed
    private void loadRssNews() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        rssService.fetchNewsData(RSS_FEED_URL, new RssService.OnFetchDataListener() {
            @Override
            public void onFetchDataSuccess(List<RssNewsItem> items) {
                runOnUiThread(() -> {
                    if (items != null && !items.isEmpty()) {
                        // Update the adapter with new data
                        newsList.clear();
                        newsList.addAll(items);
                        newsAdapter.notifyDataSetChanged();
                        
                        // Update the featured news with the first item
                        RssNewsItem featuredNews = items.get(0);
                        updateFeaturedNews(featuredNews);
                    }
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onFetchDataFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, 
                            "Lỗi khi tải tin tức: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    // Load RSS news by category
    private void loadRssNewsByCategory(String category) {
        binding.swipeRefreshLayout.setRefreshing(true);
        String categoryUrl = "https://vnexpress.net/rss/" + category + ".rss";
        
        rssService.fetchNewsData(categoryUrl, new RssService.OnFetchDataListener() {
            @Override
            public void onFetchDataSuccess(List<RssNewsItem> items) {
                runOnUiThread(() -> {
                    if (items != null && !items.isEmpty()) {
                        // Update the adapter with new data
                        newsList.clear();
                        newsList.addAll(items);
                        newsAdapter.notifyDataSetChanged();
                        
                        // Update the featured news with the first item
                        RssNewsItem featuredNews = items.get(0);
                        updateFeaturedNews(featuredNews);
                    }
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onFetchDataFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, 
                            "Lỗi khi tải tin tức: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    // Update the featured news section with the provided news item
    private void updateFeaturedNews(RssNewsItem news) {
        binding.featuredNewsTitle.setText(news.getTitle());
        binding.featuredNewsDescription.setText(news.getCleanDescription());
        
        // Load image
        Glide.with(this)
                .load(news.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.featuredNewsImage);
        
        // Set click listener for the featured news card
        binding.featuredNewsCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewsDetailActivity.class);
            intent.putExtra("article_title", news.getTitle());
            intent.putExtra("article_url", news.getLink());
            intent.putExtra("article_image", news.getImageUrl());
            intent.putExtra("article_description", news.getDescription());
            intent.putExtra("article_pubDate", news.getPubDate());
            startActivity(intent);
        });
    }

    private void searchNews(String query) {
        // Đảm bảo query không rỗng
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ khóa tìm kiếm", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Implement search functionality - filter the current list
        if (newsList != null && !newsList.isEmpty()) {
            List<RssNewsItem> filteredList = new ArrayList<>();
            
            for (RssNewsItem item : newsList) {
                if (item.getTitle().toLowerCase().contains(query.toLowerCase()) || 
                    item.getDescription().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
            
            if (filteredList.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy kết quả cho: " + query, Toast.LENGTH_SHORT).show();
                
                // Không xóa nội dung ô tìm kiếm để người dùng có thể sửa
            } else {
                newsAdapter.updateNewsList(filteredList);
                Toast.makeText(this, "Tìm thấy " + filteredList.size() + " kết quả", Toast.LENGTH_SHORT).show();
                
                // Xóa nội dung ô tìm kiếm vì tìm kiếm thành công
                binding.searchEditText.setText("");
            }
        } else {
            Toast.makeText(this, "Không có dữ liệu tin tức để tìm kiếm", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    @Deprecated
    public void onBackPressed() {
        super.onBackPressed();
        // Gọi finish() sau khi gọi super.onBackPressed()
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when activity is resumed
        loadRssNews();
    }
}
