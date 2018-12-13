package io.oversec.one.crypto.gpg

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.protobuf.ByteString
import io.oversec.one.common.ExpiringLruCache
import io.oversec.one.crypto.*
import io.oversec.one.crypto.encoding.AsciiArmouredGpgXCoder
import io.oversec.one.crypto.gpg.ui.GpgBinaryEncryptionInfoFragment
import io.oversec.one.crypto.gpg.ui.GpgTextEncryptionInfoFragment
import io.oversec.one.crypto.proto.Inner
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment
import org.apache.commons.io.IOUtils
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.OpenPgpSignatureResult
import org.openintents.openpgp.util.OpenPgpApi
import org.spongycastle.bcpg.ArmoredInputStream
import org.spongycastle.openpgp.*
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import roboguice.util.Ln
import java.io.*
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.util.ArrayList
import java.util.regex.Pattern

class GpgCryptoHandler(ctx: Context) : AbstractCryptoHandler(ctx) {

    //TODO: review caching, timeouts
    private var mMainKeyCache = ExpiringLruCache<Long, Long>(50, CACHE_MAIN_KEY_TTL)
    private var mUserNameCache = ExpiringLruCache<Long, String>(50, CACHE_USERNAME_TTL)

    override val displayEncryptionMethod: Int
        get() = R.string.encryption_method_pgp

    var gpgOwnPublicKeyId: Long
        get() = GpgPreferences.getPreferences(mCtx).gpgOwnPublicKeyId
        set(keyId) {
            GpgPreferences.getPreferences(mCtx).gpgOwnPublicKeyId = keyId
        }


    override fun buildDefaultEncryptionParams(tdr: BaseDecryptResult): AbstractEncryptionParams {
        val r = tdr as GpgDecryptResult
        val pkids = r.publicKeyIds
        return GpgEncryptionParams(pkids, AsciiArmouredGpgXCoder.ID, null)
    }

    private fun executeApi(data: Intent, inputStream: InputStream?, outputStream: OutputStream?): Intent {
        return OpenKeychainConnector.getInstance(mCtx).executeApi(data, inputStream, outputStream)
    }

    @Throws(UserInteractionRequiredException::class)
    override fun decrypt(
        msg: Outer.Msg,
        actionIntent: Intent?,
        encryptedText: String?
    ): BaseDecryptResult? {
        return tryDecrypt(msg.msgTextGpgV0, actionIntent)
    }

    @Throws(UserInteractionRequiredException::class)
    private fun tryDecrypt(
        msg: Outer.MsgTextGpgV0,
        actionIntent: Intent?
    ): GpgDecryptResult? {

        return try {
            decrypt(msg.ciphertext.toByteArray(), msg.pubKeyIdV0List, actionIntent)
        } catch (e: OpenPGPErrorException) {
            e.printStackTrace()
            GpgDecryptResult(BaseDecryptResult.DecryptError.PGP_ERROR, e.error.message)
            //        } catch (UserInteractionRequiredException e) {
            //            e.printStackTrace();
            //            throw e;
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }

    @Throws(
        GeneralSecurityException::class,
        UserInteractionRequiredException::class,
        IOException::class
    )
    override fun encrypt(
        innerData: Inner.InnerData,
        params: AbstractEncryptionParams,
        actionIntent: Intent?
    ): Outer.Msg? {
        val p = params as GpgEncryptionParams
        return try {
            encrypt(innerData.toByteArray(), p, actionIntent)
        } catch (e: OpenPGPParamsException) {
            e.printStackTrace()
            null
        } catch (e: OpenPGPErrorException) {
            e.printStackTrace() //TODO wrap???
            null
        }
    }

    @Throws(
        GeneralSecurityException::class,
        UserInteractionRequiredException::class,
        IOException::class
    )
    override fun encrypt(
        plainText: String,
        params: AbstractEncryptionParams,
        actionIntent: Intent?
    ): Outer.Msg? {
        val p = params as GpgEncryptionParams
        return try {
            encrypt(plainText.toByteArray(charset("UTF-8")), p, actionIntent)
        } catch (e: OpenPGPParamsException) {
            e.printStackTrace()
            null
        } catch (e: OpenPGPErrorException) {
            e.printStackTrace() //TODO wrap???
            null
        }
    }

    @Throws(
        OpenPGPParamsException::class,
        OpenPGPErrorException::class,
        UserInteractionRequiredException::class
    )
    private fun encrypt(
        raw: ByteArray,
        pp: GpgEncryptionParams,
        actionIntent: Intent?
    ): Outer.Msg? {


        var data = Intent()
        if (actionIntent != null) {
            data = actionIntent
        } else {
            data.action =
                    if (pp.isSign) {
                        OpenPgpApi.ACTION_SIGN_AND_ENCRYPT
                    } else OpenPgpApi.ACTION_ENCRYPT
            if (pp.allPublicKeyIds.isEmpty()) {
                throw IllegalArgumentException()
            }
            data.putExtra(OpenPgpApi.EXTRA_KEY_IDS, pp.allPublicKeyIds)
            if (pp.isSign) {
                data.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, pp.ownPublicKey)
            }
            data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, false)
        }
        val `is` = ByteArrayInputStream(raw)
        val os = ByteArrayOutputStream()

        val result = executeApi(data, `is`, os)

        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {

                val encrypted = os.toByteArray()

                val builderMsg = Outer.Msg.newBuilder()
                val pgpMsgBuilder = builderMsg.msgTextGpgV0Builder

                pgpMsgBuilder.ciphertext = ByteString.copyFrom(encrypted)
                for (pkId in pp.allPublicKeyIds) {
                    pgpMsgBuilder.addPubKeyIdV0(pkId)
                }

                builderMsg.setMsgTextGpgV0(pgpMsgBuilder)


                return builderMsg.build()
            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                val pi = result.getParcelableExtra<PendingIntent>(OpenPgpApi.RESULT_INTENT)
                throw UserInteractionRequiredException(pi, pp.allPublicKeyIds)
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {
                val error = result.getParcelableExtra<OpenPgpError>(OpenPgpApi.RESULT_ERROR)
                Ln.e("encryption error: %s", error.message)
                throw OpenPGPErrorException(error)
            }
            else -> return null
        }
    }


    override fun getTextEncryptionInfoFragment(packagename: String): AbstractTextEncryptionInfoFragment {
        return GpgTextEncryptionInfoFragment.newInstance(packagename)
    }

    override fun getBinaryEncryptionInfoFragment(packagename: String): AbstractBinaryEncryptionInfoFragment {
        return GpgBinaryEncryptionInfoFragment.newInstance(packagename)
    }

    //
    //    @Override
    //    public AbstractEncryptionParamsFragment getEncryptionParamsFragment(String packagename) {
    //        return GpgEncryptionParamsFragment.newInstance(packagename);
    //    }

    @Throws(
        OpenPGPErrorException::class,
        UserInteractionRequiredException::class,
        UnsupportedEncodingException::class
    )
    fun decrypt(
        pgpEncoded: ByteArray, pkids: List<Long>,
        actionIntent: Intent?
    ): GpgDecryptResult? {

        var data = Intent()
        if (actionIntent != null) {
            data = actionIntent
        } else {
            data.action = OpenPgpApi.ACTION_DECRYPT_VERIFY
        }

        val `is` = ByteArrayInputStream(pgpEncoded)
        val os = ByteArrayOutputStream()

        val result = executeApi(data, `is`, os)

        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {


                var sigResult: OpenPgpSignatureResult? = null
                if (result.hasExtra(OpenPgpApi.RESULT_SIGNATURE)) {
                    sigResult = result.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE)
                }

                val res = GpgDecryptResult(os.toByteArray(), pkids)

                if (sigResult != null) {
                    res.signatureResult = sigResult
                    val pi = result.getParcelableExtra<PendingIntent>(OpenPgpApi.RESULT_INTENT)
                    if (sigResult.result == OpenPgpSignatureResult.RESULT_KEY_MISSING) {
                        res.downloadMissingSignatureKeyPendingIntent = pi
                    } else {
                        res.showSignatureKeyPendingIntent = pi
                    }
                }


                return res
            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                val pi = result.getParcelableExtra<PendingIntent>(OpenPgpApi.RESULT_INTENT)
                throw UserInteractionRequiredException(pi, pkids)
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {
                val error = result.getParcelableExtra<OpenPgpError>(OpenPgpApi.RESULT_ERROR)
                Ln.e("encryption error: %s", error.message)
                throw OpenPGPErrorException(error)
            }
            else -> return null
        }
    }


    fun getFirstUserIDByKeyId(keyId: Long, actionIntent: Intent?): String? {
        var res = mUserNameCache[keyId]
        if (res == null) {
            val r = getUserIDsByKeyId(keyId, actionIntent)
            res = if (r == null) null else if (r.isNotEmpty()) r[0] else null
            if (res != null) {
                mUserNameCache.put(keyId, res)
            }
        }
        return res
    }


    private fun getUserIDsByKeyId(keyId: Long, actionIntent: Intent?): List<String>? {

        //NOTE: currently we can only find keys by the master keyId,
        //see https://github.com/open-keychain/open-keychain/issues/1841
        //and maybe implement sub-key stuff once available

        //has been fixed here:
        //https://github.com/open-keychain/open-keychain/commit/4c063ebe4683c0ffd0a80ff617967e8134b484fa

        var id = Intent()
        if (actionIntent != null) {
            id = actionIntent
        } else {
            id.action = OpenPgpApi.ACTION_GET_KEY
            id.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId)
            id.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        }
        val `is` = ByteArrayInputStream(ByteArray(0))
        val os = ByteArrayOutputStream()
        val result = executeApi(id, `is`, os)
        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                try {
                    val `in` = PGPUtil.getDecoderStream(ByteArrayInputStream(os.toByteArray()))

                    val pgpPub = PGPPublicKeyRingCollection(`in`, BcKeyFingerprintCalculator())

                    val rIt = pgpPub.keyRings

                    if (!rIt.hasNext()) {
                        Log.e("TAG", "failed to parse public key, no key rings found")
                        return null
                    }

                    val kRing = rIt.next() as PGPPublicKeyRing
                    val kIt = kRing.publicKeys

                    if (kIt.hasNext()) {
                        //first key
                        val res = ArrayList<String>()
                        val kk = kIt.next() as PGPPublicKey
                        val ki = kk.userIDs
                        while (ki.hasNext()) {
                            res.add(ki.next() as String)
                        }

                        return res

                    }

                    return null
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }
            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                Log.e("TAG", "UserInteractionRequired ")
                return null
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {
                val error = result.getParcelableExtra<OpenPgpError>(OpenPgpApi.RESULT_ERROR)
                Log.e("TAG", "Error: " + error.message)

                return null
            }
        }
        return null

    }

    fun triggerRecipientSelection(actionIntent: Intent?): PendingIntent? {
        var data = Intent()
        if (actionIntent != null) {
            data = actionIntent
        } else {
            data.action =
                    OpenPgpApi.ACTION_ENCRYPT //we do not encrypt nothing, just use this to bring up the public key selection dialog
            data.putExtra(OpenPgpApi.EXTRA_USER_IDS, arrayOfNulls<String>(0))
        }
        val `is` = ByteArrayInputStream("dummy".toByteArray())
        val os = ByteArrayOutputStream()

        val result = executeApi(data, `is`, os)

        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                //ok, it has workd now, i.e. the recipients have been selected
                return null
            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                //this is the intent we can use to bring up the key selection
                return result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {
                //this should never happen
                return null
            }
        }
        return null
    }

    fun triggerSigningKeySelection(actionIntent: Intent?): PendingIntent? {
        var data = Intent()
        if (actionIntent != null) {
            data = actionIntent
        } else {
            data.action = OpenPgpApi.ACTION_GET_SIGN_KEY_ID
        }
        val `is` = ByteArrayInputStream("dummy".toByteArray())
        val os = ByteArrayOutputStream()

        val result = executeApi(data, `is`, os)

        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                //this should never happen
                return null
            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                //this is the intent we can use to bring up the key selection
                return result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {
                //this should never happen
                return null
            }
        }
        return null
    }


    fun getDownloadKeyPendingIntent(keyId: Long, actionIntent: Intent?): PendingIntent? {
        var id = Intent()
        if (actionIntent != null) {
            id = actionIntent
        } else {
            id.action = OpenPgpApi.ACTION_GET_KEY
            id.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId)
        }

        val result = executeApi(id, null, null)

        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                return null

            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                return result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {

                return null
            }
        }
        return null
    }


    fun getMainKeyIdFromSubkeyId(keyId: Long): Long? {

        val res = keyId.let { mMainKeyCache[keyId] }
        if (res != null) {
            return res
        }

        val id = Intent()

        id.action = OpenPgpApi.ACTION_GET_KEY
        id.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId)
        id.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)

        val `is` = ByteArrayInputStream(ByteArray(0))
        val os = ByteArrayOutputStream()
        val result = executeApi(id, `is`, os)

        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                try {
                    val `in` = PGPUtil.getDecoderStream(ByteArrayInputStream(os.toByteArray()))

                    val pgpPub = PGPPublicKeyRingCollection(`in`, BcKeyFingerprintCalculator())

                    val rIt = pgpPub.keyRings

                    if (!rIt.hasNext()) {
                        Log.e("TAG", "failed to parse public key, no key rings found")
                        return null
                    }

                    val kRing = rIt.next() as PGPPublicKeyRing
                    val kIt = kRing.publicKeys

                    if (kIt.hasNext()) {
                        //first key
                        val kk = kIt.next() as PGPPublicKey
                        mMainKeyCache.put(keyId, kk.keyID)
                        return kk.keyID

                    }
                    return null
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }
            }
            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                Log.e("TAG", "UserInteractionRequired ")
                return null
            }
            OpenPgpApi.RESULT_CODE_ERROR -> {
                val error = result.getParcelableExtra<OpenPgpError>(OpenPgpApi.RESULT_ERROR)
                Log.e("TAG", "Error: " + error.message)
                return null
            }
        }
        return null

    }

    companion object {

        private const val CACHE_MAIN_KEY_TTL = (1000 * 60 * 1).toLong()
        private const val CACHE_USERNAME_TTL = (1000 * 60 * 1).toLong()

        fun parsePublicKeyIds(pgpEncoded: ByteArray): List<Long> {
            val r = ArrayList<Long>()
            try {
                val `in` = PGPUtil.getDecoderStream(ByteArrayInputStream(pgpEncoded))
                val pgpF = PGPObjectFactory(`in`, BcKeyFingerprintCalculator())
                val enc: PGPEncryptedDataList

                val o = pgpF.nextObject()
                //
                // the first object might be a GPG marker packet.
                //
                enc = if (o is PGPEncryptedDataList) {
                    o
                } else {
                    pgpF.nextObject() as PGPEncryptedDataList
                }

                val it = enc.encryptedDataObjects

                var pbe: PGPPublicKeyEncryptedData

                while (it.hasNext()) {
                    pbe = it.next() as PGPPublicKeyEncryptedData
                    if (!r.contains(pbe.keyID)) {
                        r.add(pbe.keyID)
                    }
                }

                return r
            } catch (e: Exception) {
                e.printStackTrace()
                return r
            }

        }


        fun openOpenKeyChain(ctx: Context) {
            try {
                val i = Intent(Intent.ACTION_MAIN)
                i.component = ComponentName(
                    OpenKeychainConnector.PACKAGE_NAME,
                    "org.sufficientlysecure.keychain.ui.MainActivity"
                )
                i.setPackage(OpenKeychainConnector.PACKAGE_NAME)
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                if (ctx !is Activity) {
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(i)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }

        fun signatureResultToUiText(ctx: Context, sr: OpenPgpSignatureResult): String? {
            return when (sr.result) {
                OpenPgpSignatureResult.RESULT_INVALID_INSECURE -> ctx.getString(R.string.signature_result__invalid_insecure)
                OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED -> ctx.getString(R.string.signature_result__invalid_key_expired)
                OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED -> ctx.getString(R.string.signature_result__invalid_key_revoked)
                OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE -> ctx.getString(R.string.signature_result__invalid)
                OpenPgpSignatureResult.RESULT_KEY_MISSING -> ctx.getString(R.string.signature_result__key_missing)
                OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> ctx.getString(R.string.signature_result__no_signature)
                OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> ctx.getString(R.string.signature_result__valid_confirmed)
                OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED -> ctx.getString(R.string.signature_result__valid_unconfirmed)
                else -> null
            }
        }

        fun signatureResultToUiColorResId(sr: OpenPgpSignatureResult): Int {
            return when (sr.result) {
                OpenPgpSignatureResult.RESULT_INVALID_INSECURE, OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED, OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED -> R.color.colorWarning
                OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE, OpenPgpSignatureResult.RESULT_KEY_MISSING, OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> R.color.colorError
                OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED, OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> R.color.colorOk
                else -> 0
            }
        }

        fun signatureResultToUiIconRes(sr: OpenPgpSignatureResult, small: Boolean): Int {
            return when (sr.result) {
                OpenPgpSignatureResult.RESULT_INVALID_INSECURE, OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED, OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED, OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE -> if (small) R.drawable.ic_error_red_18dp else R.drawable.ic_error_red_24dp
                OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> if (small) R.drawable.ic_warning_red_18dp else R.drawable.ic_warning_red_24dp
                OpenPgpSignatureResult.RESULT_KEY_MISSING -> if (small) R.drawable.ic_warning_orange_18dp else R.drawable.ic_warning_orange_24dp
                OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED -> if (small) R.drawable.ic_done_orange_18dp else R.drawable.ic_done_orange_24dp
                OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> if (small) R.drawable.ic_done_all_green_a700_18dp else R.drawable.ic_done_all_green_a700_24dp
                else -> 0
            }
        }

        fun signatureResultToUiColorResId_KeyOnly(sr: OpenPgpSignatureResult): Int {
            return when (sr.result) {
                OpenPgpSignatureResult.RESULT_INVALID_INSECURE, OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED, OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED, OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE, OpenPgpSignatureResult.RESULT_KEY_MISSING, OpenPgpSignatureResult.RESULT_NO_SIGNATURE, OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED -> R.color.colorWarning
                OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> R.color.colorOk
                else -> 0
            }
        }

        fun signatureResultToUiIconRes_KeyOnly(sr: OpenPgpSignatureResult, small: Boolean): Int {
            return when (sr.result) {
                OpenPgpSignatureResult.RESULT_INVALID_INSECURE, OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED, OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED, OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE, OpenPgpSignatureResult.RESULT_NO_SIGNATURE, OpenPgpSignatureResult.RESULT_KEY_MISSING, OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED -> if (small) R.drawable.ic_warning_red_18dp else R.drawable.ic_warning_red_24dp
                OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> if (small) R.drawable.ic_done_green_a700_18dp else R.drawable.ic_done_green_a700_24dp
                else -> 0
            }
        }

        fun getRawMessageAsciiArmoured(msg: Outer.Msg): String? {
            if (msg.hasMsgTextGpgV0()) {
                val data = msg.msgTextGpgV0
                val raw = data.ciphertext.toByteArray()
                val baos = ByteArrayOutputStream()
                val aos = OversecAsciiArmoredOutputStream(baos)
                aos.setHeader("Charset", "utf-8")
                return try {
                    aos.write(raw)
                    aos.flush()
                    aos.close()
                    String(baos.toByteArray(), Charset.forName("UTF-8"))
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }


            } else {
                return null
            }
        }

        @Throws(IOException::class)
        fun parseMessageAsciiArmoured(s: String): Outer.Msg {
            val ais = ArmoredInputStream(IOUtils.toInputStream(s, "UTF-8"))
            val raw = IOUtils.toByteArray(ais)

            if (raw.isEmpty()) {
                throw IOException("bad ascii armoured text")
            }

            val builderMsg = Outer.Msg.newBuilder()
            val pgpMsgBuilder = builderMsg.msgTextGpgV0Builder

            pgpMsgBuilder.ciphertext = ByteString.copyFrom(raw)

            builderMsg.setMsgTextGpgV0(pgpMsgBuilder)

            return builderMsg.build()
        }

        private val P_ASCII_ARMOR_BEGIN = Pattern.compile("-----BEGIN (.*)-----")
        private const val F_ASCII_ARMOR_END = "-----END %s-----"
        private const val LINE_LENGTH = 64

        fun sanitizeAsciiArmor(ss: String): String? {
            var s = ss
            //remove anything before ----START ....
            //remove anything after  ----END ....

            val mStart = P_ASCII_ARMOR_BEGIN.matcher(s)
            if (mStart.find()) {


                val posStart = mStart.start()

                val g1 = mStart.group(1)
                val end = String.format(F_ASCII_ARMOR_END, g1)

                val posEnd = s.indexOf(end, posStart)
                if (posEnd >= 0) {
                    s = s.substring(posStart, posEnd + end.length)
                    val sb = StringBuilder()
                    //adjust line length

                    var curLineLength = 0
                    try {
                        var bufReader = BufferedReader(StringReader(s))
                        sb.append(bufReader.readLine()).append("\n") //write BEGIN..

                        //write headers
                        bufReader.forEachLine {
                            if (it.contains(": ")) {
                                sb.append(it)
                                sb.append("\n")
                            }
                        }

                        sb.append("\n")

                        //write body
                        bufReader = BufferedReader(StringReader(s))
                        bufReader.readLine() //read BEGIN
                        bufReader.forEachLine {
                            if (!it.startsWith(end) && it.isNotEmpty() && !it.contains(": ")) {
                                curLineLength += it.length
                                when {
                                    curLineLength > LINE_LENGTH -> {
                                        sb.append("\n")
                                        curLineLength = it.length
                                        sb.append(it)
                                    }
                                    curLineLength == LINE_LENGTH -> {
                                        curLineLength = 0
                                        sb.append(it)
                                        sb.append("\n")
                                    }
                                    else -> {
                                        sb.append(it)
                                    }
                                }
                            }
                        }
                        if (curLineLength > 0) {
                            sb.append("\n")
                        }
                        sb.append(end) //write END...
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return sb.toString()
                }
            }
            return null
        }
    }
}
