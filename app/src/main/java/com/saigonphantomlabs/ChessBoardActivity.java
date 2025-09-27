package com.saigonphantomlabs;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.saigonphantomlabs.chess.Chess;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;
import com.saigonphantomlabs.sdkadbmob.UIUtils;

public class ChessBoardActivity extends AppCompatActivity {
    public ConstraintLayout backgroundLayout;
    public FrameLayout boardLayout;
    public View blackTurnIndicator;
    public View whiteTurnIndicator;

    public Chess chess = null;

    public int displayWidth;
    public int displayHeight;
    public int displayMinDimensions;

    public int blackColor;
    public int whiteColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_chess_board);

        // Initialize status bar for dynamic tint changes
        initializeStatusBar();

        //hiding actionbar
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        //change background
        backgroundLayout = findViewById(R.id.backgroundLayout);

        //initiate black and white colors
//        blackColor = getResources().getColor(R.color.white);
//        whiteColor = getResources().getColor(R.color.black);
        blackColor = ContextCompat.getColor(this, R.color.white);
        whiteColor = ContextCompat.getColor(this, R.color.black);

        //set display params
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayHeight = displayMetrics.heightPixels;
        displayWidth = displayMetrics.widthPixels;
        displayMinDimensions = Math.min(displayWidth, displayHeight);

        boardLayout = findViewById(R.id.boardLayout);
        blackTurnIndicator = findViewById(R.id.blackTurnIndicator);
        whiteTurnIndicator = findViewById(R.id.whiteTurnIndicator);

        boardLayout.getLayoutParams().height = displayMinDimensions;
        boardLayout.getLayoutParams().width = displayMinDimensions;

        if (Storage.chess == null) {
            Storage.chess = chess = new Chess(this, displayMinDimensions, boardLayout);
        } else {
            chess = Storage.chess;
            chess.changeLayout(this, displayMinDimensions, boardLayout);
        }

        findViewById(R.id.boardImage).setOnTouchListener(this::onTouch);

        // Setup modern back navigation
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return false;
        int t = displayMinDimensions / 8;
        //chess.onBoardClick((int)event.getX() % (displayMinDimensions/8), (int)event.getY()%(displayMinDimensions/8));
        chess.onBoardClick(((int) event.getX()) / t, ((int) event.getY()) / t);
        return true;
    }

    public void showPromotionActivity() {
        startActivityForResult(new Intent(this, PawnPromotionActivity.class), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Toast.makeText(this, Storage.result.toString(), Toast.LENGTH_SHORT).show();
        chess.promotionResault(Storage.result);
    }

    private void handleBackPress() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getResources().getString(R.string.warning))
                .setMessage(getResources().getString(R.string.saveBoardPrompt))
                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        Storage.chess = null;
                    }
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Add custom button colors and styling
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    public void animateTurnChange(Chessman.PlayerColor turn) {
        // Update turn indicators
        updateTurnIndicators(turn);

        // Update status bar and navigation bar tint
        updateSystemBarsTint(turn);

        // Background color animation
        ValueAnimator colorAnimation;
        if (turn == Chessman.PlayerColor.White)
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), whiteColor, blackColor);
        else
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), blackColor, whiteColor);

        colorAnimation.setDuration(300); // milliseconds
        colorAnimation.addUpdateListener(animator -> backgroundLayout.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    private void updateTurnIndicators(Chessman.PlayerColor turn) {
        if (turn == Chessman.PlayerColor.Black) {
            // Show and animate black turn indicator (TOP)
            blackTurnIndicator.setVisibility(View.VISIBLE);
            whiteTurnIndicator.setVisibility(View.GONE);
            startBlinkingAnimation(blackTurnIndicator);
        } else {
            // Show and animate white turn indicator (BOTTOM)
            whiteTurnIndicator.setVisibility(View.VISIBLE);
            blackTurnIndicator.setVisibility(View.GONE);
            startBlinkingAnimation(whiteTurnIndicator);
        }
    }

    private void startBlinkingAnimation(View indicator) {
        // Stop any existing animation
        indicator.clearAnimation();

        // Create blinking effect
        indicator.animate()
                .alpha(0.3f)
                .setDuration(400)
                .withEndAction(() -> {
                    if (indicator.getVisibility() == View.VISIBLE) {
                        indicator.animate()
                                .alpha(1.0f)
                                .setDuration(400)
                                .withEndAction(() -> {
                                    if (indicator.getVisibility() == View.VISIBLE) {
                                        // Continue blinking in a loop
                                        startBlinkingAnimation(indicator);
                                    }
                                })
                                .start();
                    }
                })
                .start();
    }

    private void initializeStatusBar() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Make status bar and navigation bar transparent to work with our tinting
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);

            // Initialize navigation bar for API 26+ (Android 8.0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }

            // Initialize with default state (will be updated when game starts)
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

            // Initialize navigation bar icons (API 26+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }

            decorView.setSystemUiVisibility(flags);
        }
    }

    private void updateSystemBarsTint(Chessman.PlayerColor turn) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();

            if (turn == Chessman.PlayerColor.White) {
                // White turn: Dark status bar and navigation bar icons (for light background feel)
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

                // Add navigation bar light icons for API 26+ (Android 8.0)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                // Black turn: Light status bar and navigation bar icons (for dark background feel)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

                // Remove navigation bar light icons for API 26+ (Android 8.0)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }

            decorView.setSystemUiVisibility(flags);
        }
    }

}