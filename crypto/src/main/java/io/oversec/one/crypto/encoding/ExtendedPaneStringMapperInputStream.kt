package io.oversec.one.crypto.encoding

import android.util.SparseIntArray
import java.io.IOException
import java.io.InputStream

class ExtendedPaneStringMapperInputStream(
    private val src: String,
    private val reverseMapping: SparseIntArray
) : InputStream() {

    private var off = 0

    @Throws(IOException::class)
    override fun read(): Int {
        try {
            val cp = src.codePointAt(off)
            var res = reverseMapping.get(cp, Integer.MIN_VALUE)
            if (res == Integer.MIN_VALUE) {

                //this is probably a fill character, just ignore it
                off = src.offsetByCodePoints(off, 1)
                res = reverseMapping.get(cp, Integer.MIN_VALUE)
                if (res == Integer.MIN_VALUE) {
                    throw UnmappedCodepointException(cp, off)
                }

            }
            off = src.offsetByCodePoints(off, 1)
            return res
        } catch (ex: StringIndexOutOfBoundsException) {
            return -1
        }

    }

    class UnmappedCodepointException(private val mCodepoint: Int, val offset: Int) : IOException()
}
