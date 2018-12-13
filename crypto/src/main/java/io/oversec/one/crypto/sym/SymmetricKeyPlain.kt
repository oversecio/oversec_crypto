package io.oversec.one.crypto.sym

import io.oversec.one.crypto.symbase.KeyUtil
import java.io.Serializable
import java.util.Date

class SymmetricKeyPlain : Serializable {
    var isSimpleKey: Boolean = false
    var id: Long = 0
    var raw: ByteArray? = null
    var createdDate: Date? = null
    var name: String? = null

    /*emtpy constructor needed for WaspDb*/
    constructor()

    constructor(
        id: Long,
        name: String,
        createdDate: Date,
        raw: ByteArray,
        isSimpleKey: Boolean
    ) : this(id, name, createdDate, raw) {
        this.isSimpleKey = isSimpleKey
    }

    constructor(id: Long, name: String, createdDate: Date, raw: ByteArray) {
        this.id = id
        this.name = name
        this.createdDate = createdDate
        this.raw = raw
    }

    constructor(raw: ByteArray) {
        this.createdDate = Date()
        this.raw = raw
    }

    fun clearKeyData() {
        KeyUtil.erase(raw)
        raw = null
    }
}
