package io.oversec.one.crypto

import android.content.Context
import io.oversec.one.crypto.encoding.IXCoder
import io.oversec.one.crypto.encoding.pad.AbstractPadder


abstract class AbstractEncryptionParams protected constructor(
    val encryptionMethod: EncryptionMethod,
    coderId: String,
    padderId: String?
) {

    var coderId: String = coderId
        private set
    var padderId: String? = padderId
        private set


    fun setXcoderAndPadder(xcoder: IXCoder, padder: AbstractPadder?) {
        coderId = xcoder.id
        padderId = padder?.id
    }

    fun setXcoderAndPadderIds(xcoderId: String, padderId: String) {
        coderId = xcoderId
        this.padderId = padderId
    }

    abstract fun isStillValid(ctx: Context): Boolean
}
