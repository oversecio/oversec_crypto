package io.oversec.one.crypto.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("CommitPrefEdits")
class GotItPreferences private constructor(context: Context) {

    private val mSharedPreferences: SharedPreferences

    init {
        mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, 0)
    }

    fun clear() {
        mSharedPreferences.edit().clear().commit()
    }

    fun isTooltipConfirmed(res: String?): Boolean {
        return if (res == null) {
            false
        } else mSharedPreferences.getBoolean(PREF_GOTIT_PREFIX + res, false)
    }

    fun setTooltipConfirmed(res: String?) {
        if (res == null) {
            return
        }
        mSharedPreferences.edit().putBoolean(PREF_GOTIT_PREFIX + res, true).commit()
    }

    companion object {

        private val PREF_GOTIT_PREFIX = "gotit_"
        private var mPreferences: GotItPreferences? = null
        private val PREF_FILE_NAME = GotItPreferences::class.java.simpleName + "1"

        @Synchronized
        fun getPreferences(context: Context): GotItPreferences {
            if (mPreferences == null) {
                mPreferences = GotItPreferences(context.applicationContext)
            }
            return mPreferences!!
        }
    }
}
