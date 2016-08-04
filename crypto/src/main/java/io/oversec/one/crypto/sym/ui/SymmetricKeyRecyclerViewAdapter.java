package io.oversec.one.crypto.sym.ui;

import android.app.Fragment;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.sym.SymUtil;
import io.oversec.one.crypto.sym.SymmetricKeyEncrypted;

import java.util.Date;
import java.util.List;

public class SymmetricKeyRecyclerViewAdapter extends RecyclerView.Adapter<SymmetricKeyRecyclerViewAdapter.ViewHolder> {

    public static final int RQ_SHOW_DETAILS = 6666;

    protected final List<SymmetricKeyEncrypted> mKeys;
    private Fragment mFragment;

    public SymmetricKeyRecyclerViewAdapter(Fragment fragment, List<SymmetricKeyEncrypted> items) {
        mKeys = items;
        mFragment = fragment;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sym_listitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Context ctx = holder.mTv2.getContext();

        SymmetricKeyEncrypted key = mKeys.get(position);

        Date confirmDate = key.getConfirmedDate();

        holder.mIvConfirmed.setVisibility(confirmDate == null ? View.GONE : View.VISIBLE);
        holder.mIvUnconfirmed.setVisibility(confirmDate == null ? View.VISIBLE : View.GONE);

        holder.mTv1.setText(key.getName());

        Date cDate = key.getCreatedDate();
        Date now = new Date();
        long diff = now.getTime() - cDate.getTime();
        int days = (int) (diff / (24 * 60 * 60 * 1000));


        TypedValue typedValue = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int color = typedValue.resourceId;

        if (days > 30) { //TODO maybe make configurable
            color = R.color.colorWarning;
        }


        holder.mTv2.setTextColor(ContextCompat.getColor(ctx,color));
        holder.mTv2.setText(ctx.getString(R.string.key_age, DateUtils.getRelativeTimeSpanString(cDate.getTime(), now.getTime(), DateUtils.DAY_IN_MILLIS)));


        SymUtil.applyAvatar(holder.mTvAvatar, key.getName());
    }

    @Override
    public int getItemCount() {
        return mKeys.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTv1, mTv2, mTvAvatar;
        public final ImageView mIvConfirmed;
        public final ImageView mIvUnconfirmed;


        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTv1 = (TextView) view.findViewById(R.id.tv1);
            mTv2 = (TextView) view.findViewById(R.id.tv2);
            mTvAvatar = (TextView) view.findViewById(R.id.tvAvatar);

            mIvConfirmed = (ImageView) view.findViewById(R.id.ivConfirmed);
            mIvUnconfirmed = (ImageView) view.findViewById(R.id.ivUnConfirmed);

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    KeyDetailsActivity.showForResult(mFragment, RQ_SHOW_DETAILS,
                            mKeys.get(getAdapterPosition()).getId());
                }
            });
        }


    }
}
