package com.example.vnews.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vnews.Adapter.NewsAdapter;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.Repository.RssService;
import com.example.vnews.databinding.FragmentExploreBinding;
import com.example.vnews.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    public static final String MOST_VIEWED_RSS_URL = "https://vnexpress.net/rss/du-lich.rss";
    public static final String TECHNOLOGY_RSS_URL = "https://vnexpress.net/rss/so-hoa.rss";
    public static final String BUSINESS_RSS_URL = "https://vnexpress.net/rss/kinh-doanh.rss";
    public static final String SPORTS_RSS_URL = "https://vnexpress.net/rss/the-thao.rss";
    public static final String ENTERTAINMENT_RSS_URL = "https://vnexpress.net/rss/giai-tri.rss";

    private FragmentExploreBinding binding;
    private NewsAdapter newsAdapter;
    private List<RssNewsItem> mostViewedNewsList;
    private RssService rssService;
    private String currentRssUrl = MOST_VIEWED_RSS_URL;
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rssService = new RssService();
        mostViewedNewsList = new ArrayList<>();
        
        // Initialize adapter with empty list
        newsAdapter = new NewsAdapter(requireContext(), mostViewedNewsList);

        setupUI();
        setupListeners();
        loadMostViewedNews();
    }

    private void setupUI() {
        // Set up RecyclerView
        binding.mostViewedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.mostViewedRecyclerView.setAdapter(newsAdapter);

        // Mở rộng phạm vi chạm của thanh tìm kiếm
        binding.searchView.setIconifiedByDefault(false);
        binding.searchView.setQueryHint("Tìm kiếm tin tức...");
        
        // Tăng kích thước của thanh tìm kiếm
        View searchPlate = binding.searchView.findViewById(R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackgroundResource(android.R.color.transparent);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) searchPlate.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            searchPlate.setLayoutParams(params);
        }

        // Tăng kích thước của icon tìm kiếm
        ImageView searchIcon = binding.searchView.findViewById(R.id.search_mag_icon);
        if (searchIcon != null) {
            ViewGroup.LayoutParams params = searchIcon.getLayoutParams();
            params.width = (int) (48 * getResources().getDisplayMetrics().density);
            params.height = (int) (48 * getResources().getDisplayMetrics().density);
            searchIcon.setLayoutParams(params);
        }

        // Tăng kích thước của nút xóa
        ImageView closeButton = binding.searchView.findViewById(R.id.search_close_btn);
        if (closeButton != null) {
            ViewGroup.LayoutParams params = closeButton.getLayoutParams();
            params.width = (int) (48 * getResources().getDisplayMetrics().density);
            params.height = (int) (48 * getResources().getDisplayMetrics().density);
            closeButton.setLayoutParams(params);
        }
    }

    private void setupListeners() {
        // SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMostViewedNews();
        });

        // SearchView
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                filterNews();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                filterNews();
                return true;
            }
        });

        // TabLayout
        binding.categoriesTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentRssUrl = MOST_VIEWED_RSS_URL;
                        break;
                    case 1:
                        currentRssUrl = TECHNOLOGY_RSS_URL;
                        break;
                    case 2:
                        currentRssUrl = BUSINESS_RSS_URL;
                        break;
                    case 3:
                        currentRssUrl = SPORTS_RSS_URL;
                        break;
                    case 4:
                        currentRssUrl = ENTERTAINMENT_RSS_URL;
                        break;
                }
                loadMostViewedNews();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }
        });
    }

    private void loadMostViewedNews() {
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.loadingProgressBar.setVisibility(View.VISIBLE);
        binding.emptyStateTextView.setVisibility(View.GONE);
        binding.mostViewedRecyclerView.setVisibility(View.GONE);

        rssService.fetchRssNews(currentRssUrl, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        binding.loadingProgressBar.setVisibility(View.GONE);
                        mostViewedNewsList.clear();
                        mostViewedNewsList.addAll(fetchedNews);
                        filterNews();
                    });
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        binding.loadingProgressBar.setVisibility(View.GONE);
                        binding.mostViewedRecyclerView.setVisibility(View.GONE);
                        binding.emptyStateTextView.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void filterNews() {
        List<RssNewsItem> filteredList = new ArrayList<>();
        for (RssNewsItem item : mostViewedNewsList) {
            if (item.getTitle().toLowerCase().contains(currentQuery.toLowerCase()) ||
                item.getDescription().toLowerCase().contains(currentQuery.toLowerCase())) {
                filteredList.add(item);
            }
        }
        newsAdapter.updateNewsList(filteredList);
        if (filteredList.isEmpty()) {
            binding.mostViewedRecyclerView.setVisibility(View.GONE);
            binding.emptyStateTextView.setVisibility(View.VISIBLE);
        } else {
            binding.mostViewedRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyStateTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 