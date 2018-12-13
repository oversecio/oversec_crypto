package io.oversec.one.crypto.encoding;

import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;

import org.junit.Test;

public class Base64XCoderTest extends EncodingTestBase {

    @Override
    AbstractXCoder createCoder() {
        return new Base64XCoder(mContext);
    }

    @Test
    public void testEncodeDecodeGpg() throws Exception {
        Outer.Msg msgIn = createTestOuterMsgPgp();
        String encoded = mCoder.encodeInternal(msgIn,null,"foo.bar");
        Outer.Msg msgOut = mCoder.decode(encoded);
        assertEquals(msgIn, msgOut);
    }

    @Test
    public void testDecodeGpgPreKotlinization() throws Exception {
        String encoded = "T1NDggG4BQqjBUFpbid0IHdvcnJpZWQgYWJvdXQgbm9ib2R5IGVsc2UgYW5kIG5vYm9keSBjYW1lIGJldHdlZW4gdXMsIG5vIG9uZSBjb3VsZCBldmVyIGNvbWUgYWJvdmUgYnV0IGknZCByYXRoZXIgd29yayBvbiB0aGlzIHdpdGggeW91IGNhdXNlIGkgZG9udCB3YW50LXdhbnQgbm9ib2R5IHdoZW4gaSBnb3QtZ290IHlvdXIgYm9keSBkb24ndCBsZXQgdGhpcyBnbyB0byB5b3VyIGhlYWQgaSBhaW4ndCBuZXZlciBzZWVuIG5vdGhpbmcgbGlrZSB0aGF0IGkgd2FubmEga25vdyBpZiB5b3UgZmVlbGluZywgdGhlIHdheSB0aGF0IGknbSBmZWVsaW4nIGknbSBoZXJlIHRvIG1ha2UgeW91IGhhcHB5LCBpJ20gaGVyZSB0byBzZWUgeW91IHNtaWxlIGkndmUgYmVlbiB3YWl0aW5nIGZvciBhIGdpcmwgbGlrZSB5b3UgZm9yIGEgbG9uZywgbG9uZywgbG9uZywgbG9uZyB0aW1lIG5vdyBvaCB5b3VyIGxvdmUsIG9oIG91ciB0cnVzdCBoYXMgYmVlbiBicm9rZW4gc2hhcmUgc3RheSBpbiBteSBiYWNrcGFjayBmb3JldmVyIHRha2UgYSBib3csIHlvdSdyZSBvbiB0aGUgaG90dGVzdCB0aWNrZXQgbm93IChlaCBlaCBlaCkgdGhlcmUncyBhIGRyZWFtIHRoYXQgaSd2ZSBiZWVuIGNoYXNpbmcgd2UgYXJlIGdvbm5hIG1ha2UgaXQgZ2lybCB3aGVuIGkgd2FzIGxpdHRsZSB5b3UgYWluJ3QgZ290dGEgYmUgYWZyYWlkLhGaAgAAAAAAABEJAwAAAAAAAA";
        Outer.Msg msgOut = mCoder.decode(encoded);
        assertEquals(msgOut.getMsgTextGpgV0().getCiphertext().toStringUtf8(), CONTENT);
    }

    @Test
    public void testEncodeDecodeSym() throws Exception {
        Outer.Msg msgIn = createTestOuterMsgSym();
        String encoded = mCoder.encodeInternal(msgIn,null,"foo.bar");
        Outer.Msg msgOut = mCoder.decode(encoded);
        assertEquals(msgIn, msgOut);
    }

    @Test
    public void testDecodeSymPreKotlinization() throws Exception {
        String encoded = "T1NDEtUFCtIFCAYSzQUJmgIAAAAAAAASEXh4aW9obnp6a3Npd2lmbnNhGglrc2l3aWZuc2EiowVBaW4ndCB3b3JyaWVkIGFib3V0IG5vYm9keSBlbHNlIGFuZCBub2JvZHkgY2FtZSBiZXR3ZWVuIHVzLCBubyBvbmUgY291bGQgZXZlciBjb21lIGFib3ZlIGJ1dCBpJ2QgcmF0aGVyIHdvcmsgb24gdGhpcyB3aXRoIHlvdSBjYXVzZSBpIGRvbnQgd2FudC13YW50IG5vYm9keSB3aGVuIGkgZ290LWdvdCB5b3VyIGJvZHkgZG9uJ3QgbGV0IHRoaXMgZ28gdG8geW91ciBoZWFkIGkgYWluJ3QgbmV2ZXIgc2VlbiBub3RoaW5nIGxpa2UgdGhhdCBpIHdhbm5hIGtub3cgaWYgeW91IGZlZWxpbmcsIHRoZSB3YXkgdGhhdCBpJ20gZmVlbGluJyBpJ20gaGVyZSB0byBtYWtlIHlvdSBoYXBweSwgaSdtIGhlcmUgdG8gc2VlIHlvdSBzbWlsZSBpJ3ZlIGJlZW4gd2FpdGluZyBmb3IgYSBnaXJsIGxpa2UgeW91IGZvciBhIGxvbmcsIGxvbmcsIGxvbmcsIGxvbmcgdGltZSBub3cgb2ggeW91ciBsb3ZlLCBvaCBvdXIgdHJ1c3QgaGFzIGJlZW4gYnJva2VuIHNoYXJlIHN0YXkgaW4gbXkgYmFja3BhY2sgZm9yZXZlciB0YWtlIGEgYm93LCB5b3UncmUgb24gdGhlIGhvdHRlc3QgdGlja2V0IG5vdyAoZWggZWggZWgpIHRoZXJlJ3MgYSBkcmVhbSB0aGF0IGkndmUgYmVlbiBjaGFzaW5nIHdlIGFyZSBnb25uYSBtYWtlIGl0IGdpcmwgd2hlbiBpIHdhcyBsaXR0bGUgeW91IGFpbid0IGdvdHRhIGJlIGFmcmFpZC4";
        Outer.Msg msgOut = mCoder.decode(encoded);
        assertEquals(msgOut.getMsgTextSymSimpleV0().getMsgTextChaChaV0().getCostKeyhash(), SimpleSymmetricCryptoHandler.KEY_ID_COST);
        assertEquals(1,msgOut.getMsgTextSymSimpleV0().getMsgTextChaChaV0().getPerKeyCiphertextCount());
        Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext c1 = msgOut.getMsgTextSymSimpleV0().getMsgTextChaChaV0().getPerKeyCiphertext(0);
        assertEquals(c1.getSalt().toStringUtf8(),SALT);
        assertEquals(c1.getIv().toStringUtf8(),IV);
        assertEquals(c1.getCiphertext().toStringUtf8(),CONTENT);
    }
}