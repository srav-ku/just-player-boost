package com.brouken.player;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Canvas-drawn control bar glyphs. Every glyph uses the same 1.7dp hairline
 * stroke and round caps so the whole bar reads as one Apple-style icon set.
 */
public class PlayerIconDrawable extends Drawable {

    public static final int SPEED = 0;
    public static final int ASPECT = 1;
    public static final int REW10 = 2;
    public static final int FWD10 = 3;
    public static final int CHECK = 4;

    private static final float STROKE = 1.7f;

    private final int type;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();

    private float density = 1f;

    public PlayerIconDrawable(int type, float density) {
        this.type = type;
        this.density = density;
        stroke.setColor(Color.WHITE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setColor(Color.WHITE);
        fill.setStyle(Paint.Style.FILL);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setTextSize(7.6f);
    }

    @Override
    public int getIntrinsicWidth() {
        return Math.round(24f * density);
    }

    @Override
    public int getIntrinsicHeight() {
        return Math.round(24f * density);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        final float scale = Math.min(bounds.width(), bounds.height()) / 24f;
        final int save = canvas.save();
        canvas.translate(bounds.exactCenterX() - 12f * scale, bounds.exactCenterY() - 12f * scale);
        canvas.scale(scale, scale);

        switch (type) {
            case SPEED:
                drawSpeed(canvas);
                break;
            case ASPECT:
                drawAspect(canvas);
                break;
            case REW10:
                drawSkip(canvas, false);
                break;
            case FWD10:
                drawSkip(canvas, true);
                break;
            case CHECK:
                drawCheck(canvas);
                break;
            default:
                break;
        }

        canvas.restoreToCount(save);
    }

    /** Gauge: a perfect circle with a gap up-right and a needle through it. */
    private void drawSpeed(Canvas canvas) {
        oval.set(3.8f, 3.8f, 20.2f, 20.2f);
        canvas.drawArc(oval, 342f, 316f, false, stroke);

        canvas.drawLine(12f, 12f, 18.1f, 6.6f, stroke);
        canvas.drawCircle(12f, 12f, 1.5f, fill);
    }

    /** Aspect frame: hairline rounded rect with four inner crop marks. */
    private void drawAspect(Canvas canvas) {
        oval.set(2.6f, 4.8f, 21.4f, 19.2f);
        canvas.drawRoundRect(oval, 3.4f, 3.4f, stroke);

        final float in = 3.3f;
        final float len = 3.3f;
        final float left = oval.left + in;
        final float right = oval.right - in;
        final float top = oval.top + in;
        final float bottom = oval.bottom - in;

        canvas.drawLine(left, top + len, left, top, stroke);
        canvas.drawLine(left, top, left + len, top, stroke);
        canvas.drawLine(right - len, top, right, top, stroke);
        canvas.drawLine(right, top, right, top + len, stroke);
        canvas.drawLine(left, bottom - len, left, bottom, stroke);
        canvas.drawLine(left, bottom, left + len, bottom, stroke);
        canvas.drawLine(right - len, bottom, right, bottom, stroke);
        canvas.drawLine(right, bottom, right, bottom - len, stroke);
    }

    /** Rewind / forward 10 seconds: thin circular arrow with the digits inside. */
    private void drawSkip(Canvas canvas, boolean forward) {
        final float cx = 12f;
        final float cy = 12.4f;
        final float r = 7.5f;

        oval.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(oval, 290f, 320f, false, stroke);

        // Arrow head on the open end of the ring, pointing the way it turns.
        path.reset();
        if (forward) {
            path.moveTo(17.8f, 6.5f);
            path.lineTo(13.8f, 7.4f);
            path.lineTo(15.3f, 3.3f);
        } else {
            path.moveTo(6.2f, 6.5f);
            path.lineTo(10.2f, 7.4f);
            path.lineTo(8.7f, 3.3f);
        }
        path.close();
        canvas.drawPath(path, fill);

        canvas.drawText("10", cx, cy + 2.7f, text);
    }

    private void drawCheck(Canvas canvas) {
        stroke.setStrokeWidth(2.1f);
        path.reset();
        path.moveTo(4.6f, 12.6f);
        path.lineTo(9.5f, 17.4f);
        path.lineTo(19.4f, 7.2f);
        canvas.drawPath(path, stroke);
        stroke.setStrokeWidth(STROKE);
    }

    @Override
    public void setAlpha(int alpha) {
        stroke.setAlpha(alpha);
        fill.setAlpha(alpha);
        text.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        stroke.setColorFilter(colorFilter);
        fill.setColorFilter(colorFilter);
        text.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
