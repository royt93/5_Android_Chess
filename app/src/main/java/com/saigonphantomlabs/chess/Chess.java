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

import android.util.Log;

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
    private long gameStartTime = System.currentTimeMillis();

    // AI Mode
    public boolean isVsComputer = false;
    public AIEngine.Difficulty difficultyLevel = AIEngine.Difficulty.EASY;
    private AIEngine aiEngine;
    public boolean isAiThinking = false;
    private android.os.Handler aiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public King whiteKing = null;
    public King blackKing = null;

    // Selection highlight animation
    private Chessman selectedPiece = null;

    // Stats
    private GameStatsManager statsManager;
    private ObjectAnimator selectionPulseAnimator = null;

    // Move history for undo and display
    private Stack<MoveRecord> moveHistory = new Stack<>();

    // Check state tracking
    private ObjectAnimator checkFlashAnimator = null;
    private King kingInCheck = null;

    public Chess(Context ctx, int minDimension, FrameLayout boardLayout) {
        setLayoutParams(ctx, minDimension, boardLayout);
        this.aiEngine = new AIEngine();
        this.statsManager = new GameStatsManager(ctx);
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
        if (gameEnd || isAiThinking)
            return;
        if (isVsComputer && man.color != Chessman.PlayerColor.White)
            return; // Block user from clicking Black pieces in PvE (Assuming Player is White)

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
        Log.d("roy93~", "doMove: confirmed from (" + from.x + "," + from.y + ") to (" + to.x + "," + to.y + ")");
        // Store move info before making the move
        Chessman movedPiece = chessmen[from.x][from.y];
        Chessman capturedPiece = chessmen[to.x][to.y];
        boolean wasFirstMove = (movedPiece.type == Chessman.ChessmanType.Pawn) && ((Pawn) movedPiece).firstMove;

        if (move(from.x, from.y, to.x, to.y)) {
            Log.d("roy93~", "doMove: move executed successfully on data model");
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
                                    && chessmen[to.x][to.y].getPoint().y == 7)) {
                if (isVsComputer && chessmen[to.x][to.y].color == Chessman.PlayerColor.Black) {
                    // Auto-promote for AI (to Queen)
                    manToPromote = chessmen[to.x][to.y];
                    promotionResault(Chessman.ChessmanType.Queen);
                } else {
                    promote(chessmen[to.x][to.y]);
                }
            }

            // Check opponent's king status after move
            Log.d("roy93~", "doMove: checking opponent king status...");
            checkOpponentKingStatus();

            changeTurn();
            lastManPoint = null;
        } else {
            Log.e("roy93~", "doMove: move failed!");
        }
    }

    public boolean move(int xf, int yf, int xt, int yt) {
        Chessman tempMan = chessmen[xt][yt];
        chessmen[xt][yt] = chessmen[xf][yf];
        chessmen[xt][yt].setPoint(new Point(xt, yt));
        chessmen[xf][yf] = null;

        King myKing = (chessmen[xt][yt].color == Chessman.PlayerColor.White) ? whiteKing : blackKing;

        // Only check if our king is safe (not in check)
        // We don't care about Stalemate here because the turn will pass to the opponent
        boolean isKingSafe = myKing.isPointSafe();
        Log.d("roy93~", "move: " + myKing.color + " king safe=" + isKingSafe);

        if (isKingSafe) {
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
        Log.d("roy93~", "checkOpponentKingStatus: checking " + opponentKing.color + " king");

        King.KingRiskType status = validateKing(opponentKing);
        Log.d("roy93~", "checkOpponentKingStatus: status = " + status);

        // Clear any previous check animation
        clearCheckAnimation();

        if (status == King.KingRiskType.CheckMate) {
            Log.d("roy93~", "checkOpponentKingStatus: CHECKMATE detected!");
            gameEnd = true;
            playCheckSound();
            showCheckAnimation(opponentKing);
            // Show game end dialog - current player wins
            boolean whiteWins = whichPlayerTurn == Chessman.PlayerColor.White;
            ((ChessBoardActivity) ctx).showCustomGameEndDialog(
                    whiteWins, false);

            if (isVsComputer) {
                // If White wins (Player), result = 1. If Black wins (AI), result = -1
                int result = whiteWins ? 1 : -1;
                long duration = System.currentTimeMillis() - gameStartTime;
                statsManager.saveGameResult(difficultyLevel, result, duration);
            }
        } else if (status == King.KingRiskType.Stalemate) {
            Log.d("roy93~", "checkOpponentKingStatus: STALEMATE detected!");
            gameEnd = true;
            // Show stalemate dialog - draw (isStalemate = true)
            // Show stalemate dialog - draw (isStalemate = true)
            ((ChessBoardActivity) ctx).showCustomGameEndDialog(false, true);

            if (isVsComputer) {
                long duration = System.currentTimeMillis() - gameStartTime;
                statsManager.saveGameResult(difficultyLevel, 0, duration);
            }
        } else if (status == King.KingRiskType.Check) {
            Log.d("roy93~", "checkOpponentKingStatus: CHECK detected!");
            kingInCheck = opponentKing;
            playCheckSound();
            showCheckAnimation(opponentKing);
            Toast.makeText(ctx, R.string.check, Toast.LENGTH_SHORT).show();
        } else {
            Log.d("roy93~", "checkOpponentKingStatus: SAFE - game continues");
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
        Log.d("roy93~",
                "validateKing: checking " + k.color + " king at (" + k.getPoint().x + "," + k.getPoint().y + ")");
        k.generateMoves();
        Log.d("roy93~", "validateKing: king has " + k.moves.size() + " potential moves");

        boolean inCheck = !k.isPointSafe();
        Log.d("roy93~", "validateKing: inCheck = " + inCheck);

        // Check if king can move to any safe square (with simulation)
        boolean kingCanMove = false;
        for (Point p : k.moves) {
            boolean moveSafe = isKingMoveSafe(k, p);
            Log.d("roy93~", "validateKing: king move to (" + p.x + "," + p.y + ") safe = " + moveSafe);
            if (moveSafe) {
                kingCanMove = true;
                break;
            }
        }
        Log.d("roy93~", "validateKing: kingCanMove = " + kingCanMove);

        // Check if any other piece has legal moves
        boolean otherPiecesCanMove = hasAnyLegalMove(k.color);
        Log.d("roy93~", "validateKing: otherPiecesCanMove = " + otherPiecesCanMove);

        if (inCheck) {
            // King is in check
            Log.d("roy93~", "validateKing: King IS in check");
            if (kingCanMove) {
                Log.d("roy93~", "validateKing: returning CHECK (king can escape)");
                return King.KingRiskType.Check; // King can escape
            }
            boolean canSave = canAnyPieceSaveKing(k);
            Log.d("roy93~", "validateKing: canAnyPieceSaveKing = " + canSave);
            if (canSave) {
                Log.d("roy93~", "validateKing: returning CHECK (piece can save)");
                return King.KingRiskType.Check; // Another piece can save
            }
            Log.d("roy93~", "validateKing: returning CHECKMATE");
            return King.KingRiskType.CheckMate;
        } else {
            // King is NOT in check
            Log.d("roy93~", "validateKing: King is NOT in check");
            if (!kingCanMove && !otherPiecesCanMove) {
                Log.d("roy93~", "validateKing: returning STALEMATE");
                return King.KingRiskType.Stalemate;
            }
            Log.d("roy93~", "validateKing: returning SAFE");
            return King.KingRiskType.Safe;
        }
    }

    /**
     * Check if King moving to target point would be safe (simulates the move)
     */
    private boolean isKingMoveSafe(King k, Point target) {
        Point originalPos = k.getPoint();
        Chessman capturedPiece = chessmen[target.x][target.y];

        // Simulate King's move
        chessmen[target.x][target.y] = k;
        chessmen[originalPos.x][originalPos.y] = null;
        k.setPoint(target);

        // Check if King is safe at new position
        boolean isSafe = k.isPointSafe();
        Log.d("roy93~",
                "isKingMoveSafe: " + k.color + " King moving to (" + target.x + "," + target.y + ") safe=" + isSafe);

        // Undo simulation
        chessmen[originalPos.x][originalPos.y] = k;
        chessmen[target.x][target.y] = capturedPiece;
        k.setPoint(originalPos);

        return isSafe;
    }

    /**
     * Check if any piece of the given color has any legal moves
     */
    private boolean hasAnyLegalMove(Chessman.PlayerColor color) {
        King k = (color == Chessman.PlayerColor.White) ? whiteKing : blackKing;
        Log.d("roy93~", "hasAnyLegalMove: checking for " + color);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Chessman piece = chessmen[x][y];
                if (piece != null && !piece.isDead && piece.color == color
                        && piece.type != Chessman.ChessmanType.King) {
                    piece.generateMoves();
                    // Log.d("roy93~", "hasAnyLegalMove: piece " + piece.type + " at (" + x + "," +
                    // y + ") has " + piece.moves.size() + " moves");

                    // Check if any move is legal (doesn't put own king in check)
                    for (Point target : piece.moves) {
                        if (isMoveLegal(piece, target, k)) {
                            Log.d("roy93~", "hasAnyLegalMove: FOUND legal move! Piece " + piece.type + " at (" + x + ","
                                    + y + ") to (" + target.x + "," + target.y + ")");
                            return true;
                        }
                    }
                }
            }
        }
        Log.d("roy93~", "hasAnyLegalMove: NO legal moves found for any piece!");
        return false;
    }

    /**
     * Check if a move is legal (doesn't put own king in check)
     */
    private boolean isMoveLegal(Chessman piece, Point target, King k) {
        Point originalPos = piece.getPoint();
        Chessman capturedPiece = chessmen[target.x][target.y];

        // Simulate the move
        chessmen[target.x][target.y] = piece;
        chessmen[originalPos.x][originalPos.y] = null;
        piece.setPoint(target);

        // Check if our king is in check after this move
        boolean kingInCheck = !k.isPointSafe();
        // Log.d("roy93~", "isMoveLegal: checking " + piece.type + " to (" + target.x +
        // "," + target.y + ") -> kingInCheck=" + kingInCheck);

        // Undo the simulation
        chessmen[originalPos.x][originalPos.y] = piece;
        chessmen[target.x][target.y] = capturedPiece;
        piece.setPoint(originalPos);

        return !kingInCheck;
    }

    /**
     * Check if any piece of the same color can make a move that removes the check
     */
    private boolean canAnyPieceSaveKing(King k) {
        Chessman.PlayerColor kingColor = k.color;
        Log.d("roy93~", "canAnyPieceSaveKing: checking for " + kingColor);

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

        if (isVsComputer && whichPlayerTurn == Chessman.PlayerColor.Black && !gameEnd) {
            triggerAITurn();
        }
    }

    private void triggerAITurn() {
        isAiThinking = true;
        // Optional: Show loading state in UI

        new Thread(() -> {
            try {
                // Determine AI think time based on difficulty to feel "natural"
                // Easy: fast, Hard: slow computation usually does it, but we can add delay
                long delay = aiEngine.getThinkDelay(difficultyLevel);
                Thread.sleep(delay);

                MoveRecord bestMove = aiEngine.getBestMove(this, difficultyLevel, Chessman.PlayerColor.Black);

                aiHandler.post(() -> {
                    // Safety checks: if game ended or activity destroyed (ctx handling not perfect
                    // here but gameEnd flag helps)
                    if (gameEnd || !isAiThinking) {
                        isAiThinking = false;
                        return;
                    }

                    if (bestMove != null) {
                        // We need to find the specific Chessman instance on the current board
                        // because the one in MoveRecord came from state prior to calculation or
                        // simulation?
                        // Actually, MoveRecord stores the reference to the piece.
                        // IMPORTANT: Simulation in AIEngine DOES modify and revert the board.
                        // So the piece instances are valid.

                        Point from = new Point(bestMove.fromX, bestMove.fromY);
                        Point to = new Point(bestMove.toX, bestMove.toY);
                        doMove(from, to);
                    } else {
                        // AI cannot move? Should have been detected as game over already.
                        // Re-check game status
                        checkOpponentKingStatus();
                    }
                    isAiThinking = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                isAiThinking = false;
            }
        }).start();
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
     * Get total move count
     */
    public int getMoveCount() {
        return moveHistory.size();
    }

    /**
     * Get game duration in milliseconds
     */
    public long getGameDurationMs() {
        return System.currentTimeMillis() - gameStartTime;
    }

    /**
     * Get formatted game duration string (MM:SS)
     */
    public String getFormattedDuration() {
        long durationMs = getGameDurationMs();
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / 1000) / 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Get count of captured white pieces
     */
    public int getCapturedWhiteCount() {
        int count = 0;
        for (Chessman man : deadMen) {
            if (man.color == Chessman.PlayerColor.White)
                count++;
        }
        return count;
    }

    /**
     * Get count of captured black pieces
     */
    public int getCapturedBlackCount() {
        int count = 0;
        for (Chessman man : deadMen) {
            if (man.color == Chessman.PlayerColor.Black)
                count++;
        }
        return count;
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
        gameStartTime = System.currentTimeMillis();

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