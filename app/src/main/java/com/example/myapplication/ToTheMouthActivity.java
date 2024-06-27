package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ToTheMouthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_the_mouth);

        // Инициализация TextView как кнопки
        TextView startTrainingToTheMouth = findViewById(R.id.StartTrainingToTheMouth);
        startTrainingToTheMouth.setOnClickListener(view -> {
            // Переход
            Intent intent = new Intent(ToTheMouthActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
