package com.example.vnews.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.R;
import com.example.vnews.View.NewsDetailActivity;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<RssNewsItem> newsList;
    private final Context context;

    public NewsAdapter(Context context, List<RssNewsItem> newsList) {
        this.context = context;
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        RssNewsItem news = newsList.get(position);
        
        holder.newsTitle.setText(news.getTitle());
        holder.newsDescription.setText(news.getCleanDescription());
        holder.newsDate.setText(news.getPubDate());
        
        // Load image using Glide
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(news.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.newsImage);
        } else {
            holder.newsImage.setImageResource(R.drawable.placeholder_image);
        }
        
        // Set click listener
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NewsDetailActivity.class);
            intent.putExtra("article_title", news.getTitle());
            intent.putExtra("article_url", news.getLink());
            intent.putExtra("article_image", news.getImageUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    // Add items to adapter
    public void updateNewsList(List<RssNewsItem> newsList) {
        this.newsList.clear();
        this.newsList.addAll(newsList);
        notifyDataSetChanged();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView newsTitle, newsDescription, newsDate;
        ImageView newsImage;
        CardView cardView;

        NewsViewHolder(View itemView) {
            super(itemView);
            newsTitle = itemView.findViewById(R.id.news_title);
            newsDescription = itemView.findViewById(R.id.news_description);
            newsDate = itemView.findViewById(R.id.news_date);
            newsImage = itemView.findViewById(R.id.news_image);
            cardView = itemView.findViewById(R.id.news_card);
        }
    }
} 