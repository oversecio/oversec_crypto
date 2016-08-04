package io.oversec.one.crypto.encoding.pad;

import android.content.Context;
import io.oversec.one.crypto.R;

public class OversecPadder extends AbstractPadder {

    private int off;
    final String pattern = " Oversec";

    OversecPadder(Context ctx) {
        super(ctx);
    }

    @Override
    void reset() {
        off = 0;
    }

    @Override
    char getNextPaddingChar() {
        char r = pattern.charAt(off);
        off++;
        if (off >= pattern.length()) {
            off = 0;
        }

        return r;
    }


    @Override
    public String getLabel() {
        return mCtx.getString(R.string.padder_oversec);
    }

    @Override
    String tail() {
        return off == 0 ? "" : pattern.substring(off);
    }

    @Override
    public String getId() {
        return mCtx.getString(R.string.padder_oversec);
    }
}
