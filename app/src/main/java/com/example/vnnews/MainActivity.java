package com.example.vnnews;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vnnews.adapter.ArticleAdapter;
import com.example.vnnews.model.Article;
import com.example.vnnews.viewmodel.ArticleViewModel;

public class MainActivity extends AppCompatActivity {
    private ArticleViewModel articleViewModel;
    private ArticleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Setup Adapter
        adapter = new ArticleAdapter();
        recyclerView.setAdapter(adapter);

        // Setup ViewModel
        articleViewModel = new ViewModelProvider(this).get(ArticleViewModel.class);
        articleViewModel.getAllArticles().observe(this, articles -> {
            adapter.setArticles(articles);
        });

        // Setup click listener
        adapter.setOnItemClickListener(article -> {
            // Handle article click
            Toast.makeText(this, "Article clicked: " + article.getTitle(), Toast.LENGTH_SHORT).show();
            // TODO: Navigate to article detail activity
        });

        // Add sample data (remove this in production)
        addSampleData();
    }

    private void addSampleData() {
        Article article1 = new Article(
            "Sample Article 1",
            "This is the content of article 1",
            "John Doe",
            "2024-01-08",
            "https://example.com/image1.jpg",
            1 // Technology category
        );
        
        Article article2 = new Article(
            "Sample Article 2",
            "This is the content of article 2",
            "Jane Smith",
            "2024-01-08",
            "https://example.com/image2.jpg",
            2 // Science category
        );

        articleViewModel.insert(article1);
        articleViewModel.insert(article2);
    }
}