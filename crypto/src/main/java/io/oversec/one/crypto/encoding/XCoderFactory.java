package io.oversec.one.crypto.encoding;

import android.content.Context;
import io.oversec.one.crypto.proto.Outer;

import java.util.ArrayList;
import java.util.List;


public class XCoderFactory {
    private static XCoderFactory INSTANCE;

    private List<IXCoder> ALL = new ArrayList<>();

    public final Base64XCoder _Base64XCoder;
    public final ZeroWidthXCoder _ZeroWidthXCoder;
    public final AsciiArmouredGpgXCoder _AsciiArmouredGpgXCoder;


    public static synchronized XCoderFactory getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new XCoderFactory(ctx);
        }
        return INSTANCE;
    }

    private XCoderFactory(Context context) {
        _Base64XCoder = new Base64XCoder(context);
        _ZeroWidthXCoder = new ZeroWidthXCoder(context);
        _AsciiArmouredGpgXCoder = new AsciiArmouredGpgXCoder(context);
        add(_Base64XCoder);
        add(_ZeroWidthXCoder);
        add(_AsciiArmouredGpgXCoder);
    }

    private void add(IXCoder coder) {
        ALL.add(coder);
    }

    public synchronized Outer.Msg decode(String s) {
        for (IXCoder coder : ALL) {
            try {
                Outer.Msg m = coder.decode(s);
                if (m != null) {
                    return m;
                }
            } catch (Exception e) {
                //
            }
        }
        return null;
    }

    public synchronized String getEncodingInfo(String s) {
        for (IXCoder coder : ALL) {
            try {
                Outer.Msg msg = coder.decode(s);
                if (msg != null) {
                    return msg.getMsgDataCase().name() + " (" + coder.getLabel(null) + ")";
                }
            } catch (Exception e) {
                //
            }
        }
        return "N/A";

    }

    public boolean isEncodingCorrupt(String s) {
        for (IXCoder coder : ALL) {
            try {
                coder.decode(s);
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }
}
