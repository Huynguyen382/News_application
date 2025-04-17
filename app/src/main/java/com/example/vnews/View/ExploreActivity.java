package com.example.vnews.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.R;
import com.example.vnews.Repository.RssService;
import com.example.vnews.databinding.ActivityExploreBinding;
import com.example.vnews.Adapter.NewsAdapter;
import com.example.vnews.Repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;

public class ExploreActivity extends AppCompatActivity {

    private ActivityExploreBinding binding;
    private NewsAdapter newsAdapter;
    private List<RssNewsItem> newsList;
    private RssService rssService;
    
    // VnExpress RSS feed URL cho tin xem nhiều
    private static final String MOST_VIEWED_RSS_URL = "https://vnexpress.net/rss/tin-xem-nhieu.rss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_explore);
        
        // Thiết lập toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.most_viewed_news);
        }
        
        // Khởi tạo các thành phần
        rssService = new RssService();
        newsList = new ArrayList<>();
        newsAdapter = new NewsAdapter(this, newsList);
        
        // Thiết lập RecyclerView
        binding.mostViewedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.mostViewedRecyclerView.setAdapter(newsAdapter);
        
        // Thiết lập SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadMostViewedNews);
        
        // Thiết lập Bottom Navigation
        setupBottomNavigation();
        
        // Tải dữ liệu
        loadMostViewedNews();
    }
    
    private void setupBottomNavigation() {
        // Sử dụng lớp BottomNavMenu để thiết lập bottom navigation
        BottomNavMenu.setup(this, binding.bottomNavigationView, R.id.navigation_explore);
    }
    
    private void loadMostViewedNews() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        rssService.fetchNewsData(MOST_VIEWED_RSS_URL, new RssService.OnFetchDataListener() {
            @Override
            public void onFetchDataSuccess(List<RssNewsItem> items) {
                runOnUiThread(() -> {
                    if (items != null && !items.isEmpty()) {
                        // Cập nhật adapter với dữ liệu mới
                        newsList.clear();
                        newsList.addAll(items);
                        newsAdapter.notifyDataSetChanged();
                    }
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onFetchDataFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ExploreActivity.this, 
                            "Lỗi khi tải tin tức: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
