package com.brouken.player;

import android.content.Context;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.ui.AspectRatioFrameLayout;

/**
 * MX Player style display menu: pick how the frame is fitted, then fine-tune
 * the zoom. Fit keeps the whole picture, Stretch fills and distorts, Crop fills
 * and trims the overflow.
 */
public class AspectSheet extends PremiumSheet {

    public interface OnAspectSelected {
        void onAspectSelected(int resizeMode, float scale);
    }

    private static final int[] ZOOMS = {80, 90, 100, 110, 125, 150};

    private final int currentMode;
    private final float currentScale;
    private final OnAspectSelected callback;

    private ChipGrid modeGrid;
    private ChipGrid zoomGrid;
    private TextView valuePill;

    public AspectSheet(@NonNull Context context, int currentMode, float currentScale,
                       OnAspectSelected callback) {
        super(context);
        this.currentMode = currentMode;
        this.currentScale = currentScale;
        this.callback = callback;
    }

    private static String modeName(int mode) {
        switch (mode) {
            case AspectRatioFrameLayout.RESIZE_MODE_FILL:
                return "Stretch";
            case AspectRatioFrameLayout.RESIZE_MODE_ZOOM:
                return "Crop";
            default:
                return "Fit";
        }
    }

    private static int zoomIndex(float scale) {
        final int percent = Math.round(scale * 100f);
        for (int i = 0; i < ZOOMS.length; i++) {
            if (ZOOMS[i] == percent) {
                return i;
            }
        }
        return 2;
    }

    private static int modeIndex(int mode) {
        switch (mode) {
            case AspectRatioFrameLayout.RESIZE_MODE_FILL:
                return 1;
            case AspectRatioFrameLayout.RESIZE_MODE_ZOOM:
                return 2;
            default:
                return 0;
        }
    }

    private static int modeForIndex(int index) {
        switch (index) {
            case 1:
                return AspectRatioFrameLayout.RESIZE_MODE_FILL;
            case 2:
                return AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
            default:
                return AspectRatioFrameLayout.RESIZE_MODE_FIT;
        }
    }

    @Override
    protected View buildContent() {
        LinearLayout content = createCard();
        valuePill = null;
        addHeader("Aspect ratio", valuePillText());

        content.addView(sectionLabel("Frame"));
        modeGrid = new ChipGrid(getContext(),
                new String[]{"Fit", "Stretch", "Crop"}, 3, modeIndex(currentMode),
                ACCENT, ROW_COLOR, 13, 8, 46, 13.5f,
                index -> {
                    apply(modeForIndex(index), 1f);
                    dismiss();
                });
        addFullWidth(modeGrid);

        content.addView(sectionLabel("Zoom"));
        zoomGrid = new ChipGrid(getContext(),
                new String[]{"80%", "90%", "100%", "110%", "125%", "150%"}, 3,
                zoomIndex(currentScale), ACCENT, ROW_COLOR, 13, 8, 44, 13f,
                index -> {
                    apply(currentMode, ZOOMS[index] / 100f);
                    dismiss();
                });
        addFullWidth(zoomGrid);

        TextView hint = new TextView(getContext());
        hint.setText("Fit shows the whole frame · Crop trims the edges to fill");
        hint.setTextColor(TEXT_DIM);
        hint.setTextSize(12f);
        hint.setGravity(Gravity.START);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(12);
        content.addView(hint, hintParams);

        return content;
    }

    private String valuePillText() {
        final int percent = Math.round(currentScale * 100f);
        return percent == 100 ? modeName(currentMode) : modeName(currentMode) + " " + percent + "%";
    }

    private void apply(int resizeMode, float scale) {
        if (card != null) {
            card.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        if (callback != null) {
            callback.onAspectSelected(resizeMode, scale);
        }
    }
}
