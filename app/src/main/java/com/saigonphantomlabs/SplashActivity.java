package com.saigonphantomlabs;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saigonphantomlabs.chess.BuildConfig;
import com.saigonphantomlabs.chess.R;

//TODO roy93~ admob
//TODO roy93~ ad applovin
//sdk 35 edge to edge
//review in app
//font scale
//120hz
//rate, more app, share app
//github
//license
//keystore

//done
//rename app
//leak canary

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_splash);
        setupViews();
    }

    private void setupViews() {
        //hiding actionbar
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        Button btnPlay = findViewById(R.id.btnPlay);
        ImageView ivBkg = findViewById(R.id.ivBkg);
        TextView tvVersion = findViewById(R.id.tvVersion);
        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText("Version " + versionName);
        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_bkg_1) // ảnh gif trong drawable/raw hoặc link URL
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivBkg);
        btnPlay.setOnClickListener(view -> {
            Intent switchActivityIntent = new Intent(this, ChessBoardActivity.class);
            startActivity(switchActivityIntent);
        });
    }
}