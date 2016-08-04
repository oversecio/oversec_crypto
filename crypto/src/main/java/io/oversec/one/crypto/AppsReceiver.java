package io.oversec.one.crypto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashSet;
import java.util.Set;

public class AppsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String packagename = intent.getData().getEncodedSchemeSpecificPart();
            fire(context, intent.getAction(), packagename);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static final Set<IAppsReceiverListener> mListeners = new HashSet<>();

    public interface IAppsReceiverListener {
        void onAppChanged(Context ctx, String action, String packagename);
    }

    private static synchronized void fire(Context ctx, String action, String packagename) {
        for (IAppsReceiverListener listener : mListeners) {
            listener.onAppChanged(ctx, action, packagename);
        }
    }

    public static synchronized void addListener(IAppsReceiverListener listener) {
        mListeners.add(listener);
    }

    public static synchronized void removeListener(IAppsReceiverListener listener) {
        mListeners.remove(listener);
    }
}
