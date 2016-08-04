package io.oversec.one.crypto.symbase;

public interface OversecKeyCacheListener {

    void onFinishedCachingKey(long keyId);

    void onStartedCachingKey(long keyId);

}
