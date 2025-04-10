    package com.example.vnnews; // Thay đổi package name nếu cần

    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.View;
    import android.widget.ImageView;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
    import com.bumptech.glide.Glide;
    import com.example.vnnews.adapter.ArticleAdapter;
    import com.example.vnnews.model.Article;
    import com.rometools.rome.feed.synd.SyndEntry;
    import com.rometools.rome.feed.synd.SyndFeed;
    import com.rometools.rome.io.SyndFeedInput;
    import com.rometools.rome.io.XmlReader;
    import com.rometools.rome.feed.synd.SyndEnclosure;
    import java.net.URL;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;

    public class ExploreActivity extends AppCompatActivity {
        private RecyclerView recyclerView;
        private ArticleAdapter adapter;
        private SwipeRefreshLayout swipeRefreshLayout;
        private ExecutorService executorService;
        private static final String RSS_URL = "https://vnexpress.net/rss/tin-moi-nhat.rss";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_explore);

            // Initialize views
            recyclerView = findViewById(R.id.recycler_view);
            swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            
            // Setup RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ArticleAdapter();
            recyclerView.setAdapter(adapter);

            // Setup SwipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener(this::loadRSSFeed);

            // Initialize executor
            executorService = Executors.newSingleThreadExecutor();

            // Load initial data
            loadRSSFeed();

            // Setup taskbar
            setupTaskbar();
        }

        private void setupTaskbar() {
            ImageView homeIcon = findViewById(R.id.home_icon);
            ImageView exploreIcon = findViewById(R.id.explore_icon);
            ImageView profileIcon = findViewById(R.id.profile_icon);

            homeIcon.setOnClickListener(v -> {
                Intent intent = new Intent(ExploreActivity.this, HomeActivity.class);
                startActivity(intent);
            });

            exploreIcon.setOnClickListener(v -> {
                // Already in ExploreActivity
            });

            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(ExploreActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        private void loadRSSFeed() {
            swipeRefreshLayout.setRefreshing(true);
            
            executorService.execute(() -> {
                try {
                    URL feedUrl = new URL(RSS_URL);
                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feed = input.build(new XmlReader(feedUrl));
                    
                    List<Article> articles = new ArrayList<>();
                    for (SyndEntry entry : feed.getEntries()) {
                        String imageUrl = extractImageUrl(entry);
                        String description = entry.getDescription() != null ? 
                            entry.getDescription().getValue() : "";
                        String author = entry.getAuthor() != null ? 
                            entry.getAuthor() : "VnExpress";
                        String pubDate = entry.getPublishedDate() != null ? 
                            entry.getPublishedDate().toString() : "";
                        
                        Article article = new Article(
                            entry.getTitle(),
                            description,
                            author,
                            pubDate,
                            imageUrl,
                            1 // Default category ID
                        );
                        articles.add(article);
                    }
                    
                    runOnUiThread(() -> {
                        adapter.setArticles(articles);
                        swipeRefreshLayout.setRefreshing(false);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ExploreActivity.this, 
                            "Lỗi khi tải tin tức: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

       private String extractImageUrl(SyndEntry entry) {
    String imageUrl = "";

    // Kiểm tra thẻ <enclosure> trước
    if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            if (enclosure.getType().startsWith("image/")) {
                imageUrl = enclosure.getUrl();
                Log.d("ExploreActivity", "Extracted Image URL from <enclosure>: " + imageUrl);
                return imageUrl;
            }
        }
    }

    // Nếu không tìm thấy trong <enclosure>, kiểm tra nội dung HTML
    if (entry.getContents() != null && !entry.getContents().isEmpty()) {
        String content = entry.getContents().get(0).getValue();
        if (content != null && content.contains("<img")) {
            int imgStart = content.indexOf("<img");
            int srcStart = content.indexOf("src=\"", imgStart);
            if (srcStart != -1) {
                int srcEnd = content.indexOf("\"", srcStart + 5);
                if (srcEnd != -1) {
                    imageUrl = content.substring(srcStart + 5, srcEnd);
                    Log.d("ExploreActivity", "Extracted Image URL from <img>: " + imageUrl);
                }
            }
        }
    }

    // Trả về null nếu không có ảnh
    return imageUrl.isEmpty() ? null : imageUrl;
}


        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }
