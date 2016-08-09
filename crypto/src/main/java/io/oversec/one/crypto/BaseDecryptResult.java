package io.oversec.one.crypto;


import com.google.protobuf.InvalidProtocolBufferException;
import io.oversec.one.crypto.proto.Inner;

import java.io.UnsupportedEncodingException;

public class BaseDecryptResult {


    public enum Result {
        OK, RETRY, FAILED_PERMANENTLY
    }

    public enum DecryptError {
        SYM_UNSUPPORTED_CIPHER, SYM_NO_MATCHING_KEY, SYM_DECRYPT_FAILED, PGP_ERROR, PROTO_ERROR, NO_HANDLER
    }


    protected EncryptionMethod mEncryptionMethod;


    protected byte[] mDecryptedRawData;
    protected Result mResult;
    protected DecryptError mError;

    protected String mErrorMessage;


    public BaseDecryptResult(EncryptionMethod encryptionMethod, byte[] rawData) {
        mEncryptionMethod = encryptionMethod;
        mDecryptedRawData = rawData;
        mResult = Result.OK;
    }

    public BaseDecryptResult(EncryptionMethod method, DecryptError error) {
        this(method, error, null);
    }


    public BaseDecryptResult(EncryptionMethod method, DecryptError error, String errorMessage) {
        mEncryptionMethod = method;
        mError = error;
        mErrorMessage = errorMessage;
        mResult = Result.RETRY;
        switch (error) {
            case SYM_DECRYPT_FAILED:
                mResult = Result.RETRY;
                break;
            case SYM_NO_MATCHING_KEY:
                mResult = Result.RETRY;
                break;
            case PGP_ERROR:
                new Exception().printStackTrace();
                mResult = Result.RETRY; //TODO: should be failed permanently or?
                break;
            case PROTO_ERROR:
                mResult = Result.FAILED_PERMANENTLY;
                break;
            case NO_HANDLER:
                mResult = Result.RETRY;
                break;
            case SYM_UNSUPPORTED_CIPHER:
                mResult = Result.RETRY;
                break;

        }
    }


    public boolean isOk() {
        return mResult == Result.OK;
    }

    public boolean isRetry() {
        return mResult == Result.RETRY;
    }

    public boolean isOversecNode() {
        return mResult == Result.OK
                || mError == DecryptError.SYM_NO_MATCHING_KEY
                || mError == DecryptError.SYM_UNSUPPORTED_CIPHER
                || mError == DecryptError.SYM_DECRYPT_FAILED
                || mError == DecryptError.NO_HANDLER
                || mError == DecryptError.PGP_ERROR;
    }


    public EncryptionMethod getEncryptionMethod() {
        return mEncryptionMethod;
    }


    public String getErrorMessage() {
        return mErrorMessage;
    }


    public Inner.InnerData getDecryptedDataAsInnerData() throws InvalidProtocolBufferException {
        return Inner.InnerData.parseFrom(mDecryptedRawData);
    }
    public String getDecryptedDataAsUtf8String() throws UnsupportedEncodingException {
        return new String(mDecryptedRawData,"UTF-8");
    }

    public Result getResult() {
        return mResult;
    }

    public DecryptError getError() {
        return mError;
    }

    @Override
    public String toString() {
        return "BaseDecryptResult{" +
                "decryptedData='" + mDecryptedRawData + '\'' +
                ", result=" + mResult +
                ", error=" + mError +
                '}';
    }


}