package io.oversec.one.crypto.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import io.oversec.one.crypto.*;
import roboguice.util.Ln;

public class EncryptionInfoActivity extends AppCompatActivity {
    private static final String EXTRA_ENCRYPTED_TEXT = "enc_text";
    private static final String EXTRA_PACKAGENAME = "packagename";

    public static final int REQUEST_CODE_DECRYPT = 5001;
    public static final int REQUEST_CODE_DOWNLOAD_MISSING_KEYS = 5002;
    public static final int REQUEST_CODE_SHOW_SIGNATURE_KEY = 5003;


    private BaseDecryptResult mTdr;
    private String mOrigText;

    private AbstractTextEncryptionInfoFragment mFragment;
    private AbstractCryptoHandler mEncryptionHandler;

    public static void show(Context ctx, String packagename, String encryptedText, View source) {
        Intent i = new Intent();
        i.setClass(ctx, EncryptionInfoActivity.class);
        i.putExtra(EXTRA_ENCRYPTED_TEXT, encryptedText);
        i.putExtra(EXTRA_PACKAGENAME, packagename);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        ActivityOptions opts = null;

        if (source != null) {
            opts = ActivityOptions.makeScaleUpAnimation(source, 0, 0, 0, 0);
        }

        if (opts != null) {
            ctx.startActivity(i, opts.toBundle());
        } else {
            ctx.startActivity(i);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //make sure to clear any pending decrypt tasks in the background, as they might interfere with pending intents generated by the user in the UI thread of this activity
        CryptoHandlerFacade.getInstance(this).clearDecryptQueue();


        setContentView(R.layout.activity_encryption_info);

        mOrigText = getIntent().getStringExtra(EXTRA_ENCRYPTED_TEXT);
        String mPackageName = getIntent().getStringExtra(EXTRA_PACKAGENAME);


        mEncryptionHandler = CryptoHandlerFacade.getInstance(this).getCryptoHandler(mOrigText);
        if (mEncryptionHandler != null) {
            mFragment = mEncryptionHandler.getTextEncryptionInfoFragment(mPackageName);
        }


        if (mFragment == null) {
            finish();
        } else {

            mFragment.setArgs(mPackageName);

            android.app.FragmentManager manager = getFragmentManager();
            android.app.FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.encryptionInfoFragment_container, mFragment, "Foo");

            transaction.commit();


            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            //noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        }

    }

    @Override
    protected void onResumeFragments() {
        update(null);
        super.onResumeFragments();
    }

    public void update(final Intent actionIntent) {

        UserInteractionRequiredException uix = null;
        try {
            mTdr = CryptoHandlerFacade.getInstance(this).decryptWithLock(mOrigText, actionIntent);
        } catch (UserInteractionRequiredException e) {
            uix = e;
        }

        mFragment.setData(this, mOrigText, mTdr, uix, mEncryptionHandler);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mFragment.onCreateOptionsMenu(this, menu);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mFragment.onPrepareOptionsMenu(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            setupExitTransition();
        } else {
            mFragment.onOptionsItemSelected(this, item);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setupExitTransition();
    }

    private void setupExitTransition() {
        overridePendingTransition(0, R.anim.activity_out);
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_CODE_DECRYPT) {
            if (resultCode == Activity.RESULT_OK) {
                //try to decrypt again
                final String nodeOrigText = getIntent().getStringExtra(EXTRA_ENCRYPTED_TEXT);
                try {
                    CryptoHandlerFacade.getInstance(this).decryptWithLock(nodeOrigText, data);
                    update(data);
                } catch (UserInteractionRequiredException e) {
                    try {
                        startIntentSenderForResult(e.getPendingIntent().getIntentSender(), REQUEST_CODE_DECRYPT, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e1) {
                        // and now??
                    }
                }


            } else {
                Ln.w("user cancelled pendingintent activity");
            }
        } else if (requestCode == REQUEST_CODE_DOWNLOAD_MISSING_KEYS) {
            if (resultCode == Activity.RESULT_OK) {

                update(data);

            } else {
                Ln.w("user cancelled pendingintent activity");
            }
        } else //noinspection StatementWithEmptyBody
            if (requestCode == REQUEST_CODE_SHOW_SIGNATURE_KEY) {
            //nothing to be done
        }
    }

}