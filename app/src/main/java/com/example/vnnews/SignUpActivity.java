package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySignupBinding;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonSignUp.setOnClickListener(v -> handleSignUp());

        binding.buttonGoogle.setOnClickListener(v -> {
            // TODO: Implement Google Sign Up
            Toast.makeText(this, "Google Sign Up clicked", Toast.LENGTH_SHORT).show();
        });

        binding.buttonFacebook.setOnClickListener(v -> {
            // TODO: Implement Facebook Sign Up
            Toast.makeText(this, "Facebook Sign Up clicked", Toast.LENGTH_SHORT).show();
        });

        binding.textLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleSignUp() {
        String username = binding.editTextUsername.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();

        if (validateInput(username, password, confirmPassword)) {
            // TODO: Implement actual sign up logic
            signUpUser(username, password);
        }
    }

    private boolean validateInput(String username, String password, String confirmPassword) {
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

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.layoutConfirmPassword.setError("Please confirm your password");
            return false;
        }
        binding.layoutConfirmPassword.setError(null);

        if (!password.equals(confirmPassword)) {
            binding.layoutConfirmPassword.setError("Passwords do not match");
            return false;
        }
        binding.layoutConfirmPassword.setError(null);

        // Add password strength validation
        if (password.length() < 8) {
            binding.layoutPassword.setError("Password must be at least 8 characters long");
            return false;
        }
        
        if (!password.matches(".*[A-Z].*")) {
            binding.layoutPassword.setError("Password must contain at least one uppercase letter");
            return false;
        }
        
        if (!password.matches(".*[a-z].*")) {
            binding.layoutPassword.setError("Password must contain at least one lowercase letter");
            return false;
        }
        
        if (!password.matches(".*\\d.*")) {
            binding.layoutPassword.setError("Password must contain at least one number");
            return false;
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            binding.layoutPassword.setError("Password must contain at least one special character");
            return false;
        }

        return true;
    }

    private void signUpUser(String username, String password) {
        // TODO: Replace with actual sign up logic (e.g., API call to server)
        // For now, just show success message and navigate to login
        Toast.makeText(this, "Sign up successful! Please login.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
} 