package io.oversec.one.crypto.sym;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.jwetherell.quick_response_code.data.Contents;
import com.jwetherell.quick_response_code.qrcode.QRCodeEncoder;
import org.spongycastle.util.encoders.Base64;

import java.math.BigInteger;
import java.nio.ByteBuffer;


public class SymUtil {


    public static Bitmap getQrCode(byte[] data, int dimension) {
        try {

            String b64data = (Base64.toBase64String(data));

            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(b64data, null,
                    Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(),
                    dimension);

            return qrCodeEncoder.encodeAsBitmap();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String byteArrayToHex(byte[] a) {
        return byteArrayToHex(a, null);
    }

    public static String byteArrayToHex(byte[] a, String divider) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        boolean d = false;
        for (byte b : a) {
            sb.append(String.format("%02x", b & 0xff));
            if (d && divider != null) {
                sb.append(divider);
            }
            d = !d;
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] long2bytearray(long l) {
        //TODO optimize with needing to create a ByteBuffer object

        byte b[] = new byte[8];

        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putLong(l);
        return b;
    }


    public static long bytearray2long(byte[] b) {
        //TODO optimize with creating a new ByteBuffer object

        ByteBuffer buf = ByteBuffer.wrap(b, 0, 8);
        return buf.getLong();
    }

    public static String longToHex(long l) {
        return byteArrayToHex(BigInteger.valueOf(l).toByteArray(), null);
    }


    public static long hex2long(String s) {
        return new BigInteger(s, 16).longValue();
    }

    public static String longToPrettyHex(long v) {
        return byteArrayToHex(BigInteger.valueOf(v).toByteArray(), " ");
    }

    public static void applyAvatar(TextView textView, String name) {
        int hash = name.hashCode();
        byte[] ba = SymUtil.long2bytearray(hash);
        int red = (int) ((ba[ba.length - 1] & 0xFF) * 0.8f);
        int green = (int) ((ba[ba.length - 2] & 0xFF) * 0.8f);
        int blue = (int) ((ba[ba.length - 3] & 0xFF) * 0.8f);
        int avatarColor = Color.rgb(red, green, blue);
        textView.setBackgroundColor(avatarColor);
        textView.setText(String.valueOf(name.charAt(0)));
    }

    public static void applyAvatar(TextView textView, long keyId, String name) {
        byte[] ba = SymUtil.long2bytearray(keyId);

        int red = (int) ((ba[0] & 0xFF) * 0.8f);
        int green = (int) ((ba[1] & 0xFF) * 0.8f);
        int blue = (int) ((ba[2] & 0xFF) * 0.8f);
        int color = Color.rgb(red, green, blue);

        textView.setBackgroundColor(color);
        textView.setText(String.valueOf(name.charAt(0)));
    }
}
