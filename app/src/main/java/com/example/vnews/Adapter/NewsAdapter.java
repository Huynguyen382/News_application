package com.example.vnews.Adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import androidx.core.content.ContextCompat;

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
            // Loại bỏ dãy số ở cuối nếu có
            title = title.replaceAll("\\s*\\d+$", "").trim();
            
            // Kiểm tra xem có cần chuyển đổi mã hóa không
            if (!containsVietnameseCharacters(title) && title.matches(".*[\\u00C0-\\u00FF].*")) {
                try {
                    // Kiểm tra nếu có byte không hợp lệ trong UTF-8
                    byte[] bytesUTF8 = title.getBytes(StandardCharsets.UTF_8);
                    String utf8Check = new String(bytesUTF8, StandardCharsets.UTF_8);
                    
                    if (!title.equals(utf8Check)) {
                        // Thử chuyển từ ISO-8859-1 sang UTF-8
                        byte[] bytes = title.getBytes(StandardCharsets.ISO_8859_1);
                        title = new String(bytes, StandardCharsets.UTF_8);
                        
                        // Nếu vẫn không có dấu, thử Windows-1252
                        if (!containsVietnameseCharacters(title)) {
                            byte[] bytesWin = title.getBytes("windows-1252");
                            String winTitle = new String(bytesWin, StandardCharsets.UTF_8);
                            if (containsVietnameseCharacters(winTitle)) {
                                title = winTitle;
                            }
                        }
                    }
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
                    // Kiểm tra nếu có byte không hợp lệ trong UTF-8
                    byte[] bytesUTF8 = description.getBytes(StandardCharsets.UTF_8);
                    String utf8Check = new String(bytesUTF8, StandardCharsets.UTF_8);
                    
                    if (!description.equals(utf8Check)) {
                        // Thử chuyển từ ISO-8859-1 sang UTF-8
                        byte[] bytes = description.getBytes(StandardCharsets.ISO_8859_1);
                        description = new String(bytes, StandardCharsets.UTF_8);
                        
                        // Nếu vẫn không có dấu, thử Windows-1252
                        if (!containsVietnameseCharacters(description)) {
                            byte[] bytesWin = description.getBytes("windows-1252");
                            String winDesc = new String(bytesWin, StandardCharsets.UTF_8);
                            if (containsVietnameseCharacters(winDesc)) {
                                description = winDesc;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("NewsAdapter", "Error converting description: " + description, e);
                }
            }
        }
        holder.newsDescription.setText(description);
        
        // Hiển thị ngày
        holder.newsDate.setText(news.getPubDate());
        
        // Load image using Glide with improved settings
        String imageUrl = news.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            // Đặt một URL mặc định để tránh lỗi null
            imageUrl = "https://placeholder.com/wp-content/uploads/2018/10/placeholder.png";
            Log.d("NewsAdapter", "Sử dụng URL placeholder cho item có tiêu đề: " + news.getTitle());
        }
        
        try {
            // Log URL gốc để debug
            Log.d("NewsAdapter", "Original image URL: " + imageUrl);
            
            // Đảm bảo URL bắt đầu bằng http hoặc https
            if (!imageUrl.startsWith("http")) {
                imageUrl = "https:" + imageUrl;
            }
            
            // Loại bỏ các ký tự không hợp lệ trong URL
            if (imageUrl.contains(" ")) {
                imageUrl = imageUrl.replaceAll(" ", "%20");
            }
            
            Log.d("NewsAdapter", "Final image URL: " + imageUrl);
            
            // Sử dụng RequestOptions để cấu hình Glide
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .timeout(15000) // 15 giây timeout
                    .fallback(R.drawable.placeholder_image) // Fallback khi URL là null
                    .dontTransform() // Không biến đổi hình ảnh, tăng khả năng load thành công
                    .dontAnimate(); // Không animation, giảm lỗi
            
            // Tải hình ảnh với Glide - sử dụng cả asGif() để hỗ trợ GIF
            Glide.with(context.getApplicationContext())
                    .load(imageUrl)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.newsImage);
            
        } catch (Exception e) {
            Log.e("NewsAdapter", "Error loading image: " + imageUrl, e);
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

        // Đặt màu nền mặc định cho tất cả các bài viết
        holder.cardView.setCardBackgroundColor(getCardBackgroundColor());

        // Chỉ hiển thị chức năng long press để lưu khi không phải từ SavedArticles
        if (!isFromSavedArticles) {
            setupLongPressListener(holder.cardView, news);
        }
    }

    private void setupLongPressListener(CardView cardView, RssNewsItem news) {
        final Handler handler = new Handler();
        final Runnable longPressRunnable = () -> {
            // Provide vibration feedback when the long press duration is reached
            provideVibrationFeedback();
            saveArticle(cardView, news);
        };

        // Flag để kiểm soát hiệu ứng màu
        final boolean[] isLongPressing = {false};

        cardView.setOnLongClickListener(v -> {
            // Reset trạng thái
            isLongPressing[0] = true;
            
            // Thay đổi màu nền với animation khi bắt đầu nhấn giữ
            animateCardBackground(cardView, 
                    getCardBackgroundColor(), 
                    getCardLongPressColor(), 
                    300); // Thời gian animation 300ms
            
            // Provide short vibration feedback when starting long press
            provideShortVibrationFeedback();
            
            handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
            // Hiển thị thông báo nhỏ
            Toast.makeText(context, "Giữ thêm 1 giây để lưu bài viết", Toast.LENGTH_SHORT).show();
            return true;
        });

        cardView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(longPressRunnable);
                    
                    // Khi thả tay, kiểm tra xem bài viết đã được lưu chưa
                    if (isLongPressing[0]) {
                        isLongPressing[0] = false;
                        checkAndRestoreCardColor(cardView, news);
                    }
                    break;
            }
            return false;
        });
    }
    
    // Phương thức riêng để kiểm tra và khôi phục màu sắc card dựa trên trạng thái lưu
    private void checkAndRestoreCardColor(CardView cardView, RssNewsItem news) {
        // Luôn trả về màu trắng khi thả tay, bất kể trạng thái lưu
        animateCardBackground(cardView, 
                getCardLongPressColor(), 
                getCardBackgroundColor(), 
                300);
    }

    private void saveArticle(CardView cardView, RssNewsItem news) {
        // Kiểm tra người dùng đã đăng nhập chưa
        if (!repository.isUserLoggedIn()) {
            Toast.makeText(context, "Vui lòng đăng nhập để lưu bài viết", Toast.LENGTH_SHORT).show();
            animateCardBackground(cardView, 
                    getCardLongPressColor(), 
                    getCardBackgroundColor(), 
                    300);
            return;
        }

        String userId = repository.getCurrentUserId();
        
        // Kiểm tra xem bài viết đã được lưu chưa
        repository.isArticleSaved(userId, news.getLink(), new FirebaseRepository.FirestoreCallback<Boolean>() {
            @Override
            public void onCallback(Boolean isSaved) {
                if (isSaved) {
                    Toast.makeText(context, "Bài viết đã được lưu trước đó", Toast.LENGTH_SHORT).show();
                    
                    // Hiển thị màu xanh trong 1 giây, sau đó trở về màu trắng
                    animateCardBackground(cardView, 
                            getCardLongPressColor(), 
                            getCardSavedColor(), 
                            300);
                    
                    // Đặt hẹn giờ để trở về màu trắng sau 1 giây
                    new Handler().postDelayed(() -> {
                        animateCardBackground(cardView,
                                getCardSavedColor(),
                                getCardBackgroundColor(),
                                300);
                    }, 1000);
                    
                } else {
                    // Lưu bài viết
                    repository.saveArticle(userId, news.getLink(), new FirebaseRepository.FirestoreCallback<String>() {
                        @Override
                        public void onCallback(String result) {
                            Toast.makeText(context, "Đã lưu bài viết", Toast.LENGTH_SHORT).show();
                            
                            // Thay đổi màu nền với animation khi lưu thành công
                            animateCardBackground(cardView, 
                                    getCardLongPressColor(), 
                                    getCardSavedColor(), 
                                    500); // Thời gian animation dài hơn để tạo hiệu ứng nổi bật
                            
                            // Đặt hẹn giờ để trở về màu trắng sau 1 giây
                            new Handler().postDelayed(() -> {
                                animateCardBackground(cardView,
                                        getCardSavedColor(),
                                        getCardBackgroundColor(),
                                        300);
                            }, 1000);
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // Khôi phục màu nền ban đầu nếu có lỗi
                            animateCardBackground(cardView, 
                                    getCardLongPressColor(), 
                                    getCardBackgroundColor(), 
                                    300);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Khôi phục màu nền ban đầu nếu có lỗi
                animateCardBackground(cardView, 
                        getCardLongPressColor(), 
                        getCardBackgroundColor(), 
                        300);
            }
        });
    }
    
    // Phương thức để tạo animation chuyển đổi màu nền
    private void animateCardBackground(CardView cardView, int colorFrom, int colorTo, int duration) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(duration);
        colorAnimation.addUpdateListener(animator -> {
            int animatedValue = (int) animator.getAnimatedValue();
            cardView.setCardBackgroundColor(animatedValue);
        });
        colorAnimation.start();
    }

    @Override
    public int getItemCount() {
        return newsList != null ? newsList.size() : 0;
    }

    public void updateNewsList(List<RssNewsItem> newsList) {
        this.newsList = newsList;
        notifyDataSetChanged();
    }

    /**
     * Kiểm tra chuỗi có chứa ký tự tiếng Việt không
     */
    private boolean containsVietnameseCharacters(String text) {
        return text != null && text.matches(".*[ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵỷỹ].*");
    }

    // Phương thức để cung cấp phản hồi rung ngắn khi bắt đầu nhấn giữ
    private void provideShortVibrationFeedback() {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Deprecated in API 26
                    vibrator.vibrate(20);
                }
            }
        } catch (Exception e) {
            Log.e("NewsAdapter", "Error providing vibration feedback", e);
        }
    }
    
    // Phương thức để cung cấp phản hồi rung khi hoàn thành nhấn giữ
    private void provideVibrationFeedback() {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Tạo mẫu rung: rung - nghỉ - rung
                    long[] vibrationPattern = {0, 60, 50, 60};
                    vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1));
                } else {
                    // Deprecated in API 26
                    long[] vibrationPattern = {0, 60, 50, 60};
                    vibrator.vibrate(vibrationPattern, -1);
                }
            }
        } catch (Exception e) {
            Log.e("NewsAdapter", "Error providing vibration feedback", e);
        }
    }

    // Helper methods to get colors safely
    private int getCardBackgroundColor() {
        try {
            // Define the color value directly
            return 0xFFFFFFFF; // White color for background
        } catch (Exception e) {
            Log.e("NewsAdapter", "Error getting background color", e);
            return 0xFFFFFFFF; // Same fallback
        }
    }
    
    private int getCardLongPressColor() {
        try {
            // Define the color value directly
            return 0xFFE0F7FA; // Light blue color for long press (#FFE0F7FA)
        } catch (Exception e) {
            Log.e("NewsAdapter", "Error getting long press color", e);
            return 0xFFE0F7FA; // Same fallback
        }
    }
    
    private int getCardSavedColor() {
        try {
            // Define the color value directly
            return 0xFFBBDEFB; // Light blue color for saved state (#FFBBDEFB)
        } catch (Exception e) {
            Log.e("NewsAdapter", "Error getting saved color", e);
            return 0xFFBBDEFB; // Same fallback
        }
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
