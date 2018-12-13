package io.oversec.one.crypto.gpg

import android.app.PendingIntent
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.EncryptionMethod
import org.openintents.openpgp.OpenPgpSignatureResult

class GpgDecryptResult : BaseDecryptResult {

    var showSignatureKeyPendingIntent: PendingIntent? = null
    var downloadMissingSignatureKeyPendingIntent: PendingIntent? = null
    var signatureResult: OpenPgpSignatureResult? = null

    private var mPublicKeyIds: List<Long>? = null

    val publicKeyIds: LongArray?
        get() = GpgEncryptionParams.LongListToLongArray(mPublicKeyIds)

    constructor(rawInnerData: ByteArray, publicKeyIds: List<Long>) : super(
        EncryptionMethod.GPG,
        rawInnerData
    ) {
        mPublicKeyIds = publicKeyIds
    }

    constructor(
        pgpError: BaseDecryptResult.DecryptError,
        message: String
    ) : super(EncryptionMethod.GPG, pgpError, message) {
    }
}
