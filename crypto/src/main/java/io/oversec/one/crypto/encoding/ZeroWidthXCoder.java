package io.oversec.one.crypto.encoding;

import android.content.Context;
import android.util.SparseIntArray;
import io.oversec.one.common.CoreContract;
import io.oversec.one.crypto.R;
import io.oversec.one.crypto.encoding.pad.AbstractPadder;
import io.oversec.one.crypto.proto.Outer;
import roboguice.util.Ln;

import java.io.IOException;
import java.util.Arrays;


public class ZeroWidthXCoder extends AbstractXCoder {
    public static final String ID = "zwidthbmp";

    private static final String MAGIC = "OSC";
    private static final byte[] MAGIC_BYTES = MAGIC.getBytes();
    private static final int SPREAD = 30; // Orca / Instagram fail at more than 35 consecutive inivisvle chars, let's play it safe and use 30


    private static char[][] MAPPING = new char[256][];
    private static SparseIntArray REVERSE_MAPPING = new SparseIntArray();

    private static String MAGIC_BYTES_ZEROWIDTH;



    static {
        int idx = 0;
        int start = 0xFE00;
        int end = 0xFE0F;
        for (int i = start; i <= end; i++) {
            MAPPING[idx] = Character.toChars(i);
            idx++;
        }
        int start2 = 0xE0100;
        int end2 = 0xE01EF;
        for (int i = start2; i <= end2; i++) {
            MAPPING[idx] = Character.toChars(i);
            idx++;
        }


        for (int i = 0; i < MAPPING.length; i++) {
            if (MAPPING[i].length == 1) {
                REVERSE_MAPPING.put(MAPPING[i][0], i);
            } else if (MAPPING[i].length == 2) {
                REVERSE_MAPPING.put(Character.toCodePoint(MAPPING[i][0], MAPPING[i][1]), i);
            } else {
                throw new IllegalArgumentException();
            }

        }
    }

    static {

        try {
            ExtendedPaneStringMapperOutputStream os = new ExtendedPaneStringMapperOutputStream(MAPPING, null,0);
            os.write(MAGIC_BYTES);
            MAGIC_BYTES_ZEROWIDTH = os.getEncoded();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private final CoreContract mCore;


    public ZeroWidthXCoder(Context context) {
        super(context);
        mCore = CoreContract.getInstance();
    }


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel(AbstractPadder padder) {
        return mCtx.getString(R.string.encoder_zerowidth) + (padder == null ? "" : (" (" + padder.getLabel() + ")"));

    }

    @Override
    public String getExample(AbstractPadder padder) {
        return padder == null ? "" : padder.getExample();
    }

    @Override
    public boolean isTextOnly() {
        return false;
    }

    @Override
    protected String encodeInternal(Outer.Msg msg, AbstractPadder padder, String packagename) throws IOException {
        int spread = mCore.isDbSpreadInvisibleEncoding(packagename)?SPREAD:0;
        ExtendedPaneStringMapperOutputStream smos = new ExtendedPaneStringMapperOutputStream(MAPPING,padder,spread);
        smos.write(MAGIC_BYTES);
        msg.writeDelimitedTo(smos);
        smos.flush();
        return smos.getEncoded();
    }

    @Override
    public Outer.Msg decode(String data) throws IOException {
        if (data.length() < MAGIC_BYTES_ZEROWIDTH.length()) { // small optimization, check we have at least some bytes
            return null;
        }

        //try to find magic
        int p = data.indexOf(MAGIC_BYTES_ZEROWIDTH);
        if (p<0) {
            return null;
        }

        try {
            ExtendedPaneStringMapperInputStream smis = new ExtendedPaneStringMapperInputStream(data.substring(p), REVERSE_MAPPING);
            byte[] buf = new byte[MAGIC_BYTES.length];
            //noinspection ResultOfMethodCallIgnored
            smis.read(buf);
            if (!Arrays.equals(buf, MAGIC_BYTES)) {
                return null;
            }
            Outer.Msg res = Outer.Msg.parseDelimitedFrom(smis);
            return res;
        } catch (ExtendedPaneStringMapperInputStream.UnmappedCodepointException e) {
            if (e.getOffset() <= 1) { //this simply means that the string is not encoded by us
                return null;
            } else {
                throw e;
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new IOException(e);
        }
    }


    public static String stripInvisible(String s) {
        int off = 0;
        try {
            for (;;) {
                int cp = s.codePointAt(off);
                int res = REVERSE_MAPPING.get(cp, Integer.MIN_VALUE);
                if (res == Integer.MIN_VALUE) {
                    break;
                }
                off = s.offsetByCodePoints(off, 1);
            }

            return s.substring(off);
        }
        catch (IndexOutOfBoundsException ex) {
            Ln.e(ex,"UUUUU problem stripping [%s]",s);
            return "";
        }


    }
}
