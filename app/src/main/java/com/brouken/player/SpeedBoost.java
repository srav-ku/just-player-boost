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
 * SpeedBoost - MX Player style: long-press anywhere on the video surface to
 * temporarily boost playback speed. While holding, drag right to increase and
 * left to decrease the boost smoothly. Release returns to normal speed.
 * The last boost level is remembered for the next hold.
 */
@OptIn(markerClass = UnstableApi.class)
public class SpeedBoost {

    private static final float MIN_SPEED = 2.0f;
    private static final float MAX_SPEED = 5.0f;
    private static final float SPEED_PER_PX = 0.005f;   // ~600px drag covers full range
    private static final long LONG_PRESS_MS = 300;
    private static final int VIOLET = 0xFFB7ABFF;

    private final PlayerActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView pill;
    private boolean pillAdded;

    private float level = MIN_SPEED;
    private boolean boosted = false;
    private float restoreSpeed = 1f;

    private boolean armed = false;          // long-press pending
    private float downX, downY, lastX;
    private float dragBase;
    private int slop;

    private Player.Listener boostListener;

    private final Runnable fireBoost = this::fireBoost;
    private final Runnable hidePill = this::hidePill;

    public SpeedBoost(PlayerActivity activity) {
        this.activity = activity;
        SharedPreferences sp = activity.getSharedPreferences("speed_boost", Context.MODE_PRIVATE);
        level = Math.min(MAX_SPEED, Math.max(MIN_SPEED, sp.getFloat("level", MIN_SPEED)));
        slop = ViewConfiguration.get(activity).getScaledTouchSlop();
    }

    /** Observe touches on the video surface. Never consumes events before the
     *  boost fires, so normal taps / double-tap seek / swipes keep working.
     *  Once boosting, events are consumed so other gestures stay quiet. */
    public void attach() {
        View surface = activity.findViewById(R.id.video_view);
        if (surface == null) return;
        surface.setOnTouchListener(onTouch());
    }

    private View.OnTouchListener onTouch() {
        return (v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (ev.getPointerCount() != 1) return false;
                    downX = lastX = ev.getRawX();
                    downY = ev.getRawY();
                    armed = true;
                    handler.postDelayed(fireBoost, LONG_PRESS_MS);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (boosted) {
                        float dx = ev.getRawX() - downX;
                        setLevel(dragBase + dx * SPEED_PER_PX);
                        return true;
                    }
                    if (armed) {
                        float mx = ev.getRawX() - downX;
                        float my = ev.getRawY() - downY;
                        if (Math.hypot(mx, my) > slop) {
                            armed = false;
                            handler.removeCallbacks(fireBoost);
                        }
                    }
                    return false;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (boosted) return true;
                    armed = false;
                    handler.removeCallbacks(fireBoost);
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (boosted) {
                        stopBoost();
                        saveLevel();
                        return true;
                    }
                    armed = false;
                    handler.removeCallbacks(fireBoost);
                    return false;
            }
            return false;
        };
    }

    private void fireBoost() {
        Player p = PlayerActivity.player;
        if (!armed || p == null || !p.isPlaying()) return;
        armed = false;
        startBoost(level);
    }

    private void startBoost(float speed) {
        Player p = PlayerActivity.player;
        if (p == null) return;
        restoreSpeed = p.getPlaybackParameters().speed;
        dragBase = speed;
        p.setPlaybackSpeed(speed);
        boosted = true;
        lastX = downX;
        showPill();
        attachListener(p);
    }

    private void stopBoost() {
        Player p = PlayerActivity.player;
        if (p != null && boosted) p.setPlaybackSpeed(restoreSpeed);
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