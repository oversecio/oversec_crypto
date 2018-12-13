package io.oversec.one.crypto.ui

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.borjabravo.readmoretextview.ReadMoreTextView
import com.google.protobuf.InvalidProtocolBufferException
import io.oversec.one.crypto.*
import io.oversec.one.crypto.encoding.XCoderFactory
import roboguice.util.Ln
import java.io.UnsupportedEncodingException

abstract class AbstractTextEncryptionInfoFragment : Fragment() {
    protected var mPackageName: String? = null
    protected lateinit var mGrid: ViewGroup
    protected var mTdr: BaseDecryptResult? = null
    protected lateinit var mOrigText: String
    protected lateinit var mView: View


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mGrid = mView.findViewById<View>(R.id.grid) as ViewGroup
        val bundle = this.arguments
        mPackageName = bundle?.getString(EXTRA_PACKAGENAME)
        return mView
    }

    open fun setData(
        activity: EncryptionInfoActivity,
        encodedText: String,
        tdr: BaseDecryptResult?,
        uix: UserInteractionRequiredException?,
        encryptionHandler: AbstractCryptoHandler
    ) {
        mTdr = tdr
        mOrigText = encodedText


        val lblErr = mView.findViewById<View>(R.id.lbl_err) as TextView
        val tvErr = mView.findViewById<View>(R.id.tv_err) as TextView
        //TextView lblEnc = (TextView) mView.findViewById(R.id.lbl_enc);
        val tvEnc = mView.findViewById<View>(R.id.tv_enc) as ReadMoreTextView
        val tvSize = mView.findViewById<View>(R.id.tv_size) as TextView
        val lblDec = mView.findViewById<View>(R.id.lbl_dec) as TextView
        val tvDec = mView.findViewById<View>(R.id.tv_dec) as TextView
        //TextView lblCoder = (TextView) mView.findViewById(R.id.lbl_coder);
        val tvCoder = mView.findViewById<View>(R.id.tv_coder) as TextView
        val lblMeth = mView.findViewById<View>(R.id.lbl_meth) as TextView
        val tvMeth = mView.findViewById<View>(R.id.tv_meth) as TextView
        val lblInnerPadding = mView.findViewById<View>(R.id.lbl_innerpadding) as TextView
        val tvInnerPadding = mView.findViewById<View>(R.id.tv_innerpadding) as TextView

        tvMeth.visibility = View.GONE
        lblMeth.visibility = View.GONE

        lblInnerPadding.visibility = View.GONE
        tvInnerPadding.visibility = View.GONE


        val TRIM_LENGTH = 160
        tvEnc.setTrimLength(TRIM_LENGTH)
        tvEnc.setTrimCollapsedText(
            getString(
                R.string.action_show_all_content,
                ""+(encodedText.length - TRIM_LENGTH)
            )
        )
        tvEnc.text = encodedText

        try {
            tvSize.text = getActivity().getString(
                R.string.bytes_size,
                ""+encodedText.toByteArray(charset("UTF-8")).size
            )
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        tvCoder.text = XCoderFactory.getInstance(activity).getEncodingInfo(encodedText)

        uix?.run {
            tvErr.setText(Stuff.getUserInteractionRequiredText(true))
            lblDec.visibility = View.GONE
            tvDec.visibility = View.GONE
        }


        tdr?.run {

            val encH = CryptoHandlerFacade.getInstance(getActivity()).getCryptoHandler(tdr)
            if (encH != null) {
                tvMeth.setText(encH.displayEncryptionMethod)
                tvMeth.visibility = View.VISIBLE
                lblMeth.visibility = View.VISIBLE
            }

            if (tdr.isOk) {
                lblDec.visibility = View.VISIBLE
                tvDec.visibility = View.VISIBLE
                try {
                    val innerData = tdr.decryptedDataAsInnerData
                    if (innerData.hasTextAndPaddingV0()) {
                        val textAndPadding = tdr.decryptedDataAsInnerData.textAndPaddingV0

                        tvDec.text = textAndPadding.text
                        tvInnerPadding.text = getActivity().getString(
                            R.string.bytes_size,
                            ""+textAndPadding.padding.size()
                        )

                        lblInnerPadding.visibility = View.VISIBLE
                        tvInnerPadding.visibility = View.VISIBLE
                    } else {
                        tvDec.text = getString(
                            R.string.error_cannot_show_inner_data_of_type,
                            tdr.decryptedDataAsInnerData.dataCase.name
                        )
                    }
                } catch (e: InvalidProtocolBufferException) {
                    try {
                        val innerText = tdr.decryptedDataAsUtf8String
                        tvDec.text = innerText
                        lblInnerPadding.visibility = View.GONE
                        tvInnerPadding.visibility = View.GONE
                    } catch (e1: UnsupportedEncodingException) {
                        tvDec.text = getString(R.string.error_cannot_show_inner_data)
                    }

                }



                lblErr.visibility = View.GONE
                tvErr.visibility = View.GONE

            } else {
                var m = getActivity().getString(Stuff.getErrorText(tdr.error!!))
                tdr.errorMessage?.let {  m += "\n" + it }

                tvErr.text = m
                lblDec.visibility = View.GONE
                tvDec.visibility = View.GONE
            }


        }

        val btAction = mView.findViewById<View>(R.id.btnPerformUserInteraction) as Button
        if (uix == null) {
            btAction.visibility = View.GONE
        } else {
            btAction.visibility = View.VISIBLE
            btAction.setText(R.string.action_perform_passwordinput)
            btAction.setOnClickListener {
                try {
                    val atdr = CryptoHandlerFacade.getInstance(getActivity())
                        .decryptWithLock(mOrigText, null)
                    //should never go through, but well, just in case
                    setData(activity, mOrigText, atdr, null, encryptionHandler)
                } catch (e: UserInteractionRequiredException) {
                    try {
                        activity.startIntentSenderForResult(
                            e.pendingIntent.intentSender,
                            EncryptionInfoActivity.REQUEST_CODE_DECRYPT,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (e1: IntentSender.SendIntentException) {
                        e1.printStackTrace()
                        //TODO: what now?
                    }

                }
            }
        }
    }

    abstract fun onCreateOptionsMenu(activity: Activity, menu: Menu): Boolean

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_share_decrypted).isVisible = mTdr != null && mTdr!!.isOk
    }

    open fun onOptionsItemSelected(activity: Activity, item: MenuItem) {
        val id = item.itemId
        if (id == R.id.action_share_encrypted) {
            share(activity, mOrigText, activity.getString(R.string.action_share_encrypted))
        } else if (id == R.id.action_share_decrypted) {
            MaterialDialog.Builder(activity)
                .title(R.string.confirmation_share_decrypted__title)
                .content(R.string.confirmation_share_decrypted__content)
                .positiveText(R.string.action_share)
                .negativeText(R.string.action_cancel)
                .autoDismiss(true)
                .onPositive { dialog, which ->
                    try {
                        val innerData = mTdr!!.decryptedDataAsInnerData
                        if (innerData.hasTextAndPaddingV0()) {
                            val textAndPadding = mTdr!!.decryptedDataAsInnerData.textAndPaddingV0

                            share(
                                activity,
                                textAndPadding.text,
                                activity.getString(R.string.action_share_decrypted)
                            )
                        } else {
                            Ln.w(
                                "Can't share inner data of type %s",
                                mTdr!!.decryptedDataAsInnerData.dataCase.name
                            )
                        }
                    } catch (e: InvalidProtocolBufferException) {
                        try {
                            val innerText = mTdr!!.decryptedDataAsUtf8String
                            share(
                                activity,
                                innerText,
                                activity.getString(R.string.action_share_decrypted)
                            )
                        } catch (e1: UnsupportedEncodingException) {
                            Ln.w("Can't share inner data!")
                        }

                    }
                }
                .show()
        }
    }

    protected fun share(activity: Activity, data: String, title: String) {
        val intent2 = Intent()
        intent2.action = Intent.ACTION_SEND
        intent2.type = "text/plain"
        intent2.putExtra(Intent.EXTRA_TEXT, data)
        activity.startActivity(Intent.createChooser(intent2, title))
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
