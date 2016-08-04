package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.*;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import com.afollestad.materialdialogs.util.DialogUtils;

public class MaterialTitleBodyListItem {

    private final Builder mBuilder;

    private MaterialTitleBodyListItem(Builder builder) {
        mBuilder = builder;
    }

    public Drawable getIcon() {
        return mBuilder.mIcon;
    }

    public CharSequence getTitle() {
        return mBuilder.mTitle;
    }

    public CharSequence getBody() {
        return mBuilder.mBody;
    }

    public int getIconPadding() {
        return mBuilder.mIconPadding;
    }

    @ColorInt
    public int getBackgroundColor() {
        return mBuilder.mBackgroundColor;
    }

    public static class Builder {

        private final Context mContext;
        protected Drawable mIcon;
        protected CharSequence mTitle;
        protected CharSequence mBody;
        protected int mIconPadding;
        protected int mBackgroundColor;

        public Builder(Context context) {
            mContext = context;
            mBackgroundColor = Color.parseColor("#BCBCBC");
        }

        public Builder icon(Drawable icon) {
            this.mIcon = icon;
            return this;
        }

        public Builder icon(@DrawableRes int iconRes) {
            return icon(ContextCompat.getDrawable(mContext, iconRes));
        }

        public Builder iconPadding(@IntRange(from = 0, to = Integer.MAX_VALUE) int padding) {
            this.mIconPadding = padding;
            return this;
        }

        public Builder iconPaddingDp(@IntRange(from = 0, to = Integer.MAX_VALUE) int paddingDp) {
            this.mIconPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingDp,
                    mContext.getResources().getDisplayMetrics());
            return this;
        }

        public Builder iconPaddingRes(@DimenRes int paddingRes) {
            return iconPadding(mContext.getResources().getDimensionPixelSize(paddingRes));
        }

        public Builder title(CharSequence content) {
            this.mTitle = content;
            return this;
        }

        public Builder body(CharSequence content) {
            this.mBody = content;
            return this;
        }

        public Builder title(@StringRes int contentRes) {
            return title(mContext.getString(contentRes));
        }

        public Builder body(@StringRes int contentRes) {
            return body(mContext.getString(contentRes));
        }

        public Builder backgroundColor(@ColorInt int color) {
            this.mBackgroundColor = color;
            return this;
        }

        public Builder backgroundColorRes(@ColorRes int colorRes) {
            return backgroundColor(DialogUtils.getColor(mContext, colorRes));
        }

        public Builder backgroundColorAttr(@AttrRes int colorAttr) {
            return backgroundColor(DialogUtils.resolveColor(mContext, colorAttr));
        }

        public MaterialTitleBodyListItem build() {
            return new MaterialTitleBodyListItem(this);
        }
    }


}