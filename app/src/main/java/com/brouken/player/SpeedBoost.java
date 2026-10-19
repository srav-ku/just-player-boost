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
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

/**
 * SpeedBoost - MX Player style: long-press on the video to temporarily boost
 * playback speed. Drag right to increase, left to decrease (down to 1.0x).
 * Release always returns to the exact pre-boost speed. Level is remembered.
 */
@OptIn(markerClass = UnstableApi.class)
public class SpeedBoost {

    private static final float MIN_SPEED = 1.0f;
    private static final float MAX_SPEED = 4.0f;
    private static final float START_SPEED = 2.0f;
    private static final float SPEED_PER_PX = 0.005f;

    private final PlayerActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView pill;
    private boolean pillAdded;

    private float level = START_SPEED;     // remembered boost level
    private boolean boosted = false;
    private float baseSpeed = 1f;          // exact speed before boost
    private float startX;
    private float dragBase;

    private Player.Listener boostListener;
    private final Runnable hidePill = this::hidePill;

    public SpeedBoost(PlayerActivity activity) {
        this.activity = activity;
        SharedPreferences sp = activity.getSharedPreferences("speed_boost", Context.MODE_PRIVATE);
        level = Math.min(MAX_SPEED, Math.max(MIN_SPEED, sp.getFloat("level", START_SPEED)));
    }

    public void attach() {
        View surface = activity.findViewById(R.id.video_view);
        if (surface instanceof CustomPlayerView) {
            CustomPlayerView cpv = (CustomPlayerView) surface;
            cpv.setSpeedBoost(this);
            cpv.setOnTouchListener(onTouch());
        }
    }

    /** Called by CustomPlayerView when a long-press is confirmed. */
    public void onLongPressDetected(float rawX) {
        Player p = PlayerActivity.player;
        if (boosted || PlayerActivity.locked || p == null || !p.isPlaying()) return;
        // if last level was nearly 1x a hold would do nothing; start at 2x instead
        float start = level < 1.25f ? START_SPEED : level;
        startX = rawX;
        startBoost(start);
    }

    private View.OnTouchListener onTouch() {
        return (v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    if (boosted) {
                        float dx = ev.getRawX() - startX;
                        setLevel(dragBase + dx * SPEED_PER_PX);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (boosted) return true;
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (boosted) {
                        stopBoost();
                        saveLevel();
                        return true;
                    }
                    return false;
            }
            return false;
        };
    }

    private void startBoost(float start) {
        Player p = PlayerActivity.player;
        baseSpeed = p.getPlaybackParameters().speed;
        dragBase = start;
        p.setPlaybackSpeed(start);
        boosted = true;
        showPill();
        attachListener(p);
    }

    private void stopBoost() {
        Player p = PlayerActivity.player;
        if (p != null && boosted) p.setPlaybackSpeed(baseSpeed);
        boosted = false;
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
        activity.getSharedPreferences("speed_boost", Context.MODE_PRIVATE)
                .edit().putFloat("level", level).apply();
    }

    private void attachListener(Player p) {
        detachListener();
        boostListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isPlaying && boosted) stopBoost();
            }
            @Override
            public void onPlaybackStateChanged(int state) {
                if ((state == Player.STATE_ENDED || state == Player.STATE_IDLE) && boosted) {
                    stopBoost();
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

    // ---------------- pill OSD ----------------

    private void ensurePill() {
        if (pillAdded) return;
        ViewGroup playerView = activity.findViewById(R.id.video_view);
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

    private void hidePill() {
        if (pill != null && !boosted) pill.animate().alpha(0f).setDuration(200).start();
    }

    private void updatePill() {
        if (pill != null) pill.setText(String.format("%.2fx", level));
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}