package io.oversec.one.crypto.ui.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.text.TextPaint;

import java.io.IOException;
import java.io.InputStream;

public class ImgUtil {

    public static ImageInfo parseImageInfo(byte[] bb) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(bb, 0, bb.length, options);
        return new ImageInfo(options.outMimeType, options.outWidth, options.outHeight);
    }

    public static ImageInfo parseImageInfo(InputStream is) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        return new ImageInfo(options.outMimeType, options.outWidth, options.outHeight);
    }

    public static Bitmap loadImage(InputStream inputStream, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeStream(inputStream, null, options);
    }
}
