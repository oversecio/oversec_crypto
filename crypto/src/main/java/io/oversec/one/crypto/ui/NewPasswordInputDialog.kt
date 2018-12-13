package io.oversec.one.crypto.ui

import android.content.*
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.IBinder
import android.os.RemoteException
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import io.oversec.one.crypto.IZxcvbnService
import io.oversec.one.crypto.R
import io.oversec.one.crypto.ui.util.EditTextPasswordWithVisibilityToggle
import uk.co.biddell.diceware.dictionaries.DiceWare
import java.io.IOException

object NewPasswordInputDialog {
    private const val ENTROPY_MEDIUM = 45
    private const val ENTROPY_HIGH_SHARE = 75
    private const val ENTROPY_HIGH_PBKDF = 75
    private const val ENTROPY_HIGH_DEVICE = 60

    private const val DICEWARE_WORDS_KEYSTORE = 4
    private const val DICEWARE_WORDS_SHARE = 5
    private const val DICEWARE_WORDS_PBKDF = 6

    enum class MODE {
        SHARE, KEYSTORE, PBKDF
    }

    fun show(ctx: Context, mode: MODE, callback: NewPasswordInputDialogCallback) {
        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    ctx,
                    "io.oversec.one.crypto.ZxcvbnService"
                )
            ) //do NOT reference by CLASS
        ctx.startService(serviceIntent)
        val mZxcvbnService = arrayOfNulls<IZxcvbnService>(1)
        val mConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mZxcvbnService[0] = IZxcvbnService.Stub.asInterface(service)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mZxcvbnService[0] = null
            }
        }
        ctx.bindService(serviceIntent, mConnection, Context.BIND_ALLOW_OOM_MANAGEMENT)

        val dialog = MaterialDialog.Builder(ctx)
            .customView(R.layout.new_password_input_dialog, false)
            .positiveText(getPositiveText(mode))
            .neutralText(R.string.common_cancel)
            .autoDismiss(false)
            .dismissListener {
                try {
                    mZxcvbnService[0]?.exit()
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }

                ctx.unbindService(mConnection)
            }
            .cancelListener { dialog ->
                callback.neutralAction()
                //TODO: clear passwords on cancel?
                dialog.dismiss()
            }
            .onPositive { dialog, which ->
                val view = dialog.customView
                if (handlePositive(view!!, callback, mode, mZxcvbnService[0])) {
                    dialog.dismiss()
                }
            }
            .onNeutral { dialog, which ->
                val view = dialog.customView
                val etPw1 = view!!.findViewById<View>(R.id.new_password_password) as EditText
                val etPw2 = view.findViewById<View>(R.id.new_password_password_again) as EditText
                etPw1.setText("") //TODO better way?
                etPw2.setText("") //TODO better way?
                callback.neutralAction()
                dialog.dismiss()
            }.build()

        dialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val view = dialog.customView

        val etPw1 =
            view!!.findViewById<View>(R.id.new_password_password) as EditTextPasswordWithVisibilityToggle
        val etPw2 =
            view.findViewById<View>(R.id.new_password_password_again) as EditTextPasswordWithVisibilityToggle

        etPw2.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (handlePositive(view, callback, mode, mZxcvbnService[0])) {
                    dialog.dismiss()
                    return@OnEditorActionListener true
                }
            }
            false
        })

        val wrapPw1 = view.findViewById<View>(R.id.new_password_password_wrapper) as TextInputLayout

        val tvText = view.findViewById<View>(R.id.new_password_text) as TextView
        tvText.setText(getBody(mode))

        val tvTitle = view.findViewById<View>(R.id.new_password_title) as TextView
        tvTitle.setText(getTitle(mode))

        val cbWeak = view.findViewById<View>(R.id.cb_accept_weak_password) as CheckBox
        cbWeak.visibility = View.GONE

        val btSuggest = view.findViewById<View>(R.id.new_password_generate) as Button
        btSuggest.setOnClickListener {
            //  cbShowPassphrase.setChecked(true);
            try {
                val dw = DiceWare(ctx).getDiceWords(
                    getDicewareExtraSecurity(mode)!!,
                    getDicewareNumWords(mode)
                )

                etPw1.setText(dw.toString())
                etPw1.setPasswordVisible(true)
                etPw2.setPasswordVisible(true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val sbStrength = view.findViewById<View>(R.id.create_key_seekbar) as SeekBar
        etPw1.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val entropy = calcPasswordEntropy(s, wrapPw1, mZxcvbnService[0])
                updateSeekBar(view, sbStrength, entropy, mode)
            }

            override fun afterTextChanged(s: Editable) {
            }
        })

        updateSeekBar(view, sbStrength, 0, mode)
        dialog.show()
    }


    private fun getBody(mode: MODE): Int {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> R.string.new_password_keystore_text
            NewPasswordInputDialog.MODE.PBKDF -> R.string.new_password_pbkdf_text
            NewPasswordInputDialog.MODE.SHARE -> R.string.new_password_share_text
        }
    }

    private fun getTitle(mode: MODE): Int {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> R.string.new_password_keystore_title
            NewPasswordInputDialog.MODE.PBKDF -> R.string.new_password_pbkdf_title
            NewPasswordInputDialog.MODE.SHARE -> R.string.new_password_share_title
        }
    }

    private fun getDicewareExtraSecurity(mode: MODE): DiceWare.Type? {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> DiceWare.Type.PASSPHRASE
            NewPasswordInputDialog.MODE.PBKDF -> DiceWare.Type.PASSPHRASE_EXTRA_SECURITY
            NewPasswordInputDialog.MODE.SHARE -> DiceWare.Type.PASSPHRASE_EXTRA_SECURITY
        }
    }

    private fun getDicewareNumWords(mode: MODE): Int {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> DICEWARE_WORDS_KEYSTORE
            NewPasswordInputDialog.MODE.PBKDF -> DICEWARE_WORDS_PBKDF
            NewPasswordInputDialog.MODE.SHARE -> DICEWARE_WORDS_SHARE
        }
    }

    private fun getPositiveText(mode: MODE): Int {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> R.string.action_save
            NewPasswordInputDialog.MODE.PBKDF -> R.string.action_generate
            NewPasswordInputDialog.MODE.SHARE -> R.string.action_share
        }
    }


    private fun getEntropyHighLevel(mode: MODE): Int {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> ENTROPY_HIGH_DEVICE
            NewPasswordInputDialog.MODE.PBKDF -> ENTROPY_HIGH_PBKDF
            NewPasswordInputDialog.MODE.SHARE -> ENTROPY_HIGH_SHARE
        }
    }

    private fun getEntropyMinimum(mode: MODE): Int {
        return when (mode) {
            NewPasswordInputDialog.MODE.KEYSTORE -> ENTROPY_MEDIUM //facilitate usage fur "dumb" users
            NewPasswordInputDialog.MODE.PBKDF -> ENTROPY_MEDIUM //facilitate usage fur "dumb" users
            NewPasswordInputDialog.MODE.SHARE -> ENTROPY_HIGH_SHARE
        }
    }

    private fun updateSeekBar(view: View?, sbStrength: SeekBar, entropy: Int, mode: MODE) {

        sbStrength.max = 100
        sbStrength.progress = Math.max(10, entropy)
        var color = R.color.password_strength_low
        if (entropy >= ENTROPY_MEDIUM) {
            color = R.color.password_strength_medium
        }


        if (entropy >= getEntropyHighLevel(mode)) {
            color = R.color.password_strength_high
            val cbWeak = view!!.findViewById<View>(R.id.cb_accept_weak_password) as CheckBox
            cbWeak.visibility = View.GONE
        }
        sbStrength.progressDrawable.colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(sbStrength.context, color),
            PorterDuff.Mode.MULTIPLY
        )


    }


    private fun calcPasswordEntropy(
        s: CharSequence,
        wrapper: TextInputLayout,
        zxcvbn: IZxcvbnService?
    ): Int {
        zxcvbn?: return 0  //service not bound?
        return try {
            val r = zxcvbn.calcEntropy(s.toString())
            wrapper.error = r.warning
            r.entropy
        } catch (ex: RemoteException) {
            ex.printStackTrace()
            0
        }

    }


    private fun handlePositive(
        view: View,
        callback: NewPasswordInputDialogCallback,
        mode: MODE,
        zxcvbn: IZxcvbnService?
    ): Boolean {
        val etPw1 = view.findViewById<View>(R.id.new_password_password) as EditText
        val etPw2 = view.findViewById<View>(R.id.new_password_password_again) as EditText
        val wrapPw1 = view.findViewById<View>(R.id.new_password_password_wrapper) as TextInputLayout
        val wrapPw2 =
            view.findViewById<View>(R.id.new_password_password_again_wrapper) as TextInputLayout
        val cbWeak = view.findViewById<View>(R.id.cb_accept_weak_password) as CheckBox
        val editablePw1 = etPw1.text
        val editablePw2 = etPw2.text

        if (editablePw1.toString() != editablePw2.toString()) {
            wrapPw1.error = view.context.getString(R.string.error_passwords_dont_match)
            return false
        }


        val entropy = calcPasswordEntropy(etPw1.text, wrapPw1, zxcvbn)

        if (entropy < getEntropyMinimum(mode) && !cbWeak.isChecked) {
            wrapPw1.error = view.context.getString(R.string.error_password_length)

            cbWeak.visibility = View.VISIBLE
            cbWeak.requestFocus()
            cbWeak.parent.requestChildFocus(cbWeak, cbWeak)
            return false
        }


        val pl = editablePw1.length
        val aPassPhrase = CharArray(pl)
        editablePw1.getChars(0, pl, aPassPhrase, 0)

        etPw1.setText("") //TODO better way?
        etPw2.setText("") //TODO better way?

        callback.positiveAction(aPassPhrase)


        return true
    }


}
