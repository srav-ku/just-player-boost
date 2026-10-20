package com.brouken.player;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Shared chrome for the player's pop-up menus. One hairline card, one type
 * scale, one motion curve - so speed, audio and subtitle menus feel like the
 * same system instead of three different dialogs.
 */
public abstract class PremiumSheet extends Dialog {

    protected static final int CARD_COLOR = 0xF2161618;
    protected static final int HAIRLINE = 0x1FFFFFFF;
    protected static final int ROW_COLOR = 0x0FFFFFFF;
    protected static final int ROW_SELECTED = 0x33B7ABFF;
    protected static final int ACCENT = 0xFFB7ABFF;
    protected static final int TEXT = 0xFFF5F5F7;
    protected static final int TEXT_DIM = 0xFF8E8E93;

    protected LinearLayout card;

    protected PremiumSheet(@NonNull Context context) {
        super(context);
    }

    protected int dp(float value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    protected int maxCardWidth() {
        return Math.min(dp(470), getContext().getResources().getDisplayMetrics().widthPixels - dp(96));
    }

    /** Builds the card and returns the vertical container to fill. */
    protected LinearLayout createCard() {
        card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(10), dp(22), dp(18));

        GradientDrawable background = new GradientDrawable();
        background.setColor(CARD_COLOR);
        background.setCornerRadius(dp(26));
        background.setStroke(dp(1), HAIRLINE);
        card.setBackground(background);

        View handle = new View(getContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x2EFFFFFF);
        handleBg.setCornerRadius(dp(2));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(34), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(16);
        card.addView(handle, handleParams);

        return card;
    }

    protected void addHeader(String title, String trailing) {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextColor(TEXT);
        titleView.setTextSize(16.5f);
        titleView.setLetterSpacing(0.01f);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(titleView);

        if (trailing != null) {
            TextView trailingView = new TextView(getContext());
            trailingView.setText(trailing);
            trailingView.setTextColor(ACCENT);
            trailingView.setTextSize(13);
            trailingView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            trailingView.setBackground(pillBackground());
            trailingView.setPadding(dp(10), dp(3), dp(10), dp(3));
            header.addView(trailingView);
        }

        card.addView(header);
    }

    protected void addCaption(String caption) {
        TextView captionView = new TextView(getContext());
        captionView.setText(caption);
        captionView.setTextColor(TEXT_DIM);
        captionView.setTextSize(12.5f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        card.addView(captionView, params);
    }

    /** A quiet row: 46dp tall, hairline outline, rounded 13dp. */
    protected LinearLayout createRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(14), 0);
        row.setBackground(rowBackground(false));
        row.setClickable(true);
        row.setFocusable(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            row.setDefaultFocusHighlightEnabled(false);
        }
        return row;
    }

    protected void setRowSelected(LinearLayout row, boolean selected) {
        row.setBackground(rowBackground(selected));
    }

    protected GradientDrawable rowBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(13));
        background.setColor(selected ? ROW_SELECTED : ROW_COLOR);
        if (!selected) {
            background.setStroke(dp(1), 0x14FFFFFF);
        }
        return background;
    }

    protected GradientDrawable pillBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));
        background.setColor(0x22B7ABFF);
        return background;
    }

    protected TextView createRowLabel(String text) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setTextSize(14.5f);
        label.setTextColor(TEXT);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return label;
    }

    protected TextView sectionLabel(String text) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setTextColor(TEXT_DIM);
        label.setTextSize(11.5f);
        label.setLetterSpacing(0.08f);
        label.setAllCaps(true);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(16);
        params.bottomMargin = dp(8);
        label.setLayoutParams(params);
        return label;
    }

    /** Chips need full width so every column keeps its even weight. */
    protected void addFullWidth(View view) {
        card.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    protected void addFullWidth(LinearLayout parent, View view) {
        parent.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Chip grid with one violet selection; callbacks run on tap. */
    protected static class ChipGrid extends LinearLayout {

        public interface OnChipSelected {
            void onChipSelected(int index);
        }

        private final TextView[] chips;
        private final int accent;
        private final int rowColor;
        private final int radius;
        private final int gap;
        private int selected;

        public ChipGrid(android.content.Context context, String[] labels, int columns,
                        int selected, int accent, int rowColor, int radiusDp, int gapDp,
                        int heightDp, float textSize, OnChipSelected listener) {
            super(context);
            this.chips = new TextView[labels.length];
            this.accent = accent;
            this.rowColor = rowColor;
            this.radius = radiusDp;
            this.gap = gapDp;
            this.selected = selected;
            setOrientation(VERTICAL);

            final float density = context.getResources().getDisplayMetrics().density;
            LinearLayout row = null;
            for (int i = 0; i < labels.length; i++) {
                if (i % columns == 0) {
                    row = new LinearLayout(context);
                    row.setOrientation(HORIZONTAL);
                    LayoutParams rowParams = new LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (i > 0) {
                        rowParams.topMargin = Math.round(gapDp * density);
                    }
                    addView(row, rowParams);
                }
                final int index = i;
                TextView chip = new TextView(context);
                chip.setText(labels[i]);
                chip.setGravity(Gravity.CENTER);
                chip.setTextSize(textSize);
                chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                chip.setClickable(true);
                chip.setFocusable(false);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    chip.setDefaultFocusHighlightEnabled(false);
                }
                chip.setTextColor(TEXT);
                LayoutParams params = new LayoutParams(0, Math.round(heightDp * density), 1f);
                if (i % columns > 0) {
                    params.setMarginStart(Math.round(gapDp * density));
                }
                chip.setLayoutParams(params);
                chip.setOnClickListener(v -> {
                    setSelected(index);
                    if (listener != null) {
                        listener.onChipSelected(index);
                    }
                });
                this.chips[i] = chip;
                row.addView(chip);
            }
            paint();
        }

        public void setSelected(int index) {
            selected = index;
            paint();
        }

        public int getSelected() {
            return selected;
        }

        private void paint() {
            final float density = getContext().getResources().getDisplayMetrics().density;
            for (int i = 0; i < chips.length; i++) {
                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(Math.round(radius * density));
                boolean active = i == selected;
                background.setColor(active ? accent : rowColor);
                if (!active) {
                    background.setStroke(Math.round(density), 0x14FFFFFF);
                }
                chips[i].setBackground(background);
                chips[i].setTextColor(active ? 0xFFFFFFFF : TEXT);
            }
        }
    }

    protected FrameLayout createCheck() {
        FrameLayout holder = new FrameLayout(getContext());
        android.widget.ImageView check = new android.widget.ImageView(getContext());
        check.setImageDrawable(new PlayerIconDrawable(PlayerIconDrawable.CHECK,
                getContext().getResources().getDisplayMetrics().density));
        check.setColorFilter(ACCENT);
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER);
        holder.addView(check, checkParams);
        return holder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(buildContent());
        setCanceledOnTouchOutside(true);
    }

    protected abstract View buildContent();

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.5f);
        window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.y = dp(18);
        window.setAttributes(windowParams);

        window.setLayout(maxCardWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);

        card.setTranslationY(dp(26));
        card.setAlpha(0f);
        card.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(210)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start();
    }

    @Override
    public void dismiss() {
        if (card != null && isShowing()) {
            card.animate()
                    .translationY(dp(18))
                    .alpha(0f)
                    .setDuration(130)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .withEndAction(() -> PremiumSheet.super.dismiss())
                    .start();
        } else {
            super.dismiss();
        }
    }
}
