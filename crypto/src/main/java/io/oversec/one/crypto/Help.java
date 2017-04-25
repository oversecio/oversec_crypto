package io.oversec.one.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

public class Help {



    public enum ANCHOR {
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

        bossmode_active, main_padders, settextfailed,  button_compose, paste_clipboard, main_help_accessibilitysettingsnotresolvable,


    }

    public static void open(Context ctx) {
        open(ctx, (String) null);
    }

    public static void open(Context ctx, ANCHOR anchor) {
        open(ctx, anchor == null ? null : anchor.name());
    }

    public static void open(Context ctx, String anchor) {
        try {
            String url =  anchor;
            if (url==null || !url.startsWith(getUrlIndex(ctx))) {
                url = getUrlIndex(ctx) + (anchor == null ? "" : "#alias_" + anchor);
            }
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            //if (!(ctx instanceof Activity))
            {
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            ctx.startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getUrlIndex(Context ctx) {
        return "http://"+ctx.getString(R.string.feature_help_host)+"/index.html";
    }

    public static String getAnchorForPackageInfos(Context ctx, String packagename) throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(packagename, 0);
        int versionNumber = packageInfo.versionCode;
        String packageNameReplaced = packagename.replace('.', '-');
        return getUrlIndex(ctx) + "#package_" + packageNameReplaced + "$" + versionNumber;
    }


    public static void openForPackage(Context ctx, String packagename) {
        if (packagename == null) return;
        try {
            String url = getAnchorForPackageInfos(ctx,packagename);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (!(ctx instanceof Activity)) {
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ctx.startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static CharSequence getApplicationName(Context ctx, String packagename) {
        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(packagename, 0);
            return  ctx.getPackageManager().getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            return packagename;
        }

    }
}
