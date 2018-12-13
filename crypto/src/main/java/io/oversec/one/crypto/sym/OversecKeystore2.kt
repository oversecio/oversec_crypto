package io.oversec.one.crypto.sym

import android.annotation.SuppressLint
import android.content.Context
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import io.oversec.one.crypto.proto.Kex
import io.oversec.one.crypto.symbase.KeyCache
import io.oversec.one.crypto.symbase.KeyUtil
import io.oversec.one.crypto.symbase.OversecChacha20Poly1305
import io.oversec.one.crypto.symbase.OversecKeyCacheListener
import net.rehacktive.waspdb.WaspFactory
import org.spongycastle.util.encoders.Base64
import org.spongycastle.util.encoders.DecoderException
import roboguice.util.Ln

import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.util.*

class OversecKeystore2 private constructor(private val mCtx: Context) {
    private val mKeyCache = KeyCache.getInstance(mCtx)
    private val mDb = WaspFactory.openOrCreateDatabase(mCtx.filesDir.path, DATABASE_NAME, null)
    private val mSymmetricEncryptedKeys = mDb.openOrCreateHash("symmetric_keys")

    private val mListeners = ArrayList<KeyStoreListener>()


    val encryptedKeys_sorted: List<SymmetricKeyEncrypted>
        get() {
            val list: List<SymmetricKeyEncrypted>? = mSymmetricEncryptedKeys.getAllValues()
            return list?.sortedBy {
                it.name
            } ?: emptyList()
        }

    val isEmpty: Boolean
        get() {
            val allKeys = mSymmetricEncryptedKeys.getAllKeys<Any>()
            return allKeys == null || allKeys.isEmpty()
        }


    fun clearAllCaches() {
        mKeyCache.clearAll()
    }


    @Synchronized
    @Throws(
        NoSuchAlgorithmException::class,
        IOException::class,
        OversecKeystore2.AliasNotUniqueException::class
    )
    fun addKey__longoperation(plainKey: SymmetricKeyPlain, password: CharArray): Long? {

        val allKeys: List<SymmetricKeyEncrypted> =
            mSymmetricEncryptedKeys.getAllValues() ?: emptyList()

        (allKeys.firstOrNull() {
            it.name == plainKey.name
        })?.run {
            throw AliasNotUniqueException(plainKey.name)
        }

        val id = KeyUtil.calcKeyId(
            Arrays.copyOf(plainKey.raw!!, plainKey.raw!!.size),
            SymmetricCryptoHandler.BCRYPT_FINGERPRINT_COST
        )
        plainKey.id = id
        val encKey = encryptSymmetricKey(plainKey, password)

        mSymmetricEncryptedKeys.put(id, encKey)

        mKeyCache.doCacheKey(plainKey, 0)

        fireChange()

        return id
    }



@Synchronized
fun confirmKey(id: Long?) {
    val k = getSymmetricKeyEncrypted(id)
    k?.confirmedDate = Date()
    mSymmetricEncryptedKeys.put(id, k)
}

@Synchronized
fun getConfirmDate(id: Long?): Date? {
    return getSymmetricKeyEncrypted(id)?.confirmedDate
}

@Synchronized
fun getCreatedDate(id: Long?): Date? {
    val k = mSymmetricEncryptedKeys.get<SymmetricKeyEncrypted>(id)
    return k?.createdDate

}

@Synchronized
fun deleteKey(id: Long?) {
    mSymmetricEncryptedKeys.remove(id)
    fireChange()
}


@Synchronized
fun getKeyIdByHashedKeyId(hashedKeyId: Long, salt: ByteArray, cost: Int): Long? {

    val allIds = mSymmetricEncryptedKeys.getAllKeys<Long>()
    if (allIds != null) {
        for (id in allIds) {
            val aSessionKeyId = KeyUtil.calcSessionKeyId(id!!, salt, cost)

            if (aSessionKeyId == hashedKeyId) {
                return id
            }
        }
    }
    return null
}


@Synchronized
fun hasKey(keyId: Long?): Boolean {
    return mSymmetricEncryptedKeys.get<Any>(keyId) != null
}


@Synchronized
@Throws(KeyNotCachedException::class)
fun getPlainKeyAsTransferBytes(id: Long?): ByteArray {
    val k = mKeyCache[id]
    return getPlainKeyAsTransferBytes(k.raw)
}

@Synchronized
@Throws(IOException::class, OversecChacha20Poly1305.MacMismatchException::class)
fun doCacheKey__longoperation(keyId: Long?, pw: CharArray, ttl: Long) {
    val k = mSymmetricEncryptedKeys.get<SymmetricKeyEncrypted>(keyId)
        ?: throw IllegalArgumentException("invalid key id")

    var dec: SymmetricKeyPlain? = null //it might still/already be cached
    try {
        dec = mKeyCache[keyId]
    } catch (e: KeyNotCachedException) {
        //ignore
    }

    if (dec == null) {
        dec = decryptSymmetricKey(k, pw)
    }
    mKeyCache.doCacheKey(dec, ttl)
}


@Synchronized
@Throws(KeyNotCachedException::class)
fun getPlainKeyData(id: Long?): ByteArray? {

    val k = mKeyCache[id]
    return k.raw

}

@Synchronized
@Throws(KeyNotCachedException::class)
fun getPlainKey(id: Long?): SymmetricKeyPlain {

    return mKeyCache[id]

}

@Synchronized
fun addKeyCacheListener(l: OversecKeyCacheListener) {
    mKeyCache.addKeyCacheListener(l)
}

@Synchronized
fun removeKeyCacheListener(l: OversecKeyCacheListener) {
    mKeyCache.removeKeyCacheListener(l)

}

fun getSymmetricKeyEncrypted(id: Long?): SymmetricKeyEncrypted? {
    return mSymmetricEncryptedKeys.get(id)
}

fun hasName(name: String): Boolean {
    val l = mSymmetricEncryptedKeys.getAllValues<SymmetricKeyEncrypted>()
    l ?: return false;
    for (key in l) {
        if (key.name == name) {
            return true
        }
    }
    return false
}


@Throws(IOException::class)
fun encryptSymmetricKey(
    plainKey: SymmetricKeyPlain,
    password: CharArray
): SymmetricKeyEncrypted {
    val cost = KeyUtil.DEFAULT_KEYSTORAGE_BCRYPT_COST

    val bcrypt_salt = KeyUtil.getRandomBytes(16)

    val bcryptedPassword = KeyUtil.brcryptifyPassword(password, bcrypt_salt, cost, 32)
    KeyUtil.erase(password)

    val chachaIv = KeyUtil.getRandomBytes(8)

    val ciphertext =
        OversecChacha20Poly1305.enChacha(plainKey.raw!!, bcryptedPassword, chachaIv)

    Ln.w("XXX encryptSymmetricKey password="+String(password))
    Ln.w("XXX encryptSymmetricKey bcryptedPassword="+bytesToHex(bcryptedPassword))
    Ln.w("XXX encryptSymmetricKey ciphertext="+bytesToHex(ciphertext!!))
    Ln.w("XXX encryptSymmetricKey iv="+bytesToHex(chachaIv))
    Ln.w("XXX encryptSymmetricKey salt="+bytesToHex(bcrypt_salt))

    KeyUtil.erase(bcryptedPassword)


    return SymmetricKeyEncrypted(
        plainKey.id, plainKey.name!!, plainKey.createdDate!!,
        bcrypt_salt, chachaIv, cost, ciphertext
    )

}


    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v.ushr(4)]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun hexToBytes(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                    ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

@Throws(IOException::class, OversecChacha20Poly1305.MacMismatchException::class)
fun decryptSymmetricKey(k: SymmetricKeyEncrypted, password: CharArray): SymmetricKeyPlain {

    Ln.w("XXX decryptSymmetricKey password="+String(password))

    val bcryptedPassword = KeyUtil.brcryptifyPassword(password, k.salt!!, k.cost, 32)
    KeyUtil.erase(password)

    Ln.w("XXX decryptSymmetricKey bcryptedPassword="+bytesToHex(bcryptedPassword))
    Ln.w("XXX decryptSymmetricKey ciphertext="+bytesToHex(k.ciphertext!!))
    Ln.w("XXX decryptSymmetricKey iv="+bytesToHex(k.iv!!))
    Ln.w("XXX decryptSymmetricKey salt="+bytesToHex(k.salt!!))

    val raw = OversecChacha20Poly1305.deChacha(k.ciphertext!!, bcryptedPassword, k.iv!!)
    KeyUtil.erase(bcryptedPassword)


    return SymmetricKeyPlain(
        k.id, k.name!!, k.createdDate!!,
        raw
    )
}


class AliasNotUniqueException(val alias: String?) : Exception()


@Synchronized
fun addListener(v: KeyStoreListener) {
    mListeners.add(v)
}

@Synchronized
fun removeListener(v: KeyStoreListener) {
    mListeners.remove(v)
}

@Synchronized
private fun fireChange() {
    for (v in mListeners) {
        v.onKeyStoreChanged()
    }
}


interface KeyStoreListener {

    fun onKeyStoreChanged()

}

class Base64DecodingException(ex: DecoderException) : Exception(ex)

companion object {

    private const val DATABASE_NAME = "keystore"

    @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
    @Volatile
    private var INSTANCE: OversecKeystore2? = null

    init {
        Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
    }

    fun noop() {
        //just a dummy method we can call in order to make sure the static code get's initialized
    }


    fun getInstance(ctx: Context): OversecKeystore2 =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: OversecKeystore2(ctx.applicationContext).also { INSTANCE = it }
        }


    fun getPlainKeyAsTransferBytes(raw: ByteArray?): ByteArray {
        val builder = Kex.KeyTransferV0.newBuilder()
        val plainKeyBuilder = builder.symmetricKeyPlainV0Builder
        plainKeyBuilder.keydata = ByteString.copyFrom(raw!!)
        return builder.build().toByteArray()
    }

    fun getEncryptedKeyAsTransferBytes(key: SymmetricKeyEncrypted): ByteArray {
        val builder = Kex.KeyTransferV0.newBuilder()
        val encryptedKeyBuilder = builder.symmetricKeyEncryptedV0Builder
        encryptedKeyBuilder.id = key.id
        encryptedKeyBuilder.alias = key.name
        encryptedKeyBuilder.createddate = key.createdDate!!.time
        encryptedKeyBuilder.cost = key.cost
        encryptedKeyBuilder.iv = ByteString.copyFrom(key.iv!!)
        encryptedKeyBuilder.salt = ByteString.copyFrom(key.salt!!)
        encryptedKeyBuilder.ciphertext = ByteString.copyFrom(key.ciphertext!!)
        return builder.build().toByteArray()

    }

    @Throws(OversecKeystore2.Base64DecodingException::class)
    fun getEncryptedKeyFromBase64Text(text: String): SymmetricKeyEncrypted? {
        try {
            val data = Base64.decode(text)
            return getEncryptedKeyFromTransferBytes(data)
        } catch (ex: DecoderException) {
            throw Base64DecodingException(ex)
        }

    }

    fun getEncryptedKeyFromTransferBytes(data: ByteArray): SymmetricKeyEncrypted? {
        try {
            val transfer = Kex.KeyTransferV0
                .parseFrom(data)

            if (transfer.hasSymmetricKeyEncryptedV0()) {
                val encryptedKeyV0 = transfer.symmetricKeyEncryptedV0
                val cost = encryptedKeyV0.cost
                val ciphertext = encryptedKeyV0.ciphertext.toByteArray()
                val salt = encryptedKeyV0.salt.toByteArray()
                val iv = encryptedKeyV0.iv.toByteArray()
                val created = encryptedKeyV0.createddate
                val alias = encryptedKeyV0.alias
                val id = encryptedKeyV0.id
                return SymmetricKeyEncrypted(
                    id,
                    alias,
                    Date(created),
                    salt,
                    iv,
                    cost,
                    ciphertext
                )
            } else {
                Ln.w("data array doesn't contain secret key")
                return null
            }

        } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()
            return null
        }

    }

    @Throws(OversecKeystore2.Base64DecodingException::class)
    fun getPlainKeyFromBase64Text(text: String): SymmetricKeyPlain? {
        try {
            val data = Base64.decode(text)
            return getPlainKeyFromTransferBytes(data)
        } catch (ex: DecoderException) {
            throw Base64DecodingException(ex)
        }

    }

    fun getPlainKeyFromTransferBytes(data: ByteArray): SymmetricKeyPlain? {
        try {
            val transfer = Kex.KeyTransferV0
                .parseFrom(data)

            return if (transfer.hasSymmetricKeyPlainV0()) {
                val plainKeyV0 = transfer.symmetricKeyPlainV0
                val keyBytes = plainKeyV0.keydata.toByteArray()
                SymmetricKeyPlain(keyBytes)
            } else {
                Ln.w("data array doesn't contain secret key")
                null
            }
        } catch (e: InvalidProtocolBufferException) {
            e.printStackTrace()
            return null
        }
    }
}
}
