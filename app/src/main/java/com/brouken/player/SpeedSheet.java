package com.brouken.player;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Playback-speed picker: one card, an even grid of chips, a single violet
 * selection. Sits in the same family as the audio and subtitle sheets.
 */
public class SpeedSheet extends PremiumSheet {

    public interface OnSpeedSelected {
        void onSpeedSelected(float speed);
    }

    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f};
    private static final int SELECTED_COLOR = 0xFF8A5CF6;

    private final float current;
    private final OnSpeedSelected callback;

    public SpeedSheet(@NonNull Context context, float current, OnSpeedSelected callback) {
        super(context);
        this.current = current;
        this.callback = callback;
    }

    @Override
    protected View buildContent() {
        LinearLayout content = createCard();
        addHeader("Playback speed", format(current));
        addCaption("Hold the video to boost · drag while holding");

        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = dp(16);
        content.addView(grid, gridParams);

        LinearLayout row = null;
        for (int i = 0; i < SPEEDS.length; i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    rowParams.topMargin = dp(8);
                }
                grid.addView(row, rowParams);
            }
            row.addView(createChip(SPEEDS[i]), chipParams(i % 4));
        }

        return content;
    }

    private LinearLayout.LayoutParams chipParams(int column) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(46), 1f);
        if (column > 0) {
            params.setMarginStart(dp(8));
        }
        return params;
    }

    private TextView createChip(final float speed) {
        final boolean selected = Math.abs(speed - current) < 0.011f;

        TextView chip = new TextView(getContext());
        chip.setText(format(speed));
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(13.5f);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setClickable(true);
        chip.setFocusable(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            chip.setDefaultFocusHighlightEnabled(false);
        }
        chip.setBackground(chipBackground(selected));
        chip.setTextColor(selected ? 0xFFFFFFFF : TEXT);
        chip.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (callback != null) {
                callback.onSpeedSelected(speed);
            }
            dismiss();
        });
        return chip;
    }

    private GradientDrawable chipBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(13));
        background.setColor(selected ? SELECTED_COLOR : ROW_COLOR);
        if (!selected) {
            background.setStroke(dp(1), 0x14FFFFFF);
        }
        return background;
    }

    private static String format(float speed) {
        if (Math.abs(speed - Math.round(speed)) < 0.011f) {
            return String.format(Locale.US, "%d×", Math.round(speed));
        }
        return String.format(Locale.US, "%.2f×", speed);
    }
}
