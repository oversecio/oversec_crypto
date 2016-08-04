package io.oversec.one.crypto.sym;

import android.app.PendingIntent;
import io.oversec.one.crypto.UserInteractionRequiredException;

public class KeyNotCachedException extends UserInteractionRequiredException {

    public KeyNotCachedException(PendingIntent pi) {
        super(pi);
    }
}
