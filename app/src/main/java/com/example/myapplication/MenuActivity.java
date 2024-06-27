package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.protobuf.DescriptorProtos;

public class MenuActivity extends AppCompatActivity {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle drawerToggle;


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {



                drawerLayout.closeDrawer(GravityCompat.START);

                if (item.getItemId() == R.id.profile) {

                    Toast.makeText(MenuActivity.this, "Profile Selected", Toast.LENGTH_SHORT).show();

                    return true;
                } else if (item.getItemId() == R.id.start_training) {
                    Toast.makeText(MenuActivity.this, "Training Selected", Toast.LENGTH_SHORT).show();
                    // Start MainActivity
                    Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                    startActivity(intent);
                    return true;
                }

                else if (item.getItemId() == R.id.history) {
                    Toast.makeText(MenuActivity.this, "History Selected", Toast.LENGTH_SHORT).show();
                    return true;
                }

                return false;
            }
        });


        // Инициализация TextView как кнопки
        TextView startTraining = findViewById(R.id.StartToTheMouth);
        startTraining.setOnClickListener(view -> startTrainingActivity());
    }

    private void startTrainingActivity() {
        Intent intent = new Intent(MenuActivity.this, ToTheMouthActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
        {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }
}
