package com.brouken.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

/**
 * SpeedBoost - hold the button for a temporary playback speed boost,
 * drag vertically while holding to smoothly change the boost level.
 * Quick tap toggles a pinned boost. Level is remembered.
 */
@OptIn(markerClass = UnstableApi.class)
public class SpeedBoost {

    private static final float MIN_SPEED = 2.0f;
    private static final float MAX_SPEED = 5.0f;
    private static final float SPEED_PER_PX = 0.005f;   // ~600px drag covers the full range
    private static final long TAP_MS = 280;
    private static final int VIOLET = 0xFFB7ABFF;

    private final PlayerActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ImageButton button;
    private TextView pill;
    private boolean pillAdded;

    private float level = MIN_SPEED;
    private boolean boosted = false;
    private boolean pinned = false;
    private float restoreSpeed = 1f;

    private float downY;
    private long downAt;
    private float dragBase;
    private boolean dragging;

    private Player.Listener boostListener;

    private final Runnable hidePill = () -> hidePill();

    public SpeedBoost(PlayerActivity activity) {
        this.activity = activity;
        SharedPreferences sp = prefs();
        level = Math.min(MAX_SPEED, Math.max(MIN_SPEED, sp.getFloat("level", MIN_SPEED)));
    }

    private SharedPreferences prefs() {
        return activity.getSharedPreferences("speed_boost", Context.MODE_PRIVATE);
    }

    public ImageButton createButton() {
        button = new ImageButton(activity, null, 0, R.style.ExoStyledControls_Button_Bottom);
        button.setImageResource(R.drawable.ic_boost);
        button.setId(View.generateViewId());
        button.setContentDescription("Speed boost");
        button.setOnTouchListener(onTouch());
        if (pinned) applyTint();
        return button;
    }

    private View.OnTouchListener onTouch() {
        return (v, ev) -> {
            if (PlayerActivity.player == null) return false;
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = ev.getRawY();
                    downAt = System.currentTimeMillis();
                    dragging = false;
                    if (!boosted && !pinned) startBoost(level);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dy = downY - ev.getRawY();
                    if (!dragging && Math.abs(dy) > ViewConfiguration.get(activity).getScaledTouchSlop()) {
                        dragging = true;
                        dragBase = level;
                    }
                    if (dragging) {
                        setLevel(dragBase + dy * SPEED_PER_PX);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean quickTap = !dragging && (System.currentTimeMillis() - downAt) < TAP_MS;
                    if (quickTap) {
                        // tap: toggle pinned boost
                        if (boosted) {
                            stopBoost();
                            pinned = false;
                        } else {
                            pinned = true;
                            startBoost(level);
                        }
                    } else {
                        if (pinned && !boosted) {
                            // was pinned, held again to adjust: re-boost handled below
                        }
                        if (boosted && !pinned) stopBoost();
                        if (dragging) saveLevel();
                    }
                    dragging = false;
                    return true;
            }
            return false;
        };
    }

    private void startBoost(float speed) {
        Player p = PlayerActivity.player;
        if (p == null) return;
        restoreSpeed = p.getPlaybackParameters().speed;
        p.setPlaybackSpeed(speed);
        boosted = true;
        showPill();
        applyTint();
        attachListener(p);
    }

    private void stopBoost() {
        Player p = PlayerActivity.player;
        if (p != null && boosted) p.setPlaybackSpeed(restoreSpeed);
        boosted = false;
        applyTint();
        detachListener();
        handler.removeCallbacks(hidePill);
        handler.postDelayed(hidePill, 350);
    }

    private void setLevel(float v) {
        level = Math.min(MAX_SPEED, Math.max(MIN_SPEED, v));
        if (boosted && PlayerActivity.player != null) {
            PlayerActivity.player.setPlaybackSpeed(level);
        }
        updatePill();
    }

    private void saveLevel() {
        prefs().edit().putFloat("level", level).apply();
    }

    private void attachListener(Player p) {
        detachListener();
        boostListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying && boosted && !pinned) {
                    stopBoost();
                }
            }
            @Override
            public void onPlaybackStateChanged(int state) {
                if ((state == Player.STATE_ENDED || state == Player.STATE_IDLE) && boosted) {
                    stopBoost();
                    pinned = false;
                }
            }
        };
        p.addListener(boostListener);
    }

    private void detachListener() {
        if (boostListener != null && PlayerActivity.player != null) {
            PlayerActivity.player.removeListener(boostListener);
        }
        boostListener = null;
    }

    private void applyTint() {
        if (button == null) return;
        if (boosted || pinned) button.setColorFilter(VIOLET);
        else button.clearColorFilter();
    }

    // ---------------- pill OSD ----------------

    private void ensurePill() {
        if (pillAdded) return;
        ViewGroup playerView = activity.findViewById(R.id.player_view);
        if (playerView == null) return;
        pill = new TextView(activity);
        pill.setTypeface(Typeface.DEFAULT_BOLD);
        pill.setTextColor(Color.WHITE);
        pill.setTextSize(15);
        int hpad = dp(14), vpad = dp(7);
        pill.setPadding(hpad, vpad, hpad, vpad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6141219);
        bg.setCornerRadius(dp(22));
        pill.setBackground(bg);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = dp(48);
        pill.setLayoutParams(lp);
        pill.setAlpha(0f);
        playerView.addView(pill);
        pillAdded = true;
    }

    private void showPill() {
        ensurePill();
        if (pill == null) return;
        updatePill();
        handler.removeCallbacks(hidePill);
        pill.animate().alpha(1f).setDuration(140).start();
    }

    private void updatePill() {
        if (pill != null) pill.setText(String.format("%.2fx", level));
    }

    private void hidePill() {
        if (pill != null && !boosted) pill.animate().alpha(0f).setDuration(200).start();
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}