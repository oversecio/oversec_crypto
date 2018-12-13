package io.oversec.one.crypto.symsimple.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.Fragment
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
import io.oversec.one.crypto.encoding.ZeroWidthXCoder
import io.oversec.one.crypto.sym.SymPreferences
import io.oversec.one.crypto.sym.SymmetricKeyPlain
import io.oversec.one.crypto.symbase.KeyCache
import io.oversec.one.crypto.symbase.KeyUtil
import io.oversec.one.crypto.symsimple.PasswordCantDecryptException
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler
import io.oversec.one.crypto.ui.util.KeystoreTTLSpinner
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Date
import kotlin.math.max

//TODO refactor this and UnlockKeyActivity to have a common base class
class AddPasswordKeyActivity : FragmentActivity() {

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
        private lateinit var mLayout: FrameLayout

        lateinit var tvTitle: TextView
        lateinit var tvOrigText: TextView
        lateinit var etPassPhrase: EditText
        lateinit var passphraseWrapper: TextInputLayout
        lateinit var tTLSpinner : KeystoreTTLSpinner

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            val ctw = ContextThemeWrapper(context, R.style.MyAppTheme)
            val alert = AlertDialog.Builder(ctw)

            // No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
            //alert.setTitle()

            val inflater = LayoutInflater.from(ctw)
            mLayout = inflater.inflate(R.layout.passphrase_dialog, null) as FrameLayout
            alert.setView(mLayout)

            tvTitle = mLayout.findViewById(R.id.passphrase_text) as TextView
            tvOrigText = mLayout.findViewById(R.id.orig_text) as TextView
            etPassPhrase = mLayout.findViewById(R.id.passphrase_passphrase) as EditText
            passphraseWrapper = mLayout.findViewById(R.id.passphrase_wrapper) as TextInputLayout
            tTLSpinner = mLayout.findViewById(R.id.ttl_spinner) as KeystoreTTLSpinner

            tvTitle.setText(getString(R.string.simplesym_add_password_title))

            val encryptedText = getActivity()!!.intent.getStringExtra(EXTRA_ENCRYPTED_TEXT)
            if (encryptedText != null) {
                tvOrigText.text = ZeroWidthXCoder.stripInvisible(encryptedText)
                (mLayout.findViewById(R.id.orig_text_container) as View).setVisibility(View.VISIBLE)

            }

            passphraseWrapper.hint = getString(R.string.simplesym_add_password_hint)
            tTLSpinner.selectedTTL = SymPreferences.getInstance(context!!).keystoreSimpleTTL

            alert.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }


            // open keyboard.
            // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
            etPassPhrase.onFocusChangeListener =
                    View.OnFocusChangeListener { v, hasFocus ->
                        etPassPhrase.post(Runnable {
                            if (getActivity() == null || etPassPhrase == null) {
                                return@Runnable
                            }
                            val imm = getActivity()!!
                                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(
                                etPassPhrase,
                                InputMethodManager.SHOW_IMPLICIT
                            )
                        })
                    }
            etPassPhrase.requestFocus()

            etPassPhrase.setImeActionLabel(
                getString(android.R.string.ok),
                EditorInfo.IME_ACTION_DONE
            )
            etPassPhrase.setOnEditorActionListener(this)


            etPassPhrase.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

            etPassPhrase.transformationMethod = PasswordTransformationMethod.getInstance()

            val dialog = alert.create()


            dialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                activity!!.getString(R.string.action_save_shared_passphrase),
                null as DialogInterface.OnClickListener?
            )


            if (getActivity()!!.intent.getBooleanExtra(EXTRA_SHOW_IGNORE, false)) {

                dialog.setButton(
                    DialogInterface.BUTTON_NEUTRAL,
                    activity.getString(R.string.action_ignore),
                    null as DialogInterface.OnClickListener?
                )
            }

            return dialog
        }

        override fun onStart() {
            super.onStart()

            // Override the default behavior so the dialog is NOT dismissed on click
            val positive = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
            positive.setOnClickListener {

                val editable = etPassPhrase.text
                val pl = editable.length
                val aPassPhrase = CharArray(pl)
                editable.getChars(0, pl, aPassPhrase, 0)


                val timeToLiveSeconds = tTLSpinner.selectedTTL

                var sessionKeyIds: LongArray? = null
                var sessionKeySalts: Array<ByteArray>? = null
                var sessionKeyCost = 0
                if (arguments != null && arguments!!.get(EXTRA_KEYHASH_ID) != null) {
                    sessionKeyIds = arguments!!.getLongArray(EXTRA_KEYHASH_ID)

                    //stupid dalvik would crash on casting Object[] to byte[][], so
                    //a little bit more expressive but seems to work...
                    val xx = arguments!!.getSerializable(EXTRA_KEYHASH_SALT) as Array<Any>

                    sessionKeySalts = Array(xx.size) { i -> xx[i] as ByteArray }

                    sessionKeyCost = arguments!!.getInt(EXTRA_KEYHASH_COST)
                }
                if (aPassPhrase.size > 1) {
                    doOpen(
                        aPassPhrase,
                        timeToLiveSeconds,
                        sessionKeyIds,
                        sessionKeySalts,
                        sessionKeyCost
                    )
                }
            }
            if (activity!!.intent.getBooleanExtra(EXTRA_SHOW_IGNORE, false)) {
                val neutral = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_NEUTRAL)
                neutral.setOnClickListener {
                    activity!!.setResult(Activity.RESULT_FIRST_USER)
                    activity!!.finish()
                }
            }
        }

        private fun doOpen(
            aPassPhrase: CharArray,
            timeToLiveSeconds: Int,
            expectedKeyIdHashes: LongArray?,
            saltsForKeyHash: Array<ByteArray>?,
            costForKeyHash: Int
        ) {


            SymPreferences.getInstance(context!!).keystoreSimpleTTL = timeToLiveSeconds
            val progressDialog = MaterialDialog.Builder(activity!!)
                .theme(Theme.LIGHT)
                .title(R.string.progress_generating_key)
                .content(R.string.please_wait_keyderivation)
                .progress(true, 0)
                .show()
            Thread(Runnable {
                try {

                    val keyId = addPasswordToCache__longoperation(
                        aPassPhrase,
                        timeToLiveSeconds,
                        expectedKeyIdHashes,
                        saltsForKeyHash,
                        costForKeyHash,
                        KeyCache.getInstance(activity!!)
                    )

                    dismiss()
                    val data = Intent()
                    data.putExtra(EXTRA_RESULT_KEY_ID, keyId)
                    activity!!.setResult(Activity.RESULT_OK, data)
                    activity!!.finish()
                } catch (e: PasswordCantDecryptException) {
                    activity!!.runOnUiThread {
                        passphraseWrapper.error =
                                getString(R.string.error_simplesym_password_doesnt_match)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        activity!!.runOnUiThread { passphraseWrapper.error = e.localizedMessage }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }

                } finally {
                    KeyUtil.erase(aPassPhrase)
                    try {
                        activity!!.runOnUiThread {
                            etPassPhrase.setText("") //TODO better way to _really_ clear the internal char array of edittexts?
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
            activity?.run { setResult(Activity.RESULT_CANCELED); finish() }
        }

        override fun onDismiss(dialog: DialogInterface?) {
            super.onDismiss(dialog)
            hideKeyboard()
        }

        private fun hideKeyboard() {
            activity ?: return

            val inputManager = activity!!
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }

        override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
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

        const val EXTRA_RESULT_KEY_ID = "EXTRA_RESULT_KEY_ID"
        private const val FRAGMENT_TAG = "dialog"
        private const val EXTRA_KEYHASH_ID = "EXTRA_KEYHASH_ID"
        private const val EXTRA_KEYHASH_SALT = "EXTRA_KEYHASH_SALT"
        private const val EXTRA_KEYHASH_COST = "EXTRA_KEYHASH_COST"
        private const val EXTRA_ENCRYPTED_TEXT = "EXTRA_ENCRYPTED_TEXT"
        private const val EXTRA_SHOW_IGNORE = "EXTRA_SHOW_IGNORE"

        @SuppressLint("InlinedApi")
        fun buildPendingIntent(
            ctx: Context,
            expectedSessionKeyHashes: LongArray?,
            saltForSessionKeyHash: Array<ByteArray>,
            costForSessionKeyHash: Int,
            encryptedText: String?
        ): PendingIntent {
            val i = Intent()
            i.setClass(ctx, AddPasswordKeyActivity::class.java)
            if (expectedSessionKeyHashes != null) {
                val bundle = Bundle()
                bundle.putSerializable(EXTRA_KEYHASH_SALT, saltForSessionKeyHash)
                bundle.putLongArray(EXTRA_KEYHASH_ID, expectedSessionKeyHashes)
                bundle.putInt(EXTRA_KEYHASH_COST, costForSessionKeyHash)
                bundle.putString(EXTRA_ENCRYPTED_TEXT, encryptedText)
                bundle.putBoolean(EXTRA_SHOW_IGNORE, true)
                i.putExtras(bundle)

            }

            val flags = (PendingIntent.FLAG_ONE_SHOT
                    or PendingIntent.FLAG_CANCEL_CURRENT
                    or PendingIntent.FLAG_IMMUTABLE)

            return PendingIntent.getActivity(
                ctx, 0,
                i, flags
            )
        }

        fun showForResult(frag: Fragment, rq: Int) {

            val i = Intent()
            i.putExtra(EXTRA_SHOW_IGNORE, false)
            i.setClass(frag.activity, AddPasswordKeyActivity::class.java)
            frag.startActivityForResult(i, rq)


        }

        @Throws(
            NoSuchAlgorithmException::class,
            IOException::class,
            PasswordCantDecryptException::class
        )
        private fun addPasswordToCache__longoperation(
            pw: CharArray,
            ttl: Int,
            expectedKeyIdHashes: LongArray?,
            saltsForKeyHash: Array<ByteArray>?,
            costForKeyHash: Int,
            keyCache: KeyCache
        ): Long? {
            val keyName = pw[0] + "*".repeat(max(pw.size -2, 0)) + pw[pw.size - 1]

            val rawkey = KeyUtil.brcryptifyPassword(
                pw,
                SimpleSymmetricCryptoHandler.KEY_DERIVATION_SALT,
                SimpleSymmetricCryptoHandler.KEY_DERIVATION_COST,
                32
            )
            KeyUtil.erase(pw)

            val id = KeyUtil.calcKeyId(
                Arrays.copyOf(rawkey, rawkey.size),
                SimpleSymmetricCryptoHandler.KEY_ID_COST
            )

            if (expectedKeyIdHashes != null) {
                //only succeed if the key id matches the expected one
                var match = false
                for (i in expectedKeyIdHashes.indices) {
                    val hash = KeyUtil.calcSessionKeyId(id, saltsForKeyHash!![i], costForKeyHash)
                    if (hash == expectedKeyIdHashes[i]) {
                        match = true
                        break
                    }
                }
                if (!match) {
                    throw PasswordCantDecryptException()
                }
            }

            val key = SymmetricKeyPlain(id, keyName, Date(), rawkey, true)

            keyCache.doCacheKey(key, ttl.toLong())

            return id
        }
    }
}
