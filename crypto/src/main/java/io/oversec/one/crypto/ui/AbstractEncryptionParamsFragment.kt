package io.oversec.one.crypto.ui

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.oversec.one.crypto.EncryptionMethod
import io.oversec.one.crypto.ui.util.StandaloneTooltipView

abstract class AbstractEncryptionParamsFragment : Fragment(), WithHelp {

    protected var mPackageName: String? = null
    protected var mIsForTextEncryption: Boolean = false
    protected var mView: View? = null
    protected var mTooltip: StandaloneTooltipView? = null
    protected var mArrowPosition: Int = 0

    protected lateinit var mContract: EncryptionParamsActivityContract
    protected var mHideToolTip: Boolean = false

    abstract val method: EncryptionMethod


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mContract = activity as EncryptionParamsActivityContract
        val bundle = this.arguments
        if (bundle != null) {
            mPackageName = bundle.getString(EXTRA_PACKAGENAME)
            mIsForTextEncryption = bundle.getBoolean(EXTRA_ISFORTEXT)
        }

        return mView
    }


    fun setArgs(packageName: String, isForTextEncryption: Boolean, state: Bundle?) {
        val bundle = state ?: Bundle()
        bundle.putString(EXTRA_PACKAGENAME, packageName)
        bundle.putBoolean(EXTRA_ISFORTEXT, isForTextEncryption)

        arguments = bundle
    }

    fun setToolTipPosition(i: Int) {
        mArrowPosition = i
        if (mTooltip != null) {
            mTooltip!!.setArrowPosition(i)
        }
    }

    // public abstract void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);

    abstract fun getTabTitle(ctx: Context): String

    abstract fun saveState(b: Bundle)

    fun setToolTipVisible(b: Boolean) {
        mHideToolTip = !b
    }

    companion object {
        private const val EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME"
        private const val EXTRA_ISFORTEXT = "EXTRA_ISFORTEXT"
    }
}
