package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonBack.setOnClickListener(v -> finish());

        binding.buttonLogin.setOnClickListener(v -> handleLogin());

        binding.buttonGoogle.setOnClickListener(v -> {
            // TODO: Implement Google Sign In
            Toast.makeText(this, "Google Sign In clicked", Toast.LENGTH_SHORT).show();
        });

        binding.buttonFacebook.setOnClickListener(v -> {
            // TODO: Implement Facebook Sign In
            Toast.makeText(this, "Facebook Sign In clicked", Toast.LENGTH_SHORT).show();
        });

        binding.textForgotPassword.setOnClickListener(v -> {
            // TODO: Navigate to Forgot Password screen
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        binding.textRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void handleLogin() {
        String username = binding.editTextUsername.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (validateInput(username, password)) {
            // TODO: Implement actual login logic
            loginUser(username, password);
        }
    }

    private boolean validateInput(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            binding.layoutUsername.setError("Username is required");
            return false;
        }
        binding.layoutUsername.setError(null);

        if (TextUtils.isEmpty(password)) {
            binding.layoutPassword.setError("Password is required");
            return false;
        }
        binding.layoutPassword.setError(null);

        return true;
    }

    private void loginUser(String username, String password) {
        // TODO: Replace with actual authentication logic
        if (username.equals("admin") && password.equals("password")) {
            // Login successful
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // Login failed
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }
} 