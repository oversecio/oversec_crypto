package io.oversec.one.crypto.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.RelativeLayout

import java.util.ArrayList

class SecureRelativeLayout : RelativeLayout {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun requestSendAccessibilityEvent(
        view: View,
        event: AccessibilityEvent
    ): Boolean {
        // Never send accessibility events.
        return false
    }

    @SuppressLint("InlinedApi")
    override fun getImportantForAccessibility(): Int {
        return View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun addChildrenForAccessibility(outChildren: ArrayList<View>) {
        //nothing
    }
}
