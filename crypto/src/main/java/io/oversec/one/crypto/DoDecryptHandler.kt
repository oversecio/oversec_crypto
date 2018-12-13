package io.oversec.one.crypto

interface DoDecryptHandler {
    fun onResult(tdr: BaseDecryptResult?)
    fun onUserInteractionRequired()
}
