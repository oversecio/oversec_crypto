package io.oversec.one.crypto.encoding

import android.content.Context
import io.oversec.one.crypto.R
import io.oversec.one.crypto.encoding.pad.AbstractPadder
import io.oversec.one.crypto.gpg.GpgCryptoHandler
import io.oversec.one.crypto.gpg.OversecAsciiArmoredOutputStream
import io.oversec.one.crypto.proto.Outer
import java.io.IOException

class AsciiArmouredGpgXCoder(ctx: Context) : AbstractXCoder(ctx) {

    override val id: String
        get() = ID

    override val isTextOnly: Boolean
        get() = true

    @Throws(IOException::class)
    override fun encodeInternal(
        msg: Outer.Msg,
        ignore: AbstractPadder?,
        packagename: String
    ): String {
        return GpgCryptoHandler.getRawMessageAsciiArmoured(msg) ?: ""
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    override fun decode(s: String): Outer.Msg? {
        val i = s.indexOf(OversecAsciiArmoredOutputStream.headerStart)
        if (i < 0) {
            return null
        } else {
            val k = s.indexOf(
                OversecAsciiArmoredOutputStream.footerStart,
                i + OversecAsciiArmoredOutputStream.headerStart.length
            )
            if (k < 0) {
                throw IllegalArgumentException("invalid ascii armour")
            }
        }

        return GpgCryptoHandler.parseMessageAsciiArmoured(s)
    }

    override fun getLabel(padder: AbstractPadder?): String {
        return mCtx.getString(R.string.encoder_gpg_ascii_armoured)
    }

    override fun getExample(padder: AbstractPadder?): String {
        return "-----BEGIN PGP MESSAGE-----"
    }

    companion object {
        const val ID = "gpg-ascii-armoured"
    }
}
