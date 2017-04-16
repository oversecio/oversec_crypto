package io.oversec.one.crypto.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.oversec.one.crypto.EncryptionMethod;
import io.oversec.one.crypto.ui.util.StandaloneTooltipView;

public abstract class AbstractEncryptionParamsFragment extends Fragment implements WithHelp {


    private static final String EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME";
    private static final String EXTRA_ISFORTEXT = "EXTRA_ISFORTEXT";

    protected String mPackageName;
    protected boolean mIsForTextEncryption;
    protected View mView;
    protected StandaloneTooltipView mTooltip;
    protected int mArrowPosition;

    protected EncryptionParamsActivityContract mContract;
    protected boolean mHideToolTip;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContract = (EncryptionParamsActivityContract) getActivity();
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mPackageName = bundle.getString(EXTRA_PACKAGENAME);
            mIsForTextEncryption = bundle.getBoolean(EXTRA_ISFORTEXT);
        }

        return mView;
    }


    public void setArgs(String packageName, boolean isForTextEncryption, Bundle state) {
        Bundle bundle = state == null ? new Bundle() : state;
        bundle.putString(EXTRA_PACKAGENAME, packageName);
        bundle.putBoolean(EXTRA_ISFORTEXT, isForTextEncryption);

        setArguments(bundle);
    }

    public void setToolTipPosition(int i) {
        mArrowPosition = i;
        if (mTooltip != null) {
            mTooltip.setArrowPosition(i);
        }
    }

    // public abstract void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);

    public abstract String getTabTitle(Context ctx);

    public abstract EncryptionMethod getMethod();

    public abstract void saveState(Bundle b);

    public void setToolTipVisible(boolean b) {
        mHideToolTip = !b;
    }
}
