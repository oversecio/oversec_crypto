package io.oversec.one.crypto.encoding

import io.oversec.one.crypto.encoding.pad.AbstractPadder
import java.io.IOException
import java.io.OutputStream

class ExtendedPaneStringMapperOutputStream(
    private val mapping: Array<CharArray>,
    private val padder: AbstractPadder?,
    private val spread: Int
) : OutputStream() {

    private val buf = StringBuilder()
    private var cntInvisibleChars = 0

    val encoded: String
        get() = buf.toString()

    @Throws(IOException::class)
    override fun write(oneByte: Int) {
        buf.append(mapping[oneByte and 0xFF])
        cntInvisibleChars++
        if (padder != null && spread > 0 && cntInvisibleChars > spread) {
            buf.append(padder.nextPaddingChar)
            cntInvisibleChars = 0
        }
    }
}
