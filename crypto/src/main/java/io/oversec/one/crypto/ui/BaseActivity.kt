package io.oversec.one.crypto.ui

import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import io.oversec.one.crypto.R

abstract class BaseActivity : AppCompatActivity() {
    protected fun showError(msg: String, runnable: Runnable?) {
        runOnUiThread {
            MaterialDialog.Builder(this@BaseActivity)
                .title(R.string.common_error_title)
                .iconRes(R.drawable.ic_error_black_24dp)
                .cancelable(true)
                .content(msg)
                .neutralText(R.string.common_ok)
                .cancelListener { dialog ->
                    dialog.dismiss()
                    runnable?.run()
                }
                .onNeutral { dialog, which ->
                    dialog.dismiss()
                    runnable?.run()
                }
                .show()
        }
    }
}
