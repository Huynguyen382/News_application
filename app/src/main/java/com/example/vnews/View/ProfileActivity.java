package com.example.vnews.View;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.example.vnews.Model.users;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.databinding.ActivityProfileBinding;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int FIREBASE_TIMEOUT_MS = 10000; // 10 seconds timeout

    private ActivityProfileBinding binding;
    private FirebaseRepository repository;
    private Handler timeoutHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile);
        
        // Khởi tạo repository
        repository = new FirebaseRepository();
        timeoutHandler = new Handler(Looper.getMainLooper());
        
        // Thiết lập toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.profile);
        }
        
        // Thiết lập BottomNavigationView
        setupBottomNavigation();
        
        // Thiết lập nút thử lại
        binding.btnRetry.setOnClickListener(v -> loadUserProfile());
        
        // Tải thông tin người dùng
        loadUserProfile();
    }
    
    private void setupBottomNavigation() {
        // Sử dụng lớp BottomNavMenu để quản lý bottom navigation
        BottomNavMenu.setup(this, binding.bottomNavigationView, R.id.navigation_profile);
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    private void loadUserProfile() {
        // Clear any pending timeout callbacks
        timeoutHandler.removeCallbacksAndMessages(null);
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Network is not available, showing offline message");
            showOfflineMessage();
            return;
        }
        
        if (repository.isUserLoggedIn()) {
            String userId = repository.getCurrentUserId();
            
            // Ẩn thông báo offline và nút thử lại
            binding.offlineLayout.setVisibility(View.GONE);
            
            // Hiển thị trạng thái đang tải
            binding.progressBar.setVisibility(View.VISIBLE);
            
            // Set a flag to track if callback has been called
            final AtomicBoolean callbackCalled = new AtomicBoolean(false);
            
            // Set timeout for Firebase operation
            timeoutHandler.postDelayed(() -> {
                // Only show timeout if callback hasn't been called yet
                if (callbackCalled.compareAndSet(false, true)) {
                    Log.d(TAG, "Firebase operation timed out");
                    binding.progressBar.setVisibility(View.GONE);
                    showOfflineMessage();
                }
            }, FIREBASE_TIMEOUT_MS);
            
            // Tải thông tin người dùng từ Firestore với timeout
            repository.getUserProfile(userId, new FirebaseRepository.FirestoreCallback<users>() {
                @Override
                public void onCallback(users user) {
                    // Only process if this is the first callback
                    if (callbackCalled.compareAndSet(false, true)) {
                        // Cancel the timeout
                        timeoutHandler.removeCallbacksAndMessages(null);
                        
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            
                            if (user != null) {
                                Log.d(TAG, "User profile loaded successfully");
                                
                                // Hiển thị email người dùng thay vì tên người dùng
                                String displayName = user.getEmail(); // Sử dụng email làm tên hiển thị
                                if (displayName == null || displayName.isEmpty()) {
                                    // Fallback to username if email is null/empty
                                    displayName = user.getUsername();
                                }
                                binding.textFullName.setText(displayName);
                                
                                // Hiện thị email ở textEmail
                                binding.textEmail.setText(user.getEmail());
                                
                                // Check if the user is an offline placeholder
                                boolean isOfflinePlaceholder = user.getUsername() != null && 
                                    user.getUsername().contains("offline");
                                if (isOfflinePlaceholder) {
                                    // Show a small offline indicator if using placeholder data
                                    View offlineIndicator = binding.offlineIndicator;
                                    if (offlineIndicator != null) {
                                        offlineIndicator.setVisibility(View.VISIBLE);
                                    }
                                    // Also log that we're using placeholder data
                                    Log.d(TAG, "Using offline placeholder data for user");
                                } else {
                                    // Hide the offline indicator
                                    View offlineIndicator = binding.offlineIndicator;
                                    if (offlineIndicator != null) {
                                        offlineIndicator.setVisibility(View.GONE);
                                    }
                                }
                                
                                // Tải avatar nếu có
                                if (user.getAvtUrl() != null && !user.getAvtUrl().isEmpty()) {
                                    Glide.with(ProfileActivity.this)
                                            .load(user.getAvtUrl())
                                            .placeholder(R.drawable.default_avatar)
                                            .error(R.drawable.default_avatar)
                                            .circleCrop()
                                            .into(binding.imageAvatar);
                                }
                            } else {
                                // User is null, possible offline scenario
                                Log.d(TAG, "User profile is null");
                                showOfflineMessage();
                            }
                        });
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    // Only process if this is the first callback
                    if (callbackCalled.compareAndSet(false, true)) {
                        // Cancel the timeout
                        timeoutHandler.removeCallbacksAndMessages(null);
                        
                        Log.e(TAG, "Error loading user profile", e);
                        
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            
                            // Kiểm tra nếu lỗi do offline
                            if (e.getMessage() != null && e.getMessage().contains("offline")) {
                                showOfflineMessage();
                            } else {
                                Toast.makeText(ProfileActivity.this, 
                                        "Lỗi khi tải thông tin: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            
            // Thiết lập nút đăng xuất
            binding.buttonLogout.setOnClickListener(v -> {
                // Đăng xuất và quay lại màn hình chính
                repository.logoutUser();
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                
                // Chuyển hướng về HomeActivity
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
    
    private void showOfflineMessage() {
        binding.progressBar.setVisibility(View.GONE);
        binding.offlineLayout.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear any pending callbacks to prevent leaks
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
