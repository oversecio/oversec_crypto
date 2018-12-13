package io.oversec.one.crypto.ui

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.CryptoHandlerFacade
import io.oversec.one.crypto.R
import io.oversec.one.crypto.images.xcoder.ImageXCoder
import io.oversec.one.crypto.proto.Outer

abstract class AbstractBinaryEncryptionInfoFragment : Fragment() {
    protected var mPackageName: String? = null
    protected lateinit var mGrid: ViewGroup

    protected lateinit var mView: View

    private var mTmpMsg: Outer.Msg? = null
    private var mTmpRes: BaseDecryptResult? = null
    private var mTmpCoder: ImageXCoder? = null
    private lateinit var mTvCoder: TextView
    private lateinit var mLblCoder: TextView
    private lateinit var mTvMeth: TextView
    private lateinit var mLblMeth: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mGrid = mView.findViewById<View>(R.id.grid) as ViewGroup

        val bundle = this.arguments
        if (bundle != null) {
            mPackageName = bundle.getString(EXTRA_PACKAGENAME)
        }

        mTvCoder = mView.findViewById<View>(R.id.tv_coder) as TextView
        mLblCoder = mView.findViewById<View>(R.id.lbl_coder) as TextView

        mTvMeth = mView.findViewById<View>(R.id.tv_meth) as TextView
        mLblMeth = mView.findViewById<View>(R.id.lbl_meth) as TextView

        mTvCoder.visibility = View.GONE
        mLblCoder.visibility = View.GONE

        //TODO move to a clean fragment impl with  args and state
        if (mTmpMsg != null) {
            handleSetData(mTmpMsg!!, mTmpRes, mTmpCoder)
            mTmpMsg = null
            mTmpRes = null
            mTmpCoder = null
        }
        return mView
    }

    //TODO move to a clean fragment impl with  args and state
    fun setData(msg: Outer.Msg, tdr: BaseDecryptResult, coder: ImageXCoder) {
        if (mView != null) {
            handleSetData(msg, tdr, coder)
        } else {
            mTmpMsg = msg
            mTmpRes = tdr
            mTmpCoder = coder
        }
    }

    protected open fun handleSetData(msg: Outer.Msg, tdr: BaseDecryptResult?, coder: ImageXCoder?) {
        //mTvCoder.setText(coder.getClass().getSimpleName());
        val encH = CryptoHandlerFacade.getInstance(activity).getCryptoHandler(tdr!!)
        if (encH != null) {
            mTvMeth.setText(encH.displayEncryptionMethod)
            mTvMeth.visibility = View.VISIBLE
            mLblMeth.visibility = View.VISIBLE
        }
    }

    fun setArgs(packageName: String) {
        val bundle = Bundle()
        bundle.putString(EXTRA_PACKAGENAME, packageName)
        arguments = bundle
    }

    companion object {
        private const val EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME"
    }
}

