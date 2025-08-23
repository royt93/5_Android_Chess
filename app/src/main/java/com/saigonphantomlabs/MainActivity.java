package com.saigonphantomlabs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.saigonphantomlabs.chess.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
    }

    public void btnPlayClick(View view) {
        Intent switchActivityIntent = new Intent(this, ChessBoardActivity.class);
        startActivity(switchActivityIntent);
    }
}