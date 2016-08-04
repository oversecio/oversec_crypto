package io.oversec.one.crypto.symbase;


import io.oversec.one.crypto.sym.KeyNotCachedException;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import org.spongycastle.crypto.generators.BCrypt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;

public class KeyUtil {
    public static final int BCRYPT_PBKDF_COST = 12; //for generating keys from passphrases

    public static final int BCRYPT_SESSIONKEYID_COST_DEFAULT = 5;
    public static final int DEFAULT_KEYSTORAGE_BCRYPT_COST = 10;

    private static final byte[] BCRYPT_FINGERPRINT_CONSTANT_SALT = SymUtil.hexStringToByteArray("DEFEC8B16B00B566DEFEC8B16B00B566");

    public static final SecureRandom secureRandom = new SecureRandom();
    public static final int GENERATE_RANDOM_KEYS_BYTES = 32;


    public static long calcSessionKeyId(long fingerprint, byte[] salt, int cost) {
        byte[] input = SymUtil.long2bytearray(fingerprint);
        byte[] hashed = BCrypt.generate(input, salt, cost);

        //first 64 bit are our sessionKeyId
        long res = SymUtil.bytearray2long(hashed);

        return res;
    }

    public static long calcKeyId(byte[] plain, int cost) throws NoSuchAlgorithmException {

        //calc 384 bit hash
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        byte[] rawHashed = md.digest(plain);


        //use final 128 bit for calculating fingerprint
        byte[] sha256To384 = new byte[16];
        System.arraycopy(rawHashed, 32, sha256To384, 0, 16);


        //This should take a bit, in fact it should take around 5 .. 10 seconds
        byte[] b = BCrypt.generate(sha256To384, BCRYPT_FINGERPRINT_CONSTANT_SALT, cost);

        //use first 8 bytes as fingerprint
        long fp = SymUtil.bytearray2long(b);

        erase(plain);
        erase(rawHashed);
        erase(sha256To384);

        return fp;
    }

    private static byte[] getBaseKey(byte[] secretRaw) throws NoSuchAlgorithmException {


        //calc 384 bit hash
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        byte[] rawHashed = md.digest(secretRaw);

        //use first 256 bit as the key
        byte[] sha0To255 = new byte[32];
        System.arraycopy(rawHashed, 0, sha0To255, 0, 32);

        erase(rawHashed);

        return sha0To255;
    }

    public static void erase(byte[] bb) {
        if (bb != null)
            Arrays.fill(bb, (byte) 0);
    }

    public static void erase(char[] bb) {
        if (bb != null)
            Arrays.fill(bb, (char) 0);
    }

    public static synchronized byte[] encryptSymmetricChaCha(byte[] plain, byte[] salt, byte[] ivx, SymmetricKeyPlain k) throws IOException, KeyNotCachedException, NoSuchAlgorithmException {
        byte[] hashedKey = getEncryptionKey(k.getRaw(), salt);
        byte[] ciphertext = OversecChacha20Poly1305.enChacha(plain, hashedKey, ivx);
        return ciphertext;
    }

    public static byte[] decryptSymmetricChaCha(byte[] ciphertext, byte[] salt, byte[] ivx, SymmetricKeyPlain k) throws NoSuchAlgorithmException, IOException, OversecChacha20Poly1305.MacMismatchException {
        byte[] hashedKey = getEncryptionKey(k.getRaw(), salt);
        byte[] plaintext = OversecChacha20Poly1305.deChacha(ciphertext, hashedKey, ivx);
        return plaintext;
    }

    private static byte[] getEncryptionKey(byte[] raw, byte[] salt) throws NoSuchAlgorithmException {
        byte[] base = getBaseKey(raw);
        byte[] keybytes = new byte[32]; //256 bit

        byte[] hash1 = BCrypt.generate(base, salt, 4);
        //use first 128 bit for first part of key
        System.arraycopy(hash1, 0, keybytes, 0, 16);

        //need more entropy, hash again, using last 16 bytes  of initial hash as the salt
        byte[] salt2 = new byte[salt.length];
        System.arraycopy(hash1, 8, salt2, 0, salt2.length);

        byte[] hash2 = BCrypt.generate(base, salt2, 4);
        //use first 128 bit for last part of key
        System.arraycopy(hash1, 0, keybytes, 16, 16);

        erase(hash1);
        erase(hash2);
        erase(salt2);
        erase(base);

        return keybytes;
    }


    public static byte[] getRandomBytes(int length) {
        byte[] randomByteArray = new byte[length];
        secureRandom.nextBytes(randomByteArray);

        return randomByteArray;
    }

    public static byte[] brcryptifyPassword(char[] plain, byte[] salt, int cost, int outputBytes) {

        byte[] chars = toBytes(plain);
        erase(plain);

        byte[] hash = BCrypt.generate(chars, salt, cost);

        byte[] res = new byte[outputBytes];

        int transferred = 0;
        int transfer = Math.min(hash.length, res.length);

        System.arraycopy(hash, 0, res, 0, transfer);
        transferred += transfer;

        while (transferred < outputBytes) {
            byte[] salt2 = new byte[salt.length];
            System.arraycopy(hash, 0, salt2, 0, salt2.length);

            hash = BCrypt.generate(chars, salt2, cost);

            transfer = Math.min(hash.length, outputBytes - transferred);
            System.arraycopy(hash, 0, res, transferred, transfer);
            transferred += transfer;
        }

        erase(hash);
        erase(chars);

        return res;
    }


    public static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static SymmetricKeyPlain createNewKey() {
        byte bytes[] = KeyUtil.getRandomBytes(GENERATE_RANDOM_KEYS_BYTES);
        SymmetricKeyPlain res = new SymmetricKeyPlain();
        res.setCreatedDate(new Date());
        res.setRaw(bytes);
        return res;
    }


}
