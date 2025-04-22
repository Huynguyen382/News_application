package com.example.vnews.View;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.vnews.Adapter.NewsAdapter;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Repository.RssService;
import com.example.vnews.Utils.EyeProtectionManager;
import com.example.vnews.databinding.FragmentHomeBinding;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseRepository repository;
    private SharedPreferences preferences;
    private NewsAdapter newsAdapter;
    private List<RssNewsItem> newsList;
    private RssService rssService;
    
    // VnExpress RSS feed URL
    private static final String RSS_FEED_URL = "https://vnexpress.net/rss/tin-moi-nhat.rss";
    private static final String FEATURED_RSS_URL = "https://vnexpress.net/rss/tin-noi-bat.rss";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        preferences = requireActivity().getSharedPreferences("VNNews", MODE_PRIVATE);
        repository = new FirebaseRepository();
        rssService = new RssService();
        newsList = new ArrayList<>();
        
        // Initialize adapter with empty list
        newsAdapter = new NewsAdapter(requireContext(), newsList);

        setupUI();
        setupListeners();
        loadRssNews();
        loadFeaturedNews();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra lại trạng thái đăng nhập và cập nhật UI mỗi khi fragment được hiển thị
        updateWelcomeMessage();
    }
    
    /**
     * Cập nhật thông báo chào mừng dựa trên trạng thái đăng nhập
     */
    private void updateWelcomeMessage() {
        // Kiểm tra lại trạng thái đăng nhập
        if (repository.isUserLoggedIn()) {
            String userJson = preferences.getString("user_data", "");
            if (!userJson.isEmpty()) {
                com.example.vnews.Model.users user = new com.google.gson.Gson().fromJson(userJson, com.example.vnews.Model.users.class);
                // Log để debug
                android.util.Log.d("HomeFragment", "User fullname: " + user.getFullname());
                
                String displayName = user.getFullname();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = user.getUsername(); // Sử dụng username nếu fullname trống
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = "bạn"; // Fallback nếu cả hai đều trống
                    }
                }
                
                binding.welcomeText.setText(getString(R.string.welcome_user_name, displayName));
                
                // Tải ảnh đại diện nếu có
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    Glide.with(this)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .circleCrop()
                        .into(binding.profileImage);
                }
            } else {
                // Người dùng đã đăng nhập nhưng không có dữ liệu trong SharedPreferences
                tryGetUserDataFromFirebase();
            }
        } else {
            // Khi chưa đăng nhập, hiển thị "Xin chào, Khách" và đặt ảnh mặc định
            binding.welcomeText.setText(getString(R.string.welcome_user_name, "Khách"));
            Glide.with(this)
                .load(R.drawable.default_avatar)
                .circleCrop()
                .into(binding.profileImage);
                
            // Xóa thông tin user cũ khỏi SharedPreferences để tránh hiển thị dữ liệu cũ
            preferences.edit().remove("user_data").apply();
        }
    }
    
    /**
     * Tải dữ liệu người dùng từ Firebase
     */
    private void tryGetUserDataFromFirebase() {
        repository.getCurrentUserData(new FirebaseRepository.FirestoreCallback<com.example.vnews.Model.users>() {
            @Override
            public void onCallback(com.example.vnews.Model.users user) {
                if (user != null && isAdded()) {
                    String displayName = user.getFullname();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = user.getUsername();
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = "bạn";
                        }
                    }
                    
                    binding.welcomeText.setText(getString(R.string.welcome_user_name, displayName));
                    
                    // Lưu thông tin người dùng vào SharedPreferences
                    preferences.edit().putString("user_data", new com.google.gson.Gson().toJson(user)).apply();
                    
                    // Tải ảnh đại diện nếu có
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        Glide.with(HomeFragment.this)
                            .load(user.getAvatar())
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .circleCrop()
                            .into(binding.profileImage);
                    }
                }
            }
            
            @Override
            public void onError(Exception e) {
                android.util.Log.e("HomeFragment", "Error getting user data: " + e.getMessage());
                if (isAdded()) {
                    binding.welcomeText.setText(getString(R.string.welcome_user_name, "bạn"));
                }
            }
        });
    }

    private void setupUI() {
        // Cập nhật trạng thái đăng nhập và hiển thị thông báo chào mừng
        updateWelcomeMessage();
        
        // Set up RecyclerView
        binding.newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.newsRecyclerView.setAdapter(newsAdapter);
        
        // Hiển thị ngày hiện tại
        updateCurrentDate();
        
        // Thiết lập TabLayout
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Chọn danh mục tin tức dựa trên tab được chọn
                loadNewsByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Tải lại tin tức khi tab được chọn lại
                loadNewsByCategory(tab.getPosition());
            }
        });
        
        // Thiết lập sự kiện cho TextInputEditText
        binding.searchEditText.setOnClickListener(v -> {
            // Chuyển sang màn hình tìm kiếm (ExploreFragment)
            navigateToFragment(1);
        });
        
        // Thiết lập sự kiện cho ảnh đại diện
        binding.profileImage.setOnClickListener(v -> {
            // Chuyển đến màn hình Profile
            if (repository.isUserLoggedIn()) {
                navigateToFragment(2); // Index của ProfileFragment trong ViewPager
            } else {
                // Khi chưa đăng nhập, hiển thị hộp thoại xác nhận
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                builder.setTitle("Đăng nhập")
                        .setMessage("Bạn cần đăng nhập để xem trang cá nhân")
                        .setPositiveButton("Đăng nhập", (dialog, which) -> {
                            // Chuyển đến màn hình đăng nhập
                            startActivity(new Intent(requireActivity(), LoginActivity.class));
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            // Không làm gì
                        })
                        .setCancelable(true)
                        .show();
            }
        });
    }
    
    private void setupListeners() {
        // Set up SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadRssNews();
            loadFeaturedNews();
        });
        
        // Thiết lập sự kiện cho tin nổi bật
        binding.featuredNewsCard.setOnClickListener(v -> {
            openNewsDetail(binding.featuredNewsTitle.getTag().toString());
        });
    }
    
    private void loadRssNews() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        rssService.fetchRssNews(RSS_FEED_URL, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        newsList.clear();
                        newsList.addAll(fetchedNews);
                        newsAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }
    
    private void loadFeaturedNews() {
        rssService.fetchRssNews(FEATURED_RSS_URL, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded() && !fetchedNews.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        // Lấy tin đầu tiên làm tin nổi bật
                        RssNewsItem featuredItem = fetchedNews.get(0);
                        
                        // Hiển thị thông tin tin nổi bật
                        binding.featuredNewsTitle.setText(featuredItem.getTitle());
                        binding.featuredNewsDescription.setText(featuredItem.getDescription());
                        
                        // Lưu link để mở khi click
                        binding.featuredNewsTitle.setTag(featuredItem.getLink());
                        
                        // Tải ảnh nếu có
                        if (featuredItem.getImageUrl() != null && !featuredItem.getImageUrl().isEmpty()) {
                            Glide.with(requireContext())
                                .load(featuredItem.getImageUrl())
                                .centerCrop()
                                .into(binding.featuredNewsImage);
                        }
                    });
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                // Xử lý lỗi nếu cần
            }
        });
    }
    
    private void loadNewsByCategory(int tabPosition) {
        String rssUrl;
        
        switch (tabPosition) {
            case 0: // Tất cả
                rssUrl = "https://vnexpress.net/rss/tin-moi-nhat.rss";
                break;
            case 1: // Chính trị
                rssUrl = "https://vnexpress.net/rss/thoi-su.rss";
                break;
            case 2: // Kinh doanh
                rssUrl = "https://vnexpress.net/rss/kinh-doanh.rss";
                break;
            case 3: // Thể thao
                rssUrl = "https://vnexpress.net/rss/the-thao.rss";
                break;
            default:
                rssUrl = "https://vnexpress.net/rss/tin-moi-nhat.rss";
                break;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        rssService.fetchRssNews(rssUrl, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        newsList.clear();
                        newsList.addAll(fetchedNews);
                        newsAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }
    
    private void updateCurrentDate() {
        // Lấy và hiển thị ngày hiện tại
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", java.util.Locale.getDefault());
        String currentDate = dateFormat.format(new java.util.Date());
        binding.dateTextView.setText(currentDate);
    }
    
    private void navigateToFragment(int position) {
        // Chuyển đến Fragment khác trong ViewPager
        androidx.viewpager2.widget.ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
        if (viewPager != null) {
            viewPager.setCurrentItem(position, true);
        }
    }
    
    private void openNewsDetail(String newsUrl) {
        // Mở chi tiết tin tức
        Intent intent = new Intent(requireActivity(), NewsDetailActivity.class);
        intent.putExtra("news_url", newsUrl);
        startActivity(intent);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 