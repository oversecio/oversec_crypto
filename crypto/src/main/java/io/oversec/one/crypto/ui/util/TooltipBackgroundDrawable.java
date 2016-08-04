package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import io.oversec.one.crypto.R;

public class TooltipBackgroundDrawable extends Drawable {
    public static final float ARROW_RATIO_DEFAULT = 1.5f;
    private static final int CORNER_RADIUS_DEFAULT_DP = 5;
    private static final int STROKE_WIDTH_DEFAULT_DP = 2;
    private final Paint mStroke;


    private Paint mPaint;
    private RectF mRect;
    private Path mPath;


    private float mArrowRatio;
    private float mCornerRoundness;

    private int mPadding = 0;
    private int mArrowWeight = 0;
    private ARROW_SIDE mArrowSide;
    private int mArrowPos;

    public enum ARROW_SIDE {
        LEFT, RIGHT, TOP, BOTTOM, NONE
    }

    public TooltipBackgroundDrawable(final Context context) {


        final int backgroundColor = ContextCompat.getColor(context,R.color.tooltipBG);
        final int borderColor = ContextCompat.getColor(context,R.color.tooltipBorder);

        this.mCornerRoundness = dipToPixels(context, CORNER_RADIUS_DEFAULT_DP);
        this.mArrowRatio = ARROW_RATIO_DEFAULT;

        this.mRect = new RectF();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(backgroundColor);
        mPaint.setStyle(Paint.Style.FILL);

        mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStroke.setColor(borderColor);
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setStrokeWidth(dipToPixels(context, STROKE_WIDTH_DEFAULT_DP));

        mPath = new Path();
    }

    public int dipToPixels(Context ctx, int dip) {
        Resources r = ctx.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
        canvas.drawPath(mPath, mStroke);
    }

    public void setAnchor(final ARROW_SIDE ARROWSIDE, int padding, int position) {
        if (ARROWSIDE != this.mArrowSide || padding != this.mPadding || position != this.mArrowPos) {
            this.mArrowSide = ARROWSIDE;
            this.mPadding = padding;
            this.mArrowPos = position;
            this.mArrowWeight = (int) ((float) padding / mArrowRatio);

            final Rect bounds = getBounds();
            if (!bounds.isEmpty()) {
                calculatePath(getBounds());
                invalidateSelf();
            }
        }
    }

    void calculatePath(Rect outBounds) {
        int left = outBounds.left + mPadding;
        int top = outBounds.top + mPadding;
        int right = outBounds.right - mPadding;
        int bottom = outBounds.bottom - mPadding;


        if (null != mArrowSide) {
            calculatePathWithGravity(outBounds, left, top, right, bottom);
        } else {
            mRect.set(left, top, right, bottom);
            mPath.addRoundRect(mRect, mCornerRoundness, mCornerRoundness, Path.Direction.CW);
        }
    }

    private void calculatePathWithGravity(
            final Rect outBounds, final int left, final int top, final int right, final int bottom) {

        mPath.reset();

        mPath.moveTo(left + mCornerRoundness, top);

        if (mArrowSide == ARROW_SIDE.TOP) {
            float arrowPointX = outBounds.left + outBounds.width() * mArrowPos / 100;
            mPath.lineTo(left + arrowPointX - mArrowWeight, top);
            mPath.lineTo(left + arrowPointX, outBounds.top);
            mPath.lineTo(left + arrowPointX + mArrowWeight, top);
        }


        mPath.lineTo(right - mCornerRoundness, top);
        mPath.quadTo(right, top, right, top + mCornerRoundness);

        if (mArrowSide == ARROW_SIDE.RIGHT) {
            float arrowPointY = outBounds.top + outBounds.height() * mArrowPos / 100;
            mPath.lineTo(right, top + arrowPointY - mArrowWeight);
            mPath.lineTo(outBounds.right, top + arrowPointY);
            mPath.lineTo(right, top + arrowPointY + mArrowWeight);
        }


        mPath.lineTo(right, bottom - mCornerRoundness);
        mPath.quadTo(right, bottom, right - mCornerRoundness, bottom);

        if (mArrowSide == ARROW_SIDE.BOTTOM) {
            float arrowPointX = outBounds.left + outBounds.width() * mArrowPos / 100;
            mPath.lineTo(left + arrowPointX + mArrowWeight, bottom);
            mPath.lineTo(left + arrowPointX, outBounds.bottom);
            mPath.lineTo(left + arrowPointX - mArrowWeight, bottom);
        }

        mPath.lineTo(left + mCornerRoundness, bottom);
        mPath.quadTo(left, bottom, left, bottom - mCornerRoundness);

        if (mArrowSide == ARROW_SIDE.LEFT) {
            float arrowPointY = outBounds.top + outBounds.height() * mArrowPos / 100;
            mPath.lineTo(left, top + arrowPointY + mArrowWeight);
            mPath.lineTo(outBounds.left, top + arrowPointY);
            mPath.lineTo(left, top + arrowPointY - mArrowWeight);
        }

        mPath.lineTo(left, top + mCornerRoundness);
        mPath.quadTo(left, top, left + mCornerRoundness, top);
    }


    @Override
    protected void onBoundsChange(final Rect bounds) {
        super.onBoundsChange(bounds);
        calculatePath(bounds);
    }


    @Override
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setAlpha(final int alpha) {
        mPaint.setAlpha(alpha);

    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }


}
