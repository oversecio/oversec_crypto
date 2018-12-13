package io.oversec.one.crypto.gpg

import org.spongycastle.bcpg.CRC24
import org.spongycastle.bcpg.HashAlgorithmTags
import org.spongycastle.bcpg.PacketTags
import org.spongycastle.util.Strings
import java.io.IOException
import java.io.OutputStream
import java.util.Hashtable

/**
 * Output stream that writes data in ASCII Armored format.
 *
 * Original source here:
 *
 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/bcpg/ArmoredOutputStream.java
 *
 * Didn't want to fork/build bouncy/spongycastle only for that change, so took the sources of only that class and
 * patched it to not include a version header.
 */
class OversecAsciiArmoredOutputStream
/**
 * Constructs an armored output stream with [default headers][.resetHeaders].
 *
 * @param out the OutputStream to wrap.
 */
    (
    private var out: OutputStream
) : OutputStream() {
    private var buf = IntArray(3)
    private var bufPtr = 0
    private var crc = CRC24()
    private var chunkCount = 0
    private var lastb: Int = 0

    private var start = true
    private var clearText = false
    private var newLine = false

    private var nl: String? = Strings.lineSeparator()
    private var type: String? = null
    private var version = "BCPG v@RELEASE_NAME@"
    private var headers = Hashtable<String,String>()

    /**
     * encode the input data producing a base 64 encoded byte array.
     */
    @Throws(IOException::class)
    private fun encode(
        out: OutputStream,
        data: IntArray,
        len: Int
    ) {
        val d1: Int
        val d2: Int
        val d3: Int

        when (len) {
            0        /* nothing left to do */ -> {
            }
            1 -> {
                d1 = data[0]

                out.write(encodingTable[d1.ushr(2) and 0x3f].toInt())
                out.write(encodingTable[d1 shl 4 and 0x3f].toInt())
                out.write('='.toInt())
                out.write('='.toInt())
            }
            2 -> {
                d1 = data[0]
                d2 = data[1]

                out.write(encodingTable[d1.ushr(2) and 0x3f].toInt())
                out.write(encodingTable[d1 shl 4 or d2.ushr(4) and 0x3f].toInt())
                out.write(encodingTable[d2 shl 2 and 0x3f].toInt())
                out.write('='.toInt())
            }
            3 -> {
                d1 = data[0]
                d2 = data[1]
                d3 = data[2]

                out.write(encodingTable[d1.ushr(2) and 0x3f].toInt())
                out.write(encodingTable[d1 shl 4 or d2.ushr(4) and 0x3f].toInt())
                out.write(encodingTable[d2 shl 2 or d3.ushr(6) and 0x3f].toInt())
                out.write(encodingTable[d3 and 0x3f].toInt())
            }
            else -> throw IOException("unknown length in encode")
        }
    }

    init {

        if (nl == null) {
            nl = "\r\n"
        }

        resetHeaders()
    }

    /**
     * Constructs an armored output stream with default and custom headers.
     *
     * @param out the OutputStream to wrap.
     * @param headers additional headers that add to or override the [default][.resetHeaders].
     */
    constructor(
        out: OutputStream,
        headers: Hashtable<String, String>
    ) : this(out) {

        val e = headers.keys()

        while (e.hasMoreElements()) {
            val key = e.nextElement()

            this.headers.put(key, headers[key])
        }
    }

    /**
     * Set an additional header entry.
     *
     * @param name the name of the header entry.
     * @param value the value of the header entry.
     */
    fun setHeader(
        name: String,
        value: String
    ) {
        this.headers.put(name, value)
    }

    /**
     * Reset the headers
     */
    private fun resetHeaders() {
        headers.clear()
        //headers.put("Version", version);
    }

    /**
     * Start a clear text signed message.
     * @param hashAlgorithm
     */
    @Throws(IOException::class)
    fun beginClearText(
        hashAlgorithm: Int
    ) {
        val hash = when (hashAlgorithm) {
            HashAlgorithmTags.SHA1 -> "SHA1"
            HashAlgorithmTags.SHA256 -> "SHA256"
            HashAlgorithmTags.SHA384 -> "SHA384"
            HashAlgorithmTags.SHA512 -> "SHA512"
            HashAlgorithmTags.MD2 -> "MD2"
            HashAlgorithmTags.MD5 -> "MD5"
            HashAlgorithmTags.RIPEMD160 -> "RIPEMD160"
            else -> throw IOException("unknown hash algorithm tag in beginClearText: $hashAlgorithm")
        }

        val armorHdr = "-----BEGIN PGP SIGNED MESSAGE-----" + nl!!
        val hdrs = "Hash: $hash$nl$nl"

        for (i in 0 until armorHdr.length) {
            out.write(armorHdr[i].toInt())
        }

        for (i in 0 until hdrs.length) {
            out.write(hdrs[i].toInt())
        }

        clearText = true
        newLine = true
        lastb = 0
    }

    fun endClearText() {
        clearText = false
    }

    @Throws(IOException::class)
    private fun writeHeaderEntry(
        name: String,
        value: String
    ) {
        for (i in 0 until name.length) {
            out.write(name[i].toInt())
        }

        out.write(':'.toInt())
        out.write(' '.toInt())

        for (i in 0 until value.length) {
            out.write(value[i].toInt())
        }

        for (i in 0 until nl!!.length) {
            out.write(nl!![i].toInt())
        }
    }

    @Throws(IOException::class)
    override fun write(
        b: Int
    ) {
        if (clearText) {
            out.write(b)

            if (newLine) {
                if (!(b == '\n'.toInt() && lastb == '\r'.toInt())) {
                    newLine = false
                }
                if (b == '-'.toInt()) {
                    out.write(' '.toInt())
                    out.write('-'.toInt())      // dash escape
                }
            }
            if (b == '\r'.toInt() || b == '\n'.toInt() && lastb != '\r'.toInt()) {
                newLine = true
            }
            lastb = b
            return
        }

        if (start) {
            val newPacket = b and 0x40 != 0

            val tag = if (newPacket) {
                b and 0x3f
            } else {
                b and 0x3f shr 2
            }

            type = when (tag) {
                PacketTags.PUBLIC_KEY -> "PUBLIC KEY BLOCK"
                PacketTags.SECRET_KEY -> "PRIVATE KEY BLOCK"
                PacketTags.SIGNATURE -> "SIGNATURE"
                else -> "MESSAGE"
            }

            for (i in 0 until headerStart.length) {
                out.write(headerStart[i].toInt())
            }

            for (i in 0 until type!!.length) {
                out.write(type!![i].toInt())
            }

            for (i in 0 until headerTail.length) {
                out.write(headerTail[i].toInt())
            }

            for (i in 0 until nl!!.length) {
                out.write(nl!![i].toInt())
            }

            //writeHeaderEntry("Version", (String)headers.get("Version"));

            val e = headers.keys()
            while (e.hasMoreElements()) {
                val key = e.nextElement() as String

                if (key != "Version") {
                    writeHeaderEntry(key, headers.get(key) as String)
                }
            }

            for (i in 0 until nl!!.length) {
                out.write(nl!![i].toInt())
            }

            start = false
        }

        if (bufPtr == 3) {
            encode(out, buf, bufPtr)
            bufPtr = 0
            if (++chunkCount and 0xf == 0) {
                for (i in 0 until nl!!.length) {
                    out.write(nl!![i].toInt())
                }
            }
        }

        crc.update(b)
        buf[bufPtr++] = b and 0xff
    }

    @Throws(IOException::class)
    override fun flush() {
    }

    /**
     * **Note**: close() does not close the underlying stream. So it is possible to write
     * multiple objects using armoring to a single stream.
     */
    @Throws(IOException::class)
    override fun close() {
        if (type != null) {
            encode(out, buf, bufPtr)

            for (i in 0 until nl!!.length) {
                out.write(nl!![i].toInt())
            }
            out.write('='.toInt())

            val crcV = crc.value

            buf[0] = crcV shr 16 and 0xff
            buf[1] = crcV shr 8 and 0xff
            buf[2] = crcV and 0xff

            encode(out, buf, 3)

            for (i in 0 until nl!!.length) {
                out.write(nl!![i].toInt())
            }

            for (i in 0 until footerStart.length) {
                out.write(footerStart[i].toInt())
            }

            for (i in 0 until type!!.length) {
                out.write(type!![i].toInt())
            }

            for (i in 0 until footerTail.length) {
                out.write(footerTail[i].toInt())
            }

            for (i in 0 until nl!!.length) {
                out.write(nl!![i].toInt())
            }

            out.flush()

            type = null
            start = true
        }
    }

    companion object {
        private val encodingTable = byteArrayOf(
            'A'.toByte(),
            'B'.toByte(),
            'C'.toByte(),
            'D'.toByte(),
            'E'.toByte(),
            'F'.toByte(),
            'G'.toByte(),
            'H'.toByte(),
            'I'.toByte(),
            'J'.toByte(),
            'K'.toByte(),
            'L'.toByte(),
            'M'.toByte(),
            'N'.toByte(),
            'O'.toByte(),
            'P'.toByte(),
            'Q'.toByte(),
            'R'.toByte(),
            'S'.toByte(),
            'T'.toByte(),
            'U'.toByte(),
            'V'.toByte(),
            'W'.toByte(),
            'X'.toByte(),
            'Y'.toByte(),
            'Z'.toByte(),
            'a'.toByte(),
            'b'.toByte(),
            'c'.toByte(),
            'd'.toByte(),
            'e'.toByte(),
            'f'.toByte(),
            'g'.toByte(),
            'h'.toByte(),
            'i'.toByte(),
            'j'.toByte(),
            'k'.toByte(),
            'l'.toByte(),
            'm'.toByte(),
            'n'.toByte(),
            'o'.toByte(),
            'p'.toByte(),
            'q'.toByte(),
            'r'.toByte(),
            's'.toByte(),
            't'.toByte(),
            'u'.toByte(),
            'v'.toByte(),
            'w'.toByte(),
            'x'.toByte(),
            'y'.toByte(),
            'z'.toByte(),
            '0'.toByte(),
            '1'.toByte(),
            '2'.toByte(),
            '3'.toByte(),
            '4'.toByte(),
            '5'.toByte(),
            '6'.toByte(),
            '7'.toByte(),
            '8'.toByte(),
            '9'.toByte(),
            '+'.toByte(),
            '/'.toByte()
        )
        const val headerStart = "-----BEGIN PGP "
        const val headerTail = "-----"
        const val footerStart = "-----END PGP "
        const val footerTail = "-----"
    }
}