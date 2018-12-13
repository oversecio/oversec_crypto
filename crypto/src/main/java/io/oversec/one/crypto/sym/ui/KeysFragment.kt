package io.oversec.one.crypto.sym.ui

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.oversec.one.crypto.Help
import io.oversec.one.crypto.R
import io.oversec.one.crypto.sym.OversecKeystore2
import io.oversec.one.crypto.ui.WithHelp
import io.oversec.one.crypto.ui.util.SimpleDividerItemDecoration

class KeysFragment : Fragment(), WithHelp, OversecKeystore2.KeyStoreListener {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mKeystore: OversecKeystore2

    override val helpAnchor: Help.ANCHOR
        get() = Help.ANCHOR.main_keys

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val ctx = container!!.context
        mKeystore = OversecKeystore2.getInstance(ctx)

        val res = inflater.inflate(R.layout.sym_fragment_keys, container, false)
        mRecyclerView = res.findViewById<View>(R.id.list) as RecyclerView

        mRecyclerView.layoutManager = LinearLayoutManager(ctx)
        mRecyclerView.addItemDecoration(SimpleDividerItemDecoration(ctx))
        // checkIntent(getIntent());

        val fab = res.findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            KeyImportCreateActivity.showAddKeyDialog(
                this@KeysFragment,
                RQ_CREATE_NEW_KEY
            )
        }

        refreshList()

        mKeystore.addListener(this)
        return res
    }

    override fun onDestroyView() {
        mKeystore.removeListener(this)
        super.onDestroyView()
    }

    private fun refreshList() {
        mRecyclerView.adapter =
                SymmetricKeyRecyclerViewAdapter(this@KeysFragment, mKeystore.encryptedKeys_sorted)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RQ_CREATE_NEW_KEY && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val keyId = data.getLongExtra(EXTRA_KEY_ID, 0)
                if (keyId != 0L) {
                    KeyDetailsActivity.showForResult(this@KeysFragment, RQ_CREATE_NEW_KEY, keyId)
                }
            }
        }
        refreshList()
    }

    override fun onKeyStoreChanged() {
        mRecyclerView.post { refreshList() }

    }

    companion object {

        private const val RQ_CREATE_NEW_KEY = 7007
        const val EXTRA_KEY_ID = "EXTRA_KEY_ID"

        fun newInstance(): KeysFragment {
            return KeysFragment()
        }
    }
}
