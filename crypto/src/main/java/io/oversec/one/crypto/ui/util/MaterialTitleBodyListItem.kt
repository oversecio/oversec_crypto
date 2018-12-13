package io.oversec.one.crypto.ui.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.annotation.*
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import com.afollestad.materialdialogs.util.DialogUtils

class MaterialTitleBodyListItem private constructor(private val mBuilder: Builder) {

    val icon: Drawable?
        get() = mBuilder.mIcon

    val title: CharSequence?
        get() = mBuilder.mTitle

    val body: CharSequence?
        get() = mBuilder.mBody

    val iconPadding: Int
        get() = mBuilder.mIconPadding

    val backgroundColor: Int
        @ColorInt
        get() = mBuilder.mBackgroundColor

    class Builder(private val mContext: Context) {
        var mIcon: Drawable? = null
        var mTitle: CharSequence? = null
        var mBody: CharSequence? = null
        var mIconPadding: Int = 0
        var mBackgroundColor= Color.parseColor("#BCBCBC")


        fun icon(icon: Drawable?): Builder {
            this.mIcon = icon
            return this
        }

        fun icon(@DrawableRes iconRes: Int): Builder {
            return icon(ContextCompat.getDrawable(mContext, iconRes))
        }

        fun iconPadding(padding: Int): Builder {
            this.mIconPadding = padding
            return this
        }

        fun iconPaddingDp( paddingDp: Int
        ): Builder {
            this.mIconPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, paddingDp.toFloat(),
                mContext.resources.displayMetrics
            ).toInt()
            return this
        }

        fun iconPaddingRes(@DimenRes paddingRes: Int): Builder {
            return iconPadding(mContext.resources.getDimensionPixelSize(paddingRes))
        }

        fun title(content: CharSequence): Builder {
            this.mTitle = content
            return this
        }

        fun body(content: CharSequence): Builder {
            this.mBody = content
            return this
        }

        fun title(@StringRes contentRes: Int): Builder {
            return title(mContext.getString(contentRes))
        }

        fun body(@StringRes contentRes: Int): Builder {
            return body(mContext.getString(contentRes))
        }

        fun backgroundColor(@ColorInt color: Int): Builder {
            this.mBackgroundColor = color
            return this
        }

        fun backgroundColorRes(@ColorRes colorRes: Int): Builder {
            return backgroundColor(DialogUtils.getColor(mContext, colorRes))
        }

        fun backgroundColorAttr(@AttrRes colorAttr: Int): Builder {
            return backgroundColor(DialogUtils.resolveColor(mContext, colorAttr))
        }

        fun build(): MaterialTitleBodyListItem {
            return MaterialTitleBodyListItem(this)
        }
    }
}