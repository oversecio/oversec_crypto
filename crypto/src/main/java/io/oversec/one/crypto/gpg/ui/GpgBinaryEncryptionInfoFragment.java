package io.oversec.one.crypto.gpg.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.gpg.GpgCryptoHandler;
import io.oversec.one.crypto.gpg.GpgDecryptResult;
import io.oversec.one.crypto.gpg.OpenKeychainConnector;
import io.oversec.one.crypto.images.xcoder.ImageXCoder;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.ui.AbstractBinaryEncryptionInfoFragment;
import io.oversec.one.crypto.ui.EncryptionInfoActivity;
import org.openintents.openpgp.OpenPgpSignatureResult;

import java.util.List;

public class GpgBinaryEncryptionInfoFragment extends AbstractBinaryEncryptionInfoFragment {


    private TextView mTvPgpRecipients;
    private TextView mTvPgpSignatureResult;
    private TextView mTvPgpSignatureKey;
    private TextView mLblPgpRecipients;
    private TextView mLblPgpSignatureResult;
    private TextView mLblPgpSignatureKey;

    public static GpgBinaryEncryptionInfoFragment newInstance(String packagename) {
        GpgBinaryEncryptionInfoFragment fragment = new GpgBinaryEncryptionInfoFragment();
        fragment.setArgs(packagename);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.encryption_info_binary_gpg, container, false);

        mLblPgpRecipients = (TextView) mView.findViewById(R.id.lbl_pgp_recipients);
        mTvPgpRecipients = (TextView) mView.findViewById(R.id.tv_pgp_recipients);

        mLblPgpSignatureResult = (TextView) mView.findViewById(R.id.lbl_pgp_signature_result);
        mTvPgpSignatureResult = (TextView) mView.findViewById(R.id.tv_pgp_signature_result);
        mLblPgpSignatureKey = (TextView) mView.findViewById(R.id.lbl_pgp_signature_key);
        mTvPgpSignatureKey = (TextView) mView.findViewById(R.id.tv_pgp_signature_key);

        mLblPgpRecipients.setVisibility(View.GONE);
        mTvPgpRecipients.setVisibility(View.GONE);
        mLblPgpSignatureResult.setVisibility(View.GONE);
        mTvPgpSignatureResult.setVisibility(View.GONE);
        mLblPgpSignatureKey.setVisibility(View.GONE);
        mTvPgpSignatureKey.setVisibility(View.GONE);

        super.onCreateView(inflater, container, savedInstanceState);

        return mView;
    }

    @Override
    protected void handleSetData(Outer.Msg msg, BaseDecryptResult tdr, ImageXCoder coder) {

        final GpgDecryptResult r = (GpgDecryptResult) tdr;


        if (r == null) {

            mLblPgpRecipients.setVisibility(View.GONE);
            mTvPgpRecipients.setVisibility(View.GONE);

        } else {
            GpgCryptoHandler cryptoHandler = (GpgCryptoHandler) CryptoHandlerFacade.getInstance(getActivity()).getCryptoHandler(EncryptionMethod.GPG);

            byte[] raw = null;
            if (msg.hasMsgTextGpgV0()) {
                raw = msg.getMsgTextGpgV0().getCiphertext().toByteArray();
            }
            if (raw != null) {
                List<Long> pkids = GpgCryptoHandler.parsePublicKeyIds(raw);
                setPublicKeyIds(mLblPgpRecipients, mTvPgpRecipients, pkids, cryptoHandler);
            }


            if (r.getSignatureResult() != null) {
                mLblPgpSignatureResult.setVisibility(View.VISIBLE);
                mTvPgpSignatureResult.setVisibility(View.VISIBLE);

                OpenPgpSignatureResult sr = r.getSignatureResult();
                mTvPgpSignatureResult.setText(GpgCryptoHandler.signatureResultToUiText(getActivity(), sr));
                mTvPgpSignatureResult.setCompoundDrawablesWithIntrinsicBounds(0, 0, GpgCryptoHandler.signatureResultToUiIconRes(sr, false), 0);
                int color = ContextCompat.getColor(getActivity(),GpgCryptoHandler.signatureResultToUiColorResId(sr));
                mTvPgpSignatureResult.setTextColor(color);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    tvPgpSignatureResult.setCompoundDrawableTintList(GpgCryptoHandler.getColorStateListAllStates(color));
//                }
                switch (sr.getResult()) {
                    case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                    case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                    case OpenPgpSignatureResult.RESULT_NO_SIGNATURE: {
                        mLblPgpSignatureKey.setVisibility(View.GONE);
                        mTvPgpSignatureKey.setVisibility(View.GONE);
                        break;
                    }
                    case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
                    case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
                    case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                    case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                    case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED: {
                        mLblPgpSignatureKey.setVisibility(View.VISIBLE);
                        mTvPgpSignatureKey.setVisibility(View.VISIBLE);

                        String sb = sr.getPrimaryUserId() +
                                "\n" +
                                "[" + SymUtil.longToPrettyHex(sr.getKeyId()) + "]";

                        mTvPgpSignatureKey.setText(sb);
                        mTvPgpSignatureKey.setCompoundDrawablesWithIntrinsicBounds(0, 0, GpgCryptoHandler.signatureResultToUiIconRes_KeyOnly(sr, false), 0);
                        int kColor = ContextCompat.getColor(getActivity(),GpgCryptoHandler.signatureResultToUiColorResId_KeyOnly(sr));
                        mTvPgpSignatureKey.setTextColor(kColor);
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                            tvPgpSignatureKey.setCompoundDrawableTintList(GpgCryptoHandler.getColorStateListAllStates(kColor));
//                        }
                    }

                }
            }


        }

        Button btKeyDetails = (Button) mView.findViewById(R.id.btnKeyDetailsGpg);
        btKeyDetails.setVisibility(View.GONE);
        Button btKeyAction = (Button) mView.findViewById(R.id.btnKeyActionGpg);
        btKeyAction.setVisibility(View.GONE);

        if (r != null) {

            if (r.getDownloadMissingSignatureKeyPendingIntent() != null) {
                btKeyAction.setVisibility(View.VISIBLE);
                btKeyAction.setText(R.string.action_download_missing_signature_key);
                btKeyAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        try {
                            getActivity().startIntentSenderForResult(r.getDownloadMissingSignatureKeyPendingIntent().getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_DOWNLOAD_MISSING_KEYS, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e1) {
                            e1.printStackTrace();
                            //TODO: what now?
                        }


                    }
                });
            } else if (r.getShowSignatureKeyPendingIntent() != null) {
                btKeyAction.setVisibility(View.VISIBLE);
                btKeyAction.setText(R.string.action_show_signature_key);
                btKeyAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        try {
                            getActivity().startIntentSenderForResult(r.getShowSignatureKeyPendingIntent().getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_SHOW_SIGNATURE_KEY, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e1) {
                            e1.printStackTrace();
                            //TODO: what now?
                        }


                    }
                });
            }


            btKeyDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //for now we can only jump to the list of keys [i.e. the main activity] in OKC, since we're dealing with subkeys here...
                    GpgCryptoHandler.openOpenKeyChain(getActivity());
                }
            });

        }
    }

    private void setPublicKeyIds(TextView lblPgpRecipients, TextView tvPgpRecipients, List<Long> publicKeyIds, AbstractCryptoHandler encryptionHandler) {
        GpgCryptoHandler pe = (GpgCryptoHandler) encryptionHandler;

        int okcVersion = OpenKeychainConnector.getVersion(lblPgpRecipients.getContext());


        if (publicKeyIds != null) {
            lblPgpRecipients.setVisibility(View.VISIBLE);
            tvPgpRecipients.setVisibility(View.VISIBLE);
            SpannableStringBuilder sb = new SpannableStringBuilder();
            for (final long pkid : publicKeyIds) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }

                String userName = pe.getFirstUserIDByKeyId(pkid, null);
                if (userName != null) {
                    sb.append(userName).append("\n[").append(SymUtil.longToPrettyHex(pkid)).append("]");
                } else {
                    //userName might be null if OKC version < OpenKeychainConnector.V_GET_SUBKEY
                    //however in older versions we still don't know if the user doesn't have the key
                    //so best for now just show the keyId only without any additional remarks
                    //that might just confuse users.

                    if (okcVersion >= OpenKeychainConnector.V_GET_SUBKEY) {

                        int start = sb.length();
                        sb.append(lblPgpRecipients.getContext().getString(R.string.action_download_missing_public_key));
                        int end = sb.length();
                        ClickableSpan clickToDownloadKey = new ClickableSpan() {
                            public void onClick(View view) {
                                downloadKey(pkid, null);
                            }
                        };
                        sb.setSpan(clickToDownloadKey, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        sb.append("\n");
                        sb.append("[").append(SymUtil.longToPrettyHex(pkid)).append("]");
                    } else {
                        sb.append("[").append(SymUtil.longToPrettyHex(pkid)).append("]");
                    }
                }

            }
            tvPgpRecipients.setText(sb);
            tvPgpRecipients.setMovementMethod(LinkMovementMethod.getInstance());

        }

    }

    private synchronized void downloadKey(final long keyId, Intent actionIntent) {

        GpgCryptoHandler cryptoHandler = (GpgCryptoHandler) CryptoHandlerFacade.getInstance(getActivity()).getCryptoHandler(EncryptionMethod.GPG);
        PendingIntent pi = cryptoHandler.getDownloadKeyPendingIntent(keyId, actionIntent);

        if (pi != null) {
            try {
                getActivity().startIntentSenderForResult(pi.getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_DOWNLOAD_MISSING_KEYS, null, 0, 0, 0);

            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }

        }

    }
}
