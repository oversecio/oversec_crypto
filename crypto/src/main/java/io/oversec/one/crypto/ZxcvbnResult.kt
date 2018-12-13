package io.oversec.one.crypto

import android.os.Parcel
import android.os.Parcelable

class ZxcvbnResult : Parcelable {

    private var mWarning: String? = null
    var entropy: Int = 0
        private set

    val warning: CharSequence?
        get() = mWarning

    constructor(source: Parcel) {
        mWarning = source.readString()
        entropy = source.readInt()
    }

    constructor(result: String, entropy: Int) {
        mWarning = result
        this.entropy = entropy
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mWarning)
        dest.writeInt(entropy)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<ZxcvbnResult> = object : Parcelable.Creator<ZxcvbnResult> {
            override fun newArray(size: Int): Array<ZxcvbnResult?> {
                return arrayOfNulls(size)
            }

            override fun createFromParcel(source: Parcel): ZxcvbnResult {
                return ZxcvbnResult(source)
            }
        }
    }
}
