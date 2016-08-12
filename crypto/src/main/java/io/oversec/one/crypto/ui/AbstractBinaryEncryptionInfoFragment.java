package io.oversec.one.crypto.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.oversec.one.crypto.AbstractCryptoHandler;
import io.oversec.one.crypto.BaseDecryptResult;
import io.oversec.one.crypto.CryptoHandlerFacade;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.images.xcoder.ImageXCoder;
import io.oversec.one.crypto.proto.Outer;

public abstract class AbstractBinaryEncryptionInfoFragment extends Fragment {


    private static final String EXTRA_PACKAGENAME = "EXTRA_PACKAGENAME";
    protected String mPackageName;
    protected ViewGroup mGrid;

    protected View mView;

    private Outer.Msg mTmpMsg;
    private BaseDecryptResult mTmpRes;
    private ImageXCoder mTmpCoder;
    private TextView mTvCoder;
    private TextView mLblCoder;
    private TextView mTvMeth;
    private TextView mLblMeth;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mGrid = (ViewGroup) mView.findViewById(R.id.grid);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mPackageName = bundle.getString(EXTRA_PACKAGENAME);
        }

        mTvCoder = (TextView) mView.findViewById(R.id.tv_coder);
        mLblCoder = (TextView) mView.findViewById(R.id.lbl_coder);

        mTvMeth = (TextView) mView.findViewById(R.id.tv_meth);
        mLblMeth = (TextView) mView.findViewById(R.id.lbl_meth);

        mTvCoder.setVisibility(View.GONE);
        mLblCoder.setVisibility(View.GONE);

        //TODO move to a clean fragment impl with  args and state
        if (mTmpMsg != null) {
            handleSetData(mTmpMsg, mTmpRes, mTmpCoder);
            mTmpMsg = null;
            mTmpRes = null;
            mTmpCoder = null;

        }

        return mView;
    }

    //TODO move to a clean fragment impl with  args and state
    public void setData(Outer.Msg msg, BaseDecryptResult tdr, ImageXCoder coder) {
        if (mView != null) {
            handleSetData(msg, tdr, coder);
        } else {
            mTmpMsg = msg;
            mTmpRes = tdr;
            mTmpCoder = coder;
        }


    }

    protected void handleSetData(Outer.Msg msg, BaseDecryptResult tdr, ImageXCoder coder) {
        //mTvCoder.setText(coder.getClass().getSimpleName());
        AbstractCryptoHandler encH = CryptoHandlerFacade.getInstance(getActivity()).getCryptoHandler(tdr);
        if (encH != null) {
            mTvMeth.setText(encH.getDisplayEncryptionMethod());
            mTvMeth.setVisibility(View.VISIBLE);
            mLblMeth.setVisibility(View.VISIBLE);

        }
    }


    public void setArgs(String packageName) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PACKAGENAME, packageName);
        setArguments(bundle);
    }


}
