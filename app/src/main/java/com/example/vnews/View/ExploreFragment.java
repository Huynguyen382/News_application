package com.example.vnews.View;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vnews.Adapter.NewsAdapter;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.Repository.RssService;
import com.example.vnews.databinding.FragmentExploreBinding;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private NewsAdapter newsAdapter;
    private List<RssNewsItem> mostViewedNewsList;
    private RssService rssService;

    // RSS feed URL for most viewed articles
    private static final String MOST_VIEWED_RSS_URL = "https://vnexpress.net/rss/tin-xem-nhieu.rss";

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
        // Set up toolbar title
        binding.toolbar.setTitle("Khám phá");
        
        // Set up RecyclerView
        binding.mostViewedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.mostViewedRecyclerView.setAdapter(newsAdapter);
    }

    private void setupListeners() {
        // Set up SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMostViewedNews();
        });
    }

    private void loadMostViewedNews() {
        binding.swipeRefreshLayout.setRefreshing(true);
        
        rssService.fetchRssNews(MOST_VIEWED_RSS_URL, new RssService.OnFetchCompleteListener() {
            @Override
            public void onFetchComplete(List<RssNewsItem> fetchedNews) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        mostViewedNewsList.clear();
                        mostViewedNewsList.addAll(fetchedNews);
                        newsAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onFetchFailed(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 