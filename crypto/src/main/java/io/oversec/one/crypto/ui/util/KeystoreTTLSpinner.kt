package io.oversec.one.crypto.ui.util

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.widget.AppCompatSpinner
import android.util.AttributeSet
import io.oversec.one.crypto.R
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class KeystoreTTLSpinner : AppCompatSpinner {

    var selectedTTL: Int
        get() {
            val selectedItemPosition = selectedItemPosition
            val item = adapter.getItem(selectedItemPosition)
            return (item as Cursor).getInt(1)
        }
        set(ttl) {
            val pos = TTLS!!.indexOf(ttl)
            setSelection(pos)
        }


    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initView(context)
    }

    private fun initView(context: Context) {

        val cursor = MatrixCursor(arrayOf("_id", "TTL", "description"), 5)
        var i = 0
        for (ttl in TTLS!!) {
            cursor.addRow(arrayOf(i++, ttl, getContext().getString(TTL_NAMES!![ttl]!!)))
        }

        adapter = SimpleCursorAdapter(
            getContext(), R.layout.simple_item, cursor,
            arrayOf("description"), intArrayOf(R.id.simple_item_text), 0
        )
    }

    companion object {

        private var TTL_NAMES: Map<Int, Int>? = null
        private var TTLS: ArrayList<Int>? = null

        init {
            val cacheTtlNames = HashMap<Int, Int>()
            cacheTtlNames[0] = R.string.keystore_ttl_lock_screen
            cacheTtlNames[60 * 5] = R.string.keystore_ttl_five_minutes
            cacheTtlNames[60 * 30] = R.string.keystore_ttl_thirty_minutes
            cacheTtlNames[60 * 60] = R.string.keystore_ttl_one_hour
            cacheTtlNames[60 * 60 * 6] = R.string.keystore_ttl_six_hours
            cacheTtlNames[60 * 60 * 24] = R.string.keystore_ttl_one_day
            cacheTtlNames[Integer.MAX_VALUE] = R.string.keystore_ttl_forever
            TTL_NAMES = Collections.unmodifiableMap(cacheTtlNames)
            TTLS = ArrayList(TTL_NAMES!!.keys)
            Collections.sort(TTLS!!)
        }
    }
}
