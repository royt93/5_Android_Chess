package com.saigonphantomlabs.chess;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Stack;

public class Chess {
    // OPT: pre-sized to typical game max (32 pieces can be captured)
    public ArrayList<Chessman> deadMen = new ArrayList<>(32);
    public Chessman[/* column - x */][/* row - y */] chessmen = new Chessman[8][8];
    public Chessman.PlayerColor whichPlayerTurn = Chessman.PlayerColor.Black;
    public Point lastManPoint = null;
    // ctx giữ Activity → WeakReference để Chess (dù bị giữ ngoài ý muốn) không leak Activity.
    private java.lang.ref.WeakReference<Context> ctxRef;

    /** Context đang gắn (có thể null nếu Activity đã bị thu hồi hoặc model-only mode). */
    public Context getCtx() {
        return ctxRef == null ? null : ctxRef.get();
    }

    public void setCtx(Context c) {
        this.ctxRef = (c == null) ? null : new java.lang.ref.WeakReference<>(c);
    }

    // Callback UI (đã tách khỏi Activity cụ thể) — xem ChessBoardView
    private ChessBoardView boardView;
    public int minDimension = 0;
    private Chessman manToPromote = null;
    private FrameLayout boardLayout = null;

    // OPT: pre-sized to max legal moves per piece (queen max ~27)
    public ArrayList<View> validMoveButtons = new ArrayList<>(28);

    // Âm thanh tách sang ChessAudio (SoundPool) — giảm god-class
    private ChessAudio audio;

    // Game state
    private boolean gameEnd = false;
    private long gameStartTime = System.currentTimeMillis();
    private boolean isDestroyed = false; // [ML-04] Lifecycle guard for AI thread

    // --- Special-move state (chỉ áp dụng cho ván thật) ---
    // true khi AIEngine đang search → tắt castling & en passant để minimax không bị corrupt
    public boolean inAiSimulation = false;
    // Ô con tốt vừa "nhảy qua" (đích của nước bắt tốt qua đường); null nếu không có.
    // Con tốt bị bắt được suy ra trực tiếp từ vị trí khi thực thi, nên không cần lưu riêng.
    public Point enPassantTarget = null;

    // --- Luật hoà (draw) ---
    // Đồng hồ 50 nước: số nửa-nước kể từ lần cuối ĂN QUÂN hoặc ĐẨY TỐT; >=100 → hoà.
    public int halfMoveClock = 0;
    // Đếm số lần mỗi thế cờ xuất hiện (lặp thế 3 lần / threefold → hoà).
    private final java.util.HashMap<String, Integer> positionCounts = new java.util.HashMap<>();

    // AI Mode
    public boolean isVsComputer = false;
    public AIEngine.Difficulty difficultyLevel = AIEngine.Difficulty.EASY;
    private AIEngine aiEngine;
    public boolean isAiThinking = false;
    // Khởi tạo trong constructor production (không field-init) để test seam không chạm Android
    private android.os.Handler aiHandler;

    public King whiteKing = null;
    public King blackKing = null;

    // Animation highlight (chọn quân) + flash (chiếu) tách sang ChessAnimator
    private final ChessAnimator animator = new ChessAnimator();
    // Đánh giá an toàn vua (check/mate/stalemate + hợp lệ nước) tách sang KingSafetyEvaluator
    private final KingSafetyEvaluator kingSafety = new KingSafetyEvaluator(this);

    // Stats
    private GameStatsManager statsManager;

    // Move history for undo and display
    private Stack<MoveRecord> moveHistory = new Stack<>();

    /**
     * Test seam — constructor rỗng chỉ dùng cho unit test rule logic (sinh nước đi).
     * KHÔNG dùng trong app: bỏ qua mọi khởi tạo Android (SoundPool, board, UI).
     * `chessmen` đã được khởi tạo inline thành mảng 8x8 rỗng; test tự đặt quân.
     */
    Chess() {
    }

    /** Test seam: tiêm fake ChessBoardView để kiểm thử execution/undo không cần Activity. */
    void setBoardViewForTest(ChessBoardView view) {
        this.boardView = view;
    }

    public Chess(Context ctx, int minDimension, FrameLayout boardLayout) {
        this.boardView = (ChessBoardView) ctx; // ChessBoardActivity implements ChessBoardView
        setLayoutParams(ctx, minDimension, boardLayout);
        this.aiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        this.aiEngine = new AIEngine();
        this.statsManager = new GameStatsManager(ctx);
        this.audio = new ChessAudio(ctx);
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
        setCtx(ctx);
        this.minDimension = minDimension;
        this.boardLayout = boardLayout;

        // Propagate current dimension to all chessmen
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (chessmen[i][j] != null) {
                    chessmen[i][j].minDimension = minDimension;
                }
            }
        }
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
        ChessLog.d("doMove: confirmed from (" + from.x + "," + from.y + ") to (" + to.x + "," + to.y + ")");
        // Store move info before making the move
        Chessman movedPiece = chessmen[from.x][from.y];

        // --- Phát hiện nước đặc biệt ---
        boolean isCastleMove = (movedPiece instanceof King)
                && Math.abs(to.x - from.x) == 2 && to.y == from.y;
        boolean isEnPassantMove = (movedPiece instanceof Pawn)
                && to.x != from.x && chessmen[to.x][to.y] == null
                && enPassantTarget != null && to.equals(enPassantTarget);

        // Con tốt bị bắt qua đường nằm ở (to.x, from.y), KHÔNG phải ô đích
        Chessman epVictim = null;
        if (isEnPassantMove) {
            epVictim = chessmen[to.x][from.y];
            // Gỡ khỏi data model TRƯỚC khi move() kiểm tra an toàn vua (đúng với discovered check)
            chessmen[to.x][from.y] = null;
        }

        Chessman capturedPiece = chessmen[to.x][to.y]; // null cho en passant & castling
        boolean wasFirstMove = (movedPiece.type == Chessman.ChessmanType.Pawn) && ((Pawn) movedPiece).firstMove;
        boolean movedWasUnmoved =
                (movedPiece instanceof King && !((King) movedPiece).hasMoved)
                || (movedPiece instanceof Rook && !((Rook) movedPiece).hasMoved);
        Point prevEpTarget = enPassantTarget;

        // Reset highlights BEFORE starting animations to prevent property overwrites cancelling the sequence
        resetValidMoveButtons();

        if (move(from.x, from.y, to.x, to.y)) {
            ChessLog.d("doMove: move executed successfully on data model");
            // Record the move for history/undo
            MoveRecord record = new MoveRecord(from.x, from.y, to.x, to.y,
                    movedPiece, isEnPassantMove ? epVictim : capturedPiece, null, wasFirstMove);
            record.prevEnPassantTarget = prevEpTarget;
            record.movedPieceWasUnmoved = movedWasUnmoved;

            // En passant: hiển thị quân bị bắt + animation chết
            if (isEnPassantMove && epVictim != null) {
                record.isEnPassant = true;
                record.epVictimX = to.x;
                record.epVictimY = from.y;
                kill(epVictim);
                boardView.addCapturedPiece(epVictim);
            }

            // Nhập thành: di chuyển xe tương ứng theo vua
            if (isCastleMove) {
                int row = from.y;
                int rookFromX = (to.x == 6) ? 7 : 0; // king-side vs queen-side
                int rookToX   = (to.x == 6) ? 5 : 3;
                Chessman rook = chessmen[rookFromX][row];
                if (rook != null) {
                    chessmen[rookToX][row] = rook;
                    chessmen[rookFromX][row] = null;
                    rook.setPoint(new Point(rookToX, row));
                    rook.moveButton(rookToX, row);
                    if (rook instanceof Rook) ((Rook) rook).hasMoved = true;
                    record.isCastle = true;
                    record.rookFromX = rookFromX; record.rookFromY = row;
                    record.rookToX = rookToX;   record.rookToY = row;
                    record.castledRook = rook;
                }
            }

            // Cập nhật cờ hasMoved của vua/xe
            if (movedPiece instanceof King) ((King) movedPiece).hasMoved = true;
            if (movedPiece instanceof Rook) ((Rook) movedPiece).hasMoved = true;

            // Cập nhật en passant target cho nước kế tiếp (tốt vừa đi 2 ô)
            if (movedPiece.type == Chessman.ChessmanType.Pawn && Math.abs(to.y - from.y) == 2) {
                enPassantTarget = new Point(from.x, (from.y + to.y) / 2);
            } else {
                enPassantTarget = null;
            }

            moveHistory.push(record);

            // Update undo button visibility
            boardView.updateUndoButton(true);

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

            // --- Luật hoà: cập nhật đồng hồ 50 nước + đếm lặp thế (tính SAU promotion) ---
            record.prevHalfMoveClock = halfMoveClock;
            boolean resetClock = (movedPiece.type == Chessman.ChessmanType.Pawn)
                    || capturedPiece != null || isEnPassantMove;
            halfMoveClock = resetClock ? 0 : halfMoveClock + 1;
            String posKey = positionKey();
            record.resultingPositionKey = posKey;
            positionCounts.merge(posKey, 1, Integer::sum);

            // Check opponent's king status after move
            ChessLog.d("doMove: checking opponent king status...");
            checkOpponentKingStatus();

            changeTurn();
            lastManPoint = null;
        } else {
            ChessLog.e("doMove: move failed!");
            // Nước không hợp lệ → trả lại con tốt đã gỡ tạm cho en passant
            if (isEnPassantMove && epVictim != null) {
                chessmen[to.x][from.y] = epVictim;
            }
        }
    }

    public boolean move(int xf, int yf, int xt, int yt) {
        Chessman tempMan = chessmen[xt][yt];
        chessmen[xt][yt] = chessmen[xf][yf];
        chessmen[xt][yt].setPoint(new Point(xt, yt));
        chessmen[xf][yf] = null;
        
        ChessLog.d(chessmen[xt][yt].color + " Moved: " + chessmen[xt][yt].type.name() + " from [" + xf + "," + yf + "] to [" + xt + "," + yt + "]");

        King myKing = (chessmen[xt][yt].color == Chessman.PlayerColor.White) ? whiteKing : blackKing;

        // Only check if our king is safe (not in check)
        // We don't care about Stalemate here because the turn will pass to the opponent
        boolean isKingSafe = myKing.isPointSafe();
        ChessLog.d("move: " + myKing.color + " king safe=" + isKingSafe);

        if (isKingSafe) {
            if (tempMan != null) {
                ChessLog.d(myKing.color + " Captured: " + tempMan.type.name() + " at [" + xt + "," + yt + "]");
                kill(tempMan);
                // Add captured piece to display
                boardView.addCapturedPiece(tempMan);
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
        Context cIllegal = getCtx();
        if (cIllegal != null) Toast.makeText(cIllegal, R.string.illegal_move, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Check opponent's king status after a move
     */
    private void checkOpponentKingStatus() {
        King opponentKing = (whichPlayerTurn == Chessman.PlayerColor.White) ? blackKing : whiteKing;
        ChessLog.d("checkOpponentKingStatus: checking " + opponentKing.color + " king");

        King.KingRiskType status = kingSafety.validate(opponentKing);
        ChessLog.d("checkOpponentKingStatus: status = " + status);

        // Clear any previous check animation
        clearCheckAnimation();

        // Hoà theo luật (50 nước / lặp thế 3 lần) — chỉ khi KHÔNG phải chiếu hết/hết cờ.
        if (status != King.KingRiskType.CheckMate && status != King.KingRiskType.Stalemate
                && isDrawByRule()) {
            ChessLog.d("checkOpponentKingStatus: DRAW (50-move / threefold)");
            gameEnd = true;
            boardView.showCustomGameEndDialog(false, true); // hoà (dùng chung dialog stalemate)
            if (isVsComputer && statsManager != null) {
                long duration = System.currentTimeMillis() - gameStartTime;
                statsManager.saveGameResult(difficultyLevel, 0, duration);
            }
            return;
        }

        if (status == King.KingRiskType.CheckMate) {
            ChessLog.d("CHECKMATE! " + whichPlayerTurn + " Wins! Game Over.");
            gameEnd = true;
            playCheckSound();
            showCheckAnimation(opponentKing);
            // Show game end dialog - current player wins
            boolean whiteWins = whichPlayerTurn == Chessman.PlayerColor.White;
            boardView.showCustomGameEndDialog(
                    whiteWins, false);

            if (isVsComputer) {
                // If White wins (Player), result = 1. If Black wins (AI), result = -1
                int result = whiteWins ? 1 : -1;
                long duration = System.currentTimeMillis() - gameStartTime;
                statsManager.saveGameResult(difficultyLevel, result, duration);
            }
        } else if (status == King.KingRiskType.Stalemate) {
            ChessLog.d("checkOpponentKingStatus: STALEMATE detected!");
            gameEnd = true;
            // Show stalemate dialog - draw (isStalemate = true)
            // Show stalemate dialog - draw (isStalemate = true)
            boardView.showCustomGameEndDialog(false, true);

            if (isVsComputer) {
                long duration = System.currentTimeMillis() - gameStartTime;
                statsManager.saveGameResult(difficultyLevel, 0, duration);
            }
        } else if (status == King.KingRiskType.Check) {
            ChessLog.d("checkOpponentKingStatus: CHECK detected!");
            playCheckSound();
            showCheckAnimation(opponentKing); // ChessAnimator tự lưu kingInCheck để reset sau
            Context cCheck = getCtx();
            if (cCheck != null) Toast.makeText(cCheck, R.string.check, Toast.LENGTH_SHORT).show();
        } else {
            ChessLog.d("checkOpponentKingStatus: SAFE - game continues");
        }
    }

    /** Flash khi vua bị chiếu — delegate sang ChessAnimator. */
    private void showCheckAnimation(King king) {
        animator.showCheck(getCtx(), king);
    }

    /** Xoá flash chiếu — delegate sang ChessAnimator. */
    private void clearCheckAnimation() {
        animator.clearCheck();
    }

    /**
     * Play check warning sound
     */
    private void playCheckSound()       { if (audio != null) audio.playCheck(); }
    private void playIllegalMoveSound() { if (audio != null) audio.playIllegal(); }
    public  void playMoveSound(boolean isWhite) { if (audio != null) audio.playMove(isWhite); }

    /** True nếu ván hoà theo luật: 50 nước không ăn quân/đẩy tốt, HOẶC lặp thế 3 lần. */
    private boolean isDrawByRule() {
        if (halfMoveClock >= 100) return true;             // luật 50 nước (100 nửa-nước)
        Integer c = positionCounts.get(positionKey());
        return c != null && c >= 3;                        // lặp thế (threefold)
    }

    /**
     * Khoá thế cờ để phát hiện lặp thế: bố cục 64 ô + bên đi + ô en passant + quyền nhập
     * thành (qua hasMoved của 2 vua). Hai thế "giống nhau" ⇔ cùng khoá.
     */
    private String positionKey() {
        StringBuilder sb = new StringBuilder(72);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Chessman m = chessmen[x][y];
                if (m == null || m.isDead) { sb.append('.'); continue; }
                char c;
                switch (m.type) {
                    case King:   c = 'k'; break;
                    case Queen:  c = 'q'; break;
                    case Rook:   c = 'r'; break;
                    case Bishop: c = 'b'; break;
                    case Knight: c = 'n'; break;
                    default:     c = 'p';
                }
                sb.append(m.color == Chessman.PlayerColor.White ? Character.toUpperCase(c) : c);
            }
        }
        sb.append(whichPlayerTurn == Chessman.PlayerColor.White ? 'w' : 'b');
        sb.append(enPassantTarget == null ? "-" : ("" + enPassantTarget.x + enPassantTarget.y));
        sb.append(whiteKing != null && !whiteKing.hasMoved ? 'K' : '-');
        sb.append(blackKing != null && !blackKing.hasMoved ? 'k' : '-');
        return sb.toString();
    }

    // [Refactor] validateKing + 6 helper mô phỏng an toàn vua đã chuyển sang KingSafetyEvaluator.

    public void kill(Chessman m) {
        deadMen.add(m);
        m.isDead = true;

        // Model-only mode (unit test): không có button/ctx → bỏ qua haptic + animation
        if (m.button == null) return;

        // Haptic feedback for capture
        performCaptureHaptic();

        // Death animation: fade-out only (no scale overflow — button still in boardLayout until removed)
        m.button.animate()
                .alpha(0f)
                .rotation(180f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(350)
                .setInterpolator(new AccelerateInterpolator(1.5f))
                .withEndAction(() -> {
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
        boardView.animateTurnChange(whichPlayerTurn);

        if (isVsComputer && whichPlayerTurn == Chessman.PlayerColor.Black && !gameEnd) {
            triggerAITurn();
        }
    }

    private void triggerAITurn() {
        isAiThinking = true;

        new Thread(() -> {
            try {
                long delay = aiEngine.getThinkDelay(difficultyLevel);
                Thread.sleep(delay);

                // [BUG-01] Check lifecycle before heavy computation
                if (isDestroyed) {
                    isAiThinking = false;
                    return;
                }

                MoveRecord bestMove = aiEngine.getBestMove(this, difficultyLevel, Chessman.PlayerColor.Black);

                aiHandler.post(() -> {
                    // [BUG-01] Lifecycle guard: refuse to touch UI if destroyed
                    if (isDestroyed || gameEnd || !isAiThinking) {
                        isAiThinking = false;
                        return;
                    }

                    if (bestMove != null) {
                        Point from = new Point(bestMove.fromX, bestMove.fromY);
                        Point to = new Point(bestMove.toX, bestMove.toY);
                        doMove(from, to);
                    } else {
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

    /**
     * [ML-04] Cancel pending AI handler callbacks and mark as destroyed.
     * Must be called from ChessBoardActivity.onDestroy().
     */
    public void cancelAiHandler() {
        isDestroyed = true;
        isAiThinking = false;
        if (aiHandler != null) aiHandler.removeCallbacksAndMessages(null);
        clearCheckAnimation();
        clearSelectionHighlight();
        // OPT: Release SoundPool resources
        if (audio != null) { audio.release(); audio = null; }
    }

    public void promote(Chessman man) {
        manToPromote = man;
        boardView.showPromotionActivity();
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
        // LEAK-SAFE: dùng listener (onAnimationEnd chạy cả khi HOÀN TẤT lẫn CANCEL) thay
        // withEndAction → tốt cũ luôn được gỡ; guard isDestroyed để không thêm view trên
        // Activity đã huỷ. Cờ cancelled phân biệt: cancel-thường → snap quân mới vào ngay
        // (model đã phong cấp nên view BẮT BUỘC hiện, không được để quân biến mất); kết thúc
        // tự nhiên → chạy Phase 2 entrance animation.
        final boolean[] cancelled = {false};
        oldPawn.button.animate()
                .scaleX(0.2f)
                .scaleY(0.2f)
                .rotation(720f) // Two full rotations
                .alpha(0.3f)
                .setDuration(400)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(android.animation.Animator a) {
                        cancelled[0] = true;
                    }

                    @Override
                    public void onAnimationEnd(android.animation.Animator a) {
                        // Remove old pawn (guard parent null)
                        if (oldPawn.button.getParent() != null) {
                            ((ViewGroup) oldPawn.button.getParent()).removeView(oldPawn.button);
                        }
                        if (isDestroyed || boardLayout == null) return; // không thêm view nếu đã huỷ

                        // Add new piece to board (guard chống thêm trùng nếu listener gọi 2 lần)
                        if (newPiece.button.getParent() == null) {
                            boardLayout.addView(newPiece.button);
                        }
                        playTransformationSound();

                        if (cancelled[0]) {
                            // Cancel-thường: bỏ qua entrance, snap thẳng về trạng thái hiển thị cuối
                            newPiece.resetButtonState();
                            return;
                        }

                        // Phase 2: New piece appears with dramatic entrance
                        newPiece.button.setScaleX(0.1f);
                        newPiece.button.setScaleY(0.1f);
                        newPiece.button.setAlpha(0f);
                        newPiece.button.setRotation(-180f);

                        // Dramatic entrance: grow from tiny to 1.0, never exceeds 1.0
                        newPiece.button.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .rotation(0f)
                                .alpha(1.0f)
                                .setDuration(500)
                                .setInterpolator(new OvershootInterpolator(1.5f))
                                .withEndAction(() -> newPiece.resetButtonState())
                                .start();
                    }
                })
                .start();
    }

    private void playPromotionSound()      { if (audio != null) audio.playPromotion(); }
    private void playTransformationSound() { if (audio != null) audio.playTransformation(); }

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
        Context ctx = getCtx();   // UI-only path (ctx non-null); local resolve từ WeakRef
        ImageButton btn = new ImageButton(ctx);
        int width = minDimension / 8;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);

        lp.setMargins(width * p.x, width * p.y, minDimension - (width * p.x + width),
                minDimension - (width * p.y + width));

        btn.setLayoutParams(lp);
        btn.setBackground(ctx.getResources().getDrawable(R.drawable.bg_valid_move_game, ctx.getTheme()));

        btn.setOnClickListener(v -> {
            onBoardClick(p.x, p.y);
        });

        // Start with invisible and scaled down
        btn.setAlpha(0f);
        btn.setScaleX(0f);
        btn.setScaleY(0f);

        validMoveButtons.add(btn);
        boardLayout.addView(btn);

        // Animate pop-in: grow from 0 to 1.0 only — never exceeds bounds
        btn.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setStartDelay(index * 40L)
                .setDuration(250)
                .setInterpolator(new OvershootInterpolator(1.2f))
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
        ChessLog.d("resetValidMoveButtons() Triggered: Clearing UI move dots and selection highlights");
        // Clear selection when moves are reset
        clearSelectionHighlight();

        // [WARN-02] Guard against NPE when parent is null (e.g. after rotation)
        for (View v : validMoveButtons) {
            if (v.getParent() != null) {
                ((ViewGroup) v.getParent()).removeView(v);
            }
        }
        validMoveButtons.clear();
    }

    /** Pulse quân được chọn — delegate sang ChessAnimator. */
    private void startSelectionPulse(Chessman man) {
        animator.startSelection(getCtx(), man);
    }

    /** Xoá highlight quân được chọn — delegate sang ChessAnimator. */
    private void clearSelectionHighlight() {
        ChessLog.d("clearSelectionHighlight() Triggered");
        animator.clearSelection();
    }

    /**
     * Perform haptic feedback for tactile response
     */
    // Haptic tách sang ChessHaptics — Chess chỉ delegate
    private void performHapticFeedback(View view, int feedbackType) {
        ChessHaptics.selection(view);
    }

    public void performCaptureHaptic() {
        ChessHaptics.capture(getCtx());
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
            Context cUndo = getCtx();
            if (cUndo != null) Toast.makeText(cUndo, R.string.no_undo_available, Toast.LENGTH_SHORT).show();
            return;
        }

        MoveRecord lastMove = moveHistory.pop();
        ChessLog.d("Undo Triggered: " + lastMove.movedPiece.type.name() + " back to [" + lastMove.fromX + "," + lastMove.fromY + "]");

        // Clear any selection
        resetValidMoveButtons();
        clearCheckAnimation();

        // [BUG-04] Detect if this was a promotion move:
        // After promotion, chessmen[toX][toY] is the promoted piece (Queen/Rook/etc.),
        // while lastMove.movedPiece is the original Pawn object.
        Chessman currentPieceAtDest = chessmen[lastMove.toX][lastMove.toY];
        boolean wasPromotion = (currentPieceAtDest != null)
                && (currentPieceAtDest != lastMove.movedPiece)
                && (lastMove.movedPiece instanceof Pawn);

        Chessman movedPiece;
        if (wasPromotion) {
            // Remove promoted piece's button from the board
            if (currentPieceAtDest.button != null && currentPieceAtDest.button.getParent() != null) {
                ((ViewGroup) currentPieceAtDest.button.getParent()).removeView(currentPieceAtDest.button);
            }
            // Restore original Pawn
            movedPiece = lastMove.movedPiece;
            movedPiece.type = Chessman.ChessmanType.Pawn;
            movedPiece.isDead = false;
            movedPiece.createButton();
            boardLayout.addView(movedPiece.button);
        } else {
            movedPiece = currentPieceAtDest;
        }

        if (movedPiece == null) return; // Safety guard

        // Move piece back to original position
        chessmen[lastMove.fromX][lastMove.fromY] = movedPiece;
        chessmen[lastMove.toX][lastMove.toY] = null;
        movedPiece.setPoint(new Point(lastMove.fromX, lastMove.fromY));

        // Animate piece back
        movedPiece.moveButton(lastMove.fromX, lastMove.fromY);

        // Restore first move status for pawns
        if (lastMove.wasFirstMove && movedPiece instanceof Pawn) {
            ((Pawn) movedPiece).firstMove = true;
        }

        // Khôi phục cờ hasMoved của vua/xe nếu nước này là lần đi đầu của chúng
        if (lastMove.movedPieceWasUnmoved) {
            if (movedPiece instanceof King) ((King) movedPiece).hasMoved = false;
            if (movedPiece instanceof Rook) ((Rook) movedPiece).hasMoved = false;
        }

        // Hoàn tác nhập thành: đưa xe về ô gốc
        if (lastMove.isCastle && lastMove.castledRook != null) {
            Chessman rook = lastMove.castledRook;
            chessmen[lastMove.rookFromX][lastMove.rookFromY] = rook;
            chessmen[lastMove.rookToX][lastMove.rookToY] = null;
            rook.setPoint(new Point(lastMove.rookFromX, lastMove.rookFromY));
            rook.moveButton(lastMove.rookFromX, lastMove.rookFromY);
            if (rook instanceof Rook) ((Rook) rook).hasMoved = false;
        }

        // Khôi phục trạng thái en passant TRƯỚC nước đi này
        enPassantTarget = lastMove.prevEnPassantTarget;

        // Khôi phục luật hoà: trả đồng hồ 50 nước + gỡ 1 lần đếm thế của nước này
        halfMoveClock = lastMove.prevHalfMoveClock;
        if (lastMove.resultingPositionKey != null) {
            Integer c = positionCounts.get(lastMove.resultingPositionKey);
            if (c != null) {
                if (c <= 1) positionCounts.remove(lastMove.resultingPositionKey);
                else positionCounts.put(lastMove.resultingPositionKey, c - 1);
            }
        }

        // Restore captured piece if any
        if (lastMove.capturedPiece != null) {
            Chessman restored = lastMove.capturedPiece;
            // En passant: con tốt bị bắt nằm ở (epVictimX, epVictimY), không phải ô đích
            int rx = lastMove.isEnPassant ? lastMove.epVictimX : lastMove.toX;
            int ry = lastMove.isEnPassant ? lastMove.epVictimY : lastMove.toY;
            chessmen[rx][ry] = restored;
            restored.setPoint(new Point(rx, ry));
            restored.isDead = false;
            deadMen.remove(restored);

            // Recreate button and animate back in (model-only mode bỏ qua khi ctx null)
            if (getCtx() != null) {
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
            }

            // Remove from captured pieces display
            boardView.removeCapturedPiece(restored);
        }

        // Switch turn back
        if (whichPlayerTurn == Chessman.PlayerColor.White)
            whichPlayerTurn = Chessman.PlayerColor.Black;
        else
            whichPlayerTurn = Chessman.PlayerColor.White;

        boardView.animateTurnChange(whichPlayerTurn);

        // Reset game end state if was checkmate
        gameEnd = false;

        // Update undo button visibility
        boardView.updateUndoButton(!moveHistory.isEmpty());

        // Play undo sound
        playIllegalMoveSound();
    }

    /**
     * Reset game to initial state
     */
    public void resetGame() {
        ChessLog.d("=== GAME START / RESET ===");
        // Clear history
        moveHistory.clear();
        deadMen.clear();
        gameEnd = false;
        gameStartTime = System.currentTimeMillis();
        // Reset trạng thái en passant (vua/xe mới tạo lại nên hasMoved=false sẵn)
        enPassantTarget = null;
        // Reset luật hoà
        halfMoveClock = 0;
        positionCounts.clear();

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
        boardView.updateUndoButton(false);
        boardView.clearCapturedPieces();
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