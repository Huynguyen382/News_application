package com.example.vnews.View;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vnews.R;
import com.example.vnews.Utils.EyeProtectionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Áp dụng chế độ bảo vệ mắt nếu được bật
        EyeProtectionManager.applyEyeProtectionIfEnabled(this);
    }
}