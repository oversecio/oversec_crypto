package io.oversec.one.crypto.images.xcoder

import android.content.Context
import io.oversec.one.crypto.images.xcoder.blackandwhite.BlackAndWhiteImageXCoder
import java.util.ArrayList

object ImageXCoderFacade {

    fun getAll(ctx: Context): List<ImageXCoder> {
        return ArrayList<ImageXCoder>().also { it.add(BlackAndWhiteImageXCoder(ctx)) }
    }
}
