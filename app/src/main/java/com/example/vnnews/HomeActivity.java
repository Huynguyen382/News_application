package com.example.vnnews;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.vnnews.adapter.NewsAdapter;
import com.example.vnnews.database.AppDatabase;
import com.example.vnnews.databinding.ActivityHomeBinding;
import com.example.vnnews.model.News;
import com.example.vnnews.model.User;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements NewsAdapter.OnNewsClickListener {
    private ActivityHomeBinding binding;
    private NewsAdapter newsAdapter;
    private User currentUser;
    private AppDatabase database;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = AppDatabase.getInstance(this);
        preferences = getSharedPreferences("VNNews", MODE_PRIVATE);

        setupUI();
        setupListeners();
        loadFeaturedNews();
        loadLatestNews();
    }

    private void setupUI() {
        // Set welcome text with user's name
        currentUser = getCurrentUser();
        binding.welcomeText.setText(getString(R.string.welcome_user, currentUser.getFullName()));

        // Set current date
        String date = DateFormat.format("EEEE, dd MMMM", Calendar.getInstance(Locale.getDefault())).toString();
        binding.dateText.setText(date);

        // Setup RecyclerView
        newsAdapter = new NewsAdapter(this, this);
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
                loadNewsByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Swipe refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFeaturedNews();
            loadLatestNews();
        });

        // Search listener
        binding.searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = v.getText().toString().trim();
            if (!query.isEmpty()) {
                searchNews(query);
            }
            return true;
        });

        // Bottom navigation listener
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_explore) {
                startActivity(new Intent(this, ExploreActivity.class));
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return true;
        });
    }

    private void loadFeaturedNews() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        // TODO: Replace with actual API call
        News featuredNews = new News(
            1L,
            "Tin tức nổi bật trong ngày",
            "Mô tả ngắn gọn về tin tức nổi bật",
            "Nội dung chi tiết của tin tức",
            "https://example.com/image.jpg",
            "Chính trị",
            new Date(),
            "VNNews",
            "Admin",
            1000,
            true
        );

        // Update UI
        binding.featuredNewsTitle.setText(featuredNews.getTitle());
        binding.featuredNewsDescription.setText(featuredNews.getDescription());
        Glide.with(this)
            .load(featuredNews.getImageUrl())
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(binding.featuredNewsImage);

        binding.swipeRefreshLayout.setRefreshing(false);
    }

    private void loadLatestNews() {
        // TODO: Replace with actual API call
        List<News> latestNews = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            latestNews.add(new News(
                (long) i,
                "Tin tức " + (i + 1),
                "Mô tả ngắn gọn về tin tức " + (i + 1),
                "Nội dung chi tiết của tin tức",
                "https://example.com/image.jpg",
                "Chính trị",
                new Date(),
                "VNNews",
                "Admin",
                100 * i,
                false
            ));
        }
        newsAdapter.setNewsList(latestNews);
    }

    private void loadNewsByCategory(int categoryPosition) {
        String category;
        switch (categoryPosition) {
            case 1:
                category = "Chính trị";
                break;
            case 2:
                category = "Kinh doanh";
                break;
            case 3:
                category = "Thể thao";
                break;
            default:
                category = "Tất cả";
                break;
        }
        // TODO: Load news by category from API
        Toast.makeText(this, "Đang tải tin tức " + category, Toast.LENGTH_SHORT).show();
    }

    private void searchNews(String query) {
        // TODO: Implement news search
        Toast.makeText(this, "Đang tìm kiếm: " + query, Toast.LENGTH_SHORT).show();
    }

    private User getCurrentUser() {
        String username = preferences.getString("username", "");
        return database.userDao().getUserByUsername(username);
    }

    @Override
    public void onNewsClick(News news) {
        Intent intent = new Intent(this, NewsDetailActivity.class);
        intent.putExtra("news_id", news.getId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 