package io.oversec.one.crypto.encoding;

import io.oversec.one.crypto.proto.Outer;
import org.junit.Test;

public class AsciiArmouredGpgXCoderTest extends EncodingTestBase {

    @Override
    AbstractXCoder createCoder() {
        return new AsciiArmouredGpgXCoder(mContext);
    }

    @Test
    public void testEncodeDecode() throws Exception {
        Outer.Msg msgIn = createTestOuterMsgPgp();
        String encoded = mCoder.encodeInternal(msgIn,null,"foo.bar");
        Outer.Msg msgOut = mCoder.decode(encoded);

        assertEquals(msgOut.getMsgTextGpgV0().getCiphertext().toStringUtf8(), CONTENT);
        assertTrue(msgOut.getMsgTextGpgV0().getPubKeyIdV0List().isEmpty()); //AsciiArmour encoding strips this info!
    }


    @Test
    public void testDecodePreKotlinization() throws Exception {
        String encoded = "-----BEGIN PGP MESSAGE-----\n" +
                "Charset: utf-8\n" +
                "\n" +
                "QWluJ3Qgd29ycmllZCBhYm91dCBub2JvZHkgZWxzZSBhbmQgbm9ib2R5IGNhbWUg\n" +
                "YmV0d2VlbiB1cywgbm8gb25lIGNvdWxkIGV2ZXIgY29tZSBhYm92ZSBidXQgaSdk\n" +
                "IHJhdGhlciB3b3JrIG9uIHRoaXMgd2l0aCB5b3UgY2F1c2UgaSBkb250IHdhbnQt\n" +
                "d2FudCBub2JvZHkgd2hlbiBpIGdvdC1nb3QgeW91ciBib2R5IGRvbid0IGxldCB0\n" +
                "aGlzIGdvIHRvIHlvdXIgaGVhZCBpIGFpbid0IG5ldmVyIHNlZW4gbm90aGluZyBs\n" +
                "aWtlIHRoYXQgaSB3YW5uYSBrbm93IGlmIHlvdSBmZWVsaW5nLCB0aGUgd2F5IHRo\n" +
                "YXQgaSdtIGZlZWxpbicgaSdtIGhlcmUgdG8gbWFrZSB5b3UgaGFwcHksIGknbSBo\n" +
                "ZXJlIHRvIHNlZSB5b3Ugc21pbGUgaSd2ZSBiZWVuIHdhaXRpbmcgZm9yIGEgZ2ly\n" +
                "bCBsaWtlIHlvdSBmb3IgYSBsb25nLCBsb25nLCBsb25nLCBsb25nIHRpbWUgbm93\n" +
                "IG9oIHlvdXIgbG92ZSwgb2ggb3VyIHRydXN0IGhhcyBiZWVuIGJyb2tlbiBzaGFy\n" +
                "ZSBzdGF5IGluIG15IGJhY2twYWNrIGZvcmV2ZXIgdGFrZSBhIGJvdywgeW91J3Jl\n" +
                "IG9uIHRoZSBob3R0ZXN0IHRpY2tldCBub3cgKGVoIGVoIGVoKSB0aGVyZSdzIGEg\n" +
                "ZHJlYW0gdGhhdCBpJ3ZlIGJlZW4gY2hhc2luZyB3ZSBhcmUgZ29ubmEgbWFrZSBp\n" +
                "dCBnaXJsIHdoZW4gaSB3YXMgbGl0dGxlIHlvdSBhaW4ndCBnb3R0YSBiZSBhZnJh\n" +
                "aWQu\n" +
                "=XhVo\n" +
                "-----END PGP MESSAGE-----\n" +
                "\n";

        Outer.Msg msgOut = mCoder.decode(encoded);

        assertEquals(msgOut.getMsgTextGpgV0().getCiphertext().toStringUtf8(), CONTENT);
        assertTrue(msgOut.getMsgTextGpgV0().getPubKeyIdV0List().isEmpty()); //AsciiArmour encoding strips this info!
    }
}