package io.oversec.one.crypto.sym.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import io.oversec.one.common.MainPreferences;
import io.oversec.one.crypto.Help;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.TemporaryContentProvider;
import io.oversec.one.crypto.sym.*;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symbase.OversecKeyCacheListener;
import io.oversec.one.crypto.ui.NewPasswordInputDialog;
import io.oversec.one.crypto.ui.NewPasswordInputDialogCallback;
import io.oversec.one.crypto.ui.SecureBaseActivity;
import io.oversec.one.crypto.ui.util.Util;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KeyDetailsActivity extends SecureBaseActivity implements OversecKeyCacheListener {

    private static final String EXTRA_ID = "id";
    private static final int RQ_UNLOCK = 1008;
    private static final int RQ_SEND_ENCRYPTED = 1009;
    private static final int RQ_VIEW_ENCRYPTED = 1010;
    private static final int KEYSHARE_BITMAP_WIDTH_PX = 480;


    private Button mBtRevealQr;
    private OversecKeystore2 mKeystore;


    public static void show(Context ctx, Long keyId) {
        Intent i = new Intent();
        i.setClass(ctx, KeyDetailsActivity.class);
        if (!(ctx instanceof Activity)) {
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        i.putExtra(EXTRA_ID, keyId);
        ctx.startActivity(i);
    }

    public static void showForResult(Fragment f, int rq, long id) {
        Intent i = new Intent();
        i.setClass(f.getActivity(), KeyDetailsActivity.class);
        i.putExtra(EXTRA_ID, id);
        f.startActivityForResult(i, rq);
    }

    private ImageView mIvQr;
    private long mId;
    private FloatingActionButton btConfirm;
    private TextView mTvConfirmed;
    private ImageView mIvConfirmed;
    private ImageView mIvUnConfirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeystore = OversecKeystore2.getInstance(this);

        if (!MainPreferences.isAllowScreenshots(this)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }

        mId = getIntent().getLongExtra(EXTRA_ID, 0);
        setContentView(R.layout.sym_activity_key_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView mTvAlias = (TextView) findViewById(R.id.tv_alias);

        TextView mTvAvatar = (TextView) findViewById(R.id.tvAvatar);

        TextView mTvDate = (TextView) findViewById(R.id.tv_date);
        mTvConfirmed = (TextView) findViewById(R.id.tv_confirmed);
        TextView mTvHash = (TextView) findViewById(R.id.tv_hash);


        mIvConfirmed = (ImageView) findViewById(R.id.ivConfirmed);
        mIvUnConfirmed = (ImageView) findViewById(R.id.ivUnConfirmed);


        mIvQr = (ImageView) findViewById(R.id.ivQr);
        mBtRevealQr = (Button) findViewById(R.id.btRevealQr);
        mBtRevealQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlainKeyQR();
            }
        });


        SymmetricKeyEncrypted key = mKeystore.getSymmetricKeyEncrypted(mId);

        //noinspection ConstantConditions
        mTvAlias.setText(key.getName());
        //noinspection ConstantConditions
        mTvHash.setText(SymUtil.longToPrettyHex(key.getId()));

        Date createdDate = key.getCreatedDate();
        mTvDate.setText(SimpleDateFormat.getDateTimeInstance().format(
                createdDate));

        setKeyImage(true);

        btConfirm = (FloatingActionButton) findViewById(R.id.fab);

        btConfirm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showConfirmDialog();
            }

        });

        refreshConfirm();

        SymUtil.applyAvatar(mTvAvatar, key.getName());


        mKeystore.addKeyCacheListener(this);
    }

    private void showPlainKeyQR() {
        boolean ok = setKeyImage(false);
        if (!ok) {
            UnlockKeyActivity.showForResult(this, mId, RQ_UNLOCK);
        } else {
            mBtRevealQr.setVisibility(View.GONE);
        }
    }


    private boolean setKeyImage(boolean blur) {
        int dimension = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, getResources().getDisplayMetrics());
        try {
            Bitmap bm;
            if (blur) {
                bm = SymUtil.getQrCode(KeyUtil.getRandomBytes(32), dimension);
                Bitmap bmSmallTmp = Bitmap.createScaledBitmap(bm, 25, 25, true);
                bm = Bitmap.createScaledBitmap(bmSmallTmp, dimension, dimension, true);
            } else {
                bm = SymUtil.getQrCode(mKeystore.getPlainKeyAsTransferBytes(mId), dimension);
            }
            mIvQr.setImageBitmap(bm);
            return true;
        } catch (KeyNotCachedException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


    private void refreshConfirm() {
        Date confirmDate = mKeystore.getConfirmDate(mId);
        btConfirm.setVisibility(confirmDate == null ? View.VISIBLE : View.GONE);
        mTvConfirmed.setText(
                confirmDate == null ? getString(R.string.label_key_unconfirmed) :
                        SimpleDateFormat.getDateTimeInstance().format(
                                confirmDate));
        mIvConfirmed.setVisibility(confirmDate == null ? View.GONE : View.VISIBLE);
        mIvUnConfirmed.setVisibility(confirmDate == null ? View.VISIBLE : View.GONE);
    }

    private void showConfirmDialog() {
        long fp = mId;

        //TODO: make custom dialog. highlight the fingerprint, monospace typeface
        new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .iconRes(R.drawable.ic_warning_black_24dp)
                .cancelable(true)
                .content(getString(R.string.dialog_confirm_key_text, SymUtil.longToPrettyHex(fp)))
                .positiveText(R.string.common_ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            mKeystore.confirmKey(mId);

                            refreshConfirm();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showError(getString(R.string.common_error_body, e.getMessage()), null);
                        }
                    }
                })
                .negativeText(R.string.common_cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void showDeleteDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.app_name)
                .iconRes(R.drawable.ic_warning_black_24dp)
                .cancelable(true)
                .content(getString(R.string.action_delete_key_confirm))
                .positiveText(R.string.common_ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            mKeystore.deleteKey(mId);

                            setResult(RESULT_FIRST_USER);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            showError(getString(R.string.common_error_body, e.getMessage()), null);
                        }
                    }
                })
                .negativeText(R.string.common_cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_key_details, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.action_delete_key) {
            showDeleteDialog();
            return true;
        } else if (id == R.id.action_send_encrypted) {
            share(RQ_SEND_ENCRYPTED);
            return true;
        } else if (id == R.id.action_view_encrypted) {
            share(RQ_VIEW_ENCRYPTED);
            return true;
        } else if (id == R.id.help) {
            Help.open(this, Help.ANCHOR.symkey_details);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void share(int rq) {
        try {
            SymmetricKeyPlain plainKey = mKeystore.getPlainKey(mId);
            share(plainKey, rq);
        } catch (KeyNotCachedException e) {
            try {
                startIntentSenderForResult(e.getPendingIntent().getIntentSender(), rq, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void share(final SymmetricKeyPlain plainKey, final int rq) {
        NewPasswordInputDialogCallback cb = new NewPasswordInputDialogCallback() {
            @Override
            public void positiveAction(char[] pw) {
                share(pw, plainKey, rq);
            }

            @Override
            public void neutralAction() {

            }
        };

        NewPasswordInputDialog.show(this, NewPasswordInputDialog.MODE.SHARE, cb);
    }

    private void share(final char[] pw, final SymmetricKeyPlain plainKey, final int rq) {
        final Dialog d = new MaterialDialog.Builder(this)
                .title(R.string.progress_encrypting)
                .content(R.string.please_wait_encrypting)
                .progress(true, 0)
                .cancelable(false)
                .show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {


                    final SymmetricKeyEncrypted encKey = mKeystore.encryptSymmetricKey(plainKey, pw);


                    d.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            share(encKey, rq);
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace();
                    d.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showError(
                                    getString(R.string.common_error_body,
                                            e.getMessage()), null);
                        }
                    });
                } finally {
                    KeyUtil.erase(pw);
                }
            }
        });

        t.start();
    }


    private void share(final SymmetricKeyEncrypted encKey, final int rq) {
        try {
            Uri uri = TemporaryContentProvider.prepare(this, "image/png", TemporaryContentProvider.TTL_1_HOUR, null);
            Bitmap bm = SymUtil.getQrCode(OversecKeystore2.getEncryptedKeyAsTransferBytes(encKey), KEYSHARE_BITMAP_WIDTH_PX);
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os==null) {
                //damnit
                return;
            }
            bm.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();


            final Intent intent = new Intent();
            int cid = 0;


            if (rq == RQ_SEND_ENCRYPTED) {
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setType("image/png");
                cid = R.string.intent_chooser_send_encryptedkey;
            } else if (rq == RQ_VIEW_ENCRYPTED) {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/png");
                cid = R.string.intent_chooser_view_encryptedkey;
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Util.share(this, intent, null, getString(cid), true, null, false);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RQ_UNLOCK == requestCode) {
            if (resultCode == RESULT_OK) {
                setKeyImage(false);
                mBtRevealQr.setVisibility(View.GONE);
            }
        } else if (RQ_SEND_ENCRYPTED == requestCode) {
            if (resultCode == RESULT_OK) {
                share(RQ_SEND_ENCRYPTED);
            }
        } else if (RQ_VIEW_ENCRYPTED == requestCode) {
            if (resultCode == RESULT_OK) {
                share(RQ_VIEW_ENCRYPTED);
            }
        }


    }

    @Override
    protected void onDestroy() {
        mKeystore.removeKeyCacheListener(this);
        super.onDestroy();
    }


    @Override
    public void onFinishedCachingKey(long keyId) {
        if (mId == keyId) {
            finish();
        }
    }

    @Override
    public void onStartedCachingKey(long keyId) {

    }
}
