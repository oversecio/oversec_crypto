package io.oversec.one.crypto.encoding;

import android.util.SparseIntArray;

import java.io.IOException;
import java.io.InputStream;

public class ExtendedPaneStringMapperInputStream extends InputStream {

    private final String mSrc;
    private final SparseIntArray mReverseMapping;
    private int mOff = 0;

    public ExtendedPaneStringMapperInputStream(String src, SparseIntArray reverseMapping) {
        mSrc = src;
        mReverseMapping = reverseMapping;
    }

    @Override
    public int read() throws IOException {
        try {
            int cp = mSrc.codePointAt(mOff);
            int res = mReverseMapping.get(cp, Integer.MIN_VALUE);
            if (res == Integer.MIN_VALUE) {

                    //this is probably a fill character, just ignore it
                    mOff = mSrc.offsetByCodePoints(mOff, 1);
                    res = mReverseMapping.get(cp, Integer.MIN_VALUE);
                    if (res == Integer.MIN_VALUE) {
                        throw new UnmappedCodepointException(cp, mOff);
                    }


            }
            mOff = mSrc.offsetByCodePoints(mOff, 1);
            return res;
        } catch (StringIndexOutOfBoundsException ex) {
            return -1;
        }
    }


    public int getOff() {
        return mOff;
    }

    public static class UnmappedCodepointException extends IOException {
        private final int mCodepoint;
        private final int mOffset;

        public UnmappedCodepointException(int codepoint, int offset) {
            mCodepoint = codepoint;
            mOffset = offset;
        }

        public int getOffset() {
            return mOffset;
        }
    }
}
