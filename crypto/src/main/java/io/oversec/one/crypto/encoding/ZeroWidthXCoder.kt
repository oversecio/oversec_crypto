package io.oversec.one.crypto.encoding

import android.content.Context
import android.util.SparseIntArray
import io.oversec.one.common.CoreContract
import io.oversec.one.crypto.R
import io.oversec.one.crypto.encoding.pad.AbstractPadder
import io.oversec.one.crypto.proto.Outer
import roboguice.util.Ln
import java.io.IOException
import java.util.Arrays

class ZeroWidthXCoder(context: Context) : AbstractXCoder(context) {

    private val mCore = CoreContract.instance

    override val id: String
        get() = ID

    override val isTextOnly: Boolean
        get() = false

    override fun getLabel(padder: AbstractPadder?): String {
        return mCtx.getString(R.string.encoder_zerowidth) + (padder?.let { "("+it.label+")" } ?: "")
    }

    override fun getExample(padder: AbstractPadder?): String {
        return padder?.example ?: ""
    }

    @Throws(IOException::class)
    override fun encodeInternal(
        msg: Outer.Msg,
        padder: AbstractPadder?,
        packagename: String
    ): String {
        val spread = if (mCore.isDbSpreadInvisibleEncoding(packagename)) SPREAD else 0
        val smos = ExtendedPaneStringMapperOutputStream(MAPPING, padder, spread)
        smos.write(MAGIC_BYTES)
        msg.writeDelimitedTo(smos)
        smos.flush()
        return smos.encoded
    }

    @Throws(IOException::class)
    override fun decode(encText: String): Outer.Msg? {
        if (encText.length < MAGIC_BYTES_ZEROWIDTH.length) { // small optimization, check we have at least some bytes
            return null
        }

        //try to find magic
        val p = encText.indexOf(MAGIC_BYTES_ZEROWIDTH)
        if (p < 0) {
            return null
        }

        try {
            val smis = ExtendedPaneStringMapperInputStream(encText.substring(p), REVERSE_MAPPING)
            val buf = ByteArray(MAGIC_BYTES.size)

            smis.read(buf)
            return if (!Arrays.equals(buf, MAGIC_BYTES)) {
                null
            } else Outer.Msg.parseDelimitedFrom(smis)
        } catch (e: ExtendedPaneStringMapperInputStream.UnmappedCodepointException) {
            return if (e.offset <= 1) { //this simply means that the string is not encoded by us
                null
            } else {
                throw e
            }
        } catch (e: StringIndexOutOfBoundsException) {
            throw IOException(e)
        }
    }

    companion object {
        const val ID = "zwidthbmp"

        private const val MAGIC = "OSC"
        private val MAGIC_BYTES = MAGIC.toByteArray()
        private const val SPREAD =
            30 // Orca / Instagram fail at more than 35 consecutive inivisvle chars, let's play it safe and use 30

        private val MAPPING = Array<CharArray>(256) {
            if (it <= 0xF) {
                Character.toChars(0xFE00 + it)
            } else {
                Character.toChars(0xE0100 + it - 0x10)
            }
        }
        private val REVERSE_MAPPING = SparseIntArray()
        private var MAGIC_BYTES_ZEROWIDTH = calcMagicBytes()

        init {
            for (i in MAPPING.indices) {
                when {
                    MAPPING[i].size == 1 -> REVERSE_MAPPING.put(MAPPING[i][0].toInt(), i)
                    MAPPING[i].size == 2 -> REVERSE_MAPPING.put(Character.toCodePoint(MAPPING[i][0], MAPPING[i][1]), i)
                    else -> throw IllegalArgumentException()
                }
            }
        }

        private fun calcMagicBytes(): String {
            val os = ExtendedPaneStringMapperOutputStream(MAPPING, null, 0)
            os.write(MAGIC_BYTES)
            return os.encoded
        }

        fun stripInvisible(s: String): String {
            var off = 0
            return try {
                while (true) {
                    val cp = s.codePointAt(off)
                    val res = REVERSE_MAPPING.get(cp, Integer.MIN_VALUE)
                    if (res == Integer.MIN_VALUE) {
                        break
                    }
                    off = s.offsetByCodePoints(off, 1)
                }
                s.substring(off)
            } catch (ex: IndexOutOfBoundsException) {
                Ln.e(ex, "problem stripping [%s]", s)
                ""
            }
        }
    }
}
