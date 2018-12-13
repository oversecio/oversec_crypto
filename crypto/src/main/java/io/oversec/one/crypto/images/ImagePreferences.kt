package io.oversec.one.crypto.images

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("CommitPrefEdits")
class ImagePreferences private constructor(context: Context) {
    private val mSharedPreferences= context.getSharedPreferences(PREF_FILE_NAME, 0)

    var xCoder: String?
        get() = mSharedPreferences.getString(PREF_IMAGE__CODER, null)
        set(name) {
            mSharedPreferences.edit().putString(PREF_IMAGE__CODER, name).commit()
        }

    fun clear() {
        mSharedPreferences.edit().clear().commit()
    }

    companion object {
        private const val PREF_IMAGE__CODER = "coder"

        private var mImagePreferences: ImagePreferences? = null
        private val PREF_FILE_NAME = ImagePreferences::class.java.simpleName

        @Synchronized
        fun getPreferences(context: Context): ImagePreferences {
            return mImagePreferences ?:  ImagePreferences(context).also { mImagePreferences = it  }
        }
    }

}
