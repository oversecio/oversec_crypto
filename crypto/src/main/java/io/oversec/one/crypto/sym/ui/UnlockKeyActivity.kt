package io.oversec.one.crypto.sym.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import io.oversec.one.common.MainPreferences
import io.oversec.one.crypto.R
import io.oversec.one.crypto.sym.OversecKeystore2
import io.oversec.one.crypto.sym.SymPreferences
import io.oversec.one.crypto.sym.SymmetricKeyEncrypted
import io.oversec.one.crypto.symbase.KeyUtil
import io.oversec.one.crypto.symbase.OversecChacha20Poly1305
import io.oversec.one.crypto.ui.util.KeystoreTTLSpinner
import roboguice.util.Ln
import java.io.IOException

class UnlockKeyActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MainPreferences.isAllowScreenshots(this)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }


    override fun onResumeFragments() {
        super.onResumeFragments()

        val aEncryptedKey = OversecKeystore2.getInstance(this)
            .getSymmetricKeyEncrypted(intent.extras!!.getLong(EXTRA_KEY_ID, 0))
        if (aEncryptedKey == null) {
            Ln.w("something went wrong, couldn't find request key!")
            finish()
            return
        }

        val frag = PassphraseDialogFragment()
        frag.arguments = intent.extras
        frag.show(supportFragmentManager, FRAGMENT_TAG)
    }

    override fun onPause() {
        super.onPause()

        val dialog = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as DialogFragment?
        dialog?.dismiss()
    }


    class PassphraseDialogFragment : DialogFragment(), TextView.OnEditorActionListener {
        private lateinit var mPassphraseEditText: EditText
        private lateinit var mLayout: FrameLayout
        private lateinit var mTTLSpinner: KeystoreTTLSpinner
        private lateinit var mPassphraseWrapper: TextInputLayout
        private lateinit var mEncryptedKey: SymmetricKeyEncrypted
        private lateinit var mTitle: TextView

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity

            val ctw = ContextThemeWrapper(context, R.style.AppTheme)

            mEncryptedKey = OversecKeystore2.getInstance( requireActivity())
                .getSymmetricKeyEncrypted(arguments!!.getLong(EXTRA_KEY_ID, 0))!!


            val alert = AlertDialog.Builder(ctw)

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()


            val inflater = LayoutInflater.from(ctw)
            mLayout = inflater.inflate(R.layout.passphrase_dialog, null) as FrameLayout
            alert.setView(mLayout)

            mTitle = mLayout.findViewById<View>(R.id.passphrase_text) as TextView
            mTitle.text = getString(R.string.unlock_key_title, mEncryptedKey.name)

            mPassphraseEditText =
                    mLayout.findViewById<View>(R.id.passphrase_passphrase) as EditText
            mPassphraseWrapper =
                    mLayout.findViewById<View>(R.id.passphrase_wrapper) as TextInputLayout


            mTTLSpinner = mLayout.findViewById<View>(R.id.ttl_spinner) as KeystoreTTLSpinner
            mTTLSpinner.selectedTTL = SymPreferences.getInstance( requireActivity()).keystoreSymTTL

            alert.setNegativeButton(android.R.string.cancel) { dialog, id -> dialog.cancel() }


            // open keyboard.
            // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
            mPassphraseEditText.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus ->
                        mPassphraseEditText.post(Runnable {
                            if (getActivity() == null || mPassphraseEditText == null) {
                                return@Runnable
                            }
                            val imm =  requireActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(mPassphraseEditText, InputMethodManager.SHOW_IMPLICIT)
                        })
                    }
            mPassphraseEditText.requestFocus()

            mPassphraseEditText.setImeActionLabel(
                getString(android.R.string.ok),
                EditorInfo.IME_ACTION_DONE
            )
            mPassphraseEditText.setOnEditorActionListener(this)


            mPassphraseEditText.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

            mPassphraseEditText.transformationMethod = PasswordTransformationMethod.getInstance()

            val dialog = alert.create()
            dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                activity!!.getString(R.string.action_unlock_key),
                null as DialogInterface.OnClickListener?
            )

            return dialog
        }

        override fun onStart() {
            super.onStart()

            // Override the default behavior so the dialog is NOT dismissed on click
            val positive = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val editable = mPassphraseEditText.text
                val pl = editable.length
                val aPassPhrase = CharArray(pl)
                editable.getChars(0, pl, aPassPhrase, 0)

                val timeToLiveSeconds = mTTLSpinner.selectedTTL

                doOpen(aPassPhrase, timeToLiveSeconds)
            }
        }

        private fun doOpen(aPassPhrase: CharArray, timeToLiveSeconds: Int) {
            val aKeystore = OversecKeystore2.getInstance(activity!!)

            SymPreferences.getInstance(context!!).keystoreSymTTL = timeToLiveSeconds
            val progressDialog = MaterialDialog.Builder(activity!!)
                .theme(Theme.LIGHT)
                .title(R.string.progress_unlocking)
                .content(R.string.please_wait_decrypting)
                .progress(true, 0)
                .show()
            Thread(Runnable {
                try {
                    aKeystore.doCacheKey__longoperation(
                        mEncryptedKey.id,
                        aPassPhrase,
                        timeToLiveSeconds.toLong()
                    )
                    dismiss()
                    val a = activity
                    if (a != null) {
                        a.setResult(Activity.RESULT_OK)
                        a.finish()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: OversecChacha20Poly1305.MacMismatchException) {
                    e.printStackTrace()
                    activity!!.runOnUiThread {
                        mPassphraseWrapper.error = getString(R.string.error_password_wrong)
                    }
                } finally {
                    KeyUtil.erase(aPassPhrase)
                    try {
                        activity!!.runOnUiThread {
                            mPassphraseEditText.setText("") //TODO better way to _really_ clear the internal char array of edittexts?
                            progressDialog.dismiss()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                }
            }).start()


        }

        override fun onCancel(dialog: DialogInterface?) {
            super.onCancel(dialog)
            requireActivity().also {  it.setResult(Activity.RESULT_CANCELED, it.intent)}.also { it.finish() }
        }

        override fun onDismiss(dialog: DialogInterface?) {
            super.onDismiss(dialog)

            hideKeyboard()
        }

        private fun hideKeyboard() {
            if (activity == null) {
                return
            }

            val inputManager = activity!!
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
            // Associate the "done" button on the soft keyboard with the okay button in the view
            if (EditorInfo.IME_ACTION_DONE == actionId) {
                val dialog = dialog as AlertDialog
                val bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

                bt.performClick()
                return true
            }
            return false
        }

    }

    companion object {

        private val FRAGMENT_TAG = "dialog"
        val EXTRA_KEY_ID = "key_id"

        fun buildPendingIntent(ctx: Context, keyId: Long?): PendingIntent {
            val i = Intent()
            i.setClass(ctx, UnlockKeyActivity::class.java)
            i.putExtra(EXTRA_KEY_ID, keyId)


            @SuppressLint("InlinedApi") val flags = (PendingIntent.FLAG_ONE_SHOT
                    or PendingIntent.FLAG_CANCEL_CURRENT
                    or PendingIntent.FLAG_IMMUTABLE)

            return PendingIntent.getActivity(
                ctx, 0,
                i, flags
            )
        }

        fun showForResult(ctx: Context, id: Long, rq: Int) {
            val i = Intent()
            i.setClass(ctx, UnlockKeyActivity::class.java)
            i.putExtra(EXTRA_KEY_ID, id)
            if (ctx !is Activity) {
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                ctx.startActivity(i)
            } else {
                i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                ctx.startActivityForResult(i, rq)
            }
        }
    }


}
