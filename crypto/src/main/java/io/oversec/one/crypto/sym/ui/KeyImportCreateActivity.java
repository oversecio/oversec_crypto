package io.oversec.one.crypto.sym.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import io.oversec.one.crypto.Consts;
import io.oversec.one.crypto.Help;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.sym.OversecKeystore2;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.sym.SymmetricKeyEncrypted;
import io.oversec.one.crypto.sym.SymmetricKeyPlain;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symbase.OversecChacha20Poly1305;
import io.oversec.one.crypto.ui.NewPasswordInputDialog;
import io.oversec.one.crypto.ui.NewPasswordInputDialogCallback;
import io.oversec.one.crypto.ui.SecureBaseActivity;
import io.oversec.one.crypto.ui.util.ImageInfo;
import io.oversec.one.crypto.ui.util.ImgUtil;
import io.oversec.one.crypto.ui.util.MaterialTitleBodyAdapter;
import io.oversec.one.crypto.ui.util.MaterialTitleBodyListItem;

import java.io.IOException;

public class KeyImportCreateActivity extends SecureBaseActivity implements QRCodeReaderView.OnQRCodeReadListener, ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int RQ_SHOW_DETAILS_AFTER_SAVE = 1000;

    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;

    private static final String EXTRA_MODE = "mode";
    public static final String CREATE_MODE_RANDOM = "random";
    public static final String CREATE_MODE_SCAN = "scan";
    public static final String CREATE_MODE_PASSPHRASE = "pbkdf";
    private static final String EXTRA_KEY = "EXTRA_KEY";
    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_PBKDF_INPUT = "EXTRA_PBKDF_INPUT";

    private EditText mEtAlias;
    private TextInputLayout mTvAliasWrapper;
    private TextView mProgressLabel;
    private OversecKeystore2 mKeystore;
    private Toolbar mToolbar;
    private String mImportedString;
    private QRCodeReaderView mQRCodeReaderView;
    private char[] mTempPbkdfInput;


    private static void showForResult(Fragment fragment, int rq, String mode) {


        Intent i = new Intent();
        i.setClass(fragment.getActivity(), KeyImportCreateActivity.class);
        i.putExtra(EXTRA_MODE, mode);
        fragment.startActivityForResult(i, rq);
    }


    public static void showAddKeyDialog(final Fragment fragment, final int requestCode) {

        final MaterialTitleBodyAdapter adapter = new MaterialTitleBodyAdapter(fragment.getActivity());
        adapter.add(new MaterialTitleBodyListItem.Builder(fragment.getActivity())
                .title(R.string.action_createkey_random_title)
                .body(R.string.action_createkey_random_body)
                .icon(R.drawable.ic_vpn_key_black_24dp)
                .backgroundColor(Color.WHITE)
                .build());
        adapter.add(new MaterialTitleBodyListItem.Builder(fragment.getActivity())
                .title(R.string.action_createkey_pw_title)
                .body(R.string.action_createkey_pw_body)
                .icon(R.drawable.ic_font_download_black_24dp)
                .backgroundColor(Color.WHITE)
                .build());
        adapter.add(new MaterialTitleBodyListItem.Builder(fragment.getActivity())
                .title(R.string.action_addkey_importqr_title)
                .body(R.string.action_addkey_importqr_body)
                .icon(R.drawable.ic_memory_black_24dp)
                //.iconPaddingDp(8)
                .build());

        new MaterialDialog.Builder(fragment.getActivity())
                .title(R.string.title_dialog_add_key)
                .adapter(adapter, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        dialog.dismiss();
                        switch (which) {
                            case 0: // create randomekey
                                showForResult(fragment, requestCode, KeyImportCreateActivity.CREATE_MODE_RANDOM);
                                break;
                            case 1: // create randomekey
                                showForResult(fragment, requestCode, KeyImportCreateActivity.CREATE_MODE_PASSPHRASE);
                                break;
                            case 2: // import QR
                                showForResult(fragment, requestCode, KeyImportCreateActivity.CREATE_MODE_SCAN);
                                break;

                        }
                    }
                })
                .show();


    }

    private ProgressBar mProgressBar;


    private ImageView mIvQr;
    private SymmetricKeyPlain mImportedKey;
    private TextView mTvData;

    private boolean mTrustKey = false;
    private FloatingActionButton mBtSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


        setContentView(R.layout.sym_activity_createkey_random);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        mKeystore = OversecKeystore2.getInstance(this);

        mQRCodeReaderView = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        //noinspection ConstantConditions
        mQRCodeReaderView.setVisibility(View.GONE);

        mBtSave = (FloatingActionButton) findViewById(R.id.fab);
        mBtSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSave();
            }
        });

        mEtAlias = (EditText) findViewById(R.id.et_alias);
        mTvAliasWrapper = (TextInputLayout) findViewById(R.id.alias_wrapper);


        mTvData = (TextView) findViewById(R.id.tvData);


        TextView mTvCaptionAlias = (TextView) findViewById(R.id.caption_alias);
        //noinspection ConstantConditions
        mTvCaptionAlias.setText(getString(R.string.createkey_pbe_alias_caption,
                Consts.MIN_ALIAS_LENGTH));


        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mProgressLabel = (TextView) findViewById(R.id.progressLabel);

        mIvQr = (ImageView) findViewById(R.id.ivQr);
        //noinspection ConstantConditions
        mIvQr.setVisibility(View.GONE);

        mBtSave.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            mImportedKey = (SymmetricKeyPlain) savedInstanceState.getSerializable(EXTRA_KEY);
            mTempPbkdfInput = savedInstanceState.getCharArray(EXTRA_PBKDF_INPUT);
            String title = savedInstanceState.getString(EXTRA_TITLE);
            if (title != null) {
                setTitleWTF(title);
            }
//            mImportedString = savedInstanceState.getString(EXTRA_QR_STRING);
//            if (mImportedString!=null) {
//                handleImportQr(mImportedString);
//            }
        }

        if (mImportedKey == null) {
            if (mTempPbkdfInput != null) { // we were in the middle of generating a key, start over
                createWithPassphrase(mTempPbkdfInput);
            } else {
                if (getIntent().getType() != null && getIntent().getType().startsWith("image/")) {
                    //handle shared image
                    handleSendImage(getIntent());
                    setTitleWTF(getString(R.string.title_activity_generate_key_imported));
                } else if (CREATE_MODE_SCAN.equals(getIntent().getStringExtra(EXTRA_MODE))) {
                    //DO SCAN
                    setTitleWTF(getString(R.string.title_activity_generate_key_scanning));
                    mProgressBar.setVisibility(View.GONE);
                    mProgressLabel.setVisibility(View.GONE);

                    // check Android 6 permission
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        System.out.println("XYZZY onCreateAAAAA -> startBarcodeScan");

                        startBarcodeScan();
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }

                } else if (CREATE_MODE_RANDOM.equals(getIntent().getStringExtra(EXTRA_MODE))) {
                    //GENERATE RANDOM
                    setTitleWTF(getString(R.string.title_activity_generate_key));
                    createRandom();
                } else //noinspection StatementWithEmptyBody
                    if (CREATE_MODE_PASSPHRASE.equals(getIntent().getStringExtra(EXTRA_MODE))) {
                    setTitleWTF(getString(R.string.title_activity_generate_key_passphrasebased));
                    createWithPassphrase();
                } else {
                    //don't know what to do, better bail out
                }
            }
        } else {
            setKeyData(mImportedKey);
        }


    }

    private void createWithPassphrase() {
        NewPasswordInputDialogCallback cb = new NewPasswordInputDialogCallback() {
            @Override
            public void positiveAction(char[] pw) {
                createWithPassphrase(pw);
            }

            @Override
            public void neutralAction() {
                finish();
            }
        };
        NewPasswordInputDialog.show(this, NewPasswordInputDialog.MODE.PBKDF, cb);
    }

    private void createWithPassphrase(final char[] pw) {
        mTempPbkdfInput = pw;
        mProgressLabel.setText(R.string.progress_generating_key);
        mTrustKey = true; //we trust our locally generated key by default
        new AsyncTask<Void, Void, SymmetricKeyPlain>() {

            @Override
            protected SymmetricKeyPlain doInBackground(Void... params) {
                byte[] salt = new byte[16]; //constant zero bytes, yes, i know this is bad, but
                //this is meant so that people can exchange a key just by communicating a passphrase.

                byte[] raw = KeyUtil.brcryptifyPassword(pw, salt, KeyUtil.BCRYPT_PBKDF_COST, 32);
                KeyUtil.erase(pw);

                SymmetricKeyPlain key = new SymmetricKeyPlain(raw);
                return key;
            }

            @Override
            protected void onPostExecute(SymmetricKeyPlain key) {

                setKeyData(key);
                mTempPbkdfInput = null;

            }

        }.execute();
    }

    private void createRandom() {
        mProgressLabel.setText(R.string.progress_generating_key);
        mTrustKey = true; //we trust our locally generated key by default

        new AsyncTask<Void, Void, SymmetricKeyPlain>() {

            @Override
            protected SymmetricKeyPlain doInBackground(Void... params) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return KeyUtil.createNewKey();
            }

            @Override
            protected void onPostExecute(SymmetricKeyPlain key) {
                setKeyData(key);

            }

        }.execute();
    }

    private void setTitleWTF(String title) {
        mToolbar.setTitle(title);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
        setTitle(title);
    }

    public Bitmap rotateABit(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap targetBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        canvas.drawARGB(255, 255, 255, 255);
        Matrix matrix = new Matrix();
        matrix.setRotate(5, width / 2, height / 2);
        canvas.drawBitmap(bmpOriginal, matrix, new Paint());
        return targetBitmap;
    }

    private void handleSendImage(final Intent intent) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    try {

                        ImageInfo ii = ImgUtil.parseImageInfo(getContentResolver().openInputStream(imageUri));

                        int sampleSize = Math.max(1, ii.getHeight() * ii.getWidth() / (1024 * 1024));

                        Bitmap bitmap = ImgUtil.loadImage(getContentResolver().openInputStream(imageUri), sampleSize);
                        int width = bitmap.getWidth(), height = bitmap.getHeight();
                        int[] pixels = new int[width * height];
                        Bitmap bmDistorted = rotateABit(bitmap);  //zxing has a problem with synthetic, perfect images, so distort it a bit ;-)

                        //TODO: try with orig image first, only then rotate
                        //TODO: control sample size, maybe downsample image first!

                        bmDistorted.getPixels(pixels, 0, width, 0, 0, width, height);

                        bitmap.recycle();
                        bmDistorted.recycle();

                        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                        BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
                        MultiFormatReader reader = new MultiFormatReader();
                        try {
                            Result result = reader.decode(bBitmap);
                            handleImportQR(result);
                        } catch (NotFoundException e) {
                            showError(getString(R.string.importimage_nothing_found), new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        showError(e.getMessage(), new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        });
                    }
                }
            }
        }).start();


    }

    private void startBarcodeScan() {
        mQRCodeReaderView.setVisibility(View.VISIBLE);
        mQRCodeReaderView.setOnQRCodeReadListener(this);
        mQRCodeReaderView.getCameraManager().startPreview();
//        IntentIntegrator ii = new IntentIntegrator(this);
//        ii.setPrompt(getString(R.string.app_name));
//        ii.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    // Called when your device have no camera
    @Override
    public void cameraNotFound() {

    }

    // Called when there's no QR codes in the camera preview image
    @Override
    public void QRCodeNotFoundOnCamImage() {

    }


    // Called when a QR is decoded
    // "text" : the text encoded in QR
    // "points" : points where QR control points are placed
    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        handleImportQr(text);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mQRCodeReaderView.getCameraManager().stopPreview();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    System.out.println("XYZZY onRequestPermissionsResult -> startBarcodeScan");
                    startBarcodeScan();
                } else {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    private void setKeyData(SymmetricKeyPlain key) {
        mImportedKey = key;
        mProgressBar.setVisibility(View.GONE);
        mProgressLabel.setVisibility(View.GONE);
        mBtSave.setVisibility(View.VISIBLE);
        displayKeyData(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_KEY, mImportedKey);
        outState.putString(EXTRA_TITLE, getTitle().toString());
        outState.putCharArray(EXTRA_PBKDF_INPUT, mTempPbkdfInput);
//        outState.putString(EXTRA_QR_STRING,mImportedString);
    }

    private void doSave() {
//        mTvAliasWrapper.setError(getString(R.string.error_alias_length,
//                Consts.MIN_ALIAS_LENGTH));

        View errorView = null;
        mTvAliasWrapper.setError(null);

        String name = mEtAlias.getText().toString().trim();
        if (name.length() < Consts.MIN_ALIAS_LENGTH) {
            errorView = mEtAlias;
            mTvAliasWrapper.setError(getString(R.string.error_alias_length,
                    Consts.MIN_ALIAS_LENGTH));
        } else {
            if (mKeystore.hasName(name)) {
                errorView = mEtAlias;
                mTvAliasWrapper.setError(getString(R.string.error_alias_exists));
            }
        }


        if (errorView != null) {
            errorView.requestFocus();
        } else {

            NewPasswordInputDialog.show(this, NewPasswordInputDialog.MODE.KEYSTORE, new NewPasswordInputDialogCallback() {
                @Override
                public void positiveAction(char[] pw) {
                    saveKey(mImportedKey, mEtAlias.getText().toString(), pw);
                }

                @Override
                public void neutralAction() {

                }
            });


        }

    }

    private void displayKeyData(boolean blur) {

        mIvQr.setVisibility(View.GONE);
        mTvData.setVisibility(View.GONE);
        if (mImportedKey != null) {
            mProgressLabel.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.GONE);


            mTvData.setVisibility(View.GONE);
            mIvQr.setVisibility(View.VISIBLE);
            int dimension = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, getResources().getDisplayMetrics());

            Bitmap bm;
            if (blur) {
                bm = SymUtil.getQrCode(KeyUtil.getRandomBytes(32), dimension);
                Bitmap bmSmallTmp = Bitmap.createScaledBitmap(bm, 25, 25, true);
                bm = Bitmap.createScaledBitmap(bmSmallTmp, dimension, dimension, true);
            } else {
                bm = SymUtil.getQrCode(OversecKeystore2.getPlainKeyAsTransferBytes(
                        mImportedKey.getRaw()
                ), dimension);
            }
            mIvQr.setImageBitmap(bm);


        }

    }

    private void saveKey(final SymmetricKeyPlain key, final String name, final char[] password) {
        key.setName(name);

        final Dialog d = new MaterialDialog.Builder(this)
                .title(R.string.progress_saving)
                .content(R.string.please_wait_encrypting)
                .progress(true, 0)
                .cancelable(false)
                .show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final long keyId = mKeystore.addKey__longoperation(key, password);
                    if (mTrustKey) {
                        mKeystore.confirmKey(keyId);
                    }

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {


                            Intent d = new Intent();
                            d.putExtra(KeysFragment.EXTRA_KEY_ID, keyId);
                            setResult(RESULT_OK, d);
                            finish();
                        }
                    });

                } catch (final OversecKeystore2.AliasNotUniqueException e) {

                    d.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showError(
                                    getString(R.string.common_error_body,
                                            e.getAlias()), null);
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
                }
            }
        });

        t.start();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_key_createimport, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.help) {
            String mode = getIntent().getStringExtra(EXTRA_MODE);
            if (CREATE_MODE_PASSPHRASE.equals(mode)) {
                Help.open(this, Help.ANCHOR.symkey_create_pbkdf);
            } else if (CREATE_MODE_RANDOM.equals(mode)) {
                Help.open(this, Help.ANCHOR.symkey_create_random);
            } else if (CREATE_MODE_SCAN.equals(mode)) {
                Help.open(this, Help.ANCHOR.symkey_create_scan);
            }

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RQ_SHOW_DETAILS_AFTER_SAVE) {
            finish();
        }
    }

    void handleImportQR(final Result result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (BarcodeFormat.QR_CODE.toString().equals(
                        result.getBarcodeFormat().toString())) {
                    String s = result.getText();
                    handleImportQr(s);
                } else {
                    showError(
                            getString(R.string.error_invalid_barcode_format), new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });
                }
            }
        });

    }

    private void handleImportQr(final String s) {
        mImportedString = s;

        SymmetricKeyPlain key = OversecKeystore2.getPlainKeyFromBase64Text(s);

        mQRCodeReaderView.getCameraManager().stopPreview();
        mQRCodeReaderView.setVisibility(View.GONE);

        if (key != null) {
            setKeyData(key);
        } else {
            SymmetricKeyEncrypted kk = OversecKeystore2.getEncryptedKeyFromBase64Text(s);
            if (kk != null) {

                new MaterialDialog.Builder(this)
                        .title(R.string.import_decrypt_key_title)
                        .content(R.string.import_decrypt_key_content)
                        .cancelable(true)
                        .autoDismiss(true)
                        .negativeText(R.string.common_cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        .input(R.string.keystore_password_hint, R.string.prefill_password_fields, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                handleEncryptedImport(s, input.toString().toCharArray());
                            }
                        }).show();


            } else {
                showError(
                        getString(R.string.error_invalid_barcode_content),
                        new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }
                );
            }
        }
    }

    private void handleEncryptedImport(final String s, char[] password) {
        SymmetricKeyEncrypted encryptedKey = OversecKeystore2.getEncryptedKeyFromBase64Text(s);
        try {
            SymmetricKeyPlain plainKey = mKeystore.decryptSymmetricKey(encryptedKey, password);

            SymmetricKeyEncrypted existingKey = mKeystore.getSymmetricKeyEncrypted(encryptedKey.getId());

            if (existingKey != null) {
                showError(getString(R.string.error_key_exists, existingKey.getName()), new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
                return;
            }

            setKeyData(plainKey);
            mImportedString = null;


            new MaterialDialog.Builder(this)
                    .title(R.string.title_activity_generate_key_imported)
                    .content(R.string.key_import_key_decrypted)
                    .positiveText(R.string.common_ok)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OversecChacha20Poly1305.MacMismatchException e) {
            showError(getString(R.string.error_password_wrong), new Runnable() {
                @Override
                public void run() {
                    handleImportQr(s);
                }
            });
        }
    }


}
