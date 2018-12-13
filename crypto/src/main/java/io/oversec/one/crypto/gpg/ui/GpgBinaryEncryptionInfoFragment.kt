package io.oversec.one.crypto.gpg.ui

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.oversec.one.crypto.*
import io.oversec.one.crypto.gpg.GpgCryptoHandler
import io.oversec.one.crypto.gpg.GpgDecryptResult
import io.oversec.one.crypto.gpg.OpenKeychainConnector
import io.oversec.one.crypto.images.xcoder.ImageXCoder
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment
import io.oversec.one.crypto.ui.EncryptionInfoActivity
import org.openintents.openpgp.OpenPgpSignatureResult

class GpgBinaryEncryptionInfoFragment : AbstractBinaryEncryptionInfoFragment() {


    private lateinit var mTvPgpRecipients: TextView
    private lateinit var mTvPgpSignatureResult: TextView
    private lateinit var mTvPgpSignatureKey: TextView
    private lateinit var mLblPgpRecipients: TextView
    private lateinit var mLblPgpSignatureResult: TextView
    private lateinit var mLblPgpSignatureKey: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.encryption_info_binary_gpg, container, false)

        mLblPgpRecipients = mView.findViewById<View>(R.id.lbl_pgp_recipients) as TextView
        mTvPgpRecipients = mView.findViewById<View>(R.id.tv_pgp_recipients) as TextView

        mLblPgpSignatureResult = mView.findViewById<View>(R.id.lbl_pgp_signature_result) as TextView
        mTvPgpSignatureResult = mView.findViewById<View>(R.id.tv_pgp_signature_result) as TextView
        mLblPgpSignatureKey = mView.findViewById<View>(R.id.lbl_pgp_signature_key) as TextView
        mTvPgpSignatureKey = mView.findViewById<View>(R.id.tv_pgp_signature_key) as TextView

        mLblPgpRecipients.visibility = View.GONE
        mTvPgpRecipients.visibility = View.GONE
        mLblPgpSignatureResult.visibility = View.GONE
        mTvPgpSignatureResult.visibility = View.GONE
        mLblPgpSignatureKey.visibility = View.GONE
        mTvPgpSignatureKey.visibility = View.GONE

        super.onCreateView(inflater, container, savedInstanceState)

        return mView
    }

    override fun handleSetData(msg: Outer.Msg, tdr: BaseDecryptResult?, coder: ImageXCoder?) {
        super.handleSetData(msg, tdr, coder)

        val r = tdr as GpgDecryptResult
        val cryptoHandler =
            CryptoHandlerFacade.getInstance(activity).getCryptoHandler(EncryptionMethod.GPG) as GpgCryptoHandler


        if (msg.hasMsgTextGpgV0()) {
            val pkids = msg.msgTextGpgV0.pubKeyIdV0List
            setPublicKeyIds(
                mLblPgpRecipients,
                mTvPgpRecipients,
                pkids,
                cryptoHandler,
                msg,
                tdr,
                coder
            )
        }
        if (r == null) {
            //
            //            mLblPgpRecipients.setVisibility(View.GONE);
            //            mTvPgpRecipients.setVisibility(View.GONE);

        } else {

            if (r.signatureResult != null) {
                mLblPgpSignatureResult.visibility = View.VISIBLE
                mTvPgpSignatureResult.visibility = View.VISIBLE

                val sr = r.signatureResult
                mTvPgpSignatureResult.text =
                        GpgCryptoHandler.signatureResultToUiText(activity, sr!!)
                mTvPgpSignatureResult.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    GpgCryptoHandler.signatureResultToUiIconRes(sr, false),
                    0
                )
                val color = ContextCompat.getColor(
                    activity,
                    GpgCryptoHandler.signatureResultToUiColorResId(sr)
                )
                mTvPgpSignatureResult.setTextColor(color)
                //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //                    tvPgpSignatureResult.setCompoundDrawableTintList(GpgCryptoHandler.getColorStateListAllStates(color));
                //                }
                when (sr.result) {
                    OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE, OpenPgpSignatureResult.RESULT_KEY_MISSING, OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> {
                        mLblPgpSignatureKey.visibility = View.GONE
                        mTvPgpSignatureKey.visibility = View.GONE
                    }
                    OpenPgpSignatureResult.RESULT_INVALID_INSECURE, OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED, OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED, OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED, OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> {
                        mLblPgpSignatureKey.visibility = View.VISIBLE
                        mTvPgpSignatureKey.visibility = View.VISIBLE

                        val sb = sr.primaryUserId +
                                "\n" +
                                "[" + SymUtil.longToPrettyHex(sr.keyId) + "]"

                        mTvPgpSignatureKey.text = sb
                        mTvPgpSignatureKey.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            GpgCryptoHandler.signatureResultToUiIconRes_KeyOnly(sr, false),
                            0
                        )
                        val kColor = ContextCompat.getColor(
                            activity,
                            GpgCryptoHandler.signatureResultToUiColorResId_KeyOnly(sr)
                        )
                        mTvPgpSignatureKey.setTextColor(kColor)
                        //                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        //                            tvPgpSignatureKey.setCompoundDrawableTintList(GpgCryptoHandler.getColorStateListAllStates(kColor));
                        //                        }
                    }
                }
            }


        }

        val btKeyDetails = mView.findViewById<View>(R.id.btnKeyDetailsGpg) as Button
        btKeyDetails.visibility = View.GONE
        val btKeyAction = mView.findViewById<View>(R.id.btnKeyActionGpg) as Button
        btKeyAction.visibility = View.GONE

        if (r != null) {

            if (r.downloadMissingSignatureKeyPendingIntent != null) {
                btKeyAction.visibility = View.VISIBLE
                btKeyAction.setText(R.string.action_download_missing_signature_key)
                btKeyAction.setOnClickListener {
                    try {
                        activity.startIntentSenderForResult(
                            r.downloadMissingSignatureKeyPendingIntent!!.intentSender,
                            EncryptionInfoActivity.REQUEST_CODE_DOWNLOAD_MISSING_KEYS,
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
            } else if (r.showSignatureKeyPendingIntent != null) {
                btKeyAction.visibility = View.VISIBLE
                btKeyAction.setText(R.string.action_show_signature_key)
                btKeyAction.setOnClickListener {
                    try {
                        activity.startIntentSenderForResult(
                            r.showSignatureKeyPendingIntent!!.intentSender,
                            EncryptionInfoActivity.REQUEST_CODE_SHOW_SIGNATURE_KEY,
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

            btKeyDetails.setOnClickListener {
                //for now we can only jump to the list of keys [i.e. the main activity] in OKC, since we're dealing with subkeys here...
                GpgCryptoHandler.openOpenKeyChain(activity)
            }
        }
    }

    private fun setPublicKeyIds(
        lblPgpRecipients: TextView,
        tvPgpRecipients: TextView?,
        publicKeyIds: List<Long>?,
        encryptionHandler: AbstractCryptoHandler,
        msg: Outer.Msg,
        tdr: BaseDecryptResult,
        coder: ImageXCoder?
    ) {
        val pe = encryptionHandler as GpgCryptoHandler

        val okcVersion = OpenKeychainConnector.getInstance(lblPgpRecipients.context).getVersion()


        if (publicKeyIds != null) {
            lblPgpRecipients.visibility = View.VISIBLE
            tvPgpRecipients!!.visibility = View.VISIBLE
            val sb = SpannableStringBuilder()

            publicKeyIds.forEach { pkid ->
                if (sb.isNotEmpty()) {
                    sb.append("\n\n")
                }

                val userName = pe.getFirstUserIDByKeyId(pkid, null)
                if (userName != null) {
                    sb.append(userName).append("\n[").append(SymUtil.longToPrettyHex(pkid))
                        .append("]")
                } else {
                    //userName might be null if OKC version < OpenKeychainConnector.V_GET_SUBKEY
                    //however in older versions we still don't know if the user doesn't have the key
                    //so best for now just show the keyId only without any additional remarks
                    //that might just confuse users.

                    if (okcVersion >= OpenKeychainConnector.V_GET_SUBKEY) {
                        val start = sb.length
                        sb.append(lblPgpRecipients.context.getString(R.string.action_download_missing_public_key))
                        val end = sb.length
                        val clickToDownloadKey = object : ClickableSpan() {
                            override fun onClick(view: View) {
                                downloadKey(pkid, null)
                            }
                        }
                        sb.setSpan(
                            clickToDownloadKey,
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sb.append("\n")
                        sb.append("[").append(SymUtil.longToPrettyHex(pkid)).append("]")
                    } else {
                        sb.append("[").append(SymUtil.longToPrettyHex(pkid)).append("]")
                    }
                }


            }
            tvPgpRecipients.text = sb
            tvPgpRecipients.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    @Synchronized
    private fun downloadKey(keyId: Long, actionIntent: Intent?) {

        val cryptoHandler =
            CryptoHandlerFacade.getInstance(activity).getCryptoHandler(EncryptionMethod.GPG) as GpgCryptoHandler
        val pi = cryptoHandler.getDownloadKeyPendingIntent(keyId, actionIntent)

        if (pi != null) {
            try {
                activity.startIntentSenderForResult(
                    pi.intentSender,
                    EncryptionInfoActivity.REQUEST_CODE_DOWNLOAD_MISSING_KEYS,
                    null,
                    0,
                    0,
                    0
                )
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
            }
        }
    }

    companion object {

        fun newInstance(packagename: String): GpgBinaryEncryptionInfoFragment {
            val fragment = GpgBinaryEncryptionInfoFragment()
            fragment.setArgs(packagename)
            return fragment
        }
    }
}
