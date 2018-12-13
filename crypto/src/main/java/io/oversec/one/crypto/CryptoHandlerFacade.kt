package io.oversec.one.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.google.protobuf.ByteString
import io.oversec.one.crypto.encoding.XCoderFactory
import io.oversec.one.crypto.encoding.pad.XCoderAndPadderFactory
import io.oversec.one.crypto.gpg.GpgCryptoHandler
import io.oversec.one.crypto.proto.Inner
import io.oversec.one.crypto.proto.Outer
import io.oversec.one.crypto.sym.SymmetricCryptoHandler
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler

import java.io.IOException
import java.security.GeneralSecurityException
import java.util.HashMap
import java.util.LinkedHashMap


class CryptoHandlerFacade private constructor(private val mCtx: Context) : Handler.Callback {

    private val mDecryptHandler: Handler
    private val mEncryptionHandlers = HashMap<EncryptionMethod, AbstractCryptoHandler>()

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            WHAT_DECRYPT -> {
                val p = msg.obj as DecryptAsyncParams
                try {
                    val tdr = decrypt(p.enc, null, p.encryptedText)
                    p.callback.onResult(tdr)
                } catch (e: UserInteractionRequiredException) {
                    p.callback.onUserInteractionRequired()
                }
                return true
            }
        }
        return false
    }

    private inner class DecryptAsyncParams(
        internal val packagename: String,
        internal val enc: Outer.Msg,
        internal val callback: DoDecryptHandler,
        internal val encryptedText: String?
    )

    init {
        val mDecryptHandlerThread = HandlerThread("DECRYPT")
        mDecryptHandlerThread.start()
        mDecryptHandler = Handler(mDecryptHandlerThread.looper, this)

        //not checking for OpenKeychain here as it should be possible to just post-install that
        mEncryptionHandlers[EncryptionMethod.GPG] = GpgCryptoHandler(mCtx)
        mEncryptionHandlers[EncryptionMethod.SYM] = SymmetricCryptoHandler(mCtx)
        mEncryptionHandlers[EncryptionMethod.SIMPLESYM] = SimpleSymmetricCryptoHandler(mCtx)
    }

    fun getCryptoHandler(encoded: String): AbstractCryptoHandler? {
        val decoded = XCoderFactory.getInstance(mCtx).decode(encoded)
        return try {
            when {
                decoded == null -> null
                decoded.hasMsgTextGpgV0() -> mEncryptionHandlers[EncryptionMethod.GPG]
                decoded.hasMsgTextSymV0() -> mEncryptionHandlers[EncryptionMethod.SYM]
                decoded.hasMsgTextSymSimpleV0() -> mEncryptionHandlers[EncryptionMethod.SIMPLESYM]
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCryptoHandler(result: BaseDecryptResult): AbstractCryptoHandler? {
        return mEncryptionHandlers[result.encryptionMethod]
    }

    fun getCryptoHandler(encryptionMethod: EncryptionMethod): AbstractCryptoHandler {
        return mEncryptionHandlers[encryptionMethod]!!
    }

    fun clearDecryptQueue() {
        mDecryptHandler.removeMessages(WHAT_DECRYPT)
    }

    @Throws(UserInteractionRequiredException::class)
    fun decryptWithLock(encoded: String, actionIntent: Intent?): BaseDecryptResult? {
        val decoded = getEncodedData(mCtx, encoded)
        return decoded?.let { decrypt(it, actionIntent, encoded) }
    }

    fun decryptAsync(
        packagename: String,
        enc: Outer.Msg,
        callback: DoDecryptHandler,
        encryptedText: String?
    ) {
        mDecryptHandler.sendMessageAtFrontOfQueue(
            mDecryptHandler.obtainMessage(
                WHAT_DECRYPT, DecryptAsyncParams(
                    packagename, enc,
                    callback, encryptedText
                )
            )
        )
    }

    @Throws(UserInteractionRequiredException::class)
    fun decrypt(
        enc: String,
        actionIntent: Intent?
    ): BaseDecryptResult? {

        val msg = XCoderFactory.getInstance(mCtx).decode(enc)
        return msg?.let{decrypt(it, actionIntent, enc)}
    }

    @Throws(UserInteractionRequiredException::class)
    fun decrypt(
        msg: Outer.Msg,
        actionIntent: Intent?, encryptedText: String?
    ): BaseDecryptResult? {

        val res: BaseDecryptResult?
        var method: EncryptionMethod? = null
        when {
            msg.hasMsgTextSymV0() -> method = EncryptionMethod.SYM
            msg.hasMsgTextSymSimpleV0() -> method = EncryptionMethod.SIMPLESYM
            msg.hasMsgTextGpgV0() -> method = EncryptionMethod.GPG
        }
        val h = mEncryptionHandlers[method]

        res = when {
            h!=null -> h.decrypt(msg, actionIntent, encryptedText)
            else -> BaseDecryptResult(method, BaseDecryptResult.DecryptError.NO_HANDLER)
        }

        return res
    }

    @Throws(
        GeneralSecurityException::class,
        IOException::class,
        UserInteractionRequiredException::class
    )
    fun encrypt(
        inner: Inner.InnerData,
        encryptionParams: AbstractEncryptionParams?,
        actionIntent: Intent?
    ): Outer.Msg? {
        requireNotNull(encryptionParams) { "no encryption params found" }

        val h = mEncryptionHandlers[encryptionParams.encryptionMethod]
        requireNotNull(h)
        return h.encrypt(inner, encryptionParams, actionIntent)
    }

    @Throws(Exception::class)
    fun encrypt(
        encryptionParams: AbstractEncryptionParams,
        srcText: String,
        appendNewLines: Boolean,
        padding: ByteArray?,
        packagename: String,
        actionIntent: Intent?
    ): String {

        val h = mEncryptionHandlers[encryptionParams.encryptionMethod]
        val xCoderAndPadder = XCoderAndPadderFactory.getInstance(mCtx)
            .get(encryptionParams.coderId, encryptionParams.padderId)

        requireNotNull(h)
        val msg: Outer.Msg?
        if (xCoderAndPadder!!.xcoder.isTextOnly) {

            msg = h.encrypt(srcText, encryptionParams, actionIntent)
        } else {
            val innerDataBuilder = Inner.InnerData.newBuilder()

            val textAndPaddingBuilder = innerDataBuilder.textAndPaddingV0Builder

            textAndPaddingBuilder.text = srcText


            padding?.let {
                if (it.isNotEmpty()) textAndPaddingBuilder.padding = ByteString.copyFrom(padding)
            }

            val innerData = innerDataBuilder.build()

            msg = h.encrypt(innerData, encryptionParams, actionIntent)
        }

        return xCoderAndPadder.encode(msg!!, srcText, appendNewLines, packagename)

    }

    companion object {
        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var INSTANCE: CryptoHandlerFacade? = null

        fun getInstance(ctx: Context): CryptoHandlerFacade =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CryptoHandlerFacade(ctx.applicationContext).also { INSTANCE = it }
            }

        private const val WHAT_DECRYPT = 1
        private val mEncodedCache = object : LinkedHashMap<String, Outer.Msg?>() {
            val MAX_CACHE_ENTRIES = 200

            override fun removeEldestEntry(eldest: Map.Entry<String, Outer.Msg?>): Boolean {
                return size > MAX_CACHE_ENTRIES
            }
        }


        fun getEncodedData(ctx: Context, encText: String?): Outer.Msg? {
            if (encText.isNullOrEmpty()) {
                return null
            }
            synchronized(mEncodedCache) {
                if (mEncodedCache.containsKey(encText)) {
                    return mEncodedCache[encText]
                }
                val res = XCoderFactory.getInstance(ctx).decode(encText)
                mEncodedCache[encText] = res
                return res
            }
        }

        fun isEncoded(ctx: Context, encText: String?): Boolean {
            return if (encText.isNullOrEmpty())
                false
            else getEncodedData(ctx, encText) != null
        }

        fun isEncodingCorrupt(ctx: Context, encText: String?): Boolean {
            return if (encText.isNullOrEmpty())
                false
            else XCoderFactory.getInstance(ctx).isEncodingCorrupt(encText)

        }
    }

}
