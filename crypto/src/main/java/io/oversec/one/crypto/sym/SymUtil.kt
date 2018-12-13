package io.oversec.one.crypto.sym

import android.graphics.Bitmap
import android.graphics.Color
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.jwetherell.quick_response_code.data.Contents
import com.jwetherell.quick_response_code.qrcode.QRCodeEncoder
import org.spongycastle.util.encoders.Base64

import java.math.BigInteger
import java.nio.ByteBuffer

object SymUtil {

    fun getQrCode(data: ByteArray, dimension: Int): Bitmap? {
        try {
            val b64data = Base64.toBase64String(data)

            val qrCodeEncoder = QRCodeEncoder(
                b64data, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(),
                dimension
            )

            return qrCodeEncoder.encodeAsBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    @JvmOverloads
    fun byteArrayToHex(a: ByteArray, divider: String? = null): String {
        val sb = StringBuilder(a.size * 2)
        var d = false
        for (b in a) {
            sb.append(String.format("%02x", b.toInt() and 0xff))
            if (d && divider != null) {
                sb.append(divider)
            }
            d = !d
        }
        return sb.toString()
    }

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                    ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun long2bytearray(l: Long): ByteArray {
        //TODO optimize with needing to create a ByteBuffer object

        val b = ByteArray(8)

        val buf = ByteBuffer.wrap(b)
        buf.putLong(l)
        return b
    }


    fun bytearray2long(b: ByteArray): Long {
        //TODO optimize with creating a new ByteBuffer object

        val buf = ByteBuffer.wrap(b, 0, 8)
        return buf.long
    }

    fun longToHex(l: Long): String {
        return byteArrayToHex(BigInteger.valueOf(l).toByteArray(), null)
    }

    fun hex2long(s: String): Long {
        return BigInteger(s, 16).toLong()
    }

    fun longToPrettyHex(v: Long): String {
        return byteArrayToHex(BigInteger.valueOf(v).toByteArray(), " ")
    }

    fun applyAvatar(textView: TextView, name: String) {
        val hash = name.hashCode()
        val ba = SymUtil.long2bytearray(hash.toLong())
        val red = ((ba[ba.size - 1].toInt() and 0xFF) * 0.8f).toInt()
        val green = ((ba[ba.size - 2].toInt() and 0xFF) * 0.8f).toInt()
        val blue = ((ba[ba.size - 3].toInt() and 0xFF) * 0.8f).toInt()
        val avatarColor = Color.rgb(red, green, blue)
        textView.setBackgroundColor(avatarColor)
        textView.text = name[0].toString()
    }

    fun applyAvatar(textView: TextView, keyId: Long, name: String) {
        val ba = SymUtil.long2bytearray(keyId)

        val red = ((ba[0].toInt() and 0xFF) * 0.8f).toInt()
        val green = ((ba[1].toInt() and 0xFF) * 0.8f).toInt()
        val blue = ((ba[2].toInt() and 0xFF) * 0.8f).toInt()
        val color = Color.rgb(red, green, blue)

        textView.setBackgroundColor(color)
        textView.text = name[0].toString()
    }
}
