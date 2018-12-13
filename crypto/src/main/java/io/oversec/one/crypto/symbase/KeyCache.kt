package io.oversec.one.crypto.symbase

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.oversec.one.crypto.sym.KeyNotCachedException
import io.oversec.one.crypto.sym.KeystoreIntentService
import io.oversec.one.crypto.sym.SymmetricKeyPlain
import io.oversec.one.crypto.sym.ui.UnlockKeyActivity
import roboguice.util.Ln
import java.util.*

class KeyCache private constructor(private val mCtx: Context) {

    private val mAlarmManager = mCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val mNotificationManager =
        mCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mKeyCacheListeners = ArrayList<OversecKeyCacheListener>()
    private val mPendingAlarms = HashMap<Long, PendingIntent>()
    private val mKeyMap = HashMap<Long, SymmetricKeyPlain>()
    private val mExpireOnScreenOff = HashSet<Long>()

    private var requestCount = 0

    private val allCachedKeyAliases: List<String>
        get() {
            return mKeyMap.mapValues {
                it.value.name
            }.values.filterNotNull().sortedBy {
                it
            }

        }


    val allSimpleKeys: List<SymmetricKeyPlain>
        get() {
            return mKeyMap.filterValues {
                it.isSimpleKey
            }.values.sortedBy {
                it.createdDate
            }
        }

    init {
        val aIntentReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (action == OVERSEC_BROADCAST_ACTION_EXPIRE_KEY) {
                    val keyId = intent.getLongExtra(EXTRA_KEY_ID, -1)
                    expire(keyId)
                }

                if (action == Intent.ACTION_SCREEN_OFF) {
                    expireOnScreenOff()
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(OVERSEC_BROADCAST_ACTION_EXPIRE_KEY)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        mCtx.registerReceiver(aIntentReceiver, filter)
    }

    @Synchronized
    fun doCacheKey(plainKey: SymmetricKeyPlain, ttl: Long) {
        val keyId = plainKey.id

        if (mKeyMap.containsKey(keyId)) {
            Ln.d("already cached!")
            //ToDo: this should never happen, but if so, maybe we should update the TTL ?

        } else {

            mKeyMap[keyId] = plainKey
            mKeyCacheListeners.forEach {
                it.onStartedCachingKey(keyId)
            }

            mNotificationManager.notify(
                KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS,
                KeystoreIntentService.buildCachedKeysNotification(mCtx, allCachedKeyAliases)
            )

            if (ttl == 0L) {
                mExpireOnScreenOff.add(keyId)
            } else if (ttl < Integer.MAX_VALUE) {

                val intent = Intent(OVERSEC_BROADCAST_ACTION_EXPIRE_KEY)
                intent.putExtra(EXTRA_KEY_ID, keyId)
                // request code should be unique for each PendingIntent, thus keyId is used
                val alarmIntent = PendingIntent.getBroadcast(
                    mCtx, ++requestCount, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                mPendingAlarms[keyId] = alarmIntent

                val triggerTime = Date().time + ttl * 1000
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, alarmIntent)
            }
        }


    }

    @Synchronized
    fun clearAll() {
        mKeyMap.keys.forEach {
            expire(it)
        }
        mKeyMap.clear()
        mNotificationManager.cancel(KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS)
    }

    @Synchronized
    fun expire(keyId: Long) {
        val pi = mPendingAlarms[keyId]
        if (pi != null) {
            mAlarmManager.cancel(pi)
        }
        mExpireOnScreenOff.remove(keyId)
        val k = mKeyMap[keyId]
        if (k != null) {
            //TODO: why does this happen? Maybe restarting the app after a crash and alarms still being in place?
            k.clearKeyData()
            mKeyMap.remove(keyId)
            for (l in mKeyCacheListeners) {
                l.onFinishedCachingKey(keyId)
            }
        }

        if (mKeyMap.size > 0) {
            mNotificationManager.notify(
                KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS,
                KeystoreIntentService.buildCachedKeysNotification(mCtx, allCachedKeyAliases)
            )
        } else {
            mNotificationManager.cancel(KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS)
        }
    }

    @Synchronized
    internal fun expireOnScreenOff() {
        val ids = HashSet(mExpireOnScreenOff)
        for (id in ids) {
            expire(id!!)
        }
        mExpireOnScreenOff.clear()
    }

    @Synchronized
    @Throws(KeyNotCachedException::class)
    operator fun get(keyId: Long?): SymmetricKeyPlain {
        return mKeyMap[keyId] ?: throw KeyNotCachedException(
            UnlockKeyActivity.buildPendingIntent(
                mCtx,
                keyId
            )
        )
    }

    fun hasKey(keyId: Long?): Boolean {
        return mKeyMap.containsKey(keyId)
    }

    @Synchronized
    fun addKeyCacheListener(l: OversecKeyCacheListener) {
        mKeyCacheListeners.add(l)
    }

    @Synchronized
    fun removeKeyCacheListener(l: OversecKeyCacheListener) {
        mKeyCacheListeners.remove(l)
    }


    @Synchronized
    fun getKeyByHashedKeyId(keyhash: Long, salt: ByteArray, costKeyhash: Int): SymmetricKeyPlain? {
        return mKeyMap.keys.find {
            val aSessionKeyId = KeyUtil.calcSessionKeyId(it, salt, costKeyhash)
            aSessionKeyId == keyhash
        }?.let {
            mKeyMap[it]
        }
    }

    companion object {
        private const val EXTRA_KEY_ID = "EXTRA_KEY_ID"
        private const val OVERSEC_BROADCAST_ACTION_EXPIRE_KEY =
            "OVERSEC_BROADCAST_ACTION_EXPIRE_KEY"


        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var INSTANCE: KeyCache? = null

        fun getInstance(ctx: Context): KeyCache =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyCache(ctx.applicationContext).also { INSTANCE = it }
            }
    }
}