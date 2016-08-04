package io.oversec.one.crypto.ui.util;

public class ImageInfo {
    private final String mMimeType;
    private final int mWidth;
    private final int mHeight;

    public ImageInfo(String mimtype, int width, int height) {
        mMimeType = mimtype;
        mWidth = width;
        mHeight = height;
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
                "mMimeType='" + mMimeType + '\'' +
                ", mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                '}';
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getMimetype() {
        return mMimeType;
    }
}
