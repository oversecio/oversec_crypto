package io.oversec.one.crypto.images.xcoder

import android.net.Uri
import io.oversec.one.crypto.proto.Outer
import java.io.IOException

interface ImageXCoder {

    @Throws(IOException::class)
    fun parse(uri: Uri): Outer.Msg

    @Throws(IOException::class)
    fun encode(msg: Outer.Msg): Uri
}
