package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDAdapter;
import io.oversec.one.crypto.R;

public class MaterialTitleBodyAdapter extends ArrayAdapter<MaterialTitleBodyListItem> implements MDAdapter {

    private MaterialDialog dialog;

    public MaterialTitleBodyAdapter(Context context) {
        super(context, R.layout.listitem_title_and_body, android.R.id.title);
    }

    @Override
    public void setDialog(MaterialDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int index, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_title_and_body, parent, false);
        } else {
            view = convertView;
        }


        if (dialog != null) {
            final MaterialTitleBodyListItem item = getItem(index);
            ImageView ic = (ImageView) view.findViewById(R.id.icon);
            if (item.getIcon() != null) {
                ic.setImageDrawable(item.getIcon());
                ic.setPadding(item.getIconPadding(), item.getIconPadding(),
                        item.getIconPadding(), item.getIconPadding());
                ic.getBackground().setColorFilter(item.getBackgroundColor(),
                        PorterDuff.Mode.SRC_ATOP);
            } else {
                ic.setVisibility(View.GONE);
            }
            TextView tv1 = (TextView) view.findViewById(R.id.tv1);
            tv1.setTextColor(dialog.getBuilder().getItemColor());
            tv1.setText(item.getTitle());
            dialog.setTypeface(tv1, dialog.getBuilder().getRegularFont());

            TextView tv2 = (TextView) view.findViewById(R.id.tv2);
            tv2.setTextColor(dialog.getBuilder().getItemColor());
            tv2.setText(item.getBody());
            dialog.setTypeface(tv1, dialog.getBuilder().getRegularFont());
        }
        return view;
    }

//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
//    private boolean isRTL() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
//            return false;
//        Configuration config = getContext().getResources().getConfiguration();
//        return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
//    }
}