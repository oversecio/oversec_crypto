package io.oversec.one.common

object Consts {
    private var NOTIFICATION_ID_BASE = 0

    fun getNextNotificationId():Int {
        return ++NOTIFICATION_ID_BASE;
    }

}
