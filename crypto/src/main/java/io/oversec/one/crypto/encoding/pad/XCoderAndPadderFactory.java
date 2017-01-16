package io.oversec.one.crypto.encoding.pad;

import android.content.Context;
import io.oversec.one.common.CoreContract;
import io.oversec.one.crypto.Issues;
import io.oversec.one.crypto.encoding.XCoderAndPadder;
import io.oversec.one.crypto.encoding.XCoderFactory;

import java.util.ArrayList;
import java.util.List;

public class XCoderAndPadderFactory {


    private static XCoderAndPadderFactory INSTANCE;

    private final Context mCtx;
    private final CoreContract mCore;
    private ArrayList<XCoderAndPadder> mAll = new ArrayList<>();
    private ArrayList<XCoderAndPadder> mSym;
    private ArrayList<XCoderAndPadder> mGpg;
    private ArrayList<XCoderAndPadder> mSymExcludeInvisible;
    private ArrayList<XCoderAndPadder> mGpgExcludeInvisible;
    private XCoderAndPadder mManualZeroWidthXcoder;

    public static synchronized XCoderAndPadderFactory getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new XCoderAndPadderFactory(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    private XCoderAndPadderFactory(Context ctx) {
        mCtx = ctx;
        mCore = CoreContract.getInstance();
        reload();


    }

    public void reload() {
        mAll.clear();
        mManualZeroWidthXcoder = addZeroWidthXcoder(new ManualPadder(mCtx));
        CoreContract contract = CoreContract.getInstance();

        List<PadderContent> allFromDb = contract.getAllPaddersSorted();
        for (PadderContent pc : allFromDb) {
            addZeroWidthXcoder(new GutenbergPadder(mCtx, pc.getName(), pc.getContent()));
        }


        mSym = new ArrayList<>(mAll);
        mGpg = new ArrayList<>(mAll);
        mSymExcludeInvisible = new ArrayList<>();
        mGpgExcludeInvisible = new ArrayList<>();

        XCoderAndPadder l = new XCoderAndPadder(XCoderFactory.getInstance(mCtx)._Base64XCoder, null);
        mSym.add(0, l);
        mSymExcludeInvisible.add(0, l);
        mAll.add(l);

        XCoderAndPadder l2 = new XCoderAndPadder(XCoderFactory.getInstance(mCtx)._AsciiArmouredGpgXCoder, null);
        mGpg.add(0, l2);
        mGpgExcludeInvisible.add(0, l2);
        mAll.add(l2);
    }

    private XCoderAndPadder addZeroWidthXcoder(AbstractPadder padder) {
        XCoderAndPadder res = new XCoderAndPadder(XCoderFactory.getInstance(mCtx)._ZeroWidthXCoder, padder);
        mAll.add(res);
        return res;
    }

    public ArrayList<XCoderAndPadder> getSym(String packagename) {
        ArrayList<XCoderAndPadder> res = new ArrayList<>(Issues.cantHandleInvisibleEncoding(packagename) ? mSymExcludeInvisible : mSym);
        if (mCore.isDbSpreadInvisibleEncoding(packagename)) {
            //spreaded doesn't work with manual padding
            res.remove(mManualZeroWidthXcoder);
        }

        return res;
    }


    public ArrayList<XCoderAndPadder> getGpg(String packagename) {
        ArrayList<XCoderAndPadder> res = new ArrayList<>(Issues.cantHandleInvisibleEncoding(packagename) ? mGpgExcludeInvisible:mGpg);
        if (mCore.isDbSpreadInvisibleEncoding(packagename)) {
            //spreaded doesn't work with manual padding
            res.remove(mManualZeroWidthXcoder);
        }

        return res;
    }

    public XCoderAndPadder get(String coderId, String padderId) {

        for (XCoderAndPadder x : mAll) {
            if (x.getXcoder().getId().equals(coderId)) {
                if (padderId == null || x.getPadder() == null) {
                    return x;
                } else {
                    if (x.getPadder().getId().equals(padderId)) {
                        return x;
                    }
                }
            }
        }

        return null;
    }
}