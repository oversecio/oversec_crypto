package io.oversec.one.crypto.sym;

import android.content.Context;
import android.content.Intent;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.EncryptionMethod;
import io.oversec.one.crypto.UserInteractionRequiredException;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.ui.SymmetricBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.sym.ui.SymmetricTextEncryptionInfoFragment;
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler;
import io.oversec.one.crypto.symbase.SymmetricDecryptResult;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;

public class SymmetricCryptoHandler extends BaseSymmetricCryptoHandler {

    public static final int BCRYPT_FINGERPRINT_COST = 10;

    private final OversecKeystore2 mKeyStore;


    static {
        OversecKeystore2.noop();//init security provider
    }

    public SymmetricCryptoHandler(Context ctx) {
        super(ctx);
        mKeyStore = OversecKeystore2.getInstance(ctx);
    }

    @Override
    protected EncryptionMethod getMethod() {
        return EncryptionMethod.SYM;
    }


    @Override
    public AbstractEncryptionParams buildDefaultEncryptionParams(BaseDecryptResult tdr) {
        SymmetricDecryptResult r = (SymmetricDecryptResult) tdr;
        return new SymmetricEncryptionParams(r.getSymmetricKeyId(), null, null);
    }


    @Override
    public BaseDecryptResult decrypt(Outer.Msg msg, Intent actionIntent, String encryptedText) throws UserInteractionRequiredException {
        return tryDecrypt(msg.getMsgTextSymV0(), encryptedText);
    }

    @Override
    public AbstractTextEncryptionInfoFragment getTextEncryptionInfoFragment(String packagename) {
        return SymmetricTextEncryptionInfoFragment.newInstance(packagename);
    }


    @Override
    public AbstractBinaryEncryptionInfoFragment getBinaryEncryptionInfoFragment(String packagename) {
        return SymmetricBinaryEncryptionInfoFragment.newInstance(packagename);
    }


    @Override
    protected SymmetricKeyPlain getKeyByHashedKeyId(long keyhash, byte[] salt, int cost, String encryptedText) throws KeyNotCachedException {
        Long keyId = mKeyStore.getKeyIdByHashedKeyId(keyhash, salt, cost);
        return keyId == null ? null : mKeyCache.get(keyId);
    }

    @Override
    protected void handleNoKeyFoundForDecryption(long[] keyHashes, byte[][] salts, int costKeyhash, String encryptedText) throws UserInteractionRequiredException {
        //noop
    }

    @Override
    protected void setMessage(Outer.Msg.Builder builderMsg, Outer.MsgTextSymV0.Builder symMsgBuilder) {
        builderMsg.setMsgTextSymV0(symMsgBuilder);
    }


    public boolean hasAnyKey() {
        return !mKeyStore.isEmpty();
    }
}
