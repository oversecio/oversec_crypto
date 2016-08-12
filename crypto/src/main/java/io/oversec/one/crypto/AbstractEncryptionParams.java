package io.oversec.one.crypto;

import android.content.Context;
import io.oversec.one.crypto.encoding.IXCoder;
import io.oversec.one.crypto.encoding.pad.AbstractPadder;


public abstract class AbstractEncryptionParams {

    private String mCoderId;
    private String mPadderId;

    private EncryptionMethod mEncryptionMethod;

    protected AbstractEncryptionParams(EncryptionMethod method, String coderId, String padderId) {
        mEncryptionMethod = method;
        mCoderId = coderId;
        mPadderId = padderId;
    }

    public EncryptionMethod getEncryptionMethod() {
        return mEncryptionMethod;
    }


    public String getCoderId() {
        return mCoderId;
    }

    public String getPadderId() {
        return mPadderId;
    }

    public void setXcoderAndPadder(IXCoder xcoder, AbstractPadder padder) {
        mCoderId = xcoder.getId();
        mPadderId = padder == null ? null : padder.getId();
    }

    public void setXcoderAndPadderIds(String xcoderId, String padderId) {
        mCoderId =  xcoderId;
        mPadderId = padderId;
    }


    public abstract boolean isStillValid(Context ctx);
}
