package com.example.vnnews.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.vnnews.R;
import com.example.vnnews.databinding.ItemNewsBinding;
import com.example.vnnews.model.News;
import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private final Context context;
    private final List<News> newsList;
    private final OnNewsClickListener listener;

    public interface OnNewsClickListener {
        void onNewsClick(News news);
    }

    public NewsAdapter(Context context, OnNewsClickListener listener) {
        this.context = context;
        this.newsList = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNewsBinding binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new NewsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);
        holder.bind(news);
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public void setNewsList(List<News> newsList) {
        this.newsList.clear();
        this.newsList.addAll(newsList);
        notifyDataSetChanged();
    }

    public void addNews(List<News> newsList) {
        int startPosition = this.newsList.size();
        this.newsList.addAll(newsList);
        notifyItemRangeInserted(startPosition, newsList.size());
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewsBinding binding;

        NewsViewHolder(ItemNewsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(News news) {
            // Load image
            Glide.with(context)
                .load(news.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.newsImage);

            // Set texts
            binding.newsCategory.setText(news.getCategory());
            binding.newsTitle.setText(news.getTitle());
            binding.newsDescription.setText(news.getDescription());
            binding.newsSource.setText(news.getSource());
            binding.newsTime.setText(DateUtils.getRelativeTimeSpanString(
                news.getPublishedDate().getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ));
            binding.newsViews.setText(String.valueOf(news.getViews()));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNewsClick(news);
                }
            });
        }
    }
} 