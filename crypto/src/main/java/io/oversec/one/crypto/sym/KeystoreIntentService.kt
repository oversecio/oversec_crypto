package io.oversec.one.crypto.sym

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import io.oversec.one.common.Consts
import io.oversec.one.common.CoreContract
import io.oversec.one.crypto.R

class KeystoreIntentService : IntentService("oversec_keystore_intent_service") {

    override fun onHandleIntent(intent: Intent?) {
        val action = intent!!.action
        if (ACTION_CLEAR_ALL_CACHED_KEYS == action) {
            OversecKeystore2.getInstance(this).clearAllCaches()
            CoreContract.instance.clearEncryptionCache()
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(it)
        }
    }

    companion object {

        val NOTIFICATION_ID__CACHED_KEYS = Consts.getNextNotificationId()
        private const val ACTION_CLEAR_ALL_CACHED_KEYS = "ACTION_CLEAR_ALL_CACHED_KEYS"


        fun buildCachedKeysNotification(ctx: Context, keyAliases: List<String>): Notification {
            //        Bitmap largeIcon = BitmapFactory.decodeResource(ctx.getResources(),
            //                R.drawable.oversec_lock_127);

            val mainIntent = Intent(ctx, KeystoreIntentService::class.java)
            mainIntent.action = ACTION_CLEAR_ALL_CACHED_KEYS
            val pendingMainIntent = PendingIntent.getService(
                ctx, 0,
                mainIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val clearKeycacheIntent = Intent(ctx, KeystoreIntentService::class.java)
            clearKeycacheIntent.action = ACTION_CLEAR_ALL_CACHED_KEYS
            val PendingClearKeycacheIntent = PendingIntent.getService(
                ctx, 0,
                clearKeycacheIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(
                ctx
            )
                .setSmallIcon(R.drawable.ic_cachedkey_24dp)
                .setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
                //.setLargeIcon(largeIcon)
                .setContentTitle(
                    ctx.resources.getQuantityString(
                        R.plurals.notification_cachedkeys_title,
                        keyAliases.size,
                        keyAliases.size
                    )
                )
                .setContentText(ctx.getString(R.string.notification_cachedkeys_touch_to_clear))
                .setContentIntent(pendingMainIntent)


            builder.addAction(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    R.drawable.ic_clear_black_24dp
                else
                    R.drawable.ic_clear_white_24dp,
                ctx.getString(
                    R.string.notification_action_clear_cached_keys
                ),
                PendingClearKeycacheIntent
            )

            val n = builder.build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                n.visibility = Notification.VISIBILITY_SECRET
            }
            return n
        }

        private fun keyAliasesToString(keyAliases: List<String>): CharSequence {
            val sb = StringBuilder()
            for (alias in keyAliases) {
                if (sb.length > 0) {
                    sb.append(", ")
                }
                sb.append(alias)
            }
            return sb.toString()
        }
    }
}
