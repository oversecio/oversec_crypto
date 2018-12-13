package io.oversec.one.crypto.encoding.pad

import android.content.Context

abstract class AbstractRangePadder internal constructor(ctx: Context) : AbstractPadder(ctx) {

    protected abstract val firstChar: Char
    protected abstract val lastChar: Char

    override fun reset() {}

    override val nextPaddingChar: Char
        get() = (firstChar.toDouble() + Math.random() * (lastChar - firstChar)).toChar()

    override fun tail(): String {
        val t = (MAX_TAIL_CHARS * Math.random()).toInt()
        val sb = StringBuilder(t)
        for (i in 0 until t) {
            sb.append(nextPaddingChar)
        }
        return sb.toString()
    }

    companion object {
        const val MAX_TAIL_CHARS = 8
    }
}
