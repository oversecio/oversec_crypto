package io.oversec.one.crypto;

import java.util.*;

/**
 * Created by yao on 05/09/16.
 */

public class Issues {
    private static Set<String> PACKAGES_WITH_ISSUES = new HashSet<>();

    static {
        PACKAGES_WITH_ISSUES.add("com.android.messaging");
        PACKAGES_WITH_ISSUES.add("com.google.android.apps.inbox");
        PACKAGES_WITH_ISSUES.add("com.google.android.apps.messaging");
        PACKAGES_WITH_ISSUES.add("com.google.android.gm");
        PACKAGES_WITH_ISSUES.add("com.google.android.talk");
        PACKAGES_WITH_ISSUES.add("org.telegram.messenger");
        PACKAGES_WITH_ISSUES.add("org.thoughtcrime.securesms");
        PACKAGES_WITH_ISSUES.add("com.instagram.android");
        PACKAGES_WITH_ISSUES.add("com.facebook.orca");
        PACKAGES_WITH_ISSUES.add("com.google.android.apps.fireball"); //allo

    }
    public static boolean hasKnownIssues(String packagename) {
        return PACKAGES_WITH_ISSUES.contains(packagename);
    }

    private static Set<String> PACKAGES_WITH_SERIOUS_ISSUES = new HashSet<>();

    static {
        PACKAGES_WITH_SERIOUS_ISSUES.add("org.telegram.messenger"); //telegram
        PACKAGES_WITH_SERIOUS_ISSUES.add("org.thoughtcrime.securesms"); //signal
        PACKAGES_WITH_SERIOUS_ISSUES.add("com.wire"); //wire

    }
    public static boolean hasSeriousIssues(String packagename) {
        return PACKAGES_WITH_SERIOUS_ISSUES.contains(packagename);
    }

    private static Set<String> PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK = new HashSet<>();
    static {
        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add("com.google.android.apps.inbox"); //Inbox by Gmail
        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add("com.google.android.gm"); //Gmail
        PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.add(" com.moez.QKSMS"); //QKSMS

    }
    public static boolean cantHandleInvisibleEncoding(String packagename) {
        return PACKAGES_WHERE_INVISIBLE_ENCODING_DOESNT_WORK.contains(packagename);
    }


    private static Map<String,Integer> PACKAGES_WITH_LIMITED_INPUT_FIELDS = new HashMap<>();
    static {
        PACKAGES_WITH_LIMITED_INPUT_FIELDS.put("com.google.android.apps.messaging",2000); //new Google Messenger
    }
    public static Integer getInputFieldLimit(String packagename) {
        return PACKAGES_WITH_LIMITED_INPUT_FIELDS.get(packagename);
    }

    private static Set<String> PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPRTANT_VIEWS = new HashSet<>();
    static {
        PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPRTANT_VIEWS.add("com.google.android.talk");//Hangouts
        PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPRTANT_VIEWS.add("com.google.android.apps.messaging"); //new Messaging
        PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPRTANT_VIEWS.add("com.google.android.apps.fireball"); //Allo

    }
    public static Collection<String> getPackagesThatNeedIncludeNonImportantViews() {
        return PACKAGES_THAT_NEED_TO_SCRAPE_NON_IMPRTANT_VIEWS;
    }

    private static Set<String> PACKAGES_THAT_NEED_COMPOSE_BUTTON = new HashSet<>();
    static {
        PACKAGES_THAT_NEED_COMPOSE_BUTTON.add("com.google.android.apps.inbox");//Inbox By Gmail
        PACKAGES_THAT_NEED_COMPOSE_BUTTON.add("com.evernote");//Inbox By Gmail
    }
    public static Collection<String> getPackagesThatNeedComposeButton() {
        return PACKAGES_THAT_NEED_COMPOSE_BUTTON;
    }


    private static Set<String> PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING = new HashSet<>();
    static {
        PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING.add("com.facebook.orca");//Facebook Messenger
        PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING.add("com.instagram.android"); //Instagram

    }
    public static Collection<String> getPackagesThatNeedSpreadInvisibleEncoding() {
        return PACKAGES_THAT_NEED_SPREAD_INVISIBLE_ENCODING;
    }


}
