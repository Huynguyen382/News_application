package com.example.vnews.View;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.databinding.ActivityLoginBinding;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        repository = new FirebaseRepository();

        // Thiết lập các listener
        setupListeners();
    }

    private void setupListeners() {
        // Xử lý sự kiện nút Quay lại
        binding.buttonBack.setOnClickListener(view -> {
            finish();
        });
        
        // Xử lý sự kiện nút Đăng nhập
        binding.buttonLogin.setOnClickListener(view -> attemptLogin());

        // Xử lý sự kiện nút Đăng ký
        binding.textRegister.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });
        
        // Xử lý sự kiện nút Đăng nhập Google
        binding.buttonGoogle.setOnClickListener(view -> {
            Toast.makeText(this, "Chức năng đăng nhập bằng Google sẽ sớm ra mắt", Toast.LENGTH_SHORT).show();
        });
        
        // Xử lý sự kiện nút Đăng nhập Facebook
        binding.buttonFacebook.setOnClickListener(view -> {
            Toast.makeText(this, "Chức năng đăng nhập bằng Facebook sẽ sớm ra mắt", Toast.LENGTH_SHORT).show();
        });
        
        // Xử lý sự kiện quên mật khẩu
        binding.textForgotPassword.setOnClickListener(view -> {
            Toast.makeText(this, "Chức năng quên mật khẩu sẽ sớm ra mắt", Toast.LENGTH_SHORT).show();
        });
        
        // Xử lý sự kiện nút Đăng ký (phía dưới)
        binding.buttonSignUp.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });
    }

    private void attemptLogin() {
        // Lấy dữ liệu nhập
        String username = binding.editTextUsername.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        
        Log.d(TAG, "Đang thử đăng nhập với username: " + username);

        // Kiểm tra dữ liệu nhập
        if (TextUtils.isEmpty(username)) {
            binding.editTextUsername.setError("Vui lòng nhập tên đăng nhập");
            binding.editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.editTextPassword.setError("Vui lòng nhập mật khẩu");
            binding.editTextPassword.requestFocus();
            return;
        }

        // Hiển thị loading - sử dụng loading dialog thay vì progressBar
        showLoadingDialog(true);

        // Thử đăng nhập trực tiếp bằng email trước (nếu user nhập email thay vì username)
        if (username.contains("@")) {
            Log.d(TAG, "Đầu vào có dạng email, thử đăng nhập trực tiếp");
            
            repository.loginUser(username, password, new FirebaseRepository.FirestoreCallback<String>() {
                @Override
                public void onCallback(String userId) {
                    Log.d(TAG, "Đăng nhập trực tiếp thành công với userId: " + userId);
                    loginSuccess(userId);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Đăng nhập trực tiếp thất bại", e);
                    // Nếu đăng nhập trực tiếp thất bại, thử đăng nhập bằng username
                    tryLoginWithUsername(username, password);
                }
            });
        } else {
            // Đăng nhập bằng username
            tryLoginWithUsername(username, password);
        }
    }
    
    private void tryLoginWithUsername(String username, String password) {
        Log.d(TAG, "Thử đăng nhập bằng username: " + username);
        
        // Thực hiện đăng nhập bằng tên đăng nhập thay vì email
        repository.loginWithUsername(username, password, new FirebaseRepository.FirestoreCallback<String>() {
            @Override
            public void onCallback(String userId) {
                Log.d(TAG, "Đăng nhập với username thành công, userId: " + userId);
                loginSuccess(userId);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Đăng nhập với username thất bại", e);
                loginFailed(e);
            }
        });
    }
    
    private void loginSuccess(String userId) {
        Log.d(TAG, "Đăng nhập thành công với userId: " + userId);
        
        runOnUiThread(() -> {
            showLoadingDialog(false);
            
            // Đăng nhập thành công, chuyển về trang chính (MainContainerActivity)
            Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    private void loginFailed(Exception e) {
        Log.e(TAG, "Đăng nhập thất bại", e);
        
        // Hiển thị danh sách người dùng để debug
        debugListUsers();
        
        runOnUiThread(() -> {
            showLoadingDialog(false);
            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void debugListUsers() {
        Log.d(TAG, "Liệt kê tất cả người dùng để debug");
        
        repository.getAllUsers(new FirebaseRepository.FirestoreCallback<List<com.example.vnews.Model.users>>() {
            @Override
            public void onCallback(List<com.example.vnews.Model.users> users) {
                if (users.isEmpty()) {
                    Log.d(TAG, "Không có người dùng nào trong database");
                } else {
                    Log.d(TAG, "Danh sách người dùng (" + users.size() + "):");
                    for (com.example.vnews.Model.users user : users) {
                        Log.d(TAG, String.format("User: id=%s, username=%s, email=%s", 
                                user.getId(), user.getUsername(), user.getEmail()));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Lỗi khi lấy danh sách người dùng", e);
            }
        });
    }
    
    private void showLoadingDialog(boolean show) {
        // TODO: Thực hiện hiển thị dialog loading
        // Tạm thời hiển thị toast
        if (show) {
            Toast.makeText(this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
        }
    }
}