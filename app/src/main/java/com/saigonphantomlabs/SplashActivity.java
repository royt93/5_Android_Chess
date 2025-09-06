package com.saigonphantomlabs;

import android.annotation.SuppressLint;
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
import com.saigonphantomlabs.sdkadbmob.UIUtils;

//TODO roy93~ admob
//TODO roy93~ ad applovin
//TODO roy93~ review in app
//TODO roy93~ font scale
//TODO roy93~ 120hz
//TODO roy93~ rate, more app, share app
//TODO roy93~ github
//TODO roy93~ license

//done
//rename app
//leak canary
//keystore
//sdk 35 edge to edge

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_splash);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.rootLayout),
                true,
                true);
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