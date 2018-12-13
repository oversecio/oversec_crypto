package io.oversec.one.crypto.sym.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.text.InputType
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.dlazaro66.qrcodereaderview.QRCodeReaderView
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import io.oversec.one.crypto.Consts
import io.oversec.one.crypto.Help
import io.oversec.one.crypto.R
import io.oversec.one.crypto.sym.OversecKeystore2
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.sym.SymmetricKeyPlain
import io.oversec.one.crypto.symbase.KeyUtil
import io.oversec.one.crypto.symbase.OversecChacha20Poly1305
import io.oversec.one.crypto.ui.NewPasswordInputDialog
import io.oversec.one.crypto.ui.NewPasswordInputDialogCallback
import io.oversec.one.crypto.ui.SecureBaseActivity
import io.oversec.one.crypto.ui.util.*
import kotlinx.android.synthetic.main.sym_activity_createkey_random.*
import kotlinx.android.synthetic.main.sym_content_create_key_random.*

import java.io.IOException

class KeyImportCreateActivity : SecureBaseActivity(), QRCodeReaderView.OnQRCodeReadListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var mKeystore: OversecKeystore2
    private var mImportedString: String? = null
    private var mTempPbkdfInput: CharArray? = null
    private var mImportedKey: SymmetricKeyPlain? = null
    private var mTrustKey = false

    @SuppressLint("StringFormatInvalid", "RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        setContentView(R.layout.sym_activity_createkey_random)
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mKeystore = OversecKeystore2.getInstance(this)

        qrdecoderview.visibility = View.GONE

        fab.setOnClickListener { doSave() }

        val mTvCaptionAlias = findViewById<View>(R.id.caption_alias) as TextView

        mTvCaptionAlias.text = getString(
            R.string.createkey_pbe_alias_caption,
            "" + Consts.MIN_ALIAS_LENGTH
        )

        ivQr.visibility = View.GONE
        fab.visibility = View.GONE

        if (savedInstanceState != null) {
            mImportedKey = savedInstanceState.getSerializable(EXTRA_KEY) as SymmetricKeyPlain
            mTempPbkdfInput = savedInstanceState.getCharArray(EXTRA_PBKDF_INPUT)
            val title = savedInstanceState.getString(EXTRA_TITLE)
            if (title != null) {
                setTitleWTF(title)
            }
            //            mImportedString = savedInstanceState.getString(EXTRA_QR_STRING);
            //            if (mImportedString!=null) {
            //                handleImportQr(mImportedString);
            //            }
        }

        if (mImportedKey == null) {
            if (mTempPbkdfInput != null) { // we were in the middle of generating a key, start over
                createWithPassphrase(mTempPbkdfInput!!)
            } else {
                if (intent.type != null && intent.type!!.startsWith("image/")) {
                    //handle shared image
                    handleSendImage(intent)
                    setTitleWTF(getString(R.string.title_activity_generate_key_imported))
                } else if (CREATE_MODE_SCAN == intent.getStringExtra(EXTRA_MODE)) {
                    //DO SCAN
                    setTitleWTF(getString(R.string.title_activity_generate_key_scanning))
                    progress.visibility = View.GONE
                    progressLabel.visibility = View.GONE


                    // check Android 6 permission
                    if (Util.checkCameraAccess(this)) {
                        startBarcodeScan()
                    } else {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.CAMERA),
                            MY_PERMISSIONS_REQUEST_CAMERA
                        )
                    }

                } else if (CREATE_MODE_RANDOM == intent.getStringExtra(EXTRA_MODE)) {
                    //GENERATE RANDOM
                    setTitleWTF(getString(R.string.title_activity_generate_key))
                    createRandom()
                } else
                    if (CREATE_MODE_PASSPHRASE == intent.getStringExtra(EXTRA_MODE)) {
                        setTitleWTF(getString(R.string.title_activity_generate_key_passphrasebased))
                        createWithPassphrase()
                    } else {
                        //don't know what to do, better bail out
                    }
            }
        } else {
            setKeyData(mImportedKey!!)
        }


    }


    private fun createWithPassphrase() {
        val cb = object : NewPasswordInputDialogCallback {
            override fun positiveAction(pw: CharArray) {
                createWithPassphrase(pw)
            }

            override fun neutralAction() {
                finish()
            }
        }
        NewPasswordInputDialog.show(this, NewPasswordInputDialog.MODE.PBKDF, cb)
    }

    private fun createWithPassphrase(pw: CharArray) {
        mTempPbkdfInput = pw
        progressLabel.setText(R.string.progress_generating_key)
        mTrustKey = true //we trust our locally generated key by default

        @SuppressLint("StaticFieldLeak")
        val task = object : AsyncTask<Void, Void, SymmetricKeyPlain>() {

            override fun doInBackground(vararg params: Void): SymmetricKeyPlain {
                val salt = ByteArray(16) //constant zero bytes, yes, i know this is bad, but
                //this is meant so that people can exchange a key just by communicating a passphrase.

                val raw = KeyUtil.brcryptifyPassword(pw, salt, 32)
                KeyUtil.erase(pw)

                return SymmetricKeyPlain(raw)
            }

            override fun onPostExecute(key: SymmetricKeyPlain) {

                setKeyData(key)
                mTempPbkdfInput = null

            }

        }
        task.execute()
    }

    private fun createRandom() {
        progressLabel.setText(R.string.progress_generating_key)
        mTrustKey = true //we trust our locally generated key by default

        @SuppressLint("StaticFieldLeak")
        val task = object : AsyncTask<Void, Void, SymmetricKeyPlain>() {

            override fun doInBackground(vararg params: Void): SymmetricKeyPlain {
                try {
                    Thread.sleep(1500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                return KeyUtil.createNewKey()
            }

            override fun onPostExecute(key: SymmetricKeyPlain) {
                setKeyData(key)

            }

        }
        task.execute()
    }

    private fun setTitleWTF(title: String) {
        toolbar.title = title
        supportActionBar!!.title = title
        setTitle(title)
    }

    fun rotateABit(bmpOriginal: Bitmap?): Bitmap {
        val width: Int
        val height: Int
        height = bmpOriginal!!.height
        width = bmpOriginal.width
        val targetBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetBitmap)
        canvas.drawARGB(255, 255, 255, 255)
        val matrix = Matrix()
        matrix.setRotate(5f, (width / 2).toFloat(), (height / 2).toFloat())
        canvas.drawBitmap(bmpOriginal, matrix, Paint())
        return targetBitmap
    }

    private fun handleSendImage(intent: Intent) {

        Thread(Runnable {
            var imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (imageUri==null) {
                imageUri = intent.data
            }
            if (imageUri != null) {
                try {

                    val ii = ImgUtil.parseImageInfo(contentResolver.openInputStream(imageUri)!!)

                    val sampleSize = Math.max(1, ii.height * ii.width / (1024 * 1024))

                    val bitmap =
                        ImgUtil.loadImage(contentResolver.openInputStream(imageUri)!!, sampleSize)
                    requireNotNull(bitmap)
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    val bmDistorted =
                        rotateABit(bitmap)  //zxing has a problem with synthetic, perfect images, so distort it a bit ;-)

                    //TODO: try with orig image first, only then rotate
                    //TODO: control sample size, maybe downsample image first!

                    bmDistorted.getPixels(pixels, 0, width, 0, 0, width, height)

                    bitmap.recycle()
                    bmDistorted.recycle()

                    val source = RGBLuminanceSource(width, height, pixels)
                    val bBitmap = BinaryBitmap(HybridBinarizer(source))
                    val reader = MultiFormatReader()
                    try {
                        val result = reader.decode(bBitmap)
                        handleImportQR(result)
                    } catch (e: NotFoundException) {
                        qrdecoderview.stopCamera()
                        showError(
                            getString(R.string.importimage_nothing_found),
                            Runnable { finish() })

                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    if (Util.checkExternalStorageAccess(this@KeyImportCreateActivity, e)) {
                        ActivityCompat.requestPermissions(
                            this@KeyImportCreateActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                        )
                        return@Runnable
                    }

                    qrdecoderview.stopCamera()
                    showError(e.message!!, Runnable { finish() })
                }

            } else {
                showError("Error", Runnable { finish() })
            }
        }).start()


    }

    private fun startBarcodeScan() {
        qrdecoderview.visibility = View.VISIBLE
        qrdecoderview.setOnQRCodeReadListener(this)
        qrdecoderview.setAutofocusInterval(2000L)
        qrdecoderview.startCamera()
        //        IntentIntegrator ii = new IntentIntegrator(this);
        //        ii.setPrompt(getString(R.string.app_name));
        //        ii.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    // Called when a QR is decoded
    // "text" : the text encoded in QR
    // "points" : points where QR control points are placed
    override fun onQRCodeRead(text: String, points: Array<PointF>) {
        try {
            handleImportQr(text)
        } catch (e: OversecKeystore2.Base64DecodingException) {
            e.printStackTrace()
            qrdecoderview.stopCamera()
            showError(
                getString(R.string.error_invalid_barcode_content),
                Runnable { finish() }
            )
        }

    }


    override fun onPause() {
        super.onPause()
        qrdecoderview.stopCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED

                    && Util.checkCameraAccess(this)
                ) {
                    // permission was granted, and we surely can acces the camera
                    startBarcodeScan()
                } else {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                finish() //TODO implement retry
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setKeyData(key: SymmetricKeyPlain) {
        mImportedKey = key
        progress.visibility = View.GONE
        progressLabel!!.visibility = View.GONE
        fab.visibility = View.VISIBLE
        displayKeyData(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(EXTRA_KEY, mImportedKey)
        outState.putString(EXTRA_TITLE, title.toString())
        outState.putCharArray(EXTRA_PBKDF_INPUT, mTempPbkdfInput)
        //        outState.putString(EXTRA_QR_STRING,mImportedString);
    }

    private fun doSave() {
        //        mTvAliasWrapper.setError(getString(R.string.error_alias_length,
        //                Consts.MIN_ALIAS_LENGTH));

        var errorView: View? = null
        alias_wrapper.error = null

        val name = et_alias.text.toString().trim { it <= ' ' }
        if (name.length < Consts.MIN_ALIAS_LENGTH) {
            errorView = et_alias
            alias_wrapper.error = getString(
                R.string.error_alias_length,
                ""+Consts.MIN_ALIAS_LENGTH
            )
        } else {
            if (mKeystore.hasName(name)) {
                errorView = et_alias
                alias_wrapper.error = getString(R.string.error_alias_exists)
            }
        }


        errorView?.requestFocus() ?: NewPasswordInputDialog.show(
            this,
            NewPasswordInputDialog.MODE.KEYSTORE,
            object : NewPasswordInputDialogCallback {
                override fun positiveAction(pw: CharArray) {
                    saveKey(mImportedKey!!, et_alias.text.toString(), pw)
                }

                override fun neutralAction() {

                }
            })

    }

    private fun displayKeyData(blur: Boolean) {

        ivQr.visibility = View.GONE
        tvData.visibility = View.GONE
        if (mImportedKey != null) {
            progressLabel.visibility = View.GONE
            progress.visibility = View.GONE

            tvData.visibility = View.GONE
            ivQr.visibility = View.VISIBLE
            val dimension = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                240f,
                resources.displayMetrics
            ).toInt()

            var bm: Bitmap?
            if (blur) {
                bm = SymUtil.getQrCode(KeyUtil.getRandomBytes(32), dimension)
                val bmSmallTmp = Bitmap.createScaledBitmap(bm!!, 25, 25, true)
                bm = Bitmap.createScaledBitmap(bmSmallTmp, dimension, dimension, true)
            } else {
                bm = SymUtil.getQrCode(
                    OversecKeystore2.getPlainKeyAsTransferBytes(
                        mImportedKey!!.raw
                    ), dimension
                )
            }
            ivQr.setImageBitmap(bm)
        }
    }

    private fun saveKey(key: SymmetricKeyPlain, name: String, password: CharArray) {
        key.name = name

        val d = MaterialDialog.Builder(this)
            .title(R.string.progress_saving)
            .content(R.string.please_wait_encrypting)
            .progress(true, 0)
            .cancelable(false)
            .show()

        val t = Thread(Runnable {
            try {
                val keyId = mKeystore.addKey__longoperation(key, password)!!
                if (mTrustKey) {
                    mKeystore.confirmKey(keyId)
                }

                runOnUiThread {
                    d.dismiss()
                    val di = Intent()
                    di.putExtra(KeysFragment.EXTRA_KEY_ID, keyId)
                    setResult(Activity.RESULT_OK, di)
                    finish()
                }

            } catch (e: OversecKeystore2.AliasNotUniqueException) {

                d.dismiss()
                runOnUiThread {
                    showError(
                        getString(
                            R.string.common_error_body,
                            e.alias
                        ), null
                    )
                }


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
            }
        })

        t.start()


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_key_createimport, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.help) {
            when (intent.getStringExtra(EXTRA_MODE)) {
                CREATE_MODE_PASSPHRASE -> Help.open(this, Help.ANCHOR.symkey_create_pbkdf)
                CREATE_MODE_RANDOM -> Help.open(this, Help.ANCHOR.symkey_create_random)
                CREATE_MODE_SCAN -> Help.open(this, Help.ANCHOR.symkey_create_scan)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RQ_SHOW_DETAILS_AFTER_SAVE) {
            finish()
        }
    }

    private fun handleImportQR(result: Result) {
        runOnUiThread {
            if (BarcodeFormat.QR_CODE.toString() == result.barcodeFormat.toString()) {
                val s = result.text
                try {
                    handleImportQr(s)
                } catch (e: OversecKeystore2.Base64DecodingException) {
                    e.printStackTrace()
                    qrdecoderview.stopCamera()
                    showError(
                        getString(R.string.error_invalid_barcode_content), Runnable { finish() })
                }

            } else {
                showError(
                    getString(R.string.error_invalid_barcode_format), Runnable { finish() })
            }
        }

    }

    @Throws(OversecKeystore2.Base64DecodingException::class)
    private fun handleImportQr(s: String) {
        mImportedString = s

        val key = OversecKeystore2.getPlainKeyFromBase64Text(s)

        qrdecoderview.stopCamera()
        qrdecoderview.visibility = View.GONE

        if (key != null) {
            setKeyData(key)
        } else {
            val kk = OversecKeystore2.getEncryptedKeyFromBase64Text(s)
            if (kk != null) {

                MaterialDialog.Builder(this)
                    .title(R.string.import_decrypt_key_title)
                    .content(R.string.import_decrypt_key_content)
                    .cancelable(true)
                    .autoDismiss(true)
                    .negativeText(R.string.common_cancel)
                    .onNegative { dialog, which ->
                        dialog.dismiss()
                        finish()
                    }
                    .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .input(
                        R.string.keystore_password_hint,
                        R.string.prefill_password_fields
                    ) { dialog, input ->
                        try {
                            handleEncryptedImport(s, input.toString().toCharArray())
                        } catch (e: OversecKeystore2.Base64DecodingException) {
                            e.printStackTrace()
                            showError(
                                getString(R.string.error_invalid_barcode_content),
                                Runnable { finish() })
                        }
                    }.show()


            } else {
                showError(
                    getString(R.string.error_invalid_barcode_content),
                    Runnable { finish() }
                )
            }
        }
    }

    @Throws(OversecKeystore2.Base64DecodingException::class)
    private fun handleEncryptedImport(s: String, password: CharArray) {
        val encryptedKey = OversecKeystore2.getEncryptedKeyFromBase64Text(s)
        try {
            val plainKey = mKeystore.decryptSymmetricKey(encryptedKey!!, password)

            val existingKey = mKeystore.getSymmetricKeyEncrypted(encryptedKey.id)

            if (existingKey != null) {
                showError(
                    getString(R.string.error_key_exists, existingKey.name),
                    Runnable { finish() })
                return
            }

            setKeyData(plainKey)
            mImportedString = null

            MaterialDialog.Builder(this)
                .title(R.string.title_activity_generate_key_imported)
                .content(R.string.key_import_key_decrypted)
                .positiveText(R.string.common_ok)
                .show()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: OversecChacha20Poly1305.MacMismatchException) {
            showError(getString(R.string.error_password_wrong), Runnable {
                try {
                    handleImportQr(s)
                } catch (e1: OversecKeystore2.Base64DecodingException) {
                    e1.printStackTrace()
                }
            })
        }

    }

    companion object {
        const val RQ_SHOW_DETAILS_AFTER_SAVE = 1000

        const val MY_PERMISSIONS_REQUEST_CAMERA = 42
        const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 43

        private const val EXTRA_MODE = "mode"
        const val CREATE_MODE_RANDOM = "random"
        const val CREATE_MODE_SCAN = "scan"
        const val CREATE_MODE_PASSPHRASE = "pbkdf"
        private const val EXTRA_KEY = "EXTRA_KEY"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_PBKDF_INPUT = "EXTRA_PBKDF_INPUT"

        private fun showForResult(fragment: Fragment, rq: Int, mode: String) {
            val i = Intent()
            i.setClass(fragment.activity, KeyImportCreateActivity::class.java)
            i.putExtra(EXTRA_MODE, mode)
            fragment.startActivityForResult(i, rq)
        }


        fun showAddKeyDialog(fragment: Fragment, requestCode: Int) {

            val adapter = MaterialTitleBodyAdapter(fragment.activity)
            adapter.add(
                MaterialTitleBodyListItem.Builder(fragment.activity)
                    .title(R.string.action_createkey_random_title)
                    .body(R.string.action_createkey_random_body)
                    .icon(R.drawable.ic_vpn_key_black_24dp)
                    .backgroundColor(Color.WHITE)
                    .build()
            )
            adapter.add(
                MaterialTitleBodyListItem.Builder(fragment.activity)
                    .title(R.string.action_createkey_pw_title)
                    .body(R.string.action_createkey_pw_body)
                    .icon(R.drawable.ic_font_download_black_24dp)
                    .backgroundColor(Color.WHITE)
                    .build()
            )
            adapter.add(
                MaterialTitleBodyListItem.Builder(fragment.activity)
                    .title(R.string.action_addkey_importqr_title)
                    .body(R.string.action_addkey_importqr_body)
                    .icon(R.drawable.ic_memory_black_24dp)
                    //.iconPaddingDp(8)
                    .build()
            )

            MaterialDialog.Builder(fragment.activity)
                .title(R.string.title_dialog_add_key)
                .adapter(adapter) { dialog, itemView, which, text ->
                    dialog.dismiss()
                    when (which) {
                        0 // create randomekey
                        -> showForResult(
                            fragment,
                            requestCode,
                            KeyImportCreateActivity.CREATE_MODE_RANDOM
                        )
                        1 // create randomekey
                        -> showForResult(
                            fragment,
                            requestCode,
                            KeyImportCreateActivity.CREATE_MODE_PASSPHRASE
                        )
                        2 // import QR
                        -> showForResult(
                            fragment,
                            requestCode,
                            KeyImportCreateActivity.CREATE_MODE_SCAN
                        )
                    }
                }
                .show()
        }
    }
}
