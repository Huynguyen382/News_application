package com.example.vnews.View;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.vnews.Model.users;
import com.example.vnews.R;
import com.example.vnews.Repository.FirebaseRepository;
import com.example.vnews.databinding.ActivitySignupBinding;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_signup);
        repository = new FirebaseRepository();

        // Thiết lập các listener
        setupListeners();
    }

    private void setupListeners() {
        // Xử lý sự kiện nút Quay lại
        binding.buttonBack.setOnClickListener(view -> {
            finish();
        });

        // Xử lý sự kiện nút Đăng ký
        binding.buttonSignUp.setOnClickListener(view -> attemptSignup());

        // Xử lý sự kiện Đăng nhập ngay
        binding.textLogin.setOnClickListener(view -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });

        // Xử lý sự kiện đăng ký bằng Google
        binding.buttonGoogle.setOnClickListener(view -> {
            Toast.makeText(this, "Chức năng đăng ký bằng Google sẽ sớm ra mắt", Toast.LENGTH_SHORT).show();
        });

        // Xử lý sự kiện đăng ký bằng Facebook
        binding.buttonFacebook.setOnClickListener(view -> {
            Toast.makeText(this, "Chức năng đăng ký bằng Facebook sẽ sớm ra mắt", Toast.LENGTH_SHORT).show();
        });
    }

    private void attemptSignup() {
        // Lấy dữ liệu nhập
        String username = binding.editTextUsername.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String fullName = binding.editTextFullName.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();

        // Kiểm tra dữ liệu nhập
        if (TextUtils.isEmpty(username)) {
            binding.editTextUsername.setError("Vui lòng nhập tên đăng nhập");
            binding.editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.editTextEmail.setError("Vui lòng nhập email");
            binding.editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(fullName)) {
            binding.editTextFullName.setError("Vui lòng nhập họ và tên");
            binding.editTextFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.editTextPassword.setError("Vui lòng nhập mật khẩu");
            binding.editTextPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.editTextConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            binding.editTextConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.editTextConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            binding.editTextConfirmPassword.requestFocus();
            return;
        }

        // Hiển thị thông báo đang đăng ký
        Toast.makeText(this, "Đang đăng ký...", Toast.LENGTH_SHORT).show();
        
        // Kiểm tra xem username đã tồn tại chưa trước khi đăng ký
        checkUsernameExists(username, email, password, fullName);
    }
    
    private void checkUsernameExists(String username, String email, String password, String fullName) {
        Log.d("SignupActivity", "Kiểm tra username: " + username);
        
        // Sử dụng một truy vấn custom trong FirebaseRepository
        repository.isUsernameExists(username, new FirebaseRepository.FirestoreCallback<Boolean>() {
            @Override
            public void onCallback(Boolean exists) {
                if (!exists) {
                    // Username chưa tồn tại, tiếp tục đăng ký
                    Log.d("SignupActivity", "Username không tồn tại, tiếp tục đăng ký");
                    registerNewUser(email, password, username, fullName);
                } else {
                    // Username đã tồn tại
                    Log.d("SignupActivity", "Username đã tồn tại");
                    runOnUiThread(() -> {
                        binding.editTextUsername.setError("Tên đăng nhập đã tồn tại");
                        binding.editTextUsername.requestFocus();
                        Toast.makeText(SignupActivity.this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e("SignupActivity", "Lỗi khi kiểm tra username", e);
                Toast.makeText(SignupActivity.this, "Lỗi khi kiểm tra tên đăng nhập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void registerNewUser(String email, String password, String username, String fullName) {
        Log.d("SignupActivity", "Đăng ký người dùng mới: " + username + ", email: " + email);
        
        // Thực hiện đăng ký
        repository.registerUser(email, password, new FirebaseRepository.FirestoreCallback<String>() {
            @Override
            public void onCallback(String userId) {
                Log.d("SignupActivity", "Đăng ký Firebase Auth thành công, userId: " + userId);
                
                // Tạo đối tượng người dùng
                users user = new users();
                user.setId(userId);
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(password);
                user.setAvtUrl(""); // Để trống hoặc đặt URL avatar mặc định
                
                Log.d("SignupActivity", "Lưu thông tin người dùng vào Firestore");
                
                // Lưu thông tin user vào Firestore
                repository.addUserProfile(user, new FirebaseRepository.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void result) {
                        Log.d("SignupActivity", "Lưu thông tin người dùng thành công");
                        
                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                            // Sau khi đăng ký thành công, quay lại màn hình đăng nhập
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("SignupActivity", "Lỗi khi lưu thông tin người dùng", e);
                        
                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "Lỗi khi lưu thông tin người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("SignupActivity", "Đăng ký thất bại", e);
                
                runOnUiThread(() -> {
                    Toast.makeText(SignupActivity.this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
