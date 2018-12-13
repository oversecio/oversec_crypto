package io.oversec.one.crypto.symbase

import io.oversec.one.crypto.AbstractEncryptionParams
import io.oversec.one.crypto.EncryptionMethod
import java.util.ArrayList

abstract class BaseSymmetricEncryptionParams protected constructor(
    method: EncryptionMethod,
    coderId: String,
    padderId: String?
) : AbstractEncryptionParams(method, coderId, padderId) {
    var keyIds: List<Long> = ArrayList()
        protected set


}
