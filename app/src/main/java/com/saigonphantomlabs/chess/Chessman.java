package com.saigonphantomlabs.chess;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import android.util.Log;

public abstract class Chessman {
    public enum ChessmanType {
        King,
        Queen,
        Rook,
        Bishop,
        Knight,
        Pawn
    }

    /*
     * .BLACK..
     * ........
     * ........
     * ...X....
     * ........
     * ........
     * ........
     * .WHITE..
     */
    public enum PlayerColor {
        Black,
        White
    }

    private enum PathConditions {
        Increase,
        Decrease,
        Hold
    }

    private Point point;

    public ArrayList<Point> moves = new ArrayList<>();
    public ChessmanType type;
    public PlayerColor color;
    public boolean isDead = false;
    public ImageButton button;
    public Chess parent;
    public int width;
    public int minDimension;

    public ArrayList<Point> getMoves() {
        return moves;
    }

    public abstract void generateMoves();

    public void setPoint(Point p) {
        point = p;
    }

    public Point getPoint() {
        return point;
    }

    public abstract void createButton();

    public void createButton(Drawable icon, int minDimension, Context ctx) {
        ImageButton btn = new ImageButton(ctx);
        width = minDimension / 8;
        this.minDimension = minDimension;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, width);

        lp.setMargins(width * getPoint().x, width * getPoint().y, minDimension - (width * getPoint().x + width),
                minDimension - (width * getPoint().y + width));

        btn.setLayoutParams(lp);
        btn.setImageDrawable(icon);
        btn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btn.setPadding(0, 0, 0, 0);
        btn.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        btn.setOnClickListener(v -> {
            // Add selection feedback animation - quick pulse
            v.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(80)
                                .withEndAction(() -> {
                                    // Ensure properties are reset after selection animation
                                    v.setScaleX(1.0f);
                                    v.setScaleY(1.0f);
                                    v.setClickable(true);
                                    v.setEnabled(true);
                                })
                                .start();
                    })
                    .start();

            parent.onManClick(this);
        });

        this.button = btn;
    }

    // Method to reset all animation properties (including position)
    public void resetButtonState() {
        resetAnimationProperties();
    }

    public void moveButton(int x, int y) {
        // Play sound with proper resource management to avoid memory leak
        try {
            MediaPlayer mp;
            if (color == PlayerColor.White) {
                mp = MediaPlayer.create(parent.ctx, R.raw.chess_1);
            } else {
                mp = MediaPlayer.create(parent.ctx, R.raw.chess_2);
            }
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get current position
        FrameLayout.LayoutParams currentLp = (FrameLayout.LayoutParams) button.getLayoutParams();
        final float startX = currentLp.leftMargin;
        final float startY = currentLp.topMargin;

        // Calculate destination using the passed parameters
        final float destX = width * x;
        final float destY = width * y;

        Log.d("roy93~", "moveButton [" + color + " " + type + "] Phase 1 STARTED -> (" + startX + "," + startY + ") to destX/Y (" + destX + "," + destY + ")");

        // Phase 1: Intense Lift and spin
        button.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .translationZ(30f)
                .rotation(45f)
                .setDuration(120)
                .setInterpolator(new OvershootInterpolator(2f))
                .withEndAction(() -> {
                    Log.d("roy93~", "moveButton [" + color + " " + type + "] Phase 2 FLIGHT started");
                    // Phase 2: Fast flight
                    button.animate()
                            .translationX(destX - startX)
                            .translationY(destY - startY)
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .rotation(-15f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .withEndAction(() -> {
                                Log.d("roy93~", "moveButton [" + color + " " + type + "] Phase 3 SLAM started");
                                // Phase 3: Slam down with huge bounce
                                button.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .translationZ(0f)
                                        .rotation(0f)
                                        .setDuration(200)
                                        .setInterpolator(new BounceInterpolator())
                                        .withEndAction(() -> {
                                            Log.d("roy93~", "moveButton [" + color + " " + type + "] FINISHED. Updating LayoutParams to Grid (" + x + "," + y + ")");
                                            updateLayoutPositionAndResetTranslation(x, y);
                                        })
                                        .start();
                            })
                            .start();
                })
                .start();
    }

    // Update the actual layout margins to the new position
    private void updateLayoutPosition(int x, int y) {
        if (button != null && button.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) button.getLayoutParams();
            lp.setMargins(width * x, width * y,
                    parent.minDimension - (width * x + width),
                    parent.minDimension - (width * y + width));
            button.setLayoutParams(lp);
        }
    }

    // Update layout position and reset translation atomically
    private void updateLayoutPositionAndResetTranslation(int x, int y) {
        if (button != null && button.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            Log.d("roy93~", "updateLayoutPositionAndResetTranslation [" + color + " " + type + "] mapping to grid (" + x + ", " + y + ")");
            // Update layout position
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) button.getLayoutParams();
            lp.setMargins(width * x, width * y,
                    parent.minDimension - (width * x + width),
                    parent.minDimension - (width * y + width));
            button.setLayoutParams(lp);

            // Reset translation and other properties
            button.setTranslationX(0f);
            button.setTranslationY(0f);
            button.setTranslationZ(0f);
            button.setScaleX(1.0f);
            button.setScaleY(1.0f);
            button.setRotation(0f);
            button.setAlpha(1.0f);
            button.setClickable(true);
            button.setEnabled(true);
            button.clearAnimation();
        }
    }

    // Reset animation properties only (not position)
    private void resetAnimationProperties() {
        if (button != null) {
            button.setScaleX(1.0f);
            button.setScaleY(1.0f);
            button.setTranslationX(0f);
            button.setTranslationY(0f);
            button.setTranslationZ(0f);
            button.setRotation(0f);
            button.setAlpha(1.0f);
            button.setClickable(true);
            button.setEnabled(true);
            button.clearAnimation();
        }
    }

    public boolean isPointSafe() {
        return isPointSafe(this.getPoint());
    }

    public boolean isPointSafe(Point point) {
        // vertical/horizontal checks
        if (!isPathSafe(point, PathConditions.Hold, PathConditions.Decrease))
            return false;
        if (!isPathSafe(point, PathConditions.Hold, PathConditions.Increase))
            return false;
        if (!isPathSafe(point, PathConditions.Decrease, PathConditions.Hold))
            return false;
        if (!isPathSafe(point, PathConditions.Increase, PathConditions.Hold))
            return false;

        // oblique checks

        if (!isPathSafe(point, PathConditions.Decrease, PathConditions.Decrease))
            return false;
        if (!isPathSafe(point, PathConditions.Increase, PathConditions.Increase))
            return false;
        if (!isPathSafe(point, PathConditions.Decrease, PathConditions.Increase))
            return false;
        if (!isPathSafe(point, PathConditions.Increase, PathConditions.Decrease))
            return false;

        // knight checks
        int x = point.x - 1;
        int y = point.y - 2;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        x = point.x - 1;
        y = point.y + 2;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        x = point.x - 2;
        y = point.y - 1;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        x = point.x - 2;
        y = point.y + 1;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        x = point.x + 1;
        y = point.y - 2;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        x = point.x + 1;
        y = point.y + 2;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        // [WARN-01] Fixed: was (-2,-1) duplicate; correct value is (+2,-1)
        x = point.x + 2;
        y = point.y - 1;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;
        x = point.x + 2;
        y = point.y + 1;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Knight)
            return false;

        // pawn checks
        if (color == PlayerColor.Black)
            y = point.y + 1;
        else
            y = point.y - 1;
        x = point.x - 1;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Pawn)
            return false;
        x = point.x + 1;
        if (Point.isValid(x, y) && parent.chessmen[x][y] != null
                && parent.chessmen[x][y].color != color
                && parent.chessmen[x][y].type == ChessmanType.Pawn)
            return false;

        // king checks
        for (int i = point.x - 1; i < point.x + 2; i++)
            for (int j = point.y - 1; j < point.y + 2; j++)
                if (Point.isValid(i, j) && !(point.x == i && point.y == j) && parent.chessmen[i][j] != null
                        && parent.chessmen[i][j].color != color && parent.chessmen[i][j].type == ChessmanType.King)
                    return false;

        return true;
    }

    private boolean isPathSafe(Point p, PathConditions xCondition, PathConditions yCondition) {
        int x = doCondition(p.x, xCondition);
        int y = doCondition(p.y, yCondition);
        while (Point.isValid(x, y)) {
            if (parent.chessmen[x][y] != null) {
                if (parent.chessmen[x][y].color == color)
                    break;
                if ((xCondition != PathConditions.Hold && yCondition != PathConditions.Hold)
                        && isThereObliqueMover(x, y) || isThereDirectMover(x, y))
                    return false;
                else
                    break;
            }
            x = doCondition(x, xCondition);
            y = doCondition(y, yCondition);
        }
        return true;
    }

    private int doCondition(int v, PathConditions c) {
        if (c == PathConditions.Increase)
            return v + 1;
        if (c == PathConditions.Decrease)
            return v - 1;
        return v;
    }

    private boolean isThereDirectMover(int x, int y) {
        return parent.chessmen[x][y] != null && (parent.chessmen[x][y].type == ChessmanType.Queen
                || parent.chessmen[x][y].type == ChessmanType.Rook);
    }

    private boolean isThereObliqueMover(int x, int y) {
        return parent.chessmen[x][y] != null && (parent.chessmen[x][y].type == ChessmanType.Queen
                || parent.chessmen[x][y].type == ChessmanType.Bishop);
    }

    public void addVerticalMovePoints() {
        /*
         * ...#....
         * ...#....
         * ...#....
         * ...X....
         * ........
         * ........
         * ........
         * ........
         */
        Point p = new Point(point.x, point.y - 1);
        while (p.isValid()) {
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                p = new Point(p.x, p.y - 1);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
        /*
         * ........
         * ........
         * ........
         * ...X....
         * ...#....
         * ...#....
         * ...#....
         * ...#....
         */
        p = new Point(point.x, point.y + 1);
        while (p.isValid()) {
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                p = new Point(p.x, p.y + 1);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
    }

    public void addHorizontalMovePoints() {
        /*
         * ........
         * ........
         * ........
         * ###X....
         * ........
         * ........
         * ........
         * ........
         */
        Point p = new Point(point.x - 1, point.y);
        while (p.isValid()) {
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                p = new Point(p.x - 1, p.y);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
        /*
         * ........
         * ........
         * ........
         * ...X####
         * ........
         * ........
         * ........
         * ........
         */
        p = new Point(point.x + 1, point.y);
        while (p.isValid()) {
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                p = new Point(p.x + 1, p.y);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
    }

    public void addAroundMovePoints() {
        /*
         * ........
         * ........
         * ..###...
         * ..#X#...
         * ..###...
         * ........
         * ........
         * ........
         */
        for (int i = this.getPoint().x - 1; i < this.getPoint().x + 2; i++)
            for (int j = this.getPoint().y - 1; j < this.getPoint().y + 2; j++)
                if (Point.isValid(i, j) && !(point.x == i && point.y == j) && isPointMovable(i, j))
                    this.moves.add(new Point(i, j));
    }

    public void addObliqueNWtoSEMovePoints() {
        /*
         * #.......
         * .#......
         * ..#.....
         * ...X....
         * ........
         * ........
         * ........
         * ........
         */
        int i = point.x - 1;
        int j = point.y - 1;
        while (Point.isValid(i, j)) {
            Point p = new Point(i, j);
            i--;
            j--;
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
        /*
         * ........
         * ........
         * ........
         * ...X....
         * ....#...
         * .....#..
         * ......#.
         * .......#
         */
        i = point.x + 1;
        j = point.y + 1;
        while (Point.isValid(i, j)) {
            Point p = new Point(i, j);
            i++;
            j++;
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
    }

    public void addObliqueNEtoSWMovePoints() {
        /*
         * ......#.
         * .....#..
         * ....#...
         * ...X....
         * ........
         * ........
         * ........
         * ........
         */
        int i = point.x + 1;
        int j = point.y - 1;
        while (Point.isValid(i, j)) {
            Point p = new Point(i, j);
            i++;
            j--;
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
        /*
         * ........
         * ........
         * ........
         * ...X....
         * ..#.....
         * .#......
         * #.......
         * ........
         */
        i = point.x - 1;
        j = point.y + 1;
        while (Point.isValid(i, j)) {
            Point p = new Point(i, j);
            i--;
            j++;
            if (parent.chessmen[p.x][p.y] == null) {
                if (!moves.contains(p))
                    moves.add(p);
                continue;
            }
            if (parent.chessmen[p.x][p.y].color != color) {
                if (!moves.contains(p))
                    moves.add(p);
                break;
            }
            break;
        }
    }

    public void add1StepForwardMovePoints() {
        if (color == PlayerColor.Black) {
            addForwardMovePoints(1);
            return;
        }
        addForwardMovePoints(-1);
    }

    public void add2StepForwardMovePoints() {
        if (color == PlayerColor.Black) {
            addForwardMovePoints(2);
            return;
        }
        addForwardMovePoints(-2);
    }

    private void addForwardMovePoints(int step) {
        Point p = new Point(point.x, point.y + step);
        if (p.isValid() && !moves.contains(p))
            moves.add(p);
    }

    public void addLMovePoints() {
        /*
         * ........
         * ..#.#...
         * .#...#..
         * ...X....
         * .#...#..
         * ..#.#...
         * ........
         * ........
         */
        Point[] lpoints = new Point[8];
        lpoints[0] = new Point(point.x - 1, point.y - 2);
        lpoints[1] = new Point(point.x - 1, point.y + 2);
        lpoints[2] = new Point(point.x - 2, point.y - 1);
        lpoints[3] = new Point(point.x - 2, point.y + 1);
        lpoints[4] = new Point(point.x + 1, point.y - 2);
        lpoints[5] = new Point(point.x + 1, point.y + 2);
        lpoints[6] = new Point(point.x + 2, point.y - 1);
        lpoints[7] = new Point(point.x + 2, point.y + 1);

        for (Point p : lpoints)
            if (p.isValid() && isPointMovable(p) && !moves.contains(p))
                moves.add(p);
    }

    private boolean isPointMovable(Point p) {
        return parent.chessmen[p.x][p.y] == null || parent.chessmen[p.x][p.y].color != color;
    }

    private boolean isPointMovable(int x, int y) {
        return parent.chessmen[x][y] == null || parent.chessmen[x][y].color != color;
    }
}
