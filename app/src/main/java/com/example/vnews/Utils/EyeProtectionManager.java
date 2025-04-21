package com.example.vnews.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Utility class to manage eye protection mode across the application
 */
public class EyeProtectionManager {
    private static final String PREFS_NAME = "EyeProtectionPrefs";
    private static final String KEY_EYE_PROTECTION_ENABLED = "eyeProtectionEnabled";
    
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
            applyEyeProtection(activity);
        } else {
            removeEyeProtection(activity);
        }
    }

    /**
     * Apply eye protection overlay regardless of setting
     * @param activity The activity to apply the overlay to
     */
    private static void applyEyeProtection(Activity activity) {
        // Remove any existing overlay first
        removeEyeProtection(activity);
        
        // Get the root view
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View rootView = decorView.findViewById(android.R.id.content);

        // Create overlay view
        View overlayView = new View(activity);
        overlayView.setId(OVERLAY_ID);
        overlayView.setBackgroundColor(OVERLAY_COLOR);
        
        // Add overlay to the activity's content view
        if (rootView instanceof ViewGroup) {
            ViewGroup contentView = (ViewGroup) rootView;
            
            // Create FrameLayout params that match parent
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            
            contentView.addView(overlayView, params);
        }
    }

    /**
     * Remove eye protection overlay
     * @param activity The activity to remove the overlay from
     */
    private static void removeEyeProtection(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View rootView = decorView.findViewById(android.R.id.content);
        
        if (rootView instanceof ViewGroup) {
            ViewGroup contentView = (ViewGroup) rootView;
            View overlayView = contentView.findViewById(OVERLAY_ID);
            if (overlayView != null) {
                contentView.removeView(overlayView);
            }
        }
    }
} 