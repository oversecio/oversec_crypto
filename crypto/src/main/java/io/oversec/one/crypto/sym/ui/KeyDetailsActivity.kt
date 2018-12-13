package io.oversec.one.crypto.sym.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import io.oversec.one.common.MainPreferences
import io.oversec.one.crypto.Help
import io.oversec.one.crypto.R
import io.oversec.one.crypto.TemporaryContentProvider
import io.oversec.one.crypto.sym.*
import io.oversec.one.crypto.symbase.KeyUtil
import io.oversec.one.crypto.symbase.OversecKeyCacheListener
import io.oversec.one.crypto.ui.NewPasswordInputDialog
import io.oversec.one.crypto.ui.NewPasswordInputDialogCallback
import io.oversec.one.crypto.ui.SecureBaseActivity
import io.oversec.one.crypto.ui.util.Util
import kotlinx.android.synthetic.main.sym_activity_key_details.*
import kotlinx.android.synthetic.main.sym_content_key_details.*
import roboguice.util.Ln
import java.text.SimpleDateFormat

class KeyDetailsActivity : SecureBaseActivity(), OversecKeyCacheListener {

    private lateinit var mKeystore:OversecKeystore2
    private var mId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mKeystore = OversecKeystore2.getInstance(this)

        if (!MainPreferences.isAllowScreenshots(this)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        mId = intent.getLongExtra(EXTRA_ID, 0)
        val key = mKeystore.getSymmetricKeyEncrypted(mId)
        if (key == null) {
            Ln.w("couldn't find request key with id %s", mId)
            finish()
            return
        }

        setContentView(R.layout.sym_activity_key_details)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        btRevealQr.setOnClickListener { showPlainKeyQR() }

        tv_alias.text = key.name
        tv_hash.text = SymUtil.longToPrettyHex(key.id)

        val createdDate = key.createdDate
        tv_date.text = SimpleDateFormat.getDateTimeInstance().format(
            createdDate
        )

        setKeyImage(true)
        fab.setOnClickListener { showConfirmDialog() }

        refreshConfirm()

        SymUtil.applyAvatar(tvAvatar, key.name!!)
        mKeystore.addKeyCacheListener(this)
    }

    private fun showPlainKeyQR() {
        val ok = setKeyImage(false)
        if (!ok) {
            UnlockKeyActivity.showForResult(this, mId, RQ_UNLOCK)
        } else {
            btRevealQr.visibility = View.GONE
        }
    }


    private fun setKeyImage(blur: Boolean): Boolean {
        val dimension =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240f, resources.displayMetrics)
                .toInt()
        try {
            var bm: Bitmap?
            if (blur) {
                bm = SymUtil.getQrCode(KeyUtil.getRandomBytes(32), dimension)
                val bmSmallTmp = Bitmap.createScaledBitmap(bm!!, 25, 25, true)
                bm = Bitmap.createScaledBitmap(bmSmallTmp, dimension, dimension, true)
            } else {
                bm = SymUtil.getQrCode(mKeystore.getPlainKeyAsTransferBytes(mId), dimension)
            }
            ivQr.setImageBitmap(bm)
            return true
        } catch (ex: KeyNotCachedException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return false
    }


    @SuppressLint("RestrictedApi")
    private fun refreshConfirm() {
        val confirmDate = mKeystore.getConfirmDate(mId)
        fab.visibility = if (confirmDate == null) View.VISIBLE else View.GONE
        tv_confirmed.text = if (confirmDate == null)
            getString(R.string.label_key_unconfirmed)
        else
            SimpleDateFormat.getDateTimeInstance().format(
                confirmDate
            )
        ivConfirmed.visibility = if (confirmDate == null) View.GONE else View.VISIBLE
        ivUnConfirmed.visibility = if (confirmDate == null) View.VISIBLE else View.GONE
    }

    private fun showConfirmDialog() {
        val fp = mId

        //TODO: make custom dialog. highlight the fingerprint, monospace typeface
        MaterialDialog.Builder(this)
            .title(R.string.app_name)
            .iconRes(R.drawable.ic_warning_black_24dp)
            .cancelable(true)
            .content(getString(R.string.dialog_confirm_key_text, SymUtil.longToPrettyHex(fp)))
            .positiveText(R.string.common_ok)
            .onPositive { dialog, which ->
                try {
                    mKeystore.confirmKey(mId)

                    refreshConfirm()
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError(getString(R.string.common_error_body, e.message), null)
                }
            }
            .negativeText(R.string.common_cancel)
            .onNegative { dialog, which -> dialog.dismiss() }
            .show()
    }


    private fun showDeleteDialog() {
        MaterialDialog.Builder(this)
            .title(R.string.app_name)
            .iconRes(R.drawable.ic_warning_black_24dp)
            .cancelable(true)
            .content(getString(R.string.action_delete_key_confirm))
            .positiveText(R.string.common_ok)
            .onPositive { dialog, which ->
                try {
                    mKeystore.deleteKey(mId)

                    setResult(Activity.RESULT_FIRST_USER)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError(getString(R.string.common_error_body, e.message), null)
                }
            }
            .negativeText(R.string.common_cancel)
            .onNegative { dialog, which -> dialog.dismiss() }
            .show()


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_key_details, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> finish()
            R.id.action_delete_key -> {
                showDeleteDialog()
                return true
            }
            R.id.action_send_encrypted -> {
                share(RQ_SEND_ENCRYPTED)
                return true
            }
            R.id.action_view_encrypted -> {
                share(RQ_VIEW_ENCRYPTED)
                return true
            }
            R.id.help -> {
                Help.open(this, Help.ANCHOR.symkey_details)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }


    private fun share(rq: Int) {
        try {
            val plainKey = mKeystore.getPlainKey(mId)
            share(plainKey, rq)
        } catch (e: KeyNotCachedException) {
            try {
                startIntentSenderForResult(e.pendingIntent.intentSender, rq, null, 0, 0, 0)
            } catch (e1: IntentSender.SendIntentException) {
                e1.printStackTrace()
            }
        }
    }

    private fun share(plainKey: SymmetricKeyPlain, rq: Int) {
        val cb = object : NewPasswordInputDialogCallback {
            override fun positiveAction(pw: CharArray) {
                share(pw, plainKey, rq)
            }

            override fun neutralAction() {

            }
        }
        NewPasswordInputDialog.show(this, NewPasswordInputDialog.MODE.SHARE, cb)
    }

    private fun share(pw: CharArray, plainKey: SymmetricKeyPlain, rq: Int) {
        val d = MaterialDialog.Builder(this)
            .title(R.string.progress_encrypting)
            .content(R.string.please_wait_encrypting)
            .progress(true, 0)
            .cancelable(false)
            .show()

        val t = Thread(Runnable {
            try {
                val encKey = mKeystore.encryptSymmetricKey(plainKey, pw)

                d.dismiss()
                runOnUiThread { share(encKey, rq) }

            } catch (e: Exception) {
                e.printStackTrace()
                d.dismiss()
                runOnUiThread {
                    showError(
                        getString(
                            R.string.common_error_body,
                            e.message
                        ), null
                    )
                }
            } finally {
                KeyUtil.erase(pw)
            }
        })

        t.start()
    }


    private fun share(encKey: SymmetricKeyEncrypted, rq: Int) {
        try {
            val uri = TemporaryContentProvider.prepare(
                this,
                "image/png",
                TemporaryContentProvider.TTL_1_HOUR,
                null
            )
            val bm = SymUtil.getQrCode(
                OversecKeystore2.getEncryptedKeyAsTransferBytes(encKey),
                KEYSHARE_BITMAP_WIDTH_PX
            )
            val os = contentResolver.openOutputStream(uri)
                ?: //damnit
                return
            bm!!.compress(Bitmap.CompressFormat.PNG, 100, os)
            os.close()


            val intent = Intent()
            var cid = 0


            if (rq == RQ_SEND_ENCRYPTED) {
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.type = "image/png"
                cid = R.string.intent_chooser_send_encryptedkey
            } else if (rq == RQ_VIEW_ENCRYPTED) {
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "image/png")
                cid = R.string.intent_chooser_view_encryptedkey
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            Util.share(this, intent, null, getString(cid), true, null, false)

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (RQ_UNLOCK == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                setKeyImage(false)
                btRevealQr.visibility = View.GONE
            }
        } else if (RQ_SEND_ENCRYPTED == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                share(RQ_SEND_ENCRYPTED)
            }
        } else if (RQ_VIEW_ENCRYPTED == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                share(RQ_VIEW_ENCRYPTED)
            }
        }


    }

    override fun onDestroy() {
        mKeystore.removeKeyCacheListener(this)
        super.onDestroy()
    }


    override fun onFinishedCachingKey(keyId: Long) {
        if (mId == keyId) {
            finish()
        }
    }

    override fun onStartedCachingKey(keyId: Long) {

    }

    companion object {

        private const val EXTRA_ID = "id"
        private const val RQ_UNLOCK = 1008
        private const val RQ_SEND_ENCRYPTED = 1009
        private const val RQ_VIEW_ENCRYPTED = 1010
        private const val KEYSHARE_BITMAP_WIDTH_PX = 480

        fun show(ctx: Context, keyId: Long?) {
            val i = Intent()
            i.setClass(ctx, KeyDetailsActivity::class.java)
            if (ctx !is Activity) {
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            i.putExtra(EXTRA_ID, keyId)
            ctx.startActivity(i)
        }

        fun showForResult(f: Fragment, rq: Int, id: Long) {
            val i = Intent()
            i.setClass(f.activity, KeyDetailsActivity::class.java)
            i.putExtra(EXTRA_ID, id)
            f.startActivityForResult(i, rq)
        }
    }
}
