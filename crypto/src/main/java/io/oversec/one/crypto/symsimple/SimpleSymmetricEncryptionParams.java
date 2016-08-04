package io.oversec.one.crypto.symsimple;

import android.content.Context;
import io.oversec.one.crypto.EncryptionMethod;
import io.oversec.one.crypto.symbase.BaseSymmetricEncryptionParams;
import io.oversec.one.crypto.symbase.KeyCache;

import java.util.List;

public class SimpleSymmetricEncryptionParams extends BaseSymmetricEncryptionParams {

    protected SimpleSymmetricEncryptionParams(String coderId, String padderId) {
        super(EncryptionMethod.SIMPLESYM, coderId, padderId);
    }

    public SimpleSymmetricEncryptionParams(List<Long> keyIds, String coderId, String padderId) {
        this(coderId, padderId);
        if (keyIds != null) {
            mKeyIds = keyIds;
        }
    }

    public SimpleSymmetricEncryptionParams(Long keyId, String coderId, String padderId) {
        this(coderId, padderId);
        mKeyIds.add(keyId);
    }


    @Override
    public boolean isStillValid(Context ctx) {
        if (mKeyIds == null) {
            return false;
        }
        KeyCache kc = KeyCache.getInstance(ctx);

        for (Long keyId : mKeyIds) {
            if (!kc.hasKey(keyId)) {
                return false;
            }
        }
        return true;
    }
}
