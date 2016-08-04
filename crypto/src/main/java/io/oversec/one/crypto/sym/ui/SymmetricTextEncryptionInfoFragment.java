package io.oversec.one.crypto.sym.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.sym.SymmetricCryptoHandler;
import io.oversec.one.crypto.symbase.SymmetricDecryptResult;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;
import io.oversec.one.crypto.ui.EncryptionInfoActivity;

import java.util.Date;

public class SymmetricTextEncryptionInfoFragment extends AbstractTextEncryptionInfoFragment {

    public static SymmetricTextEncryptionInfoFragment newInstance(String packagename) {
        SymmetricTextEncryptionInfoFragment fragment = new SymmetricTextEncryptionInfoFragment();
        fragment.setArgs(packagename);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.encryption_info_text_sym, container, false);
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
        TextView lblConfirm = (TextView) mView.findViewById(R.id.lbl_key_confirm);
        TextView tvConfirm = (TextView) mView.findViewById(R.id.tv_key_confirm);

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
            share(activity, SymmetricCryptoHandler.getRawMessageJson(CryptoHandlerFacade.getEncodedData(activity, mOrigText)), activity.getString(R.string.action_share_sym_base64));
        } else {
            super.onOptionsItemSelected(activity, item);
        }
    }
}
