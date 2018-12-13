package io.oversec.one.crypto.ui.util

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDAdapter
import io.oversec.one.crypto.R

class MaterialTitleBodyAdapter(context: Context) : ArrayAdapter<MaterialTitleBodyListItem>(
    context,
    R.layout.listitem_title_and_body,
    android.R.id.title
), MDAdapter {

    private var dialog: MaterialDialog? = null

    override fun setDialog(dialog: MaterialDialog) {
        this.dialog = dialog
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(index: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        if (convertView == null) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.listitem_title_and_body, parent, false)
        } else {
            view = convertView
        }


        if (dialog != null) {
            val item = getItem(index)
            val ic = view.findViewById<View>(R.id.icon) as ImageView
            if (item!!.icon != null) {
                ic.setImageDrawable(item.icon)
                ic.setPadding(
                    item.iconPadding, item.iconPadding,
                    item.iconPadding, item.iconPadding
                )
                ic.background.setColorFilter(
                    item.backgroundColor,
                    PorterDuff.Mode.SRC_ATOP
                )
            } else {
                ic.visibility = View.GONE
            }
            val tv1 = view.findViewById<View>(R.id.tv1) as TextView
            tv1.setTextColor(dialog!!.builder.itemColor)
            tv1.text = item.title
            dialog!!.setTypeface(tv1, dialog!!.builder.regularFont)

            val tv2 = view.findViewById<View>(R.id.tv2) as TextView
            tv2.setTextColor(dialog!!.builder.itemColor)
            tv2.text = item.body
            dialog!!.setTypeface(tv1, dialog!!.builder.regularFont)
        }
        return view
    }
}