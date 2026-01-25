package com.saigonphantomlabs.chess;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AIEngine {

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD,
        UNBEATABLE
    }

    private static final String TAG = "AIEngine";
    private final Random random = new Random();

    // Piece values for evaluation
    private static final int VALUE_PAWN = 100;
    private static final int VALUE_KNIGHT = 320;
    private static final int VALUE_BISHOP = 330;
    private static final int VALUE_ROOK = 500;
    private static final int VALUE_QUEEN = 900;
    private static final int VALUE_KING = 20000;

    // --- Piece Square Tables (Mirror for Black if needed, but we can index
    // properly) ---
    // Defined for White side (bottom). For Black, we mirror the Rank index.

    private static final int[] PST_PAWN = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] PST_KNIGHT = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    private static final int[] PST_BISHOP = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    // Rooks prefer open files and 7th rank
    private static final int[] PST_ROOK = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0
    };

    private static final int[] PST_QUEEN = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
    };

    // King safety table (Middle game)
    private static final int[] PST_KING_MID = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    public MoveRecord getBestMove(Chess board, Difficulty difficulty, Chessman.PlayerColor aiColor) {
        Log.d(TAG, "getBestMove: Difficulty=" + difficulty + ", AI Color=" + aiColor);
        long startTime = System.currentTimeMillis();

        // 1. OPENING BOOK CHECK
        if (difficulty == Difficulty.HARD || difficulty == Difficulty.UNBEATABLE) {
            MoveRecord bookMove = getOpeningBookMove(board, aiColor);
            if (bookMove != null) {
                Log.d(TAG, "getBestMove: Found book move " + bookMove.getNotation(board.ctx));
                return bookMove;
            }
        }

        List<MoveRecord> allMoves = generateAllLegalMoves(board, aiColor);
        if (allMoves.isEmpty()) {
            return null; // No moves available (Checkmate or Stalemate)
        }

        MoveRecord bestMove = null;

        switch (difficulty) {
            case EASY:
                // Random legal move
                bestMove = allMoves.get(random.nextInt(allMoves.size()));
                break;

            case MEDIUM:
                // Depth 2
                bestMove = findBestMoveMinimax(board, aiColor, 2);
                break;

            case HARD:
                // Depth 3 with positional evaluation
                bestMove = findBestMoveMinimax(board, aiColor, 3);
                break;

            case UNBEATABLE:
                // Depth 4 with Alpha-Beta Pruning
                bestMove = findBestMoveMinimaxAlphaBeta(board, aiColor, 4);
                break;
        }

        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "getBestMove took " + duration + "ms. Selected move: " + bestMove);
        return bestMove;
    }

    public long getThinkDelay(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 500;
            case MEDIUM:
                return 800;
            case HARD:
                return 1200;
            case UNBEATABLE:
                return 1500;
            default:
                return 1000;
        }
    }

    // --- Move Generation Helper ---

    private List<MoveRecord> generateAllLegalMoves(Chess board, Chessman.PlayerColor color) {
        List<MoveRecord> moves = new ArrayList<>();
        Chessman[][] chessmen = board.chessmen;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Chessman piece = chessmen[x][y];
                if (piece != null && !piece.isDead && piece.color == color) {
                    piece.generateMoves();
                    for (Point target : piece.moves) {
                        // Check if move is legal (doesn't expose King to check)
                        // We need a way to simulate this without permanently modifying the board
                        // For now, let's assume validMoves generated by Chessman are mostly correct,
                        // but we MUST strictly check king safety.

                        // NOTE: Chess.java's move() method does this check.
                        // We need a simulation method that doesn't trigger UI updates or sounds.
                        if (isMoveLegalSimulation(board, piece, target)) {
                            // Create a record. Note: capturedPiece might be null.
                            Chessman captured = chessmen[target.x][target.y];
                            moves.add(new MoveRecord(x, y, target.x, target.y, piece, captured, null, false));
                        }
                    }
                }
            }
        }
        return moves;
    }

    private boolean isMoveLegalSimulation(Chess board, Chessman piece, Point target) {
        Point originalPos = piece.getPoint();
        Chessman capturedPiece = board.chessmen[target.x][target.y];

        // Temporarily apply move
        board.chessmen[target.x][target.y] = piece;
        board.chessmen[originalPos.x][originalPos.y] = null;
        piece.setPoint(target);

        King myKing = (piece.color == Chessman.PlayerColor.White) ? board.whiteKing : board.blackKing;
        boolean isSafe = myKing.isPointSafe();

        // Revert move
        board.chessmen[originalPos.x][originalPos.y] = piece;
        board.chessmen[target.x][target.y] = capturedPiece;
        piece.setPoint(originalPos);

        return isSafe;
    }

    // --- Minimax Logic ---

    // Simple Minimax for Medium/Hard
    private MoveRecord findBestMoveMinimax(Chess board, Chessman.PlayerColor aiColor, int depth) {
        int bestScore = Integer.MIN_VALUE;
        MoveRecord bestMove = null;
        List<MoveRecord> legalMoves = generateAllLegalMoves(board, aiColor);
        Collections.shuffle(legalMoves); // Add randomness primarily for equal-value moves

        for (MoveRecord move : legalMoves) {
            // Apply move simulation
            simulateMove(board, move);

            // Call minimax for opponent
            int score = minimax(board, depth - 1, false, aiColor);

            // Undo move simulation
            undoSimulateMove(board, move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    // Minimax with Alpha-Beta for Unbeatable
    private MoveRecord findBestMoveMinimaxAlphaBeta(Chess board, Chessman.PlayerColor aiColor, int depth) {
        int bestScore = Integer.MIN_VALUE;
        MoveRecord bestMove = null;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        List<MoveRecord> legalMoves = generateAllLegalMoves(board, aiColor);
        // Simple move ordering: captures first could improve pruning efficiency
        // For now, simpler implementation.
        Collections.shuffle(legalMoves);

        for (MoveRecord move : legalMoves) {
            simulateMove(board, move);
            int score = minimaxAlphaBeta(board, depth - 1, false, aiColor, alpha, beta);
            undoSimulateMove(board, move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);
        }
        return bestMove;
    }

    private int minimax(Chess board, int depth, boolean isMaximizing, Chessman.PlayerColor aiColor) {
        if (depth == 0) {
            return evaluateBoard(board, aiColor);
        }

        Chessman.PlayerColor currentPlayer = isMaximizing ? aiColor
                : (aiColor == Chessman.PlayerColor.White ? Chessman.PlayerColor.Black : Chessman.PlayerColor.White);
        List<MoveRecord> legalMoves = generateAllLegalMoves(board, currentPlayer);

        if (legalMoves.isEmpty()) {
            // Check for mate or stalemate
            // Evaluating relative to AI: if AI is maximizing and has no moves -> Loss if
            // checkmate
            // This simulation is simplified; usually we check isKingSafe.
            King k = (currentPlayer == Chessman.PlayerColor.White) ? board.whiteKing : board.blackKing;
            if (!k.isPointSafe()) {
                return isMaximizing ? -100000 : 100000;
            }
            return 0; // Stalemate
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (MoveRecord move : legalMoves) {
                simulateMove(board, move);
                int eval = minimax(board, depth - 1, false, aiColor);
                undoSimulateMove(board, move);
                maxEval = Math.max(maxEval, eval);
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (MoveRecord move : legalMoves) {
                simulateMove(board, move);
                int eval = minimax(board, depth - 1, true, aiColor);
                undoSimulateMove(board, move);
                minEval = Math.min(minEval, eval);
            }
            return minEval;
        }
    }

    private int minimaxAlphaBeta(Chess board, int depth, boolean isMaximizing, Chessman.PlayerColor aiColor, int alpha,
            int beta) {
        if (depth == 0) {
            return evaluateBoard(board, aiColor);
        }

        Chessman.PlayerColor currentPlayer = isMaximizing ? aiColor
                : (aiColor == Chessman.PlayerColor.White ? Chessman.PlayerColor.Black : Chessman.PlayerColor.White);
        List<MoveRecord> legalMoves = generateAllLegalMoves(board, currentPlayer);

        if (legalMoves.isEmpty()) {
            King k = (currentPlayer == Chessman.PlayerColor.White) ? board.whiteKing : board.blackKing;
            if (!k.isPointSafe()) {
                return isMaximizing ? -100000 : 100000;
            }
            return 0;
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (MoveRecord move : legalMoves) {
                simulateMove(board, move);
                int eval = minimaxAlphaBeta(board, depth - 1, false, aiColor, alpha, beta);
                undoSimulateMove(board, move);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha)
                    break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (MoveRecord move : legalMoves) {
                simulateMove(board, move);
                int eval = minimaxAlphaBeta(board, depth - 1, true, aiColor, alpha, beta);
                undoSimulateMove(board, move);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha)
                    break;
            }
            return minEval;
        }
    }

    // --- Evaluation Function ---
    // Positive value favors AI, Negative favors Opponent
    private int evaluateBoard(Chess board, Chessman.PlayerColor aiColor) {
        int score = 0;

        // Count material and Positional Score
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Chessman piece = board.chessmen[x][y];
                if (piece != null && !piece.isDead) {
                    int value = getPieceValue(piece);
                    int pst = getPSTValue(piece, x, y);

                    if (piece.color == aiColor) {
                        score += (value + pst);
                    } else {
                        score -= (value + pst);
                    }
                }
            }
        }
        return score;
    }

    private int getPSTValue(Chessman piece, int x, int y) {
        // Map (x,y) to 1D index 0-63.
        // Array is laid out row 0 to 7.
        // In array, index = row * 8 + col.
        // board y=0 is row 0. x=0 is col 0.
        // IF Piece is WHITE (Bottom), it matches the table orientation (Bottom is rank
        // 7/8).
        // Wait, standard PSTs usually assume Rank 1 is bottom (index 56-63) or top
        // (index 0-7)?
        // My PSTs above are defined from Rank 8 (Top/Black Start) to Rank 1
        // (Bottom/White Start).
        // Let's assume standard layout:
        // Rank 8 (y=0 in logic? No wait.)
        // In Chess.java:
        // y=0 is TOP (Black Start).
        // y=7 is BOTTOM (White Start).

        // My PST Tables logic:
        // Index 0-7: Rank 8 (Top) - Promotion rank for White, Start for Black.
        // Index 56-63: Rank 1 (Bottom) - Start for White.

        // If Piece is WHITE:
        // It starts at y=7. We want y=7 to map to PST "Safe/Start" rows.
        // If PST is designed for White View perspective where bottom is safe:
        // My table PST_PAWN: row 6 (index 48-55) has values 5, 10... (Rank 2 - Start
        // pawns)
        // row 1 (index 8-15) has values 50 (Rank 7 - Promotion imminent)
        // So my table is oriented such that Top of Array (Index 0) is "Far away/Attack
        // zone".
        // In Chess.java y=0 is Top.
        // So for WHITE moving UP (y decreasing): y=0 is target.
        // PST Index = y * 8 + x. (Top row y=0 matches Top row of PST).
        // So for WHITE: PST[y*8 + x] works perfectly if table is "Good to be at Top".
        // Yes, PST_PAWN row 1 (y=1) has 50. Row 6 (y=6) has 5.
        // White starts at y=6. Moves to y=0.

        // If Piece is BLACK:
        // It starts at y=0. Moves to y=7.
        // We need to Flip the board vertically for Black.
        // Effective Y for Black = 7 - y.
        // So use PST[(7-y)*8 + x].
        // Also flip X? Usually PST is symmetric horizontally, but King table isn't
        // always?
        // King table is symmetric. So X doesn't matter.

        int index;
        if (piece.color == Chessman.PlayerColor.White) {
            index = y * 8 + x;
        } else {
            index = (7 - y) * 8 + x;
        }

        // Safety check
        if (index < 0 || index >= 64)
            return 0;

        switch (piece.type) {
            case Pawn:
                return PST_PAWN[index];
            case Knight:
                return PST_KNIGHT[index];
            case Bishop:
                return PST_BISHOP[index];
            case Rook:
                return PST_ROOK[index];
            case Queen:
                return PST_QUEEN[index];
            case King:
                return PST_KING_MID[index];
            default:
                return 0;
        }
    }

    // --- Opening Book ---

    private MoveRecord getOpeningBookMove(Chess board, Chessman.PlayerColor aiColor) {
        // Simple hardcoded responses for early game
        // Assuming AI is BLACK
        if (aiColor == Chessman.PlayerColor.White)
            return null; // We only implemented for Black for now

        int moveCount = board.getMoveCount(); // We need a way to count moves or check board state.
        // If moveCount not available, check piece positions?
        // Let's implement a very basic state check.

        // Detect "Start Position" (approximate)
        if (isPieceAt(board, 4, 1, Chessman.ChessmanType.Pawn, Chessman.PlayerColor.Black) &&
                isPieceAt(board, 3, 1, Chessman.ChessmanType.Pawn, Chessman.PlayerColor.Black)) {

            // Response to 1. e4 (White Pawn to 4,4? No, y=7->5. White Pawn starts y=6. e4
            // is (4,4))
            if (isPieceAt(board, 4, 4, Chessman.ChessmanType.Pawn, Chessman.PlayerColor.White)) {
                // e5 (King's Pawn Game) -> Black Pawn (4,1) to (4,3)
                // Check if (4,3) is empty
                if (board.chessmen[4][3] == null) {
                    return createBookMoveRecord(board, 4, 1, 4, 3);
                }
            }

            // Response to 1. d4 (White Pawn to 3,4)
            if (isPieceAt(board, 3, 4, Chessman.ChessmanType.Pawn, Chessman.PlayerColor.White)) {
                // d5 (Queen's Pawn Game) -> Black Pawn (3,1) to (3,3)
                if (board.chessmen[3][3] == null) {
                    return createBookMoveRecord(board, 3, 1, 3, 3);
                }
            }
        }

        return null;
    }

    private boolean isPieceAt(Chess board, int x, int y, Chessman.ChessmanType type, Chessman.PlayerColor color) {
        Chessman p = board.chessmen[x][y];
        return p != null && p.type == type && p.color == color;
    }

    private MoveRecord createBookMoveRecord(Chess board, int fx, int fy, int tx, int ty) {
        Chessman piece = board.chessmen[fx][fy];
        return new MoveRecord(fx, fy, tx, ty, piece, null, null, piece.type == Chessman.ChessmanType.Pawn);
    }

    private int getPieceValue(Chessman piece) {
        switch (piece.type) {
            case Pawn:
                return VALUE_PAWN;
            case Knight:
                return VALUE_KNIGHT;
            case Bishop:
                return VALUE_BISHOP;
            case Rook:
                return VALUE_ROOK;
            case Queen:
                return VALUE_QUEEN;
            case King:
                return VALUE_KING;
            default:
                return 0;
        }
    }

    // --- Simulation Helpers ---
    // IMPORTANT: These must match board representation logic in Chess.java but
    // WITHOUT side effects (UI, Sound)
    private void simulateMove(Chess board, MoveRecord move) {
        Chessman movedPiece = move.movedPiece;
        // move.capturedPiece might be null or valid.

        board.chessmen[move.toX][move.toY] = movedPiece;
        board.chessmen[move.fromX][move.fromY] = null;
        movedPiece.setPoint(new Point(move.toX, move.toY));

        // Handle Promotion Simulation: Temporarily change type to Queen
        if (movedPiece.type == Chessman.ChessmanType.Pawn &&
                ((movedPiece.color == Chessman.PlayerColor.White && move.toY == 0) ||
                        (movedPiece.color == Chessman.PlayerColor.Black && move.toY == 7))) {
            movedPiece.type = Chessman.ChessmanType.Queen;
        }
    }

    private void undoSimulateMove(Chess board, MoveRecord move) {
        Chessman movedPiece = move.movedPiece;
        Chessman capturedPiece = move.capturedPiece; // Can be null

        // Revert Promotion: If it was a pawn originally, revert type
        // Note: This relies on the fact that 'movedPiece' is the persistent object.
        // If we promoted, we changed its type. We must revert it.
        // We know it was a pawn if:
        // 1. It is currently a Queen (what we promoted to)
        // 2. AND the MoveRecord says it originally moved from a position where it was a
        // pawn?
        // Actually best is to check if it's currently a Queen but intended as Pawn?
        // Better: Check if we are undoing a move from end row.

        // Simpler: Just check if the piece object WAS a pawn before simulation?
        // We modified the object directly. We need to know previous state.
        // Hack: Check board position. If we are undoing a move TO the last rank,
        // and the piece is now a Queen, and it WAS a pawn move...

        if (movedPiece.type == Chessman.ChessmanType.Queen &&
                ((movedPiece.color == Chessman.PlayerColor.White && move.toY == 0) ||
                        (movedPiece.color == Chessman.PlayerColor.Black && move.toY == 7))) {
            // Basic heuristic: check if it was originally a pawn move logic?
            // Since we don't store "wasPromotion" boolean in MoveRecord just for simulation
            // easily without changing constructor...
            // Let's rely on the fact that only Pawns can reach the last rank and become
            // Queens in this specific simulation function.
            // Wait, a Queen can also move to the last rank.
            // We need to know if it CHANGED.
            // Let's check the MoveRecord's movedPiece type? No, movedPiece reference is
            // mutable.
            // We need to store original type or deduce it.
            // DEDUCTION: If we are here, we know the piece matches 'movedPiece'.
            // Is there any other way?
            // Let's look at getBestMove -> generateAllLegalMoves -> MoveRecord creation.
            // At creation time, the piece type is Pawn. So we can store that or use a
            // custom field?
            // MoveRecord has 'promotedTo' but that's null in simulation until we set it,
            // but we are not using 'promotedTo' field in simulation loop, we just modify
            // object.

            // FIX: We can check if the move distance/pattern matches a Pawn move?
            // OR simpler: MoveRecord stores 'movedPiece'. Check if 'movedPiece' itself is
            // an instance of Pawn class?
            // YES! Even if we change .type field, the Java object is still instance of Pawn
            // class.
            if (movedPiece instanceof Pawn) {
                movedPiece.type = Chessman.ChessmanType.Pawn;
            }
        }

        board.chessmen[move.fromX][move.fromY] = movedPiece;
        board.chessmen[move.toX][move.toY] = capturedPiece; // Restore captured or null
        movedPiece.setPoint(new Point(move.fromX, move.fromY));
    }
}
