package com.saigonphantomlabs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saigonphantomlabs.chess.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
        setupViews();
    }

    private void setupViews() {
        Button btnPlay = findViewById(R.id.btnPlay);
        ImageView ivBkg = findViewById(R.id.ivBkg);
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