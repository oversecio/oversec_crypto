package io.oversec.one.crypto.images.xcoder.blackandwhite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import io.oversec.one.crypto.TemporaryContentProvider;
import io.oversec.one.crypto.images.xcoder.ContentNotFullyEmbeddedException;
import io.oversec.one.crypto.images.xcoder.ImageXCoder;
import io.oversec.one.crypto.proto.Outer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Encodes one bit as one black/white pixel.
 * <p>
 * While this approach may seem to be quite naive,
 * it is surprisingly robust and surivives JPEG comression well!
 * <p>

 */
public class BlackAndWhiteImageXCoder implements ImageXCoder {

    public static final int MAX_OUT_WH = 1024;


    private Context mCtx;

    public BlackAndWhiteImageXCoder(Context ctx) {
        mCtx = ctx;
    }

    @Override
    public Outer.Msg parse(Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565; //save some space
        InputStream is = mCtx.getContentResolver().openInputStream(uri);
        if (is==null) {
            throw new IOException();
        }
        Bitmap bm = BitmapFactory.decodeStream(is, null, options);
        is.close();

        BitmapInputStream bis = new BitmapInputStream(bm);
        Outer.Msg res = Outer.Msg.parseDelimitedFrom(bis);
        return res;
    }

    @Override
    public Uri encode(Outer.Msg msg) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.writeDelimitedTo(baos);
        baos.close();
        byte[] plain = baos.toByteArray();

        int wh = (int) Math.ceil(Math.sqrt(plain.length * 8));

        if (wh >= MAX_OUT_WH) {
            throw new ContentNotFullyEmbeddedException();
        }

        Bitmap bm = Bitmap.createBitmap(wh, wh, Bitmap.Config.ARGB_8888);


        int offset = 0;
        for (byte aPlain : plain) {
            offset = setBitmapPixel(bm, offset, wh, aPlain); //TODO: needs some  optimization
        }

        Uri uri = TemporaryContentProvider.prepare(mCtx, "image/png", TemporaryContentProvider.TTL_5_MINUTES, TemporaryContentProvider.TAG_ENCRYPTED_IMAGE);
        OutputStream os = mCtx.getContentResolver().openOutputStream(uri);
        if (os==null) {
            throw new IOException();
        }
        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();

        return uri;
    }

    private int setBitmapPixel(Bitmap bm, int offset, int wh, byte v) {


        for (int i = 0; i < 8; i++) {
            int y = offset / wh;
            int x = offset - y * wh;
            int color = Color.BLACK;
            int b = ((v >> (7 - i)) & 0x01);
            if (b > 0) {
                color = Color.WHITE;
            }
            bm.setPixel(x, y, color);

            offset++;
        }
        return offset;

    }

}
