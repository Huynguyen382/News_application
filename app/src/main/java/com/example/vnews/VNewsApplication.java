package com.example.vnews;

import android.app.Application;

import com.example.vnews.Repository.FirebaseRepository;
import com.google.firebase.FirebaseApp;

public class VNewsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseRepository.getInstance();
    }
} 