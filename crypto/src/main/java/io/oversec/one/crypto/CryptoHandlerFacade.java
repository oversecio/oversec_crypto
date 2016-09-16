package io.oversec.one.crypto;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.google.protobuf.ByteString;
import io.oversec.one.crypto.encoding.XCoderAndPadder;
import io.oversec.one.crypto.encoding.XCoderFactory;
import io.oversec.one.crypto.encoding.pad.XCoderAndPadderFactory;
import io.oversec.one.crypto.gpg.GpgCryptoHandler;
import io.oversec.one.crypto.proto.Inner;
import io.oversec.one.crypto.proto.Outer;
import io.oversec.one.crypto.sym.SymmetricCryptoHandler;
import io.oversec.one.crypto.symbase.KeyUtil;
import io.oversec.one.crypto.symsimple.SimpleSymmetricCryptoHandler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class CryptoHandlerFacade implements Handler.Callback {
    private static CryptoHandlerFacade INSTANCE;


    private static final int WHAT_DECRYPT = 1;
    private static LinkedHashMap<String, Outer.Msg> mEncodedCache = new LinkedHashMap<String, Outer.Msg>() {
        public static final int MAX_CACHE_ENTRIES = 200;

        @Override
        protected boolean removeEldestEntry(Entry<String, Outer.Msg> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private final Handler mDecryptHandler;


    private Map<EncryptionMethod, AbstractCryptoHandler> mEncryptionHandlers = new HashMap<>();


    private Context mCtx;


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case WHAT_DECRYPT:

                DecryptAsyncParams p = (DecryptAsyncParams) msg.obj;
                try {
                    BaseDecryptResult tdr = decrypt(p.enc, null, p.encryptedText);
                    p.callback.onResult(tdr);
                } catch (UserInteractionRequiredException e) {
                    p.callback.onUserInteractionRequired();
                }
                return true;
        }

        return false;
    }


    private class DecryptAsyncParams {
        final String packagename;
        final Outer.Msg enc;
        final DoDecryptHandler callback;
        final String encryptedText;


        public DecryptAsyncParams(String packagename, Outer.Msg enc, DoDecryptHandler callback, String encryptedText) {
            this.enc = enc;
            this.packagename = packagename;
            this.callback = callback;
            this.encryptedText = encryptedText;
        }
    }

    public synchronized static CryptoHandlerFacade getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new CryptoHandlerFacade(ctx);
        }
        return INSTANCE;
    }


    private CryptoHandlerFacade(Context ctx) {
        mCtx = ctx;

        HandlerThread mDecryptHandlerThread = new HandlerThread(
                "DECRYPT");
        mDecryptHandlerThread.start();
        mDecryptHandler = new Handler(mDecryptHandlerThread.getLooper(), this);

        //not checking for OpenKeychain here as it should be possible to just post-install that
        mEncryptionHandlers.put(EncryptionMethod.GPG, new GpgCryptoHandler(ctx));
        mEncryptionHandlers.put(EncryptionMethod.SYM, new SymmetricCryptoHandler(ctx));
        mEncryptionHandlers.put(EncryptionMethod.SIMPLESYM, new SimpleSymmetricCryptoHandler(ctx));
    }

    public AbstractCryptoHandler getCryptoHandler(String encoded) {
        try {
            Outer.Msg decoded = XCoderFactory.getInstance(mCtx).decode(encoded);
            if (decoded.hasMsgTextGpgV0()) {
                return mEncryptionHandlers.get(EncryptionMethod.GPG);
            } else if (decoded.hasMsgTextSymV0()) {
                return mEncryptionHandlers.get(EncryptionMethod.SYM);
            } else if (decoded.hasMsgTextSymSimpleV0()) {
                return mEncryptionHandlers.get(EncryptionMethod.SIMPLESYM);
            }
        } catch (Exception e) {
            e.printStackTrace();


        }
        return null;
    }


    public AbstractCryptoHandler getCryptoHandler(BaseDecryptResult result) {
        return mEncryptionHandlers.get(result.getEncryptionMethod());
    }

    public AbstractCryptoHandler getCryptoHandler(EncryptionMethod encryptionMethod) {
        return mEncryptionHandlers.get(encryptionMethod);
    }


    public void clearDecryptQueue() {
        mDecryptHandler.removeMessages(WHAT_DECRYPT);
    }

//    public void clearCache() {
//        //no need to cleat the decodeCache, it keeps only decoded but still encrypted data
//        //mDecodeCache.evictAll();
//    }


    public BaseDecryptResult decryptWithLock(String encoded, Intent actionIntent) throws UserInteractionRequiredException {
        Outer.Msg decoded = getEncodedData(mCtx, encoded);
        return decoded == null ? null : decrypt(decoded, actionIntent, encoded);
    }


    public void decryptAsync(String packagename, Outer.Msg enc, DoDecryptHandler callback, String encryptedText) {
        mDecryptHandler.sendMessageAtFrontOfQueue(mDecryptHandler.obtainMessage(WHAT_DECRYPT, new DecryptAsyncParams(packagename, enc,
                callback, encryptedText)));
    }


    public static Outer.Msg getEncodedData(Context ctx, String encText) {
        if (encText == null || encText.length() == 0) {
            return null;
        }

        synchronized (mEncodedCache) {
            if (mEncodedCache.containsKey(encText)) {
                return mEncodedCache.get(encText);
            }
            Outer.Msg res = XCoderFactory.getInstance(ctx).decode(encText);
            mEncodedCache.put(encText, res);
            return res;
        }

    }


    public static boolean isEncoded(Context ctx, String encText) {

        return getEncodedData(ctx, encText) != null;
    }

    public static boolean isEncodingCorrupt(Context ctx, String encText) {
        //noinspection SimplifiableIfStatement
        if (encText == null || encText.length() == 0) {
            return false;
        }

        return XCoderFactory.getInstance(ctx).isEncodingCorrupt(encText);

    }

    public BaseDecryptResult decrypt(String enc,
                                     Intent actionIntent) throws UserInteractionRequiredException {

        Outer.Msg msg = XCoderFactory.getInstance(mCtx).decode(enc);
        return msg == null ? null : decrypt(msg, actionIntent, enc);

    }

    public BaseDecryptResult decrypt(Outer.Msg msg,
                                     Intent actionIntent, String encryptedText) throws UserInteractionRequiredException {

        BaseDecryptResult res;
        EncryptionMethod method = null;
        if (msg.hasMsgTextSymV0()) {
            method = EncryptionMethod.SYM;
        } else if (msg.hasMsgTextSymSimpleV0()) {
            method = EncryptionMethod.SIMPLESYM;
        } else if (msg.hasMsgTextGpgV0()) {
            method = EncryptionMethod.GPG;
        }
        AbstractCryptoHandler h = mEncryptionHandlers.get(method);
        if (h != null) {
            res = h.decrypt(msg, actionIntent, encryptedText);
        } else {
            res = new BaseDecryptResult(method, BaseDecryptResult.DecryptError.NO_HANDLER);
        }


        return res;
    }

    public Outer.Msg encrypt(Inner.InnerData inner, AbstractEncryptionParams encryptionParams, Intent actionIntent)
            throws GeneralSecurityException, IOException, UserInteractionRequiredException {
        if (encryptionParams == null) {
            throw new IllegalArgumentException("no encryption params found");
        }
        AbstractCryptoHandler h = mEncryptionHandlers.get(encryptionParams.getEncryptionMethod());
        if (h == null) {
            throw new IllegalArgumentException();
        } else {
            return h.encrypt(inner, encryptionParams, actionIntent);
        }
    }

    public String encrypt(AbstractEncryptionParams encryptionParams, String srcText, boolean appendNewLines, int innerPad, String packagename, Intent actionIntent)
            throws Exception {




        AbstractCryptoHandler h = mEncryptionHandlers.get(encryptionParams.getEncryptionMethod());
        XCoderAndPadder xCoderAndPadder = XCoderAndPadderFactory.getInstance(mCtx).get(encryptionParams.getCoderId(), encryptionParams.getPadderId());

        if (h == null) {
            throw new IllegalArgumentException();
        } else {
            Outer.Msg msg;
            if (xCoderAndPadder.getXcoder().isTextOnly()) {

                msg = h.encrypt(srcText, encryptionParams, actionIntent);
            }
            else
            {
                Inner.InnerData.Builder innerDataBuilder = Inner.InnerData.newBuilder();

                Inner.TextAndPaddingV0.Builder textAndPaddingBuilder = innerDataBuilder.getTextAndPaddingV0Builder();

                textAndPaddingBuilder.setText(srcText);

                if (innerPad > 0) {
                    byte[] padding = KeyUtil.getRandomBytes(innerPad);
                    textAndPaddingBuilder.setPadding(ByteString.copyFrom(padding));
                }

                Inner.InnerData innerData = innerDataBuilder.build();

                msg = h.encrypt(innerData, encryptionParams, actionIntent);
            }

            String r = xCoderAndPadder.encode(msg, srcText, appendNewLines,packagename);
            return r;
        }


    }


}
