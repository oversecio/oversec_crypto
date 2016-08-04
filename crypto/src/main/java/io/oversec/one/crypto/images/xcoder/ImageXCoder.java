package io.oversec.one.crypto.images.xcoder;

import android.net.Uri;
import io.oversec.one.crypto.proto.Outer;

import java.io.IOException;

public interface ImageXCoder {

    Outer.Msg parse(Uri uri) throws IOException;

    Uri encode(Outer.Msg msg) throws IOException;
}
