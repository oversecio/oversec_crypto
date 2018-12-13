package io.oversec.one.crypto

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.symbase.KeyUtil
import roboguice.util.Ln

import java.io.*
import java.lang.IllegalArgumentException
import java.util.ArrayList
import java.util.HashMap


class TemporaryContentProvider : ContentProvider() {

    internal class Entry(val mimetype: String, val ttl_seconds: Int, val tag: String?) {
        var data: ByteArray? = null
    }


    @Synchronized
    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val token = getTokenFromUri(uri) ?: throw IllegalArgumentException(uri.toString())
        try {

            if ("w" == mode) {
                val entry = mEntries[token] ?: throw FileNotFoundException("unprepared token!")
                if (entry.data != null) {
                    throw FileNotFoundException("data has already been provided!")
                }
                val pipe = ParcelFileDescriptor.createPipe()
                val pfdRead = pipe[0]
                val pfdWrite = pipe[1]
                val `is` = ParcelFileDescriptor.AutoCloseInputStream(pfdRead)
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                TransferThreadIn(context, `is`, token).start()
                return pfdWrite
            } else if ("r" == mode) {

                val entry =
                    mEntries[token] ?: throw FileNotFoundException("unknown or expired token!")
                if (entry.data == null) {
                    throw FileNotFoundException("data not yet provided token!")
                }
                val pipe = ParcelFileDescriptor.createPipe()
                val pfdRead = pipe[0]
                val pfdWrite = pipe[1]
                val os = ParcelFileDescriptor.AutoCloseOutputStream(pfdWrite)
                TransferThreadOut(os, entry.data!!).start()
                return pfdRead
            }
        } catch (ex: IOException) {
            throw FileNotFoundException(ex.message)
        }


        return null

    }

    class Recv : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_EXPIRE_BUFFER == intent.action) {
                val token = intent.getStringExtra(EXTRA_TOKEN)
                expire(token)
            }
        }
    }


    internal inner class TransferThreadIn(
        private val mCtx: Context,
        private val mIn: InputStream,
        private val mToken: String
    ) : Thread("TTI") {


        init {
            isDaemon = true
        }

        override fun run() {
            val baos = ByteArrayOutputStream()


            try {
                baos.use {
                    mIn.copyTo(baos, 4096)
                }

                val entry = mEntries[mToken]
                if (entry == null) {
                    Ln.d(
                        "TCPR Entry for token %s not found, somebody has re-prepared another content with the same tag, ignoring content input stream",
                        mToken
                    )
                } else {
                    entry.data = baos.toByteArray()

                    val ttlSeconds = mEntries[mToken]!!.ttl_seconds
                    val am = mCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + ttlSeconds * 1000,
                        buildPendingIntent(mCtx, mToken)
                    )

                    if (!mReceiverRegistered) {

                        val filter = IntentFilter(ACTION_EXPIRE_BUFFER)
                        mCtx.applicationContext.registerReceiver(Recv(), filter)
                        mReceiverRegistered = true
                    }
                }

            } catch (e: IOException) {
                try {
                    baos.close()
                } catch (ignored: IOException) {
                }

            } finally {
                try {
                    mIn.close()
                } catch (ignored: IOException) {
                }

            }
        }

    }


    internal inner class TransferThreadOut(private val mOut: OutputStream, private val mData: ByteArray) :
        Thread("TTO") {

        init {
            isDaemon = true
        }

        override fun run() {
            //synchronized (mData)  //hmm, this is dead-locking in case multiple clients access it
            run {
                val mIn = ByteArrayInputStream(mData)

                try {
                    mOut.use {
                        mIn.copyTo(mOut, 4096)
                    }
                } catch (ignored: IOException) {
                    //ignoring possible exception on closing
                }
            }
        }
    }

    override fun onCreate(): Boolean {
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException("query not supported")
    }

    override fun getType(uri: Uri): String? {
        val token = getTokenFromUri(uri)
        val entry = mEntries[token]
        return entry?.mimetype
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("insert not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("delete not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw UnsupportedOperationException("update not supported")
    }

    companion object {

        const val TTL_5_MINUTES = 60 * 5
        const val TTL_1_HOUR = 60 * 60

        const val TAG_ENCRYPTED_IMAGE = "ENCRYPTED_IMAGE"
        const val TAG_CAMERA_SHOT = "CAMERA_SHOT"
        const val TAG_DECRYPTED_IMAGE = "DECRYPTED_IMAGE"

        private const val ACTION_EXPIRE_BUFFER = "OVERSEC_ACTION_EXPIRE_BUFFER"
        private const val EXTRA_TOKEN = "token"

        private val mEntries = HashMap<String, Entry>()
        private var mReceiverRegistered: Boolean = false


        @Synchronized
        fun prepare(ctx: Context, mimetype: String, ttl_seconds: Int, tag: String?): Uri {
            Ln.d("TCPR prepare tag=%s", tag)
            //delete all existing entries with the same tag

            if (tag != null) {
                val toRemove = ArrayList<String>()
                for ((key, value) in mEntries) {
                    if (tag == value.tag) {
                        toRemove.add(key)
                    }
                }
                for (key in toRemove) {
                    expire(key)
                }

            }


            val token = createRandomToken()
            Ln.d("TCPR prepared tag=%s  token=%s", tag, token)
            mEntries[token] = Entry(mimetype, ttl_seconds, tag)

            val authority = ctx.resources.getString(R.string.tempcontent_authority)
            return Uri.parse("content://$authority/$token")
        }

        private fun createRandomToken(): String {
            return SymUtil.byteArrayToHex(KeyUtil.getRandomBytes(16))
        }


        @Synchronized
        fun deleteUri(uri: Uri?) {
            val token = getTokenFromUri(uri ?: return)
            token?.let{ expire(it) }
        }

        private fun getTokenFromUri(uri: Uri): String? {
            val segs = uri.pathSegments
            return if (segs.size >= 1) {
                segs[0]
            } else null
        }

        private fun expire(token: String) {

            val entry = mEntries[token]
            if (entry?.data != null) {

                synchronized(entry.data!!) {
                    KeyUtil.erase(entry.data)
                    entry.data = null
                }
            }
            Ln.d("TCPR expired entry token=%s", token)
            mEntries.remove(token)
        }

        private fun buildPendingIntent(ctx: Context, token: String): PendingIntent {
            val i = Intent()
            //i.setClass(ctx, TemporaryContentProvider.class);  //doesn't work with dynamically registered receivers
            i.action = ACTION_EXPIRE_BUFFER
            i.putExtra(EXTRA_TOKEN, token)

            val flags = 0//PendingIntent.FLAG_ONE_SHOT
            //                | PendingIntent.FLAG_CANCEL_CURRENT
            //                | PendingIntent.FLAG_IMMUTABLE;
            return PendingIntent.getBroadcast(
                ctx, 0,
                i, flags
            )

        }
    }


}
