package io.oversec.one.crypto.gpg;

import org.openintents.openpgp.OpenPgpError;

public class OpenPGPErrorException extends Exception {
    private final OpenPgpError mError;

    public OpenPGPErrorException(OpenPgpError error) {
        mError = error;
    }

    public OpenPgpError getError() {
        return mError;
    }
}
