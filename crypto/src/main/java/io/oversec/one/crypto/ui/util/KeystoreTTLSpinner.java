package io.oversec.one.crypto.ui.util;


import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import io.oversec.one.crypto.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeystoreTTLSpinner extends AppCompatSpinner {

    private static Map<Integer, Integer> TTL_NAMES;
    private static ArrayList<Integer> TTLS;

    static {
        HashMap<Integer, Integer> cacheTtlNames = new HashMap<>();
        cacheTtlNames.put(0, R.string.keystore_ttl_lock_screen);
        cacheTtlNames.put(60 * 5, R.string.keystore_ttl_five_minutes);
        cacheTtlNames.put(60 * 30, R.string.keystore_ttl_thirty_minutes);
        cacheTtlNames.put(60 * 60, R.string.keystore_ttl_one_hour);
        cacheTtlNames.put(60 * 60 * 6, R.string.keystore_ttl_six_hours);
        cacheTtlNames.put(60 * 60 * 24, R.string.keystore_ttl_one_day);
        cacheTtlNames.put(Integer.MAX_VALUE, R.string.keystore_ttl_forever);
        TTL_NAMES = Collections.unmodifiableMap(cacheTtlNames);
        TTLS = new ArrayList<>(TTL_NAMES.keySet());
        Collections.sort(TTLS);
    }


    public KeystoreTTLSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public KeystoreTTLSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {

        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "TTL", "description"}, 5);
        int i = 0;
        for (int ttl : TTLS) {
            cursor.addRow(new Object[]{i++, ttl, getContext().getString(TTL_NAMES.get(ttl))});
        }

        setAdapter(new SimpleCursorAdapter(getContext(), R.layout.simple_item, cursor,
                new String[]{"description"}, new int[]{R.id.simple_item_text}, 0));
    }

    public int getSelectedTTL() {
        int selectedItemPosition = getSelectedItemPosition();
        Object item = getAdapter().getItem(selectedItemPosition);
        return ((Cursor) item).getInt(1);
    }

    public void setSelectedTTL(int ttl) {
        int pos = TTLS.indexOf(ttl);
        setSelection(pos);
    }
}
