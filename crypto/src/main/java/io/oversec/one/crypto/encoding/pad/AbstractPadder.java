package io.oversec.one.crypto.encoding.pad;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;

public abstract class AbstractPadder {
    private static final int NUM_EXAMPLE_CHARS = 40;
    public static final float TEXT_SIZE_FOR_WIDTH_CALCULATION = 30;
    protected final Context mCtx;
    protected Paint p = new Paint();
    protected Rect bounds = new Rect();

    AbstractPadder(Context ctx) {
        mCtx = ctx;
        p.setTextSize(TEXT_SIZE_FOR_WIDTH_CALCULATION);
    }

    public synchronized void pad(String orig, StringBuffer encoded) {
        reset();
        p.getTextBounds(orig, 0, orig.length(), bounds);
        int w = bounds.width();
        int initialEncodedLength = encoded.length();//-1   :0; //skip the initial encoding less 1 for nonspacing coder

        p.getTextBounds(encoded.toString(), initialEncodedLength, encoded.length(), bounds);
        int we = bounds.width();
        while (we < w) {
            encoded.append(getNextPaddingChar());
            p.getTextBounds(encoded.toString(), initialEncodedLength, encoded.length(), bounds);
            we = bounds.width();
        }

        String tail = tail();
        encoded.append(tail);
    }


    abstract void reset();

    abstract char getNextPaddingChar();

    public String getExample() {
        reset();
        StringBuilder sexample = new StringBuilder(NUM_EXAMPLE_CHARS);
        for (int i = 0; i < NUM_EXAMPLE_CHARS; i++) {
            sexample.append(getNextPaddingChar());
        }
        sexample.append(tail());
        return sexample.toString();
    }

    public abstract String getLabel();

    abstract String tail();

    public abstract String getId();


}
