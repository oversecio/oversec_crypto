package io.oversec.one.crypto.symbase;

import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.EncryptionMethod;
import io.oversec.one.crypto.proto.Inner;


public class SymmetricDecryptResult extends BaseDecryptResult {


    private Long mSymmetricKeyId;


    public SymmetricDecryptResult(EncryptionMethod method, DecryptError error) {
        this(method, error, null);
    }

    public SymmetricDecryptResult(EncryptionMethod method, DecryptError error, Long keyId) {
        super(method, error);
        this.mSymmetricKeyId = keyId;
    }


    public SymmetricDecryptResult(EncryptionMethod method, Inner.InnerData innerData, Long keyId) {
        super(method, innerData);
        mSymmetricKeyId = keyId;
    }

    public Long getSymmetricKeyId() {
        return this.mSymmetricKeyId;
    }


}