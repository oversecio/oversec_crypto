package io.oversec.one.crypto;

import android.support.design.BuildConfig;
import android.util.Log;
import roboguice.util.Ln;

public class Config {
    public static boolean LOG = false;

    static {
        setLog(BuildConfig.DEBUG);
    }

    public static void setLog(boolean b) {
        LOG = b;
        if (!LOG) {
            Ln.getConfig().setLoggingLevel(Log.ERROR);
        } else {
            Ln.getConfig().setLoggingLevel(Log.VERBOSE);
        }
    }
}
