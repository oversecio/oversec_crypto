package io.oversec.one.crypto.sym

import java.util.Date

class SymmetricKeyEncrypted {

    var cost: Int = 0
    var iv: ByteArray? = null
    var id: Long = 0
    var salt: ByteArray? = null
    var ciphertext: ByteArray? = null
    var createdDate: Date? = null
    var confirmedDate: Date? = null
    var name: String? = null

    /*emtpy constructor needed for WaspDb*/
    constructor()

    constructor(
        id: Long,
        name: String,
        createdDate: Date,
        salt: ByteArray,
        iv: ByteArray,
        cost: Int,
        ciphertext: ByteArray
    ) {
        this.id = id
        this.name = name

        this.createdDate = createdDate
        this.salt = salt
        this.iv = iv
        this.cost = cost
        this.ciphertext = ciphertext
    }

}
