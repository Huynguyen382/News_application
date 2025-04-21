package com.example.vnews.Adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.vnews.Model.RssNewsItem;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.View.NewsDetailActivity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<RssNewsItem> newsList;
    private final Context context;
    private final FirebaseRepository repository;
    private static final long LONG_PRESS_DURATION = 1000; // 1 giây
    private OnItemSwipeListener swipeListener;
    private boolean isFromSavedArticles = false;

    public interface OnItemSwipeListener {
        void onItemSwiped(int position);
    }

    public NewsAdapter(Context context, List<RssNewsItem> newsList) {
        this.context = context;
        this.newsList = newsList != null ? newsList : new ArrayList<>();
        this.repository = new FirebaseRepository();
    }

    public void setFromSavedArticles(boolean fromSavedArticles) {
        this.isFromSavedArticles = fromSavedArticles;
    }

    public void setOnItemSwipeListener(OnItemSwipeListener listener) {
        this.swipeListener = listener;
    }

    public RssNewsItem getItemAtPosition(int position) {
        if (position < 0 || position >= newsList.size()) {
            return null;
        }
        return newsList.get(position);
    }

    public void removeItem(int position) {
        if (position < 0 || position >= newsList.size()) {
            return;
        }
        newsList.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        if (newsList == null || newsList.isEmpty() || position >= newsList.size()) {
            return;
        }
        
        RssNewsItem news = newsList.get(position);
        if (news == null) {
            return;
        }
        
        // Hiển thị tiêu đề - đảm bảo không mất dấu tiếng Việt
        String title = news.getTitle();
        if (title != null) {
            // Kiểm tra xem có cần chuyển đổi mã hóa không
            if (!containsVietnameseCharacters(title) && title.matches(".*[\\u00C0-\\u00FF].*")) {
                try {
                    // Thử chuyển từ ISO-8859-1 sang UTF-8
                    byte[] bytes = title.getBytes(StandardCharsets.ISO_8859_1);
                    title = new String(bytes, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Log.e("NewsAdapter", "Error converting title: " + title, e);
                }
            }
        }
        holder.newsTitle.setText(title);
        
        // Hiển thị mô tả - đảm bảo không mất dấu tiếng Việt
        String description = news.getCleanDescription();
        if (description != null) {
            // Kiểm tra xem có cần chuyển đổi mã hóa không
            if (!containsVietnameseCharacters(description) && description.matches(".*[\\u00C0-\\u00FF].*")) {
                try {
                    // Thử chuyển từ ISO-8859-1 sang UTF-8
                    byte[] bytes = description.getBytes(StandardCharsets.ISO_8859_1);
                    description = new String(bytes, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Log.e("NewsAdapter", "Error converting description: " + description, e);
                }
            }
        }
        holder.newsDescription.setText(description);
        
        // Hiển thị ngày
        holder.newsDate.setText(news.getPubDate());
        
        // Load image using Glide with improved settings
        if (news.getImageUrl() != null && !news.getImageUrl().isEmpty()) {
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image);
            
            Glide.with(context.getApplicationContext())
                    .load(news.getImageUrl())
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
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
            intent.putExtra("article_description", news.getDescription());
            intent.putExtra("article_pubDate", news.getPubDate());
            context.startActivity(intent);
        });

        // Chỉ hiển thị chức năng long press để lưu khi không phải từ SavedArticles
        if (!isFromSavedArticles) {
            setupLongPressListener(holder.cardView, news);
        }
    }

    private void setupLongPressListener(View view, RssNewsItem news) {
        final Handler handler = new Handler();
        final Runnable longPressRunnable = () -> saveArticle(news);

        view.setOnLongClickListener(v -> {
            handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
            // Hiển thị thông báo nhỏ
            Toast.makeText(context, "Giữ thêm 1 giây để lưu bài viết", Toast.LENGTH_SHORT).show();
            return true;
        });

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(longPressRunnable);
                    break;
            }
            return false;
        });
    }

    private void saveArticle(RssNewsItem news) {
        // Kiểm tra người dùng đã đăng nhập chưa
        if (!repository.isUserLoggedIn()) {
            Toast.makeText(context, "Vui lòng đăng nhập để lưu bài viết", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = repository.getCurrentUserId();
        String articleId = news.getLink(); // Sử dụng URL làm ID của bài viết

        // Kiểm tra xem bài viết đã được lưu chưa
        repository.isArticleSaved(userId, articleId, new FirebaseRepository.FirestoreCallback<Boolean>() {
            @Override
            public void onCallback(Boolean isSaved) {
                if (isSaved) {
                    Toast.makeText(context, "Bài viết này đã được lưu trước đó", Toast.LENGTH_SHORT).show();
                } else {
                    // Lưu bài viết
                    repository.saveArticle(userId, articleId, new FirebaseRepository.FirestoreCallback<String>() {
                        @Override
                        public void onCallback(String result) {
                            Toast.makeText(context, "Đã lưu bài viết thành công", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(context, "Lỗi khi lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(context, "Lỗi khi kiểm tra bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return newsList != null ? newsList.size() : 0;
    }

    // Add items to adapter
    public void updateNewsList(List<RssNewsItem> newsList) {
        if (this.newsList == null) {
            this.newsList = new ArrayList<>();
        }
        this.newsList.clear();
        if (newsList != null) {
            this.newsList.addAll(newsList);
        }
        notifyDataSetChanged();
    }

    private boolean containsVietnameseCharacters(String text) {
        if (text == null) return false;
        return text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸ].*");
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