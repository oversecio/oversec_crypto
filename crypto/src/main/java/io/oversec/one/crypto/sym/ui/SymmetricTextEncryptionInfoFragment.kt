package io.oversec.one.crypto.sym.ui

import android.app.Activity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.view.*
import android.widget.Button
import android.widget.TextView
import io.oversec.one.crypto.*
import io.oversec.one.crypto.sym.OversecKeystore2
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler
import io.oversec.one.crypto.symbase.SymmetricDecryptResult
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment
import io.oversec.one.crypto.ui.EncryptionInfoActivity

class SymmetricTextEncryptionInfoFragment : AbstractTextEncryptionInfoFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.encryption_info_text_sym, container, false)
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



        val lblSymKeyAlias = mView.findViewById<View>(R.id.lbl_sym_key_name) as TextView
        val tvAvatar = mView.findViewById<View>(R.id.tvAvatar) as TextView
        val tvSym = mView.findViewById<View>(R.id.tv_sym_key_name) as TextView
        val lblConfirm = mView.findViewById<View>(R.id.lbl_key_confirm) as TextView
        val tvConfirm = mView.findViewById<View>(R.id.tv_key_confirm) as TextView
        val btKeyDetails = mView.findViewById<View>(R.id.btnKeyDetailsSym) as Button

        if (tdr == null) {
            lblSymKeyAlias.visibility = View.GONE
            tvSym.visibility = View.GONE
            tvAvatar.visibility = View.GONE

        } else {
            val r = tdr as SymmetricDecryptResult
            val keyId = r.symmetricKeyId

            if (keyId == null) {
                btKeyDetails.visibility = View.GONE
            } else {
                btKeyDetails.setOnClickListener { KeyDetailsActivity.show(getActivity(), keyId) }
            }

            if (keyId == null) {
                lblSymKeyAlias.visibility = View.GONE
                tvSym.visibility = View.GONE
                tvAvatar.visibility = View.GONE
                lblConfirm.visibility = View.GONE
                tvConfirm.visibility = View.GONE
            } else {
                val keystore = OversecKeystore2.getInstance(getActivity())
                val name = keystore.getSymmetricKeyEncrypted(keyId)?.name
                tvSym.text = name

                SymUtil.applyAvatar(tvAvatar, name!!)

                val confirmedDate = keystore.getConfirmDate(keyId)
                tvConfirm.text =
                        if (confirmedDate == null) getActivity().getString(R.string.label_key_unconfirmed) else DateUtils.formatDateTime(
                            getActivity(),
                            confirmedDate.time,
                            0
                        )
                tvConfirm.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    if (confirmedDate == null) R.drawable.ic_warning_black_24dp else R.drawable.ic_done_black_24dp,
                    0
                )
                if (confirmedDate == null) {
                    tvConfirm.setTextColor(
                        ContextCompat.getColor(
                            getActivity(),
                            R.color.colorWarning
                        )
                    )
                }


            }

        }

    }

    override fun onCreateOptionsMenu(activity: Activity, menu: Menu): Boolean {
        activity.menuInflater.inflate(R.menu.sym_menu_encryption_info, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_share_sym_base64).isVisible = mTdr != null
    }

    override fun onOptionsItemSelected(activity: Activity, item: MenuItem) {
        val id = item.itemId
        if (id == R.id.action_share_sym_base64) {
            share(
                activity,
                BaseSymmetricCryptoHandler.getRawMessageJson(
                    CryptoHandlerFacade.getEncodedData(
                        activity,
                        mOrigText
                    )!!
                )!!,
                activity.getString(R.string.action_share_sym_base64)
            )
        } else {
            super.onOptionsItemSelected(activity, item)
        }
    }

    companion object {

        fun newInstance(packagename: String): SymmetricTextEncryptionInfoFragment {
            val fragment = SymmetricTextEncryptionInfoFragment()
            fragment.setArgs(packagename)
            return fragment
        }
    }
}
