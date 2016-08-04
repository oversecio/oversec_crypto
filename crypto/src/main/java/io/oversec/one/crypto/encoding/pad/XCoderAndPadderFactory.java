package io.oversec.one.crypto.encoding.pad;

import android.content.Context;
import io.oversec.one.common.CoreContract;
import io.oversec.one.crypto.encoding.XCoderAndPadder;
import io.oversec.one.crypto.encoding.XCoderFactory;

import java.util.ArrayList;
import java.util.List;

public class XCoderAndPadderFactory {


    private static XCoderAndPadderFactory INSTANCE;

    private final Context mCtx;
    private ArrayList<XCoderAndPadder> mAll = new ArrayList<>();
    private ArrayList<XCoderAndPadder> mSym;
    private ArrayList<XCoderAndPadder> mGpg;

    public static synchronized XCoderAndPadderFactory getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new XCoderAndPadderFactory(ctx);
        }
        return INSTANCE;
    }

    private XCoderAndPadderFactory(Context ctx) {
        mCtx = ctx;
        reload();


    }

    public void reload() {
        mAll.clear();
        addZeroWidthXcoder(new ManualPadder(mCtx));


        CoreContract contract = CoreContract.getInstance();

        List<PadderContent> allFromDb = contract.getAllPaddersSorted();
        for (PadderContent pc : allFromDb) {
            addZeroWidthXcoder(new GutenbergPadder(mCtx, pc.getName(), pc.getContent()));
        }


        mSym = new ArrayList<>(mAll);
        mGpg = new ArrayList<>(mAll);

        XCoderAndPadder l = new XCoderAndPadder(XCoderFactory.getInstance(mCtx)._Base64XCoder, null);
        mSym.add(0, l);
        mAll.add(l);


        XCoderAndPadder l2 = new XCoderAndPadder(XCoderFactory.getInstance(mCtx)._AsciiArmouredGpgXCoder, null);
        mGpg.add(0, l2);
        mAll.add(l2);
    }

    private void addZeroWidthXcoder(AbstractPadder padder) {
        mAll.add(new XCoderAndPadder(XCoderFactory.getInstance(mCtx)._ZeroWidthXCoder, padder));
    }


    public ArrayList<XCoderAndPadder> getSym() {

        return mSym;
    }


    public ArrayList<XCoderAndPadder> getGpg() {

        return mGpg;
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