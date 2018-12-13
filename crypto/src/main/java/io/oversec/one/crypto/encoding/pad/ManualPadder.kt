package io.oversec.one.crypto.encoding.pad

import android.content.Context
import io.oversec.one.crypto.R

class ManualPadder internal constructor(ctx: Context) : AbstractPadder(ctx) {

    @Synchronized
    override fun pad(orig: String, encoded: StringBuffer) {
    }

    override fun reset() {}

    override val nextPaddingChar: Char
        get() = 0.toChar()

    override fun tail(): String {
        return ""
    }

    override val id
        get() = mCtx.getString(R.string.padder_manual)

    override val label
        get() = mCtx.getString(R.string.padder_manual)

    override val example
        get() = mCtx.getString(R.string.manual_padder_hint)

}
