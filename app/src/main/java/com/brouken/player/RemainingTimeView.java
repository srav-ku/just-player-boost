package com.brouken.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.media3.common.Player;

import java.util.Locale;

/**
 * Duration label. Shows "-1:23:45" (time left) by default; tapping the time
 * group flips it to the total duration and back.
 */
public class RemainingTimeView extends AppCompatTextView {

    private Player player;
    private boolean showRemaining = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refresh();
            if (player != null && isShown()) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    public RemainingTimeView(Context context) {
        super(context);
    }

    public RemainingTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RemainingTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPlayer(Player player) {
        this.player = player;
        handler.removeCallbacks(ticker);
        if (player != null && isShown()) {
            refresh();
            handler.postDelayed(ticker, 1000);
        }
    }

    public boolean isShowRemaining() {
        return showRemaining;
    }

    public void toggleRemaining() {
        showRemaining = !showRemaining;
        refresh();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (player != null && text != null && text.length() > 0 && text.charAt(0) != '-') {
            CharSequence value = formatted();
            if (value != null) {
                text = value;
            }
        }
        super.setText(text, type);
    }

    private CharSequence formatted() {
        try {
            long duration = player.getDuration();
            if (duration <= 0) {
                return null;
            }
            if (showRemaining) {
                long remaining = duration - player.getCurrentPosition();
                if (remaining < 0) {
                    remaining = 0;
                }
                return "-" + format(remaining);
            }
            return format(duration);
        } catch (Exception e) {
            return null;
        }
    }

    private void refresh() {
        CharSequence value = player == null ? null : formatted();
        if (value == null) {
            return;
        }
        try {
            super.setText(value, BufferType.NORMAL);
        } catch (Exception ignored) {
        }
    }

    private static String format(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }
}
