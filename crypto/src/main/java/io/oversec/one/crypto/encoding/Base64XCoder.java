package io.oversec.one.crypto.encoding;

import android.content.Context;
import android.util.Base64;
import com.google.protobuf.InvalidProtocolBufferException;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.encoding.pad.AbstractPadder;
import io.oversec.one.crypto.proto.Outer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Base64XCoder extends AbstractXCoder {
    public static final String ID = "base64";

    final static int FLAGS = Base64.NO_WRAP + Base64.NO_PADDING;

    private static final byte[] MAGIC_BYTES = "OSC".getBytes();
    private static final String MAGIC_BYTES_BASE64 = Base64.encodeToString(MAGIC_BYTES, FLAGS);


    public Base64XCoder(Context ctx) {
        super(ctx);
    }

    @Override
    protected String encodeInternal(Outer.Msg msg, AbstractPadder ignore, String packagename) {
        byte[] d = msg.toByteArray();
        byte[] padded = new byte[d.length + MAGIC_BYTES.length];
        System.arraycopy(MAGIC_BYTES, 0, padded, 0, MAGIC_BYTES.length);
        System.arraycopy(d, 0, padded, MAGIC_BYTES.length, d.length);
        return Base64.encodeToString(padded, FLAGS);
    }

    @Override
    public Outer.Msg decode(String data) throws InvalidProtocolBufferException, IllegalArgumentException {


        if (data.length() < MAGIC_BYTES_BASE64.length()) {
            return null;
        }

        for (int i = 0; i < MAGIC_BYTES_BASE64.length(); i++) {
            if (data.charAt(i) != MAGIC_BYTES_BASE64.charAt(i)) {
                return null;
            }
        }

        byte[] buf = Base64.decode(data, FLAGS);

        Outer.Msg res;
        try {
            res = Outer.Msg.parseFrom(new ByteArrayInputStream(buf, MAGIC_BYTES.length, buf.length - MAGIC_BYTES.length));
        } catch (IOException e) {
            throw new InvalidProtocolBufferException(e);
        }
        return res;


    }


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel(AbstractPadder padder) {
        return mCtx.getString(R.string.encoder_base64);
    }

    @Override
    public String getExample(AbstractPadder padder) {
        try {
            return Base64.encodeToString("some example text".getBytes("UTF-8"), FLAGS);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public boolean isTextOnly() {
        return false;
    }
}
