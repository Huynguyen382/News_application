package com.example.vnews.View;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.vnews.Model.users;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Utils.EyeProtectionManager;
import com.example.vnews.databinding.FragmentProfileBinding;
import com.google.gson.Gson;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.CONNECTIVITY_SERVICE;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseRepository repository;
    private SharedPreferences preferences;
    private users currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        repository = new FirebaseRepository();
        preferences = requireActivity().getSharedPreferences("VNNews", MODE_PRIVATE);
        
        setupToolbar();
        checkNetworkState();
        
        // Hiển thị thông tin phiên bản
        displayVersionInfo();
        
        // Kiểm tra người dùng đã đăng nhập hay chưa
        if (repository.isUserLoggedIn()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            
            // Lấy thông tin người dùng từ SharedPreferences
            String userJson = preferences.getString("user_data", "");
            if (!userJson.isEmpty()) {
                currentUser = new Gson().fromJson(userJson, users.class);
                displayUserInfo(currentUser);
                binding.progressBar.setVisibility(View.GONE);
            } else {
                // Lấy thông tin người dùng từ Firebase nếu không có trong SharedPreferences
                repository.getCurrentUserData(new FirebaseRepository.FirestoreCallback<users>() {
                    @Override
                    public void onCallback(users user) {
                        if (user != null) {
                            currentUser = user;
                            // Lưu thông tin người dùng vào SharedPreferences
                            preferences.edit().putString("user_data", new Gson().toJson(user)).apply();
                            displayUserInfo(user);
                        }
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            setupUI();
            setupListeners();
        } else {
            // Thay vì chuyển đến màn hình đăng nhập, quay về HomeFragment
            ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(0, true); // Chuyển đến trang Home (index 0)
                
                // Cập nhật trạng thái BottomNavigationView
                BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                }
                
                // Hiển thị thông báo về việc cần đăng nhập
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để truy cập trang cá nhân", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupToolbar() {
        binding.toolbar.setTitle("Hồ sơ");
    }
    
    private void checkNetworkState() {
        boolean isConnected = isNetworkConnected();
        binding.offlineLayout.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        binding.offlineIndicator.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        
        binding.btnRetry.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            checkNetworkState();
            if (isNetworkConnected()) {
                repository.getCurrentUserData(new FirebaseRepository.FirestoreCallback<users>() {
                    @Override
                    public void onCallback(users user) {
                        if (user != null) {
                            currentUser = user;
                            preferences.edit().putString("user_data", new Gson().toJson(user)).apply();
                            displayUserInfo(user);
                        }
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) requireActivity().getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private void setupUI() {
        // Hiển thị chế độ bảo vệ mắt nếu được bật
        binding.switchEyeProtection.setChecked(preferences.getBoolean("eye_protection_enabled", false));
    }
    
    private void setupListeners() {
        // Xử lý sự kiện khi bấm nút đăng xuất
        binding.logoutButton.setOnClickListener(v -> {
            // Hiển thị thông báo xác nhận trước khi đăng xuất
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        // Đăng xuất khỏi Firebase
                        repository.signOut();
                        
                        // Xóa dữ liệu người dùng khỏi SharedPreferences
                        preferences.edit().remove("user_data").apply();
                        
                        // Xóa dữ liệu cache nếu cần
                        clearUserCache();
                        
                        // Chuyển về HomeFragment thay vì mở LoginActivity và kết thúc activity hiện tại
                        // Cách 1: Chuyển về trang Home trong ViewPager
                        ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
                        if (viewPager != null) {
                            viewPager.setCurrentItem(0, true); // Chuyển đến trang Home (index 0)
                            
                            // Cập nhật trạng thái BottomNavigationView
                            BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
                            if (bottomNavigationView != null) {
                                bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                            }
                            
                            // Hiển thị thông báo
                            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                        } else {
                            // Backup: Nếu không tìm thấy ViewPager, khởi động lại MainActivity
                            Intent intent = new Intent(requireActivity(), MainContainerActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        }
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> {
                        // Không làm gì
                    })
                    .setCancelable(true)
                    .show();
        });
        
        // Xử lý sự kiện khi bấm vào nút lưu bài viết
        binding.savedArticlesButton.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), SavedArticlesActivity.class));
        });
        
        // Xử lý sự kiện khi bấm vào nút thay đổi avatar
        binding.btnChangeAvatar.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });
        
        // Xử lý sự kiện khi bật/tắt chế độ bảo vệ mắt
        binding.switchEyeProtection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean("eye_protection_enabled", isChecked).apply();
                EyeProtectionManager.applyEyeProtection(requireActivity(), isChecked);
                Toast.makeText(requireContext(), 
                        isChecked ? "Chế độ bảo vệ mắt đã bật" : "Chế độ bảo vệ mắt đã tắt", 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        // Xử lý sự kiện khi bấm vào thông tin cá nhân
        binding.personalInfoButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Tính năng thông tin cá nhân đang phát triển", Toast.LENGTH_SHORT).show();
            // TODO: Mở màn hình thông tin cá nhân
            // Intent intent = new Intent(requireActivity(), PersonalInfoActivity.class);
            // startActivity(intent);
        });
        
        // Xử lý sự kiện khi bấm vào đổi mật khẩu
        binding.changePasswordButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Tính năng đổi mật khẩu đang phát triển", Toast.LENGTH_SHORT).show();
            // TODO: Mở màn hình đổi mật khẩu
            // Intent intent = new Intent(requireActivity(), ChangePasswordActivity.class);
            // startActivity(intent);
        });
        
        // Xử lý sự kiện khi bấm vào thông tin ứng dụng
        binding.appInfoButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Thông tin ứng dụng VNews", Toast.LENGTH_SHORT).show();
            // TODO: Mở màn hình thông tin ứng dụng
            // Intent intent = new Intent(requireActivity(), AppInfoActivity.class);
            // startActivity(intent);
        });
        
        // Xử lý sự kiện khi bấm vào liên hệ hỗ trợ
        binding.contactSupportButton.setOnClickListener(v -> {
            // Mở email client để gửi email hỗ trợ
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@vnews.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ VNews - " + currentUser.getEmail());
            
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Xóa cache của người dùng
     */
    private void clearUserCache() {
        // Xóa cache của Glide nếu đã load avatar
        try {
            Glide.get(requireContext()).clearMemory();
            new Thread(() -> {
                try {
                    Glide.get(requireContext()).clearDiskCache();
                } catch (Exception e) {
                    Log.e("ProfileFragment", "Error clearing disk cache", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error clearing Glide cache", e);
        }
        
        // Xóa các SharedPreferences khác nếu có
        // preferences.edit().remove("other_user_data").apply();
    }
    
    private void displayUserInfo(users user) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                binding.userNameTextView.setText(user.getFullname());
                binding.userEmailTextView.setText(user.getEmail());
                
                // Hiển thị ảnh đại diện nếu có
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    Glide.with(this)
                            .load(user.getAvatar())
                            .apply(new RequestOptions()
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .circleCrop())
                            .into(binding.profileImageView);
                }
            });
        }
    }
    
    private void displayVersionInfo() {
        try {
            PackageInfo packageInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            binding.versionInfoText.setText("Phiên bản " + versionName + " (" + versionCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            binding.versionInfoText.setText("Phiên bản 1.0.0");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 