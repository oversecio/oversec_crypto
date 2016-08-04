package io.oversec.one.crypto.symsimple;

import android.content.Context;
import android.content.Intent;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.EncryptionMethod;
import io.oversec.one.crypto.UserInteractionRequiredException;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.KeyNotCachedException;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler;
import io.oversec.one.crypto.symbase.SymmetricDecryptResult;
import io.oversec.one.crypto.symsimple.ui.AddPasswordKeyActivity;
import io.oversec.one.crypto.symsimple.ui.SimpleSymmetricBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.symsimple.ui.SimpleSymmetricTextEncryptionInfoFragment;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;

public class SimpleSymmetricCryptoHandler extends BaseSymmetricCryptoHandler {


    public static final byte[] KEY_DERIVATION_SALT = SymUtil.hexStringToByteArray("B16B00B566DEFEC8DEFEC8B16B00B566"); //constant , this is intentional and can not be avoided for "simple" symmetric encryption
    public static final int KEY_DERIVATION_COST = 8;
    public static final int KEY_ID_COST = 6;

    public SimpleSymmetricCryptoHandler(Context ctx) {
        super(ctx);
    }

    @Override
    protected EncryptionMethod getMethod() {
        return EncryptionMethod.SIMPLESYM;
    }


    @Override
    public AbstractEncryptionParams buildDefaultEncryptionParams(BaseDecryptResult tdr) {
        SymmetricDecryptResult r = (SymmetricDecryptResult) tdr;
        return new SimpleSymmetricEncryptionParams(r.getSymmetricKeyId(), null, null);
    }


    @Override
    public BaseDecryptResult decrypt(Outer.Msg msg, Intent actionIntent, String encryptedText) throws UserInteractionRequiredException {
        return tryDecrypt(msg.getMsgTextSymSimpleV0(), encryptedText);
    }

    @Override
    public AbstractTextEncryptionInfoFragment getTextEncryptionInfoFragment(String packagename) {
        return SimpleSymmetricTextEncryptionInfoFragment.newInstance(packagename);
    }

    @Override
    public AbstractBinaryEncryptionInfoFragment getBinaryEncryptionInfoFragment(String packagename) {
        return SimpleSymmetricBinaryEncryptionInfoFragment.newInstance(packagename);
    }

    @Override
    protected SymmetricKeyPlain getKeyByHashedKeyId(long keyhash, byte[] salt, int cost, String encryptedText) throws KeyNotCachedException {
        return mKeyCache.getKeyByHashedKeyId(keyhash, salt, cost);
    }

    @Override
    protected void handleNoKeyFoundForDecryption(long[] keyHashes, byte[][] salts, int costKeyhash, String encryptedText) throws UserInteractionRequiredException {
        throw buildUserInteractionRequiredException(keyHashes, salts, costKeyhash, encryptedText);
    }

    private UserInteractionRequiredException buildUserInteractionRequiredException(long[] keyHashes, byte[][] salts, int sessionKeyCost, String encryptedText) {
        return new KeyNotCachedException(AddPasswordKeyActivity.buildPendingIntent(mCtx, keyHashes, salts, sessionKeyCost, encryptedText));
    }


    @Override
    protected void setMessage(Outer.Msg.Builder builderMsg, Outer.MsgTextSymV0.Builder symMsgBuilder) {
        builderMsg.setMsgTextSymSimpleV0(symMsgBuilder);
    }


}
