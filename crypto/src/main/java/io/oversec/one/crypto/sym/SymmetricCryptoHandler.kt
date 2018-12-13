package io.oversec.one.crypto.sym

import android.content.Context
import android.content.Intent
import io.oversec.one.crypto.AbstractEncryptionParams
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.EncryptionMethod
import io.oversec.one.crypto.UserInteractionRequiredException
import io.oversec.one.crypto.encoding.Base64XCoder
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.ui.SymmetricBinaryEncryptionInfoFragment
import io.oversec.one.crypto.sym.ui.SymmetricTextEncryptionInfoFragment
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler
import io.oversec.one.crypto.symbase.SymmetricDecryptResult
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment

class SymmetricCryptoHandler(ctx: Context) : BaseSymmetricCryptoHandler(ctx) {

    private val mKeyStore: OversecKeystore2

    override val method: EncryptionMethod
        get() = EncryptionMethod.SYM

    init {
        mKeyStore = OversecKeystore2.getInstance(ctx)
    }


    override fun buildDefaultEncryptionParams(tdr: BaseDecryptResult): AbstractEncryptionParams {
        val r = tdr as SymmetricDecryptResult
        return SymmetricEncryptionParams(r.symmetricKeyId, Base64XCoder.ID, null)
    }


    @Throws(UserInteractionRequiredException::class)
    override fun decrypt(
        msg: Outer.Msg,
        actionIntent: Intent?,
        encryptedText: String?
    ): BaseDecryptResult {
        return tryDecrypt(msg.msgTextSymV0, encryptedText)
    }

    override fun getTextEncryptionInfoFragment(packagename: String): AbstractTextEncryptionInfoFragment {
        return SymmetricTextEncryptionInfoFragment.newInstance(packagename)
    }


    override fun getBinaryEncryptionInfoFragment(packagename: String): AbstractBinaryEncryptionInfoFragment {
        return SymmetricBinaryEncryptionInfoFragment.newInstance(packagename)
    }


    @Throws(KeyNotCachedException::class)
    override fun getKeyByHashedKeyId(
        keyhash: Long,
        salt: ByteArray,
        cost: Int,
        encryptedText: String?
    ): SymmetricKeyPlain? {
        val keyId = mKeyStore.getKeyIdByHashedKeyId(keyhash, salt, cost)
        return if (keyId == null) null else mKeyCache.get(keyId)
    }

    @Throws(UserInteractionRequiredException::class)
    override fun handleNoKeyFoundForDecryption(
        keyHashes: LongArray,
        salts: Array<ByteArray>,
        costKeyhash: Int,
        encryptedText: String?
    ) {
        //noop
    }

    override fun setMessage(
        builderMsg: Outer.Msg.Builder,
        symMsgBuilder: Outer.MsgTextSymV0.Builder
    ) {
        builderMsg.setMsgTextSymV0(symMsgBuilder)
    }


    fun hasAnyKey(): Boolean {
        return !mKeyStore.isEmpty
    }

    companion object {

        const val BCRYPT_FINGERPRINT_COST = 10

        init {
            OversecKeystore2.noop()//init security provider
        }
    }
}
