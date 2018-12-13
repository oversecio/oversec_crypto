package io.oversec.one.crypto.sym.ui

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.R
import io.oversec.one.crypto.images.xcoder.ImageXCoder
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.OversecKeystore2
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.symbase.SymmetricDecryptResult
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment

class SymmetricBinaryEncryptionInfoFragment : AbstractBinaryEncryptionInfoFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.encryption_info_binary_sym, container, false)
        super.onCreateView(inflater, container, savedInstanceState)
        return mView
    }


    override fun handleSetData(msg: Outer.Msg, tdr: BaseDecryptResult?, coder: ImageXCoder?) {
        super.handleSetData(msg, tdr, coder)

        val lblSymKeyAlias = mView.findViewById<View>(R.id.lbl_sym_key_name) as TextView
        val tvSym = mView.findViewById<View>(R.id.tv_sym_key_name) as TextView
        val lblConfirm = mView.findViewById<View>(R.id.lbl_key_confirm) as TextView
        val tvConfirm = mView.findViewById<View>(R.id.tv_key_confirm) as TextView
        val tvAvatar = mView.findViewById<View>(R.id.tvAvatar) as TextView
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
                btKeyDetails.setOnClickListener { KeyDetailsActivity.show(activity, keyId) }
            }


            if (keyId == null) {
                lblSymKeyAlias.visibility = View.GONE
                tvSym.visibility = View.GONE
                tvAvatar.visibility = View.GONE
                lblConfirm.visibility = View.GONE
                tvConfirm.visibility = View.GONE
            } else {
                val keystore = OversecKeystore2.getInstance(activity)
                val name = keystore.getSymmetricKeyEncrypted(keyId)?.name
                tvSym.text = name
                SymUtil.applyAvatar(tvAvatar, name!!)


                val confirmedDate = keystore.getConfirmDate(keyId)
                tvConfirm.text =
                        if (confirmedDate == null) activity.getString(R.string.label_key_unconfirmed) else DateUtils.formatDateTime(
                            activity,
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
                    tvConfirm.setTextColor(ContextCompat.getColor(activity, R.color.colorWarning))
                }
            }
        }



    }

    companion object {

        fun newInstance(packagename: String): SymmetricBinaryEncryptionInfoFragment {
            val fragment = SymmetricBinaryEncryptionInfoFragment()
            fragment.setArgs(packagename)
            return fragment
        }
    }
}
