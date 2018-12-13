package io.oversec.one.crypto.ui.util

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatEditText
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import io.oversec.one.crypto.R

class EditTextPasswordWithVisibilityToggle : AppCompatEditText {

    private var mIconWidth: Int = 0
    private var mPasswordVisible = true

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (!isInEditMode) {
            init(attrs)
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        if (!isInEditMode) {
            init(attrs)
        }
    }

    private fun init(attrs: AttributeSet) {
        mIconWidth = dipToPixels(18)
        togglePassword()
    }

    fun setPasswordVisible(v: Boolean) {
        mPasswordVisible = v
        updatePasswordVisibility()
    }

    private fun togglePassword() {
        mPasswordVisible = !mPasswordVisible
        updatePasswordVisibility()
    }

    private fun updatePasswordVisibility() {

        if (mPasswordVisible) {
            transformationMethod = HideReturnsTransformationMethod.getInstance()
        } else {
            transformationMethod = PasswordTransformationMethod.getInstance()
        }

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_remove_red_eye_black_18dp)
        var wrap = DrawableCompat.wrap(drawable!!)
        if (mPasswordVisible) {
            DrawableCompat.setTint(wrap, ContextCompat.getColor(context, R.color.colorPrimary))
            DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN)
            wrap = wrap.mutate()
        } else {
            DrawableCompat.setTint(wrap, Color.BLACK)
            DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN)
            wrap = wrap.mutate()
        }
        setCompoundDrawablesWithIntrinsicBounds(null, null, wrap, null)
        compoundDrawablePadding = 10
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt()
            if (x >= width - paddingLeft - mIconWidth && x <= width + mIconWidth - paddingRight) {
                togglePassword()
                event.action = MotionEvent.ACTION_CANCEL
            }
        }
        return super.onTouchEvent(event)
    }

    fun dipToPixels(dip: Int): Int {
        val r = resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dip.toFloat(),
            r.displayMetrics
        ).toInt()
    }

}