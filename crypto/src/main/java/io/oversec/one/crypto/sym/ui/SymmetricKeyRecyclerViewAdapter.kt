package io.oversec.one.crypto.sym.ui

import android.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.oversec.one.crypto.R
import io.oversec.one.crypto.sym.SymUtil
import io.oversec.one.crypto.sym.SymmetricKeyEncrypted
import java.util.Date

open class SymmetricKeyRecyclerViewAdapter(
    private val mFragment: Fragment,
    protected val mKeys: List<SymmetricKeyEncrypted>
) : RecyclerView.Adapter<SymmetricKeyRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sym_listitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ctx = holder.mTv2.context

        val key = mKeys[position]

        val confirmDate = key.confirmedDate

        holder.mIvConfirmed.visibility = if (confirmDate == null) View.GONE else View.VISIBLE
        holder.mIvUnconfirmed.visibility = if (confirmDate == null) View.VISIBLE else View.GONE

        holder.mTv1.text = key.name

        val cDate = key.createdDate
        val now = Date()
        val diff = now.time - cDate!!.time
        val days = (diff / (24 * 60 * 60 * 1000)).toInt()


        val typedValue = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        var color = typedValue.resourceId

        if (days > 30) { //TODO maybe make configurable
            color = R.color.colorWarning
        }


        holder.mTv2.setTextColor(ContextCompat.getColor(ctx, color))
        holder.mTv2.text = ctx.getString(
            R.string.key_age,
            DateUtils.getRelativeTimeSpanString(cDate.time, now.time, DateUtils.DAY_IN_MILLIS)
        )


        SymUtil.applyAvatar(holder.mTvAvatar, key.name!!)
    }

    override fun getItemCount(): Int {
        return mKeys.size
    }

    open inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTv1: TextView
        val mTv2: TextView
        val mTvAvatar: TextView
        val mIvConfirmed: ImageView
        val mIvUnconfirmed: ImageView


        init {
            mTv1 = mView.findViewById<View>(R.id.tv1) as TextView
            mTv2 = mView.findViewById<View>(R.id.tv2) as TextView
            mTvAvatar = mView.findViewById<View>(R.id.tvAvatar) as TextView

            mIvConfirmed = mView.findViewById<View>(R.id.ivConfirmed) as ImageView
            mIvUnconfirmed = mView.findViewById<View>(R.id.ivUnConfirmed) as ImageView

            mView.setOnClickListener {
                KeyDetailsActivity.showForResult(
                    mFragment, RQ_SHOW_DETAILS,
                    mKeys[adapterPosition].id
                )
            }
        }
    }

    companion object {
        const val RQ_SHOW_DETAILS = 6666
    }
}
