package io.oversec.one.crypto.sym.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.oversec.one.crypto.Help;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.ui.WithHelp;
import io.oversec.one.crypto.ui.util.SimpleDividerItemDecoration;

public class KeysFragment extends Fragment implements WithHelp, OversecKeystore2.KeyStoreListener {

    private static final int RQ_CREATE_NEW_KEY = 7007;
    public static final String EXTRA_KEY_ID = "EXTRA_KEY_ID";
    private RecyclerView mRecyclerView;
    private OversecKeystore2 mKeystore;

    public KeysFragment() {
        // Required empty public constructor
    }

    public static KeysFragment newInstance() {
        KeysFragment fragment = new KeysFragment();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        Context ctx = container.getContext();
        mKeystore = OversecKeystore2.getInstance(ctx);

        View res = inflater.inflate(R.layout.sym_fragment_keys, container, false);
        mRecyclerView = (RecyclerView) res.findViewById(R.id.list);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(ctx));
        // checkIntent(getIntent());

        FloatingActionButton fab = (FloatingActionButton) res.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyImportCreateActivity.showAddKeyDialog(KeysFragment.this, RQ_CREATE_NEW_KEY);
            }
        });

        refreshList();

        mKeystore.addListener(this);
        return res;
    }

    @Override
    public void onDestroyView() {
        mKeystore.removeListener(this);
        super.onDestroyView();
    }

    void refreshList() {
        mRecyclerView.setAdapter(new SymmetricKeyRecyclerViewAdapter(KeysFragment.this, mKeystore.getEncryptedKeys_sorted()));

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RQ_CREATE_NEW_KEY && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                long keyId = data.getLongExtra(EXTRA_KEY_ID, 0);
                if (keyId != 0) {
                    KeyDetailsActivity.showForResult(KeysFragment.this, RQ_CREATE_NEW_KEY, keyId);
                }
            }
        }
        refreshList();

    }

    @Override
    public Help.ANCHOR getHelpAnchor() {
        return Help.ANCHOR.main_keys;
    }

    @Override
    public void onKeyStoreChanged() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                refreshList();
            }
        });

    }
}
