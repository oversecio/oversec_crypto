package io.oversec.one.crypto.gpg

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("CommitPrefEdits")
class GpgPreferences private constructor(context: Context) {
    private val mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, 0)

    var gpgOwnPublicKeyId: Long
        get() = mSharedPreferences.getLong(PREF_PGP_OWN_PUBLIC_KEY_ID, 0)
        set(v) {
            mSharedPreferences.edit().putLong(PREF_PGP_OWN_PUBLIC_KEY_ID, v).commit()
        }

    fun clear() {
        mSharedPreferences.edit().clear().commit()
    }

    companion object {

        private const val PREF_PGP_OWN_PUBLIC_KEY_ID = "pgp_own_public_key_id"
        private val PREF_FILE_NAME = GpgPreferences::class.java.simpleName

        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var mGpgPreferences: GpgPreferences? = null

        fun getPreferences(ctx: Context): GpgPreferences =
            mGpgPreferences ?: synchronized(this) {
                mGpgPreferences ?: GpgPreferences(ctx.applicationContext).also { mGpgPreferences = it }
            }

    }
}
