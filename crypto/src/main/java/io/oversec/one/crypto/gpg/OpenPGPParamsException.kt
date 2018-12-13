package io.oversec.one.crypto.gpg

class OpenPGPParamsException(val error: Int) : Exception() {
    companion object {
        const val UNKNOWN_KEY_ID_FOR_ENCRYPTION = 1
    }
}
