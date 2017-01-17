package io.oversec.one.crypto;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by yao on 17/01/17.
 */

public class ZxcvbnResult implements Parcelable {

    private String mWarning;
    private int mEntropy;

    public ZxcvbnResult(Parcel source) {
        mWarning = source.readString();
        mEntropy = source.readInt();
    }

    public ZxcvbnResult(String result, int entropy) {
        mWarning = result;
        mEntropy = entropy;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mWarning);
        dest.writeInt(mEntropy);
    }

    public static final Creator<ZxcvbnResult> CREATOR = new Creator<ZxcvbnResult>() {
        @Override
        public ZxcvbnResult[] newArray(int size) {
            return new ZxcvbnResult[size];
        }

        @Override
        public ZxcvbnResult createFromParcel(Parcel source) {
            return new ZxcvbnResult(source);
        }
    };

    public CharSequence getWarning() {
        return mWarning;
    }

    public int getEntropy() {
        return mEntropy;
    }
}
