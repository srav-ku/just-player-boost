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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Rounded dark sheet listing subtitle tracks in the same visual language as
 * {@link SpeedSheet}: quiet rows, violet selection, no system dialog chrome.
 */
public class TrackSheet extends Dialog {

    public interface OnTrackSelected {
        void onTrackSelected(int index);
    }

    private static final int CARD_COLOR = 0xF21A1A1C;
    private static final int ROW_COLOR = 0xFF27272B;
    private static final int ACCENT = 0xFFB7ABFF;

    private final String title;
    private final List<String> labels;
    private final int selectedIndex;
    private final OnTrackSelected callback;
    private LinearLayout card;

    public TrackSheet(@NonNull Context context, String title, List<String> labels,
                      int selectedIndex, OnTrackSelected callback) {
        super(context);
        this.title = title;
        this.labels = labels;
        this.selectedIndex = selectedIndex;
        this.callback = callback;
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
        card.setPadding(dp(24), dp(12), dp(24), dp(20));
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

        TextView heading = new TextView(getContext());
        heading.setText(title);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(20);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(heading);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(14);
        scroll.setLayoutParams(scrollParams);

        LinearLayout rows = new LinearLayout(getContext());
        rows.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            final boolean selected = i == selectedIndex;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), 0, dp(16), 0);
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dp(14));
            rowBg.setColor(selected ? 0x1FB7ABFF : ROW_COLOR);
            row.setBackground(rowBg);
            row.setClickable(true);
            row.setFocusable(true);

            TextView label = new TextView(getContext());
            label.setText(labels.get(i));
            label.setTextSize(15.5f);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setTextColor(selected ? ACCENT : 0xF2FFFFFF);
            label.setTypeface(Typeface.create("sans-serif", selected ? Typeface.BOLD : Typeface.NORMAL));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(label, labelParams);

            TextView check = new TextView(getContext());
            check.setText("✓");
            check.setTextSize(16);
            check.setTextColor(ACCENT);
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            row.addView(check);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
            rowParams.bottomMargin = dp(8);
            rows.addView(row, rowParams);

            row.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                if (callback != null) {
                    callback.onTrackSelected(index);
                }
                dismiss();
            });
        }

        scroll.addView(rows);
        card.addView(scroll);

        setContentView(card);
        setCanceledOnTouchOutside(true);
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
