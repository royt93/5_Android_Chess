package com.saigonphantomlabs;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.roy.sdkadbmob.AdManager;
import com.roy.sdkadbmob.UIUtils;
import com.saigonphantomlabs.chess.BoardThemeManager;
import com.saigonphantomlabs.chess.Chess;
import com.saigonphantomlabs.chess.ChessBoardView;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.PieceRenderer;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;

import kotlin.Unit;

public class ChessBoardActivity extends BaseActivity implements ChessBoardView {

    // Core views
    public FrameLayout boardLayout;
    public View backgroundLayout;
    public View blackTurnIndicator;
    public View whiteTurnIndicator;
    public View whiteTurnGlow;
    public View blackTurnGlow;

    private LinearLayout capturedBlackPiecesContainer;
    private LinearLayout capturedWhitePiecesContainer;
    private android.widget.ImageButton btnUndo;
    private android.widget.ImageButton btnRestart;
    private AppCompatButton btnHint;
    private android.widget.ImageButton btnMoves;
    private final android.os.Handler hintHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    // Chess clock (đồng hồ cờ) — null nếu không bật giờ
    private com.saigonphantomlabs.chess.ChessClock chessClock;
    // Snapshot ván dở đang resume (null nếu ván mới) — dùng để dựng lại đồng hồ
    private com.saigonphantomlabs.chess.GameSaveManager.SavedGame resumedSave;
    // Id slot của ván hiện tại (tạo khi ván mới, reuse khi resume) + số ply gốc đã tích luỹ
    private String currentSessionId;
    private int resumedBaseMoveCount = 0;
    // Câu đố (mate-in-N): tắt AI/đồng hồ/autosave; sai nước → undo + thử lại; chiếu hết → giải xong.
    // Nước Trắng đúng (chưa phải nước cuối) → app tự đi nước Đen đáp trả rồi chờ nước Trắng kế.
    private boolean puzzleMode = false;
    private int puzzleIndex = -1;
    private com.saigonphantomlabs.chess.Puzzle currentPuzzle;
    private int puzzlePly = 0; // số nước Trắng đã đi đúng
    private TextView tvWhiteClock, tvBlackClock;
    private long lastClockTickMs;
    private boolean clockRunning;
    private final android.os.Handler clockHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (!clockRunning || chess == null || chessClock == null) return;
            if (chess.isGameEnd()) { clockRunning = false; return; }
            long now = android.os.SystemClock.elapsedRealtime();
            chessClock.setWhiteActive(chess.whichPlayerTurn == Chessman.PlayerColor.White);
            chessClock.tick(now - lastClockTickMs);
            lastClockTickMs = now;
            updateClockDisplays();
            com.saigonphantomlabs.chess.ChessClock.Flag flag = chessClock.flagged();
            if (flag != com.saigonphantomlabs.chess.ChessClock.Flag.NONE) {
                onFlagFall(flag);
                return;
            }
            clockHandler.postDelayed(this, 200L);
        }
    };
    private ImageView boardImage;            // The board PNG / Canvas-drawn board
    private BoardThemeManager.Theme currentTheme;  // Active board color theme

    public Chess chess = null;
    public int displayWidth;
    public int displayHeight;
    public int displayMinDimensions;
    public int blackColor;
    public int whiteColor;

    // Infinite animator references — ALL cancelled in onDestroy
    private ObjectAnimator currentBlinkAnimator;
    private ObjectAnimator boardPulseAnim;
    private ObjectAnimator cornerTRAnim;
    private ObjectAnimator cornerBLAnim;
    // Turn glow ValueAnimator (for custom intensity control)
    private ValueAnimator whiteGlowAnim;
    private ValueAnimator blackGlowAnim;

    private View adView;

    private final ActivityResultLauncher<Intent> promotionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (chess != null) chess.promotionResault(Storage.result);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_chess_board);
        initializeStatusBar();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        backgroundLayout = findViewById(R.id.backgroundLayout);
        UIUtils.INSTANCE.setupEdgeToEdge2(backgroundLayout, true, true);

        blackColor = ContextCompat.getColor(this, R.color.white);
        whiteColor = ContextCompat.getColor(this, R.color.black);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            displayWidth = bounds.width();
            displayHeight = bounds.height();
        } else {
            android.util.DisplayMetrics m = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(m);
            displayHeight = m.heightPixels;
            displayWidth = m.widthPixels;
        }
        displayMinDimensions = Math.min(displayWidth, displayHeight);

        boardLayout = findViewById(R.id.boardLayout);
        blackTurnIndicator = findViewById(R.id.blackTurnIndicator);
        whiteTurnIndicator = findViewById(R.id.whiteTurnIndicator);
        whiteTurnGlow = findViewById(R.id.whiteTurnGlow);
        blackTurnGlow = findViewById(R.id.blackTurnGlow);
        capturedBlackPiecesContainer = findViewById(R.id.capturedBlackPiecesContainer);
        capturedWhitePiecesContainer = findViewById(R.id.capturedWhitePiecesContainer);
        btnUndo = findViewById(R.id.btnUndo);
        btnRestart = findViewById(R.id.btnRestart);

        btnUndo.setOnClickListener(v -> {
            if (chess != null && chess.canUndo() && !chess.isAiThinking) {
                chess.clearHint();
                chess.undoLastMove();
            }
        });
        btnRestart.setOnClickListener(v -> {
            if (chess != null && !chess.isAiThinking) showRestartConfirmDialog();
        });

        btnHint = findViewById(R.id.btnHint);
        if (btnHint != null) btnHint.setOnClickListener(v -> onHintClicked());

        btnMoves = findViewById(R.id.btnMoves);
        if (btnMoves != null) btnMoves.setOnClickListener(v -> onMovesClicked());

        tvWhiteClock = findViewById(R.id.tvWhiteClock);
        tvBlackClock = findViewById(R.id.tvBlackClock);

        // Nạp bộ quân (piece set) đã lưu vào renderer TRƯỚC khi dựng quân (pieces tạo ở listener sau).
        com.saigonphantomlabs.chess.PieceSetManager.applySaved(this);

        // Piece set button → picker glass; đổi xong render lại quân trên bàn ngay.
        View btnPieceSet = findViewById(R.id.btnPieceSet);
        if (btnPieceSet != null) btnPieceSet.setOnClickListener(v -> showPieceSetPicker());

        // Board theme button (if present in layout)
        boardImage = findViewById(R.id.boardImage);
        View btnTheme = findViewById(R.id.btnBoardTheme);
        if (btnTheme != null) {
            btnTheme.setOnClickListener(v -> {
                BoardThemePickerDialog.show(ChessBoardActivity.this, currentTheme, theme -> {
                    currentTheme = theme;
                    if (boardImage != null) {
                        BoardThemeManager.applyTheme(boardImage, theme, boardImage.getWidth());
                    }
                });
            });
        }

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackPress();
                    }
                });

        startAmbientAnimations();

        // autoManageLifecycle=FALSE → tự quản resume/pause/destroy thủ công (onResume/onPause/onDestroy).
        // Lý do: auto-manage để banner tự refresh ngầm cả khi đã rời màn cờ (sang Menu/VIP) →
        // invalid impression (policy) + tốn request/memory. Quản tay đảm bảo banner CHỈ refresh
        // khi màn cờ đang hiển thị.
        adView = AdManager.INSTANCE.loadBanner(this,
                (ViewGroup) findViewById(R.id.banner_container),
                (TextView) findViewById(R.id.tvLabelAd),
                com.google.android.gms.ads.AdSize.BANNER,
                false);
        AdManager.INSTANCE.loadInterstitial(this);

        // Board is made square by ConstraintLayout dimensionRatio="1:1" in XML.
        // Wait for layout to complete, then init Chess with the actual square board size.
        boardLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        boardLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (isFinishing()) return;

                        int boardSize = boardLayout.getWidth(); // == height (square guaranteed by XML)
                        if (boardSize <= 0) return;

                        if (Storage.getChess() == null) {
                            chess = new Chess(ChessBoardActivity.this,
                                    boardSize, boardLayout);
                            Storage.setChess(chess);
                            int pIdx = getIntent().getIntExtra("PUZZLE_INDEX", -1);
                            String resumeId = getIntent().getStringExtra("RESUME_SESSION");
                            com.saigonphantomlabs.chess.GameSaveManager.SavedGame saved =
                                    (pIdx < 0 && resumeId != null)
                                            ? com.saigonphantomlabs.chess.GameSaveManager.loadSlot(ChessBoardActivity.this, resumeId)
                                            : null;
                            if (pIdx >= 0) {
                                setupPuzzle(pIdx);
                            } else if (saved != null) {
                                // Tiếp tục ván dở: khôi phục mode + thế cờ từ snapshot
                                chess.isVsComputer = saved.isVsAi;
                                if (saved.difficulty != null) {
                                    try {
                                        chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.valueOf(saved.difficulty);
                                    } catch (IllegalArgumentException ex) {
                                        chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.EASY;
                                    }
                                }
                                resumedSave = saved;
                                currentSessionId = saved.sessionId != null ? saved.sessionId : resumeId;
                                resumedBaseMoveCount = saved.moveCount;
                                chess.loadSaveState(saved);
                            } else {
                                boolean isVsAi = getIntent().getBooleanExtra("IS_VS_AI", false);
                                chess.isVsComputer = isVsAi;
                                if (isVsAi) {
                                    String diffStr = getIntent().getStringExtra("AI_DIFFICULTY");
                                    if (diffStr != null) {
                                        try {
                                            chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.valueOf(diffStr);
                                        } catch (IllegalArgumentException ex) {
                                            chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.EASY;
                                        }
                                    }
                                }
                                currentSessionId = newSessionId(); // ván mới → session mới
                            }
                        } else {
                            chess = Storage.getChess();
                            chess.changeLayout(ChessBoardActivity.this, boardSize, boardLayout);
                        }
                        setupClock();
                        // Register touch handler
                        ImageView bImg = boardImage != null ? boardImage : (ImageView) findViewById(R.id.boardImage);
                        if (bImg != null) bImg.setOnTouchListener(ChessBoardActivity.this::onTouch);

                        // Apply saved board theme (draws 8x8 board via Canvas)
                        currentTheme = BoardThemeManager.load(ChessBoardActivity.this);
                        if (boardImage != null) {
                            BoardThemeManager.applyTheme(boardImage, currentTheme, boardSize);
                        }

                        // Hero: board flies in
                        boardLayout.setAlpha(0f);
                        boardLayout.setScaleX(0.75f);
                        boardLayout.setScaleY(0.75f);
                        boardLayout.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                .setDuration(650).setInterpolator(new OvershootInterpolator(1.2f)).start();
                    }
                });
    }

    private void startAmbientAnimations() {
        // Board alive feel: subtle alpha breathe (NO scale — board is exact square, must not overflow)
        if (boardLayout != null) {
            boardPulseAnim = ObjectAnimator.ofFloat(boardLayout, "alpha", 1.0f, 0.96f);
            boardPulseAnim.setDuration(3500);
            boardPulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            boardPulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            boardPulseAnim.setInterpolator(new DecelerateInterpolator());
            boardPulseAnim.start();
        }

        View cornerTR = findViewById(R.id.cornerGlowTR);
        if (cornerTR != null) {
            cornerTRAnim = ObjectAnimator.ofFloat(cornerTR, "alpha", 0.2f, 0.5f);
            cornerTRAnim.setDuration(2800);
            cornerTRAnim.setRepeatCount(ValueAnimator.INFINITE);
            cornerTRAnim.setRepeatMode(ValueAnimator.REVERSE);
            cornerTRAnim.start();
        }
        View cornerBL = findViewById(R.id.cornerGlowBL);
        if (cornerBL != null) {
            cornerBLAnim = ObjectAnimator.ofFloat(cornerBL, "alpha", 0.15f, 0.45f);
            cornerBLAnim.setDuration(3400);
            cornerBLAnim.setRepeatCount(ValueAnimator.INFINITE);
            cornerBLAnim.setRepeatMode(ValueAnimator.REVERSE);
            cornerBLAnim.start();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
        int t = v.getWidth() / 8;
        if (t <= 0) return false;
        chess.onBoardClick(((int) event.getX()) / t, ((int) event.getY()) / t);
        return true;
    }

    public void showPromotionActivity() {
        Intent intent = new Intent(this, PawnPromotionActivity.class);
        promotionLauncher.launch(intent);
        overridePendingTransition(R.anim.fade_zoom_in, R.anim.fade_zoom_out);
    }

    private void handleBackPress() {
        // Câu đố: không phải "ván" → back thoát thẳng về danh sách (không hỏi "Quit game?", không ad)
        if (puzzleMode) {
            com.saigonphantomlabs.chess.Storage.clearChess();
            finish();
            overridePendingTransition(R.anim.fade_zoom_in, R.anim.fade_zoom_out);
            return;
        }
        DialogUtils.showQuitDialog(this, () -> {
            Storage.clearChess();
            AdManager.INSTANCE.showInterstitial(this, new kotlin.jvm.functions.Function1<Boolean, kotlin.Unit>() {
                @Override
                public Unit invoke(Boolean adShown) {
                    finish();
                    overridePendingTransition(R.anim.fade_zoom_in, R.anim.fade_zoom_out);
                    return Unit.INSTANCE;
                }
            });
        });
    }

    /**
     * Hint: gợi ý nước tốt nhất. VIP → gợi ý ngay; non-VIP → xem rewarded (nếu có) rồi gợi ý
     * (không có ad vẫn gợi ý — không chặn tính năng). Trong PvE chỉ gợi ý khi tới lượt người.
     */
    private void onHintClicked() {
        if (chess == null || chess.isGameEnd() || chess.isAiThinking || chess.isHintThinking()
                || chess.inputLocked) return; // inputLocked: chặn gợi ý sớm/stale lúc chờ auto-reply/undo (puzzle)
        // Câu đố: gợi ý = highlight nước giải kỳ vọng hiện tại (miễn phí, không rewarded ad)
        if (puzzleMode) {
            int[] m = currentPuzzle != null ? currentPuzzle.whiteMove(puzzlePly) : null;
            if (m != null) {
                chess.showHintForMove(new com.saigonphantomlabs.chess.Point(m[0], m[1]),
                        new com.saigonphantomlabs.chess.Point(m[2], m[3]));
            }
            return;
        }
        if (chess.isVsComputer && chess.whichPlayerTurn != Chessman.PlayerColor.White) return;

        if (AdManager.INSTANCE.isVipByKeyActive()) {
            doHint();
        } else {
            AdManager.INSTANCE.showRewarded(this, earned -> {
                doHint();
                return Unit.INSTANCE;
            });
        }
    }

    private void doHint() {
        if (chess == null || isDestroyed()) return;
        chess.requestHint(move -> {
            if (move == null) {
                android.widget.Toast.makeText(this, R.string.hint_none,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            // tự xoá highlight sau 4s (hoặc khi user tương tác — xem Chess.onManClick)
            hintHandler.removeCallbacksAndMessages(null);
            hintHandler.postDelayed(() -> { if (chess != null) chess.clearHint(); }, 4000L);
        });
    }

    /** Picker bộ quân cờ (glass) — đổi xong lưu + render lại quân trên bàn ngay. */
    private void showPieceSetPicker() {
        com.saigonphantomlabs.chess.PieceSet[] sets = com.saigonphantomlabs.chess.PieceSet.values();
        DialogUtils.ChoiceOption[] opts = new DialogUtils.ChoiceOption[sets.length];
        int accent = ContextCompat.getColor(this, R.color.game_gold_primary);
        for (int i = 0; i < sets.length; i++) {
            opts[i] = new DialogUtils.ChoiceOption(getString(sets[i].nameRes), null, sets[i].emoji, accent);
        }
        DialogUtils.showChoiceDialog(this, getString(R.string.pieceset_title), 0, opts, idx -> {
            com.saigonphantomlabs.chess.PieceSetManager.setCurrent(this, sets[idx]);
            if (chess != null) chess.refreshPieceVisuals();
        });
    }

    /** Mở dialog lịch sử nước đi + nút Share PGN. */
    private void onMovesClicked() {
        if (chess == null) return;
        java.util.List<com.saigonphantomlabs.chess.MoveRecord> moves = chess.getMoveHistory();
        CharSequence body = moves.isEmpty()
                ? getString(R.string.moves_empty)
                : com.saigonphantomlabs.chess.PgnExporter.buildMoveTextMultiline(moves);
        DialogUtils.showBasicDialog(this,
                getString(R.string.moves_title),
                body,
                getString(R.string.share_pgn),   // positive → share
                getString(R.string.close),       // negative → đóng
                R.drawable.ic_moves,
                () -> sharePgn(moves),
                null);
    }

    private void sharePgn(java.util.List<com.saigonphantomlabs.chess.MoveRecord> moves) {
        String date = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.US)
                .format(new java.util.Date());
        String pgn = com.saigonphantomlabs.chess.PgnExporter.buildPgn(moves, "White", "Black", date, "*");
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Quick Chess PGN");
            share.putExtra(Intent.EXTRA_TEXT, pgn);
            startActivity(Intent.createChooser(share, getString(R.string.share_pgn)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ───────────────────────── CHESS CLOCK ─────────────────────────

    private void setupClock() {
        // Resume ván dở có đồng hồ → khôi phục thời gian còn lại 2 bên
        if (resumedSave != null && resumedSave.hasClock) {
            chessClock = new com.saigonphantomlabs.chess.ChessClock(0, resumedSave.incrementMs);
            chessClock.restore(resumedSave.whiteMs, resumedSave.blackMs, resumedSave.whiteActive);
            if (tvWhiteClock != null) tvWhiteClock.setVisibility(View.VISIBLE);
            if (tvBlackClock != null) tvBlackClock.setVisibility(View.VISIBLE);
            updateClockDisplays();
            startClock();
            return;
        }
        long ms = getIntent().getLongExtra("TIME_CONTROL_MS", 0L);
        if (ms <= 0) return;
        if (chessClock == null) chessClock = new com.saigonphantomlabs.chess.ChessClock(ms, 0);
        if (tvWhiteClock != null) tvWhiteClock.setVisibility(View.VISIBLE);
        if (tvBlackClock != null) tvBlackClock.setVisibility(View.VISIBLE);
        updateClockDisplays();
        startClock();
    }

    private void updateClockDisplays() {
        if (chessClock == null) return;
        if (tvWhiteClock != null)
            tvWhiteClock.setText(com.saigonphantomlabs.chess.ChessClock.format(chessClock.getWhiteMs()));
        if (tvBlackClock != null)
            tvBlackClock.setText(com.saigonphantomlabs.chess.ChessClock.format(chessClock.getBlackMs()));
    }

    private void startClock() {
        if (chessClock == null || chess == null || chess.isGameEnd() || clockRunning) return;
        clockRunning = true;
        lastClockTickMs = android.os.SystemClock.elapsedRealtime();
        clockHandler.post(clockTick);
    }

    private void stopClock() {
        clockRunning = false;
        clockHandler.removeCallbacks(clockTick);
    }

    private void onFlagFall(com.saigonphantomlabs.chess.ChessClock.Flag flag) {
        stopClock();
        if (chess != null) chess.endGameByTimeout();
        updateClockDisplays();
        // Bên hết giờ thua → bên kia thắng
        boolean whiteWins = (flag == com.saigonphantomlabs.chess.ChessClock.Flag.BLACK);
        pendingEndIsTimeout = true; // hết giờ ≠ chiếu hết (cho achievement)
        showCustomGameEndDialog(whiteWins, false);
    }

    // Kết thúc ván vừa rồi là do HẾT GIỜ (không phải chiếu hết) — reset sau mỗi lần show dialog.
    private boolean pendingEndIsTimeout = false;
    // Tránh ghi nhận thành tích 2 lần cho cùng 1 ván (dialog có thể dựng lại khi xoay màn).
    private boolean achievementsRecorded = false;

    private void showRestartConfirmDialog() {
        DialogUtils.showRestartDialog(this, () -> {
            if (chess != null) chess.resetGame();
            // Ván cũ bỏ → xoá slot, bắt đầu session mới
            com.saigonphantomlabs.chess.GameSaveManager.deleteSlot(this, currentSessionId);
            resumedSave = null;
            resumedBaseMoveCount = 0;
            achievementsRecorded = false;
            currentSessionId = newSessionId();
            resetClock();
        });
    }

    /** Reset đồng hồ về thời gian ban đầu khi chơi lại (no-op nếu không bật giờ). */
    private void resetClock() {
        if (chessClock == null) return;
        long ms = getIntent().getLongExtra("TIME_CONTROL_MS", 0L);
        stopClock();
        chessClock = new com.saigonphantomlabs.chess.ChessClock(ms, 0);
        updateClockDisplays();
        startClock();
    }

    /**
     * Enhanced turn change:
     * - Bright, high-alpha glow overlay (0.0 → 0.92) with slow oscillation
     * - Turn indicators pop in with overshoot
     * - System bars tinted
     */
    public void animateTurnChange(Chessman.PlayerColor turn) {
        // Câu đố: người chơi (Trắng) vừa đi mà CHƯA chiếu hết (lượt sang Đen, ván chưa kết thúc).
        if (puzzleMode && turn == Chessman.PlayerColor.Black && chess != null && !chess.isGameEnd()) {
            handlePuzzleWhiteMove();
        }
        updateTurnIndicators(turn);

        // Cancel previous glow animators before starting new ones
        if (whiteGlowAnim != null) {
            whiteGlowAnim.cancel();
            whiteGlowAnim = null;
        }
        if (blackGlowAnim != null) {
            blackGlowAnim.cancel();
            blackGlowAnim = null;
        }

        if (turn == Chessman.PlayerColor.White) {
            // White gets strong gold glow that breathes
            whiteTurnGlow.animate().alpha(0.90f).setDuration(600).start();
            blackTurnGlow.animate().alpha(0f).setDuration(400).start();
            // Breathing oscillation: 0.90 → 0.60 → 0.90 …
            whiteGlowAnim = ValueAnimator.ofFloat(0.90f, 0.55f);
            whiteGlowAnim.setDuration(2000);
            whiteGlowAnim.setRepeatCount(ValueAnimator.INFINITE);
            whiteGlowAnim.setRepeatMode(ValueAnimator.REVERSE);
            whiteGlowAnim.addUpdateListener(a -> {
                if (whiteTurnGlow != null) whiteTurnGlow.setAlpha((float) a.getAnimatedValue());
            });
            whiteGlowAnim.setStartDelay(650);
            whiteGlowAnim.start();
        } else {
            // Black gets strong cyan glow that breathes
            blackTurnGlow.animate().alpha(0.90f).setDuration(600).start();
            whiteTurnGlow.animate().alpha(0f).setDuration(400).start();
            blackGlowAnim = ValueAnimator.ofFloat(0.90f, 0.55f);
            blackGlowAnim.setDuration(2000);
            blackGlowAnim.setRepeatCount(ValueAnimator.INFINITE);
            blackGlowAnim.setRepeatMode(ValueAnimator.REVERSE);
            blackGlowAnim.addUpdateListener(a -> {
                if (blackTurnGlow != null) blackTurnGlow.setAlpha((float) a.getAnimatedValue());
            });
            blackGlowAnim.setStartDelay(650);
            blackGlowAnim.start();
        }

        updateSystemBarsTint();
    }

    private void updateTurnIndicators(Chessman.PlayerColor turn) {
        // Cancel dot blink before reassigning
        if (currentBlinkAnimator != null) {
            currentBlinkAnimator.cancel();
            currentBlinkAnimator = null;
        }
        // Reset dot states
        resetDot(findViewById(R.id.whiteTurnDot));
        resetDot(findViewById(R.id.blackTurnDot));

        if (turn == Chessman.PlayerColor.Black) {
            animatePopIn(blackTurnIndicator);
            if (whiteTurnIndicator != null) whiteTurnIndicator.setVisibility(View.INVISIBLE);
            startBreathingDot(findViewById(R.id.blackTurnDot));
        } else {
            animatePopIn(whiteTurnIndicator);
            if (blackTurnIndicator != null) blackTurnIndicator.setVisibility(View.INVISIBLE);
            startBreathingDot(findViewById(R.id.whiteTurnDot));
        }
    }

    private void resetDot(View dot) {
        if (dot == null) return;
        dot.setScaleX(1f);
        dot.setScaleY(1f);
        dot.setAlpha(1f);
    }

    private void animatePopIn(View indicator) {
        if (indicator == null) return;
        indicator.setVisibility(View.VISIBLE);
        indicator.setAlpha(0f);
        indicator.setScaleX(0.4f);
        indicator.setScaleY(0.4f);
        indicator.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(450).setInterpolator(new OvershootInterpolator(1.8f)).start();
    }

    private void startBreathingDot(View dotView) {
        if (dotView == null) return;
        // Use alpha pulse ONLY — no scale, so dot never overflows its own bounds
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.15f);
        currentBlinkAnimator = ObjectAnimator.ofPropertyValuesHolder(dotView, alpha);
        currentBlinkAnimator.setDuration(550);
        currentBlinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        currentBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        currentBlinkAnimator.start();
    }

    public void updateUndoButton(boolean visible) {
        if (puzzleMode) return; // câu đố: undo/restart luôn ẩn
        updateGameButtons(visible, visible);
    }

    public void updateGameButtons(boolean showUndo, boolean showRestart) {
        animateButton(btnUndo, showUndo);
        animateButton(btnRestart, showRestart);
    }

    private void animateButton(View btn, boolean show) {
        if (btn == null) return;
        if (show && btn.getVisibility() != View.VISIBLE) {
            btn.setVisibility(View.VISIBLE);
            btn.setAlpha(0f);
            btn.setScaleX(0.5f);
            btn.setScaleY(0.5f);
            btn.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(new OvershootInterpolator()).start();
        } else if (!show && btn.getVisibility() == View.VISIBLE) {
            // GONE (không INVISIBLE): nút ẩn KHÔNG được giữ chỗ layout → tránh "khoảng trống lạ"
            // giữa nhóm nút (undo/restart ẩn từng để lại 48dp×2 trống cạnh nút Gợi ý).
            btn.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(150)
                    .withEndAction(() -> btn.setVisibility(View.GONE)).start();
        }
    }

    public void addCapturedPiece(Chessman piece) {
        if (piece == null) return;
        LinearLayout container = (piece.color == Chessman.PlayerColor.White)
                ? capturedBlackPiecesContainer : capturedWhitePiecesContainer;
        if (container == null) return;

        ImageView img = new ImageView(this);
        int sz = (int) (30 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.setMargins(2, 0, 2, 0);
        img.setLayoutParams(lp);
        img.setImageResource(getPieceDrawableId(piece));
        img.setTag(piece);
        img.setScaleX(0f);
        img.setScaleY(0f);
        img.setAlpha(0f);
        container.addView(img);
        img.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300)
                .setInterpolator(new OvershootInterpolator()).start();
    }

    public void removeCapturedPiece(Chessman piece) {
        if (piece == null) return;
        LinearLayout container = (piece.color == Chessman.PlayerColor.White)
                ? capturedBlackPiecesContainer : capturedWhitePiecesContainer;
        if (container == null) return;
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (child.getTag() == piece) {
                View v = child;
                child.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200)
                        .withEndAction(() -> container.removeView(v)).start();
                break;
            }
        }
    }

    public void clearCapturedPieces() {
        if (capturedBlackPiecesContainer != null) capturedBlackPiecesContainer.removeAllViews();
        if (capturedWhitePiecesContainer != null) capturedWhitePiecesContainer.removeAllViews();
    }

    private int getPieceDrawableId(Chessman piece) {
        boolean w = piece.color == Chessman.PlayerColor.White;
        switch (piece.type) {
            case King:
                return w ? R.drawable.ic_kingw : R.drawable.ic_kingb;
            case Queen:
                return w ? R.drawable.ic_queenw : R.drawable.ic_queenb;
            case Rook:
                return w ? R.drawable.ic_rookw : R.drawable.ic_rookb;
            case Bishop:
                return w ? R.drawable.ic_bishopw : R.drawable.ic_bishopb;
            case Knight:
                return w ? R.drawable.ic_knightw : R.drawable.ic_knightb;
            case Pawn:
                return w ? R.drawable.ic_pawnw : R.drawable.ic_pawnb;
            default:
                return R.drawable.ic_pawnw;
        }
    }

    public void showCustomGameEndDialog(boolean whiteWins, boolean isStalemate) {
        // Câu đố: tới đây nghĩa là người chơi vừa tạo chiếu hết ⇒ GIẢI XONG (bỏ qua dialog/stats/save ván thường)
        if (puzzleMode) { showPuzzleSolvedDialog(); return; }
        // Ván đã kết thúc → không còn gì để tiếp tục, xoá slot
        com.saigonphantomlabs.chess.GameSaveManager.deleteSlot(this, currentSessionId);
        resumedSave = null;
        recordAchievements(whiteWins, isStalemate);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_end, null);

        ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        TextView durationValue = dialogView.findViewById(R.id.stats_duration_value);
        TextView movesValue = dialogView.findViewById(R.id.stats_moves_value);
        TextView whiteCapturedValue = dialogView.findViewById(R.id.stats_white_captured_value);
        TextView blackCapturedValue = dialogView.findViewById(R.id.stats_black_captured_value);
        View statsContainer = dialogView.findViewById(R.id.stats_container);
        View btnExit = dialogView.findViewById(R.id.btn_exit);
        View btnReview = dialogView.findViewById(R.id.btn_review);
        View btnPlayAgain = dialogView.findViewById(R.id.btn_play_again);

        if (isStalemate) {
            titleView.setText(R.string.stalemate);
            messageView.setText(R.string.stalemate_message);
            iconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_draw));
        } else {
            titleView.setText(whiteWins ? R.string.white_wins : R.string.black_wins);
            messageView.setText(whiteWins ? R.string.black_loses : R.string.white_loses);
            iconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_trophy));
        }

        if (chess != null) {
            if (durationValue != null) durationValue.setText(chess.getFormattedDuration());
            if (movesValue != null) movesValue.setText(String.valueOf(chess.getMoveCount()));
            if (whiteCapturedValue != null) whiteCapturedValue.setText(String.valueOf(chess.getCapturedWhiteCount()));
            if (blackCapturedValue != null) blackCapturedValue.setText(String.valueOf(chess.getCapturedBlackCount()));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Disable clipping so dialog entry scale animation isn't cut at edges
            View decorView = dialog.getWindow().getDecorView();
            if (decorView instanceof ViewGroup) {
                ((ViewGroup) decorView).setClipChildren(false);
                ((ViewGroup) decorView).setClipToPadding(false);
            }
        }

        // Entry animation + staggered stats
        Animation enterAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        if (statsContainer != null) {
            statsContainer.setTranslationY(80f);
            statsContainer.setAlpha(0f);
            statsContainer.animate().translationY(0f).alpha(1f)
                    .setStartDelay(220).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        if (btnReview != null) btnReview.setOnClickListener(v -> {
            dialog.dismiss();
            updateGameButtons(false, true);
        });
        if (btnPlayAgain != null) btnPlayAgain.setOnClickListener(v -> {
            dialog.dismiss();
            chess.resetGame();
            resumedSave = null;
            resumedBaseMoveCount = 0;
            achievementsRecorded = false;      // ván mới → cho phép ghi nhận lại
            currentSessionId = newSessionId(); // ván chơi-lại = session mới
            resetClock();
        });
        if (btnExit != null) btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            Storage.clearChess();
            AdManager.INSTANCE.showInterstitial(this, new kotlin.jvm.functions.Function1<Boolean, Unit>() {
                @Override
                public Unit invoke(Boolean adShown) {
                    finish();
                    overridePendingTransition(R.anim.fade_zoom_in, R.anim.fade_zoom_out);
                    return Unit.INSTANCE;
                }
            });
        });

        // Phân tích sau ván: chỉ bật cho ván chơi TỪ ĐẦU (không resume, vì history bị cắt) và có nước đi.
        com.google.android.material.button.MaterialButton btnAnalyze = dialogView.findViewById(R.id.btn_analyze);
        if (btnAnalyze != null) {
            boolean canAnalyze = chess != null && resumedBaseMoveCount == 0 && chess.getMoveCount() > 0;
            if (!canAnalyze) {
                btnAnalyze.setVisibility(View.GONE);
            } else {
                btnAnalyze.setOnClickListener(v -> onAnalyzeClicked(btnAnalyze));
            }
        }

        dialog.show();
    }

    /** Chạy phân tích ván off-thread (nặng CPU) rồi hiện dialog kết quả. */
    private void onAnalyzeClicked(com.google.android.material.button.MaterialButton btn) {
        if (chess == null) return;
        java.util.List<com.saigonphantomlabs.chess.MoveRecord> history = chess.getMoveHistory();
        btn.setEnabled(false);
        btn.setText(R.string.analyzing);
        new Thread(() -> {
            com.saigonphantomlabs.chess.GameAnalyzer.Result result =
                    com.saigonphantomlabs.chess.GameAnalyzer.analyze(
                            history, new com.saigonphantomlabs.chess.AIEngine());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                btn.setEnabled(true);
                btn.setText(R.string.analyze_board);
                if (result == null) {
                    android.widget.Toast.makeText(this, R.string.analysis_none,
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                showAnalysisDialog(result);
            });
        }).start();
    }

    /** Dialog liệt kê chất lượng từng nước + tổng hợp lỗi & độ chính xác 2 bên. */
    private void showAnalysisDialog(com.saigonphantomlabs.chess.GameAnalyzer.Result res) {
        CharSequence body = buildAnalysisText(res);
        DialogUtils.showBasicDialog(this,
                getString(R.string.analysis_title),
                body,
                getString(R.string.close),
                null,
                R.drawable.ic_moves,
                null,
                null);
    }

    private CharSequence buildAnalysisText(com.saigonphantomlabs.chess.GameAnalyzer.Result res) {
        StringBuilder sb = new StringBuilder();
        String acc = getString(R.string.analysis_accuracy);
        // Tổng hợp 2 bên: độ chính xác + đếm 🔴 blunder / 🟠 mistake / 🟡 inaccuracy
        sb.append(getString(R.string.analysis_white)).append(" — ").append(acc).append(' ')
                .append(res.whiteAccuracy).append("%\n")
                .append("🔴 ").append(res.whiteBlunders)
                .append("   🟠 ").append(res.whiteMistakes)
                .append("   🟡 ").append(res.whiteInaccuracies).append("\n\n");
        sb.append(getString(R.string.analysis_black)).append(" — ").append(acc).append(' ')
                .append(res.blackAccuracy).append("%\n")
                .append("🔴 ").append(res.blackBlunders)
                .append("   🟠 ").append(res.blackMistakes)
                .append("   🟡 ").append(res.blackInaccuracies).append("\n\n");

        // Chỉ liệt kê nước ĐÁNG CHÚ Ý (Inaccuracy trở lên) — gọn & đúng trọng tâm lỗi; vẫn giữ
        // bộ đếm số nước chạy qua mọi ply để đánh số chính xác (Trắng "N.", Đen "N…").
        int fullMove = 1;
        boolean anyNotable = false;
        for (com.saigonphantomlabs.chess.GameAnalyzer.MovePly p : res.plies) {
            String num = p.whiteMove ? (fullMove + ".") : (fullMove + "…");
            if (!p.whiteMove) fullMove++;
            boolean notable = p.quality == com.saigonphantomlabs.chess.GameAnalyzer.Quality.INACCURACY
                    || p.quality == com.saigonphantomlabs.chess.GameAnalyzer.Quality.MISTAKE
                    || p.quality == com.saigonphantomlabs.chess.GameAnalyzer.Quality.BLUNDER;
            if (!notable) continue;
            anyNotable = true;
            sb.append(num).append(' ').append(p.san).append("  ")
                    .append(qualityEmoji(p.quality)).append(' ').append(qualityLabel(p.quality))
                    .append(String.format(java.util.Locale.US, " (−%.1f)", p.lossCp / 100.0))
                    .append('\n');
        }
        if (!anyNotable) {
            // Không có lỗi đáng kể → bỏ phần thừa, chỉ giữ tổng hợp + dòng tích cực
            sb.append("⭐ ").append(getString(R.string.quality_best));
        }
        return sb.toString().trim();
    }

    private String qualityEmoji(com.saigonphantomlabs.chess.GameAnalyzer.Quality q) {
        switch (q) {
            case BLUNDER:    return "🔴";
            case MISTAKE:    return "🟠";
            case INACCURACY: return "🟡";
            case BEST:       return "⭐";
            default:         return "🟢"; // GOOD
        }
    }

    private String qualityLabel(com.saigonphantomlabs.chess.GameAnalyzer.Quality q) {
        switch (q) {
            case BLUNDER:    return getString(R.string.quality_blunder);
            case MISTAKE:    return getString(R.string.quality_mistake);
            case INACCURACY: return getString(R.string.quality_inaccuracy);
            case BEST:       return getString(R.string.quality_best);
            default:         return getString(R.string.quality_good);
        }
    }

    // ───────────────────────── THÀNH TÍCH (achievements) ─────────────────────────

    /** Ghi nhận thành tích khi ván kết thúc + toast huy hiệu vừa mở. Gọi 1 lần/ván. */
    private void recordAchievements(boolean whiteWins, boolean isStalemate) {
        boolean timeout = pendingEndIsTimeout;
        pendingEndIsTimeout = false; // reset bất kể, cho ván sau
        if (chess == null || achievementsRecorded) return;
        achievementsRecorded = true;

        com.saigonphantomlabs.chess.AchievementManager.GameResult r =
                new com.saigonphantomlabs.chess.AchievementManager.GameResult();
        r.vsAi = chess.isVsComputer;
        r.difficulty = chess.difficultyLevel;
        r.draw = isStalemate;
        r.humanWon = !isStalemate && whiteWins;           // người chơi = Trắng
        r.byCheckmate = !isStalemate && !timeout;          // chiếu hết (không phải hết giờ/hoà)
        r.moveCount = chess.getMoveCount();
        r.humanLostPieces = chess.getCapturedWhiteCount(); // quân Trắng bị bắt
        r.durationMs = chess.getGameDurationMs();
        // Quét lịch sử: Trắng có nhập thành / phong cấp không
        for (com.saigonphantomlabs.chess.MoveRecord m : chess.getMoveHistory()) {
            if (m.movedPiece == null
                    || m.movedPiece.color != Chessman.PlayerColor.White) continue;
            if (m.isCastle) r.whiteCastled = true;
            boolean isPawn = m.movedPiece.type == Chessman.ChessmanType.Pawn;
            if (isPawn && m.toY == 0) r.whitePromoted = true; // tốt Trắng tới hàng cuối
        }

        java.util.List<com.saigonphantomlabs.chess.AchievementManager.Achievement> newly =
                new com.saigonphantomlabs.chess.AchievementManager(this).recordGameEnd(r);
        if (!newly.isEmpty()) showAchievementToast(newly);
    }

    private void showAchievementToast(
            java.util.List<com.saigonphantomlabs.chess.AchievementManager.Achievement> list) {
        StringBuilder sb = new StringBuilder();
        for (com.saigonphantomlabs.chess.AchievementManager.Achievement a : list) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(a.emoji).append(' ').append(getString(a.titleRes));
        }
        android.widget.Toast.makeText(this,
                getString(R.string.achievement_unlocked, sb.toString()),
                android.widget.Toast.LENGTH_LONG).show();
    }

    private void initializeStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                w.setNavigationBarColor(Color.TRANSPARENT);
            View dv = w.getDecorView();
            int flags = dv.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            dv.setSystemUiVisibility(flags);
        }
    }

    // ───────────────────────── CÂU ĐỐ (puzzle mate-in-1) ─────────────────────────

    /** Dựng thế cờ câu đố từ FEN + tắt chrome ván thường. */
    private void setupPuzzle(int index) {
        puzzleMode = true;
        puzzleIndex = index;
        puzzlePly = 0;
        chess.isVsComputer = false;
        currentSessionId = newSessionId(); // không dùng (autosave tắt) nhưng tránh null
        currentPuzzle = com.saigonphantomlabs.chess.PuzzleRepository.get(index);
        if (currentPuzzle != null) {
            com.saigonphantomlabs.chess.GameSaveManager.SavedGame sg =
                    com.saigonphantomlabs.chess.FenParser.toSavedGame(currentPuzzle.fen);
            if (sg != null) chess.loadSaveState(sg);
        }
        // Ẩn nút không liên quan (undo/restart/moves) — GIỮ nút Gợi ý (highlight nước giải).
        if (btnUndo != null) btnUndo.setVisibility(View.GONE);
        if (btnRestart != null) btnRestart.setVisibility(View.GONE);
        if (btnMoves != null) btnMoves.setVisibility(View.GONE);
        // Nhãn đúng theo mateIn (tránh luôn ghi "mate in 1")
        String mateLabel = (currentPuzzle != null && currentPuzzle.mateIn == 2)
                ? getString(R.string.puzzle_mate_in_2) : getString(R.string.puzzle_mate_in_1);
        android.widget.Toast.makeText(this,
                getString(R.string.puzzle_item, index + 1) + " · " + mateLabel,
                android.widget.Toast.LENGTH_LONG).show();
    }

    /**
     * Sau nước Trắng chưa-mate trong puzzle: nếu KHỚP nước giải kỳ vọng VÀ còn nước Đen đáp →
     * app tự đi nước Đen rồi chờ nước Trắng kế (mate-in-2+). Ngược lại = SAI → hoàn tác + thử lại.
     */
    private void handlePuzzleWhiteMove() {
        java.util.List<com.saigonphantomlabs.chess.MoveRecord> h = chess.getMoveHistory();
        com.saigonphantomlabs.chess.MoveRecord last = h.isEmpty() ? null : h.get(h.size() - 1);
        int[] expected = currentPuzzle != null ? currentPuzzle.whiteMove(puzzlePly) : null;
        int[] reply = currentPuzzle != null ? currentPuzzle.blackReply(puzzlePly) : null;

        if (matchesMove(last, expected) && reply != null) {
            // Nước Trắng đúng (chưa cuối) → tiến 1 ply, app tự đi nước Đen đáp trả
            puzzlePly++;
            chess.inputLocked = true;
            final int[] r = reply;
            hintHandler.postDelayed(() -> {
                if (chess == null) return;
                chess.doMove(new com.saigonphantomlabs.chess.Point(r[0], r[1]),
                        new com.saigonphantomlabs.chess.Point(r[2], r[3]));
                chess.inputLocked = false;
            }, 450);
        } else {
            // Nước SAI → hoàn tác + nhắc thử lại (chặn input trong lúc chờ)
            chess.inputLocked = true;
            hintHandler.postDelayed(() -> {
                if (chess == null) return;
                if (!chess.isGameEnd() && chess.canUndo()) {
                    chess.undoLastMove();
                    android.widget.Toast.makeText(this, R.string.puzzle_try_again,
                            android.widget.Toast.LENGTH_SHORT).show();
                }
                chess.inputLocked = false;
            }, 500);
        }
    }

    private boolean matchesMove(com.saigonphantomlabs.chess.MoveRecord m, int[] e) {
        return m != null && e != null
                && m.fromX == e[0] && m.fromY == e[1] && m.toX == e[2] && m.toY == e[3];
    }

    private void launchPuzzle(int index) {
        com.saigonphantomlabs.chess.Storage.clearChess();
        Intent i = new Intent(this, ChessBoardActivity.class);
        i.putExtra("PUZZLE_INDEX", index);
        startActivity(i);
    }

    /** Dialog "Giải xong!" — nếu còn câu kế thì cho qua câu tiếp, không thì đóng về danh sách. */
    private void showPuzzleSolvedDialog() {
        if (currentPuzzle != null) {
            com.saigonphantomlabs.chess.PuzzleProgress.markSolved(this, currentPuzzle.id);
        }
        boolean hasNext = com.saigonphantomlabs.chess.PuzzleRepository.get(puzzleIndex + 1) != null;
        DialogUtils.showBasicDialog(this,
                getString(R.string.puzzle_solved_title),
                getString(R.string.puzzle_solved_message),
                getString(hasNext ? R.string.puzzle_next : R.string.close),
                getString(R.string.puzzle_back_list),
                R.drawable.ic_trophy,
                () -> {                       // positive
                    if (hasNext) launchPuzzle(puzzleIndex + 1);
                    finish();
                },
                this::finish);                // negative → về danh sách câu đố
    }

    private void updateSystemBarsTint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View dv = getWindow().getDecorView();
            int flags = dv.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            dv.setSystemUiVisibility(flags);
        }
    }

    // Banner lifecycle thủ công — banner CHỈ refresh khi màn cờ đang hiển thị (autoManageLifecycle=false).
    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) AdManager.INSTANCE.bannerResume(adView);
        startClock();   // tiếp tục đếm giờ (reset mốc → không tính thời gian bị pause)
    }

    @Override
    protected void onPause() {
        // Dừng auto-refresh khi rời màn cờ (sang Menu/VIP/ad…) → hết invalid impression off-screen.
        if (adView != null) AdManager.INSTANCE.bannerPause(adView);
        stopClock();    // tạm dừng đồng hồ khi rời màn (công bằng)
        persistOrClearSave();   // lưu ván dở (hoặc xoá nếu đã kết thúc)
        super.onPause();
    }

    /** Lưu ván dở khi rời màn (nếu còn chơi & đã có nước); ngược lại xoá save. */
    private void persistOrClearSave() {
        if (puzzleMode) return; // câu đố không lưu vào thư viện ván
        if (chess == null || currentSessionId == null) return;
        // Đáng lưu khi: chưa kết thúc & (đã có nước đi MỚI hoặc đang tiếp tục 1 ván resume).
        // resumedSave != null đảm bảo ván vừa resume (undo-stack rỗng) không bị xoá nhầm khi onPause.
        boolean inProgress = !chess.isGameEnd() && (chess.hasMovesMade() || resumedSave != null);
        if (inProgress) {
            com.saigonphantomlabs.chess.GameSaveManager.SavedGame g = chess.captureSaveState();
            g.sessionId = currentSessionId;
            g.savedAtMs = System.currentTimeMillis();
            g.moveCount = resumedBaseMoveCount + chess.getMoveCount(); // tích luỹ ply qua resume
            if (chessClock != null) {
                g.hasClock = true;
                g.whiteMs = chessClock.getWhiteMs();
                g.blackMs = chessClock.getBlackMs();
                g.incrementMs = chessClock.getIncrementMs();
                g.whiteActive = chessClock.isWhiteActive();
            }
            com.saigonphantomlabs.chess.GameSaveManager.saveSlot(this, g);
        } else {
            com.saigonphantomlabs.chess.GameSaveManager.deleteSlot(this, currentSessionId);
        }
    }

    /** Id slot duy nhất cho 1 ván (theo thời điểm bắt đầu). */
    private String newSessionId() {
        return "g" + System.currentTimeMillis();
    }

    /**
     * OPT: Release 3D-piece bitmap cache under memory pressure
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        PieceRenderer.clearCache();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) PieceRenderer.clearCache();
    }

    @Override
    protected void onDestroy() {
        // Cancel all infinite animators — no leaks
        cancelAnim(currentBlinkAnimator);
        currentBlinkAnimator = null;
        cancelAnim(boardPulseAnim);
        boardPulseAnim = null;
        cancelAnim(cornerTRAnim);
        cornerTRAnim = null;
        cancelAnim(cornerBLAnim);
        cornerBLAnim = null;
        if (whiteGlowAnim != null) {
            whiteGlowAnim.cancel();
            whiteGlowAnim = null;
        }
        if (blackGlowAnim != null) {
            blackGlowAnim.cancel();
            blackGlowAnim = null;
        }

        hintHandler.removeCallbacksAndMessages(null);
        stopClock();
        if (chess != null) { chess.clearHint(); chess.cancelAiHandler(); }
        if (Storage.getChess() == chess) Storage.clearChess();
        // Destroy banner thủ công → giải phóng MaxAdView + dừng refresh timer (chống leak/OOM).
        if (adView != null) {
            AdManager.INSTANCE.bannerDestroy(adView);
            adView = null;
        }
        super.onDestroy();
    }

    private void cancelAnim(ObjectAnimator a) {
        if (a != null) a.cancel();
    }
}