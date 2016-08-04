package io.oversec.one.crypto.symsimple.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.images.xcoder.ImageXCoder;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.KeyNotCachedException;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.symbase.KeyCache;
import io.oversec.one.crypto.symbase.SymmetricDecryptResult;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;

public class SimpleSymmetricBinaryEncryptionInfoFragment extends AbstractBinaryEncryptionInfoFragment {

    public static SimpleSymmetricBinaryEncryptionInfoFragment newInstance(String packagename) {
        SimpleSymmetricBinaryEncryptionInfoFragment fragment = new SimpleSymmetricBinaryEncryptionInfoFragment();
        fragment.setArgs(packagename);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.encryption_info_binary_simplesym, container, false);
        super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }


    @Override
    protected void handleSetData(Outer.Msg msg, BaseDecryptResult tdr, ImageXCoder coder) {
        super.handleSetData(msg, tdr, coder);
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


}
