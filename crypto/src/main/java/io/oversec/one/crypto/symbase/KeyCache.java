package io.oversec.one.crypto.symbase;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import io.oversec.one.crypto.sym.KeyNotCachedException;
import io.oversec.one.crypto.sym.KeystoreIntentService;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.sym.ui.UnlockKeyActivity;
import roboguice.util.Ln;

import java.io.IOException;
import java.util.*;

public class KeyCache {
    private static final String EXTRA_KEY_ID = "EXTRA_KEY_ID";
    private static final String OVERSEC_BROADCAST_ACTION_EXPIRE_KEY = "OVERSEC_BROADCAST_ACTION_EXPIRE_KEY";
    private static KeyCache INSTANCE;

    private final AlarmManager mAlarmManager;
    private final NotificationManager mNotificationManager;
    private final List<OversecKeyCacheListener> mKeyCacheListeners = new ArrayList<>();
    private final Map<Long, PendingIntent> mPendingAlarms = new HashMap<>();
    private final Map<Long, SymmetricKeyPlain> mKeyMap = new HashMap<>();
    private final Set<Long> mExpireOnScreenOff = new HashSet<>();
    private final Context mCtx;

    private int requestCount = 0;

    public static synchronized KeyCache getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new KeyCache(ctx);
        }
        return INSTANCE;
    }

    private KeyCache(Context ctx) {
        mCtx = ctx;
        mAlarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
        mNotificationManager = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        BroadcastReceiver aIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(OVERSEC_BROADCAST_ACTION_EXPIRE_KEY)) {
                    long keyId = intent.getLongExtra(EXTRA_KEY_ID, -1);
                    expire(keyId);
                }

                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    expireOnScreenOff();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(OVERSEC_BROADCAST_ACTION_EXPIRE_KEY);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mCtx.registerReceiver(aIntentReceiver, filter);
    }

    public synchronized void doCacheKey(SymmetricKeyPlain plainKey, long ttl) {
        long keyId = plainKey.getId();


        if (mKeyMap.containsKey(keyId)) {
            Ln.d("already cached!");
            //ToDo: this should never happen, but if so, maybe we should update the TTL ?

        } else {

            mKeyMap.put(keyId, plainKey);


            for (OversecKeyCacheListener l : mKeyCacheListeners) {
                l.onStartedCachingKey(keyId);
            }

            mNotificationManager.notify(KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS,
                    KeystoreIntentService.buildCachedKeysNotification(mCtx, getAllCachedKeyAliases()));

            if (ttl == 0) {
                mExpireOnScreenOff.add(keyId);
            } else if (ttl < Integer.MAX_VALUE) {

                Intent intent = new Intent(OVERSEC_BROADCAST_ACTION_EXPIRE_KEY);
                intent.putExtra(EXTRA_KEY_ID, keyId);
                // request code should be unique for each PendingIntent, thus keyId is used
                PendingIntent alarmIntent = PendingIntent.getBroadcast(mCtx, ++requestCount, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                mPendingAlarms.put(keyId, alarmIntent);

                long triggerTime = new Date().getTime() + ttl * 1000;
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, alarmIntent);
            }
        }


    }

    private List<String> getAllCachedKeyAliases() {
        ArrayList<String> res = new ArrayList();
        for (SymmetricKeyPlain k : mKeyMap.values()) {
            res.add(k.getName());
        }
        Collections.sort(res);
        return res;
    }

    public synchronized void clearAll() {
        Set<Long> ids = new HashSet<>(mKeyMap.keySet());
        for (Long id : ids) {
            expire(id);
        }
        mKeyMap.clear();
        mNotificationManager.cancel(KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS);
    }

    public synchronized void expire(long keyId) {
        PendingIntent pi = mPendingAlarms.get(keyId);
        if (pi != null) {
            mAlarmManager.cancel(pi);
        }
        mExpireOnScreenOff.remove(keyId);
        SymmetricKeyPlain k = mKeyMap.get(keyId);
        if (k != null) {
            //TODO: why does this happen? Maybe restarting the app after a crash and alarms still being in place?
            k.clearKeyData();
            mKeyMap.remove(keyId);
            for (OversecKeyCacheListener l : mKeyCacheListeners) {
                l.onFinishedCachingKey(keyId);
            }

        }


        if (mKeyMap.size() > 0) {
            mNotificationManager.notify(KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS,
                    KeystoreIntentService.buildCachedKeysNotification(mCtx, getAllCachedKeyAliases()));
        } else {
            mNotificationManager.cancel(KeystoreIntentService.NOTIFICATION_ID__CACHED_KEYS);
        }
    }

    synchronized void expireOnScreenOff() {
        Set<Long> ids = new HashSet<>(mExpireOnScreenOff);
        for (Long id : ids) {
            expire(id);
        }
        mExpireOnScreenOff.clear();
    }

    public synchronized SymmetricKeyPlain get(Long keyId) throws KeyNotCachedException {
        SymmetricKeyPlain res = mKeyMap.get(keyId);
        if (res == null) {
            throw new KeyNotCachedException(UnlockKeyActivity.buildPendingIntent(mCtx, keyId));
        }
        return res;
    }

    public boolean hasKey(Long keyId) {
        return mKeyMap.containsKey(keyId);
    }

    public synchronized void addKeyCacheListener(OversecKeyCacheListener l) {
        mKeyCacheListeners.add(l);
    }

    public synchronized void removeKeyCacheListener(OversecKeyCacheListener l) {
        mKeyCacheListeners.remove(l);

    }


    public synchronized SymmetricKeyPlain getKeyByHashedKeyId(long keyhash, byte[] salt, int costKeyhash) {

        Set<Long> allIds = mKeyMap.keySet();
        for (Long id : allIds) {
            long aSessionKeyId = KeyUtil.calcSessionKeyId(id, salt, costKeyhash);

            if (aSessionKeyId == keyhash) {
                return mKeyMap.get(id);
            }
        }

        return null;
    }


    public List<SymmetricKeyPlain> getAllSimpleKeys() {
        ArrayList<SymmetricKeyPlain> res = new ArrayList<>();
        for (SymmetricKeyPlain k : mKeyMap.values()) {
            if (k.isSimpleKey()) {
                res.add(k);
            }
        }
        Collections.sort(res, new Comparator<SymmetricKeyPlain>() {
            @Override
            public int compare(SymmetricKeyPlain lhs, SymmetricKeyPlain rhs) {
                return lhs.getCreatedDate().compareTo(rhs.getCreatedDate());
            }
        });
        return res;
    }
}
