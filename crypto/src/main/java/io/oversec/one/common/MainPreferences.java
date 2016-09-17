package io.oversec.one.common;

import android.content.Context;
import io.oversec.one.crypto.R;

public class MainPreferences {

    public static final String FILENAME = "main_prefs";

    public static boolean isAllowScreenshots(Context ctx) {
        return ctx.getSharedPreferences(FILENAME, 0).getBoolean(ctx.getString(R.string.mainprefs_allow_screenshot_key), false);
    }

    public static boolean isPanicOnScreenOff(Context ctx) {
        return ctx.getSharedPreferences(FILENAME, 0).getBoolean(ctx.getString(R.string.mainprefs_screenoffpanic_key), false);
    }

    public static boolean isHideLauncherOnPanic(Context ctx) {
        return ctx.getSharedPreferences(FILENAME, 0).getBoolean(ctx.getString(R.string.mainprefs_hidelauncheronpanic_key), false);
    }

    public static String getLauncherSecretDialerCode(Context ctx) {
        return ctx.getSharedPreferences(FILENAME, 0).getString(ctx.getString(R.string.mainprefs_launchersecretcode_key), "");
    }

    public static void setLauncherSecretDialerCode(Context ctx, String value) {
        ctx.getSharedPreferences(FILENAME, 0).edit().
                putString(ctx.getString(R.string.mainprefs_launchersecretcode_key),value).commit();
    }

    public static boolean isDialerSecretCodeBroadcastConfirmedWorking(Context ctx) {
        return ctx.getSharedPreferences(FILENAME, 0).getBoolean(ctx.getString(R.string.mainprefs_dialersecretcodebroadcastworking_key), false);
    }

    public static void setDialerSecretCodeBroadcastConfirmedWorking(Context ctx) {
        ctx.getSharedPreferences(FILENAME, 0).edit().
                putBoolean(ctx.getString(R.string.mainprefs_dialersecretcodebroadcastworking_key),true).commit();
    }
}
