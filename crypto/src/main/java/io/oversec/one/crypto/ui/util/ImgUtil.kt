package io.oversec.one.crypto.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.io.InputStream

object ImgUtil {

    fun parseImageInfo(bb: ByteArray): ImageInfo {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeByteArray(bb, 0, bb.size, options)
        return ImageInfo(options.outMimeType, options.outWidth, options.outHeight)
    }

    @Throws(IOException::class)
    fun parseImageInfo(`is`: InputStream): ImageInfo {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(`is`, null, options)
        return ImageInfo(options.outMimeType, options.outWidth, options.outHeight)
    }

    fun loadImage(inputStream: InputStream, sampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        return BitmapFactory.decodeStream(inputStream, null, options)
    }
}
