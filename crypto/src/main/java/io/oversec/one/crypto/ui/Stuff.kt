package io.oversec.one.crypto.ui

import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.R

object Stuff {
    fun getUserInteractionRequiredText(loong: Boolean): Int {
        return if (loong) R.string.decrypt_error_user_interaction_required__long else R.string.decrypt_error_user_interaction_required
    }

    fun getErrorText(error: BaseDecryptResult.DecryptError): Int {
        return when (error) {
            BaseDecryptResult.DecryptError.SYM_DECRYPT_FAILED, BaseDecryptResult.DecryptError.SYM_NO_MATCHING_KEY -> R.string.decrypt_error_generic
            BaseDecryptResult.DecryptError.PGP_ERROR -> R.string.decrypt_error_gpg
            BaseDecryptResult.DecryptError.PROTO_ERROR -> R.string.decrypt_error_proto
            BaseDecryptResult.DecryptError.NO_HANDLER, BaseDecryptResult.DecryptError.SYM_UNSUPPORTED_CIPHER -> R.string.decrypt_error_common
        }
    }
}
