package io.oversec.one.crypto;

import android.content.Context;
import android.content.Intent;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;

import java.io.IOException;
import java.security.GeneralSecurityException;


public abstract class AbstractCryptoHandler {
    protected final Context mCtx;

    public AbstractCryptoHandler(Context ctx) {
        mCtx = ctx;
    }

    public abstract int getDisplayEncryptionMethod();


    public abstract Outer.Msg encrypt(Inner.InnerData innerData, AbstractEncryptionParams params, Intent actionIntent) throws GeneralSecurityException, UserInteractionRequiredException, IOException;
    public abstract Outer.Msg encrypt(String plainText, AbstractEncryptionParams params, Intent actionIntent) throws GeneralSecurityException, UserInteractionRequiredException, IOException;

    public abstract BaseDecryptResult decrypt(Outer.Msg msg, Intent actionIntent, String encryptedText) throws UserInteractionRequiredException;

    public abstract AbstractTextEncryptionInfoFragment getTextEncryptionInfoFragment(String packagename);

    public abstract AbstractBinaryEncryptionInfoFragment getBinaryEncryptionInfoFragment(String packagename);

    public abstract AbstractEncryptionParams buildDefaultEncryptionParams(BaseDecryptResult tdr);
}
