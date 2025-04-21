package com.example.vnews;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.Utils.EyeProtectionManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class VNewsApplication extends Application {
    
    private static final String TAG = "VNewsApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Enable Firestore offline persistence
        try {
           FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);

            Log.d(TAG, "Firebase Firestore offline persistence enabled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable Firestore offline persistence", e);
        }
        
        // Initialize repository
        FirebaseRepository.getInstance();
        
        // Register activity lifecycle callbacks to apply eye protection
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // Apply eye protection when each activity is created
                EyeProtectionManager.applyEyeProtectionIfEnabled(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                // No implementation needed
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // Apply eye protection when activity is resumed (in case setting was changed)
                EyeProtectionManager.applyEyeProtectionIfEnabled(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // No implementation needed
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                // No implementation needed
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                // No implementation needed
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                // No implementation needed
            }
        });
    }
} 