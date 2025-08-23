package com.saigonphantomlabs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;

public class PawnPromotionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_pawn_promotion);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    public void selectNewType(View view) {
        if (view.getId() == R.id.btnQueen)
            select(Chessman.ChessmanType.Queen);
        else if (view.getId() == R.id.btnRook)
            select(Chessman.ChessmanType.Rook);
        else if (view.getId() == R.id.btnBishop)
            select(Chessman.ChessmanType.Bishop);
        else if (view.getId() == R.id.btnKnight)
            select(Chessman.ChessmanType.Knight);
        else
            select(Chessman.ChessmanType.Queen);
    }

    private void select(Chessman.ChessmanType newType) {
        Storage.result = newType;
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }
}
