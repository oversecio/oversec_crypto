package io.oversec.one.crypto.gpg.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import io.oversec.one.crypto.*;
import io.oversec.one.crypto.gpg.GpgCryptoHandler;
import io.oversec.one.crypto.gpg.GpgDecryptResult;
import io.oversec.one.crypto.gpg.OpenKeychainConnector;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.ui.AbstractTextEncryptionInfoFragment;
import io.oversec.one.crypto.ui.EncryptionInfoActivity;
import org.openintents.openpgp.OpenPgpSignatureResult;

import java.util.List;

public class GpgTextEncryptionInfoFragment extends AbstractTextEncryptionInfoFragment {


    private GpgCryptoHandler mCryptoHandler;

    public static GpgTextEncryptionInfoFragment newInstance(String packagename) {
        GpgTextEncryptionInfoFragment fragment = new GpgTextEncryptionInfoFragment();
        fragment.setArgs(packagename);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.encryption_info_text_gpg, container, false);
        super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }

    @Override
    public void setData(final EncryptionInfoActivity activity, String encodedText, BaseDecryptResult tdr, UserInteractionRequiredException uix, final AbstractCryptoHandler encryptionHandler) {
        super.setData(activity, encodedText, tdr, uix, encryptionHandler);

        mCryptoHandler = (GpgCryptoHandler) encryptionHandler;

        GpgDecryptResult r = (GpgDecryptResult) tdr;

        TextView lblPgpRecipients = (TextView) mView.findViewById(R.id.lbl_pgp_recipients);
        TextView tvPgpRecipients = (TextView) mView.findViewById(R.id.tv_pgp_recipients);

        TextView lblPgpSignatureResult = (TextView) mView.findViewById(R.id.lbl_pgp_signature_result);
        TextView tvPgpSignatureResult = (TextView) mView.findViewById(R.id.tv_pgp_signature_result);
        TextView lblPgpSignatureKey = (TextView) mView.findViewById(R.id.lbl_pgp_signature_key);
        TextView tvPgpSignatureKey = (TextView) mView.findViewById(R.id.tv_pgp_signature_key);


        lblPgpRecipients.setVisibility(View.GONE);
        tvPgpRecipients.setVisibility(View.GONE);
        lblPgpSignatureResult.setVisibility(View.GONE);
        tvPgpSignatureResult.setVisibility(View.GONE);
        lblPgpSignatureKey.setVisibility(View.GONE);
        tvPgpSignatureKey.setVisibility(View.GONE);


        if (r == null) {

            lblPgpRecipients.setVisibility(View.GONE);
            tvPgpRecipients.setVisibility(View.GONE);

        } else {

            Outer.Msg msg = CryptoHandlerFacade.getEncodedData(activity, encodedText);
            byte[] raw = null;
            if (msg.hasMsgTextGpgV0()) {
                raw = msg.getMsgTextGpgV0().getCiphertext().toByteArray();
            }
            if (raw != null) {
                List<Long> pkids = GpgCryptoHandler.parsePublicKeyIds(raw);
                setPublicKeyIds(lblPgpRecipients, tvPgpRecipients, pkids, encryptionHandler);
            }


            if (r.getSignatureResult() != null) {
                lblPgpSignatureResult.setVisibility(View.VISIBLE);
                tvPgpSignatureResult.setVisibility(View.VISIBLE);

                OpenPgpSignatureResult sr = ((GpgDecryptResult) tdr).getSignatureResult();
                tvPgpSignatureResult.setText(GpgCryptoHandler.signatureResultToUiText(getActivity(), sr));
                tvPgpSignatureResult.setCompoundDrawablesWithIntrinsicBounds(0, 0, GpgCryptoHandler.signatureResultToUiIconRes(sr, false), 0);
                int color = ContextCompat.getColor(getActivity(),GpgCryptoHandler.signatureResultToUiColorResId(sr));
                tvPgpSignatureResult.setTextColor(color);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    tvPgpSignatureResult.setCompoundDrawableTintList(GpgCryptoHandler.getColorStateListAllStates(color));
//                }
                switch (sr.getResult()) {
                    case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                    case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                    case OpenPgpSignatureResult.RESULT_NO_SIGNATURE: {
                        lblPgpSignatureKey.setVisibility(View.GONE);
                        tvPgpSignatureKey.setVisibility(View.GONE);
                        break;
                    }
                    case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
                    case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
                    case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                    case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                    case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED: {
                        lblPgpSignatureKey.setVisibility(View.VISIBLE);
                        tvPgpSignatureKey.setVisibility(View.VISIBLE);

                        String sb = sr.getPrimaryUserId() +
                                "\n" +
                                "[" + SymUtil.longToPrettyHex(sr.getKeyId()) + "]";

                        tvPgpSignatureKey.setText(sb);
                        tvPgpSignatureKey.setCompoundDrawablesWithIntrinsicBounds(0, 0, GpgCryptoHandler.signatureResultToUiIconRes_KeyOnly(sr, false), 0);
                        int kColor = ContextCompat.getColor(getActivity(),GpgCryptoHandler.signatureResultToUiColorResId_KeyOnly(sr));
                        tvPgpSignatureKey.setTextColor(kColor);
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
                            GpgDecryptResult xtdr = (GpgDecryptResult) CryptoHandlerFacade.getInstance(getActivity()).decryptWithLock(mOrigText, null);
                            if (xtdr.isOk() && xtdr.getDownloadMissingSignatureKeyPendingIntent() != null) {
                                try {
                                    activity.startIntentSenderForResult(xtdr.getDownloadMissingSignatureKeyPendingIntent().getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_DOWNLOAD_MISSING_KEYS, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e1) {
                                    e1.printStackTrace();
                                    //TODO: what now?
                                }
                            }
                        } catch (UserInteractionRequiredException e) {
                            //should not happen here but well
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
                            GpgDecryptResult xtdr = (GpgDecryptResult) CryptoHandlerFacade.getInstance(getActivity()).decryptWithLock(mOrigText, null);
                            if (xtdr.isOk() && xtdr.getShowSignatureKeyPendingIntent() != null) {
                                try {
                                    activity.startIntentSenderForResult(xtdr.getShowSignatureKeyPendingIntent().getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_SHOW_SIGNATURE_KEY, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e1) {
                                    e1.printStackTrace();
                                    //TODO: what now?
                                }
                            }
                        } catch (UserInteractionRequiredException e) {
                            //should not happen here but well
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

    @Override
    public boolean onCreateOptionsMenu(Activity activity, Menu menu) {
        activity.getMenuInflater().inflate(R.menu.gpg_menu_encryption_info, menu);
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_share_gpg_ascii).setVisible(mTdr != null);

    }

    @Override
    public void onOptionsItemSelected(Activity activity, MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share_gpg_ascii) {
            share(activity, GpgCryptoHandler.getRawMessageAsciiArmoured(CryptoHandlerFacade.getEncodedData(activity, mOrigText)), activity.getString(R.string.action_share_gpg_ascii));
        } else {
            super.onOptionsItemSelected(activity, item);
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
        PendingIntent pi = mCryptoHandler.getDownloadKeyPendingIntent(keyId, actionIntent);

        if (pi != null) {
            try {
                getActivity().startIntentSenderForResult(pi.getIntentSender(), EncryptionInfoActivity.REQUEST_CODE_DOWNLOAD_MISSING_KEYS, null, 0, 0, 0);

            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }

        }

    }
}
