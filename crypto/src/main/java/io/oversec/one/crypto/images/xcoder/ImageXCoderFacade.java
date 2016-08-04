package io.oversec.one.crypto.images.xcoder;

import android.content.Context;
import io.oversec.one.crypto.images.xcoder.blackandwhite.BlackAndWhiteImageXCoder;

import java.util.ArrayList;
import java.util.List;

public class ImageXCoderFacade {

    public static List<ImageXCoder> getAll(Context ctx) {
        ArrayList<ImageXCoder> res = new ArrayList<>();
        res.add(new BlackAndWhiteImageXCoder(ctx));
        return res;
    }
}
