package io.oversec.one.crypto.encoding.pad;

import android.content.Context;

public class GutenbergPadder extends AbstractPadder {

    private final String mName;
    private final String mPattern;
    private int off;

    public GutenbergPadder(Context ctx, String name, String pattern) {
        super(ctx);
        mName = name;
        mPattern = pattern.endsWith(" ") ? pattern : (pattern + " ");
    }

    @Override
    void reset() {
        off = mPattern.indexOf(".", (int) (mPattern.length() * Math.random())) + 1;
    }

    @Override
    char getNextPaddingChar() {
        if (off >= mPattern.length()) {
            off = 0;
        }
        char r = mPattern.charAt(off);
        off++;
        if (off >= mPattern.length()) {
            off = 0;
        }

        return r;
    }

    @Override
    public String getLabel() {
        return mName;
    }

    @Override
    String tail() {
        int nextSpace = mPattern.indexOf(' ', off);
        if (nextSpace == -1) {
            return mPattern.substring(off);
        } else {
            return mPattern.substring(off, nextSpace);
        }

    }

    @Override
    public String getId() {
        return mName;
    }


}
