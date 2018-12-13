package io.oversec.one.crypto.gpg.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.widget.Button
import android.widget.TextView
import io.oversec.one.crypto.*
import io.oversec.one.crypto.gpg.GpgCryptoHandler
import io.oversec.one.crypto.gpg.GpgDecryptResult
import io.oversec.one.crypto.gpg.OpenKeychainConnector
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment
import io.oversec.one.crypto.ui.EncryptionInfoActivity
import org.openintents.openpgp.OpenPgpSignatureResult

class GpgTextEncryptionInfoFragment : AbstractTextEncryptionInfoFragment() {

    private lateinit var mCryptoHandler: GpgCryptoHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.encryption_info_text_gpg, container, false)
        super.onCreateView(inflater, container, savedInstanceState)
        return mView
    }

    override fun setData(
        activity: EncryptionInfoActivity,
        encodedText: String,
        tdr: BaseDecryptResult?,
        uix: UserInteractionRequiredException?,
        encryptionHandler: AbstractCryptoHandler
    ) {
        super.setData(activity, encodedText, tdr, uix, encryptionHandler)

        mCryptoHandler = encryptionHandler as GpgCryptoHandler

        val r = tdr as GpgDecryptResult?

        val lblPgpRecipients = mView.findViewById<View>(R.id.lbl_pgp_recipients) as TextView
        val tvPgpRecipients = mView.findViewById<View>(R.id.tv_pgp_recipients) as TextView

        val lblPgpSignatureResult =
            mView.findViewById<View>(R.id.lbl_pgp_signature_result) as TextView
        val tvPgpSignatureResult =
            mView.findViewById<View>(R.id.tv_pgp_signature_result) as TextView
        val lblPgpSignatureKey = mView.findViewById<View>(R.id.lbl_pgp_signature_key) as TextView
        val tvPgpSignatureKey = mView.findViewById<View>(R.id.tv_pgp_signature_key) as TextView


        lblPgpRecipients.visibility = View.GONE
        tvPgpRecipients.visibility = View.GONE
        lblPgpSignatureResult.visibility = View.GONE
        tvPgpSignatureResult.visibility = View.GONE
        lblPgpSignatureKey.visibility = View.GONE
        tvPgpSignatureKey.visibility = View.GONE

        val msg = CryptoHandlerFacade.getEncodedData(activity, encodedText)

        if (msg!!.hasMsgTextGpgV0()) {
            val pkids = msg.msgTextGpgV0.pubKeyIdV0List
            setPublicKeyIds(
                lblPgpRecipients,
                tvPgpRecipients,
                pkids,
                encryptionHandler,
                activity,
                encodedText,
                tdr,
                uix
            )
        }

        if (r == null) {
            // lblPgpRecipients.setVisibility(View.GONE);
            //  tvPgpRecipients.setVisibility(View.GONE);
        } else {
            if (r.signatureResult != null) {
                lblPgpSignatureResult.visibility = View.VISIBLE
                tvPgpSignatureResult.visibility = View.VISIBLE

                val sr = (tdr as GpgDecryptResult).signatureResult
                tvPgpSignatureResult.text =
                        GpgCryptoHandler.signatureResultToUiText(getActivity(), sr!!)
                tvPgpSignatureResult.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    GpgCryptoHandler.signatureResultToUiIconRes(sr, false),
                    0
                )
                val color = ContextCompat.getColor(
                    getActivity(),
                    GpgCryptoHandler.signatureResultToUiColorResId(sr)
                )
                tvPgpSignatureResult.setTextColor(color)
                //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //                    tvPgpSignatureResult.setCompoundDrawableTintList(GpgCryptoHandler.getColorStateListAllStates(color));
                //                }
                when (sr.result) {
                    OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE, OpenPgpSignatureResult.RESULT_KEY_MISSING, OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> {
                        lblPgpSignatureKey.visibility = View.GONE
                        tvPgpSignatureKey.visibility = View.GONE
                    }
                    OpenPgpSignatureResult.RESULT_INVALID_INSECURE, OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED, OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED, OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED, OpenPgpSignatureResult.RESULT_VALID_CONFIRMED -> {
                        lblPgpSignatureKey.visibility = View.VISIBLE
                        tvPgpSignatureKey.visibility = View.VISIBLE

                        val sb = sr.primaryUserId +
                                "\n" +
                                "[" + SymUtil.longToPrettyHex(sr.keyId) + "]"

                        tvPgpSignatureKey.text = sb
                        tvPgpSignatureKey.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            GpgCryptoHandler.signatureResultToUiIconRes_KeyOnly(sr, false),
                            0
                        )
                        val kColor = ContextCompat.getColor(
                            getActivity(),
                            GpgCryptoHandler.signatureResultToUiColorResId_KeyOnly(sr)
                        )
                        tvPgpSignatureKey.setTextColor(kColor)
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
                        val xtdr = CryptoHandlerFacade.getInstance(getActivity()).decryptWithLock(
                            mOrigText,
                            null
                        ) as GpgDecryptResult?
                        if (xtdr!!.isOk && xtdr.downloadMissingSignatureKeyPendingIntent != null) {
                            try {
                                activity.startIntentSenderForResult(
                                    xtdr.downloadMissingSignatureKeyPendingIntent!!.intentSender,
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
                    } catch (e: UserInteractionRequiredException) {
                        //should not happen here but well
                    }
                }
            } else if (r.showSignatureKeyPendingIntent != null) {
                btKeyAction.visibility = View.VISIBLE
                btKeyAction.setText(R.string.action_show_signature_key)
                btKeyAction.setOnClickListener {
                    try {
                        val xtdr = CryptoHandlerFacade.getInstance(getActivity()).decryptWithLock(
                            mOrigText,
                            null
                        ) as GpgDecryptResult?
                        if (xtdr!!.isOk && xtdr.showSignatureKeyPendingIntent != null) {
                            try {
                                activity.startIntentSenderForResult(
                                    xtdr.showSignatureKeyPendingIntent!!.intentSender,
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
                    } catch (e: UserInteractionRequiredException) {
                        //should not happen here but well

                        //might be an external device, so let's use existing decrypt result
                        if (mTdr != null) {
                            val pi = (mTdr as GpgDecryptResult).showSignatureKeyPendingIntent
                            if (pi != null) {
                                try {
                                    activity.startIntentSenderForResult(
                                        pi.intentSender,
                                        EncryptionInfoActivity.REQUEST_CODE_SHOW_SIGNATURE_KEY,
                                        null,
                                        0,
                                        0,
                                        0
                                    )
                                } catch (e1: IntentSender.SendIntentException) {
                                    e1.printStackTrace()
                                }

                            }
                        }
                    }
                }
            }


            btKeyDetails.setOnClickListener {
                //for now we can only jump to the list of keys [i.e. the main activity] in OKC, since we're dealing with subkeys here...
                GpgCryptoHandler.openOpenKeyChain(getActivity())
            }

        }
    }

    override fun onCreateOptionsMenu(activity: Activity, menu: Menu): Boolean {
        activity.menuInflater.inflate(R.menu.gpg_menu_encryption_info, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_share_gpg_ascii).isVisible = mTdr != null

    }

    override fun onOptionsItemSelected(activity: Activity, item: MenuItem) {
        val id = item.itemId
        if (id == R.id.action_share_gpg_ascii) {
            share(
                activity,
                GpgCryptoHandler.getRawMessageAsciiArmoured(
                    CryptoHandlerFacade.getEncodedData(
                        activity,
                        mOrigText
                    )!!
                )!!,
                activity.getString(R.string.action_share_gpg_ascii)
            )
        } else {
            super.onOptionsItemSelected(activity, item)
        }
    }

    private fun setPublicKeyIds(
        lblPgpRecipients: TextView,
        tvPgpRecipients: TextView,
        publicKeyIds: List<Long>?,
        encryptionHandler: AbstractCryptoHandler,
        activity: EncryptionInfoActivity,
        encodedText: String,
        tdr: BaseDecryptResult?,
        uix: UserInteractionRequiredException?
    ) {
        val pe = encryptionHandler as GpgCryptoHandler

        val okcVersion = OpenKeychainConnector.getInstance(lblPgpRecipients.context).getVersion()

        if (publicKeyIds != null) {
            lblPgpRecipients.visibility = View.VISIBLE
            tvPgpRecipients.visibility = View.VISIBLE
            val sb = SpannableStringBuilder()

            for (pkid in publicKeyIds) {
                if (sb.length > 0) {
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
        val pi = mCryptoHandler!!.getDownloadKeyPendingIntent(keyId, actionIntent)

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

        fun newInstance(packagename: String): GpgTextEncryptionInfoFragment {
            val fragment = GpgTextEncryptionInfoFragment()
            fragment.setArgs(packagename)
            return fragment
        }
    }
}
