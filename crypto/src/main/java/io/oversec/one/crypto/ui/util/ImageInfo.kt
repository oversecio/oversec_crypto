package io.oversec.one.crypto.ui.util

class ImageInfo(val mimetype: String, val width: Int, val height: Int) {

    override fun toString(): String {
        return "ImageInfo{" +
                "mMimeType='" + mimetype + '\''.toString() +
                ", mWidth=" + width +
                ", mHeight=" + height +
                '}'.toString()
    }
}
