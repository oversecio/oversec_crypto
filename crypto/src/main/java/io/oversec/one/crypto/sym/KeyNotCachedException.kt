package io.oversec.one.crypto.sym

import android.app.PendingIntent
import io.oversec.one.crypto.UserInteractionRequiredException

class KeyNotCachedException(pi: PendingIntent) : UserInteractionRequiredException(pi)
