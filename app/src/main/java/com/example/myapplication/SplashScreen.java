package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Задержка в 3000 миллисекунд (3 секунды)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // После задержки, начать новую активность
                Intent intent = new Intent(SplashScreen.this, LoginActivity.class);
                startActivity(intent);
                // Закрыть эту активность
                finish();
            }
        }, 3000);
    }
}
