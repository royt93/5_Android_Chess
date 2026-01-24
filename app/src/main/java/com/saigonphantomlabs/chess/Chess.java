package com.saigonphantomlabs.chess;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.saigonphantomlabs.ChessBoardActivity;

import java.util.ArrayList;
import java.util.Stack;

public class Chess {
    public ArrayList<Chessman> deadMen = new ArrayList<>();
    public Chessman[/* column - x */][/* row - y */] chessmen = new Chessman[8][8];
    public Chessman.PlayerColor whichPlayerTurn = Chessman.PlayerColor.Black;
    public Point lastManPoint = null;
    public Context ctx;
    public int minDimension = 0;
    private Chessman manToPromote = null;
    private FrameLayout boardLayout = null;

    public ArrayList<View> validMoveButtons = new ArrayList<>();

    // Game state
    private boolean gameEnd = false;

    public King whiteKing = null;
    public King blackKing = null;

    // Selection highlight animation
    private Chessman selectedPiece = null;
    private ObjectAnimator selectionPulseAnimator = null;

    // Move history for undo and display
    private Stack<MoveRecord> moveHistory = new Stack<>();

    // Check state tracking
    private ObjectAnimator checkFlashAnimator = null;
    private King kingInCheck = null;

    public Chess(Context ctx, int minDimension, FrameLayout boardLayout) {
        setLayoutParams(ctx, minDimension, boardLayout);

        /*
         * BOARD
         * < X >
         * RkbQKBkR WBWBWBWB
         * < PPPPPPPP BWBWBWBW
         * ..BLACK. WBWBWBWB
         * y ........ BWBWBWBW
         * ........ WBWBWBWB
         * > .WHITE.. BWBWBWBW
         * PPPPPPPP WBWBWBWB
         * RkBQKBkR BWBWBWBW
         */

        // first row
        chessmen[0][0] = new Rook(new Point(0, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[1][0] = new Knight(new Point(1, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[2][0] = new Bishop(new Point(2, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[3][0] = new Queen(new Point(3, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[4][0] = blackKing = new King(new Point(4, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[5][0] = new Bishop(new Point(5, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[6][0] = new Knight(new Point(6, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[7][0] = new Rook(new Point(7, 0), Chessman.PlayerColor.Black, minDimension, this);

        // second row
        chessmen[0][1] = new Pawn(new Point(0, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[1][1] = new Pawn(new Point(1, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[2][1] = new Pawn(new Point(2, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[3][1] = new Pawn(new Point(3, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[4][1] = new Pawn(new Point(4, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[5][1] = new Pawn(new Point(5, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[6][1] = new Pawn(new Point(6, 1), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[7][1] = new Pawn(new Point(7, 1), Chessman.PlayerColor.Black, minDimension, this);

        // seventh row
        chessmen[0][6] = new Pawn(new Point(0, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[1][6] = new Pawn(new Point(1, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[2][6] = new Pawn(new Point(2, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[3][6] = new Pawn(new Point(3, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[4][6] = new Pawn(new Point(4, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[5][6] = new Pawn(new Point(5, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[6][6] = new Pawn(new Point(6, 6), Chessman.PlayerColor.White, minDimension, this);
        chessmen[7][6] = new Pawn(new Point(7, 6), Chessman.PlayerColor.White, minDimension, this);

        // eighth row
        chessmen[0][7] = new Rook(new Point(0, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[1][7] = new Knight(new Point(1, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[2][7] = new Bishop(new Point(2, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[3][7] = new Queen(new Point(3, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[4][7] = whiteKing = new King(new Point(4, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[5][7] = new Bishop(new Point(5, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[6][7] = new Knight(new Point(6, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[7][7] = new Rook(new Point(7, 7), Chessman.PlayerColor.White, minDimension, this);

        for (int i = 0; i < 8; i++)
            for (int j = 2; j < 6; j++) {
                chessmen[i][j] = null;
            }

        addMenToBoard(boardLayout);
        whichPlayerTurn = Chessman.PlayerColor.Black;
        changeTurn();
    }

    public void changeLayout(Context ctx, int minDimension, FrameLayout boardLayout) {
        setLayoutParams(ctx, minDimension, boardLayout);
        addMenToBoard(boardLayout);
        if (whichPlayerTurn == Chessman.PlayerColor.White)
            whichPlayerTurn = Chessman.PlayerColor.Black;
        else
            whichPlayerTurn = Chessman.PlayerColor.White;
        changeTurn();
    }

    private void setLayoutParams(Context ctx, int minDimension, FrameLayout boardLayout) {
        this.ctx = ctx;
        this.minDimension = minDimension;
        this.boardLayout = boardLayout;
    }

    public void onManClick(Chessman man) {
        if (gameEnd)
            return;
        if (man.color == whichPlayerTurn) {
            // First, reset valid move buttons (this also clears old selection)
            resetValidMoveButtons();

            lastManPoint = man.getPoint();

            // Add haptic feedback on selection
            performHapticFeedback(man.button, HapticFeedbackConstants.VIRTUAL_KEY);

            // Set new selection and start highlight animation AFTER reset
            selectedPiece = man;
            startSelectionPulse(man);

            chessmen[lastManPoint.x][lastManPoint.y].generateMoves();
            addValidMoveButtons(chessmen[lastManPoint.x][lastManPoint.y].moves);
        } else if (lastManPoint != null && chessmen[lastManPoint.x][lastManPoint.y].moves.contains(man.getPoint())) {
            onBoardClick(man.getPoint().x, man.getPoint().y);
        }
    }

    public void onBoardClick(int x, int y) {
        if (gameEnd)
            return;
        if (lastManPoint == null)
            return;
        Point clickPoint = new Point(x, y);
        if (chessmen[lastManPoint.x][lastManPoint.y].moves.contains(clickPoint)) {
            doMove(lastManPoint, clickPoint);
        }
    }

    public void doMove(Point from, Point to) {
        // Store move info before making the move
        Chessman movedPiece = chessmen[from.x][from.y];
        Chessman capturedPiece = chessmen[to.x][to.y];
        boolean wasFirstMove = (movedPiece.type == Chessman.ChessmanType.Pawn) && ((Pawn) movedPiece).firstMove;

        if (move(from.x, from.y, to.x, to.y)) {
            // Record the move for history/undo
            MoveRecord record = new MoveRecord(from.x, from.y, to.x, to.y,
                    movedPiece, capturedPiece, null, wasFirstMove);
            moveHistory.push(record);

            // Update undo button visibility
            ((ChessBoardActivity) ctx).updateUndoButton(true);

            resetValidMoveButtons();
            if (chessmen[to.x][to.y].type == Chessman.ChessmanType.Pawn &&
                    (chessmen[to.x][to.y].color == Chessman.PlayerColor.White && chessmen[to.x][to.y].getPoint().y == 0
                            || chessmen[to.x][to.y].color == Chessman.PlayerColor.Black
                                    && chessmen[to.x][to.y].getPoint().y == 7))
                promote(chessmen[to.x][to.y]);

            // Check opponent's king status after move
            checkOpponentKingStatus();

            changeTurn();
            lastManPoint = null;
        }
    }

    public boolean move(int xf, int yf, int xt, int yt) {
        Chessman tempMan = chessmen[xt][yt];
        chessmen[xt][yt] = chessmen[xf][yf];
        chessmen[xt][yt].setPoint(new Point(xt, yt));
        chessmen[xf][yf] = null;

        King.KingRiskType relatedKingStatus;
        if (chessmen[xt][yt].color == Chessman.PlayerColor.White)
            relatedKingStatus = validateKing(whiteKing);
        else
            relatedKingStatus = validateKing(blackKing);

        if (relatedKingStatus == King.KingRiskType.Safe) {
            if (tempMan != null) {
                kill(tempMan);
                // Add captured piece to display
                ((ChessBoardActivity) ctx).addCapturedPiece(tempMan);
            }
            chessmen[xt][yt].setPoint(new Point(xt, yt));
            chessmen[xt][yt].moveButton(xt, yt);
            if (chessmen[xt][yt].type == Chessman.ChessmanType.Pawn)
                ((Pawn) chessmen[xt][yt]).firstMove = false;
            return true;
        }

        chessmen[xt][yt].setPoint(new Point(xf, yf));
        chessmen[xf][yf] = chessmen[xt][yt];
        chessmen[xt][yt] = tempMan;
        // Play illegal move sound
        playIllegalMoveSound();
        Toast.makeText(ctx, R.string.illegal_move, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Check opponent's king status after a move
     */
    private void checkOpponentKingStatus() {
        King opponentKing = (whichPlayerTurn == Chessman.PlayerColor.White) ? blackKing : whiteKing;
        King.KingRiskType status = validateKing(opponentKing);

        // Clear any previous check animation
        clearCheckAnimation();

        if (status == King.KingRiskType.CheckMate) {
            gameEnd = true;
            playCheckSound();
            showCheckAnimation(opponentKing);
            // Show game end dialog
            ((ChessBoardActivity) ctx).showGameEndDialog(
                    whichPlayerTurn == Chessman.PlayerColor.White);
        } else if (status == King.KingRiskType.Check) {
            kingInCheck = opponentKing;
            playCheckSound();
            showCheckAnimation(opponentKing);
            Toast.makeText(ctx, R.string.check, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show flashing animation on king when in check
     */
    private void showCheckAnimation(King king) {
        if (king == null || king.button == null)
            return;

        // Flash animation using color overlay
        checkFlashAnimator = ObjectAnimator.ofFloat(king.button, "alpha", 1f, 0.3f);
        checkFlashAnimator.setDuration(200);
        checkFlashAnimator.setRepeatCount(5);
        checkFlashAnimator.setRepeatMode(ValueAnimator.REVERSE);
        checkFlashAnimator.start();
    }

    /**
     * Clear check animation
     */
    private void clearCheckAnimation() {
        if (checkFlashAnimator != null) {
            checkFlashAnimator.cancel();
            checkFlashAnimator = null;
        }
        if (kingInCheck != null && kingInCheck.button != null) {
            kingInCheck.button.setAlpha(1f);
        }
        kingInCheck = null;
    }

    /**
     * Play check warning sound
     */
    private void playCheckSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(ctx, R.raw.chess_1);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Play illegal move sound
     */
    private void playIllegalMoveSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(ctx, R.raw.chess_2);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private King.KingRiskType validateKing(King k) {
        k.generateMoves();

        boolean check = !k.isPointSafe();

        if (!check) {
            return King.KingRiskType.Safe;
        }

        // King is in check - need to determine if it's checkmate
        // Checkmate occurs when:
        // 1. King cannot move to a safe square, AND
        // 2. No other piece can block/capture to remove the check

        // Check if king can escape
        for (Point p : k.moves) {
            if (k.isPointSafe(p)) {
                return King.KingRiskType.Check; // King can escape
            }
        }

        // King cannot move - check if any other piece can save the king
        if (canAnyPieceSaveKing(k)) {
            return King.KingRiskType.Check; // Another piece can save
        }

        // No escape possible - it's checkmate
        return King.KingRiskType.CheckMate;
    }

    /**
     * Check if any piece of the same color can make a move that removes the check
     */
    private boolean canAnyPieceSaveKing(King k) {
        Chessman.PlayerColor kingColor = k.color;

        // Try all pieces of the same color
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Chessman piece = chessmen[x][y];
                if (piece != null && !piece.isDead && piece.color == kingColor
                        && piece.type != Chessman.ChessmanType.King) {
                    // Generate moves for this piece
                    piece.generateMoves();

                    // Try each move and see if it removes the check
                    for (Point targetMove : piece.moves) {
                        if (wouldMoveSaveKing(piece, targetMove, k)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Simulate a move and check if it would save the king from check
     */
    private boolean wouldMoveSaveKing(Chessman piece, Point target, King k) {
        Point originalPos = piece.getPoint();
        Chessman capturedPiece = chessmen[target.x][target.y];

        // Simulate the move
        chessmen[target.x][target.y] = piece;
        chessmen[originalPos.x][originalPos.y] = null;
        piece.setPoint(target);

        // Check if king is still in check
        boolean stillInCheck = !k.isPointSafe();

        // Undo the simulation
        chessmen[originalPos.x][originalPos.y] = piece;
        chessmen[target.x][target.y] = capturedPiece;
        piece.setPoint(originalPos);

        return !stillInCheck;
    }

    public void kill(Chessman m) {
        deadMen.add(m);
        m.isDead = true;

        // Haptic feedback for capture
        performCaptureHaptic();

        // Add death animation - spin and fade out
        m.button.animate()
                .scaleX(0.3f)
                .scaleY(0.3f)
                .alpha(0f)
                .rotation(360f)
                .setDuration(250)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    // Remove button after animation completes
                    if (m.button.getParent() != null) {
                        ((ViewGroup) m.button.getParent()).removeView(m.button);
                    }
                })
                .start();
    }

    public void changeTurn() {
        if (whichPlayerTurn == Chessman.PlayerColor.White)
            whichPlayerTurn = Chessman.PlayerColor.Black;
        else
            whichPlayerTurn = Chessman.PlayerColor.White;
        ((ChessBoardActivity) ctx).animateTurnChange(whichPlayerTurn);
    }

    public void promote(Chessman man) {
        manToPromote = man;
        ((ChessBoardActivity) ctx).showPromotionActivity();
    }

    public void promotionResault(Chessman.ChessmanType toType) {

        Chessman newType;
        switch (toType) {
            case Queen:
                newType = new Queen(manToPromote.getPoint(), manToPromote.color, minDimension, this);
                break;
            case Rook:
                newType = new Rook(manToPromote.getPoint(), manToPromote.color, minDimension, this);
                break;
            case Bishop:
                newType = new Bishop(manToPromote.getPoint(), manToPromote.color, minDimension, this);
                break;
            case Knight:
                newType = new Knight(manToPromote.getPoint(), manToPromote.color, minDimension, this);
                break;
            default:
                return;
        }

        // Create the new piece button
        newType.createButton();
        chessmen[manToPromote.getPoint().x][manToPromote.getPoint().y] = newType;

        // Start promotion animation
        animatePromotion(manToPromote, newType);
    }

    private void animatePromotion(Chessman oldPawn, Chessman newPiece) {
        // Play special promotion sound effect
        playPromotionSound();

        // Phase 1: Pawn transformation animation (disappear with magical effect)
        oldPawn.button.animate()
                .scaleX(0.2f)
                .scaleY(0.2f)
                .rotation(720f) // Two full rotations
                .alpha(0.3f)
                .setDuration(400)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    // Remove old pawn
                    ((ViewGroup) oldPawn.button.getParent()).removeView(oldPawn.button);

                    // Phase 2: New piece appears with dramatic entrance
                    newPiece.button.setScaleX(0.1f);
                    newPiece.button.setScaleY(0.1f);
                    newPiece.button.setAlpha(0f);
                    newPiece.button.setRotation(-180f);

                    // Add new piece to board
                    boardLayout.addView(newPiece.button);

                    // Play transformation completion sound
                    playTransformationSound();

                    // Dramatic entrance animation
                    newPiece.button.animate()
                            .scaleX(1.3f)
                            .scaleY(1.3f)
                            .rotation(0f)
                            .alpha(1.0f)
                            .setDuration(500)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .withEndAction(() -> {
                                // Phase 3: Settle to normal size with bounce
                                newPiece.button.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(300)
                                        .setInterpolator(new BounceInterpolator())
                                        .withEndAction(() -> {
                                            // Ensure all properties are reset
                                            newPiece.resetButtonState();
                                        })
                                        .start();
                            })
                            .start();
                })
                .start();
    }

    private void playPromotionSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(ctx, R.raw.chess_1);
            if (mp != null) {
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playTransformationSound() {
        try {
            MediaPlayer mp = MediaPlayer.create(ctx, R.raw.chess_2);
            if (mp != null) {
                mp.start();
                mp.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMenToBoard(FrameLayout boardLayout) {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                if (chessmen[i][j] == null)
                    continue;
                chessmen[i][j].createButton();
                boardLayout.addView(chessmen[i][j].button);
            }
    }

    public void createValidMoveButton(Point p, int index) {
        ImageButton btn = new ImageButton(ctx);
        int width = minDimension / 8;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);

        lp.setMargins(width * p.x, width * p.y, minDimension - (width * p.x + width),
                minDimension - (width * p.y + width));

        btn.setLayoutParams(lp);
        btn.setBackground(ctx.getResources().getDrawable(R.drawable.ic_point, ctx.getTheme()));

        btn.setOnClickListener(v -> {
            onBoardClick(p.x, p.y);
        });

        // Start with invisible and scaled down
        btn.setAlpha(0f);
        btn.setScaleX(0f);
        btn.setScaleY(0f);

        validMoveButtons.add(btn);
        boardLayout.addView(btn);

        // Animate pop-in with stagger effect
        btn.animate()
                .alpha(0.85f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 30L)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();
    }

    public void addValidMoveButtons(ArrayList<Point> validMoves) {
        int index = 0;
        for (Point p : validMoves) {
            createValidMoveButton(p, index);
            index++;
        }
    }

    public void resetValidMoveButtons() {
        // Clear selection when moves are reset
        clearSelectionHighlight();

        for (View v : validMoveButtons)
            ((ViewGroup) v.getParent()).removeView(v);

        validMoveButtons.clear();
    }

    /**
     * Start a pulsing animation on the selected piece
     */
    private void startSelectionPulse(Chessman man) {
        if (man == null || man.button == null)
            return;

        // Create pulse animation using PropertyValuesHolder
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f);

        selectionPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(man.button, scaleX, scaleY);
        selectionPulseAnimator.setDuration(800);
        selectionPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        selectionPulseAnimator.start();
    }

    /**
     * Clear selection highlight from previously selected piece
     */
    private void clearSelectionHighlight() {
        if (selectionPulseAnimator != null) {
            selectionPulseAnimator.cancel();
            selectionPulseAnimator = null;
        }
        if (selectedPiece != null && selectedPiece.button != null) {
            // Reset to normal state
            selectedPiece.button.setScaleX(1.0f);
            selectedPiece.button.setScaleY(1.0f);
        }
        selectedPiece = null;
    }

    /**
     * Perform haptic feedback for tactile response
     */
    private void performHapticFeedback(View view, int feedbackType) {
        if (view != null) {
            view.performHapticFeedback(feedbackType,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    /**
     * Perform stronger haptic feedback for capture actions
     */
    public void performCaptureHaptic() {
        if (ctx != null) {
            Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        }
    }

    /**
     * Get move history for display
     */
    public ArrayList<MoveRecord> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    /**
     * Check if undo is available
     */
    public boolean canUndo() {
        return !moveHistory.isEmpty();
    }

    /**
     * Undo the last move
     */
    public void undoLastMove() {
        if (moveHistory.isEmpty()) {
            Toast.makeText(ctx, R.string.no_undo_available, Toast.LENGTH_SHORT).show();
            return;
        }

        MoveRecord lastMove = moveHistory.pop();

        // Clear any selection
        resetValidMoveButtons();
        clearCheckAnimation();

        // Move piece back to original position
        Chessman movedPiece = chessmen[lastMove.toX][lastMove.toY];
        chessmen[lastMove.fromX][lastMove.fromY] = movedPiece;
        chessmen[lastMove.toX][lastMove.toY] = null;
        movedPiece.setPoint(new Point(lastMove.fromX, lastMove.fromY));

        // Animate piece back
        movedPiece.moveButton(lastMove.fromX, lastMove.fromY);

        // Restore first move status for pawns
        if (lastMove.wasFirstMove && movedPiece.type == Chessman.ChessmanType.Pawn) {
            ((Pawn) movedPiece).firstMove = true;
        }

        // Restore captured piece if any
        if (lastMove.capturedPiece != null) {
            Chessman restored = lastMove.capturedPiece;
            chessmen[lastMove.toX][lastMove.toY] = restored;
            restored.setPoint(new Point(lastMove.toX, lastMove.toY));
            restored.isDead = false;
            deadMen.remove(restored);

            // Recreate button and animate back in
            restored.createButton();
            restored.button.setScaleX(0f);
            restored.button.setScaleY(0f);
            restored.button.setAlpha(0f);
            boardLayout.addView(restored.button);

            restored.button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new OvershootInterpolator())
                    .start();

            // Remove from captured pieces display
            ((ChessBoardActivity) ctx).removeCapturedPiece(restored);
        }

        // Switch turn back
        if (whichPlayerTurn == Chessman.PlayerColor.White)
            whichPlayerTurn = Chessman.PlayerColor.Black;
        else
            whichPlayerTurn = Chessman.PlayerColor.White;

        ((ChessBoardActivity) ctx).animateTurnChange(whichPlayerTurn);

        // Reset game end state if was checkmate
        gameEnd = false;

        // Update undo button visibility
        ((ChessBoardActivity) ctx).updateUndoButton(!moveHistory.isEmpty());

        // Play undo sound
        playIllegalMoveSound();
    }

    /**
     * Reset game to initial state
     */
    public void resetGame() {
        // Clear history
        moveHistory.clear();
        deadMen.clear();
        gameEnd = false;

        // Clear animations
        clearSelectionHighlight();
        clearCheckAnimation();
        resetValidMoveButtons();

        // Remove all pieces from board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (chessmen[i][j] != null && chessmen[i][j].button != null) {
                    if (chessmen[i][j].button.getParent() != null) {
                        ((ViewGroup) chessmen[i][j].button.getParent()).removeView(chessmen[i][j].button);
                    }
                }
                chessmen[i][j] = null;
            }
        }

        // Reinitialize pieces
        initializePieces();
        addMenToBoard(boardLayout);
        whichPlayerTurn = Chessman.PlayerColor.Black;
        changeTurn();

        // Update UI
        ((ChessBoardActivity) ctx).updateUndoButton(false);
        ((ChessBoardActivity) ctx).clearCapturedPieces();
    }

    /**
     * Initialize pieces to starting positions
     */
    private void initializePieces() {
        // first row
        chessmen[0][0] = new Rook(new Point(0, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[1][0] = new Knight(new Point(1, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[2][0] = new Bishop(new Point(2, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[3][0] = new Queen(new Point(3, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[4][0] = blackKing = new King(new Point(4, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[5][0] = new Bishop(new Point(5, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[6][0] = new Knight(new Point(6, 0), Chessman.PlayerColor.Black, minDimension, this);
        chessmen[7][0] = new Rook(new Point(7, 0), Chessman.PlayerColor.Black, minDimension, this);

        // second row
        for (int i = 0; i < 8; i++) {
            chessmen[i][1] = new Pawn(new Point(i, 1), Chessman.PlayerColor.Black, minDimension, this);
        }

        // seventh row
        for (int i = 0; i < 8; i++) {
            chessmen[i][6] = new Pawn(new Point(i, 6), Chessman.PlayerColor.White, minDimension, this);
        }

        // eighth row
        chessmen[0][7] = new Rook(new Point(0, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[1][7] = new Knight(new Point(1, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[2][7] = new Bishop(new Point(2, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[3][7] = new Queen(new Point(3, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[4][7] = whiteKing = new King(new Point(4, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[5][7] = new Bishop(new Point(5, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[6][7] = new Knight(new Point(6, 7), Chessman.PlayerColor.White, minDimension, this);
        chessmen[7][7] = new Rook(new Point(7, 7), Chessman.PlayerColor.White, minDimension, this);

        // Empty middle
        for (int i = 0; i < 8; i++) {
            for (int j = 2; j < 6; j++) {
                chessmen[i][j] = null;
            }
        }
    }
}