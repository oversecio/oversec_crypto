package io.oversec.one.common

import android.content.Context
import io.oversec.one.crypto.R

object MainPreferences {

    val FILENAME = "main_prefs"

    fun isAllowScreenshots(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILENAME, 0)
            .getBoolean(ctx.getString(R.string.mainprefs_allow_screenshot_key), false)
    }

    fun isRelaxEncryptionCache(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILENAME, 0)
            .getBoolean(ctx.getString(R.string.mainprefs_relaxecache_key), false)
    }

    fun isPanicOnScreenOff(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILENAME, 0)
            .getBoolean(ctx.getString(R.string.mainprefs_screenoffpanic_key), false)
    }

    fun isHideLauncherOnPanic(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILENAME, 0)
            .getBoolean(ctx.getString(R.string.mainprefs_hidelauncheronpanic_key), false)
    }

    fun getLauncherSecretDialerCode(ctx: Context): String {
        return ctx.getSharedPreferences(FILENAME, 0)
            .getString(ctx.getString(R.string.mainprefs_launchersecretcode_key), "")
    }

    fun setLauncherSecretDialerCode(ctx: Context, value: String) {
        ctx.getSharedPreferences(FILENAME, 0).edit()
            .putString(ctx.getString(R.string.mainprefs_launchersecretcode_key), value).commit()
    }

    fun isDialerSecretCodeBroadcastConfirmedWorking(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILENAME, 0).getBoolean(
            ctx.getString(R.string.mainprefs_dialersecretcodebroadcastworking_key),
            false
        )
    }

    fun setDialerSecretCodeBroadcastConfirmedWorking(ctx: Context) {
        ctx.getSharedPreferences(FILENAME, 0).edit().putBoolean(
            ctx.getString(R.string.mainprefs_dialersecretcodebroadcastworking_key),
            true
        ).commit()
    }
}
