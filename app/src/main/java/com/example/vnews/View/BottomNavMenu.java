package com.example.vnews.View;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Lớp tiện ích để quản lý BottomNavigationView trong các màn hình của ứng dụng.
 * Lớp này giúp đồng bộ hóa xử lý sự kiện của BottomNavigationView giữa các Activity
 * và tránh lặp code.
 */
public class BottomNavMenu {

    private final Context context;
    private final BottomNavigationView bottomNavigationView;
    private final FirebaseRepository repository;
    private final int currentMenuItemId;

    /**
     * Khởi tạo BottomNavMenu
     *
     * @param context Activity hiện tại
     * @param bottomNavigationView BottomNavigationView cần xử lý
     * @param currentMenuItemId ID của item hiện tại (để đánh dấu item đang chọn)
     */
    public BottomNavMenu(Context context, BottomNavigationView bottomNavigationView, int currentMenuItemId) {
        this.context = context;
        this.bottomNavigationView = bottomNavigationView;
        this.repository = new FirebaseRepository();
        this.currentMenuItemId = currentMenuItemId;
        
        // Thiết lập item đang chọn
        this.bottomNavigationView.setSelectedItemId(currentMenuItemId);
        
        // Thiết lập listener cho bottom navigation
        setupNavigation();
    }
    
    /**
     * Thiết lập sự kiện cho BottomNavigationView
     */
    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                // Không làm gì nếu item hiện tại được chọn
                if (itemId == currentMenuItemId) {
                    return true;
                }
                
                // Xử lý chuyển trang tùy thuộc vào item được chọn
                if (itemId == R.id.navigation_home) {
                    navigateTo(HomeActivity.class);
                    return true;
                } else if (itemId == R.id.navigation_explore) {
                    navigateTo(ExploreActivity.class);
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    // Kiểm tra trạng thái đăng nhập
                    if (repository.isUserLoggedIn()) {
                        // Nếu đã đăng nhập, chuyển đến ProfileActivity
                        navigateTo(ProfileActivity.class);
                    } else {
                        // Nếu chưa đăng nhập, chuyển đến LoginActivity
                        navigateTo(LoginActivity.class);
                    }
                    return true;
                }
                
                return false;
            }
        });
    }
    
    /**
     * Chuyển đến Activity mới
     * 
     * @param activityClass Lớp Activity cần chuyển đến
     */
    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
        
        // Kết thúc Activity hiện tại nếu nó là Activity
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
    
    /**
     * Phương thức tiện ích tĩnh để thiết lập BottomNavigationView cho Activity
     * 
     * @param context Activity hiện tại
     * @param bottomNavigationView BottomNavigationView cần xử lý
     * @param currentMenuItemId ID của item hiện tại
     * @return BottomNavMenu đã được thiết lập
     */
    public static BottomNavMenu setup(Context context, BottomNavigationView bottomNavigationView, int currentMenuItemId) {
        return new BottomNavMenu(context, bottomNavigationView, currentMenuItemId);
    }
} 