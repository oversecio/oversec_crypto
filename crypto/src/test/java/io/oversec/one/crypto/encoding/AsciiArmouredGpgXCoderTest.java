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

}