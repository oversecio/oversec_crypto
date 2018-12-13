package io.oversec.one.crypto.images.xcoder.blackandwhite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import io.oversec.one.crypto.TemporaryContentProvider
import io.oversec.one.crypto.images.xcoder.ContentNotFullyEmbeddedException
import io.oversec.one.crypto.images.xcoder.ImageXCoder
import io.oversec.one.crypto.proto.Outer
import java.io.ByteArrayOutputStream
import java.io.IOException


/**
 * Encodes one bit as one black/white pixel.
 *
 *
 * While this approach may seem to be quite naive,
 * it is surprisingly robust and surivives JPEG comression well!
 */
class BlackAndWhiteImageXCoder(private val mCtx: Context) : ImageXCoder {

    @Throws(IOException::class)
    override fun parse(uri: Uri): Outer.Msg {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565 //save some space
        val inputStream = mCtx.contentResolver.openInputStream(uri) ?: throw IOException()
        val bm = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val bis = BitmapInputStream(bm!!)
        return Outer.Msg.parseDelimitedFrom(bis)
    }

    @Throws(IOException::class)
    override fun encode(msg: Outer.Msg): Uri {
        val baos = ByteArrayOutputStream()
        msg.writeDelimitedTo(baos)
        baos.close()
        val plain = baos.toByteArray()

        val wh = Math.ceil(Math.sqrt((plain.size * 8).toDouble())).toInt()

        if (wh >= MAX_OUT_WH) {
            throw ContentNotFullyEmbeddedException()
        }

        val bm = Bitmap.createBitmap(wh, wh, Bitmap.Config.ARGB_8888)


        var offset = 0
        for (aPlain in plain) {
            offset = setBitmapPixel(bm, offset, wh, aPlain) //TODO: needs some  optimization
        }

        val uri = TemporaryContentProvider.prepare(
            mCtx,
            "image/png",
            TemporaryContentProvider.TTL_5_MINUTES,
            TemporaryContentProvider.TAG_ENCRYPTED_IMAGE
        )
        val os = mCtx.contentResolver.openOutputStream(uri) ?: throw IOException()
        bm.compress(Bitmap.CompressFormat.PNG, 100, os)
        os.close()

        return uri
    }

    private fun setBitmapPixel(bm: Bitmap, offset: Int, wh: Int, v: Byte): Int {
        var aoffset = offset

        for (i in 0..7) {
            val y = aoffset / wh
            val x = aoffset - y * wh
            var color = Color.BLACK
            val vxi = v.toInt() and 0xFF
            val b = vxi shr 7 - i and 0x01
            if (b > 0) {
                color = Color.WHITE
            }
            bm.setPixel(x, y, color)

            aoffset++
        }
        return aoffset

    }

    companion object {
        const val MAX_OUT_WH = 1024
    }

}
