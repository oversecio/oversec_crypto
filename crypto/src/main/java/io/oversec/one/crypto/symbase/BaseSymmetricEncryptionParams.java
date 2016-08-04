package io.oversec.one.crypto.symbase;

import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.EncryptionMethod;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSymmetricEncryptionParams extends AbstractEncryptionParams {
    protected List<Long> mKeyIds = new ArrayList<>();

    protected BaseSymmetricEncryptionParams(EncryptionMethod method, String coderId, String padderId) {
        super(method, coderId, padderId);
    }


    public List<Long> getKeyIds() {
        return mKeyIds;
    }


}
