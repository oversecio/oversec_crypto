package io.oversec.one.crypto.sym;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import io.oversec.one.common.Consts;
import io.oversec.one.crypto.R;

import java.util.List;


public class KeystoreIntentService extends IntentService {

    public static final int NOTIFICATION_ID__CACHED_KEYS = ++Consts.NOTIFICATION_ID_BASE;
    private static final String ACTION_CLEAR_ALL_CACHED_KEYS = "ACTION_CLEAR_ALL_CACHED_KEYS";


    public KeystoreIntentService() {
        super("oversec_keystore_intent_service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (ACTION_CLEAR_ALL_CACHED_KEYS.equals(action)) {
            OversecKeystore2.getInstance(this).clearAllCaches();
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);
        }
    }


    public static Notification buildCachedKeysNotification(Context ctx, List<String> keyAliases) {
//        Bitmap largeIcon = BitmapFactory.decodeResource(ctx.getResources(),
//                R.drawable.oversec_lock_127);

        Intent mainIntent = new Intent(ctx, KeystoreIntentService.class);
        mainIntent.setAction(ACTION_CLEAR_ALL_CACHED_KEYS);
        PendingIntent pendingMainIntent = PendingIntent.getService(ctx, 0,
                mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent clearKeycacheIntent = new Intent(ctx, KeystoreIntentService.class);
        clearKeycacheIntent.setAction(ACTION_CLEAR_ALL_CACHED_KEYS);
        PendingIntent PendingClearKeycacheIntent = PendingIntent.getService(ctx, 0,
                clearKeycacheIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                ctx)
                .setSmallIcon(R.drawable.ic_cachedkey_24dp)
                .setColor(ContextCompat.getColor(ctx,R.color.colorPrimary))
                //.setLargeIcon(largeIcon)
                .setContentTitle(ctx.getResources().getQuantityString(R.plurals.notification_cachedkeys_title, keyAliases.size(), keyAliases.size()))
                .setContentText(ctx.getString(R.string.notification_cachedkeys_touch_to_clear))
                .setContentIntent(pendingMainIntent);


        builder.addAction(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                        R.drawable.ic_clear_black_24dp : R.drawable.ic_clear_white_24dp,
                ctx.getString(
                        R.string.notification_action_clear_cached_keys),
                PendingClearKeycacheIntent);

        Notification n = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            n.visibility = Notification.VISIBILITY_SECRET;
        }


        return n;
    }

    private static CharSequence keyAliasesToString(List<String> keyAliases) {
        StringBuilder sb = new StringBuilder();
        for (String alias : keyAliases) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(alias);
        }
        return sb.toString();
    }


}
