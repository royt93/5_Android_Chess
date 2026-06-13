package com.saigonphantomlabs.chess;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lưu & tiếp tục ván dở — ĐA SLOT (thư viện ván đã lưu). Dùng cách <b>snapshot</b> (ảnh chụp thế
 * cờ kiểu FEN) thay vì replay; KHÔNG reflection (an toàn R8 full-mode).
 *
 * <p>Mỗi ván = 1 file text trong {@code filesDir/saves/<sessionId>.sav} (scale tốt tới {@link #MAX_SLOTS},
 * đọc lazy, không nạp hết vào RAM như SharedPreferences). Auto-save theo <b>session</b>: ván giữ 1
 * {@code sessionId} từ lúc bắt đầu → ghi đè đúng slot khi chơi tiếp, xoá khi kết thúc. Vượt
 * {@link #MAX_SLOTS} → xoá ván cũ nhất (LRU theo {@code savedAtMs}).
 *
 * <p>{@link #serialize}/{@link #deserialize} thuần — test trực tiếp JVM. v1 giới hạn: không lưu
 * undo-stack & đếm lặp thế (threefold).
 */
public final class GameSaveManager {

    public static final int MAX_SLOTS = 100; // trần an toàn; LRU xoá cũ nhất khi vượt
    private static final String HEADER = "CHESSSAVE1";
    private static final String DIR = "saves";
    private static final String EXT = ".sav";

    private GameSaveManager() {}

    /** 1 quân trong snapshot (x,y chỉ dùng cho quân trên bàn; quân bị ăn để 0,0). */
    public static final class PieceData {
        public final int x, y;
        public final Chessman.ChessmanType type;
        public final Chessman.PlayerColor color;
        public final boolean moved;
        public PieceData(int x, int y, Chessman.ChessmanType type, Chessman.PlayerColor color, boolean moved) {
            this.x = x; this.y = y; this.type = type; this.color = color; this.moved = moved;
        }
    }

    /** Trạng thái 1 ván đã lưu (board + meta + đồng hồ). */
    public static final class SavedGame {
        public List<PieceData> pieces = new ArrayList<>();
        public List<PieceData> captured = new ArrayList<>();
        public Chessman.PlayerColor turn = Chessman.PlayerColor.White;
        public Point enPassant = null;
        public int halfMoveClock = 0;
        public boolean isVsAi = false;
        public String difficulty = null;
        // Đồng hồ cờ
        public boolean hasClock = false;
        public long whiteMs = 0, blackMs = 0, incrementMs = 0;
        public boolean whiteActive = true;
        // Slot meta
        public String sessionId = null;   // id ván (tên file)
        public long savedAtMs = 0;        // thời điểm lưu (sort + LRU)
        public int moveCount = 0;         // tổng số ply (tích luỹ qua resume) — để hiển thị
    }

    // ───────────────────────── Mã hoá / giải mã (thuần) ─────────────────────────

    public static String serialize(SavedGame g) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        sb.append("I|").append(g.sessionId == null ? "-" : g.sessionId)
                .append('|').append(g.savedAtMs).append('|').append(g.moveCount).append('\n');
        sb.append("M|").append(g.turn == Chessman.PlayerColor.White ? 'W' : 'B')
                .append('|').append(g.halfMoveClock)
                .append('|').append(g.isVsAi ? '1' : '0')
                .append('|').append(g.difficulty == null ? "-" : g.difficulty)
                .append('|').append(g.enPassant == null ? "-" : (g.enPassant.x + "," + g.enPassant.y))
                .append('\n');
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
                    case "I":
                        g.sessionId = "-".equals(f[1]) ? null : f[1];
                        g.savedAtMs = Long.parseLong(f[2]);
                        g.moveCount = Integer.parseInt(f[3]);
                        break;
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
            return null;
        }
    }

    // ───────────────────────── Slot store (file-per-slot) ─────────────────────────

    /** Lưu/ghi đè slot theo sessionId; vượt MAX_SLOTS → xoá ván cũ nhất (LRU theo savedAtMs). */
    public static void saveSlot(Context ctx, SavedGame g) {
        if (g == null || g.sessionId == null) return;
        File dir = dir(ctx);
        if (!dir.exists() && !dir.mkdirs()) return;
        writeFile(new File(dir, g.sessionId + EXT), serialize(g));
        enforceCap(ctx);
    }

    /** Danh sách ván đã lưu, sort mới→cũ theo savedAtMs (bỏ qua file hỏng). */
    public static List<SavedGame> listSlots(Context ctx) {
        List<SavedGame> out = new ArrayList<>();
        File dir = dir(ctx);
        File[] files = dir.listFiles((d, name) -> name.endsWith(EXT));
        if (files == null) return out;
        for (File f : files) {
            SavedGame g = deserialize(readFile(f));
            if (g != null) {
                if (g.sessionId == null) g.sessionId = stripExt(f.getName());
                out.add(g);
            }
        }
        Collections.sort(out, new Comparator<SavedGame>() {
            @Override public int compare(SavedGame a, SavedGame b) {
                return Long.compare(b.savedAtMs, a.savedAtMs);
            }
        });
        return out;
    }

    public static SavedGame loadSlot(Context ctx, String sessionId) {
        if (sessionId == null) return null;
        return deserialize(readFile(new File(dir(ctx), sessionId + EXT)));
    }

    public static void deleteSlot(Context ctx, String sessionId) {
        if (sessionId == null) return;
        File f = new File(dir(ctx), sessionId + EXT);
        if (f.exists()) f.delete();
    }

    public static int slotCount(Context ctx) {
        File[] files = dir(ctx).listFiles((d, name) -> name.endsWith(EXT));
        return files == null ? 0 : files.length;
    }

    private static void enforceCap(Context ctx) {
        List<SavedGame> slots = listSlots(ctx); // đã sort mới→cũ
        for (int i = MAX_SLOTS; i < slots.size(); i++) {
            deleteSlot(ctx, slots.get(i).sessionId); // xoá phần đuôi (cũ nhất)
        }
    }

    private static File dir(Context ctx) {
        return new File(ctx.getFilesDir(), DIR);
    }

    private static String stripExt(String name) {
        return name.endsWith(EXT) ? name.substring(0, name.length() - EXT.length()) : name;
    }

    private static void writeFile(File f, String content) {
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

    private static String readFile(File f) {
        if (f == null || !f.exists()) return null;
        try (FileInputStream is = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = is.read(buf);
            return n <= 0 ? "" : new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
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
