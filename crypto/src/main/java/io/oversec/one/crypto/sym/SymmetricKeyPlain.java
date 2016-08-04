package io.oversec.one.crypto.sym;

import io.oversec.one.crypto.symbase.KeyUtil;

import java.io.Serializable;
import java.util.Date;

public class SymmetricKeyPlain implements Serializable {
    private boolean mIsSimpleKey;
    private long id;
    private byte[] raw;

    private Date createdDate;
    private String name;

    public SymmetricKeyPlain() {
    }

    public SymmetricKeyPlain(long id, String name, Date createdDate, byte[] raw, boolean isSimpleKey) {
        this(id, name, createdDate, raw);
        mIsSimpleKey = isSimpleKey;
    }

    public SymmetricKeyPlain(long id, String name, Date createdDate, byte[] raw) {
        this.id = id;
        this.name = name;

        this.createdDate = createdDate;
        this.raw = raw;
    }

    public SymmetricKeyPlain(byte[] raw) {
        this.createdDate = new Date();
        this.raw = raw;
    }


    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public byte[] getRaw() {
        return raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clearKeyData() {
        KeyUtil.erase(raw);
        raw = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isSimpleKey() {
        return mIsSimpleKey;
    }
}
