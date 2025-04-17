package com.example.vnews;

import android.app.Application;
import android.util.Log;

import com.example.vnews.Repository.FirebaseRepository;
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
    }
} 