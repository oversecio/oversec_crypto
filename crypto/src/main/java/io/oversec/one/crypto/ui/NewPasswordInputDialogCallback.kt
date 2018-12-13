package io.oversec.one.crypto.ui

interface NewPasswordInputDialogCallback {
    fun positiveAction(pw: CharArray)
    fun neutralAction()
}
