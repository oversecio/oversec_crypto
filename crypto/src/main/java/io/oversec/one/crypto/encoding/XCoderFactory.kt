package io.oversec.one.crypto.encoding

import android.annotation.SuppressLint
import android.content.Context
import io.oversec.one.crypto.proto.Outer
import java.util.ArrayList

class XCoderFactory private constructor(context: Context) {

    private val ALL = ArrayList<IXCoder>()

    val base64XCoder = Base64XCoder(context).also { add(it) }
    val zeroWidthXCoder = ZeroWidthXCoder(context).also { add(it) }
    val asciiArmouredGpgXCoder = AsciiArmouredGpgXCoder(context).also { add(it) }


    private fun add(coder: IXCoder) {
        ALL.add(coder)
    }

    @Synchronized
    fun decode(s: String): Outer.Msg? {
        for (coder in ALL) {
            try {
                val m = coder.decode(s)
                if (m != null) {
                    return m
                }
            } catch (e: Exception) {
                //
            }

        }
        return null
    }

    @Synchronized
    fun getEncodingInfo(s: String): String {
        for (coder in ALL) {
            try {
                val msg = coder.decode(s)
                if (msg != null) {
                    return msg.msgDataCase.name + " (" + coder.getLabel(null) + ")"
                }
            } catch (e: Exception) {
                //
            }

        }
        return "N/A"

    }

    fun isEncodingCorrupt(s: String): Boolean {
        for (coder in ALL) {
            try {
                coder.decode(s)
            } catch (e: Exception) {
                e.printStackTrace()
                return true
            }
        }
        return false
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var INSTANCE: XCoderFactory? = null

        fun getInstance(ctx: Context): XCoderFactory =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: XCoderFactory(ctx.applicationContext).also { INSTANCE = it }
            }
    }
}
