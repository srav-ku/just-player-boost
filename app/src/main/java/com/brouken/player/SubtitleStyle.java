package com.brouken.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;

import androidx.media3.ui.CaptionStyleCompat;

/**
 * Subtitle appearance, stored per device. Everything here maps one-to-one to
 * what {@link androidx.media3.ui.SubtitleView} and CaptionStyleCompat can draw,
 * so the in-player preview is honest about what the video will look like.
 */
public class SubtitleStyle {

    private static final String PREFS = "subtitle_style";

    public static final int[] COLORS = {
            0xFFFFFFFF, 0xFFFFE066, 0xFF6EE7F9, 0xFF7CE38B,
            0xFFFFB86B, 0xFFC4B5FD, 0xFFF9A8D4
    };
    public static final String[] COLOR_NAMES = {
            "White", "Yellow", "Cyan", "Green", "Orange", "Violet", "Pink"
    };

    public static final int BG_NONE = 0;
    public static final int BG_SOFT = 1;
    public static final int BG_SOLID = 2;
    public static final int BG_VIOLET = 3;
    public static final String[] BG_NAMES = {"None", "Soft", "Solid", "Violet"};

    public static final int EDGE_NONE = CaptionStyleCompat.EDGE_TYPE_NONE;
    public static final int EDGE_OUTLINE = CaptionStyleCompat.EDGE_TYPE_OUTLINE;
    public static final int EDGE_SHADOW = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW;
    public static final int[] EDGES = {EDGE_NONE, EDGE_OUTLINE, EDGE_SHADOW};
    public static final String[] EDGE_NAMES = {"None", "Outline", "Shadow"};

    public static final String[] SIZE_NAMES = {"80%", "100%", "120%", "150%"};
    public static final float[] SIZES = {0.85f, 1f, 1.22f, 1.5f};

    public static final String[] POSITION_NAMES = {"Low", "Mid", "High"};
    public static final float[] POSITIONS = {0.03f, 0.09f, 0.18f};

    public int colorIndex = 0;
    public int background = BG_NONE;
    public int edgeIndex = 1;
    public int sizeIndex = 1;
    public int positionIndex = 1;
    public boolean bold = false;

    public static SubtitleStyle load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SubtitleStyle style = new SubtitleStyle();
        style.colorIndex = Math.min(COLORS.length - 1,
                Math.max(0, sp.getInt("color", 0)));
        style.background = sp.getInt("background", BG_NONE);
        style.edgeIndex = Math.min(EDGES.length - 1, Math.max(0, sp.getInt("edge", 1)));
        style.sizeIndex = Math.min(SIZES.length - 1, Math.max(0, sp.getInt("size", 1)));
        style.positionIndex = Math.min(POSITIONS.length - 1, Math.max(0, sp.getInt("position", 1)));
        style.bold = sp.getBoolean("bold", false);
        return style;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("color", colorIndex)
                .putInt("background", background)
                .putInt("edge", edgeIndex)
                .putInt("size", sizeIndex)
                .putInt("position", positionIndex)
                .putBoolean("bold", bold)
                .apply();
    }

    public SubtitleStyle copy() {
        SubtitleStyle style = new SubtitleStyle();
        style.colorIndex = colorIndex;
        style.background = background;
        style.edgeIndex = edgeIndex;
        style.sizeIndex = sizeIndex;
        style.positionIndex = positionIndex;
        style.bold = bold;
        return style;
    }

    public int textColor() {
        return COLORS[colorIndex];
    }

    public int backgroundColor() {
        switch (background) {
            case BG_SOFT:
                return 0x99000000;
            case BG_SOLID:
                return Color.BLACK;
            case BG_VIOLET:
                return 0xCC8A5CF6;
            default:
                return Color.TRANSPARENT;
        }
    }

    public int edgeType() {
        return EDGES[edgeIndex];
    }

    public int edgeColor() {
        return Color.BLACK;
    }

    public float sizeScale() {
        return SIZES[sizeIndex];
    }

    public float bottomFraction() {
        return POSITIONS[positionIndex];
    }

    public Typeface typeface() {
        return Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    public CaptionStyleCompat toCaptionStyle() {
        return new CaptionStyleCompat(
                textColor(),
                backgroundColor(),
                Color.TRANSPARENT,
                edgeType(),
                edgeColor(),
                typeface());
    }
}
