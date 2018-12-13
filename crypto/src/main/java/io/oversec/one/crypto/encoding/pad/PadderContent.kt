package io.oversec.one.crypto.encoding.pad

import java.util.Comparator

class PadderContent {
    var key: Long = 0
        private set
    lateinit var name: String
    private lateinit  var mSort: String
    lateinit var content: String

    val contentBegin: String
        get() {
            val length = 30
            return if (content.length < 30) content else content.substring(0, length)
        }

    /*required empty constructor for waspDb*/
    constructor()

    constructor(name: String, content: String) {
        key = System.currentTimeMillis()
        mSort = name
        this.name = name
        this.content = content
    }

    constructor(sort: String, name: String, content: String) {
        key = System.currentTimeMillis()
        mSort = sort
        this.name = name
        this.content = content
    }

    fun setSort(v: String) {
        mSort = v
    }

    companion object {
        var sortComparator: Comparator<in PadderContent> =
            Comparator { lhs, rhs -> lhs.mSort.compareTo(rhs.mSort) }
    }
}
