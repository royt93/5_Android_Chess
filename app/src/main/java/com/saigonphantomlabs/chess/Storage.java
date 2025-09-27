package com.saigonphantomlabs.chess;

import java.lang.ref.WeakReference;

/*
 * Why I didn't use startActivityForResult() ???
 * Because this one is easier ...
 * */

public class Storage {
    public static Chessman.ChessmanType result = Chessman.ChessmanType.Pawn;
    private static WeakReference<Chess> chessRef = null;

    public static Chess getChess() {
        return chessRef != null ? chessRef.get() : null;
    }

    public static void setChess(Chess chess) {
        chessRef = chess != null ? new WeakReference<>(chess) : null;
    }

    public static void clearChess() {
        chessRef = null;
    }

    // Backward compatibility
    public static Chess chess = null;
}
