package io.oversec.one.crypto.gpg

import android.content.Context
import io.oversec.one.crypto.AbstractEncryptionParams
import io.oversec.one.crypto.EncryptionMethod
import java.util.*

class GpgEncryptionParams : AbstractEncryptionParams {

    private var mOwnPublicKey: Long = 0
    var isSign = true
    private var mPublicKeys: MutableSet<Long> = HashSet()


    val publicKeyIds: Set<Long>
        get() = mPublicKeys

    val ownPublicKey: Long?
        get() = mOwnPublicKey

    val allPublicKeyIds: LongArray
        get() {
            val allKeys = ArrayList(mPublicKeys)
            if (mOwnPublicKey != 0L) {
                allKeys.add(mOwnPublicKey)
            }
            return allKeys.toLongArray()
        }

    constructor(pkids: List<Long>?, coderId: String, padderId: String?) : super(
        EncryptionMethod.GPG,
        coderId,
        padderId
    ) {
        pkids?.run { mPublicKeys.addAll(this) }

    }

    constructor(keyIds: LongArray?, coderId: String, padderId: String?) : super(
        EncryptionMethod.GPG,
        coderId,
        padderId
    ) {
        keyIds?.run {
            mPublicKeys.addAll(this.asList())
        }

    }

    fun removePublicKey(keyId: Long?) {
        mPublicKeys.remove(keyId)
    }

    fun addPublicKeyIds(keyIds: Array<Long>, omitThisKey: Long) {
        keyIds.forEach {
            if (it != omitThisKey) {
                mPublicKeys.add(it)
            }
        }
    }

    fun setOwnPublicKey(k: Long) {
        mOwnPublicKey = k
        mPublicKeys.remove(k)
    }

    fun addPublicKey(id: Long) {
        mPublicKeys.add(id)
    }

    override fun isStillValid(ctx: Context): Boolean {
        //TODO: check that OpenKEychain service is still up and running,
        //TODO: check that we do still have the keys
        return true
    }

    fun setPublicKeyIds(ids: LongArray) {
        mPublicKeys.clear()
        mPublicKeys.addAll(ids.asList())
    }

    override fun toString(): String {

        return "GpgEncryptionParams{" +
                "mOwnPublicKey=" + mOwnPublicKey +
                ", mSign=" + isSign +
                ", mPublicKeys=" + Arrays.toString(mPublicKeys.toTypedArray()) +
                '}'.toString()
    }

    companion object {

        fun LongListToLongArray(a: List<Long>?): LongArray? {
            return a?.toLongArray()
        }

        fun LongArrayToLongList(a: LongArray?): List<Long>? {
            return a?.toList()
        }

//        fun longArrayToLongArray(a: LongArray?): Array<Long>? {
//            if (a == null) {
//                return null
//            }
//            val res = arrayOfNulls<Long>(a.size)
//            for (i in res.indices) {
//                res[i] = a[i]
//            }
//            return res
//        }
//
//        fun LongArrayTolongArray(a: Array<Long>?): LongArray? {
//            if (a == null) {
//                return null
//            }
//            val res = LongArray(a.size)
//            for (i in res.indices) {
//                res[i] = a[i]
//            }
//            return res
//        }
    }
}
