package com.brouken.player;

import android.content.Context;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Audio / subtitle track list: quiet hairline rows, one violet tint for the
 * active track, no system dialog chrome.
 */
public class TrackSheet extends PremiumSheet {

    public interface OnTrackSelected {
        void onTrackSelected(int index);
    }

    private final String title;
    private final List<String> labels;
    private final int selectedIndex;
    private final OnTrackSelected callback;
    private final String actionLabel;
    private final Runnable action;

    public TrackSheet(@NonNull Context context, String title, List<String> labels,
                      int selectedIndex, OnTrackSelected callback) {
        this(context, title, labels, selectedIndex, callback, null, null);
    }

    public TrackSheet(@NonNull Context context, String title, List<String> labels,
                      int selectedIndex, OnTrackSelected callback,
                      String actionLabel, Runnable action) {
        super(context);
        this.title = title;
        this.labels = labels;
        this.selectedIndex = selectedIndex;
        this.callback = callback;
        this.actionLabel = actionLabel;
        this.action = action;
    }

    @Override
    protected View buildContent() {
        LinearLayout content = createCard();
        addHeader(title, labels.size() == 1 ? "1 track" : labels.size() + " tracks");

        ScrollView scroll = new ScrollView(getContext());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(14);

        int maxHeight = Math.round(getContext().getResources().getDisplayMetrics().heightPixels * 0.52f);
        scrollParams.height = Math.min(maxHeight, dp(46) * labels.size() + dp(8) * Math.max(0, labels.size() - 1));

        LinearLayout rows = new LinearLayout(getContext());
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);

        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            final boolean selected = i == selectedIndex;

            LinearLayout row = createRow();
            setRowSelected(row, selected);

            row.addView(createRowLabel(labels.get(i)));

            if (selected) {
                row.addView(createCheck());
            }

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            if (i > 0) {
                rowParams.topMargin = dp(8);
            }
            rows.addView(row, rowParams);

            row.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                if (callback != null) {
                    callback.onTrackSelected(index);
                }
                dismiss();
            });
        }

        content.addView(scroll, scrollParams);

        if (actionLabel != null) {
            LinearLayout row = createRow();
            row.setBackground(rowBackground(false));
            android.widget.TextView label = createRowLabel(actionLabel);
            label.setTextColor(ACCENT);
            row.addView(label);
            android.widget.TextView chevron = new android.widget.TextView(getContext());
            chevron.setText("\u203A");
            chevron.setTextColor(ACCENT);
            chevron.setTextSize(18);
            row.addView(chevron);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            rowParams.topMargin = dp(10);
            row.setOnClickListener(v -> {
                dismiss();
                if (action != null) {
                    action.run();
                }
            });
            content.addView(row, rowParams);
        }

        return content;
    }
}
