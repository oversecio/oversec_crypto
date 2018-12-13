package io.oversec.one.crypto.symsimple.ui

import android.app.Activity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import io.oversec.one.crypto.*
import io.oversec.one.crypto.sym.KeyNotCachedException
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.symbase.BaseSymmetricCryptoHandler
import io.oversec.one.crypto.symbase.KeyCache
import io.oversec.one.crypto.symbase.SymmetricDecryptResult
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment
import io.oversec.one.crypto.ui.EncryptionInfoActivity

class SimpleSymmetricTextEncryptionInfoFragment : AbstractTextEncryptionInfoFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.encryption_info_text_simplesym, container, false)
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
                val kc = KeyCache.getInstance(getActivity())
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

        fun newInstance(packagename: String): SimpleSymmetricTextEncryptionInfoFragment {
            val fragment = SimpleSymmetricTextEncryptionInfoFragment()
            fragment.setArgs(packagename)
            return fragment
        }
    }
}
