package io.oversec.one.common;

import android.app.Activity;
import android.app.Fragment;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.encoding.pad.PadderContent;

import java.util.List;

public abstract class CoreContract {

    private static CoreContract INSTANCE;

    public static void init(CoreContract impl) {
        INSTANCE = impl;
    }

    public static CoreContract getInstance() {
        return INSTANCE;
    }

    public abstract List<PadderContent> getAllPaddersSorted();

    public abstract void doIfFullVersionOrShowPurchaseDialog(Activity activity, Runnable okRunnable, int requestCode);

    public abstract void doIfFullVersionOrShowPurchaseDialog(Fragment fragment, Runnable okRunnable, int requestCode);

    public abstract AbstractEncryptionParams getBestEncryptionParams(String packageName);

    public abstract boolean isDbSpreadInvisibleEncoding(String packagename);
}
