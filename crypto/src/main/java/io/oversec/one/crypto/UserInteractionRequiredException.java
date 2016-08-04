package io.oversec.one.crypto;

import android.app.PendingIntent;

import java.util.List;

public class UserInteractionRequiredException extends Exception {

    private final PendingIntent mPendingIntent;
    private Long[] mPublicKeyIds;

    public UserInteractionRequiredException(PendingIntent pi, List<Long> pkids) {
        mPendingIntent = pi;
        if (pkids != null) {
            mPublicKeyIds = new Long[pkids.size()];
            pkids.toArray(mPublicKeyIds);
        }
    }

    public UserInteractionRequiredException(PendingIntent pi, Long[] pkids) {
        mPendingIntent = pi;
        mPublicKeyIds = pkids;
    }


    public UserInteractionRequiredException(PendingIntent pi) {
        mPendingIntent = pi;
    }

    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }


}
