package io.oversec.one.crypto.ui.util

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import io.oversec.one.crypto.R

class TooltipBackgroundDrawable(context: Context) : Drawable() {
    private val mStroke: Paint

    private val mPaint: Paint
    private val mRect: RectF
    private val mPath: Path

    private val mArrowRatio: Float
    private val mCornerRoundness: Float

    private var mPadding = 0
    private var mArrowWeight = 0
    private var mArrowSide: ARROW_SIDE? = null
    private var mArrowPos: Int = 0

    enum class ARROW_SIDE {
        LEFT, RIGHT, TOP, BOTTOM, NONE
    }

    init {
        val backgroundColor = ContextCompat.getColor(context, R.color.colorTooltipBG)
        val borderColor = ContextCompat.getColor(context, R.color.colorTooltipBorder)

        this.mCornerRoundness = dipToPixels(context, CORNER_RADIUS_DEFAULT_DP).toFloat()
        this.mArrowRatio = ARROW_RATIO_DEFAULT

        this.mRect = RectF()

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = backgroundColor
        mPaint.style = Paint.Style.FILL

        mStroke = Paint(Paint.ANTI_ALIAS_FLAG)
        mStroke.color = borderColor
        mStroke.style = Paint.Style.STROKE
        mStroke.strokeWidth = dipToPixels(context, STROKE_WIDTH_DEFAULT_DP).toFloat()

        mPath = Path()
    }

    fun dipToPixels(ctx: Context, dip: Int): Int {
        val r = ctx.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dip.toFloat(),
            r.displayMetrics
        ).toInt()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(mPath, mPaint)
        canvas.drawPath(mPath, mStroke)
    }

    fun setAnchor(ARROWSIDE: ARROW_SIDE, padding: Int, position: Int) {
        if (ARROWSIDE != this.mArrowSide || padding != this.mPadding || position != this.mArrowPos) {
            this.mArrowSide = ARROWSIDE
            this.mPadding = padding
            this.mArrowPos = position
            this.mArrowWeight = (padding.toFloat() / mArrowRatio).toInt()

            val bounds = bounds
            if (!bounds.isEmpty) {
                calculatePath(getBounds())
                invalidateSelf()
            }
        }
    }

    internal fun calculatePath(outBounds: Rect) {
        val left = outBounds.left + mPadding
        val top = outBounds.top + mPadding
        val right = outBounds.right - mPadding
        val bottom = outBounds.bottom - mPadding


        if (null != mArrowSide) {
            calculatePathWithGravity(outBounds, left, top, right, bottom)
        } else {
            mRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            mPath.addRoundRect(mRect, mCornerRoundness, mCornerRoundness, Path.Direction.CW)
        }
    }

    private fun calculatePathWithGravity(
        outBounds: Rect, left: Int, top: Int, right: Int, bottom: Int
    ) {

        mPath.reset()

        mPath.moveTo(left + mCornerRoundness, top.toFloat())

        if (mArrowSide == ARROW_SIDE.TOP) {
            val arrowPointX = (outBounds.left + outBounds.width() * mArrowPos / 100).toFloat()
            mPath.lineTo(left + arrowPointX - mArrowWeight, top.toFloat())
            mPath.lineTo(left + arrowPointX, outBounds.top.toFloat())
            mPath.lineTo(left.toFloat() + arrowPointX + mArrowWeight.toFloat(), top.toFloat())
        }


        mPath.lineTo(right - mCornerRoundness, top.toFloat())
        mPath.quadTo(right.toFloat(), top.toFloat(), right.toFloat(), top + mCornerRoundness)

        if (mArrowSide == ARROW_SIDE.RIGHT) {
            val arrowPointY = (outBounds.top + outBounds.height() * mArrowPos / 100).toFloat()
            mPath.lineTo(right.toFloat(), top + arrowPointY - mArrowWeight)
            mPath.lineTo(outBounds.right.toFloat(), top + arrowPointY)
            mPath.lineTo(right.toFloat(), top.toFloat() + arrowPointY + mArrowWeight.toFloat())
        }


        mPath.lineTo(right.toFloat(), bottom - mCornerRoundness)
        mPath.quadTo(right.toFloat(), bottom.toFloat(), right - mCornerRoundness, bottom.toFloat())

        if (mArrowSide == ARROW_SIDE.BOTTOM) {
            val arrowPointX = (outBounds.left + outBounds.width() * mArrowPos / 100).toFloat()
            mPath.lineTo(left.toFloat() + arrowPointX + mArrowWeight.toFloat(), bottom.toFloat())
            mPath.lineTo(left + arrowPointX, outBounds.bottom.toFloat())
            mPath.lineTo(left + arrowPointX - mArrowWeight, bottom.toFloat())
        }

        mPath.lineTo(left + mCornerRoundness, bottom.toFloat())
        mPath.quadTo(left.toFloat(), bottom.toFloat(), left.toFloat(), bottom - mCornerRoundness)

        if (mArrowSide == ARROW_SIDE.LEFT) {
            val arrowPointY = (outBounds.top + outBounds.height() * mArrowPos / 100).toFloat()
            mPath.lineTo(left.toFloat(), top.toFloat() + arrowPointY + mArrowWeight.toFloat())
            mPath.lineTo(outBounds.left.toFloat(), top + arrowPointY)
            mPath.lineTo(left.toFloat(), top + arrowPointY - mArrowWeight)
        }

        mPath.lineTo(left.toFloat(), top + mCornerRoundness)
        mPath.quadTo(left.toFloat(), top.toFloat(), left + mCornerRoundness, top.toFloat())
    }


    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        calculatePath(bounds)
    }


    override fun getAlpha(): Int {
        return mPaint.alpha
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha

    }

    override fun setColorFilter(cf: ColorFilter?) {}

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    companion object {
        const val ARROW_RATIO_DEFAULT = 1.5f
        private const val CORNER_RADIUS_DEFAULT_DP = 5
        private const val STROKE_WIDTH_DEFAULT_DP = 3
    }


}
