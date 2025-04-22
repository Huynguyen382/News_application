package com.example.vnews.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Utils.EyeProtectionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity chính chứa ViewPager2 và BottomNavigationView cố định
 * Cho phép người dùng vuốt để chuyển trang và giữ nguyên trạng thái BottomNavigationView
 */
public class MainContainerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // Áp dụng chế độ bảo vệ mắt nếu được bật
        EyeProtectionManager.applyEyeProtectionIfEnabled(this);

        // Khởi tạo repository
        repository = new FirebaseRepository();

        // Khởi tạo ViewPager2 và BottomNavigationView
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Thiết lập adapter cho ViewPager2
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);
        
        // Bật khả năng vuốt để chuyển trang
        viewPager.setUserInputEnabled(true);
        
        // Thiết lập số lượng trang được giữ lại trong bộ nhớ
        viewPager.setOffscreenPageLimit(2);
        
        // Thiết lập hiệu ứng chuyển trang
        viewPager.setPageTransformer(new ZoomOutPageTransformer());

        // Xử lý sự kiện chuyển trang trên ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_explore);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
                        break;
                }
            }
        });

        // Xử lý sự kiện chọn item trên BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.navigation_explore) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Kiểm tra trạng thái đăng nhập
                if (repository.isUserLoggedIn()) {
                    viewPager.setCurrentItem(2, true);
                } else {
                    // Hiển thị hộp thoại hỏi người dùng có muốn đăng nhập không
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("Đăng nhập")
                            .setMessage("Bạn cần đăng nhập để xem trang cá nhân")
                            .setPositiveButton("Đăng nhập", (dialog, which) -> {
                                // Chuyển đến màn hình đăng nhập
                                Intent intent = new Intent(this, LoginActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> {
                                // Quay về tab Home
                                viewPager.setCurrentItem(0, true);
                                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                            })
                            .setCancelable(false)
                            .show();
                    
                    // Giữ nguyên tab được chọn là Home
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                }
                return true;
            }
            
            return false;
        });
        
        // Xử lý selected_tab từ intent nếu có
        if (getIntent() != null && getIntent().hasExtra("selected_tab")) {
            int selectedTab = getIntent().getIntExtra("selected_tab", 0);
            
            // Kiểm tra nếu tab là Profile (2) và người dùng chưa đăng nhập
            if (selectedTab == 2 && !repository.isUserLoggedIn()) {
                // Hiển thị dialog đăng nhập thay vì chuyển tab
                showLoginDialog();
            } else {
                // Chuyển đến tab được chọn
                viewPager.setCurrentItem(selectedTab, false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Kiểm tra trạng thái đăng nhập mỗi khi Activity được khởi động lại
        checkLoginState();
    }
    
    /**
     * Kiểm tra trạng thái đăng nhập và cập nhật UI nếu cần
     */
    private void checkLoginState() {
        // Nếu đang ở fragment Profile nhưng đã đăng xuất, chuyển về Home
        if (viewPager.getCurrentItem() == 2 && !repository.isUserLoggedIn()) {
            viewPager.setCurrentItem(0, true);
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    /**
     * Hiển thị hộp thoại đăng nhập
     */
    private void showLoginDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Đăng nhập")
                .setMessage("Bạn cần đăng nhập để xem trang cá nhân")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    // Chuyển đến màn hình đăng nhập
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Quay về tab Home
                    viewPager.setCurrentItem(0, true);
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Adapter cho ViewPager2
     */
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Trả về Fragment tương ứng với vị trí
            switch (position) {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new ExploreFragment();
                case 2:
                    return new ProfileFragment();
                default:
                    return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            // Số lượng trang (3 trang: Home, Explore, Profile)
            return 3;
        }
    }
    
    /**
     * Hiệu ứng chuyển trang thu nhỏ và mờ dần khi lướt
     * Giống với hiệu ứng chuyển trang của Facebook
     */
    public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        @Override
        public void transformPage(@NonNull View page, float position) {
            int pageWidth = page.getWidth();
            int pageHeight = page.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // Trang nằm ngoài màn hình bên trái
                page.setAlpha(0f);
            } else if (position <= 1) { // [-1,1]
                // Hiệu ứng thu nhỏ dần và mờ dần khi lướt qua
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                
                if (position < 0) {
                    page.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    page.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Thu nhỏ trang
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);

                // Làm mờ dần trang tỉ lệ theo tỉ lệ thu nhỏ
                page.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            } else { // (1,+Infinity]
                // Trang nằm ngoài màn hình bên phải
                page.setAlpha(0f);
            }
        }
    }
} 