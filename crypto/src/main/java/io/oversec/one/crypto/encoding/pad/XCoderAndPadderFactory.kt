package io.oversec.one.crypto.encoding.pad

import android.annotation.SuppressLint
import android.content.Context
import io.oversec.one.common.CoreContract
import io.oversec.one.crypto.Issues
import io.oversec.one.crypto.encoding.XCoderAndPadder
import io.oversec.one.crypto.encoding.XCoderFactory
import java.util.ArrayList

class XCoderAndPadderFactory private constructor(private val mCtx: Context) {
    private val mCore = CoreContract.instance
    private var mAll: MutableList<XCoderAndPadder> = ArrayList(0)
    private var mSym: MutableList<XCoderAndPadder> = ArrayList(0)
    private var mGpg: MutableList<XCoderAndPadder> = ArrayList(0)
    private var mSymExcludeInvisible: MutableList<XCoderAndPadder> = ArrayList(0)
    private var mGpgExcludeInvisible: MutableList<XCoderAndPadder> = ArrayList(0)
    private var mManualZeroWidthXcoder: XCoderAndPadder? = null

    fun reload() {
        mAll.clear()
        mManualZeroWidthXcoder = addZeroWidthXcoder(ManualPadder(mCtx))
        val contract = CoreContract.instance

        val allFromDb = contract.allPaddersSorted
        for (pc in allFromDb) {
            addZeroWidthXcoder(GutenbergPadder(mCtx, pc.name, pc.content))
        }

        mSym = ArrayList(mAll)
        mGpg = ArrayList(mAll)
        mSymExcludeInvisible = ArrayList()
        mGpgExcludeInvisible = ArrayList()

        val l = XCoderAndPadder(XCoderFactory.getInstance(mCtx).base64XCoder, null)
        mSym.add(0, l)
        mSymExcludeInvisible!!.add(0, l)
        mAll.add(l)

        val l2 = XCoderAndPadder(XCoderFactory.getInstance(mCtx).asciiArmouredGpgXCoder, null)
        mGpg!!.add(0, l2)
        mGpgExcludeInvisible!!.add(0, l2)
        mAll.add(l2)
    }

    private fun addZeroWidthXcoder(padder: AbstractPadder): XCoderAndPadder {
        val res = XCoderAndPadder(XCoderFactory.getInstance(mCtx).zeroWidthXCoder, padder)
        mAll.add(res)
        return res
    }

    fun getSym(packagename: String): ArrayList<XCoderAndPadder> {
        val res =
            ArrayList(if (Issues.cantHandleInvisibleEncoding(packagename)) mSymExcludeInvisible else mSym)
        if (mCore.isDbSpreadInvisibleEncoding(packagename)) {
            //spreaded doesn't work with manual padding
            res.remove(mManualZeroWidthXcoder)
        }

        return res
    }


    fun getGpg(packagename: String): ArrayList<XCoderAndPadder> {
        val res =
            ArrayList(if (Issues.cantHandleInvisibleEncoding(packagename)) mGpgExcludeInvisible else mGpg)
        if (mCore.isDbSpreadInvisibleEncoding(packagename)) {
            //spreaded doesn't work with manual padding
            res.remove(mManualZeroWidthXcoder)
        }

        return res
    }

    operator fun get(coderId: String, padderId: String?): XCoderAndPadder? {

        for (x in mAll) {
            if (x.xcoder.id == coderId) {
                if (padderId == null || x.padder == null) {
                    return x
                } else {
                    if (x.padder.id == padderId) {
                        return x
                    }
                }
            }
        }

        return null
    }

    companion object {

        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var INSTANCE: XCoderAndPadderFactory? = null

        fun getInstance(ctx: Context): XCoderAndPadderFactory =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: XCoderAndPadderFactory(ctx.applicationContext).also { INSTANCE = it; it.reload() } }

    }
}