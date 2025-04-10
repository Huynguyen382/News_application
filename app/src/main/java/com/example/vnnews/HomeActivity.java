package com.example.vnnews;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Setup taskbar
        setupTaskbar();
    }

    private void setupTaskbar() {
        ImageView homeIcon = findViewById(R.id.home_icon);
        ImageView exploreIcon = findViewById(R.id.explore_icon);
        ImageView profileIcon = findViewById(R.id.profile_icon);

        homeIcon.setOnClickListener(v -> {
            // Already in HomeActivity
        });

        exploreIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ExploreActivity.class);
            startActivity(intent);
        });

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }
} 