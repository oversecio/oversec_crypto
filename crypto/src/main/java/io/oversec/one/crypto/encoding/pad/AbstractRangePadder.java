package io.oversec.one.crypto.encoding.pad;

import android.content.Context;

public abstract class AbstractRangePadder extends AbstractPadder {
    static final int MAX_TAIL_CHARS = 8;


    AbstractRangePadder(Context ctx) {
        super(ctx);
    }


    @Override
    public void reset() {

    }

    @Override
    public char getNextPaddingChar() {

        return (char) (getFirstChar() + Math.random() * (getLastChar() - getFirstChar()));
    }

    abstract char getFirstChar();

    abstract char getLastChar();

    @Override
    String tail() {
        int t = (int) (MAX_TAIL_CHARS * Math.random());
        StringBuilder sb = new StringBuilder(t);
        for (int i = 0; i < t; i++) {
            sb.append(getNextPaddingChar());
        }
        return sb.toString();
    }
}
