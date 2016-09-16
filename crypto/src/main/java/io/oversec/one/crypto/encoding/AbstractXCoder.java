package io.oversec.one.crypto.encoding;


import android.content.Context;
import io.oversec.one.crypto.encoding.pad.AbstractPadder;
import io.oversec.one.crypto.proto.Outer;

import java.io.IOException;

public abstract class AbstractXCoder implements IXCoder {


    protected final Context mCtx;

    protected abstract String encodeInternal(Outer.Msg msg, AbstractPadder padder, String packagename) throws IOException;


    // public abstract String getEncodedTextOnly(String encText) throws IOException;

    public AbstractXCoder(Context ctx) {
        mCtx = ctx;
    }

    @Override
    public String encode(Outer.Msg msg, AbstractPadder padder, String plainTextForWidthCalculation, boolean appendNewLines, String packagename) throws IOException {
        if (padder!=null) {
            padder.reset();
        }
        int nn = appendNewLines ? countNewLines(plainTextForWidthCalculation) : 0;
        String internal = encodeInternal(msg,padder,packagename);
        StringBuffer r = new StringBuffer(internal);
        //prependPrefix(r);
        if (nn > 0) {
            appendNewLines(r, nn);
        }
        if (plainTextForWidthCalculation != null && padder != null) {
            padder.pad(plainTextForWidthCalculation, r);
        }
        return r.toString();
    }


    private int countNewLines(String s) {
        int r = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                r++;
            }
        }
        return r;
    }

    private void appendNewLines(StringBuffer sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append('\n');
        }
    }


}
