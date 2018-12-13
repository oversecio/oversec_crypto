package io.oversec.one.crypto.encoding.pad

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect

abstract class AbstractPadder internal constructor(protected val mCtx: Context) {
    protected var p = Paint().also { it.textSize = TEXT_SIZE_FOR_WIDTH_CALCULATION }
    protected var bounds = Rect()

    abstract val nextPaddingChar: Char

    open val example: String
        get() {
            reset()
            val sexample = StringBuilder(NUM_EXAMPLE_CHARS)
            for (i in 0 until NUM_EXAMPLE_CHARS) {
                sexample.append(nextPaddingChar)
            }
            sexample.append(tail())
            return sexample.toString()
        }

    abstract val label: String
    abstract val id: String

    @Synchronized
    open fun pad(orig: String, encoded: StringBuffer) {

        p.getTextBounds(orig, 0, orig.length, bounds)
        val w = bounds.width()
        val initialEncodedLength =
            encoded.length//-1   :0; //skip the initial encoding less 1 for nonspacing coder

        p.getTextBounds(encoded.toString(), initialEncodedLength, encoded.length, bounds)
        var we = bounds.width()
        while (we < w) {
            encoded.append(nextPaddingChar)
            p.getTextBounds(encoded.toString(), initialEncodedLength, encoded.length, bounds)
            we = bounds.width()
        }

        val tail = tail()
        encoded.append(tail)
    }


    abstract fun reset()

    internal abstract fun tail(): String

    companion object {
        private const val NUM_EXAMPLE_CHARS = 40
        const val TEXT_SIZE_FOR_WIDTH_CALCULATION = 30f
    }


}
