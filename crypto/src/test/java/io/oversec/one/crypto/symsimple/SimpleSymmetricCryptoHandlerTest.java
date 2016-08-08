package io.oversec.one.crypto.symsimple;

import io.oversec.one.crypto.AbstractCryptoHandler;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.KeyUtil;
import org.junit.Test;

import java.util.Date;

public class SimpleSymmetricCryptoHandlerTest extends CryptoHandlerTestBase {


    @Test
    public void testEncryptDecrypt() throws Exception {
        final long key_id = 12345L;
        final byte[] rawKeyBytes = KeyUtil.getRandomBytes(32);

        SymmetricKeyPlain plainKey = new SymmetricKeyPlain(key_id, "foobar", new Date(), rawKeyBytes);

        mKeyCache.doCacheKey(plainKey, Integer.MAX_VALUE);

        AbstractEncryptionParams params = new SimpleSymmetricEncryptionParams(key_id, null, null);


        Inner.InnerData innerData = createInnerData(PLAIN_CONTENT);
        Outer.Msg enc = mHandler.encrypt(innerData, params, null);

        BaseDecryptResult decryptResult = mHandler.decrypt(enc, null, "some dummy text");

        assertTrue(decryptResult.isOk());

        assertEquals(decryptResult.getDecryptedData(), innerData);
        assertEquals(decryptResult.getDecryptedData().getTextAndPaddingV0().getText(), PLAIN_CONTENT);

    }


    @Override
    AbstractCryptoHandler createHandler() {
        return new SimpleSymmetricCryptoHandler(mContext);
    }
}