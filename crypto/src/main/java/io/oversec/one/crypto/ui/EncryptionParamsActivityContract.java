package io.oversec.one.crypto.ui;

import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.EncryptionMethod;


public interface EncryptionParamsActivityContract {

    int REQUEST_CODE_DOWNLOAD_KEY = 6002;
    int REQUEST_CODE_RECIPIENT_SELECTION = 6003;
    int REQUEST_CODE_OWNSIGNATUREKEY_SELECTION = 6005;
    int REQUEST_CODE__CREATE_NEW_KEY = 6006;


    void finishWithResultOk();

    void doEncrypt(AbstractEncryptionParams encryptionParams, boolean addLink);

    String getPadderId(EncryptionMethod method, String packageName);

    String getXCoderId(EncryptionMethod method, String packageName);

    void setXcoderAndPadder(EncryptionMethod method, String packageName, String coderId, String padderId);


}
