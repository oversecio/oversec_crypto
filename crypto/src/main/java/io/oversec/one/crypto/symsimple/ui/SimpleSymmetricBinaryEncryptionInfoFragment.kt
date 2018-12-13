package io.oversec.one.crypto.symsimple.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.R
import io.oversec.one.crypto.images.xcoder.ImageXCoder
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.KeyNotCachedException
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.symbase.KeyCache
import io.oversec.one.crypto.symbase.SymmetricDecryptResult
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment

class SimpleSymmetricBinaryEncryptionInfoFragment : AbstractBinaryEncryptionInfoFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.encryption_info_binary_simplesym, container, false)
        super.onCreateView(inflater, container, savedInstanceState)
        return mView
    }


    override fun handleSetData(msg: Outer.Msg, tdr: BaseDecryptResult?, coder: ImageXCoder?) {
        super.handleSetData(msg, tdr, coder)
        val r = tdr as SymmetricDecryptResult?
        val lblSymKeyAlias = mView.findViewById<View>(R.id.lbl_sym_key_name) as TextView
        val tvAvatar = mView.findViewById<View>(R.id.tvAvatar) as TextView
        val tvSym = mView.findViewById<View>(R.id.tv_sym_key_name) as TextView

        if (tdr == null) {

            lblSymKeyAlias.visibility = View.GONE
            tvSym.visibility = View.GONE
            tvAvatar.visibility = View.GONE

        } else {
            val keyId = r!!.symmetricKeyId

            if (keyId == null) {
                lblSymKeyAlias.visibility = View.GONE
                tvSym.visibility = View.GONE
                tvAvatar.visibility = View.GONE

            } else {
                val kc = KeyCache.getInstance(activity)
                var name = ""
                try {
                    name = kc.get(r.symmetricKeyId).name!!
                } catch (e: KeyNotCachedException) {
                    e.printStackTrace()
                }

                tvSym.text = name
                SymUtil.applyAvatar(tvAvatar, r.symmetricKeyId!!, name)


            }

        }
    }

    companion object {

        fun newInstance(packagename: String): SimpleSymmetricBinaryEncryptionInfoFragment {
            val fragment = SimpleSymmetricBinaryEncryptionInfoFragment()
            fragment.setArgs(packagename)
            return fragment
        }
    }


}
