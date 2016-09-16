package io.oversec.one.crypto.encoding.pad;

import android.content.Context;
import io.oversec.one.crypto.R;

public class ManualPadder extends AbstractPadder {
    ManualPadder(Context ctx) {
        super(ctx);
    }

    @Override
    public synchronized void pad(String orig, StringBuffer encoded) {
    }

    @Override
    public void reset() {
    }

    @Override
    public char getNextPaddingChar() {
        return 0;
    }

    @Override
    String tail() {
        return "";
    }

    @Override
    public String getId() {
        return mCtx.getString(R.string.padder_manual);
    }

    @Override
    public String getLabel() {
        return mCtx.getString(R.string.padder_manual);
    }


    @Override
    public String getExample() {
        return mCtx.getString(R.string.manual_padder_hint);
    }
}
