package com.brouken.player;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Canvas-drawn control bar glyphs. Vector paths for arcs are fragile across
 * renderers, so the speedometer and the aspect-ratio frame are drawn directly
 * to keep the strokes exactly where they belong at every size.
 */
public class PlayerIconDrawable extends Drawable {

    public static final int SPEED = 0;
    public static final int ASPECT = 1;

    private final int type;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float density = 1f;

    public PlayerIconDrawable(int type, float density) {
        this.type = type;
        this.density = density;
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
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

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        if (type == SPEED) {
            oval.set(1.8f, 3.5f, 22.2f, 20.5f);
            paint.setStrokeWidth(1.9f);
            canvas.drawArc(oval, 18f, 306f, false, paint);

            paint.setStrokeWidth(2.1f);
            canvas.drawLine(12f, 12f, 19.2f, 5.5f, paint);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(12f, 12f, 1.15f, paint);
        } else {
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeWidth(1.7f);
            canvas.drawRoundRect(1.3f, 3.5f, 22.7f, 20.5f, 3.2f, 3.2f, paint);

            paint.setStrokeWidth(2.1f);
            canvas.drawLine(4.4f, 7.7f, 9.6f, 7.7f, paint);
            canvas.drawLine(4.4f, 7.7f, 4.4f, 12.7f, paint);
            canvas.drawLine(19.6f, 16.3f, 14.4f, 16.3f, paint);
            canvas.drawLine(19.6f, 16.3f, 19.6f, 11.3f, paint);
        }

        canvas.restoreToCount(save);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
