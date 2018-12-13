package io.oversec.one.crypto

import android.content.Context
import android.content.Intent
import io.oversec.one.crypto.proto.Inner
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment
import java.io.IOException
import java.security.GeneralSecurityException


abstract class AbstractCryptoHandler(protected val mCtx: Context) {

    abstract val displayEncryptionMethod: Int

    @Throws(
        GeneralSecurityException::class,
        UserInteractionRequiredException::class,
        IOException::class
    )
    abstract fun encrypt(
        innerData: Inner.InnerData,
        params: AbstractEncryptionParams,
        actionIntent: Intent?
    ): Outer.Msg?

    @Throws(
        GeneralSecurityException::class,
        UserInteractionRequiredException::class,
        IOException::class
    )
    abstract fun encrypt(
        plainText: String,
        params: AbstractEncryptionParams,
        actionIntent: Intent?
    ): Outer.Msg?

    @Throws(UserInteractionRequiredException::class)
    abstract fun decrypt(
        msg: Outer.Msg,
        actionIntent: Intent?,
        encryptedText: String?
    ): BaseDecryptResult?

    abstract fun getTextEncryptionInfoFragment(packagename: String): AbstractTextEncryptionInfoFragment

    abstract fun getBinaryEncryptionInfoFragment(packagename: String): AbstractBinaryEncryptionInfoFragment

    abstract fun buildDefaultEncryptionParams(tdr: BaseDecryptResult): AbstractEncryptionParams
}
