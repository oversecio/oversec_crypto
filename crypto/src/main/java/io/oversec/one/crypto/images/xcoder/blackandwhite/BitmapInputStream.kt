package io.oversec.one.crypto.images.xcoder.blackandwhite

import android.graphics.Bitmap
import android.graphics.Color
import java.io.IOException
import java.io.InputStream

class BitmapInputStream internal constructor(private val mBm: Bitmap) : InputStream() {
    private val mW: Int
    private val mH: Int

    private var mPixelOffset: Int = 0

    init {
        mW = mBm.width
        mH = mBm.height
    }

    @Throws(IOException::class)
    override fun read(): Int {
        var y = mPixelOffset / mW
        var x = mPixelOffset - y * mW
        var res = 0
        for (k in 0..7) {
            if (x > mW || y > mH) {
                return -1
            }

            val c = mBm.getPixel(x, y)
            var score = 0
            if (Color.red(c) > 128) {
                score++
            }
            if (Color.blue(c) > 128) {
                score++
            }
            if (Color.green(c) > 128) {
                score++
            }
            if (score > 1) {
                res += 1 shl 7 - k
            }
            x++
            if (x >= mW) {
                x = 0
                y++
            }
        }
        mPixelOffset += 8
        return res
    }
}