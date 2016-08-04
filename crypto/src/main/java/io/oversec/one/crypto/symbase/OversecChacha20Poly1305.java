package io.oversec.one.crypto.symbase;


import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Mac;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.crypto.generators.Poly1305KeyGenerator;
import org.spongycastle.crypto.macs.Poly1305;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Pack;

import java.io.IOException;

public class OversecChacha20Poly1305 {
    private static final byte[] ZEROES = new byte[15];


    public static byte[] enChacha(byte[] raw, byte[] key, byte[] iv) {
        boolean encrypt = true;

        CipherParameters cp = new KeyParameter(key);
        ParametersWithIV params = new ParametersWithIV(cp, iv);
        StreamCipher engine = new ChaChaEngine();
        //noinspection ConstantConditions
        engine.init(encrypt, params);

        byte[] ciphertext = new byte[raw.length];
        engine.processBytes(raw, 0, raw.length, ciphertext, 0);


        byte[] macKeyBytes = Arrays.copyOf(key, key.length);
        Poly1305KeyGenerator.clamp(macKeyBytes);
        KeyParameter macKey = new KeyParameter(macKeyBytes);  //initRecord(engine, encrypt, 0, iv);
        byte[] mac = calculateMAC(macKey, ciphertext, 0, ciphertext.length);

        byte[] res = new byte[ciphertext.length + mac.length];

        System.arraycopy(ciphertext, 0, res, 0, ciphertext.length);
        System.arraycopy(mac, 0, res, ciphertext.length, mac.length);

        KeyUtil.erase(ciphertext);
        KeyUtil.erase(mac);

        return res;
    }


    public static byte[] deChacha(byte[] ciphertext, byte[] key, byte[] iv) throws MacMismatchException {
        boolean encrypt = false;

        CipherParameters cp = new KeyParameter(key);
        ParametersWithIV params = new ParametersWithIV(cp, iv);
        StreamCipher engine = new ChaChaEngine();
        //noinspection ConstantConditions
        engine.init(encrypt, params);
        if (getPlaintextLimit(ciphertext.length) < 0) {
            throw new IllegalArgumentException();
        }

        byte[] macKeyBytes = Arrays.copyOf(key, key.length);
        Poly1305KeyGenerator.clamp(macKeyBytes);
        KeyParameter macKey = new KeyParameter(macKeyBytes);  //initRecord(engine, encrypt, 0, iv);

        int plaintextLength = ciphertext.length - 16;


        byte[] calculatedMAC = calculateMAC(macKey, ciphertext, 0, plaintextLength);
        byte[] receivedMAC = Arrays.copyOfRange(ciphertext, ciphertext.length - 16, ciphertext.length);

        if (!Arrays.constantTimeAreEqual(calculatedMAC, receivedMAC)) {
            throw new MacMismatchException();
        }

        byte[] output = new byte[plaintextLength];
        engine.processBytes(ciphertext, 0, plaintextLength, output, 0);

        KeyUtil.erase(calculatedMAC);
        KeyUtil.erase(receivedMAC);

        return output;
    }

    private static int getPlaintextLimit(int ciphertextLimit) {
        return ciphertextLimit - 16;
    }

    private static KeyParameter initRecord(StreamCipher cipher, boolean forEncryption, long seqNo, byte[] iv) {
        cipher.init(forEncryption, new ParametersWithIV(null, iv));
        return generateRecordMACKey(cipher);
    }

    private static KeyParameter generateRecordMACKey(StreamCipher cipher) {
        byte[] firstBlock = new byte[64];
        cipher.processBytes(firstBlock, 0, firstBlock.length, firstBlock, 0);

        KeyParameter macKey = new KeyParameter(firstBlock, 0, 32);
        Arrays.fill(firstBlock, (byte) 0);
        return macKey;
    }


    private static byte[] calculateMAC(KeyParameter macKey, byte[] buf, int off, int len) {
        Mac mac = new Poly1305();
        mac.init(macKey);

        updateRecordMACText(mac, buf, off, len);
        updateRecordMACLength(mac, len);

        byte[] output = new byte[mac.getMacSize()];
        mac.doFinal(output, 0);
        return output;
    }

    private static void updateRecordMACLength(Mac mac, int len) {
        byte[] longLen = Pack.longToLittleEndian(len & 0xFFFFFFFFL);
        mac.update(longLen, 0, longLen.length);
    }

    private static void updateRecordMACText(Mac mac, byte[] buf, int off, int len) {
        mac.update(buf, off, len);

        int partial = len % 16;
        if (partial != 0) {
            mac.update(ZEROES, 0, 16 - partial);
        }
    }


    public static class MacMismatchException extends Exception {
    }
}
