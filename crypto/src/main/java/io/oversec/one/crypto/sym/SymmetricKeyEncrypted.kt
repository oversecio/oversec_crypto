package io.oversec.one.crypto.sym

import java.util.Date

class SymmetricKeyEncrypted {

    var cost: Int = 0
    lateinit var iv: ByteArray
    var id: Long = 0
    lateinit var salt: ByteArray
    lateinit var ciphertext: ByteArray
    lateinit var createdDate: Date
    lateinit var name: String
    var confirmedDate: Date? = null

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
