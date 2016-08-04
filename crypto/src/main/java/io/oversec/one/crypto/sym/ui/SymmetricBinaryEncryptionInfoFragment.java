package io.oversec.one.crypto.sym.ui;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.images.xcoder.ImageXCoder;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.symbase.SymmetricDecryptResult;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;

import java.util.Date;

public class SymmetricBinaryEncryptionInfoFragment extends AbstractBinaryEncryptionInfoFragment {

    public static SymmetricBinaryEncryptionInfoFragment newInstance(String packagename) {
        SymmetricBinaryEncryptionInfoFragment fragment = new SymmetricBinaryEncryptionInfoFragment();
        fragment.setArgs(packagename);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.encryption_info_binary_sym, container, false);
        super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }


    @Override
    protected void handleSetData(Outer.Msg msg, BaseDecryptResult tdr, ImageXCoder coder) {
        super.handleSetData(msg, tdr, coder);
        SymmetricDecryptResult r = (SymmetricDecryptResult) tdr;

        TextView lblSymKeyAlias = (TextView) mView.findViewById(R.id.lbl_sym_key_name);
        TextView tvSym = (TextView) mView.findViewById(R.id.tv_sym_key_name);
        TextView lblConfirm = (TextView) mView.findViewById(R.id.lbl_key_confirm);
        TextView tvConfirm = (TextView) mView.findViewById(R.id.tv_key_confirm);
        TextView tvAvatar = (TextView) mView.findViewById(R.id.tvAvatar);

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
                lblConfirm.setVisibility(View.GONE);
                tvConfirm.setVisibility(View.GONE);
            } else {
                OversecKeystore2 keystore = OversecKeystore2.getInstance(getActivity());
                String name = keystore.getSymmetricKeyEncrypted(keyId).getName();
                tvSym.setText(name);
                SymUtil.applyAvatar(tvAvatar, name);


                Date confirmedDate = keystore.getConfirmDate(keyId);
                tvConfirm.setText(confirmedDate == null ? getActivity().getString(R.string.label_key_unconfirmed) : DateUtils.formatDateTime(getActivity(), confirmedDate.getTime(), 0));
                tvConfirm.setCompoundDrawablesWithIntrinsicBounds(0, 0, confirmedDate == null ? R.drawable.ic_warning_black_24dp : R.drawable.ic_done_black_24dp, 0);
                if (confirmedDate == null) {
                    tvConfirm.setTextColor(ContextCompat.getColor(getActivity(),R.color.colorWarning));
                }


            }

        }


        Button btKeyDetails = (Button) mView.findViewById(R.id.btnKeyDetailsSym);

        if (r != null) {
            final Long keyId = r.getSymmetricKeyId();
            if (keyId == null) {
                btKeyDetails.setVisibility(View.GONE);
            } else {
                btKeyDetails.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        KeyDetailsActivity.show(getActivity(), keyId);
                    }
                });
            }


        }
    }


}
