package io.oversec.one.crypto.encoding;

import io.oversec.one.crypto.proto.Outer;
import org.junit.Test;

public class Base64XCoderTest extends EncodingTestBase {

    @Override
    AbstractXCoder createCoder() {
        return new Base64XCoder(mContext);
    }

    @Test
    public void testEncodeDecodeGpg() throws Exception {
        Outer.Msg msgIn = createTestOuterMsgPgp();
        String encoded = mCoder.encodeInternal(msgIn);
        Outer.Msg msgOut = mCoder.decode(encoded);
        assertEquals(msgIn, msgOut);
    }

    @Test
    public void testEncodeDecodeSym() throws Exception {
        Outer.Msg msgIn = createTestOuterMsgSym();
        String encoded = mCoder.encodeInternal(msgIn);
        Outer.Msg msgOut = mCoder.decode(encoded);
        assertEquals(msgIn, msgOut);
    }
}