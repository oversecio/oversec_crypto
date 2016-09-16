package io.oversec.one.crypto.encoding;

import io.oversec.one.crypto.encoding.pad.AbstractPadder;

import java.io.IOException;
import java.io.OutputStream;

public class ExtendedPaneStringMapperOutputStream extends OutputStream {

    private final StringBuilder mBuf;
    private final char[][] mMapping;
    private final int mSpread;
    private final AbstractPadder mPadder;

    private int mCntInvisibleChars = 0;

    public ExtendedPaneStringMapperOutputStream(char[][] mapping, AbstractPadder padder, int spread) {
        mBuf = new StringBuilder();
        mMapping = mapping;
        mSpread = spread;
        mPadder = padder;
    }


    @Override
    public void write(int oneByte) throws IOException {
        mBuf.append(mMapping[oneByte & 0xFF]);
        mCntInvisibleChars++;
        if (mPadder!=null && mSpread>0 && mCntInvisibleChars>mSpread) {
            mBuf.append(mPadder.getNextPaddingChar());
            mCntInvisibleChars = 0;
        }
    }

    public String getEncoded() {
        return mBuf.toString();
    }
}
