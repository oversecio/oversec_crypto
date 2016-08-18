package io.oversec.one.crypto.sym.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import io.oversec.one.common.MainPreferences;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.sym.SymPreferences;
import io.oversec.one.crypto.sym.SymmetricKeyEncrypted;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symbase.OversecChacha20Poly1305;
import io.oversec.one.crypto.ui.util.KeystoreTTLSpinner;
import roboguice.util.Ln;

import java.io.IOException;

public class UnlockKeyActivity extends FragmentActivity {

    private static final String FRAGMENT_TAG = "dialog";
    public static final String EXTRA_KEY_ID = "key_id";

    public static PendingIntent buildPendingIntent(Context ctx, Long keyId) {
        Intent i = new Intent();
        i.setClass(ctx, UnlockKeyActivity.class);
        i.putExtra(EXTRA_KEY_ID, keyId);


        @SuppressLint("InlinedApi") int flags = PendingIntent.FLAG_ONE_SHOT
                | PendingIntent.FLAG_CANCEL_CURRENT
                | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent res = PendingIntent.getActivity(ctx, 0,
                i, flags);

        return res;
    }

    public static void showForResult(Context ctx, long id, int rq) {
        Intent i = new Intent();
        i.setClass(ctx, UnlockKeyActivity.class);
        i.putExtra(EXTRA_KEY_ID, id);
        if (!(ctx instanceof Activity)) {
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            ctx.startActivity(i);
        } else {
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ((Activity) ctx).startActivityForResult(i, rq);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!MainPreferences.isAllowScreenshots(this)) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

    }


    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        SymmetricKeyEncrypted aEncryptedKey = OversecKeystore2.getInstance(this).getSymmetricKeyEncrypted(getIntent().getExtras().getLong(EXTRA_KEY_ID, 0));
        if (aEncryptedKey==null) {
            Ln.w("something went wrong, couldn't find request key!");
            finish();
            return;
        }

        PassphraseDialogFragment frag = new PassphraseDialogFragment();
        frag.setArguments(getIntent().getExtras());
        frag.show(getSupportFragmentManager(), FRAGMENT_TAG);
    }

    @Override
    protected void onPause() {
        super.onPause();

        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (dialog != null) {
            dialog.dismiss();
        }
    }


    public static class PassphraseDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {
        private EditText mPassphraseEditText;


        private FrameLayout mLayout;
        private KeystoreTTLSpinner mTTLSpinner;
        private TextInputLayout mPassphraseWrapper;
        private SymmetricKeyEncrypted mEncryptedKey;
        private TextView mTitle;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.AppTheme);

            mEncryptedKey = OversecKeystore2.getInstance(getActivity()).getSymmetricKeyEncrypted(getArguments().getLong(EXTRA_KEY_ID, 0));


            AlertDialog.Builder alert = new AlertDialog.Builder(ctw);

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()


            LayoutInflater inflater = LayoutInflater.from(ctw);
            mLayout = (FrameLayout) inflater.inflate(R.layout.passphrase_dialog, null);
            alert.setView(mLayout);

            mTitle = (TextView) mLayout.findViewById(R.id.passphrase_text);
            mTitle.setText(getString(R.string.unlock_key_title, mEncryptedKey.getName()));

            mPassphraseEditText = (EditText) mLayout.findViewById(R.id.passphrase_passphrase);
            mPassphraseWrapper = (TextInputLayout) mLayout.findViewById(R.id.passphrase_wrapper);


            mTTLSpinner = (KeystoreTTLSpinner) mLayout.findViewById(R.id.ttl_spinner);
            mTTLSpinner.setSelectedTTL(SymPreferences.getPreferences(getContext()).getKeystoreSymTTL());

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });


            // open keyboard.
            // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
            mPassphraseEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    mPassphraseEditText.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() == null || mPassphraseEditText == null) {
                                return;
                            }
                            InputMethodManager imm = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(mPassphraseEditText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                }
            });
            mPassphraseEditText.requestFocus();

            mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
            mPassphraseEditText.setOnEditorActionListener(this);


            mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            mPassphraseEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

            AlertDialog dialog = alert.create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                    activity.getString(R.string.action_unlock_key), (DialogInterface.OnClickListener) null);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();

            // Override the default behavior so the dialog is NOT dismissed on click
            final Button positive = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Editable editable = mPassphraseEditText.getText();
                    int pl = editable.length();
                    char[] aPassPhrase = new char[pl];
                    editable.getChars(0, pl, aPassPhrase, 0);


                    final int timeToLiveSeconds = mTTLSpinner.getSelectedTTL();


                    doOpen(aPassPhrase, timeToLiveSeconds);


                }
            });
        }

        private void doOpen(final char[] aPassPhrase, final int timeToLiveSeconds) {
            final OversecKeystore2 aKeystore = OversecKeystore2.getInstance(getActivity());


            SymPreferences.getPreferences(getContext()).setKeystoreSymTTL(timeToLiveSeconds);
            final MaterialDialog progressDialog = new MaterialDialog.Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title(R.string.progress_unlocking)
                    .content(R.string.please_wait_decrypting)
                    .progress(true, 0)
                    .show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        aKeystore.doCacheKey__longoperation(mEncryptedKey.getId(), aPassPhrase, timeToLiveSeconds);
                        dismiss();
                        getActivity().setResult(RESULT_OK);
                        getActivity().finish();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (OversecChacha20Poly1305.MacMismatchException e) {
                        e.printStackTrace();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPassphraseWrapper.setError(getString(R.string.error_password_wrong));
                            }
                        });
                    } finally {
                        KeyUtil.erase(aPassPhrase);
                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPassphraseEditText.setText(""); //TODO better way to _really_ clear the internal char array of edittexts?
                                    progressDialog.dismiss();
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }).start();


        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            getActivity().setResult(RESULT_CANCELED, getActivity().getIntent());
            getActivity().finish();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            hideKeyboard();
        }

        private void hideKeyboard() {
            if (getActivity() == null) {
                return;
            }

            InputMethodManager inputManager = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Associate the "done" button on the soft keyboard with the okay button in the view
            if (EditorInfo.IME_ACTION_DONE == actionId) {
                AlertDialog dialog = ((AlertDialog) getDialog());
                Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                bt.performClick();
                return true;
            }
            return false;
        }

    }


}
