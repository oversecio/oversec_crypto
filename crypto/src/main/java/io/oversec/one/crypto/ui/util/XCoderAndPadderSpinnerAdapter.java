package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.encoding.XCoderAndPadder;

import java.util.List;


public class XCoderAndPadderSpinnerAdapter extends ArrayAdapter<XCoderAndPadder> implements SpinnerAdapter {

    public XCoderAndPadderSpinnerAdapter(Context context, List<XCoderAndPadder> items) {
        super(context, R.layout.listitem_padding, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        } else {
            view = convertView;
        }

        TextView tv = (TextView) view.findViewById(android.R.id.text1);

        XCoderAndPadder xCoderAndPadder = getItem(position);

        tv.setText(xCoderAndPadder.getLabel());


        return view;

    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_padding, parent, false);
        } else {
            view = convertView;
        }

        TextView tv = (TextView) view.findViewById(R.id.tv_title);
        TextView tvExample = (TextView) view.findViewById(R.id.tv_example);

        XCoderAndPadder xCoderAndPadder = getItem(position);

        tv.setText(xCoderAndPadder.getLabel());
        tvExample.setText(xCoderAndPadder.getExample().trim());

        return view;
    }

    public int getPositionFor(String coderId, String padderId) {
        for (int i = 0; i < getCount(); i++) {
            if (getItem(i).getXcoder().getId().equals(coderId)) {
                if (getItem(i).getPadderId() == null) {
                    return i;
                }
                if (getItem(i).getPadderId().equals(padderId)) {
                    return i;
                }

            }
        }
        return 0;
    }
}