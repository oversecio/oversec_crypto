package io.oversec.one.crypto.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.ui.util.EditTextPasswordWithVisibilityToggle;
import roboguice.util.Ln;
import uk.co.biddell.diceware.dictionaries.DiceWare;
import uk.co.biddell.diceware.dictionaries.DiceWords;

import java.io.IOException;

public class NewPasswordInputDialog {
    private static final int ENTROPY_MEDIUM = 45;
    private static final int ENTROPY_HIGH_SHARE = 75;
    private static final int ENTROPY_HIGH_PBKDF = 75;
    private static final int ENTROPY_HIGH_DEVICE = 60;


    private static final int DICEWARE_WORDS_KEYSTORE = 4;
    private static final int DICEWARE_WORDS_SHARE = 5;
    private static final int DICEWARE_WORDS_PBKDF = 6;

    public enum MODE {
        SHARE, KEYSTORE, PBKDF
    }

    public static void show(final Context ctx, final MODE mode, final NewPasswordInputDialogCallback callback) {
        final Zxcvbn zxcvbn = new Zxcvbn();


        final MaterialDialog dialog = new MaterialDialog.Builder(ctx)

                .customView(R.layout.new_password_input_dialog, false)
                .positiveText(getPositiveText(mode))
                .neutralText(R.string.common_cancel)
                .autoDismiss(false)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        callback.neutralAction();
                        //TODO: clear passwords on cancel?
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        View view = dialog.getCustomView();
                        if (handlePositive(view, callback, mode, zxcvbn)) {
                            dialog.dismiss();
                        }
                    }

                })
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        View view = dialog.getCustomView();
                        EditText etPw1 = (EditText) view.findViewById(R.id.new_password_password);
                        EditText etPw2 = (EditText) view.findViewById(R.id.new_password_password_again);
                        etPw1.setText(""); //TODO better way?
                        etPw2.setText(""); //TODO better way?
                        callback.neutralAction();
                        dialog.dismiss();
                    }
                }).build();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);

        final View view = dialog.getCustomView();

        final EditTextPasswordWithVisibilityToggle etPw1 = (EditTextPasswordWithVisibilityToggle) view.findViewById(R.id.new_password_password);
        final EditTextPasswordWithVisibilityToggle etPw2 = (EditTextPasswordWithVisibilityToggle) view.findViewById(R.id.new_password_password_again);

        etPw2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (handlePositive(view, callback, mode, zxcvbn)) {
                        dialog.dismiss();
                        return true;
                    }
                }
                return false;
            }
        });

        final TextInputLayout wrapPw1 = (TextInputLayout) view.findViewById(R.id.new_password_password_wrapper);

        TextView tvText = (TextView) view.findViewById(R.id.new_password_text);
        tvText.setText(getBody(mode));

        TextView tvTitle = (TextView) view.findViewById(R.id.new_password_title);
        tvTitle.setText(getTitle(mode));

        CheckBox cbWeak = (CheckBox) view.findViewById(R.id.cb_accept_weak_password);
        cbWeak.setVisibility(View.GONE);

        Button btSuggest = (Button) view.findViewById(R.id.new_password_generate);
        btSuggest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  cbShowPassphrase.setChecked(true);
                try {
                    DiceWords dw = new DiceWare(ctx).getDiceWords(
                            getDicewareExtraSecurity(mode),
                            getDicewareNumWords(mode));

                    etPw1.setText(dw.toString());
                    etPw1.setPasswordVisible(true);
                    etPw2.setPasswordVisible(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        });

        final SeekBar sbStrength = (SeekBar) view.findViewById(R.id.create_key_seekbar);
        etPw1.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int entropy = calcPasswordEntropy(s, wrapPw1, zxcvbn);
                updateSeekBar(view, sbStrength, entropy, mode);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        updateSeekBar(view, sbStrength, 0, mode);
        dialog.show();
    }


    private static int getBody(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return R.string.new_password_keystore_text;
            case PBKDF:
                return R.string.new_password_pbkdf_text;
            case SHARE:
                return R.string.new_password_share_text;
        }
        return 0;
    }

    private static int getTitle(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return R.string.new_password_keystore_title;
            case PBKDF:
                return R.string.new_password_pbkdf_title;
            case SHARE:
                return R.string.new_password_share_title;
        }
        return 0;
    }

    private static DiceWare.Type getDicewareExtraSecurity(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return DiceWare.Type.PASSPHRASE;
            case PBKDF:
                return DiceWare.Type.PASSPHRASE_EXTRA_SECURITY;
            case SHARE:
                return DiceWare.Type.PASSPHRASE_EXTRA_SECURITY;
        }
        return null;
    }

    private static int getDicewareNumWords(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return DICEWARE_WORDS_KEYSTORE;
            case PBKDF:
                return DICEWARE_WORDS_PBKDF;
            case SHARE:
                return DICEWARE_WORDS_SHARE;
        }
        return 0;
    }

    private static int getPositiveText(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return R.string.action_save;
            case PBKDF:
                return R.string.action_generate;
            case SHARE:
                return R.string.action_share;
        }
        return 0;

    }


    private static int getEntropyHighLevel(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return ENTROPY_HIGH_DEVICE;
            case PBKDF:
                return ENTROPY_HIGH_PBKDF;
            case SHARE:
                return ENTROPY_HIGH_SHARE;
        }
        return 0;
    }

    private static int getEntropyMinimum(MODE mode) {
        switch (mode) {
            case KEYSTORE:
                return ENTROPY_MEDIUM; //facilitate usage fur "dumb" users
            case PBKDF:
                return ENTROPY_MEDIUM; //facilitate usage fur "dumb" users
            case SHARE:
                return ENTROPY_HIGH_SHARE;
        }
        return 0;
    }

    private static void updateSeekBar(View view, SeekBar sbStrength, int entropy, MODE mode) {

        sbStrength.setMax(100);
        sbStrength.setProgress(Math.max(10, entropy));
        int color = R.color.password_strength_low;
        if (entropy >= ENTROPY_MEDIUM) {
            color = R.color.password_strength_medium;
        }


        if (entropy >= getEntropyHighLevel(mode)) {
            color = R.color.password_strength_high;
            CheckBox cbWeak = (CheckBox) view.findViewById(R.id.cb_accept_weak_password);
            cbWeak.setVisibility(View.GONE);
        }
        sbStrength.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(sbStrength.getContext(), color), PorterDuff.Mode.MULTIPLY));


    }


    private static int calcPasswordEntropy(CharSequence s, TextInputLayout wrapper, Zxcvbn zxcvbn) {
        try {
            Strength strength = zxcvbn.measure(s.toString());

            Feedback feedback = strength.getFeedback();
            wrapper.setError(feedback.getWarning());

            int res = (int) log2(strength.getGuesses());
            Ln.d("ENTROPY: %s", res);
            return res;
        } catch (Exception ex) {
            //see https://github.com/nulab/zxcvbn4j/issues/19   ->  issues resolved, zxcvbn lib upgraded, but leaving this in here just to be sure
            return 0;
        }
    }

    public static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    private static boolean handlePositive(View view, NewPasswordInputDialogCallback callback, MODE mode, Zxcvbn zxcvbn) {
        EditText etPw1 = (EditText) view.findViewById(R.id.new_password_password);
        EditText etPw2 = (EditText) view.findViewById(R.id.new_password_password_again);
        TextInputLayout wrapPw1 = (TextInputLayout) view.findViewById(R.id.new_password_password_wrapper);
        TextInputLayout wrapPw2 = (TextInputLayout) view.findViewById(R.id.new_password_password_again_wrapper);
        CheckBox cbWeak = (CheckBox) view.findViewById(R.id.cb_accept_weak_password);
        Editable editablePw1 = etPw1.getText();
        Editable editablePw2 = etPw2.getText();

        if (!editablePw1.toString().equals(editablePw2.toString())) {
            wrapPw1.setError(view.getContext().getString(R.string.error_passwords_dont_match));
            return false;
        }


        int entropy = calcPasswordEntropy(etPw1.getText(), wrapPw1, zxcvbn);

        if (entropy < getEntropyMinimum(mode) && !cbWeak.isChecked()) {
            wrapPw1.setError(view.getContext().getString(R.string.error_password_length));

            cbWeak.setVisibility(View.VISIBLE);
            cbWeak.requestFocus();
            cbWeak.getParent().requestChildFocus(cbWeak, cbWeak);
            return false;
        }


        int pl = editablePw1.length();
        char[] aPassPhrase = new char[pl];
        editablePw1.getChars(0, pl, aPassPhrase, 0);

        etPw1.setText(""); //TODO better way?
        etPw2.setText(""); //TODO better way?

        callback.positiveAction(aPassPhrase);


        return true;
    }


}
