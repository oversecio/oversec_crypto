package io.oversec.one.crypto.encoding

import io.oversec.one.crypto.encoding.pad.AbstractPadder
import io.oversec.one.crypto.proto.Outer

class XCoderAndPadder(val xcoder: IXCoder, val padder: AbstractPadder?) {

    val label: String
        get() = xcoder.getLabel(padder)

    val example: String
        get() = xcoder.getExample(padder)

    val id: String
        get() = xcoder.id + (padder?.let{ "__" + it.id} ?: "")

    val coderId: String?
        get() = xcoder.id

    val padderId: String?
        get() = padder?.id

    @Synchronized
    @Throws(Exception::class)
    fun encode(
        msg: Outer.Msg,
        srcText: String,
        appendNewLines: Boolean,
        packagename: String
    ): String {
        return xcoder.encode(msg, padder, srcText, appendNewLines, packagename)
    }
}
