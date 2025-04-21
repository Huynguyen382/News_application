package com.example.vnews.View;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.vnews.Model.users;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Utils.EyeProtectionManager;
import com.example.vnews.databinding.ActivityProfileBinding;
import com.google.gson.Gson;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int FIREBASE_TIMEOUT_MS = 8000; // 8 giây timeout, giảm từ 10s
    private static final String PREF_NAME = "user_profile_cache";
    private static final String KEY_USER_CACHE = "cached_user";
    private static final long CACHE_VALIDITY_MS = 60 * 60 * 1000; // 1 giờ

    private ActivityProfileBinding binding;
    private FirebaseRepository repository;
    private Handler timeoutHandler;
    private SharedPreferences prefs;
    private boolean isDataLoaded = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile);
        
        // Khởi tạo
        repository = new FirebaseRepository();
        timeoutHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Apply eye protection if enabled
        EyeProtectionManager.applyEyeProtectionIfEnabled(this);
        
        // Thiết lập toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.profile);
        }
        
        // Setup Eye Protection Toggle
        setupEyeProtectionToggle();
        
        // Thiết lập BottomNavigationView
        setupBottomNavigation();
        
        // Hiển thị dữ liệu từ cache ngay lập tức (nếu có)
        displayCachedUserData();
        
        // Thiết lập nút thử lại
        binding.btnRetry.setOnClickListener(v -> {
            binding.offlineLayout.setVisibility(View.GONE);
            binding.profileContent.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
            loadUserProfile();
        });
        
        // Thiết lập nút xem bài viết đã lưu
        binding.savedArticlesLayout.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SavedArticlesActivity.class);
            startActivity(intent);
        });
        
        // Thiết lập nút đăng xuất - luôn thiết lập dù có đăng nhập hay không
        binding.buttonLogout.setOnClickListener(v -> {
            // Xóa cache khi đăng xuất
            clearUserCache();
            
            // Đăng xuất và quay lại màn hình chính
            repository.logoutUser();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            
            // Chuyển hướng về HomeActivity
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        // Tải thông tin người dùng từ Firebase (chỉ tải khi không có cache hoặc cache hết hạn)
        if (!isDataLoaded || isCacheExpired()) {
            loadUserProfile();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Kiểm tra xem người dùng còn đăng nhập không (trường hợp đăng xuất từ màn hình khác)
        if (!repository.isUserLoggedIn() && isDataLoaded) {
            // Nếu đã đăng xuất và đang hiển thị dữ liệu, chuyển đến màn hình login
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show();
            
            // Chuyển hướng về HomeActivity
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
    
    private void setupEyeProtectionToggle() {
        SwitchCompat eyeProtectionSwitch = binding.switchEyeProtection;
        
        // Set initial state based on saved preference
        boolean isEyeProtectionEnabled = EyeProtectionManager.isEyeProtectionEnabled(this);
        eyeProtectionSwitch.setChecked(isEyeProtectionEnabled);
        
        // Set listener for changes
        eyeProtectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save the new setting
                EyeProtectionManager.setEyeProtectionEnabled(ProfileActivity.this, isChecked);
                
                // Apply the change immediately
                EyeProtectionManager.applyEyeProtectionIfEnabled(ProfileActivity.this);
                
                // Show toast to confirm the change
                String message = isChecked ? 
                        getString(R.string.eye_protection_enabled) : 
                        getString(R.string.eye_protection_disabled);
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupBottomNavigation() {
        // Sử dụng lớp BottomNavMenu để quản lý bottom navigation
        BottomNavMenu.setup(this, binding.bottomNavigationView, R.id.navigation_profile);
    }
    
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null) {
                return false;
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException checking network state", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network state", e);
            return false;
        }
    }
    
    private void displayCachedUserData() {
        // Hiển thị thông tin từ cache nếu có
        String cachedUserJson = prefs.getString(KEY_USER_CACHE, null);
        
        if (cachedUserJson != null) {
            try {
                Gson gson = new Gson();
                users user = gson.fromJson(cachedUserJson, users.class);
                
                if (user != null) {
                    updateUserProfileUI(user, true);
                    isDataLoaded = true;
                    
                    // Hiển thị thông báo nếu đang dùng dữ liệu cache và không có kết nối
                    if (!isNetworkAvailable()) {
                        Toast.makeText(this, "Đang hiển thị dữ liệu đã lưu (ngoại tuyến)", Toast.LENGTH_SHORT).show();
                        // Thiết lập lại nút thử lại để luôn hiển thị cần kết nối lại
                        binding.btnRetry.setOnClickListener(v -> {
                            if (isNetworkAvailable()) {
                                binding.offlineLayout.setVisibility(View.GONE);
                                binding.profileContent.setVisibility(View.GONE);
                                binding.progressBar.setVisibility(View.VISIBLE);
                                loadUserProfile();
                            } else {
                                Toast.makeText(this, "Vẫn chưa có kết nối mạng", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached user data", e);
            }
        }
    }
    
    private boolean isCacheExpired() {
        long lastCacheTime = prefs.getLong("last_cache_time", 0);
        return System.currentTimeMillis() - lastCacheTime > CACHE_VALIDITY_MS;
    }
    
    private void cacheUserData(users user) {
        if (user == null) return;
        
        try {
            Gson gson = new Gson();
            String userJson = gson.toJson(user);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USER_CACHE, userJson);
            editor.putLong("last_cache_time", System.currentTimeMillis());
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error caching user data", e);
        }
    }
    
    private void loadUserProfile() {
        // Clear any pending timeout callbacks
        timeoutHandler.removeCallbacksAndMessages(null);
        
        // Check if Firebase is available first
        if (repository instanceof FirebaseRepository && 
                !((FirebaseRepository)repository).isFirebaseAvailable()) {
            Log.e(TAG, "Firebase services not available. Showing offline mode");
            String cachedUserJson = prefs.getString(KEY_USER_CACHE, null);
            if (cachedUserJson != null) {
                try {
                    Gson gson = new Gson();
                    users cachedUser = gson.fromJson(cachedUserJson, users.class);
                    if (cachedUser != null) {
                        updateUserProfileUI(cachedUser, true);
                        View offlineIndicator = binding.offlineIndicator;
                        if (offlineIndicator != null) {
                            offlineIndicator.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(this, "Dịch vụ Google không khả dụng. Sử dụng dữ liệu đã lưu", Toast.LENGTH_SHORT).show();
                        binding.profileContent.setVisibility(View.VISIBLE);
                        isDataLoaded = true;
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing cached data", e);
                }
            }
            
            showOfflineMessage();
            binding.emptyProfileMessage.setText("Dịch vụ Google không khả dụng. Vui lòng thử lại sau");
            return;
        }
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Network is not available, showing offline message");
            
            // Nếu có cache, hiển thị cache; nếu không, hiển thị thông báo offline
            String cachedUserJson = prefs.getString(KEY_USER_CACHE, null);
            if (cachedUserJson != null) {
                try {
                    Gson gson = new Gson();
                    users cachedUser = gson.fromJson(cachedUserJson, users.class);
                    if (cachedUser != null) {
                        // Hiển thị dữ liệu từ cache với indicator rằng đang offline
                        updateUserProfileUI(cachedUser, true);
                        View offlineIndicator = binding.offlineIndicator;
                        if (offlineIndicator != null) {
                            offlineIndicator.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(this, "Đang sử dụng dữ liệu đã lưu (ngoại tuyến)", Toast.LENGTH_SHORT).show();
                        binding.profileContent.setVisibility(View.VISIBLE);
                        isDataLoaded = true;
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing cached user data in loadUserProfile", e);
                }
            }
            
            // Nếu không có cache hoặc parsing lỗi, hiển thị thông báo offline
            if (!isDataLoaded) {
                showOfflineMessage();
            }
            return;
        }
        
        try {
            if (repository.isUserLoggedIn()) {
                String userId = repository.getCurrentUserId();
                
                if (userId == null) {
                    Log.e(TAG, "User ID is null despite user being logged in");
                    showOfflineMessage();
                    binding.emptyProfileMessage.setText("Không thể xác định người dùng. Vui lòng đăng nhập lại");
                    binding.btnRetry.setText("Đăng nhập lại");
                    binding.btnRetry.setOnClickListener(v -> {
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        startActivity(intent);
                    });
                    return;
                }
                
                // Ẩn thông báo offline
                binding.offlineLayout.setVisibility(View.GONE);
                
                // Hiển thị trạng thái đang tải (chỉ khi chưa có dữ liệu)
                if (!isDataLoaded) {
                    binding.profileContent.setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.VISIBLE);
                }
                
                // Set a flag to track if callback has been called
                final AtomicBoolean callbackCalled = new AtomicBoolean(false);
                
                // Set timeout for Firebase operation
                timeoutHandler.postDelayed(() -> {
                    // Only show timeout if callback hasn't been called yet
                    if (callbackCalled.compareAndSet(false, true)) {
                        Log.d(TAG, "Firebase operation timed out");
                        
                        // Khi timeout, lấy từ cache nếu có, nếu không hiển thị thông báo offline
                        String cachedJson = prefs.getString(KEY_USER_CACHE, null);
                        if (cachedJson != null && !isDataLoaded) {
                            try {
                                Gson gson = new Gson();
                                users cachedUser = gson.fromJson(cachedJson, users.class);
                                if (cachedUser != null) {
                                    runOnUiThread(() -> {
                                        binding.progressBar.setVisibility(View.GONE);
                                        updateUserProfileUI(cachedUser, true);
                                        Toast.makeText(ProfileActivity.this, "Sử dụng dữ liệu đã lưu do kết nối chậm", Toast.LENGTH_SHORT).show();
                                        isDataLoaded = true;
                                    });
                                    return;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing cached user data during timeout", e);
                            }
                        }
                        
                        // Nếu không có cache hoặc parsing lỗi
                        if (!isDataLoaded) {
                            runOnUiThread(() -> {
                                binding.progressBar.setVisibility(View.GONE);
                                showOfflineMessage();
                            });
                        }
                    }
                }, FIREBASE_TIMEOUT_MS);
                
                // Tải thông tin người dùng từ Firestore với timeout
                try {
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
                                        updateUserProfileUI(user, false);
                                        cacheUserData(user);
                                        isDataLoaded = true;
                                        
                                        // Ẩn indicator offline nếu có
                                        View offlineIndicator = binding.offlineIndicator;
                                        if (offlineIndicator != null) {
                                            offlineIndicator.setVisibility(View.GONE);
                                        }
                                    } else if (!isDataLoaded) {
                                        // Kiểm tra xem có dữ liệu cache không
                                        String cachedJson = prefs.getString(KEY_USER_CACHE, null);
                                        if (cachedJson != null) {
                                            try {
                                                Gson gson = new Gson();
                                                users cachedUser = gson.fromJson(cachedJson, users.class);
                                                if (cachedUser != null) {
                                                    updateUserProfileUI(cachedUser, true);
                                                    Toast.makeText(ProfileActivity.this, "Không tìm thấy dữ liệu mới, hiển thị dữ liệu đã lưu", Toast.LENGTH_SHORT).show();
                                                    isDataLoaded = true;
                                                    return;
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error parsing cached user data when server returns null", e);
                                            }
                                        }
                                        
                                        // Nếu không có cache hoặc parsing lỗi
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
                                    if (!isDataLoaded) {
                                        binding.progressBar.setVisibility(View.GONE);
                                        
                                        // Kiểm tra xem có dữ liệu cache không trước khi hiển thị lỗi
                                        String cachedJson = prefs.getString(KEY_USER_CACHE, null);
                                        if (cachedJson != null) {
                                            try {
                                                Gson gson = new Gson();
                                                users cachedUser = gson.fromJson(cachedJson, users.class);
                                                if (cachedUser != null) {
                                                    updateUserProfileUI(cachedUser, true);
                                                    
                                                    // Hiển thị thông báo lỗi cụ thể cho SecurityException
                                                    if (e instanceof SecurityException) {
                                                        Toast.makeText(ProfileActivity.this, 
                                                                "Lỗi kết nối với Google Play Services. Hiển thị dữ liệu đã lưu", 
                                                                Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(ProfileActivity.this, 
                                                                "Lỗi kết nối: Đang hiển thị dữ liệu đã lưu", 
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                    
                                                    isDataLoaded = true;
                                                    return;
                                                }
                                            } catch (Exception ex) {
                                                Log.e(TAG, "Error parsing cached user data during error", ex);
                                            }
                                        }
                                        
                                        // Nếu không có cache hoặc parsing lỗi
                                        // Kiểm tra nếu lỗi do offline hoặc các lỗi khác
                                        if (e instanceof java.net.UnknownHostException || 
                                            e instanceof java.io.IOException ||
                                            e instanceof SecurityException ||
                                            (e.getMessage() != null && (
                                                e.getMessage().contains("offline") || 
                                                e.getMessage().contains("network") || 
                                                e.getMessage().contains("timeout") ||
                                                e.getMessage().contains("UNAVAILABLE")))) {
                                            showOfflineMessage();
                                            
                                            if (e instanceof SecurityException) {
                                                binding.emptyProfileMessage.setText("Lỗi kết nối với Google Play Services");
                                            }
                                        } else {
                                            showOfflineMessage();
                                            Toast.makeText(ProfileActivity.this, 
                                                    "Lỗi khi tải thông tin: " + e.getMessage(), 
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }
                    });
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException in getUserProfile, handling gracefully", e);
                    if (callbackCalled.compareAndSet(false, true)) {
                        timeoutHandler.removeCallbacksAndMessages(null);
                        
                        // Sử dụng dữ liệu cache nếu có
                        String cachedJson = prefs.getString(KEY_USER_CACHE, null);
                        if (cachedJson != null) {
                            try {
                                Gson gson = new Gson();
                                users cachedUser = gson.fromJson(cachedJson, users.class);
                                if (cachedUser != null) {
                                    runOnUiThread(() -> {
                                        binding.progressBar.setVisibility(View.GONE);
                                        updateUserProfileUI(cachedUser, true);
                                        Toast.makeText(ProfileActivity.this, 
                                                "Lỗi kết nối với Google Play Services. Hiển thị dữ liệu đã lưu", 
                                                Toast.LENGTH_SHORT).show();
                                        isDataLoaded = true;
                                    });
                                    return;
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error parsing cached data during SecurityException", ex);
                            }
                        }
                        
                        // Nếu không có cache
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            showOfflineMessage();
                            binding.emptyProfileMessage.setText("Lỗi kết nối với Google Play Services");
                        });
                    }
                }
            } else if (!isDataLoaded) {
                showOfflineMessage();
                
                binding.emptyProfileMessage.setText("Vui lòng đăng nhập để xem thông tin tài khoản");
                binding.btnRetry.setText("Đăng nhập");
                binding.btnRetry.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    startActivity(intent);
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in loadUserProfile", e);
            String cachedUserJson = prefs.getString(KEY_USER_CACHE, null);
            if (cachedUserJson != null) {
                try {
                    Gson gson = new Gson();
                    users cachedUser = gson.fromJson(cachedUserJson, users.class);
                    if (cachedUser != null) {
                        runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            updateUserProfileUI(cachedUser, true);
                            Toast.makeText(this, "Lỗi Google Play Services. Hiển thị dữ liệu đã lưu", Toast.LENGTH_SHORT).show();
                            binding.profileContent.setVisibility(View.VISIBLE);
                            isDataLoaded = true;
                        });
                        return;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error parsing cached user data", ex);
                }
            }
            showOfflineMessage();
            binding.emptyProfileMessage.setText("Lỗi kết nối với Google Play Services");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in loadUserProfile", e);
            if (!isDataLoaded) {
                showOfflineMessage();
                binding.emptyProfileMessage.setText("Lỗi không xác định: " + e.getMessage());
            }
        }
    }
    
    private void updateUserProfileUI(users user, boolean fromCache) {
        // Hiển thị layout nội dung
        binding.profileContent.setVisibility(View.VISIBLE);
        
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
        
        // Chỉ hiển thị chỉ báo offline nếu thực sự đang offline
        if (isOfflinePlaceholder && !isNetworkAvailable()) {
            View offlineIndicator = binding.offlineIndicator;
            if (offlineIndicator != null) {
                offlineIndicator.setVisibility(View.VISIBLE);
            }
        } else {
            View offlineIndicator = binding.offlineIndicator;
            if (offlineIndicator != null) {
                offlineIndicator.setVisibility(View.GONE);
            }
        }
        
        // Tải avatar nếu có
        if (user.getAvtUrl() != null && !user.getAvtUrl().isEmpty()) {
            RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop();
            
            Glide.with(this)
                .load(user.getAvtUrl())
                .apply(options)
                .into(binding.imageAvatar);
        } else {
            binding.imageAvatar.setImageResource(R.drawable.default_avatar);
        }
        
        // Hiển thị một chỉ báo "cached" nếu dữ liệu đến từ cache
        if (fromCache && !isNetworkAvailable()) {
            Toast.makeText(this, "Đang hiển thị dữ liệu đã lưu", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearUserCache() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_CACHE);
        editor.apply();
        isDataLoaded = false;
    }
    
    private void showOfflineMessage() {
        binding.progressBar.setVisibility(View.GONE);
        binding.offlineLayout.setVisibility(View.VISIBLE);
        binding.profileContent.setVisibility(View.GONE);
        
        // Tùy chỉnh thông báo dựa trên tình trạng dữ liệu
        String cachedUserJson = prefs.getString(KEY_USER_CACHE, null);
        if (cachedUserJson != null) {
            binding.emptyProfileMessage.setText("Không có kết nối mạng. Dữ liệu hiển thị có thể không phải mới nhất.");
            binding.btnRetry.setText("Thử lại");
        } else {
            binding.emptyProfileMessage.setText("Không có kết nối mạng. Vui lòng kết nối và thử lại.");
            binding.btnRetry.setText("Thử lại");
        }
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
