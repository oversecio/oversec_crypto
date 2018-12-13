package io.oversec.one.crypto

import com.google.protobuf.InvalidProtocolBufferException
import io.oversec.one.crypto.proto.Inner
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

open class BaseDecryptResult {

    enum class Result {
        OK, RETRY, FAILED_PERMANENTLY
    }

    enum class DecryptError {
        SYM_UNSUPPORTED_CIPHER, SYM_NO_MATCHING_KEY, SYM_DECRYPT_FAILED, PGP_ERROR, PROTO_ERROR, NO_HANDLER
    }

    open var encryptionMethod: EncryptionMethod?
        protected set

    protected var mDecryptedRawData: ByteArray? = null

    var result: Result
        protected set

    var error: DecryptError? = null
        protected set

    var errorMessage: String? = null
        protected set

    constructor(method: EncryptionMethod, rawData: ByteArray) {
        encryptionMethod = method
        mDecryptedRawData = rawData
        result = Result.OK
    }

    @JvmOverloads
    constructor(method: EncryptionMethod?, err: DecryptError, errMsg: String? = null) {
        encryptionMethod = method
        error = err
        errorMessage = errMsg
        result = Result.RETRY
        when (error) {
            BaseDecryptResult.DecryptError.SYM_DECRYPT_FAILED -> result = Result.RETRY
            BaseDecryptResult.DecryptError.SYM_NO_MATCHING_KEY -> result = Result.RETRY
            BaseDecryptResult.DecryptError.PGP_ERROR -> {
                Exception().printStackTrace()
                result = Result.RETRY //TODO: should be failed permanently or?
            }
            BaseDecryptResult.DecryptError.PROTO_ERROR -> result = Result.FAILED_PERMANENTLY
            BaseDecryptResult.DecryptError.NO_HANDLER -> result = Result.RETRY
            BaseDecryptResult.DecryptError.SYM_UNSUPPORTED_CIPHER -> result = Result.RETRY
        }
    }

    val isOk: Boolean
        get() = result == Result.OK

    val isRetry: Boolean
        get() = result == Result.RETRY

    val isOversecNode: Boolean
        get() = (result == Result.OK
                || error == DecryptError.SYM_NO_MATCHING_KEY
                || error == DecryptError.SYM_UNSUPPORTED_CIPHER
                || error == DecryptError.SYM_DECRYPT_FAILED
                || error == DecryptError.NO_HANDLER
                || error == DecryptError.PGP_ERROR)


    val decryptedDataAsInnerData: Inner.InnerData
        @Throws(InvalidProtocolBufferException::class)
        get() = Inner.InnerData.parseFrom(mDecryptedRawData)

    val decryptedDataAsUtf8String: String
        @Throws(UnsupportedEncodingException::class)
        get() = String(requireNotNull(mDecryptedRawData), Charset.forName("UTF-8"))

    override fun toString(): String {
        return "BaseDecryptResult{" +
                "decryptedData='" + mDecryptedRawData + '\''.toString() +
                ", result=" + result +
                ", error=" + error +
                '}'.toString()
    }
}
