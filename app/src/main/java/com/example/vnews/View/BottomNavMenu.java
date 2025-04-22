package com.example.vnews.View;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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
                
                // Sử dụng MainContainerActivity thay vì các Activity riêng lẻ
                Intent intent = new Intent(context, MainContainerActivity.class);
                
                // Truyền vị trí tab cần chọn
                if (itemId == R.id.navigation_home) {
                    intent.putExtra("selected_tab", 0);
                } else if (itemId == R.id.navigation_explore) {
                    intent.putExtra("selected_tab", 1);
                } else if (itemId == R.id.navigation_profile) {
                    // Kiểm tra trạng thái đăng nhập
                    if (repository.isUserLoggedIn()) {
                        intent.putExtra("selected_tab", 2);
                    } else {
                        // Nếu chưa đăng nhập, hiển thị dialog hỏi
                        showLoginDialog();
                        return true;
                    }
                }
                
                // Khởi động MainContainerActivity
                context.startActivity(intent);
                
                // Kết thúc Activity hiện tại nếu nó là Activity
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
                
                return true;
            }
        });
    }
    
    /**
     * Hiển thị hộp thoại đăng nhập
     */
    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Đăng nhập")
                .setMessage("Bạn cần đăng nhập để xem trang cá nhân")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    // Chuyển đến màn hình đăng nhập
                    Intent intent = new Intent(context, LoginActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Quay về tab Home
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                })
                .setCancelable(false)
                .show();
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