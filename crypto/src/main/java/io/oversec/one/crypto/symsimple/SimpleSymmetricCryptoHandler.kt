package io.oversec.one.crypto.symsimple

import android.content.Context
import android.content.Intent
import io.oversec.one.crypto.AbstractEncryptionParams
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.EncryptionMethod
import io.oversec.one.crypto.UserInteractionRequiredException
import io.oversec.one.crypto.encoding.Base64XCoder
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.KeyNotCachedException
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.sym.SymmetricKeyPlain
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler
import io.oversec.one.crypto.symbase.SymmetricDecryptResult
import io.oversec.one.crypto.symsimple.ui.AddPasswordKeyActivity
import io.oversec.one.crypto.symsimple.ui.SimpleSymmetricBinaryEncryptionInfoFragment
import io.oversec.one.crypto.symsimple.ui.SimpleSymmetricTextEncryptionInfoFragment
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment

class SimpleSymmetricCryptoHandler(ctx: Context) : BaseSymmetricCryptoHandler(ctx) {

    override val method: EncryptionMethod
        get() = EncryptionMethod.SIMPLESYM


    override fun buildDefaultEncryptionParams(tdr: BaseDecryptResult): AbstractEncryptionParams {
        val r = tdr as SymmetricDecryptResult
        requireNotNull(r.symmetricKeyId)
        return SimpleSymmetricEncryptionParams(r.symmetricKeyId!!, Base64XCoder.ID, null)
    }


    @Throws(UserInteractionRequiredException::class)
    override fun decrypt(
        msg: Outer.Msg,
        actionIntent: Intent?,
        encryptedText: String?
    ): BaseDecryptResult {
        return tryDecrypt(msg.msgTextSymSimpleV0, encryptedText)
    }

    override fun getTextEncryptionInfoFragment(packagename: String): AbstractTextEncryptionInfoFragment {
        return SimpleSymmetricTextEncryptionInfoFragment.newInstance(packagename)
    }

    override fun getBinaryEncryptionInfoFragment(packagename: String): AbstractBinaryEncryptionInfoFragment {
        return SimpleSymmetricBinaryEncryptionInfoFragment.newInstance(packagename)
    }

    @Throws(KeyNotCachedException::class)
    override fun getKeyByHashedKeyId(
        keyhash: Long,
        salt: ByteArray,
        cost: Int,
        encryptedText: String?
    ): SymmetricKeyPlain? {
        return mKeyCache.getKeyByHashedKeyId(keyhash, salt, cost)
    }

    @Throws(UserInteractionRequiredException::class)
    override fun handleNoKeyFoundForDecryption(
        keyHashes: LongArray,
        salts: Array<ByteArray>,
        costKeyhash: Int,
        encryptedText: String?
    ) {
        throw buildUserInteractionRequiredException(keyHashes, salts, costKeyhash, encryptedText)
    }

    private fun buildUserInteractionRequiredException(
        keyHashes: LongArray,
        salts: Array<ByteArray>,
        sessionKeyCost: Int,
        encryptedText: String?
    ): UserInteractionRequiredException {
        return KeyNotCachedException(
            AddPasswordKeyActivity.buildPendingIntent(
                mCtx,
                keyHashes,
                salts,
                sessionKeyCost,
                encryptedText
            )
        )
    }


    override fun setMessage(
        builderMsg: Outer.Msg.Builder,
        symMsgBuilder: Outer.MsgTextSymV0.Builder
    ) {
        builderMsg.setMsgTextSymSimpleV0(symMsgBuilder)
    }

    companion object {
        val KEY_DERIVATION_SALT =
            SymUtil.hexStringToByteArray("B16B00B566DEFEC8DEFEC8B16B00B566") //constant , this is intentional and can not be avoided for "simple" symmetric encryption
        const val KEY_DERIVATION_COST = 8
        const val KEY_ID_COST = 6
    }


}
