package io.oversec.one.crypto.ui.util

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import io.oversec.one.crypto.Help
import io.oversec.one.crypto.R

class StandaloneTooltipView : LinearLayout {
    private var mTooltipBackgroundDrawable: TooltipBackgroundDrawable? = null
    private var mMsg: String? = null
    private var mArrowSide: Int = 0
    private var mArrowPos: Int = 0
    private var mGotItId: String? = null
    private var mHelpAlias: String? = null

    constructor(context: Context) : super(context) {
        initializeViews(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initializeViews(context, attrs)
    }


    private fun initializeViews(context: Context, attrs: AttributeSet?) {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.tooltip_standalone, this)

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StandaloneTooltipView,
            0, 0
        )

        try {
            mMsg = a.getString(R.styleable.StandaloneTooltipView_msg)
            mArrowSide = a.getInteger(R.styleable.StandaloneTooltipView_arrowSide, -1)
            mArrowPos = a.getInteger(R.styleable.StandaloneTooltipView_arrowPos, 50)
            mGotItId = a.getString(R.styleable.StandaloneTooltipView_gotit_id)
            mHelpAlias = a.getString(R.styleable.StandaloneTooltipView_help_alias)
        } finally {
            a.recycle()
        }

    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (GotItPreferences.getPreferences(context).isTooltipConfirmed(mGotItId)) {
            visibility = View.GONE
        }

        val padding = dipToPixels(DEFAULT_PADDING_DP)
        setPadding(padding, padding, padding, padding)
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //            setElevation(DEFAULT_ELEVATION);
        //        }


        mTooltipBackgroundDrawable = TooltipBackgroundDrawable(context)
        findViewById<View>(R.id.content).background = mTooltipBackgroundDrawable

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val viewOutlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        outline.setRect(
                            padding,
                            padding,
                            view.width - padding,
                            view.height - padding
                        )
                    }
                }
            }
            findViewById<View>(R.id.content).outlineProvider = viewOutlineProvider

        }

        if (mArrowSide != -1) {
            mTooltipBackgroundDrawable!!.setAnchor(
                TooltipBackgroundDrawable.ARROW_SIDE.values()[mArrowSide],
                padding,
                mArrowPos
            )
        }

        val mBtGotIt = this
            .findViewById<View>(R.id.giv_button) as Button
        val mBtMoreInfo = this
            .findViewById<View>(R.id.giv_moreinfo) as Button

        mBtGotIt.setOnClickListener {
            visibility = View.GONE
            GotItPreferences.getPreferences(context).setTooltipConfirmed(mGotItId)
        }

        if (mHelpAlias != null) {
            mBtMoreInfo.setOnClickListener { Help.open(context, mHelpAlias) }
        } else {
            mBtMoreInfo.visibility = View.GONE
        }

        val mTvText = findViewById<View>(android.R.id.text1) as TextView
        if (mMsg != null) {
            mTvText.text = mMsg
        }

    }

    fun dipToPixels(dip: Int): Int {
        val r = resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dip.toFloat(),
            r.displayMetrics
        ).toInt()
    }

    fun setArrowPosition(i: Int) {
        val padding = dipToPixels(DEFAULT_PADDING_DP)
        mArrowPos = i
        if (mArrowSide != -1) {
            mTooltipBackgroundDrawable!!.setAnchor(
                TooltipBackgroundDrawable.ARROW_SIDE.values()[mArrowSide],
                padding,
                mArrowPos
            )
        }
    }

    companion object {
        const val DEFAULT_PADDING_DP = 8
    }
}
