package io.oversec.one.crypto.symbase

interface OversecKeyCacheListener {

    fun onFinishedCachingKey(keyId: Long)

    fun onStartedCachingKey(keyId: Long)

}
