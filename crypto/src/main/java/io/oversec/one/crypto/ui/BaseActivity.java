package io.oversec.one.crypto.ui;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import io.oversec.one.crypto.R;

public abstract class BaseActivity extends AppCompatActivity {
    protected void showError(final String msg, final Runnable runnable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new MaterialDialog.Builder(BaseActivity.this)
                        .title(R.string.common_error_title)
                        .iconRes(R.drawable.ic_error_black_24dp)
                        .cancelable(true)
                        .content(msg)
                        .neutralText(R.string.common_ok)
                        .cancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                                if (runnable != null) {
                                    runnable.run();
                                }
                            }
                        })
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                                if (runnable != null) {
                                    runnable.run();
                                }
                            }
                        })
                        .show();
            }
        });

    }
}
