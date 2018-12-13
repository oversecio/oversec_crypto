package io.oversec.one.crypto.encoding;

import android.content.Context;

import com.google.protobuf.ByteString;

import io.oversec.one.crypto.BuildConfig;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.nio.charset.Charset;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public abstract class EncodingTestBase extends TestCase {


    Context mContext;
    AbstractXCoder mCoder;

    static final String CONTENT = "Ain't worried about nobody else and nobody came between us, no one could ever come above but i'd rather work on this with you cause i dont want-want nobody when i got-got your body don't let this go to your head i ain't never seen nothing like that i wanna know if you feeling, the way that i'm feelin' i'm here to make you happy, i'm here to see you smile i've been waiting for a girl like you for a long, long, long, long time now oh your love, oh our trust has been broken share stay in my backpack forever take a bow, you're on the hottest ticket now (eh eh eh) there's a dream that i've been chasing we are gonna make it girl when i was little you ain't gotta be afraid.";
    static final String IV = "ksiwifnsa";
    static final String SALT = "xxiohnzzksiwifnsa";
    static final long KEYHASH = 666L;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mCoder = createCoder();
    }

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
        Outer.MsgTextSymV0.Builder symMsgBuilder = builderMsg.getMsgTextSymV0Builder();
        Outer.MsgTextChaChaV0.Builder chachaMsgBuilder = symMsgBuilder.getMsgTextChaChaV0Builder();

        chachaMsgBuilder.setCostKeyhash(SimpleSymmetricCryptoHandler.KEY_ID_COST);

        Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext.Builder pkcBuilder = chachaMsgBuilder.addPerKeyCiphertextBuilder();

        pkcBuilder.setIv(ByteString.copyFromUtf8(IV));
        pkcBuilder.setSalt(ByteString.copyFromUtf8(SALT));
        pkcBuilder.setKeyhash(KEYHASH);

        pkcBuilder.setCiphertext(ByteString.copyFromUtf8(CONTENT));

        symMsgBuilder.setMsgTextChaChaV0(chachaMsgBuilder);

        builderMsg.setMsgTextSymSimpleV0(symMsgBuilder);

        Outer.Msg msg = builderMsg.build();

        return msg;
    }


//    private static ByteString getRandomByteString(int length) {
//        return ByteString.copyFrom(KeyUtil.getRandomBytes(length));
//    }

    abstract AbstractXCoder createCoder();
}
