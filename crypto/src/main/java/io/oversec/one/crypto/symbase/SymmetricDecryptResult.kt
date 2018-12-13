package io.oversec.one.crypto.symbase

import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.EncryptionMethod


class SymmetricDecryptResult : BaseDecryptResult {

    var symmetricKeyId: Long? = null
        private set

    @JvmOverloads
    constructor(
        method: EncryptionMethod,
        error: BaseDecryptResult.DecryptError,
        keyId: Long? = null
    ) : super(method, error) {
        this.symmetricKeyId = keyId
    }


    constructor(method: EncryptionMethod, rawInnerData: ByteArray, keyId: Long?) : super(
        method,
        rawInnerData
    ) {
        symmetricKeyId = keyId
    }


}
