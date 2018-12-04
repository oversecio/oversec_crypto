package io.oversec.one.crypto;

import android.util.Log;
import roboguice.util.Ln;

public class LoggingConfig {
    public static boolean LOG = false;

    public static void init(boolean debug) {
            setLog(debug);
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
