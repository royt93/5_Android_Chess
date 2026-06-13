package com.saigonphantomlabs.chess;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu & tiếp tục ván dở. Dùng cách <b>snapshot</b> (ảnh chụp thế cờ kiểu FEN) thay vì replay:
 * mã hoá từng quân (loại + màu + đã-đi) + lượt + en passant + đồng hồ 50-nước + quân bị ăn + đồng hồ cờ
 * thành 1 chuỗi text gọn, KHÔNG reflection (an toàn R8 full-mode).
 *
 * <p>Phần mã hoá/giải mã ({@link #serialize}/{@link #deserialize}) là thuần — test trực tiếp JVM.
 * Phần đọc trạng thái từ/ghi vào {@link Chess} nằm ở {@code Chess.captureSaveState/loadSaveState}.
 *
 * <p>v1 giới hạn: KHÔNG lưu undo-stack (sau khi resume không undo về trước điểm lưu) và bộ đếm lặp
 * thế (threefold) — đếm lại từ thế hiện tại. Đủ để chơi tiếp đúng luật.
 */
public final class GameSaveManager {

    private static final String PREFS = "chess_saved_game";
    private static final String KEY = "save";
    private static final String HEADER = "CHESSSAVE1";

    private GameSaveManager() {}

    /** 1 quân trong snapshot (x,y chỉ dùng cho quân trên bàn; quân bị ăn để 0,0). */
    public static final class PieceData {
        public final int x, y;
        public final Chessman.ChessmanType type;
        public final Chessman.PlayerColor color;
        public final boolean moved; // K/R đã di chuyển, hoặc tốt không còn nước đôi (firstMove=false)
        public PieceData(int x, int y, Chessman.ChessmanType type, Chessman.PlayerColor color, boolean moved) {
            this.x = x; this.y = y; this.type = type; this.color = color; this.moved = moved;
        }
    }

    /** Trạng thái 1 ván đã lưu (board + meta + đồng hồ). */
    public static final class SavedGame {
        public List<PieceData> pieces = new ArrayList<>();
        public List<PieceData> captured = new ArrayList<>();
        public Chessman.PlayerColor turn = Chessman.PlayerColor.White;
        public Point enPassant = null;       // null nếu không có
        public int halfMoveClock = 0;
        public boolean isVsAi = false;
        public String difficulty = null;     // tên AIEngine.Difficulty, null nếu PvP
        // Đồng hồ cờ
        public boolean hasClock = false;
        public long whiteMs = 0, blackMs = 0, incrementMs = 0;
        public boolean whiteActive = true;
    }

    // ───────────────────────── Mã hoá / giải mã (thuần) ─────────────────────────

    public static String serialize(SavedGame g) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        // M|turn|half|vsai|diff|ep
        sb.append("M|").append(g.turn == Chessman.PlayerColor.White ? 'W' : 'B')
                .append('|').append(g.halfMoveClock)
                .append('|').append(g.isVsAi ? '1' : '0')
                .append('|').append(g.difficulty == null ? "-" : g.difficulty)
                .append('|').append(g.enPassant == null ? "-" : (g.enPassant.x + "," + g.enPassant.y))
                .append('\n');
        // C|hasClock|white|black|inc|active
        sb.append("C|").append(g.hasClock ? '1' : '0')
                .append('|').append(g.whiteMs).append('|').append(g.blackMs)
                .append('|').append(g.incrementMs).append('|').append(g.whiteActive ? '1' : '0')
                .append('\n');
        for (PieceData p : g.pieces) {
            sb.append("P|").append(p.x).append('|').append(p.y).append('|')
                    .append(typeLetter(p.type)).append('|').append(colorLetter(p.color))
                    .append('|').append(p.moved ? '1' : '0').append('\n');
        }
        for (PieceData p : g.captured) {
            sb.append("D|").append(typeLetter(p.type)).append('|').append(colorLetter(p.color)).append('\n');
        }
        return sb.toString();
    }

    /** Giải mã; trả null nếu rỗng/sai header/parse lỗi (coi như không có save). */
    public static SavedGame deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] lines = s.split("\n");
            if (lines.length == 0 || !HEADER.equals(lines[0].trim())) return null;
            SavedGame g = new SavedGame();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                String[] f = line.split("\\|");
                switch (f[0]) {
                    case "M":
                        g.turn = "W".equals(f[1]) ? Chessman.PlayerColor.White : Chessman.PlayerColor.Black;
                        g.halfMoveClock = Integer.parseInt(f[2]);
                        g.isVsAi = "1".equals(f[3]);
                        g.difficulty = "-".equals(f[4]) ? null : f[4];
                        if (!"-".equals(f[5])) {
                            String[] ep = f[5].split(",");
                            g.enPassant = new Point(Integer.parseInt(ep[0]), Integer.parseInt(ep[1]));
                        }
                        break;
                    case "C":
                        g.hasClock = "1".equals(f[1]);
                        g.whiteMs = Long.parseLong(f[2]);
                        g.blackMs = Long.parseLong(f[3]);
                        g.incrementMs = Long.parseLong(f[4]);
                        g.whiteActive = "1".equals(f[5]);
                        break;
                    case "P":
                        g.pieces.add(new PieceData(Integer.parseInt(f[1]), Integer.parseInt(f[2]),
                                type(f[3]), color(f[4]), "1".equals(f[5])));
                        break;
                    case "D":
                        g.captured.add(new PieceData(0, 0, type(f[1]), color(f[2]), false));
                        break;
                    default:
                        break;
                }
            }
            return g;
        } catch (RuntimeException ex) {
            return null; // dữ liệu hỏng → bỏ qua save
        }
    }

    // ───────────────────────── SharedPreferences ─────────────────────────

    public static void save(Context ctx, SavedGame g) {
        prefs(ctx).edit().putString(KEY, serialize(g)).apply();
    }

    public static SavedGame load(Context ctx) {
        return deserialize(prefs(ctx).getString(KEY, null));
    }

    public static boolean hasSave(Context ctx) {
        return load(ctx) != null;
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().remove(KEY).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ───────────────────────── Map type/color ↔ chữ ─────────────────────────

    private static char typeLetter(Chessman.ChessmanType t) {
        switch (t) {
            case King: return 'K';
            case Queen: return 'Q';
            case Rook: return 'R';
            case Bishop: return 'B';
            case Knight: return 'N';
            case Pawn: default: return 'P';
        }
    }

    private static Chessman.ChessmanType type(String s) {
        switch (s) {
            case "K": return Chessman.ChessmanType.King;
            case "Q": return Chessman.ChessmanType.Queen;
            case "R": return Chessman.ChessmanType.Rook;
            case "B": return Chessman.ChessmanType.Bishop;
            case "N": return Chessman.ChessmanType.Knight;
            case "P": default: return Chessman.ChessmanType.Pawn;
        }
    }

    private static char colorLetter(Chessman.PlayerColor c) {
        return c == Chessman.PlayerColor.White ? 'W' : 'B';
    }

    private static Chessman.PlayerColor color(String s) {
        return "W".equals(s) ? Chessman.PlayerColor.White : Chessman.PlayerColor.Black;
    }
}
