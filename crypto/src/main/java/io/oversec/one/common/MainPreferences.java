package io.oversec.one.common;

import android.content.Context;
import io.oversec.one.crypto.R;

public class MainPreferences {

    public static final String FILENAME = "main_prefs";

    public static boolean isAllowScreenshots(Context ctx) {
        return ctx.getSharedPreferences(FILENAME, 0).getBoolean(ctx.getString(R.string.mainprefs_allow_screenshot_key), false);
    }
}
