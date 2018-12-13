package io.oversec.one.crypto.encoding.pad

import android.content.Context
import io.oversec.one.crypto.R

class OversecPadder internal constructor(ctx: Context) : AbstractPadder(ctx) {

    private var off: Int = 0
    private val pattern = " Oversec"

    override fun reset() {
        off = 0
    }

    override val nextPaddingChar: Char
        get() {
            val r = pattern[off]
            off++
            if (off >= pattern.length) {
                off = 0
            }

            return r
        }

    override val label
        get() = mCtx.getString(R.string.padder_oversec)


    override fun tail(): String {
        return if (off == 0) "" else pattern.substring(off)
    }

    override val id
        get() = mCtx.getString(R.string.padder_oversec)

}
