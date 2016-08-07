package io.oversec.one.crypto;


public interface DoDecryptHandler {
    void onResult( BaseDecryptResult tdr);

    void onUserInteractionRequired();
}
