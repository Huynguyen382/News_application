package com.example.vnnews;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.vnnews.databinding.ActivityNewsDetailBinding;
import com.example.vnnews.database.AppDatabase;
import com.example.vnnews.model.News;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NewsDetailActivity extends AppCompatActivity {
    private ActivityNewsDetailBinding binding;
    private AppDatabase database;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = AppDatabase.getInstance(this);
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        long newsId = getIntent().getLongExtra("news_id", -1);
        if (newsId != -1) {
            loadNewsDetails(newsId);
        }
    }

    private void loadNewsDetails(long newsId) {
        News news = database.newsDao().getNewsById(newsId);
        if (news != null) {
            binding.newsTitle.setText(news.getTitle());
            binding.newsCategory.setText(news.getCategory());
            binding.newsContent.setText(news.getContent());
            binding.newsSource.setText(news.getSource());
            binding.newsTime.setText(dateFormat.format(news.getPublishedDate()));
            binding.newsViews.setText(String.valueOf(news.getViews()));

            Glide.with(this)
                .load(news.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.newsImage);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 