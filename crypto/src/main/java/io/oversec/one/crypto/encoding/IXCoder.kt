package io.oversec.one.crypto.encoding

import io.oversec.one.crypto.encoding.pad.AbstractPadder
import io.oversec.one.crypto.proto.Outer

interface IXCoder {

    val id: String
    val isTextOnly: Boolean

    @Throws(Exception::class)
    fun decode(encText: String): Outer.Msg?

    @Throws(Exception::class)
    fun encode(
        msg: Outer.Msg,
        padder: AbstractPadder?,
        plainTextForWidthCalculation: String?,
        appendNewLines: Boolean,
        packagename: String
    ): String

    fun getLabel(padder: AbstractPadder?): String
    fun getExample(padder: AbstractPadder?): String
}
