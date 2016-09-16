package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Created by yao on 16/09/16.
 */

public class UntouchableSeekBar extends SeekBar {
    public UntouchableSeekBar(Context context) {
        super(context);
    }

    public UntouchableSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UntouchableSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
