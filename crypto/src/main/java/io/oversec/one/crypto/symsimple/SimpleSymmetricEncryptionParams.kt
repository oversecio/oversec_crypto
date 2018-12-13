package io.oversec.one.crypto.symsimple

import android.content.Context
import io.oversec.one.crypto.EncryptionMethod
import io.oversec.one.crypto.symbase.BaseSymmetricEncryptionParams
import io.oversec.one.crypto.symbase.KeyCache

class SimpleSymmetricEncryptionParams protected constructor(coderId: String, padderId: String?) :
    BaseSymmetricEncryptionParams(EncryptionMethod.SIMPLESYM, coderId, padderId) {

    constructor(keyIds: List<Long>, coderId: String, padderId: String?) : this(coderId, padderId) {
        this.keyIds = keyIds
    }

    constructor(keyId: Long, coderId: String, padderId: String?) : this(coderId, padderId) {
        keyIds = ArrayList(this.keyIds).also { it.add(keyId) }
    }

    override fun isStillValid(ctx: Context): Boolean {
        val kc = KeyCache.getInstance(ctx)
        if (keyIds.any({!kc.hasKey(it)})) {
            return false;
        }
        return true
    }
}
