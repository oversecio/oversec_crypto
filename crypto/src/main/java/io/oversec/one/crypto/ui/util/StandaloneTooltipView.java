package io.oversec.one.crypto.ui.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import io.oversec.one.crypto.Help;
import io.oversec.one.crypto.R;

public class StandaloneTooltipView extends LinearLayout {
    private static final float DEFAULT_ELEVATION = 3f;
    public static final int DEFAULT_PADDING_DP = 8;
    private TooltipBackgroundDrawable mTooltipBackgroundDrawable;
    private String mMsg;
    private int mArrowSide;
    private int mArrowPos;
    private String mGotItId;
    private String mHelpAlias;

    public StandaloneTooltipView(Context context) {
        super(context);
        initializeViews(context, null);
    }

    public StandaloneTooltipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context, attrs);
    }

    public StandaloneTooltipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeViews(context, attrs);
    }


    private void initializeViews(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.tooltip_standalone, this);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.StandaloneTooltipView,
                0, 0);

        try {
            mMsg = a.getString(R.styleable.StandaloneTooltipView_msg);
            mArrowSide = a.getInteger(R.styleable.StandaloneTooltipView_arrowSide, -1);
            mArrowPos = a.getInteger(R.styleable.StandaloneTooltipView_arrowPos, 50);
            mGotItId = a.getString(R.styleable.StandaloneTooltipView_gotit_id);
            mHelpAlias = a.getString(R.styleable.StandaloneTooltipView_help_alias);
        } finally {
            a.recycle();
        }

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (GotItPreferences.getPreferences(getContext()).isTooltipConfirmed(mGotItId)) {
            setVisibility(View.GONE);
        }

        final int padding = dipToPixels(DEFAULT_PADDING_DP);
        setPadding(padding, padding, padding, padding);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            setElevation(DEFAULT_ELEVATION);
//        }


        mTooltipBackgroundDrawable = new TooltipBackgroundDrawable(getContext());
        findViewById(R.id.content).setBackground(mTooltipBackgroundDrawable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        outline.setRect(padding,padding, view.getWidth()-padding, view.getHeight()-padding);
                    }
                }
            };
            findViewById(R.id.content).setOutlineProvider(viewOutlineProvider);

        }

        if (mArrowSide != -1) {
            mTooltipBackgroundDrawable.setAnchor(TooltipBackgroundDrawable.ARROW_SIDE.values()[mArrowSide], padding, mArrowPos);
        }

        Button mBtGotIt = (Button) this
                .findViewById(R.id.giv_button);
        Button mBtMoreInfo = (Button) this
                .findViewById(R.id.giv_moreinfo);

        mBtGotIt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVisibility(View.GONE);
                GotItPreferences.getPreferences(getContext()).setTooltipConfirmed(mGotItId);
            }
        });

        if (mHelpAlias != null) {
            mBtMoreInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Help.open(getContext(), mHelpAlias);
                }
            });
        } else {
            mBtMoreInfo.setVisibility(View.GONE);
        }

        TextView mTvText = (TextView) findViewById(android.R.id.text1);
        if (mMsg != null) {
            mTvText.setText(mMsg);
        }

    }

    public int dipToPixels(int dip) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    public void setArrowPosition(int i) {
        int padding = dipToPixels(DEFAULT_PADDING_DP);
        mArrowPos = i;
        if (mArrowSide != -1) {
            mTooltipBackgroundDrawable.setAnchor(TooltipBackgroundDrawable.ARROW_SIDE.values()[mArrowSide], padding, mArrowPos);
        }
    }
}
