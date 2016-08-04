package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import io.oversec.one.crypto.R;


public class EditTextPasswordWithVisibilityToggle extends AppCompatEditText {

    private int mIconWidth;
    private boolean mPasswordVisible = true;

    public EditTextPasswordWithVisibilityToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            init(attrs);
        }
    }

    public EditTextPasswordWithVisibilityToggle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            init(attrs);
        }
    }

    private void init(AttributeSet attrs) {
        mIconWidth = dipToPixels(18);
        togglePassword();
    }

    public void setPasswordVisible(boolean v) {
        mPasswordVisible = v;
        updatePasswordVisibility();
    }

    private void togglePassword() {
        mPasswordVisible = !mPasswordVisible;
        updatePasswordVisibility();
    }

    private void updatePasswordVisibility() {

        if (mPasswordVisible) {
            setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
        }


        Drawable drawable = ContextCompat.getDrawable(getContext(),R.drawable.ic_remove_red_eye_black_18dp);
        Drawable wrap = DrawableCompat.wrap(drawable);
        if (mPasswordVisible) {

            DrawableCompat.setTint(wrap, ContextCompat.getColor(getContext(), R.color.colorPrimary));
            DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN);
            wrap = wrap.mutate();
        } else {
            DrawableCompat.setTint(wrap, Color.BLACK);
            DrawableCompat.setTintMode(wrap, PorterDuff.Mode.SRC_IN);
            wrap = wrap.mutate();
        }
        setCompoundDrawablesWithIntrinsicBounds(null, null, wrap, null);
        setCompoundDrawablePadding(10);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            final int x = (int) event.getX();


            if (x >= (getWidth() - getPaddingLeft()) - mIconWidth && x <= getWidth() + mIconWidth - getPaddingRight()) {
                togglePassword();
                event.setAction(MotionEvent.ACTION_CANCEL);
            }

        }
        return super.onTouchEvent(event);
    }


    public int dipToPixels(int dip) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

}