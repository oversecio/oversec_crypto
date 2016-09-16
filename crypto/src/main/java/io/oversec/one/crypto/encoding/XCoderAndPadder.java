package io.oversec.one.crypto.encoding;

import io.oversec.one.crypto.encoding.pad.AbstractPadder;
import io.oversec.one.crypto.proto.Outer;


public class XCoderAndPadder {
    private final IXCoder mCoder;
    private final AbstractPadder mPadder;

    public XCoderAndPadder(IXCoder xCoder, AbstractPadder padder) {
        mCoder = xCoder;
        mPadder = padder;
    }

    public String getLabel() {
        return mCoder.getLabel(mPadder);
    }

    public String getExample() {
        return mCoder.getExample(mPadder);
    }

    public String getId() {
        return mCoder.getId() + (mPadder == null ? "" : ("__" + mPadder.getId()));
    }

    public AbstractPadder getPadder() {
        return mPadder;
    }

    public IXCoder getXcoder() {
        return mCoder;
    }

    public synchronized String encode(Outer.Msg msg, String srcText, boolean appendNewLines, String packagename) throws Exception {
        return mCoder.encode(msg, mPadder, srcText, appendNewLines, packagename);
    }

    public String getCoderId() {
        return mCoder == null ? null : mCoder.getId();
    }

    public String getPadderId() {
        return mPadder == null ? null : mPadder.getId();
    }
}
