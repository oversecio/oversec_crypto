package io.oversec.one.crypto.sym

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("CommitPrefEdits")
class SymPreferences private constructor(context: Context) {
    private val mSharedPreferences= context.getSharedPreferences(PREF_FILE_NAME, 0)

    var keystoreSymTTL: Int
        get() = mSharedPreferences.getInt(PREF_KEY_SYM_TTL, 0)
        set(v) {
            val editor = mSharedPreferences.edit()
            editor.putInt(PREF_KEY_SYM_TTL, v)
            editor.commit()
        }

    var keystoreSimpleTTL: Int
        get() = mSharedPreferences.getInt(PREF_KEY_SIMPLE_TTL, Integer.MAX_VALUE)
        set(v) {
            val editor = mSharedPreferences.edit()
            editor.putInt(PREF_KEY_SIMPLE_TTL, v)
            editor.commit()
        }

    fun clear() {
        mSharedPreferences.edit().clear().commit()
    }

    companion object {

        private const val PREF_KEY_SYM_TTL = "keystore_ttl_sym"
        private const val PREF_KEY_SIMPLE_TTL = "keystore_ttl_simple"
        private val PREF_FILE_NAME = SymPreferences::class.java.simpleName

        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var mSymPreferences: SymPreferences? = null

        fun getInstance(ctx: Context): SymPreferences =
            mSymPreferences ?: synchronized(this) {
                mSymPreferences ?: SymPreferences(ctx.applicationContext).also { mSymPreferences = it }
            }

    }

}
