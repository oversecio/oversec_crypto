package io.oversec.one.crypto.encoding.pad;

import java.util.Comparator;

public class PadderContent {
    private long mKey;
    private String mName;
    private String mSort;
    private String mContent;

    public static Comparator<? super PadderContent> sortComparator = new Comparator<PadderContent>() {
        @Override
        public int compare(PadderContent lhs, PadderContent rhs) {
            return lhs.mSort.compareTo(rhs.mSort);
        }
    };

    public PadderContent() {
    }

    public PadderContent(String name, String content) {
        mKey = System.currentTimeMillis();
        mSort = name;
        mName = name;
        mContent = content;


    }


    public PadderContent(String sort, String name, String content) {
        mKey = System.currentTimeMillis();
        mSort = sort;
        mName = name;
        mContent = content;


    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String mContent) {
        this.mContent = mContent;
    }

    public long getKey() {
        return mKey;
    }

    public String getContentBegin() {
        final int length = 30;
        return mContent.length() < 30 ? mContent : mContent.substring(0, length);
    }

    public void setSort(String v) {
        mSort = v;
    }
}
