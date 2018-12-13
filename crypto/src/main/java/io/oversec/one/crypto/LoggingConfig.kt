package io.oversec.one.crypto

import android.util.Log
import roboguice.util.Ln

object LoggingConfig {
    var LOG = false

    fun init(debug: Boolean) {
        setLog(debug)
    }

    fun setLog(b: Boolean) {
        LOG = b
        if (!LOG) {
            Ln.getConfig().loggingLevel = Log.ERROR
        } else {
            Ln.getConfig().loggingLevel = Log.VERBOSE
        }
    }
}
