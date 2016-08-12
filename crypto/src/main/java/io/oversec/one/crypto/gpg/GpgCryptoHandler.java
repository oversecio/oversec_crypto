package io.oversec.one.crypto.gpg;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.oversec.one.common.ExpiringLruCache;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.gpg.ui.GpgBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.gpg.ui.GpgTextEncryptionInfoFragment;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;
import org.apache.commons.io.IOUtils;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.openpgp.*;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import roboguice.util.Ln;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings("RedundantThrows")
public class GpgCryptoHandler extends AbstractCryptoHandler {


    private static final long CACHE_MAIN_KEY_TTL = 1000 * 60 * 1;
    private static final long CACHE_USERNAME_TTL = 1000 * 60 * 1;

    //TODO: review caching, timeouts
    ExpiringLruCache<Long, Long> mMainKeyCache = new ExpiringLruCache<>(50, CACHE_MAIN_KEY_TTL);
    ExpiringLruCache<Long, String> mUserNameCache = new ExpiringLruCache<>(50, CACHE_USERNAME_TTL);

    public GpgCryptoHandler(Context ctx) {
        super(ctx);
        OpenKeychainConnector.init(ctx.getApplicationContext());
    }

    @Override
    public int getDisplayEncryptionMethod() {
        return R.string.encryption_method_pgp;
    }


    @Override
    public AbstractEncryptionParams buildDefaultEncryptionParams(BaseDecryptResult tdr) {
        GpgDecryptResult r = (GpgDecryptResult) tdr;
        Long[] pkids = r.getPublicKeyIds();
        return new GpgEncryptionParams(GpgEncryptionParams.LongArrayToLongList(pkids), null, null);
    }


    private Intent executeApi(Intent data, InputStream is, OutputStream os) {
        return OpenKeychainConnector.executeApi(data, is, os);
    }


    @Override
    public BaseDecryptResult decrypt(Outer.Msg msg, Intent actionIntent, String encryptedText) throws UserInteractionRequiredException {
        return tryDecrypt(msg.getMsgTextGpgV0(), actionIntent);
    }

    private GpgDecryptResult tryDecrypt(
            Outer.MsgTextGpgV0 msg,
            Intent actionIntent) throws UserInteractionRequiredException {


        try {

            return decrypt(msg.getCiphertext().toByteArray(), msg.getPubKeyIdV0List(), actionIntent);
        } catch (OpenPGPErrorException e) {
            e.printStackTrace();
            return new GpgDecryptResult(BaseDecryptResult.DecryptError.PGP_ERROR, e.getError().getMessage());
//        } catch (UserInteractionRequiredException e) {
//            e.printStackTrace();
//            throw e;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public Outer.Msg encrypt(Inner.InnerData innerData, AbstractEncryptionParams params, Intent actionIntent) throws GeneralSecurityException, UserInteractionRequiredException, IOException {
        GpgEncryptionParams p = (GpgEncryptionParams) params;
        try {
            return encrypt(innerData.toByteArray(), p, actionIntent);
        } catch (OpenPGPParamsException e) {
            e.printStackTrace();
            return null;
        } catch (OpenPGPErrorException e) {
            e.printStackTrace(); //TODO wrap???
            return null;
        }
    }

    @Override
    public Outer.Msg encrypt(String plainText, AbstractEncryptionParams params, Intent actionIntent) throws GeneralSecurityException, UserInteractionRequiredException, IOException {
        GpgEncryptionParams p = (GpgEncryptionParams) params;
        try {
            return encrypt(plainText.getBytes("UTF-8"), p, actionIntent);
        } catch (OpenPGPParamsException e) {
            e.printStackTrace();
            return null;
        } catch (OpenPGPErrorException e) {
            e.printStackTrace(); //TODO wrap???
            return null;
        }
    }


    @SuppressWarnings("RedundantThrows")
    private Outer.Msg encrypt(byte[] raw, GpgEncryptionParams pp, Intent actionIntent) throws OpenPGPParamsException, OpenPGPErrorException, UserInteractionRequiredException {


        Intent data = new Intent();
        if (actionIntent != null) {
            data = actionIntent;
        } else {
            data.setAction(pp.isSign() ? OpenPgpApi.ACTION_SIGN_AND_ENCRYPT : OpenPgpApi.ACTION_ENCRYPT);
            if (pp.getAllPublicKeyIds().length == 0) {
                throw new IllegalArgumentException();
            }
            data.putExtra(OpenPgpApi.EXTRA_KEY_IDS, pp.getAllPublicKeyIds());
            if (pp.isSign()) {
                data.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, pp.getOwnPublicKey());
            }
            data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, false);
        }
        InputStream is = new ByteArrayInputStream(raw);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Intent result = executeApi(data, is, os);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {

                byte[] encrypted = os.toByteArray();


                Outer.Msg.Builder builderMsg = Outer.Msg.newBuilder();
                Outer.MsgTextGpgV0.Builder pgpMsgBuilder = builderMsg.getMsgTextGpgV0Builder();

                pgpMsgBuilder.setCiphertext(ByteString.copyFrom(encrypted));
                for (long pkId : pp.getAllPublicKeyIds()) {
                    pgpMsgBuilder.addPubKeyIdV0(pkId);
                }

                builderMsg.setMsgTextGpgV0(pgpMsgBuilder);



                Outer.Msg msg = builderMsg.build();


                return msg;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                throw new UserInteractionRequiredException(pi, GpgEncryptionParams.longArrayToLongArray(pp.getAllPublicKeyIds()));
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                Ln.e("encryption error: %s", error.getMessage());
                throw new OpenPGPErrorException(error);
            }
            default:
                return null;
        }
    }


    @Override
    public AbstractTextEncryptionInfoFragment getTextEncryptionInfoFragment(String packagename) {
        return GpgTextEncryptionInfoFragment.newInstance(packagename);
    }

    @Override
    public AbstractBinaryEncryptionInfoFragment getBinaryEncryptionInfoFragment(String packagename) {
        return GpgBinaryEncryptionInfoFragment.newInstance(packagename);
    }

//
//    @Override
//    public AbstractEncryptionParamsFragment getEncryptionParamsFragment(String packagename) {
//        return GpgEncryptionParamsFragment.newInstance(packagename);
//    }

    @SuppressWarnings("RedundantThrows")
    public GpgDecryptResult decrypt(byte[] pgpEncoded, List<Long> pkids,
                                    Intent actionIntent) throws OpenPGPErrorException, UserInteractionRequiredException, UnsupportedEncodingException {

        Intent data = new Intent();
        if (actionIntent != null) {
            data = actionIntent;
        } else {
            data.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);
        }

        InputStream is = new ByteArrayInputStream(pgpEncoded);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Intent result = executeApi(data, is, os);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {


                OpenPgpSignatureResult sigResult = null;
                if (result.hasExtra(OpenPgpApi.RESULT_SIGNATURE)) {
                    sigResult = result.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
                }

                GpgDecryptResult res = new GpgDecryptResult(os.toByteArray(), pkids);

                if (sigResult != null) {
                    res.setSignatureResult(sigResult);
                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    if (sigResult.getResult() == OpenPgpSignatureResult.RESULT_KEY_MISSING) {
                        res.setDownloadMissingSignatureKeyPendingIntent(pi);
                    } else {
                        res.setShowSignatureKeyPendingIntent(pi);
                    }
                }


                return res;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                throw new UserInteractionRequiredException(pi, pkids);
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                Ln.e("encryption error: %s", error.getMessage());
                throw new OpenPGPErrorException(error);
            }
            default:
                return null;
        }
    }

    public static List<Long> parsePublicKeyIds(byte[] pgpEncoded) {
        List<Long> r = new ArrayList<>();
        try {
            InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(pgpEncoded));
            PGPObjectFactory pgpF = new PGPObjectFactory(in, new BcKeyFingerprintCalculator());
            PGPEncryptedDataList enc;

            Object o = pgpF.nextObject();
            //
            // the first object might be a GPG marker packet.
            //
            if (o instanceof PGPEncryptedDataList) {
                enc = (PGPEncryptedDataList) o;
            } else {
                enc = (PGPEncryptedDataList) pgpF.nextObject();
            }


            Iterator<?> it = enc.getEncryptedDataObjects();

            PGPPublicKeyEncryptedData pbe;

            while (it.hasNext()) {
                pbe = (PGPPublicKeyEncryptedData) it.next();
                if (!r.contains(pbe.getKeyID())) {
                    r.add(pbe.getKeyID());
                }
            }

            return r;
        } catch (IOException e) {
            e.printStackTrace();
            return r;
        }
    }


    public String getFirstUserIDByKeyId(long keyId, Intent actionIntent) {
        String res = mUserNameCache.get(keyId);
        if (res == null) {
            List<String> r = getUserIDsByKeyId(keyId, actionIntent);
            res = r == null ? null : (r.size() > 0 ? r.get(0) : null);
            if (res != null) {
                mUserNameCache.put(keyId, res);
            }
        }
        return res;
    }
//
//    public String getAllUserIDsByKeyId(long keyId, Intent actionIntent) {
//        List<String> r = getUserIDsByKeyId(keyId, actionIntent);
//        if (r == null || r.size() == 0) {
//            return null;
//        }
//        StringBuilder sb = new StringBuilder();
//        for (String s : r
//                ) {
//            if (sb.length() > 0) {
//                sb.append(", ");
//            }
//            sb.append(s);
//        }
//        return sb.toString();
//    }


    public List<String> getUserIDsByKeyId(long keyId, Intent actionIntent) {

        //NOTE: currently we can only find keys by the master keyId,
        //see https://github.com/open-keychain/open-keychain/issues/1841
        //and maybe implement sub-key stuff once available

        //has been fixed here:
        //https://github.com/open-keychain/open-keychain/commit/4c063ebe4683c0ffd0a80ff617967e8134b484fa

        Intent id = new Intent();
        if (actionIntent != null) {
            id = actionIntent;
        } else {
            id.setAction(OpenPgpApi.ACTION_GET_KEY);
            id.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId);
            id.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
        }
        InputStream is = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Intent result = executeApi(id, is, os);
        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {

                try {
                    InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(os.toByteArray()));

                    PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator());

                    Iterator rIt = pgpPub.getKeyRings();

                    if (!rIt.hasNext()) {
                        Log.e("TAG", "failed to parse public key, no key rings found");
                        return null;
                    }

                    PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
                    Iterator kIt = kRing.getPublicKeys();

                    if (kIt.hasNext()) {
                        //first key
                        List<String> res = new ArrayList<>();
                        PGPPublicKey kk = (PGPPublicKey) kIt.next();
                        Iterator ki = kk.getUserIDs();
                        while (ki.hasNext()) {
                            res.add((String) ki.next());
                        }

                        return res;

                    }

                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                Log.e("TAG", "UserInteractionRequired ");
                return null;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                Log.e("TAG", "Error: " + error.getMessage());

                return null;
            }
        }
        return null;

    }


    public static void openOpenKeyChain(Context ctx) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName(OpenKeychainConnector.PACKAGE_NAME, "org.sufficientlysecure.keychain.ui.MainActivity"));
            i.setPackage(OpenKeychainConnector.PACKAGE_NAME);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            if (!(ctx instanceof Activity)) {
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ctx.startActivity(i);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public PendingIntent triggerRecipientSelection(Intent actionIntent) {
        Intent data = new Intent();
        if (actionIntent != null) {
            data = actionIntent;
        } else {
            data.setAction(OpenPgpApi.ACTION_ENCRYPT); //we do not encrypt nothing, just use this to bring up the public key selection dialog
            data.putExtra(OpenPgpApi.EXTRA_USER_IDS, new String[0]);
        }
        InputStream is = new ByteArrayInputStream("dummy".getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Intent result = executeApi(data, is, os);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
                //ok, it has workd now, i.e. the recipients have been selected
                return null;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                //this is the intent we can use to bring up the key selection
                PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                return pi;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                //this should never happen
                return null;
            }

        }
        return null;
    }

    public PendingIntent triggerSigningKeySelection(Intent actionIntent) {
        Intent data = new Intent();
        if (actionIntent != null) {
            data = actionIntent;
        } else {
            data.setAction(OpenPgpApi.ACTION_GET_SIGN_KEY_ID);
        }
        InputStream is = new ByteArrayInputStream("dummy".getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Intent result = executeApi(data, is, os);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
                //this should never happen
                return null;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                //this is the intent we can use to bring up the key selection
                PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                return pi;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                //this should never happen
                return null;
            }

        }
        return null;
    }


    public static String signatureResultToUiText(Context ctx, OpenPgpSignatureResult sr) {
        switch (sr.getResult()) {
            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
                return ctx.getString(R.string.signature_result__invalid_insecure);
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
                return ctx.getString(R.string.signature_result__invalid_key_expired);
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                return ctx.getString(R.string.signature_result__invalid_key_revoked);
            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                return ctx.getString(R.string.signature_result__invalid);
            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                return ctx.getString(R.string.signature_result__key_missing);
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
                return ctx.getString(R.string.signature_result__no_signature);
            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return ctx.getString(R.string.signature_result__valid_confirmed);
            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                return ctx.getString(R.string.signature_result__valid_unconfirmed);
            default:
                return null;
        }
    }


    public static int signatureResultToUiColorResId(OpenPgpSignatureResult sr) {
        switch (sr.getResult()) {
            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                return R.color.colorWarning;
            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
                return R.color.colorError;
            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return R.color.colorOk;
            default:
                return 0;
        }
    }


    public static int signatureResultToUiIconRes(OpenPgpSignatureResult sr, boolean small) {
        switch (sr.getResult()) {
            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                return small ? R.drawable.ic_error_red_18dp : R.drawable.ic_error_red_24dp;
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
                return small ? R.drawable.ic_warning_red_18dp : R.drawable.ic_warning_red_24dp;
            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                return small ? R.drawable.ic_warning_orange_18dp : R.drawable.ic_warning_orange_24dp;
            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                return small ? R.drawable.ic_done_orange_18dp : R.drawable.ic_done_orange_24dp;
            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return small ? R.drawable.ic_done_all_green_a700_18dp : R.drawable.ic_done_all_green_a700_24dp;
            default:
                return 0;
        }
    }


    public static int signatureResultToUiColorResId_KeyOnly(OpenPgpSignatureResult sr) {
        switch (sr.getResult()) {
            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                return R.color.colorWarning;
            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return R.color.colorOk;
            default:
                return 0;
        }
    }

    public static int signatureResultToUiIconRes_KeyOnly(OpenPgpSignatureResult sr, boolean small) {
        switch (sr.getResult()) {
            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                return small ? R.drawable.ic_warning_red_18dp : R.drawable.ic_warning_red_24dp;
            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return small ? R.drawable.ic_done_green_a700_18dp : R.drawable.ic_done_green_a700_24dp;
            default:
                return 0;
        }
    }


    public PendingIntent getDownloadKeyPendingIntent(long keyId, Intent actionIntent) {
        Intent id = new Intent();
        if (actionIntent != null) {
            id = actionIntent;
        } else {
            id.setAction(OpenPgpApi.ACTION_GET_KEY);
            id.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId);
        }

        Intent result = executeApi(id, null, null);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
                return null;

            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                return pi;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {

                return null;
            }
        }
        return null;
    }


    public static String getRawMessageAsciiArmoured(Outer.Msg msg) {
        if (msg.hasMsgTextGpgV0()) {
            Outer.MsgTextGpgV0 data = msg.getMsgTextGpgV0();
            byte[] raw = data.getCiphertext().toByteArray();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OversecAsciiArmoredOutputStream aos = new OversecAsciiArmoredOutputStream(baos);
            aos.setHeader("Charset","utf-8");
            try {
                aos.write(raw);
                aos.flush();
                aos.close();
                return new String(baos.toByteArray(), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }


        } else {
            return null;
        }
    }

    public static Outer.Msg parseMessageAsciiArmoured(String s) throws IOException {
        ArmoredInputStream ais = new ArmoredInputStream(IOUtils.toInputStream(s, "UTF-8"));
        byte[] raw = IOUtils.toByteArray(ais);

        if (raw.length == 0) {
            throw new IOException("bad ascii armoured text");
        }

        Outer.Msg.Builder builderMsg = Outer.Msg.newBuilder();
        Outer.MsgTextGpgV0.Builder pgpMsgBuilder = builderMsg.getMsgTextGpgV0Builder();

        pgpMsgBuilder.setCiphertext(ByteString.copyFrom(raw));

        builderMsg.setMsgTextGpgV0(pgpMsgBuilder);
        Outer.Msg msg = builderMsg.build();

        return msg;
    }

    public void setGpgOwnPublicKeyId(long keyId) {
        GpgPreferences.getPreferences(mCtx).setGpgOwnPublicKeyId(keyId);
    }

    public long getGpgOwnPublicKeyId() {
        return GpgPreferences.getPreferences(mCtx).getGpgOwnPublicKeyId();
    }


    public Long getMainKeyIdFromSubkeyId(Long keyId) {

        Long res = mMainKeyCache.get(keyId);
        if (res != null) {
            return res;
        }

        Intent id = new Intent();

        id.setAction(OpenPgpApi.ACTION_GET_KEY);
        id.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId);
        id.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        InputStream is = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Intent result = executeApi(id, is, os);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS: {

                try {
                    InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(os.toByteArray()));

                    PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator());

                    Iterator rIt = pgpPub.getKeyRings();

                    if (!rIt.hasNext()) {
                        Log.e("TAG", "failed to parse public key, no key rings found");
                        return null;
                    }

                    PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
                    Iterator kIt = kRing.getPublicKeys();

                    if (kIt.hasNext()) {
                        //first key
                        PGPPublicKey kk = (PGPPublicKey) kIt.next();
                        mMainKeyCache.put(keyId, kk.getKeyID());
                        return kk.getKeyID();

                    }
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                Log.e("TAG", "UserInteractionRequired ");
                return null;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                Log.e("TAG", "Error: " + error.getMessage());
                return null;
            }
        }
        return null;

    }

    static final Pattern P_ASCII_ARMOR_BEGIN = Pattern.compile("-----BEGIN (.*)-----");
    static final String F_ASCII_ARMOR_END = "-----END %s-----";
    static final int LINE_LENGTH = 64;
    public static String sanitizeAsciiArmor(String s) {
        //remove anything before ----START ....
        //remove anything after  ----END ....

        Matcher mStart = P_ASCII_ARMOR_BEGIN.matcher(s);
        if (mStart.find()) {


            int posStart = mStart.start();

            String g1 = mStart.group(1);
            String end = String.format(F_ASCII_ARMOR_END,g1);

            int posEnd = s.indexOf(end,posStart);
            if (posEnd>=0) {
                s = s.substring(posStart,posEnd+end.length());
                StringBuilder sb = new StringBuilder();
                //adjust line length


                String line=null;
                String lastLine = null;
                int curLineLength = 0;
                boolean inBody = false;
                BufferedReader bufReader = new BufferedReader(new StringReader(s));
                try {
                    while( (line=bufReader.readLine()) != null )
                    {
                        if (line.startsWith(end) && curLineLength>0) {
                            sb.append("\n");
                        }
                        //insert blank line after headers
                        if (line.trim().length()>0 && lastLine!=null && lastLine.contains(": ") && !line.contains(": ")) {
                            sb.append("\n");
                            inBody = true;
                        }

                        sb.append(line);
                        curLineLength+=line.length();
                        if (!inBody || curLineLength==LINE_LENGTH) {
                            sb.append("\n");
                            curLineLength = 0;
                        }
                        if (line.trim().length()==0) {
                            inBody = true;
                        }


                        lastLine = line;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String res = sb.toString();
                return res;
            }

        }
        return  null;
    }
}
