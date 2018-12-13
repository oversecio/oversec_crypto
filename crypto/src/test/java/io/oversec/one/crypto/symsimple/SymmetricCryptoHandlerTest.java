package io.oversec.one.crypto.symsimple;

import io.oversec.one.crypto.AbstractCryptoHandler;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.sym.SymmetricCryptoHandler;
import io.oversec.one.crypto.sym.SymmetricEncryptionParams;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symbase.OversecKeyCacheListener;
import org.junit.Test;

import java.util.Date;

public class SymmetricCryptoHandlerTest extends CryptoHandlerTestBase {
    private static final char[] PASSWORD = "passwordpassword1".toCharArray();

    private final OversecKeystore2 mKeyStore;

    public SymmetricCryptoHandlerTest() {
        mKeyStore = OversecKeystore2.Companion.getInstance(mContext);
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        long key_id = 12345L;
        final byte[] rawKeyBytes = KeyUtil.INSTANCE.getRandomBytes(32);

        SymmetricKeyPlain plainKey = new SymmetricKeyPlain(key_id, "foobar", new Date(), rawKeyBytes);

        key_id = mKeyStore.addKey__longoperation(plainKey,PASSWORD);

        mKeyStore.doCacheKey__longoperation(key_id,PASSWORD,Integer.MAX_VALUE);

        AbstractEncryptionParams params = new SymmetricEncryptionParams(key_id, null, null);


        Inner.InnerData innerData = createInnerData(PLAIN_CONTENT);
        Outer.Msg enc = mHandler.encrypt(innerData, params, null);

        BaseDecryptResult decryptResult = mHandler.decrypt(enc, null, "some dummy text");

        assertTrue(decryptResult.isOk());

        assertEquals(decryptResult.getDecryptedDataAsInnerData(), innerData);
        assertEquals(decryptResult.getDecryptedDataAsInnerData().getTextAndPaddingV0().getText(), PLAIN_CONTENT);

    }


    @Override
    AbstractCryptoHandler createHandler() {
        return new SymmetricCryptoHandler(mContext);
    }
}