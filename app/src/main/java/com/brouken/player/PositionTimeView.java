package com.brouken.player;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * Elapsed-time label. media3 formats positions as "00:11" / "01:02:03"; the
 * reference player uses "0:11" / "1:02:03", so drop the redundant leading zeros.
 */
public class PositionTimeView extends AppCompatTextView {

    public PositionTimeView(Context context) {
        super(context);
    }

    public PositionTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PositionTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(stripLeadingZeros(text), type);
    }

    private static CharSequence stripLeadingZeros(CharSequence text) {
        if (text == null) {
            return null;
        }
        final String value = text.toString();
        final int colon = value.indexOf(':');
        if (colon < 1) {
            return value;
        }
        int keep = 0;
        while (keep < colon - 1 && value.charAt(keep) == '0') {
            keep++;
        }
        return keep == 0 ? value : value.substring(keep);
    }
}
