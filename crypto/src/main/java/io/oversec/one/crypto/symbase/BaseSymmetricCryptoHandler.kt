package io.oversec.one.crypto.symbase

import android.content.Context
import android.content.Intent
import com.google.protobuf.ByteString
import io.oversec.one.crypto.*
import io.oversec.one.crypto.proto.Inner
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.KeyNotCachedException
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.sym.SymmetricKeyPlain
import roboguice.util.Ln
import java.io.IOException
import java.security.GeneralSecurityException

abstract class BaseSymmetricCryptoHandler(ctx: Context) : AbstractCryptoHandler(ctx) {

    protected val mKeyCache: KeyCache = KeyCache.getInstance(ctx)
    protected abstract val method: EncryptionMethod

    override val displayEncryptionMethod: Int
        get() = R.string.encryption_method_sym

    @Throws(KeyNotCachedException::class)
    protected abstract fun getKeyByHashedKeyId(
        keyhash: Long,
        salt: ByteArray,
        cost: Int,
        encryptedText: String?
    ): SymmetricKeyPlain?


    @Throws(UserInteractionRequiredException::class)
    protected fun tryDecrypt(
        symMsg: Outer.MsgTextSymV0,
        encryptedText: String?
    ): SymmetricDecryptResult {

        if (symMsg.hasMsgTextChaChaV0()) {
            val chachaMsg = symMsg.msgTextChaChaV0
            val pkcl = chachaMsg.perKeyCiphertextList

            var key: SymmetricKeyPlain? = null
            var matchingPkc: Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext? = null
            for (pkc in pkcl) {

                // sym: key exists, but is not cached -> throws KeyNotCachedException, OK
                // simple (if key exists it is always cached)
                key = getKeyByHashedKeyId(
                    pkc.keyhash,
                    pkc.salt.toByteArray(),
                    chachaMsg.costKeyhash,
                    encryptedText
                )

                if (key != null) {
                    matchingPkc = pkc
                    break
                }
            }

            if (key == null) {
                Ln.d("SYM: NO MATCHING KEY")

                //sym: if key exists but not cached we will not reach here, a KeyNotCachedException will have been thrown before
                //sym: if key doesn't exists, return SYM_NO_MATCHING_KEY
                //simple: if key doesn't exist, throw a userInteraction exception to enter the key
                val keyHashes = LongArray(pkcl.size) {
                    pkcl[it].keyhash
                }
                val salts = Array(pkcl.size) {
                    pkcl[it].salt.toByteArray()
                }
                handleNoKeyFoundForDecryption(
                    keyHashes,
                    salts,
                    chachaMsg.costKeyhash,
                    encryptedText
                )

                return SymmetricDecryptResult(
                    method,
                    BaseDecryptResult.DecryptError.SYM_NO_MATCHING_KEY
                )
            } else {
                Ln.d("SYM: try decrypt with key %s", key.id)
                var rawData: ByteArray? = null
                try {
                    requireNotNull(matchingPkc)
                    rawData = tryDecryptChacha(matchingPkc, key)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return if (rawData != null) {
                    Ln.d("SYM: try last used key SUCCESS")
                    SymmetricDecryptResult(method, rawData, key.id)
                } else {
                    Ln.d("SYM: DECRYPTION FAILED")
                    SymmetricDecryptResult(
                        method,
                        BaseDecryptResult.DecryptError.SYM_DECRYPT_FAILED
                    )
                }
            }

        }
        Ln.d("SYM: DECRYPTION FAILED")
        return SymmetricDecryptResult(method, BaseDecryptResult.DecryptError.SYM_UNSUPPORTED_CIPHER)

    }

    @Throws(UserInteractionRequiredException::class)
    protected abstract fun handleNoKeyFoundForDecryption(
        keyHashes: LongArray,
        salts: Array<ByteArray>,
        costKeyhash: Int,
        encryptedText: String?
    )

    @Throws(
        IOException::class,
        GeneralSecurityException::class,
        UserInteractionRequiredException::class
    )
    override fun encrypt(
        innerData: Inner.InnerData,
        params: AbstractEncryptionParams,
        actionIntent: Intent?
    ): Outer.Msg {
        val p = params as BaseSymmetricEncryptionParams
        return encrypt(innerData.toByteArray(), p.keyIds)
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
    ): Outer.Msg {
        val p = params as BaseSymmetricEncryptionParams
        return encrypt(plainText.toByteArray(charset("UTF-8")), p.keyIds)
    }

    @Throws(GeneralSecurityException::class, IOException::class, KeyNotCachedException::class)
    private fun encrypt(plain: ByteArray, keyIds: List<Long>): Outer.Msg {

        val cost_key_id = KeyUtil.BCRYPT_SESSIONKEYID_COST_DEFAULT //TODO make configurable

        val builderMsg = Outer.Msg.newBuilder()
        val symMsgBuilder = builderMsg.msgTextSymV0Builder
        val chachaMsgBuilder = symMsgBuilder.msgTextChaChaV0Builder

        chachaMsgBuilder.costKeyhash = cost_key_id

        for (keyId in keyIds) {

            val pkcBuilder = chachaMsgBuilder.addPerKeyCiphertextBuilder()

            val salt = KeyUtil.getRandomBytes(SALT_LENGTH)
            val iv = KeyUtil.getRandomBytes(IV_LENGTH)
            val hashedKeyId = KeyUtil.calcSessionKeyId(keyId, salt, cost_key_id)


            val plainKey = mKeyCache.get(keyId)  //throws KeyNotCached

            val ciphertext = KeyUtil.encryptSymmetricChaCha(plain, salt, iv, plainKey)


            pkcBuilder.iv = ByteString.copyFrom(iv)
            pkcBuilder.keyhash = hashedKeyId
            pkcBuilder.salt = ByteString.copyFrom(salt)
            pkcBuilder.ciphertext = ByteString.copyFrom(ciphertext)

        }

        KeyUtil.erase(plain)

        symMsgBuilder.setMsgTextChaChaV0(chachaMsgBuilder)

        setMessage(builderMsg, symMsgBuilder)

        val msg = builderMsg.build()

        Ln.d("BCRYPT: ...encrypt")
        return msg
    }

    protected abstract fun setMessage(
        builderMsg: Outer.Msg.Builder,
        symMsgBuilder: Outer.MsgTextSymV0.Builder
    )

    @Throws(
        IOException::class,
        GeneralSecurityException::class,
        OversecChacha20Poly1305.MacMismatchException::class
    )
    protected fun tryDecryptChacha(
        matchingPkc: Outer.MsgTextChaChaV0_KeyAndSaltAndCiphertext,
        key: SymmetricKeyPlain
    ): ByteArray {
        return KeyUtil.decryptSymmetricChaCha(
            matchingPkc.ciphertext.toByteArray(),
            matchingPkc.salt.toByteArray(),
            matchingPkc.iv.toByteArray(),
            key
        )
    }

    companion object {

        const val IV_LENGTH = 8
        const val SALT_LENGTH = 16 //DO NOT CHANGE, needs to match Bcrypt.SALT_SIZE_BYTES

        fun getRawMessageJson(msg: Outer.Msg): String? {
            var chachaMsg: Outer.MsgTextChaChaV0? = null;
            if (msg.hasMsgTextSymSimpleV0()) {
                val symSimpleV0Msg = msg.msgTextSymSimpleV0
                if (symSimpleV0Msg.hasMsgTextChaChaV0()) {
                    chachaMsg = symSimpleV0Msg.msgTextChaChaV0
                }
            } else
                if (msg.hasMsgTextSymV0()) {
                    val symV0Msg = msg.msgTextSymV0
                    if (symV0Msg.hasMsgTextChaChaV0()) {
                        chachaMsg = symV0Msg.msgTextChaChaV0
                    }
                }

            if (chachaMsg != null) {
                val sb = StringBuilder()

                sb.append("{")
                sb.append("\n")
                sb.append("  \"cipher\":\"chacha20+poly1305\"")
                sb.append(",\n")
                sb.append("  \"cost_keyhash\":").append(chachaMsg.costKeyhash)
                sb.append(",\n")

                sb.append("  \"per_key_encrypted_data\": [")
                sb.append("\n")

                val pkcl = chachaMsg.perKeyCiphertextList
                for (pkc in pkcl) {
                    sb.append("   {")
                    sb.append("\n")
                    sb.append("  \"keyhash\":\"")
                        .append(SymUtil.byteArrayToHex(SymUtil.long2bytearray(pkc.keyhash)))
                        .append("\"")
                    sb.append(",\n")
                    sb.append("  \"salt\":\"")
                        .append(SymUtil.byteArrayToHex(pkc.salt.toByteArray())).append("\"")
                    sb.append(",\n")
                    sb.append("  \"iv\":\"")
                        .append(SymUtil.byteArrayToHex(pkc.iv.toByteArray())).append("\"")
                    sb.append(",\n")
                    sb.append("  \"ciphertext\":\"")
                        .append(SymUtil.byteArrayToHex(pkc.ciphertext.toByteArray()))
                        .append("\"")
                    sb.append("\n")
                    sb.append("   }")
                    sb.append("\n")
                }
                sb.append("  ]")
                sb.append("}")

                return sb.toString()


            } else {
                return null
            }
        }
    }
}
