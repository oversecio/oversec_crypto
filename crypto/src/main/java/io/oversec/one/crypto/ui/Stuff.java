package io.oversec.one.crypto.ui;

import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.R;

public class Stuff {
    public static int getUserInteractionRequiredText(boolean loong) {
        return loong ? R.string.decrypt_error_user_interaction_required__long : R.string.decrypt_error_user_interaction_required;
    }

    public static int getErrorText(BaseDecryptResult.DecryptError error) {
        switch (error) {
            case SYM_DECRYPT_FAILED:
            case SYM_NO_MATCHING_KEY:
                return R.string.decrypt_error_generic;
            case PGP_ERROR:
                return R.string.decrypt_error_gpg;
            case PROTO_ERROR:
                return R.string.decrypt_error_proto;
            case NO_HANDLER:
            case SYM_UNSUPPORTED_CIPHER:
                return R.string.decrypt_error_common;
        }
        return 0;
    }
}
