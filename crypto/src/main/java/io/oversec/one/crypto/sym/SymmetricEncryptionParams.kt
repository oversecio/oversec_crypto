package io.oversec.one.crypto.sym

import android.content.Context
import io.oversec.one.crypto.EncryptionMethod
import io.oversec.one.crypto.symbase.BaseSymmetricEncryptionParams

class SymmetricEncryptionParams protected constructor(coderId: String, padderId: String?) :
    BaseSymmetricEncryptionParams(EncryptionMethod.SYM, coderId, padderId) {

    constructor(keyIds: List<Long>, coderId: String, padderId: String?) : this(coderId, padderId) {
        this.keyIds = keyIds
    }

    constructor(keyId: Long?, coderId: String, padderId: String?) : this(coderId, padderId) {
        requireNotNull(keyId)
        keyIds = ArrayList(this.keyIds).also { it.add(keyId) }
    }

    override fun isStillValid(ctx: Context): Boolean {
        val ks = OversecKeystore2.getInstance(ctx)
        if (keyIds.any({!ks.hasKey(it)})) {
            return false;
        }
        return true
    }
}
