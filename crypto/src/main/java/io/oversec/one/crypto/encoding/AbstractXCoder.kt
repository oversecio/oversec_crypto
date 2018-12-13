package io.oversec.one.crypto.encoding

import android.content.Context
import io.oversec.one.crypto.encoding.pad.AbstractPadder
import io.oversec.one.crypto.proto.Outer
import java.io.IOException

abstract class AbstractXCoder (protected val mCtx: Context) : IXCoder {

    @Throws(IOException::class)
    abstract fun encodeInternal(
        msg: Outer.Msg,
        padder: AbstractPadder?,
        packagename: String
    ): String

    @Throws(IOException::class)
    override fun encode(
        msg: Outer.Msg,
        padder: AbstractPadder?,
        plainTextForWidthCalculation: String?,
        appendNewLines: Boolean,
        packagename: String
    ): String {
        padder?.reset()
        val nn = if (appendNewLines && plainTextForWidthCalculation!=null) (plainTextForWidthCalculation.lines().size-1) else 0
        val internal = encodeInternal(msg, padder, packagename)
        val r = StringBuffer(internal)
        //prependPrefix(r);
        if (nn > 0) {
            r.append("\n".repeat(nn))
        }
        plainTextForWidthCalculation?.let {  padder?.pad(it, r)  }
        return r.toString()
    }


    private fun countNewLines(s: String): Int {
        return s.lines().size-1
    }



}
