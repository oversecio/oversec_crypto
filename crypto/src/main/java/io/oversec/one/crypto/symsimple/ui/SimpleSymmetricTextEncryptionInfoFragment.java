package io.oversec.one.crypto.symsimple.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.sym.KeyNotCachedException;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.symbase.KeyCache;
import io.oversec.one.crypto.symbase.SymmetricDecryptResult;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;
import io.oversec.one.crypto.ui.EncryptionInfoActivity;

public class SimpleSymmetricTextEncryptionInfoFragment extends AbstractTextEncryptionInfoFragment {

    public static SimpleSymmetricTextEncryptionInfoFragment newInstance(String packagename) {
        SimpleSymmetricTextEncryptionInfoFragment fragment = new SimpleSymmetricTextEncryptionInfoFragment();
        fragment.setArgs(packagename);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.encryption_info_text_simplesym, container, false);
        super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }


    @Override
    public void setData(EncryptionInfoActivity activity, String encodedText, BaseDecryptResult tdr, UserInteractionRequiredException uix, final AbstractCryptoHandler encryptionHandler) {
        super.setData(activity, encodedText, tdr, uix, encryptionHandler);

        SymmetricDecryptResult r = (SymmetricDecryptResult) tdr;
        TextView lblSymKeyAlias = (TextView) mView.findViewById(R.id.lbl_sym_key_name);
        TextView tvAvatar = (TextView) mView.findViewById(R.id.tvAvatar);
        TextView tvSym = (TextView) mView.findViewById(R.id.tv_sym_key_name);

        if (tdr == null) {

            lblSymKeyAlias.setVisibility(View.GONE);
            tvSym.setVisibility(View.GONE);
            tvAvatar.setVisibility(View.GONE);

        } else {
            final Long keyId = r.getSymmetricKeyId();

            if (keyId == null) {
                lblSymKeyAlias.setVisibility(View.GONE);
                tvSym.setVisibility(View.GONE);
                tvAvatar.setVisibility(View.GONE);

            } else {
                KeyCache kc = KeyCache.getInstance(getActivity());
                String name = "";
                try {
                    name = kc.get(r.getSymmetricKeyId()).getName();
                } catch (KeyNotCachedException e) {
                    e.printStackTrace();
                }
                tvSym.setText(name);
                SymUtil.applyAvatar(tvAvatar, r.getSymmetricKeyId(), name);


            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Activity activity, Menu menu) {
        activity.getMenuInflater().inflate(R.menu.sym_menu_encryption_info, menu);
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_share_sym_base64).setVisible(mTdr != null);

    }

    @Override
    public void onOptionsItemSelected(Activity activity, MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share_sym_base64) {
            share(activity, SimpleSymmetricCryptoHandler.getRawMessageJson(CryptoHandlerFacade.getEncodedData(activity, mOrigText)), activity.getString(R.string.action_share_sym_base64));
        } else {
            super.onOptionsItemSelected(activity, item);
        }
    }
}
