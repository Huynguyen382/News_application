package com.example.vnnews;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vnnews.database.AppDatabase;
import com.example.vnnews.databinding.ActivitySignupBinding;
import com.example.vnnews.model.User;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private AppDatabase database;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize database and executor
        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // Set click listener
        binding.buttonSignUp.setOnClickListener(v -> handleSignUp());
    }

    private void handleSignUp() {
        String username = binding.editTextUsername.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String fullName = binding.editTextFullName.getText().toString().trim();

        // Validate input
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || 
            email.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            // Check if username already exists
            User existingUser = database.userDao().getUserByUsername(username);
            if (existingUser != null) {
                runOnUiThread(() -> 
                    Toast.makeText(SignUpActivity.this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show());
                return;
            }

            // Check if email already exists
            existingUser = database.userDao().getUserByEmail(email);
            if (existingUser != null) {
                runOnUiThread(() -> 
                    Toast.makeText(SignUpActivity.this, "Email đã tồn tại", Toast.LENGTH_SHORT).show());
                return;
            }

            // Create new user
            User newUser = new User(username, password, email, fullName);
            database.userDao().insert(newUser);

            runOnUiThread(() -> {
                Toast.makeText(SignUpActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        binding = null;
    }
} 