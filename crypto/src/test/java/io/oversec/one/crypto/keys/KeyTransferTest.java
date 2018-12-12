package io.oversec.one.crypto.keys;

import android.content.Context;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Date;

import io.oversec.one.crypto.AbstractCryptoHandler;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.Utils;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.sym.SymmetricKeyEncrypted;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.KeyCache;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symsimple.CryptoHandlerTestBase;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;
import io.oversec.one.crypto.symsimple.SimpleSymmetricEncryptionParams;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public class KeyTransferTest extends TestCase {

    private final OversecKeystore2 mKeystore;
    Context mContext;

    public KeyTransferTest() {
        mContext = RuntimeEnvironment.application;
        mKeystore = OversecKeystore2.getInstance(mContext);
    }

    @Test
    public void testSerializeDeserialize() throws Exception {

        String rawKeyBytesS = "CAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBA";
        char[] password = new char[] {'p','a','s','s'};

        final byte[] rawKeyBytes = Utils.hexToBytes(rawKeyBytesS);

        SymmetricKeyPlain plainKey = new SymmetricKeyPlain(rawKeyBytes);
        Long idd = mKeystore.addKey__longoperation(plainKey, password);
        byte[] data = mKeystore.getPlainKeyAsTransferBytes(idd);

        SymmetricKeyPlain plainKey2 = mKeystore.getPlainKeyFromTransferBytes(data);
        assertNotNull(plainKey2);

        assertEquals(Utils.bytesToHex(plainKey.getRaw()), Utils.bytesToHex(plainKey2.getRaw()));
    }


    @Test
    public void testSerializeEncryptDecryptDeserialize() throws Exception {

        String rawKeyBytesS = "CAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBA";
        char[] password = new char[] {'p','a','s','s'};
        char[] password2 = Arrays.copyOf(password,password.length); //need a copy as passwords get nulled out

        final long key_id = 12345L;
        final byte[] rawKeyBytes = Utils.hexToBytes(rawKeyBytesS);

        SymmetricKeyPlain plainKey = new SymmetricKeyPlain(key_id, "foobar", new Date(), rawKeyBytes);

        SymmetricKeyEncrypted encKey = mKeystore.encryptSymmetricKey(plainKey, password);
        byte[] data = mKeystore.getEncryptedKeyAsTransferBytes(encKey);

        SymmetricKeyEncrypted encKey2 = mKeystore.getEncryptedKeyFromTransferBytes(data);
        assertNotNull(encKey2);

        assertEquals(Utils.bytesToHex(encKey.getCiphertext()),Utils.bytesToHex(encKey2.getCiphertext()));


        SymmetricKeyPlain plainKey2 = mKeystore.decryptSymmetricKey(encKey2, password2);
        assertNotNull(plainKey2);

        assertEquals(Utils.bytesToHex(plainKey.getRaw()), Utils.bytesToHex(plainKey2.getRaw()));
        assertEquals(plainKey.getId(), plainKey2.getId());


    }


    @Test
    public void testDecryptDeserializeFromPreviousVersion() throws Exception {
        String ser = "12680939300000000000001206666F6F62617218C3E09D87FA2C200A2A10E2BE19685F867A32819218555530AF0A32082B87F607E084B5343A30A3E5C7BC88F15CB18549E9001F2804972420C12A93199630E1DC0888DC987C7E4210C4C524E005D214A457E28ADEA381";

        String rawKeyBytesS = "CAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBACAFFEEBA";
        char[] password = new char[] {'p','a','s','s'};

        final long key_id = 12345L;
        final byte[] rawKeyBytes = Utils.hexToBytes(rawKeyBytesS);

        SymmetricKeyEncrypted encKey = mKeystore.getEncryptedKeyFromTransferBytes(Utils.hexToBytes(ser));
        assertNotNull(encKey);

        SymmetricKeyPlain plainKey = mKeystore.decryptSymmetricKey(encKey, password);
        assertNotNull(plainKey);

        assertEquals(rawKeyBytesS, Utils.bytesToHex(plainKey.getRaw()));
        assertEquals(key_id, plainKey.getId());


    }
}