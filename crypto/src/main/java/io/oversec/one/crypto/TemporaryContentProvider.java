package io.oversec.one.crypto;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.symbase.KeyUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TemporaryContentProvider extends ContentProvider {
    public static final int TTL_5_MINUTES = 60 * 5;
    public static final int TTL_1_HOUR = 60 * 60;


    public static final String AUTHORITY = "oversec_temporary_content";
    private static final String ACTION_EXPIRE_BUFFER = "OVERSEC_ACTION_EXPIRE_BUFFER";
    private static final String EXTRA_TOKEN = "token";
    public static final String TAG_ENCRYPTED_IMAGE = "ENCRYPTED_IMAGE";
    public static final String TAG_CAMERA_SHOT = "CAMERA_SHOT";
    public static final String TAG_DECRYPTED_IMAGE = "DECRYPTED_IMAGE";

    private static Map<String, Entry> mEntries = new HashMap<>();
    private static boolean mReceiverRegistered;


    public static synchronized Uri prepare(Context ctx, String mimetype, int ttl_seconds, String tag) {

        //delete all existing entries with the same tag

        if (tag != null) {
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, Entry> entry : mEntries.entrySet()) {
                if (tag.equals(entry.getValue().tag)) {
                    toRemove.add(entry.getKey());
                }
            }
            for (String key : toRemove) {
                expire(key);
            }

        }


        String token = createRandomToken();

        mEntries.put(token, new Entry(mimetype, ttl_seconds, tag));
        return Uri.parse("content://" + AUTHORITY + "/" + token);
    }

    static class Entry {
        final String tag;
        final String mimetype;
        final int ttl_seconds;
        byte[] data;

        public Entry(String mimetype, int ttl_seconds, String tag) {
            this.mimetype = mimetype;
            this.ttl_seconds = ttl_seconds;
            this.tag = tag;
        }
    }

    private static String createRandomToken() {
        return SymUtil.byteArrayToHex(KeyUtil.getRandomBytes(16));
    }


    public synchronized static void deleteUri(Uri uri) {
        if (uri == null) {
            return;
        }
        String token = getTokenFromUri(uri);
        if (token != null) {
            expire(token);
        }
    }

    private static String getTokenFromUri(Uri uri) {
        List<String> segs = uri.getPathSegments();
        if (segs.size() >= 1) {
            return segs.get(0);
        }
        return null;
    }

    private static void expire(String token) {
        Entry entry = mEntries.get(token);
        if (entry != null && entry.data != null) {
            //noinspection SynchronizeOnNonFinalField
            synchronized (entry.data) {
                KeyUtil.erase(entry.data);
                entry.data = null;
            }
        }
        mEntries.remove(token);
    }

    private static PendingIntent buildPendingIntent(Context ctx, String token) {
        Intent i = new Intent();
        //i.setClass(ctx, TemporaryContentProvider.class);  //doesn't work with dynamically registered receivers
        i.setAction(ACTION_EXPIRE_BUFFER);
        i.putExtra(EXTRA_TOKEN, token);

        int flags = 0;//PendingIntent.FLAG_ONE_SHOT
//                | PendingIntent.FLAG_CANCEL_CURRENT
//                | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent res = PendingIntent.getBroadcast(ctx, 0,
                i, flags);
        return res;

    }


    @Nullable
    @Override
    public synchronized ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        String token = getTokenFromUri(uri);

        try {

            if ("w".equals(mode)) {
                Entry entry = mEntries.get(token);
                if (entry == null) {
                    throw new FileNotFoundException("unprepared token!");
                }
                if (entry.data != null) {
                    throw new FileNotFoundException("data has already been provided!");
                }
                ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                ParcelFileDescriptor pfdRead = pipe[0];
                ParcelFileDescriptor pfdWrite = pipe[1];
                InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfdRead);
                new TransferThreadIn(getContext(), is, token).start();
                return pfdWrite;
            } else if ("r".equals(mode)) {

                Entry entry = mEntries.get(token);
                if (entry == null) {
                    throw new FileNotFoundException("unknown or expired token!");
                }
                if (entry.data == null) {
                    throw new FileNotFoundException("data not yet provided token!");
                }
                ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                ParcelFileDescriptor pfdRead = pipe[0];
                ParcelFileDescriptor pfdWrite = pipe[1];
                OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(pfdWrite);
                new TransferThreadOut(os, entry.data).start();
                return pfdRead;
            }
        } catch (IOException ex) {
            throw new FileNotFoundException(ex.getMessage());
        }


        return null;

    }

    public static class Recv extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_EXPIRE_BUFFER.equals(intent.getAction())) {
                String token = intent.getStringExtra(EXTRA_TOKEN);
                expire(token);
            }
        }
    }


    class TransferThreadIn extends Thread {
        final InputStream mIn;
        private final String mToken;
        private final Context mCtx;


        TransferThreadIn(Context ctx, InputStream in, String token) {
            super("TTI");
            mCtx = ctx;
            mIn = in;
            mToken = token;
            setDaemon(true);
        }

        @Override
        public void run() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buf = new byte[4096];
            int len;

            try {
                while ((len = mIn.read(buf)) > 0) {

                    baos.write(buf, 0, len);
                }
                baos.close();
                Entry entry = mEntries.get(mToken);
                entry.data = baos.toByteArray();

                int ttl_seconds = mEntries.get(mToken).ttl_seconds;
                AlarmManager am = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (ttl_seconds * 1000), buildPendingIntent(mCtx, mToken));

                if (!mReceiverRegistered) {

                    IntentFilter filter = new IntentFilter(ACTION_EXPIRE_BUFFER);
                    mCtx.getApplicationContext().registerReceiver(new Recv(), filter);
                    mReceiverRegistered = true;
                }


            } catch (IOException e) {
                try {
                    baos.close();
                } catch (IOException ignored) {
                }
            } finally {
                try {
                    mIn.close();
                } catch (IOException ignored) {
                }

            }
        }

    }


    class TransferThreadOut extends Thread {
        final OutputStream mOut;
        private final byte[] mData;


        TransferThreadOut(OutputStream out, byte[] data) {
            super("TTO");
            mOut = out;
            mData = data;
            setDaemon(true);
        }

        @Override
        public void run() {
            //synchronized (mData)  //hmm, this is dead-locking in case multiple clients access it
            {
                ByteArrayInputStream mIn = new ByteArrayInputStream(mData);

                byte[] buf = new byte[4096 * 4];
                int len;

                try {
                    while ((len = mIn.read(buf)) > 0) {

                        mOut.write(buf, 0, len);
                    }
                    mOut.close();
                } catch (IOException e) {
                    try {
                        mOut.close();
                    } catch (IOException ignored) {
                    }
                } finally {
                    try {
                        mIn.close();
                    } catch (IOException ignored) {
                    }

                }
            }
        }

    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("query not supported");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        String token = getTokenFromUri(uri);
        Entry entry = mEntries.get(token);
        if (entry != null) {
            return entry.mimetype;
        }
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("insert not supported");
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("delete not supported");
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("update not supported");
    }


}
