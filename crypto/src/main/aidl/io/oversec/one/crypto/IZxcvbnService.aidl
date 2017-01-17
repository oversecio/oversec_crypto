package io.oversec.one.crypto;

import io.oversec.one.crypto.ZxcvbnResult;

interface IZxcvbnService {

    ZxcvbnResult calcEntropy(String aString);
    void exit();
}
