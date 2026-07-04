package com.arena.ai.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(800);
        fadeIn.setFillAfter(true);

        AlphaAnimation fadeInDelayed = new AlphaAnimation(0.0f, 1.0f);
        fadeInDelayed.setDuration(600);
        fadeInDelayed.setStartOffset(400);
        fadeInDelayed.setFillAfter(true);

        logo.startAnimation(fadeIn);
        appName.startAnimation(fadeInDelayed);
        tagline.startAnimation(fadeInDelayed);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
