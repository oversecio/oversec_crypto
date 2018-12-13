package io.oversec.one.crypto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.HashSet

class AppsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            intent?.data?.encodedSchemeSpecificPart?.let {
                packagename -> fire(context, intent.action, packagename )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    interface IAppsReceiverListener {
        fun onAppChanged(ctx: Context, action: String, packagename: String)
    }

    companion object {

        private val mListeners = HashSet<IAppsReceiverListener>()

        @Synchronized
        private fun fire(ctx: Context, action: String, packagename: String) {
            mListeners.forEach {
                it.onAppChanged(ctx, action, packagename)
            }
        }

        @Synchronized
        fun addListener(listener: IAppsReceiverListener) {
            mListeners.add(listener)
        }

        @Synchronized
        fun removeListener(listener: IAppsReceiverListener) {
            mListeners.remove(listener)
        }
    }
}
