package com.example.vnews.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Utility class to manage eye protection mode across the application
 */
public class EyeProtectionManager {
    private static final String PREFS_NAME = "VNNews";
    private static final String KEY_EYE_PROTECTION_ENABLED = "eye_protection_enabled";
    
    // The color of the overlay filter (light amber tint)
    private static final int OVERLAY_COLOR = Color.parseColor("#17FFB65C");
    private static final int OVERLAY_ID = 9997;

    /**
     * Check if eye protection mode is enabled
     * @param context The context
     * @return true if enabled, false otherwise
     */
    public static boolean isEyeProtectionEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_EYE_PROTECTION_ENABLED, false);
    }

    /**
     * Enable or disable eye protection mode
     * @param context The context
     * @param enabled Whether to enable eye protection
     */
    public static void setEyeProtectionEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_EYE_PROTECTION_ENABLED, enabled).apply();
    }

    /**
     * Apply eye protection overlay to an activity
     * @param activity The activity to apply the overlay to
     */
    public static void applyEyeProtectionIfEnabled(Activity activity) {
        if (isEyeProtectionEnabled(activity)) {
            applyEyeProtection(activity, true);
        } else {
            removeEyeProtection(activity);
        }
    }
    
    /**
     * Apply eye protection overlay to an activity
     * Phương thức này giữ lại để tương thích ngược với các lệnh gọi cũ
     * @param activity The activity to apply the overlay to
     */
    public static void applyEyeProtection(Activity activity) {
        // Gọi phương thức mới với tham số là enabled=true
        applyEyeProtection(activity, true);
    }

    /**
     * Apply eye protection overlay to an activity with special handling for activities with CoordinatorLayout
     * @param activity The activity to apply the overlay to
     * @param enabled Whether to enable eye protection
     */
    public static void applyEyeProtection(Activity activity, boolean enabled) {
        // Log cho mục đích debug
        Log.d("EyeProtectionManager", "Applying eye protection: " + enabled + " to " + activity.getClass().getSimpleName());
        
        // Lưu trạng thái chế độ bảo vệ mắt
        setEyeProtectionEnabled(activity, enabled);
        
        if (enabled) {
            // Gỡ bỏ overlay hiện có
            removeEyeProtection(activity);
            
            // Lấy view gốc
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            View rootView = decorView.findViewById(android.R.id.content);

            if (rootView instanceof ViewGroup) {
                ViewGroup contentView = (ViewGroup) rootView;
                
                // Tạo overlay view mới
                View overlayView = new View(activity);
                overlayView.setId(OVERLAY_ID);
                overlayView.setBackgroundColor(OVERLAY_COLOR);
                
                // Đặt overlay ở vị trí cao nhất trong view hierarchy
                overlayView.setElevation(Float.MAX_VALUE - 1);
                
                // Tạo params phù hợp
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                        
                // Thêm overlay vào view gốc
                contentView.addView(overlayView, params);
                
                // Đảm bảo có các thuộc tính chính xác
                overlayView.setClickable(false);
                overlayView.setFocusable(false);
                
                // Log để debug
                Log.d("EyeProtectionManager", "Added overlay to " + contentView.getClass().getSimpleName());
            } else {
                Log.e("EyeProtectionManager", "RootView is not a ViewGroup, cannot apply overlay");
            }
        } else {
            removeEyeProtection(activity);
        }
    }

    /**
     * Remove eye protection overlay
     * @param activity The activity to remove the overlay from
     */
    private static void removeEyeProtection(Activity activity) {
        Log.d("EyeProtectionManager", "Removing eye protection overlay from " + activity.getClass().getSimpleName());
        
        try {
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            View rootView = decorView.findViewById(android.R.id.content);
            
            if (rootView instanceof ViewGroup) {
                ViewGroup contentView = (ViewGroup) rootView;
                View overlayView = contentView.findViewById(OVERLAY_ID);
                if (overlayView != null) {
                    contentView.removeView(overlayView);
                    Log.d("EyeProtectionManager", "Successfully removed overlay");
                } else {
                    Log.d("EyeProtectionManager", "No overlay found to remove");
                }
            }
        } catch (Exception e) {
            Log.e("EyeProtectionManager", "Error removing eye protection overlay", e);
        }
    }
} 