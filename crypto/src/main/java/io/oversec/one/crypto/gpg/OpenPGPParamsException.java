package io.oversec.one.crypto.gpg;

public class OpenPGPParamsException extends Exception {
    public static final int UNKNOWN_KEY_ID_FOR_ENCRYPTION = 1;
    private final int mError;

    public OpenPGPParamsException(int error) {
        mError = error;
    }

    public int getError() {
        return mError;
    }
}
