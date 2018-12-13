package io.oversec.one.crypto.encoding

import android.content.Context
import android.util.Base64
import com.google.protobuf.InvalidProtocolBufferException
import io.oversec.one.crypto.R
import io.oversec.one.crypto.encoding.pad.AbstractPadder
import io.oversec.one.crypto.proto.Outer
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

class Base64XCoder(ctx: Context) : AbstractXCoder(ctx) {

    override val id: String
        get() = ID

    override val isTextOnly: Boolean
        get() = false

    override fun encodeInternal(
        msg: Outer.Msg,
        ignore: AbstractPadder?,
        packagename: String
    ): String {
        val d = msg.toByteArray()
        val padded = ByteArray(d.size + MAGIC_BYTES.size)
        System.arraycopy(MAGIC_BYTES, 0, padded, 0, MAGIC_BYTES.size)
        System.arraycopy(d, 0, padded, MAGIC_BYTES.size, d.size)
        return Base64.encodeToString(padded, FLAGS)
    }

    @Throws(InvalidProtocolBufferException::class, IllegalArgumentException::class)
    override fun decode(encText: String): Outer.Msg? {
        if (encText.length < MAGIC_BYTES_BASE64.length) {
            return null
        }

        for (i in 0 until MAGIC_BYTES_BASE64.length) {
            if (encText[i] != MAGIC_BYTES_BASE64[i]) {
                return null
            }
        }

        val buf = Base64.decode(encText, FLAGS)

        val res: Outer.Msg
        try {
            res = Outer.Msg.parseFrom(
                ByteArrayInputStream(
                    buf,
                    MAGIC_BYTES.size,
                    buf.size - MAGIC_BYTES.size
                )
            )
        } catch (e: IOException) {
            throw InvalidProtocolBufferException(e)
        }

        return res
    }

    override fun getLabel(padder: AbstractPadder?): String {
        return mCtx.getString(R.string.encoder_base64)
    }

    override fun getExample(padder: AbstractPadder?): String {
        return try {
            Base64.encodeToString("some example text".toByteArray(charset("UTF-8")), FLAGS)
        } catch (e: UnsupportedEncodingException) {
            ""
        }
    }

    companion object {
        const val ID = "base64"
        internal const val FLAGS = Base64.NO_WRAP + Base64.NO_PADDING
        private val MAGIC_BYTES = "OSC".toByteArray()
        private val MAGIC_BYTES_BASE64 = Base64.encodeToString(MAGIC_BYTES, FLAGS)
    }
}
