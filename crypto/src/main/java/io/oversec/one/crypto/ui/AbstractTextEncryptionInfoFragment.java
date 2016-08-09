package io.oversec.one.crypto.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.borjabravo.readmoretextview.ReadMoreTextView;
import com.google.protobuf.InvalidProtocolBufferException;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.encoding.XCoderFactory;
import io.oversec.one.crypto.proto.Inner;
import roboguice.util.Ln;

import java.io.UnsupportedEncodingException;

public abstract class AbstractTextEncryptionInfoFragment extends Fragment {


    private static final java.lang.String EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME";
    protected String mPackageName;
    protected ViewGroup mGrid;
    protected BaseDecryptResult mTdr;
    protected String mOrigText;
    protected View mView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mGrid = (ViewGroup) mView.findViewById(R.id.grid);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mPackageName = bundle.getString(EXTRA_PACKAGENAME);
        }

        return mView;
    }

    public void setData(final EncryptionInfoActivity activity, final String origText, BaseDecryptResult tdr, UserInteractionRequiredException uix, final AbstractCryptoHandler encryptionHandler) {
        mTdr = tdr;
        mOrigText = origText;


        TextView lblErr = (TextView) mView.findViewById(R.id.lbl_err);
        TextView tvErr = (TextView) mView.findViewById(R.id.tv_err);
        //TextView lblEnc = (TextView) mView.findViewById(R.id.lbl_enc);
        ReadMoreTextView tvEnc = (ReadMoreTextView) mView.findViewById(R.id.tv_enc);
        TextView tvSize = (TextView) mView.findViewById(R.id.tv_size);
        TextView lblDec = (TextView) mView.findViewById(R.id.lbl_dec);
        TextView tvDec = (TextView) mView.findViewById(R.id.tv_dec);
       //TextView lblCoder = (TextView) mView.findViewById(R.id.lbl_coder);
        TextView tvCoder = (TextView) mView.findViewById(R.id.tv_coder);
        TextView lblMeth = (TextView) mView.findViewById(R.id.lbl_meth);
        TextView tvMeth = (TextView) mView.findViewById(R.id.tv_meth);
        TextView lblInnerPadding = (TextView) mView.findViewById(R.id.lbl_innerpadding);
        TextView tvInnerPadding = (TextView) mView.findViewById(R.id.tv_innerpadding);

        tvMeth.setVisibility(View.GONE);
        lblMeth.setVisibility(View.GONE);

        lblInnerPadding.setVisibility(View.GONE);
        tvInnerPadding.setVisibility(View.GONE);


        int TRIM_LENGTH = 160;
        tvEnc.setTrimLength(TRIM_LENGTH);
        tvEnc.setTrimCollapsedText(getString(R.string.action_show_all_content, origText.length() - TRIM_LENGTH));
        tvEnc.setText(origText);

        try {
            tvSize.setText(getActivity().getString(R.string.bytes_size, origText.getBytes("UTF-8").length));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        tvCoder.setText(XCoderFactory.getInstance(activity).getEncodingInfo(origText));

        if (uix != null) {

            tvErr.setText(Stuff.getUserInteractionRequiredText(true));

            lblDec.setVisibility(View.GONE);
            tvDec.setVisibility(View.GONE);


        }

        if (tdr != null) {

            AbstractCryptoHandler encH = CryptoHandlerFacade.getInstance(getActivity()).getCryptoHandler(tdr);
            if (encH != null) {
                tvMeth.setText(encH.getDisplayEncryptionMethod());
                tvMeth.setVisibility(View.VISIBLE);
                lblMeth.setVisibility(View.VISIBLE);
            }

            if (tdr.isOk()) {

                try {
                    Inner.InnerData innerData = tdr.getDecryptedDataAsInnerData();
                    if (innerData.hasTextAndPaddingV0()) {
                        Inner.TextAndPaddingV0 textAndPadding = tdr.getDecryptedDataAsInnerData().getTextAndPaddingV0();

                        tvDec.setText(textAndPadding.getText());
                        tvInnerPadding.setText(getActivity().getString(R.string.bytes_size, textAndPadding.getPadding().size()));

                        lblInnerPadding.setVisibility(View.VISIBLE);
                        tvInnerPadding.setVisibility(View.VISIBLE);
                    }
                    else {
                        tvDec.setText(getString(R.string.error_cannot_show_inner_data_of_type, tdr.getDecryptedDataAsInnerData().getDataCase().name()));
                    }
                } catch (InvalidProtocolBufferException e) {
                    try {
                        String innerText = tdr.getDecryptedDataAsUtf8String();
                        lblInnerPadding.setVisibility(View.GONE);
                        tvInnerPadding.setVisibility(View.GONE);
                    } catch (UnsupportedEncodingException e1) {
                        tvDec.setText(getString(R.string.error_cannot_show_inner_data));
                    }
                }



                lblErr.setVisibility(View.GONE);
                tvErr.setVisibility(View.GONE);

            } else {
                String m = getActivity().getString(Stuff.getErrorText(tdr.getError()));
                if (tdr.getErrorMessage() != null) {
                    m += "\n" + tdr.getErrorMessage();
                }


                tvErr.setText(m);
                lblDec.setVisibility(View.GONE);
                tvDec.setVisibility(View.GONE);
            }


        }

        Button btAction = (Button) mView.findViewById(R.id.btnPerformUserInteraction);
        if (uix == null) {
            btAction.setVisibility(View.GONE);
        } else {
            btAction.setVisibility(View.VISIBLE);
            btAction.setText(R.string.action_perform_passwordinput);
            btAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        BaseDecryptResult atdr = CryptoHandlerFacade.getInstance(getActivity()).decryptWithLock(mOrigText, null);
                        //should never go through, but well, just in case
                        setData(activity, mOrigText, atdr, null, encryptionHandler);
                    } catch (UserInteractionRequiredException e) {
                        try {
                            activity.startIntentSenderForResult(e.getPendingIntent().getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_DECRYPT, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e1) {
                            e1.printStackTrace();
                            //TODO: what now?
                        }
                    }


                }
            });
        }
    }

    @SuppressWarnings("SameReturnValue")
    public abstract boolean onCreateOptionsMenu(Activity activity, Menu menu);

    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_share_decrypted).setVisible(mTdr != null && mTdr.isOk());


    }

    public void onOptionsItemSelected(final Activity activity, MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share_encrypted) {
            share(activity, mOrigText, activity.getString(R.string.action_share_encrypted));
        } else if (id == R.id.action_share_decrypted) {
            new MaterialDialog.Builder(activity)
                    .title(R.string.confirmation_share_decrypted__title)
                    .content(R.string.confirmation_share_decrypted__content)
                    .positiveText(R.string.action_share)
                    .negativeText(R.string.action_cancel)
                    .autoDismiss(true)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                Inner.InnerData innerData = mTdr.getDecryptedDataAsInnerData();
                                if (innerData.hasTextAndPaddingV0()) {
                                    Inner.TextAndPaddingV0 textAndPadding = mTdr.getDecryptedDataAsInnerData().getTextAndPaddingV0();

                                    share(activity, textAndPadding.getText(), activity.getString(R.string.action_share_decrypted));
                                } else {
                                    Ln.w("Can't share inner data of type %s", mTdr.getDecryptedDataAsInnerData().getDataCase().name());
                                }
                            } catch (InvalidProtocolBufferException e) {
                                try {
                                    String innerText = mTdr.getDecryptedDataAsUtf8String();
                                    share(activity, innerText, activity.getString(R.string.action_share_decrypted));
                                } catch (UnsupportedEncodingException e1) {
                                    Ln.w("Can't share inner data!");
                                }
                            }

                        }
                    })
                    .show();

        }

    }

    protected void share(Activity activity, String data, String title) {
        Intent intent2 = new Intent();
        intent2.setAction(Intent.ACTION_SEND);
        intent2.setType("text/plain");
        intent2.putExtra(Intent.EXTRA_TEXT, data);
        activity.startActivity(Intent.createChooser(intent2, title));
    }

    public void setArgs(String packageName) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PACKAGENAME, packageName);
        setArguments(bundle);
    }
}
