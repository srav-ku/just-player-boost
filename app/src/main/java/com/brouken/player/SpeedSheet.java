package com.brouken.player;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Compact, premium playback-speed picker: a floating dark card with rounded
 * corners, a drag handle and evenly spaced speed chips.
 */
public class SpeedSheet extends Dialog {

    public interface OnSpeedSelected {
        void onSpeedSelected(float speed);
    }

    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f};

    private static final int CARD_COLOR = 0xF21A1A1C;
    private static final int CHIP_COLOR = 0xFF27272B;
    private static final int ACCENT = 0xFFB7ABFF;

    private final float current;
    private final OnSpeedSelected callback;
    private final int dp24;
    private LinearLayout card;

    public SpeedSheet(@NonNull Context context, float current, OnSpeedSelected callback) {
        super(context);
        this.current = current;
        this.callback = callback;
        this.dp24 = Math.round(24 * context.getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(12), dp(24), dp(22));
        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD_COLOR);
        background.setCornerRadius(dp(28));
        background.setStroke(dp(1), 0x14FFFFFF);
        card.setBackground(background);

        View handle = new View(getContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x38FFFFFF);
        handleBg.setCornerRadius(dp(2));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(18);
        card.addView(handle, handleParams);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(getContext());
        title.setText("Playback speed");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        TextView currentLabel = new TextView(getContext());
        currentLabel.setText(format(current));
        currentLabel.setTextColor(ACCENT);
        currentLabel.setTextSize(15);
        currentLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(currentLabel);

        card.addView(header);

        TextView subtitle = new TextView(getContext());
        subtitle.setText("Hold anywhere on the video to boost");
        subtitle.setTextColor(0xFF8E8E93);
        subtitle.setTextSize(12.5f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(3);
        card.addView(subtitle, subtitleParams);

        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(4);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = dp(18);
        card.addView(grid, gridParams);

        for (int i = 0; i < SPEEDS.length; i++) {
            final float speed = SPEEDS[i];
            TextView chip = new TextView(getContext());
            chip.setText(format(speed));
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(15);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setBackground(chipBackground(Math.abs(speed - current) < 0.011f));
            chip.setTextColor(Math.abs(speed - current) < 0.011f ? 0xFF0E0E10 : 0xF2FFFFFF);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 4), GridLayout.spec(i % 4, 1f));
            params.width = 0;
            params.height = dp(44);
            params.setMargins(dp(5), dp(5), dp(5), dp(5));
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                if (callback != null) {
                    callback.onSpeedSelected(speed);
                }
                dismiss();
            });

            grid.addView(chip);
        }

        setContentView(card);
        setCanceledOnTouchOutside(true);
    }

    private GradientDrawable chipBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(14));
        background.setColor(selected ? ACCENT : CHIP_COLOR);
        return background;
    }

    private static String format(float speed) {
        if (Math.abs(speed - Math.round(speed)) < 0.011f) {
            return String.format(Locale.US, "%d×", Math.round(speed));
        }
        return String.format(Locale.US, "%.2f×", speed).replace("0×", "×");
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.55f);
        window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.y = dp(22);
        window.setAttributes(windowParams);

        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(dp(460), screenWidth - dp(64));
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

        card.setTranslationY(dp(48));
        card.setAlpha(0f);
        card.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(230)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();
    }
}
