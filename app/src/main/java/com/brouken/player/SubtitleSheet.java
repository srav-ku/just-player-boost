package com.brouken.player;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Subtitle appearance editor: size, colour, background box, edge and screen
 * position, previewed live against a simulated video frame and applied to the
 * running player as you tap.
 */
public class SubtitleSheet extends PremiumSheet {

    public interface OnStyleChanged {
        void onStyleChanged(SubtitleStyle style);
    }

    private static final String SAMPLE = "Subtitle preview";

    private final SubtitleStyle style;
    private final OnStyleChanged callback;

    private FrameLayout previewFrame;
    private TextView previewText;
    private LinearLayout colorRow;
    private int previewHeight;

    private ChipGrid sizeGrid;
    private ChipGrid backgroundGrid;
    private ChipGrid edgeGrid;
    private ChipGrid positionGrid;
    private ChipGrid weightGrid;

    public SubtitleSheet(@NonNull Context context, SubtitleStyle style, OnStyleChanged callback) {
        super(context);
        this.style = style.copy();
        this.callback = callback;
    }

    @Override
    protected View buildContent() {
        final int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        LinearLayout content = createCard();
        addHeader("Subtitles", SubtitleStyle.SIZE_NAMES[style.sizeIndex]);

        previewHeight = Math.min(dp(96), Math.round(screenHeight * 0.19f));
        previewFrame = new FrameLayout(getContext());
        GradientDrawable frameBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF23232A, 0xFF141418, 0xFF0B0B0D});
        frameBg.setCornerRadius(dp(14));
        previewFrame.setBackground(frameBg);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, previewHeight);
        previewParams.topMargin = dp(12);
        content.addView(previewFrame, previewParams);

        previewText = new TextView(getContext());
        previewText.setText(SAMPLE);
        previewText.setMaxLines(2);
        previewText.setGravity(Gravity.CENTER);
        previewText.setIncludeFontPadding(false);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        textParams.leftMargin = dp(10);
        textParams.rightMargin = dp(10);
        previewFrame.addView(previewText, textParams);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min(dp(158), Math.round(screenHeight * 0.32f)));
        scrollParams.topMargin = dp(4);
        content.addView(scroll, scrollParams);

        LinearLayout options = new LinearLayout(getContext());
        options.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(options, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        options.addView(sectionLabel("Size"));
        sizeGrid = new ChipGrid(getContext(), SubtitleStyle.SIZE_NAMES, 4, style.sizeIndex,
                ACCENT, ROW_COLOR, 11, 6, 36, 12.5f, index -> {
                    style.sizeIndex = index;
                    changed();
                });
        options.addView(sizeGrid, fullWidth());

        options.addView(sectionLabel("Colour"));
        colorRow = new LinearLayout(getContext());
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < SubtitleStyle.COLORS.length; i++) {
            final int index = i;
            View swatch = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(26), dp(26));
            params.setMarginEnd(dp(7));
            swatch.setLayoutParams(params);
            swatch.setClickable(true);
            swatch.setOnClickListener(v -> {
                style.colorIndex = index;
                changed();
            });
            colorRow.addView(swatch);
        }
        options.addView(colorRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        options.addView(sectionLabel("Background"));
        backgroundGrid = new ChipGrid(getContext(), SubtitleStyle.BG_NAMES, 4, style.background,
                ACCENT, ROW_COLOR, 11, 6, 36, 12.5f, index -> {
                    style.background = index;
                    changed();
                });
        options.addView(backgroundGrid, fullWidth());

        options.addView(sectionLabel("Edge"));
        edgeGrid = new ChipGrid(getContext(), SubtitleStyle.EDGE_NAMES, 3, style.edgeIndex,
                ACCENT, ROW_COLOR, 11, 6, 36, 12.5f, index -> {
                    style.edgeIndex = index;
                    changed();
                });
        options.addView(edgeGrid, fullWidth());

        options.addView(sectionLabel("Position"));
        positionGrid = new ChipGrid(getContext(), SubtitleStyle.POSITION_NAMES, 3, style.positionIndex,
                ACCENT, ROW_COLOR, 11, 6, 36, 12.5f, index -> {
                    style.positionIndex = index;
                    changed();
                });
        options.addView(positionGrid, fullWidth());

        options.addView(sectionLabel("Weight"));
        weightGrid = new ChipGrid(getContext(), new String[]{"Regular", "Bold"}, 2, style.bold ? 1 : 0,
                ACCENT, ROW_COLOR, 11, 6, 36, 12.5f, index -> {
                    style.bold = index == 1;
                    changed();
                });
        options.addView(weightGrid, fullWidth());

        changed();
        return content;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void changed() {
        sizeGrid.setSelected(style.sizeIndex);
        backgroundGrid.setSelected(style.background);
        edgeGrid.setSelected(style.edgeIndex);
        positionGrid.setSelected(style.positionIndex);
        weightGrid.setSelected(style.bold ? 1 : 0);
        paintColors();
        paintPreview();
        if (callback != null) {
            callback.onStyleChanged(style.copy());
        }
    }

    private void paintColors() {
        if (colorRow == null) {
            return;
        }
        for (int i = 0; i < colorRow.getChildCount(); i++) {
            View swatch = colorRow.getChildAt(i);
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(SubtitleStyle.COLORS[i]);
            if (i == style.colorIndex) {
                background.setStroke(dp(2), 0xFFFFFFFF);
            } else {
                background.setStroke(dp(1), 0x33FFFFFF);
            }
            swatch.setBackground(background);
        }
    }

    private void paintPreview() {
        if (previewText == null) {
            return;
        }
        previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * style.sizeScale());
        previewText.setTextColor(style.textColor());
        previewText.setTypeface(Typeface.create("sans-serif", style.bold ? Typeface.BOLD : Typeface.NORMAL));

        switch (style.background) {
            case SubtitleStyle.BG_SOFT: {
                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(dp(5));
                background.setColor(0xB3000000);
                previewText.setBackground(background);
                previewText.setPadding(dp(6), dp(3), dp(6), dp(3));
                break;
            }
            case SubtitleStyle.BG_SOLID: {
                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(dp(4));
                background.setColor(0xFF000000);
                previewText.setBackground(background);
                previewText.setPadding(dp(6), dp(3), dp(6), dp(3));
                break;
            }
            case SubtitleStyle.BG_VIOLET: {
                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(dp(5));
                background.setColor(0xCC8A5CF6);
                previewText.setBackground(background);
                previewText.setPadding(dp(6), dp(3), dp(6), dp(3));
                break;
            }
            default:
                previewText.setBackground(null);
                previewText.setPadding(0, 0, 0, 0);
                break;
        }

        switch (style.edgeType()) {
            case SubtitleStyle.EDGE_OUTLINE:
                previewText.setShadowLayer(2.5f, 0f, 0f, 0xFF000000);
                break;
            case SubtitleStyle.EDGE_SHADOW:
                previewText.setShadowLayer(2.5f, 0f, 2f, 0xFF000000);
                break;
            default:
                previewText.setShadowLayer(0f, 0f, 0f, 0x00000000);
                break;
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) previewText.getLayoutParams();
        params.bottomMargin = Math.max(dp(6), Math.round(previewHeight * style.bottomFraction()));
        previewText.setLayoutParams(params);
    }
}
