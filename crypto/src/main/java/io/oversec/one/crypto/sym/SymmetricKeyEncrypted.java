package io.oversec.one.crypto.sym;

import java.util.Date;

public class SymmetricKeyEncrypted {

    private int cost;
    private byte[] iv;
    private long id;
    private byte[] salt;
    private byte[] ciphertext;

    private Date createdDate;
    private Date confirmedDate;
    private String name;

    public SymmetricKeyEncrypted() {
    }

    public SymmetricKeyEncrypted(long id, String name, Date createdDate, byte[] salt, byte[] iv, int cost, byte[] ciphertext) {
        this.id = id;
        this.name = name;

        this.createdDate = createdDate;
        this.salt = salt;
        this.iv = iv;
        this.cost = cost;
        this.ciphertext = ciphertext;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(byte[] ciphertext) {
        this.ciphertext = ciphertext;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public Date getConfirmedDate() {
        return confirmedDate;
    }

    public void setConfirmedDate(Date confirmedDate) {
        this.confirmedDate = confirmedDate;
    }

}
