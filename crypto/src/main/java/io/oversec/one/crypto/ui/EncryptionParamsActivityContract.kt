package io.oversec.one.crypto.ui

import io.oversec.one.crypto.AbstractEncryptionParams
import io.oversec.one.crypto.EncryptionMethod

interface EncryptionParamsActivityContract {

    fun finishWithResultOk()

    fun doEncrypt(encryptionParams: AbstractEncryptionParams, addLink: Boolean)

    fun getPadderId(method: EncryptionMethod, packageName: String): String

    fun getXCoderId(method: EncryptionMethod, packageName: String): String

    fun setXcoderAndPadder(
        method: EncryptionMethod,
        packageName: String,
        coderId: String,
        padderId: String
    )

    companion object {
        const val REQUEST_CODE_DOWNLOAD_KEY = 6002
        const val REQUEST_CODE_RECIPIENT_SELECTION = 6003
        const val REQUEST_CODE_OWNSIGNATUREKEY_SELECTION = 6005
        const val REQUEST_CODE__CREATE_NEW_KEY = 6006
    }


}
