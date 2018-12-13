package io.oversec.one.crypto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object Help {

    enum class ANCHOR {
        encparams_pgp,
        encparams_simple,
        encparams_sym,
        main_help,
        main_apps,
        main_keys,
        main_help_acsconfig,

        symkey_create_pbkdf,
        symkey_create_random,
        symkey_create_scan,
        symkey_details,

        appconfig_main,
        appconfig_appearance,
        appconfig_lab,

        button_hide_visible,
        button_hide_hidden, main_settings,

        button_encrypt_initial,
        button_encrypt_encryptionparamsremembered,

        input_insufficientpadding,
        input_corruptedencoding,

        bossmode_active, main_padders, settextfailed, button_compose, paste_clipboard, main_help_accessibilitysettingsnotresolvable
    }

    fun open(ctx: Context, anchor: ANCHOR?) {
        open(ctx, anchor?.name)
    }

    @JvmOverloads
    fun open(ctx: Context, anchor: String? = null) {
        try {
            var url = anchor
            if (url == null || !url.startsWith(getUrlIndex(ctx))) {
                url = getUrlIndex(ctx) + if (anchor == null) "" else "#alias_$anchor"
            }
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            //if (!(ctx instanceof Activity))
            run { i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            ctx.startActivity(i)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getUrlIndex(ctx: Context): String {
        return "http://" + ctx.getString(R.string.feature_help_host) + "/index.html"
    }

    @Throws(PackageManager.NameNotFoundException::class)
    fun getAnchorForPackageInfos(ctx: Context, packagename: String): String {
        val packageInfo = ctx.packageManager.getPackageInfo(packagename, 0)
        val versionNumber = packageInfo.versionCode
        val packageNameReplaced = packagename.replace('.', '-')
        return getUrlIndex(ctx) + "#package_" + packageNameReplaced + "$" + versionNumber
    }


    fun openForPackage(ctx: Context, packagename: String?) {
        if (packagename == null) return
        try {
            val url = getAnchorForPackageInfos(ctx, packagename)
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            if (ctx !is Activity) {
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(i)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getApplicationName(ctx: Context, packagename: String): CharSequence {
        return try {
            val ai = ctx.packageManager.getApplicationInfo(packagename, 0)
            ctx.packageManager.getApplicationLabel(ai)
        } catch (e: PackageManager.NameNotFoundException) {
            packagename
        }

    }
}
