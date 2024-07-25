package cn.ac.iscas.kdtree;

import java.math.BigInteger;

public class KDTreePoint {

    public int m;
    public BigInteger id;
    public BigInteger[] data;

    public KDTreePoint(BigInteger[] data) {
        this.m = data.length;
        this.data = new BigInteger[m];
        System.arraycopy(data, 0, this.data, 0, m);
    }

    public KDTreePoint(BigInteger id, BigInteger[] data) {
        this.m = data.length;
        this.id = id;

        this.data = new BigInteger[m];
        System.arraycopy(data, 0, this.data, 0, m);
    }
}
