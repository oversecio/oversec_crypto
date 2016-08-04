package io.oversec.one.crypto;


public interface DoDecryptHandler {
    void onResult(int nodeId, BaseDecryptResult tdr);

    void onUserInteractionRequired();
}
