package com.example.vnews.View;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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

        // Search listener
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = v.getText().toString().trim();
            if (!query.isEmpty()) {
                searchNews(query);
            }
            return true;
        });

        // Bottom navigation - Sử dụng lớp BottomNavMenu
        BottomNavMenu.setup(this, binding.bottomNavigationView, R.id.navigation_home);
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
            } else {
                newsAdapter.updateNewsList(filteredList);
                Toast.makeText(this, "Tìm thấy " + filteredList.size() + " kết quả", Toast.LENGTH_SHORT).show();
            }
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
