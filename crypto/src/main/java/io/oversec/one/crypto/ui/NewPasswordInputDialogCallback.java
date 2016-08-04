package io.oversec.one.crypto.ui;


public interface NewPasswordInputDialogCallback {
    void positiveAction(char[] pw);

    void neutralAction();
}
