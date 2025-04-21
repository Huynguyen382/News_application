package com.example.vnews.Utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

@GlideModule
public class VNewsGlideModule extends AppGlideModule {
    private static final String TAG = "VNewsGlideModule";
    private static final int MEMORY_CACHE_SIZE = 20 * 1024 * 1024; // 20 MB
    private static final int DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final int TIMEOUT_SECONDS = 30;

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Thiết lập cache bộ nhớ
        builder.setMemoryCache(new LruResourceCache(MEMORY_CACHE_SIZE));
        
        // Thiết lập cache đĩa
        builder.setDiskCache(new InternalCacheDiskCacheFactory(
                context, DISK_CACHE_SIZE));
        
        // Thiết lập chất lượng hình ảnh và các tùy chọn khác
        builder.setDefaultRequestOptions(new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565) // giảm bộ nhớ sử dụng
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(false));
        
        // Bật log cho debug
        builder.setLogLevel(Log.ERROR);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, 
                                  @NonNull Registry registry) {
        // Tạo OkHttpClient với timeout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        
        // Đăng ký với OkHttp cho việc tải URL
        registry.replace(GlideUrl.class, InputStream.class,
                new OkHttpUrlLoader.Factory(okHttpClient));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false; // Tắt phân tích manifest để tối ưu hóa hiệu suất
    }
} 