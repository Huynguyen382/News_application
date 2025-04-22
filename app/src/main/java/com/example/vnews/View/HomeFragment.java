package com.example.vnews.View;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
    private List<RssNewsItem> filteredNewsList;
    private RssService rssService;
    
    private static final String RSS_FEED_URL = "https://vnexpress.net/rss/tin-moi-nhat.rss";
    private static final String FEATURED_RSS_URL = "https://vnexpress.net/rss/tin-noi-bat.rss";
    private static final String BACKUP_RSS_URL = "https://vnexpress.net/rss/thoi-su.rss";

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
        filteredNewsList = new ArrayList<>();
        
        // Initialize adapter with empty list
        newsAdapter = new NewsAdapter(requireContext(), filteredNewsList);

        setupUI();
        setupListeners();
        loadRssNews();
        loadFeaturedNews();
        
        // Thiết lập UI để ẩn bàn phím khi chạm vào nền
        setupUIToDismissKeyboard(view);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        updateWelcomeMessage();
        
        binding.newsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.newsRecyclerView.setAdapter(newsAdapter);
        
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
        
        // Thiết lập chức năng tìm kiếm
        binding.searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Khi người dùng focus vào ô tìm kiếm, hiển thị gợi ý tìm kiếm
                binding.searchEditText.setHint("Nhập từ khóa tìm kiếm...");
                
                // Hiển thị nút xóa khi focus vào ô tìm kiếm
                if (!binding.searchEditText.getText().toString().isEmpty()) {
                    binding.searchLayout.setEndIconVisible(true);
                }
                
                // Hiển thị bàn phím khi focus vào ô tìm kiếm
                showKeyboard(binding.searchEditText);
            } else {
                binding.searchEditText.setHint("Tìm kiếm tin tức");
                
                // Ẩn nút xóa nếu không có text
                if (binding.searchEditText.getText().toString().isEmpty()) {
                    binding.searchLayout.setEndIconVisible(false);
                }
            }
        });
        
        // Thiết lập nút xóa văn bản cho searchLayout
        binding.searchLayout.setEndIconDrawable(R.drawable.ic_clear);
        binding.searchLayout.setEndIconVisible(false);
        binding.searchLayout.setEndIconOnClickListener(v -> {
            binding.searchEditText.setText("");
            binding.searchLayout.setEndIconVisible(false);
            
            // Giữ focus sau khi xóa
            binding.searchEditText.requestFocus();
            showKeyboard(binding.searchEditText);
        });
        
        // Thêm TextWatcher để lọc tin tức khi người dùng nhập
        binding.searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không xử lý
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Kiểm soát hiển thị nút xóa
                binding.searchLayout.setEndIconVisible(!s.toString().isEmpty());
                
                // Lọc tin tức dựa trên văn bản được nhập
                filterNews(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Không xử lý
            }
        });
        
        binding.profileImage.setOnClickListener(v -> {
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
            Object linkObj = binding.featuredNewsTitle.getTag();
            if (linkObj != null) {
                String newsUrl = linkObj.toString();
                String title = binding.featuredNewsTitle.getText().toString();
                String description = binding.featuredNewsDescription.getText().toString();

                // Lấy URL hình ảnh thông qua tag của ImageView
                String imageUrl = "";
                Object imgUrlTag = binding.featuredNewsImage.getTag();
                if (imgUrlTag != null) {
                    imageUrl = imgUrlTag.toString();
                }
                
                Log.d("HomeFragment", "Opening featured news with URL: " + newsUrl);
                Log.d("HomeFragment", "Title: " + title);
                Log.d("HomeFragment", "Image URL: " + imageUrl);

                // Tạo Intent với đầy đủ thông tin
                Intent intent = new Intent(requireActivity(), NewsDetailActivity.class);
                intent.putExtra("article_url", newsUrl);
                intent.putExtra("article_title", title);
                intent.putExtra("article_description", description);
                intent.putExtra("article_image", imageUrl);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Không thể mở bài viết này", Toast.LENGTH_SHORT).show();
            }
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
                        
                        // Reset filtered list
                        filteredNewsList.clear();
                        filteredNewsList.addAll(fetchedNews);
                        
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
        // Thử tải tin nổi bật từ URL chính
        rssService.fetchRssNews(FEATURED_RSS_URL, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded() && fetchedNews != null && !fetchedNews.isEmpty()) {
                    processFeaturedNews(fetchedNews);
                } else {
                    // Nếu không nhận được dữ liệu từ URL chính, thử dùng URL dự phòng
                    tryBackupRssSource();
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                Log.e("HomeFragment", "Failed to fetch featured news", e);
                if (isAdded()) {
                    // Nếu có lỗi, thử dùng URL dự phòng
                    tryBackupRssSource();
                }
            }
        });
    }
    
    private void tryBackupRssSource() {
        Log.d("HomeFragment", "Trying backup RSS source");
        rssService.fetchRssNews(BACKUP_RSS_URL, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded() && fetchedNews != null && !fetchedNews.isEmpty()) {
                    processFeaturedNews(fetchedNews);
                } else {
                    requireActivity().runOnUiThread(() -> {
                        setFallbackImage();
                        binding.featuredNewsTitle.setText("Không thể tải tin nổi bật");
                        binding.featuredNewsDescription.setText("Vui lòng kiểm tra kết nối internet và thử lại sau");
                    });
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                Log.e("HomeFragment", "Failed to fetch backup news", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        setFallbackImage();
                        binding.featuredNewsTitle.setText("Không thể tải tin nổi bật");
                        binding.featuredNewsDescription.setText("Vui lòng kiểm tra kết nối internet và thử lại sau");
                    });
                }
            }
        });
    }
    
    private void processFeaturedNews(List<RssNewsItem> fetchedNews) {
        requireActivity().runOnUiThread(() -> {
            // Lấy tin đầu tiên làm tin nổi bật
            RssNewsItem featuredItem = fetchedNews.get(0);
            
            // Kiểm tra xem tin nổi bật có dữ liệu hợp lệ không
            if (featuredItem.getTitle() == null || featuredItem.getTitle().isEmpty()) {
                binding.featuredNewsTitle.setText("Tin tức mới nhất");
            } else {
                binding.featuredNewsTitle.setText(featuredItem.getTitle());
            }
            
            if (featuredItem.getDescription() == null || featuredItem.getDescription().isEmpty()) {
                binding.featuredNewsDescription.setText("Không có mô tả");
            } else {
                binding.featuredNewsDescription.setText(featuredItem.getCleanDescription());
            }
            
            // Lưu link để mở khi click
            binding.featuredNewsTitle.setTag(featuredItem.getLink());
            
            // Tải ảnh nếu có
            loadFeaturedImage(featuredItem);
        });
    }
    
    private void loadFeaturedImage(RssNewsItem featuredItem) {
        String imageUrl = featuredItem.getImageUrl();
        Log.d("HomeFragment", "Attempting to load featuredNewsImage with URL: " + imageUrl);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Lưu URL hình ảnh vào tag của ImageView để sử dụng sau này
            binding.featuredNewsImage.setTag(imageUrl);
            
            // Thử tải ảnh với URL được cung cấp
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.news_placeholder)
                .error(R.drawable.news_placeholder)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        Log.e("HomeFragment", "Failed to load image: " + imageUrl, e);
                        // Thử phương pháp lấy ảnh thay thế
                        tryAlternativeImageLoading(featuredItem);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Log.d("HomeFragment", "Image loaded successfully: " + imageUrl);
                        return false;
                    }
                })
                .centerCrop()
                .into(binding.featuredNewsImage);
        } else {
            // Thử phương pháp lấy ảnh thay thế nếu không có URL
            tryAlternativeImageLoading(featuredItem);
        }
    }
    
    private void tryAlternativeImageLoading(RssNewsItem featuredItem) {
        // Sử dụng phương thức getEffectiveImageUrl để lấy URL hiệu quả hơn
        String effectiveUrl = featuredItem.getEffectiveImageUrl();
        Log.d("HomeFragment", "Effective image URL: " + effectiveUrl);
        
        if (effectiveUrl != null && !effectiveUrl.isEmpty()) {
            // Lưu URL hình ảnh hiệu quả vào tag của ImageView
            binding.featuredNewsImage.setTag(effectiveUrl);
            
            Glide.with(requireContext())
                .load(effectiveUrl)
                .placeholder(R.drawable.news_placeholder)
                .error(R.drawable.news_placeholder)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        Log.e("HomeFragment", "Failed to load effective image: " + effectiveUrl, e);
                        
                        // Thử trực tiếp với Jsoup nếu vẫn thất bại
                        String description = featuredItem.getDescription();
                        if (description != null && description.contains("<img")) {
                            try {
                                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(description);
                                org.jsoup.nodes.Element imgElement = doc.select("img").first();
                                if (imgElement != null) {
                                    String jsoupImageUrl = imgElement.attr("src");
                                    Log.d("HomeFragment", "Extracted image URL with Jsoup: " + jsoupImageUrl);
                                    if (jsoupImageUrl != null && !jsoupImageUrl.isEmpty()) {
                                        // Lưu URL hình ảnh từ Jsoup vào tag
                                        binding.featuredNewsImage.setTag(jsoupImageUrl);
                                        
                                        Glide.with(requireContext())
                                            .load(jsoupImageUrl)
                                            .placeholder(R.drawable.news_placeholder)
                                            .error(R.drawable.news_placeholder)
                                            .centerCrop()
                                            .into(binding.featuredNewsImage);
                                        return true;
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e("HomeFragment", "Error extracting image with Jsoup", ex);
                            }
                        }
                        
                        // Nếu tất cả các phương pháp đều thất bại, hiển thị ảnh dự phòng
                        setFallbackImage();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        Log.d("HomeFragment", "Effective image loaded successfully: " + effectiveUrl);
                        return false;
                    }
                })
                .centerCrop()
                .into(binding.featuredNewsImage);
        } else {
            setFallbackImage();
        }
    }
    
    private void setFallbackImage() {
        // Đặt ảnh mặc định khi không có ảnh và xóa tag URL
        binding.featuredNewsImage.setTag(null);
        Glide.with(requireContext())
            .load(R.drawable.news_placeholder)
            .centerCrop()
            .into(binding.featuredNewsImage);
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
                        
                        // Clear search text when changing category
                        binding.searchEditText.setText("");
                        
                        // Update filtered list
                        filteredNewsList.clear();
                        filteredNewsList.addAll(fetchedNews);
                        
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
    
    /**
     * Lọc danh sách tin tức dựa trên từ khóa tìm kiếm
     * @param query chuỗi tìm kiếm
     */
    private void filterNews(String query) {
        if (query.isEmpty()) {
            // Nếu không có từ khóa tìm kiếm, hiển thị tất cả tin tức
            filteredNewsList.clear();
            filteredNewsList.addAll(newsList);
            binding.searchResultsCount.setVisibility(View.GONE);
            binding.noResultsView.setVisibility(View.GONE);
        } else {
            // Lọc tin tức dựa trên từ khóa tìm kiếm (không phân biệt chữ hoa/thường)
            String lowerCaseQuery = query.toLowerCase();
            
            filteredNewsList.clear();
            
            for (RssNewsItem item : newsList) {
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery) || 
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery))) {
                    filteredNewsList.add(item);
                }
            }
            
            // Hiển thị kết quả tìm kiếm
            if (filteredNewsList.isEmpty()) {
                binding.noResultsView.setVisibility(View.VISIBLE);
                binding.searchResultsCount.setVisibility(View.GONE);
            } else {
                binding.noResultsView.setVisibility(View.GONE);
                binding.searchResultsCount.setVisibility(View.VISIBLE);
                binding.searchResultsCount.setText(getString(R.string.search_results_count, 
                        filteredNewsList.size(), query));
            }
        }
        
        // Cập nhật adapter
        newsAdapter.notifyDataSetChanged();
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
        intent.putExtra("article_url", newsUrl);
        startActivity(intent);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    /**
     * Thiết lập UI để ẩn bàn phím khi chạm bên ngoài EditText
     */
    private void setupUIToDismissKeyboard(View view) {
        // Nếu không phải là EditText, thêm sự kiện chạm để ẩn bàn phím
        if (!(view instanceof android.widget.EditText)) {
            view.setOnTouchListener((v, event) -> {
                if (binding != null && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    clearSearchEditTextFocus();
                }
                return false;
            });
        }
        
        // Nếu là ViewGroup, thiết lập đệ quy cho các view con
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                setupUIToDismissKeyboard(childView);
            }
        }
    }
    
    /**
     * Ẩn bàn phím và xóa focus khỏi ô tìm kiếm
     */
    private void clearSearchEditTextFocus() {
        hideKeyboard();
        if (binding.searchEditText.hasFocus()) {
            binding.searchEditText.clearFocus();
        }
    }

    /**
     * Ẩn bàn phím
     */
    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        View focusedView = requireActivity().getCurrentFocus();
        if (imm != null && focusedView != null) {
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    /**
     * Hiển thị bàn phím
     */
    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
} 