package io.oversec.one.crypto.ui

import android.os.Bundle
import android.view.WindowManager
import io.oversec.one.common.MainPreferences

abstract class SecureBaseActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MainPreferences.isAllowScreenshots(this)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}
