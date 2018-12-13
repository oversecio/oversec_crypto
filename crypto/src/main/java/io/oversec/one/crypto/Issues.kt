package io.oversec.one.crypto

import java.util.*

object Issues {
    private val PACKAGES_WITH_ISSUES = HashSet<String>()
    private val PACKAGES_WITH_SERIOUS_ISSUES = HashSet<String>()
    private val PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK = HashSet<String>()
    private val PACKAGES_WITH_LIMITED_INPUT_FIELDS = HashMap<String, Int>()

    private val PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPORTANT_VIEWS = HashSet<String>()
    val packagesThatNeedIncludeNonImportantViews: Collection<String>
        get() = PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPORTANT_VIEWS

    private val PACKAGES_THAT_NEED_COMPOSE_BUTTON = HashSet<String>()
    val packagesThatNeedComposeButton: Collection<String>
        get() = PACKAGES_THAT_NEED_COMPOSE_BUTTON

    private val PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING = HashSet<String>()
    val packagesThatNeedSpreadInvisibleEncoding: Collection<String>
        get() = PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING

    fun hasKnownIssues(packagename: String): Boolean {
        return PACKAGES_WITH_ISSUES.contains(packagename)
    }

    fun hasSeriousIssues(packagename: String): Boolean {
        return PACKAGES_WITH_SERIOUS_ISSUES.contains(packagename)
    }

    fun cantHandleInvisibleEncoding(packagename: String): Boolean {
        return PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.contains(packagename)
    }

    fun getInputFieldLimit(packagename: String): Int? {
        return PACKAGES_WITH_LIMITED_INPUT_FIELDS[packagename]
    }

    init {
        PACKAGES_WITH_ISSUES.add("com.android.messaging")
        PACKAGES_WITH_ISSUES.add("com.google.android.apps.inbox")
        PACKAGES_WITH_ISSUES.add("com.google.android.apps.messaging")
        PACKAGES_WITH_ISSUES.add("com.google.android.gm")
        PACKAGES_WITH_ISSUES.add("com.evernote")
        PACKAGES_WITH_ISSUES.add("com.google.android.talk")
        PACKAGES_WITH_ISSUES.add("org.telegram.messenger")
        PACKAGES_WITH_ISSUES.add("org.thoughtcrime.securesms")
        PACKAGES_WITH_ISSUES.add("com.instagram.android")
        PACKAGES_WITH_ISSUES.add("com.facebook.orca")
        PACKAGES_WITH_ISSUES.add("com.google.android.apps.fireball") //allo

        PACKAGES_WITH_SERIOUS_ISSUES.add("org.telegram.messenger") //telegram
        PACKAGES_WITH_SERIOUS_ISSUES.add("org.thoughtcrime.securesms") //signal
        PACKAGES_WITH_SERIOUS_ISSUES.add("com.wire") //wire

        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add("com.google.android.apps.inbox") //Inbox by Gmail
        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add("com.google.android.gm") //Gmail
        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add("com.evernote") //Evernote
        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add(" com.moez.QKSMS") //QKSMS

        PACKAGES_WITH_LIMITED_INPUT_FIELDS["com.google.android.apps.messaging"] = 2000 //new Google Messenger

        PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPORTANT_VIEWS.add("com.google.android.talk")//Hangouts
        PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPORTANT_VIEWS.add("com.google.android.apps.messaging") //new Messaging
        PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPORTANT_VIEWS.add("com.google.android.apps.fireball") //Allo

        PACKAGES_THAT_NEED_COMPOSE_BUTTON.add("com.google.android.gm") //Gmail
        PACKAGES_THAT_NEED_COMPOSE_BUTTON.add("com.google.android.apps.inbox")//Inbox By Gmail
        PACKAGES_THAT_NEED_COMPOSE_BUTTON.add("com.evernote")//Inbox By Gmail

        PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING.add("com.facebook.orca")//Facebook Messenger
        PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING.add("com.instagram.android") //Instagram
    }


}
