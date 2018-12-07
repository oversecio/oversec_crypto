package io.oversec.one.crypto;

import android.app.Activity;
import android.app.Fragment;

import java.util.Collections;
import java.util.List;

import io.oversec.one.common.CoreContract;
import io.oversec.one.crypto.encoding.pad.PadderContent;

public class CoreContractTestBase extends CoreContract {

    @Override
    public List<PadderContent> getAllPaddersSorted() {
        return Collections.emptyList();
    }

    @Override
    public void doIfFullVersionOrShowPurchaseDialog(Activity activity, Runnable okRunnable, int requestCode) {
        okRunnable.run();
    }

    @Override
    public void doIfFullVersionOrShowPurchaseDialog(Fragment fragment, Runnable okRunnable, int requestCode) {
        okRunnable.run();
    }

    @Override
    public AbstractEncryptionParams getBestEncryptionParams(String packageName) {
        return null;
    }

    @Override
    public boolean isDbSpreadInvisibleEncoding(String packagename) {
        return false;
    }

    @Override
    public void clearEncryptionCache() {

    }

    @Override
    public void putInEncryptionCache(String encText, BaseDecryptResult r) {

    }

    @Override
    public BaseDecryptResult getFromEncryptionCache(String encText) {
        return null;
    }
}
