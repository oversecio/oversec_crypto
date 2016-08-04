package io.oversec.one.crypto.images.xcoder.blackandwhite;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.IOException;
import java.io.InputStream;

public class BitmapInputStream extends InputStream {

    private final Bitmap mBm;
    private final int mW;
    private final int mH;

    private int mPixelOffset;

    BitmapInputStream(Bitmap bm) {
        mBm = bm;
        mW = bm.getWidth();
        mH = bm.getHeight();
    }

    @Override
    public int read() throws IOException {
        int y = mPixelOffset / mW;
        int x = mPixelOffset - y * mW;
        int res = 0;
        for (int k = 0; k < 8; k++) {
            if (x > mW || y > mH) {
                return -1;
            }

            int c = mBm.getPixel(x, y);
            int score = 0;
            if (Color.red(c) > 128) {
                score++;
            }
            if (Color.blue(c) > 128) {
                score++;
            }
            if (Color.green(c) > 128) {
                score++;
            }
            if (score > 1) {
                res += (byte) 1 << (7 - k);
            }
            x++;
            if (x >= mW) {
                x = 0;
                y++;
            }
        }
        mPixelOffset += 8;
        return res;
    }
}