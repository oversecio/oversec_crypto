package io.oversec.one.crypto.sym;

import android.content.Context;
import io.oversec.one.crypto.EncryptionMethod;
import io.oversec.one.crypto.symbase.BaseSymmetricEncryptionParams;

import java.util.List;

public class SymmetricEncryptionParams extends BaseSymmetricEncryptionParams {

    protected SymmetricEncryptionParams(String coderId, String padderId) {
        super(EncryptionMethod.SYM, coderId, padderId);
    }

    public SymmetricEncryptionParams(List<Long> keyIds, String coderId, String padderId) {
        this(coderId, padderId);
        if (keyIds != null) {
            mKeyIds = keyIds;
        }
    }

    public SymmetricEncryptionParams(Long keyId, String coderId, String padderId) {
        this(coderId, padderId);
        mKeyIds.add(keyId);
    }


    @Override
    public boolean isStillValid(Context ctx) {
        if (mKeyIds == null) {
            return false;
        }
        OversecKeystore2 ks = OversecKeystore2.getInstance(ctx);
        for (Long keyId : mKeyIds) {
            if (!ks.hasKey(keyId)) {
                return false;
            }
        }
        return true;
    }
}
