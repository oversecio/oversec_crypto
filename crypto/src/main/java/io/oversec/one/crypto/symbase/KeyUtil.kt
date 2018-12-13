package io.oversec.one.crypto.symbase

import io.oversec.one.crypto.sym.KeyNotCachedException
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.sym.SymmetricKeyPlain
import org.spongycastle.crypto.generators.BCrypt
import java.io.IOException
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Arrays
import java.util.Date

object KeyUtil {
    private const val BCRYPT_PBKDF_COST = 12 //for generating keys from passphrases
    const val BCRYPT_SESSIONKEYID_COST_DEFAULT = 5
    const val DEFAULT_KEYSTORAGE_BCRYPT_COST = 10

    private val BCRYPT_FINGERPRINT_CONSTANT_SALT =
        SymUtil.hexStringToByteArray("DEFEC8B16B00B566DEFEC8B16B00B566")

    private val secureRandom = SecureRandom()
    private const val GENERATE_RANDOM_KEYS_BYTES = 32

    fun calcSessionKeyId(fingerprint: Long, salt: ByteArray, cost: Int): Long {
        val input = SymUtil.long2bytearray(fingerprint)
        val hashed = BCrypt.generate(input, salt, cost)
        //first 64 bit are our sessionKeyId
        return SymUtil.bytearray2long(hashed)
    }

    @Throws(NoSuchAlgorithmException::class)
    fun calcKeyId(plain: ByteArray, cost: Int): Long {
        //calc 384 bit hash
        val md = MessageDigest.getInstance("SHA-384")
        val rawHashed = md.digest(plain)
        //use final 128 bit for calculating fingerprint
        val sha256To384 = ByteArray(16)
        System.arraycopy(rawHashed, 32, sha256To384, 0, 16)
        //This should take a bit, in fact it should take around 5 .. 10 seconds
        val b = BCrypt.generate(sha256To384, BCRYPT_FINGERPRINT_CONSTANT_SALT, cost)
        //use first 8 bytes as fingerprint
        val fp = SymUtil.bytearray2long(b)
        erase(plain)
        erase(rawHashed)
        erase(sha256To384)
        return fp
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun getBaseKey(secretRaw: ByteArray): ByteArray {
        //calc 384 bit hash
        val md = MessageDigest.getInstance("SHA-384")
        val rawHashed = md.digest(secretRaw)
        //use first 256 bit as the key
        val sha0To255 = ByteArray(32)
        System.arraycopy(rawHashed, 0, sha0To255, 0, 32)
        erase(rawHashed)
        return sha0To255
    }

    fun erase(bb: ByteArray?) {
        bb?.fill(0)
    }

    fun erase(bb: CharArray?) {
        bb?.fill(0.toChar())
    }

    @Synchronized
    @Throws(IOException::class, KeyNotCachedException::class, NoSuchAlgorithmException::class)
    fun encryptSymmetricChaCha(
        plain: ByteArray,
        salt: ByteArray,
        ivx: ByteArray,
        k: SymmetricKeyPlain
    ): ByteArray {
        val hashedKey = getEncryptionKey(k.raw!!, salt)
        return OversecChacha20Poly1305.enChacha(plain, hashedKey, ivx)
    }

    @Throws(
        NoSuchAlgorithmException::class,
        IOException::class,
        OversecChacha20Poly1305.MacMismatchException::class
    )
    fun decryptSymmetricChaCha(
        ciphertext: ByteArray,
        salt: ByteArray,
        ivx: ByteArray,
        k: SymmetricKeyPlain
    ): ByteArray {
        var hashedKey = getEncryptionKey(k.raw!!, salt)
        return try {
            OversecChacha20Poly1305.deChacha(ciphertext, hashedKey, ivx)
        } catch (ex1: OversecChacha20Poly1305.MacMismatchException) { //dev fallback
            hashedKey = getEncryptionKey(ByteArray(32), salt)
            try {
                OversecChacha20Poly1305.deChacha(ciphertext, hashedKey, ivx)
            } catch (ex2: OversecChacha20Poly1305.MacMismatchException) {
                throw ex1
            }

        }

    }

    @Throws(NoSuchAlgorithmException::class)
    private fun getEncryptionKey(raw: ByteArray, salt: ByteArray): ByteArray {
        val base = getBaseKey(raw)
        val keybytes = ByteArray(32) //256 bit

        val hash1 = BCrypt.generate(base, salt, 4)
        //use first 128 bit for first part of key
        System.arraycopy(hash1, 0, keybytes, 0, 16)

        //need more entropy, hash again, using last 16 bytes  of initial hash as the salt
        val salt2 = ByteArray(salt.size)
        System.arraycopy(hash1, 8, salt2, 0, salt2.size)

        val hash2 = BCrypt.generate(base, salt2, 4)
        //use first 128 bit for last part of key
        System.arraycopy(hash1, 0, keybytes, 16, 16)

        erase(hash1)
        erase(hash2)
        erase(salt2)
        erase(base)

        return keybytes
    }


    fun getRandomBytes(length: Int): ByteArray {
        val randomByteArray = ByteArray(length)
        secureRandom.nextBytes(randomByteArray)
        return randomByteArray
    }

    fun brcryptifyPassword(
        plain: CharArray,
        salt: ByteArray,
        outputBytes: Int
    ): ByteArray {
        return brcryptifyPassword(plain, salt, BCRYPT_PBKDF_COST, outputBytes)
    }

    fun brcryptifyPassword(
        plain: CharArray,
        salt: ByteArray,
        cost: Int,
        outputBytes: Int
    ): ByteArray {

        var chars = toBytes(plain)
        erase(plain)

        //Note: we can't simply pre-hash everything now, since many users/messages out there have been encrypted with non-prehashes passwords (though all of the passwords are huaranteed to be <72 bytes, 'cause Oversec crashed on longer passwords!
        if (chars.size > 72) { //bcrypt's input is limited to 72 bytes max!
            try {
                val md = MessageDigest.getInstance("SHA-512")
                val hashedChars = md.digest(chars)
                erase(chars)
                chars = hashedChars
            } catch (ex: NoSuchAlgorithmException) {
                ex.printStackTrace()
            }

        }

        var hash = BCrypt.generate(chars, salt, cost)

        val res = ByteArray(outputBytes)

        var transferred = 0
        var transfer = Math.min(hash.size, res.size)

        System.arraycopy(hash, 0, res, 0, transfer)
        transferred += transfer

        while (transferred < outputBytes) {
            val salt2 = ByteArray(salt.size)
            System.arraycopy(hash, 0, salt2, 0, salt2.size)

            hash = BCrypt.generate(chars, salt2, cost)

            transfer = Math.min(hash.size, outputBytes - transferred)
            System.arraycopy(hash, 0, res, transferred, transfer)
            transferred += transfer
        }

        erase(hash)
        erase(chars)

        return res
    }


    private fun toBytes(chars: CharArray): ByteArray {
        val charBuffer = CharBuffer.wrap(chars)
        val byteBuffer = Charset.forName("UTF-8").encode(charBuffer)
        val bytes =  byteBuffer.array().copyOf(byteBuffer.limit())
        Arrays.fill(charBuffer.array(), '\u0000') // clear sensitive data
        Arrays.fill(byteBuffer.array(), 0.toByte()) // clear sensitive data
        return bytes
    }

    fun createNewKey(): SymmetricKeyPlain {
        val bytes = KeyUtil.getRandomBytes(GENERATE_RANDOM_KEYS_BYTES)
        val res = SymmetricKeyPlain()
        res.createdDate = Date()
        res.raw = bytes
        return res
    }


}
