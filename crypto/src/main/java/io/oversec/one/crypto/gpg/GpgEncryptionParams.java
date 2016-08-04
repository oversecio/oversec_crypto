package io.oversec.one.crypto.gpg;

import android.content.Context;
import io.oversec.one.crypto.AbstractEncryptionParams;
import io.oversec.one.crypto.EncryptionMethod;

import java.util.*;

public class GpgEncryptionParams extends AbstractEncryptionParams {


    long mOwnPublicKey;
    boolean mSign;
    Set<Long> mPublicKeys = new HashSet<>();

    public GpgEncryptionParams(List<Long> pkids, String coderId, String padderId) {
        super(EncryptionMethod.GPG, coderId, padderId);
        mPublicKeys.addAll(pkids);
    }

    public GpgEncryptionParams(long[] keyIds, String coderId, String padderId) {
        super(EncryptionMethod.GPG, coderId, padderId);
        for (long keyId : keyIds) {
            mPublicKeys.add(keyId);
        }
    }


    public Set<Long> getPublicKeyIds() {
        return mPublicKeys;
    }

    public void removePublicKey(Long keyId) {
        mPublicKeys.remove(keyId);
    }

    public Long getOwnPublicKey() {
        return mOwnPublicKey;
    }

    public void addPublicKeyIds(Long[] keyIds, long omitThisKey) {

        for (Long keyId : keyIds) {
            if (!keyId.equals(omitThisKey)) {
                mPublicKeys.add(keyId);
            }
        }
    }

    public static Long[] LongListToLongArray(List<Long> a) {
        if (a == null) {
            return null;
        }
        Long[] res = new Long[a.size()];
        a.toArray(res);
        return res;
    }

    public static List<Long> LongArrayToLongList(Long[] a) {
        if (a == null) {
            return null;
        }
        return new ArrayList<>(Arrays.asList(a));
    }

    public static Long[] longArrayToLongArray(long[] a) {
        if (a == null) {
            return null;
        }
        Long[] res = new Long[a.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = a[i];
        }
        return res;
    }

    public static long[] LongArrayTolongArray(Long[] a) {
        if (a == null) {
            return null;
        }
        long[] res = new long[a.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = a[i];
        }
        return res;
    }

    public void setOwnPublicKey(long k) {
        mOwnPublicKey = k;
        mPublicKeys.remove(k);
    }

    public void setSign(boolean s) {
        mSign = s;
    }

    public long[] getAllPublicKeyIds() {
        Set<Long> allKeys = new HashSet<>(mPublicKeys);

        if (mOwnPublicKey != 0) {
            allKeys.add(mOwnPublicKey);
        }
        long[] res = new long[allKeys.size()];
        int i = 0;
        for (long id : allKeys
                ) {
            res[i] = id;
            i++;
        }
        return res;


    }

    public boolean isSign() {
        return mSign;
    }

    public void addPublicKey(Long id) {
        mPublicKeys.add(id);
    }

    @Override
    public boolean isStillValid(Context ctx) {
        //TODO: check that OpenKEychain service is still up and running,
        //TODO: check that we do still have the keys
        return true;
    }

    public void setPublicKeyIds(long[] ids) {
        mPublicKeys.clear();
        for (long id : ids) {
            mPublicKeys.add(id);
        }
    }

    @Override
    public String toString() {

        return "GpgEncryptionParams{" +
                "mOwnPublicKey=" + mOwnPublicKey +
                ", mSign=" + mSign +
                ", mPublicKeys=" + Arrays.toString(mPublicKeys.toArray()) +
                '}';
    }
}
