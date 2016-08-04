package io.oversec.one.crypto.encoding;

import java.io.IOException;
import java.io.OutputStream;

public class ExtendedPaneStringMapperOutputStream extends OutputStream {

    private final StringBuilder mBuf;
    private final char[][] mMapping;


    public ExtendedPaneStringMapperOutputStream(char[][] mapping) {
        mBuf = new StringBuilder();
        mMapping = mapping;
    }


    @Override
    public void write(int oneByte) throws IOException {
        mBuf.append(mMapping[oneByte & 0xFF]);
    }

    public String getEncoded() {
        return mBuf.toString();
    }
}
