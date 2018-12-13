package io.oversec.one.crypto.symbase

import org.spongycastle.crypto.Mac
import org.spongycastle.crypto.StreamCipher
import org.spongycastle.crypto.engines.ChaChaEngine
import org.spongycastle.crypto.generators.Poly1305KeyGenerator
import org.spongycastle.crypto.macs.Poly1305
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import org.spongycastle.util.Arrays
import org.spongycastle.util.Pack
import roboguice.util.Ln


/**
 * Originally from
 * https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/crypto/tls/Chacha20Poly1305.java
 */
object OversecChacha20Poly1305 {
    private val ZEROES = ByteArray(15)

    fun enChacha(raw: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val encrypt = true

        val cp = KeyParameter(key)
        val params = ParametersWithIV(cp, iv)
        val engine = ChaChaEngine()

        engine.init(encrypt, params)

        val ciphertext = ByteArray(raw.size)
        engine.processBytes(raw, 0, raw.size, ciphertext, 0)

        val macKeyBytes = Arrays.copyOf(key, key.size)
        Poly1305KeyGenerator.clamp(macKeyBytes)
        val macKey = KeyParameter(macKeyBytes)  //initRecord(engine, encrypt, 0, iv);
        val mac = calculateMAC(macKey, ciphertext, 0, ciphertext.size)

        val res = ByteArray(ciphertext.size + mac.size)

        System.arraycopy(ciphertext, 0, res, 0, ciphertext.size)
        System.arraycopy(mac, 0, res, ciphertext.size, mac.size)

        KeyUtil.erase(ciphertext)
        KeyUtil.erase(mac)

        return res
    }

    @Throws(OversecChacha20Poly1305.MacMismatchException::class)
    fun deChacha(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val encrypt = false

        val cp = KeyParameter(key)
        val params = ParametersWithIV(cp, iv)
        val engine = ChaChaEngine()

        engine.init(encrypt, params)
        if (getPlaintextLimit(ciphertext.size) < 0) {
            throw IllegalArgumentException()
        }

        var macKeyBytes = Arrays.copyOf(key, key.size)
        Poly1305KeyGenerator.clamp(macKeyBytes)
        var macKey = KeyParameter(macKeyBytes)  //initRecord(engine, encrypt, 0, iv);

        val plaintextLength = ciphertext.size - 16

        val calculatedMAC = calculateMAC(macKey, ciphertext, 0, plaintextLength)
        val receivedMAC = Arrays.copyOfRange(ciphertext, ciphertext.size - 16, ciphertext.size)

        KeyUtil.erase(macKeyBytes)

        if (!Arrays.constantTimeAreEqual(calculatedMAC, receivedMAC)) {
            //MAC might not match cause it was calculated with the old buggy Poly1305 impl of BC 1.54
            macKeyBytes = Arrays.copyOf(key, key.size)
            Poly1305KeyGenerator_asof_BC_1_54.clamp(macKeyBytes)
            macKey = KeyParameter(macKeyBytes)  //initRecord(engine, encrypt, 0, iv);
            val calculatedMAC1_54= calculateMAC_1_54(macKey, ciphertext, 0, plaintextLength)
            KeyUtil.erase(macKeyBytes)
            if (!Arrays.constantTimeAreEqual(calculatedMAC1_54, receivedMAC)) {
                throw MacMismatchException()
            }
            Ln.w("DECHACHA succesfully workarounded an incorrect old Poly1305 MAC")
        }

        val output = ByteArray(plaintextLength)
        engine.processBytes(ciphertext, 0, plaintextLength, output, 0)

        KeyUtil.erase(calculatedMAC)
        KeyUtil.erase(receivedMAC)

        return output
    }

    private fun getPlaintextLimit(ciphertextLimit: Int): Int {
        return ciphertextLimit - 16
    }

    private fun initRecord(
        cipher: StreamCipher,
        forEncryption: Boolean,
        seqNo: Long,
        iv: ByteArray
    ): KeyParameter {
        cipher.init(forEncryption, ParametersWithIV(null, iv))
        return generateRecordMACKey(cipher)
    }

    private fun generateRecordMACKey(cipher: StreamCipher): KeyParameter {
        val firstBlock = ByteArray(64)
        cipher.processBytes(firstBlock, 0, firstBlock.size, firstBlock, 0)

        val macKey = KeyParameter(firstBlock, 0, 32)
        Arrays.fill(firstBlock, 0.toByte())
        return macKey
    }


    private fun calculateMAC(macKey: KeyParameter, buf: ByteArray, off: Int, len: Int): ByteArray {
        val mac = Poly1305()
        mac.init(macKey)

        updateRecordMACText(mac, buf, off, len)
        updateRecordMACLength(mac, len)

        val output = ByteArray(mac.macSize)
        mac.doFinal(output, 0)
        return output
    }

    private fun calculateMAC_1_54(macKey: KeyParameter, buf: ByteArray, off: Int, len: Int): ByteArray {
        val mac = Poly1305_asof_BC_1_54()
        mac.init(macKey)

        updateRecordMACText(mac, buf, off, len)
        updateRecordMACLength(mac, len)

        val output = ByteArray(mac.macSize)
        mac.doFinal(output, 0)
        return output
    }

    private fun updateRecordMACLength(mac: Mac, len: Int) {
        val longLen = Pack.longToLittleEndian(len.toLong() and 0xFFFFFFFFL)
        mac.update(longLen, 0, longLen.size)
    }

    private fun updateRecordMACText(mac: Mac, buf: ByteArray, off: Int, len: Int) {
        mac.update(buf, off, len)

        val partial = len % 16
        if (partial != 0) {
            mac.update(ZEROES, 0, 16 - partial)
        }
    }

    class MacMismatchException : Exception()
}
