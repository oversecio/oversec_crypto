package io.oversec.one.crypto.encoding.pad

import android.content.Context

class GutenbergPadder(ctx: Context, private val mName: String, pattern: String) :
    AbstractPadder(ctx) {
    private val mPattern = if (pattern.endsWith(" ")) pattern else "$pattern "
    private var off: Int = 0

    override fun reset() {
        off = mPattern.indexOf(".", (mPattern.length * Math.random()).toInt()) + 1
    }

    override val nextPaddingChar: Char
        get() {
            if (off >= mPattern.length) {
                off = 0
            }
            val r = mPattern[off]
            off++
            if (off >= mPattern.length) {
                off = 0
            }
            return r
        }

    override val label
        get() = mName

    override fun tail(): String {
        val nextSpace = mPattern.indexOf(' ', off)
        return if (nextSpace == -1) {
            mPattern.substring(off)
        } else {
            mPattern.substring(off, nextSpace)
        }
    }

    override val id
        get() = mName

}
