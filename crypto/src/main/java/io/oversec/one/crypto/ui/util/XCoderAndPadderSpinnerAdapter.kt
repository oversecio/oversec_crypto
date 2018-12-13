package io.oversec.one.crypto.ui.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import android.widget.TextView
import io.oversec.one.crypto.R
import io.oversec.one.crypto.encoding.XCoderAndPadder

class XCoderAndPadderSpinnerAdapter(context: Context, items: List<XCoderAndPadder>) :
    ArrayAdapter<XCoderAndPadder>(context, R.layout.listitem_padding, items), SpinnerAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView?:LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        val tv = view.findViewById<View>(android.R.id.text1) as TextView

        val xCoderAndPadder = getItem(position)

        tv.text = xCoderAndPadder!!.label

        return view

    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView?: LayoutInflater.from(context).inflate(R.layout.listitem_padding, parent, false)

        val tv = view.findViewById<View>(R.id.tv_title) as TextView
        val tvExample = view.findViewById<View>(R.id.tv_example) as TextView

        val xCoderAndPadder = getItem(position)

        tv.text = xCoderAndPadder!!.label
        tvExample.text = xCoderAndPadder.example.trim { it <= ' ' }

        return view
    }

    fun getPositionFor(coderId: String, padderId: String): Int {
        for (i in 0 until count) {
            if (getItem(i)!!.xcoder.id == coderId) {
                if (getItem(i)!!.padderId == null) {
                    return i
                }
                if (getItem(i)!!.padderId == padderId) {
                    return i
                }
            }
        }
        return 0
    }
}