package io.oversec.one.crypto.encoding;

import android.content.Context;
import com.google.protobuf.ByteString;
import io.oversec.one.crypto.BuildConfig;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;
import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public abstract class EncodingTestBase extends TestCase {

    @Mock
    Context mContext;

    AbstractXCoder mCoder = createCoder();

    static final String CONTENT = "Ain't worried about nobody else and nobody came between us, no one could ever come above but i'd rather work on this with you cause i dont want-want nobody when i got-got your body don't let this go to your head i ain't never seen nothing like that i wanna know if you feeling, the way that i'm feelin' i'm here to make you happy, i'm here to see you smile i've been waiting for a girl like you for a long, long, long, long time now oh your love, oh our trust has been broken share stay in my backpack forever take a bow, you're on the hottest ticket now (eh eh eh) there's a dream that i've been chasing we are gonna make it girl when i was little you ain't gotta be afraid.";

    public static final Outer.Msg createTestOuterMsgPgp() {
        Outer.Msg.Builder builderMsg = Outer.Msg.newBuilder();
        Outer.MsgTextGpgV0.Builder pgpMsgBuilder = builderMsg.getMsgTextGpgV0Builder();
        pgpMsgBuilder.setCiphertext(ByteString.copyFromUtf8(CONTENT));
        pgpMsgBuilder.addPubKeyIdV0(666L);
        pgpMsgBuilder.addPubKeyIdV0(777L);

        builderMsg.setMsgTextGpgV0(pgpMsgBuilder);

        Outer.Msg msg = builderMsg.build();
        return msg;
    }

    public static final Outer.Msg createTestOuterMsgSym() {
        Outer.Msg.Builder builderMsg = Outer.Msg.newBuilder();
        Outer.MsgTextSymV0.Builder pgpMsgBuilder = builderMsg.getMsgTextSymV0Builder();
        Outer.MsgTextChaChaV0.Builder builderSym = pgpMsgBuilder.getMsgTextChaChaV0Builder();
        builderSym.setCostKeyhash(SimpleSymmetricCryptoHandler.KEY_ID_COST);


        Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext.Builder perSymPerKey = builderSym.addPerKeyCiphertextBuilder();
        perSymPerKey.setKeyhash(666L);
        perSymPerKey.setIv(getRandomByteString(BaseSymmetricCryptoHandler.IV_LENGTH));
        perSymPerKey.setSalt(getRandomByteString(BaseSymmetricCryptoHandler.SALT_LENGTH));
        perSymPerKey.setCiphertext(getRandomByteString(1234));

        Outer.Msg msg = builderMsg.build();
        return msg;
    }

    private static ByteString getRandomByteString(int length) {
        return ByteString.copyFrom(KeyUtil.getRandomBytes(length));
    }

    abstract AbstractXCoder createCoder();
}
