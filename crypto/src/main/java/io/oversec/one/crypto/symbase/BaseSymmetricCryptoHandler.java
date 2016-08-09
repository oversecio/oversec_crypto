package io.oversec.one.crypto.symbase;

import android.content.Context;
import android.content.Intent;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.KeyNotCachedException;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import roboguice.util.Ln;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;


public abstract class BaseSymmetricCryptoHandler extends AbstractCryptoHandler {


    public static final int IV_LENGTH = 8;
    public static final int SALT_LENGTH = 16; //DO NOT CHANGE, needs to match Bcrypt.SALT_SIZE_BYTES

    protected final KeyCache mKeyCache;

    public BaseSymmetricCryptoHandler(Context ctx) {
        super(ctx);
        mKeyCache = KeyCache.getInstance(ctx);
    }

    protected abstract EncryptionMethod getMethod();


    @Override
    public int getDisplayEncryptionMethod() {
        return R.string.encryption_method_sym;
    }

    protected abstract SymmetricKeyPlain getKeyByHashedKeyId(long keyhash, byte[] salt, int cost, String encryptedText) throws KeyNotCachedException;


    protected SymmetricDecryptResult tryDecrypt(Outer.MsgTextSymV0 symMsg, String encryptedText) throws UserInteractionRequiredException {

        if (symMsg.hasMsgTextChaChaV0()) {
            Outer.MsgTextChaChaV0 chachaMsg = symMsg.getMsgTextChaChaV0();
            List<Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext> pkcl = chachaMsg.getPerKeyCiphertextList();


            SymmetricKeyPlain key = null;
            Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext matchingPkc = null;
            for (Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext pkc : pkcl) {


                // sym: key exists, but is not cached -> throws KeyNotCachedException, OK
                // simple (if key exists it is always cached)
                key = getKeyByHashedKeyId(pkc.getKeyhash(), pkc.getSalt().toByteArray(), chachaMsg.getCostKeyhash(), encryptedText);


                if (key != null) {
                    matchingPkc = pkc;
                    break;
                }
            }


            if (key == null) {
                Ln.d("SYM: NO MATCHING KEY");

                //sym: if key exists but not cached we will not reach here, a KeyNotCachedException will have been thrown before
                //sym: if key doesn't exists, return SYM_NO_MATCHING_KEY
                //simple: if key doesn't exist, throw a userInteraction exception to enter the key
                long[] keyHashes = new long[pkcl.size()];
                byte[][] salts = new byte[pkcl.size()][];
                int i = 0;
                for (Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext pkc : pkcl) {
                    keyHashes[i] = pkc.getKeyhash();
                    salts[i] = pkc.getSalt().toByteArray();
                    i++;
                }
                handleNoKeyFoundForDecryption(keyHashes, salts, chachaMsg.getCostKeyhash(), encryptedText);

                return new SymmetricDecryptResult(getMethod(), BaseDecryptResult.DecryptError.SYM_NO_MATCHING_KEY);
            } else {
                Ln.d("SYM: try decrypt with key %s", key.getId());
                byte[] rawData = null;
                try {
                    rawData = tryDecryptChacha(matchingPkc, key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (rawData != null) {
                    Ln.d("SYM: try last used key SUCCESS");
                    return new SymmetricDecryptResult(getMethod(), rawData, key.getId());
                } else {
                    Ln.d("SYM: DECRYPTION FAILED");
                    return new SymmetricDecryptResult(getMethod(), BaseDecryptResult.DecryptError.SYM_DECRYPT_FAILED);
                }
            }

        }
        Ln.d("SYM: DECRYPTION FAILED");
        return new SymmetricDecryptResult(getMethod(), BaseDecryptResult.DecryptError.SYM_UNSUPPORTED_CIPHER);

    }

    protected abstract void handleNoKeyFoundForDecryption(long[] keyHashes, byte[][] salts, int costKeyhash, String encryptedText) throws UserInteractionRequiredException;



    @Override
    public Outer.Msg encrypt(Inner.InnerData innerData, AbstractEncryptionParams params, Intent actionIntent) throws IOException, GeneralSecurityException, UserInteractionRequiredException {
        BaseSymmetricEncryptionParams p = (BaseSymmetricEncryptionParams) params;
        return encrypt(innerData.toByteArray(), p.getKeyIds());
    }


    @Override
    public Outer.Msg encrypt(String plainText, AbstractEncryptionParams params, Intent actionIntent) throws GeneralSecurityException, UserInteractionRequiredException, IOException {
        BaseSymmetricEncryptionParams p = (BaseSymmetricEncryptionParams) params;
        return encrypt(plainText.getBytes("UTF-8"), p.getKeyIds());
    }

    private Outer.Msg encrypt(byte[] plain, List<Long> keyIds) throws GeneralSecurityException, IOException, KeyNotCachedException {

        int cost_key_id = KeyUtil.BCRYPT_SESSIONKEYID_COST_DEFAULT; //TODO make configurable

        Outer.Msg.Builder builderMsg = Outer.Msg.newBuilder();
        Outer.MsgTextSymV0.Builder symMsgBuilder = builderMsg.getMsgTextSymV0Builder();
        Outer.MsgTextChaChaV0.Builder chachaMsgBuilder = symMsgBuilder.getMsgTextChaChaV0Builder();

        chachaMsgBuilder.setCostKeyhash(cost_key_id);

        for (Long keyId : keyIds) {

            Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext.Builder pkcBuilder = chachaMsgBuilder.addPerKeyCiphertextBuilder();

            byte[] salt = KeyUtil.getRandomBytes(SALT_LENGTH);
            byte[] iv = KeyUtil.getRandomBytes(IV_LENGTH);
            long hashedKeyId = KeyUtil.calcSessionKeyId(keyId, salt, cost_key_id);


            SymmetricKeyPlain plainKey = mKeyCache.get(keyId);  //throws KeyNotCached

            byte[] ciphertext = KeyUtil.encryptSymmetricChaCha(plain, salt, iv, plainKey);


            pkcBuilder.setIv(ByteString.copyFrom(iv));
            pkcBuilder.setKeyhash(hashedKeyId);
            pkcBuilder.setSalt(ByteString.copyFrom(salt));
            pkcBuilder.setCiphertext(ByteString.copyFrom(ciphertext));

        }

        KeyUtil.erase(plain);

        symMsgBuilder.setMsgTextChaChaV0(chachaMsgBuilder);

        setMessage(builderMsg, symMsgBuilder);

        Outer.Msg msg = builderMsg.build();

        Ln.d("BCRYPT: ...encrypt");
        return msg;
    }

    protected abstract void setMessage(Outer.Msg.Builder builderMsg, Outer.MsgTextSymV0.Builder symMsgBuilder);


    protected byte[] tryDecryptChacha(Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext matchingPkc, SymmetricKeyPlain key)
            throws IOException, GeneralSecurityException, OversecChacha20Poly1305.MacMismatchException {


        byte[] plain = KeyUtil.decryptSymmetricChaCha(
                matchingPkc.getCiphertext().toByteArray(),
                matchingPkc.getSalt().toByteArray(),
                matchingPkc.getIv().toByteArray(),
                key);

        return plain;
    }


    public static String getRawMessageJson(Outer.Msg msg) {
        if (msg.hasMsgTextSymV0()) {
            Outer.MsgTextSymV0 symV0Msg = msg.getMsgTextSymV0();
            if (symV0Msg.hasMsgTextChaChaV0()) {
                Outer.MsgTextChaChaV0 chachaMsg = symV0Msg.getMsgTextChaChaV0();

                StringBuilder sb = new StringBuilder();

                sb.append("{");
                sb.append("\n");
                sb.append("  \"cipher\":\"chacha20+poly1305\"");
                sb.append(",\n");
                sb.append("  \"cost_keyhash\":").append(chachaMsg.getCostKeyhash());
                sb.append(",\n");

                sb.append("  \"per_key_encrypted_data\": [");
                sb.append("\n");

                List<Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext> pkcl = chachaMsg.getPerKeyCiphertextList();
                for (Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext pkc : pkcl) {
                    sb.append("   {");
                    sb.append("\n");
                    sb.append("  \"keyhash\":\"").append(SymUtil.byteArrayToHex(SymUtil.long2bytearray(pkc.getKeyhash()))).append("\"");
                    sb.append(",\n");
                    sb.append("  \"salt\":\"").append(SymUtil.byteArrayToHex(pkc.getSalt().toByteArray())).append("\"");
                    sb.append(",\n");
                    sb.append("  \"iv\":\"").append(SymUtil.byteArrayToHex(pkc.getIv().toByteArray())).append("\"");
                    sb.append(",\n");
                    sb.append("  \"ciphertext\":\"").append(SymUtil.byteArrayToHex(pkc.getCiphertext().toByteArray())).append("\"");
                    sb.append("\n");
                    sb.append("   }");
                    sb.append("\n");
                }
                sb.append("  ]");
                sb.append("}");

                return sb.toString();
            } else {
                return null;
            }

        } else {
            return null;
        }

    }


}
