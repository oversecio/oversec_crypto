package io.oversec.one.crypto.gpg;

import android.app.PendingIntent;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.EncryptionMethod;
import org.openintents.openpgp.OpenPgpSignatureResult;

import java.util.List;

public class GpgDecryptResult extends BaseDecryptResult {


    private PendingIntent mShowSignatureKeyPendingIntent;
    private PendingIntent mDownloadMissingIgnatureKeyPendingIntent;
    private OpenPgpSignatureResult mSignatureResult;
    private List<Long> mPublicKeyIds;

    public GpgDecryptResult(byte[] rawInnerData, List<Long> publicKeyIds) {
        super(EncryptionMethod.GPG, rawInnerData);
        mPublicKeyIds = publicKeyIds;
    }


    public GpgDecryptResult(DecryptError pgpError, String message) {
        super(EncryptionMethod.GPG, pgpError, message);
    }

    public void setDownloadMissingSignatureKeyPendingIntent(PendingIntent downloadMissingSignatureKeyFromKeyserverPendingIntent) {
        this.mDownloadMissingIgnatureKeyPendingIntent = downloadMissingSignatureKeyFromKeyserverPendingIntent;
    }


    public void setShowSignatureKeyPendingIntent(PendingIntent pi) {
        this.mShowSignatureKeyPendingIntent = pi;
    }

    public PendingIntent getShowSignatureKeyPendingIntent() {
        return mShowSignatureKeyPendingIntent;
    }


    public OpenPgpSignatureResult getSignatureResult() {
        return mSignatureResult;
    }

    public EncryptionMethod getEncryptionMethod() {
        return mEncryptionMethod;
    }

    public Long[] getPublicKeyIds() {
        return GpgEncryptionParams.LongListToLongArray(mPublicKeyIds);
    }

    public PendingIntent getDownloadMissingSignatureKeyPendingIntent() {
        return mDownloadMissingIgnatureKeyPendingIntent;
    }


    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        this.mSignatureResult = signatureResult;
    }
}