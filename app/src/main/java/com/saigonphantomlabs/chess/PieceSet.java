package com.saigonphantomlabs.chess;

/**
 * Bộ quân cờ (piece set) dạng LIGHTWEIGHT: KHÔNG thêm asset PNG — chỉ tint runtime màu quân qua
 * {@link PieceRenderer} (PorterDuff MULTIPLY, giữ shading + hiệu ứng 3D). CLASSIC = không tint.
 *
 * <p>tint MULTIPLY: quân Trắng (PNG sáng) đổi màu rõ theo {@code whiteTint}; quân Đen (PNG tối) giữ
 * tối với sắc nhẹ theo {@code blackTint}. {@code 0} = không tint.
 */
public enum PieceSet {
    CLASSIC("♟️", 0,            0,            R.string.pieceset_classic),
    GOLD   ("👑", 0xFFFFD24D,   0xFF8A8070,   R.string.pieceset_gold),
    NEON   ("⚡", 0xFF00E5FF,   0xFFFF4DA6,   R.string.pieceset_neon),
    ROYAL  ("💜", 0xFFCBB6FF,   0xFF7A5FC0,   R.string.pieceset_royal),
    EMBER  ("🔥", 0xFFFFB36B,   0xFFC0452A,   R.string.pieceset_ember);

    public final String emoji;
    public final int whiteTint;
    public final int blackTint;
    public final int nameRes;

    PieceSet(String emoji, int whiteTint, int blackTint, int nameRes) {
        this.emoji = emoji;
        this.whiteTint = whiteTint;
        this.blackTint = blackTint;
        this.nameRes = nameRes;
    }

    /** Tint cho 1 màu quân (0 = không tint). */
    public int tintFor(boolean isWhite) {
        return isWhite ? whiteTint : blackTint;
    }

    /** An toàn parse theo tên (mặc định CLASSIC nếu null/lạ). */
    public static PieceSet fromName(String name) {
        if (name != null) {
            for (PieceSet s : values()) if (s.name().equals(name)) return s;
        }
        return CLASSIC;
    }
}
