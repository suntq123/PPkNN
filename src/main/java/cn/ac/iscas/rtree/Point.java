package cn.ac.iscas.rtree;

import java.math.BigInteger;

/**
 * @ClassName Point
 * @Description n维空间中的点，所有的维度被存储在一个BigInteger数组中
 */
public class Point implements Cloneable {
    private BigInteger[] data;

    // public Point(int[] data) {
    //     if (data == null) {
    //         throw new IllegalArgumentException("Coordinates cannot be null."); // ★坐标不能为空
    //     }
    //     if (data.length < 2) {
    //         throw new IllegalArgumentException("Point dimension should be greater than 1."); // ★点的维度必须大于1
    //     }

    //     this.data = new BigInteger[data.length];
    //     for (int i = 0; i < data.length; i++) {
    //         this.data[i] = BigInteger.valueOf(data[i]); // 复制数组
    //     }
    // }

    public Point(BigInteger[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Coordinates cannot be null."); // ★坐标不能为空
        }
        if (data.length < 2) {
            throw new IllegalArgumentException("Point dimension should be greater than 1."); // ★点的维度必须大于1
        }

        // this.data = new BigInteger[data.length];
        // for (int i = 0; i < data.length; i++) {
        //     this.data[i] = data[i]; // 复制数组
        // }
        this.data = data.clone();
    }

    @Override // 重写clone接口
    protected Object clone() {
        BigInteger[] copy = new BigInteger[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return new Point(copy);
    }

    @Override // 重写tostring（）方法
    public String toString() {
        StringBuffer sBuffer = new StringBuffer("(");

        for (int i = 0; i < data.length - 1; i++) {
            sBuffer.append(data[i]).append(",");
        }

        sBuffer.append(data[data.length - 1]).append(")"); // 最后一位数据后面不再添加逗号，追加放在循环外面

        return sBuffer.toString();
    }

    public BigInteger[] getData() {
        return data;
    }

    /**
     * @return 返回Point的维度
     */
    public int getDimension() {
        return data.length;
    }

    /**
     * @param index
     * @return 返回Point坐标第i位
     */
    public BigInteger indexOf(int index) {
        return data[index];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point) // 如果obj是point的实例
        {
            Point point = (Point) obj;

            if (point.getDimension() != getDimension()) // 维度相同的点才能比较
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