package io.oversec.one.crypto.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;
import io.oversec.one.common.MainPreferences;

public abstract class SecureBaseActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MainPreferences.isAllowScreenshots(this)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
