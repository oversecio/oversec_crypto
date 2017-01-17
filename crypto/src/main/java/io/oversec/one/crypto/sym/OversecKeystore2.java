package io.oversec.one.crypto.sym;

import android.content.Context;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.aztec.decoder.Decoder;
import io.oversec.one.crypto.proto.Kex;
import io.oversec.one.crypto.symbase.KeyCache;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symbase.OversecChacha20Poly1305;
import io.oversec.one.crypto.symbase.OversecKeyCacheListener;
import net.rehacktive.waspdb.WaspDb;
import net.rehacktive.waspdb.WaspFactory;
import net.rehacktive.waspdb.WaspHash;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.DecoderException;
import roboguice.util.Ln;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.*;

public final class OversecKeystore2 {


    private static final String DATABASE_NAME = "keystore";


    private static OversecKeystore2 mINSTANCE;

    @SuppressWarnings("FieldCanBeLocal")
    private final Context mCtx;
    private KeyCache mKeyCache;


    @SuppressWarnings("FieldCanBeLocal")
    private final WaspDb mDb;
    private final WaspHash mSymmetricEncryptedKeys;


    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private List<KeyStoreListener> mListeners = new ArrayList<>();


    @SuppressWarnings("EmptyMethod")
    public static void noop() {
        //just a dummy method we can call in order to make sure the static code get's initialized
    }


    public static synchronized void init(Context ctx) {
        if (mINSTANCE != null) {
            Ln.w("keystore already inited!");
        }

        mINSTANCE = new OversecKeystore2(ctx);
    }

    public static synchronized OversecKeystore2 getInstance(Context ctx) {
        if (mINSTANCE == null) {
            init(ctx.getApplicationContext());
        }

        return mINSTANCE;

    }

    private OversecKeystore2(Context ctx) {

        mCtx = ctx;

        mDb = WaspFactory.openOrCreateDatabase(ctx.getFilesDir().getPath(), DATABASE_NAME, null);
        mSymmetricEncryptedKeys = mDb.openOrCreateHash("symmetric_keys");

        mKeyCache = KeyCache.getInstance(ctx);

    }


    public void clearAllCaches() {
        mKeyCache.clearAll();

        //TODO more?
    }


    public synchronized Long addKey__longoperation(SymmetricKeyPlain plainKey, char[] password) throws
            NoSuchAlgorithmException,
            IOException, AliasNotUniqueException {

        List<SymmetricKeyEncrypted> v = mSymmetricEncryptedKeys.getAllValues();
        v=v==null?v:Collections.EMPTY_LIST;

        for (SymmetricKeyEncrypted k : v) {
            if (k.getName().equals(plainKey.getName())) {
                throw new AliasNotUniqueException(plainKey.getName());
            }
        }

        long id = KeyUtil.calcKeyId(Arrays.copyOf(plainKey.getRaw(), plainKey.getRaw().length), SymmetricCryptoHandler.BCRYPT_FINGERPRINT_COST);
        plainKey.setId(id);
        SymmetricKeyEncrypted encKey = encryptSymmetricKey(plainKey, password);


        mSymmetricEncryptedKeys.put(id, encKey);

        mKeyCache.doCacheKey(plainKey, 0);

        fireChange();

        return id;
    }


    public synchronized void confirmKey(Long id) {
        SymmetricKeyEncrypted k = getSymmetricKeyEncrypted(id);

        k.setConfirmedDate(new Date());
        mSymmetricEncryptedKeys.put(id, k);
    }

    public synchronized Date getConfirmDate(Long id) {
        return getSymmetricKeyEncrypted(id).getConfirmedDate();
    }

    public synchronized Date getCreatedDate(Long id) {
        SymmetricKeyEncrypted k = mSymmetricEncryptedKeys.get(id);
        if (k == null) {
            return null;
        } else {
            return k.getCreatedDate();
        }

    }

    public synchronized void deleteKey(Long id) {
        mSymmetricEncryptedKeys.remove(id);
        fireChange();
    }


    public synchronized Long getKeyIdByHashedKeyId(long hashedKeyId, byte[] salt, int cost) {

        List<Long> allIds = mSymmetricEncryptedKeys.getAllKeys();
        if (allIds!=null) {
            for (Long id : allIds) {
                long aSessionKeyId = KeyUtil.calcSessionKeyId(id, salt, cost);

                if (aSessionKeyId == hashedKeyId) {
                    return id;
                }
            }
        }

        return null;

    }


    public synchronized boolean hasKey(Long keyId) {
        return mSymmetricEncryptedKeys.get(keyId) != null;
    }


    public synchronized byte[] getPlainKeyAsTransferBytes(Long id) throws KeyNotCachedException {
        SymmetricKeyPlain k = mKeyCache.get(id);
        return getPlainKeyAsTransferBytes(k.getRaw());
    }


    public static byte[] getPlainKeyAsTransferBytes(byte[] raw) {
        Kex.KeyTransferV0.Builder builder = Kex.KeyTransferV0.newBuilder();
        Kex.SymmetricKeyPlainV0.Builder plainKeyBuilder = builder.getSymmetricKeyPlainV0Builder();
        plainKeyBuilder.setKeydata(ByteString.copyFrom(raw));
        byte[] payload = builder.build().toByteArray();
        return payload;
    }

    public static byte[] getEncryptedKeyAsTransferBytes(SymmetricKeyEncrypted key) {
        Kex.KeyTransferV0.Builder builder = Kex.KeyTransferV0.newBuilder();
        Kex.SymmetricKeyEncryptedV0.Builder encryptedKeyBuilder = builder.getSymmetricKeyEncryptedV0Builder();
        encryptedKeyBuilder.setId(key.getId());
        encryptedKeyBuilder.setAlias(key.getName());
        encryptedKeyBuilder.setCreateddate(key.getCreatedDate().getTime());
        encryptedKeyBuilder.setCost(key.getCost());
        encryptedKeyBuilder.setIv(ByteString.copyFrom(key.getIv()));
        encryptedKeyBuilder.setSalt(ByteString.copyFrom(key.getSalt()));
        encryptedKeyBuilder.setCiphertext(ByteString.copyFrom(key.getCiphertext()));
        byte[] payload = builder.build().toByteArray();
        return payload;

    }

    public static SymmetricKeyEncrypted getEncryptedKeyFromBase64Text(String text) throws Base64DecodingException {
        try {
            byte[] data = Base64.decode(text);
            return getEncryptedKeyFromTransferBytes(data);
        }
        catch (DecoderException ex) {
            throw new Base64DecodingException(ex);
        }
    }

    public static SymmetricKeyEncrypted getEncryptedKeyFromTransferBytes(byte[] data) {
        try {
            Kex.KeyTransferV0 transfer = Kex.KeyTransferV0
                    .parseFrom(data);


            if (transfer.hasSymmetricKeyEncryptedV0()) {
                Kex.SymmetricKeyEncryptedV0 encryptedKeyV0 = transfer.getSymmetricKeyEncryptedV0();
                int cost = encryptedKeyV0.getCost();
                byte[] ciphertext = encryptedKeyV0.getCiphertext().toByteArray();
                byte[] salt = encryptedKeyV0.getSalt().toByteArray();
                byte[] iv = encryptedKeyV0.getIv().toByteArray();
                long created = encryptedKeyV0.getCreateddate();
                String alias = encryptedKeyV0.getAlias();
                long id = encryptedKeyV0.getId();
                return new SymmetricKeyEncrypted(id, alias, new Date(created), salt, iv, cost, ciphertext);
            } else {
                Ln.w("data array doesn't contain secret key");
                return null;
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SymmetricKeyPlain getPlainKeyFromBase64Text(String text) throws Base64DecodingException {
        try {
            byte[] data = Base64.decode(text);
            return getPlainKeyFromTransferBytes(data);
        }
        catch (DecoderException ex) {
            throw new Base64DecodingException(ex);
        }
    }


    public static SymmetricKeyPlain getPlainKeyFromTransferBytes(byte[] data) {

        try {
            Kex.KeyTransferV0 transfer = Kex.KeyTransferV0
                    .parseFrom(data);

            if (transfer.hasSymmetricKeyPlainV0()) {
                Kex.SymmetricKeyPlainV0 plainKeyV0 = transfer.getSymmetricKeyPlainV0();
                byte[] keyBytes = plainKeyV0.getKeydata().toByteArray();
                return new SymmetricKeyPlain(keyBytes);
            } else {
                Ln.w("data array doesn't contain secret key");
                return null;
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void doCacheKey__longoperation(Long keyId, char[] pw, long ttl) throws IOException, OversecChacha20Poly1305.MacMismatchException {
        SymmetricKeyEncrypted k = mSymmetricEncryptedKeys.get(keyId);
        if (k == null) {
            throw new IllegalArgumentException("invalid key id");
        }

        SymmetricKeyPlain dec = null; //it might still/already be cached
        try {
            dec = mKeyCache.get(keyId);
        } catch (KeyNotCachedException e) {
            //ignore
        }
        if (dec == null) {
            dec = decryptSymmetricKey(k, pw);
        }
        mKeyCache.doCacheKey(dec, ttl);
    }


    public synchronized byte[] getPlainKeyData(Long id) throws KeyNotCachedException {

        SymmetricKeyPlain k = mKeyCache.get(id);
        return k.getRaw();

    }

    public synchronized SymmetricKeyPlain getPlainKey(Long id) throws KeyNotCachedException {

        SymmetricKeyPlain k = mKeyCache.get(id);
        return k;

    }

    public synchronized void addKeyCacheListener(OversecKeyCacheListener l) {
        mKeyCache.addKeyCacheListener(l);
    }

    public synchronized void removeKeyCacheListener(OversecKeyCacheListener l) {
        mKeyCache.removeKeyCacheListener(l);

    }

    public SymmetricKeyEncrypted getSymmetricKeyEncrypted(Long id) {
        return mSymmetricEncryptedKeys.get(id);
    }


    public List<SymmetricKeyEncrypted> getEncryptedKeys_sorted() {
        List<SymmetricKeyEncrypted> l = mSymmetricEncryptedKeys.getAllValues();
        l=l==null?Collections.EMPTY_LIST:l;
        Collections.sort(l, new Comparator<SymmetricKeyEncrypted>() {
            @Override
            public int compare(SymmetricKeyEncrypted lhs, SymmetricKeyEncrypted rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        return l;
    }

    public boolean isEmpty() {
        List<Object> allKeys = mSymmetricEncryptedKeys.getAllKeys();
        return allKeys==null||allKeys.isEmpty();
    }

    public boolean hasName(String name) {
        List<SymmetricKeyEncrypted> l = mSymmetricEncryptedKeys.getAllValues();
        for (SymmetricKeyEncrypted key : l) {
            if (key.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }


    public SymmetricKeyEncrypted encryptSymmetricKey(SymmetricKeyPlain plainKey, char[] password) throws IOException {
        int cost = KeyUtil.DEFAULT_KEYSTORAGE_BCRYPT_COST;

        byte[] bcrypt_salt = KeyUtil.getRandomBytes(16);

        byte[] bcryptedPassword = KeyUtil.brcryptifyPassword(password, bcrypt_salt, cost, 32);
        KeyUtil.erase(password);

        byte[] chachaIv = KeyUtil.getRandomBytes(8);

        byte[] ciphertext = OversecChacha20Poly1305.enChacha(plainKey.getRaw(), bcryptedPassword, chachaIv);
        KeyUtil.erase(bcryptedPassword);

        SymmetricKeyEncrypted sek = new SymmetricKeyEncrypted(plainKey.getId(), plainKey.getName(), plainKey.getCreatedDate(),
                bcrypt_salt, chachaIv, cost, ciphertext);


        return sek;

    }


    public SymmetricKeyPlain decryptSymmetricKey(SymmetricKeyEncrypted k, char[] password) throws IOException, OversecChacha20Poly1305.MacMismatchException {

        byte[] bcryptedPassword = KeyUtil.brcryptifyPassword(password, k.getSalt(), k.getCost(), 32);
        KeyUtil.erase(password);

        byte[] raw = OversecChacha20Poly1305.deChacha(k.getCiphertext(), bcryptedPassword, k.getIv());
        KeyUtil.erase(bcryptedPassword);


        SymmetricKeyPlain sdk = new SymmetricKeyPlain(k.getId(), k.getName(), k.getCreatedDate(),
                raw);

        return sdk;
    }


    public static class AliasNotUniqueException extends Exception {
        private final String mAlias;

        public AliasNotUniqueException(String alias) {
            mAlias = alias;
        }

        public String getAlias() {
            return mAlias;
        }
    }


    public synchronized void addListener(KeyStoreListener v) {
        mListeners.add(v);
    }

    public synchronized void removeListener(KeyStoreListener v) {
        mListeners.remove(v);
    }

    private synchronized void fireChange() {
        for (KeyStoreListener v : mListeners) {
            v.onKeyStoreChanged();
        }
    }


    public interface KeyStoreListener {

        void onKeyStoreChanged();

    }

    public static class Base64DecodingException extends Exception {
        public Base64DecodingException(DecoderException ex) {
            super(ex);
        }
    }
}
