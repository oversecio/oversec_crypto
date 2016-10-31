package io.oversec.one.crypto.symsimple.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
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
import io.oversec.one.crypto.encoding.ZeroWidthXCoder;
import io.oversec.one.crypto.sym.SymPreferences;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.KeyCache;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symsimple.PasswordCantDecryptException;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;
import io.oversec.one.crypto.ui.util.KeystoreTTLSpinner;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

//TODO refactor this and UnlockKeyActivity to have a common base class
public class AddPasswordKeyActivity extends FragmentActivity {

    public static final String EXTRA_RESULT_KEY_ID = "EXTRA_RESULT_KEY_ID";
    private static final String FRAGMENT_TAG = "dialog";
    private static final String EXTRA_KEYHASH_ID = "EXTRA_KEYHASH_ID";
    private static final String EXTRA_KEYHASH_SALT = "EXTRA_KEYHASH_SALT";
    private static final String EXTRA_KEYHASH_COST = "EXTRA_KEYHASH_COST";
    private static final String EXTRA_ENCRYPTED_TEXT = "EXTRA_ENCRYPTED_TEXT";
    private static final String EXTRA_SHOW_IGNORE = "EXTRA_SHOW_IGNORE";

    public static PendingIntent buildPendingIntent(
            Context ctx,
            long[] expectedSessionKeyHashes,
            byte[][] saltForSessionKeyHash,
            int costForSessionKeyHash,
            String encryptedText) {
        Intent i = new Intent();
        i.setClass(ctx, AddPasswordKeyActivity.class);
        if (expectedSessionKeyHashes != null) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(EXTRA_KEYHASH_SALT, saltForSessionKeyHash);
            bundle.putLongArray(EXTRA_KEYHASH_ID, expectedSessionKeyHashes);
            bundle.putInt(EXTRA_KEYHASH_COST, costForSessionKeyHash);
            bundle.putString(EXTRA_ENCRYPTED_TEXT, encryptedText);
            bundle.putBoolean(EXTRA_SHOW_IGNORE, true);
            i.putExtras(bundle);

        }

        int flags = PendingIntent.FLAG_ONE_SHOT
                | PendingIntent.FLAG_CANCEL_CURRENT
                | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent res = PendingIntent.getActivity(ctx, 0,
                i, flags);

        return res;
    }

    public static void showForResult(Fragment frag, int rq) {

        Intent i = new Intent();
        i.putExtra(EXTRA_SHOW_IGNORE, false);
        i.setClass(frag.getActivity(), AddPasswordKeyActivity.class);
        frag.startActivityForResult(i, rq);


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

        private TextView mTitle;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.AppTheme);


            AlertDialog.Builder alert = new AlertDialog.Builder(ctw);

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()


            LayoutInflater inflater = LayoutInflater.from(ctw);
            mLayout = (FrameLayout) inflater.inflate(R.layout.passphrase_dialog, null);
            alert.setView(mLayout);

            mTitle = (TextView) mLayout.findViewById(R.id.passphrase_text);
            mTitle.setText(getString(R.string.simplesym_add_password_title));

            String encryptedText = getActivity().getIntent().getStringExtra(EXTRA_ENCRYPTED_TEXT);
            if (encryptedText != null) {
                ((TextView) mLayout.findViewById(R.id.orig_text)).setText(ZeroWidthXCoder.stripInvisible(encryptedText));
                mLayout.findViewById(R.id.orig_text_container).setVisibility(View.VISIBLE);
            }

            mPassphraseEditText = (EditText) mLayout.findViewById(R.id.passphrase_passphrase);
            mPassphraseWrapper = (TextInputLayout) mLayout.findViewById(R.id.passphrase_wrapper);

            mPassphraseWrapper.setHint(getString(R.string.simplesym_add_password_hint));

            mTTLSpinner = (KeystoreTTLSpinner) mLayout.findViewById(R.id.ttl_spinner);
            mTTLSpinner.setSelectedTTL(SymPreferences.getPreferences(getContext()).getKeystoreSimpleTTL());

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
                    activity.getString(R.string.action_save_shared_passphrase), (DialogInterface.OnClickListener) null);


            if (getActivity().getIntent().getBooleanExtra(EXTRA_SHOW_IGNORE, false)) {

                dialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                        activity.getString(R.string.action_ignore), (DialogInterface.OnClickListener) null);
            }

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

                    long[] sessionKeyIds = null;
                    byte[][] sessionKeySalts = null;
                    int sessionKeyCost = 0;
                    if (getArguments() != null && getArguments().get(EXTRA_KEYHASH_ID) != null) {
                        sessionKeyIds = getArguments().getLongArray(EXTRA_KEYHASH_ID);

                        //stupid dalvik would crash on casting Object[] to byte[][], so
                        //a little bit more expressive but seems to work...
                        Object[] xx = (Object[]) getArguments().getSerializable(EXTRA_KEYHASH_SALT);
                        //noinspection ConstantConditions
                        sessionKeySalts = new byte[xx.length][];
                        for (int i = 0; i < xx.length; i++) {
                            sessionKeySalts[i] = (byte[]) xx[i];
                        }
                        sessionKeyCost = getArguments().getInt(EXTRA_KEYHASH_COST);
                    }
                    if (aPassPhrase.length > 1) {
                        doOpen(aPassPhrase, timeToLiveSeconds, sessionKeyIds, sessionKeySalts, sessionKeyCost);
                    }


                }
            });
            if (getActivity().getIntent().getBooleanExtra(EXTRA_SHOW_IGNORE, false)) {
                final Button neutral = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEUTRAL);
                neutral.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().setResult(RESULT_FIRST_USER);
                        getActivity().finish();
                    }
                });
            }
        }

        private void doOpen(final char[] aPassPhrase, final int timeToLiveSeconds, final long[] expectedKeyIdHashes, final byte[][] saltsForKeyHash, final int costForKeyHash) {


            SymPreferences.getPreferences(getContext()).setKeystoreSimpleTTL(timeToLiveSeconds);
            final MaterialDialog progressDialog = new MaterialDialog.Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title(R.string.progress_generating_key)
                    .content(R.string.please_wait_keyderivation)
                    .progress(true, 0)
                    .show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        Long keyId = addPasswordToCache__longoperation(aPassPhrase, timeToLiveSeconds, expectedKeyIdHashes, saltsForKeyHash, costForKeyHash, KeyCache.getInstance(getActivity()));

                        dismiss();
                        Intent data = new Intent();
                        data.putExtra(EXTRA_RESULT_KEY_ID, keyId);
                        getActivity().setResult(RESULT_OK, data);
                        getActivity().finish();
                    } catch (final PasswordCantDecryptException e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPassphraseWrapper.setError(getString(R.string.error_simplesym_password_doesnt_match));
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPassphraseWrapper.setError(e.getLocalizedMessage());
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
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
            getActivity().setResult(RESULT_CANCELED);
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

    private static Long addPasswordToCache__longoperation(char[] pw, int ttl, long[] expectedKeyIdHashes, byte[][] saltsForKeyHash, int costForKeyHash, KeyCache keyCache) throws NoSuchAlgorithmException, IOException, PasswordCantDecryptException {
        String keyName = pw[0] + stars(pw.length - 2) + pw[pw.length - 1];

        byte[] rawkey = KeyUtil.brcryptifyPassword(pw, SimpleSymmetricCryptoHandler.KEY_DERIVATION_SALT, SimpleSymmetricCryptoHandler.KEY_DERIVATION_COST, 32);
        KeyUtil.erase(pw);


        long id = KeyUtil.calcKeyId(rawkey, SimpleSymmetricCryptoHandler.KEY_ID_COST);

        if (expectedKeyIdHashes != null) {
            //only succeed if the key id matches the expected one
            boolean match = false;
            for (int i = 0; i < expectedKeyIdHashes.length; i++) {
                Long hash = KeyUtil.calcSessionKeyId(id, saltsForKeyHash[i], costForKeyHash);
                if (hash.equals(expectedKeyIdHashes[i])) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                throw new PasswordCantDecryptException();
            }
        }

        SymmetricKeyPlain key = new SymmetricKeyPlain(id, keyName, new Date(), rawkey, true);

        keyCache.doCacheKey(key, ttl);

        return id;
    }

    private static String stars(int i) {
        if (i < 0) {
            i = 0;
        }
        StringBuilder sb = new StringBuilder(i);
        for (int k = 0; k < i; k++) {
            sb.append('*');
        }
        return sb.toString();
    }
}
