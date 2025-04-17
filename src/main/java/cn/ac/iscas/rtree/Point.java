package cn.ac.iscas.rtree;

import java.math.BigInteger;

/**
 * @ClassName Point
 * @Description
 */
public class Point implements Cloneable {
    private BigInteger[] data;

    // public Point(int[] data) {
    //     if (data == null) {
    //         throw new IllegalArgumentException("Coordinates cannot be null.");
    //     }
    //     if (data.length < 2) {
    //         throw new IllegalArgumentException("Point dimension should be greater than 1.");
    //     }

    //     this.data = new BigInteger[data.length];
    //     for (int i = 0; i < data.length; i++) {
    //         this.data[i] = BigInteger.valueOf(data[i]);
    //     }
    // }

    public Point(BigInteger[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Coordinates cannot be null.");
        }
        if (data.length < 2) {
            throw new IllegalArgumentException("Point dimension should be greater than 1.");
        }

        // this.data = new BigInteger[data.length];
        // for (int i = 0; i < data.length; i++) {
        //     this.data[i] = data[i];
        // }
        this.data = data.clone();
    }

    @Override
    protected Object clone() {
        BigInteger[] copy = new BigInteger[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return new Point(copy);
    }

    @Override
    public String toString() {
        StringBuffer sBuffer = new StringBuffer("(");

        for (int i = 0; i < data.length - 1; i++) {
            sBuffer.append(data[i]).append(",");
        }

        sBuffer.append(data[data.length - 1]).append(")");

        return sBuffer.toString();
    }

    public BigInteger[] getData() {
        return data;
    }

    /**
     * @return
     */
    public int getDimension() {
        return data.length;
    }

    /**
     * @param index
     * @return
     */
    public BigInteger indexOf(int index) {
        return data[index];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point)
        {
            Point point = (Point) obj;

            if (point.getDimension() != getDimension())
                throw new IllegalArgumentException("Points must be of equal dimensions to be compared.");

            for (int i = 0; i < getDimension(); i++) {
                // if (getFloatCoordinate(i) != point.getFloatCoordinate(i))
                //     return false;
                if (!this.indexOf(i).equals(point.indexOf(i)))
                    return false;
            }
        }

        if (!(obj instanceof Point))
            return false;

        return true;
    }

    public static void main(String[] args) {
        int[] testData = { 1, 2, 3 };
        BigInteger[] data = new BigInteger[testData.length];

        for (int i = 0; i < data.length; i++) {
            data[i] = BigInteger.valueOf(testData[i]);
        }

        Point point1 = new Point(data);
        System.out.println(point1);
    }
}