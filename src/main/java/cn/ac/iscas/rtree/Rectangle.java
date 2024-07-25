package cn.ac.iscas.rtree;

import java.math.BigInteger;

/**
 * 外包矩形
 *
 * @ClassName Rectangle
 * @Description
 */
public class Rectangle implements Cloneable // 继承克隆接口
{
    private BigInteger data;

    private Point low; // 左下角的点
    private Point high; // 右上角的点

    public Rectangle() {
    }

    public Rectangle(Point p1, Point p2) // 初始化时，第一个参数为左下角，第二个参数为右上角
    {
        if (p1 == null || p2 == null) // 点对象不能为空
        {
            throw new IllegalArgumentException("Points cannot be null.");
        }
        if (p1.getDimension() != p2.getDimension()) // 点的维度应该相等
        {
            throw new IllegalArgumentException("Points must be of same dimension.");
        }
        // 先左下角后右上角
        for (int i = 0; i < p1.getDimension(); i++) {
            if (p1.indexOf(i).compareTo(p2.indexOf(i)) > 0) {
                throw new IllegalArgumentException("坐标点为先左下角后右上角");
            }
        }
        low = (Point) p1.clone();
        high = (Point) p2.clone();
    }

    public Rectangle(Point p1, Point p2, boolean isShared) {
        if (p1 == null || p2 == null) // 点对象不能为空
        {
            throw new IllegalArgumentException("Points cannot be null.");
        }
        if (p1.getDimension() != p2.getDimension()) // 点的维度应该相等
        {
            throw new IllegalArgumentException("Points must be of same dimension.");
        }
        low = (Point) p1.clone();
        high = (Point) p2.clone();
    }

    public void setData(BigInteger data) {
        this.data = data;
    }

    public void setLow(Point low) {
        this.low = low;
    }

    public void setHigh(Point high) {
        this.high = high;
    }

    public Rectangle(Point p1, Point p2, BigInteger data) {
        this(p1, p2);
        this.data = data;
    }

    public BigInteger getData() {
        return data;
    }

    /**
     * 返回Rectangle左下角的Point
     *
     * @return Point
     */
    public Point getLow() {
        return (Point) low.clone();
    }

    /**
     * 返回Rectangle右上角的Point
     *
     * @return Point
     */
    public Point getHigh() {
        return high;
    }

    /**
     * @param rectangle
     * @return 包围两个Rectangle的最小Rectangle
     */
    public Rectangle getUnionRectangle(Rectangle rectangle) {
        if (rectangle == null) // 矩形不能为空
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getDimension() != getDimension()) // 矩形维度必须相同
        {
            throw new IllegalArgumentException("Rectangle must be of same dimension.");
        }

        BigInteger[] min = new BigInteger[getDimension()];
        BigInteger[] max = new BigInteger[getDimension()];

        for (int i = 0; i < getDimension(); i++) {
            // 第一个参数是当前矩形的坐标值，第二个参数是传入的参数的矩形的坐标值
            min[i] = (low.indexOf(i).compareTo(rectangle.low.indexOf(i)) < 0) ? low.indexOf(i)
                    : rectangle.low.indexOf(i);
            max[i] = (high.indexOf(i).compareTo(rectangle.high.indexOf(i)) > 0) ? high.indexOf(i)
                    : rectangle.high.indexOf(i);
            // min[i] = Math.min(low.getFloatCoordinate(i), rectangle.low.getFloatCoordinate(i));
            // max[i] = Math.max(high.getFloatCoordinate(i), rectangle.high.getFloatCoordinate(i));
        }

        return new Rectangle(new Point(min), new Point(max));
    }

    /**
     * @param r1
     * @param r2
     * @return 包围两个Rectangle的最小Rectangle
     */
    public static Rectangle getUnionRectangle(Rectangle r1, Rectangle r2) {
        return r1.getUnionRectangle(r2);

        // if (r1 == null || r2 == null) // 矩形不能为空
        //     throw new IllegalArgumentException("Rectangle cannot be null.");

        // if (r1.getDimension() != r2.getDimension()) // 矩形维度必须相同
        // {
        //     throw new IllegalArgumentException("Rectangle must be of same dimension.");
        // }

        // float[] min = new float[r1.getDimension()];
        // float[] max = new float[r2.getDimension()];

        // for (int i = 0; i < r1.getDimension(); i++) {
        //     // 第一个参数是当前矩形的坐标值，第二个参数是传入的参数的矩形的坐标值
        //     min[i] = Math.min(r1.low.getFloatCoordinate(i), r2.low.getFloatCoordinate(i));
        //     max[i] = Math.max(r1.high.getFloatCoordinate(i), r2.high.getFloatCoordinate(i));
        // }

        // return new Rectangle(new Point(min), new Point(max));
    }

    /**
     * @param rectangles
     * @return 包围一系列Rectangle的最小Rectangle
     */
    public static Rectangle getUnionRectangle(Rectangle[] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            throw new IllegalArgumentException("Rectangle array is empty.");

        Rectangle r = (Rectangle) rectangles[0].clone();
        for (int i = 1; i < rectangles.length; i++) {
            r = r.getUnionRectangle(rectangles[i]); // 获得包裹矩形r与r[i]的最小边界的矩形再赋值给r
        }

        return r; // 返回包围一系列Rectangle的最小Rectangle
    }

    /**
     * @return 返回Rectangle的面积
     */
    public BigInteger getArea() {
        BigInteger area = BigInteger.ONE;
        for (int i = 0; i < getDimension(); i++) {
            area = area.multiply(high.indexOf(i).subtract(low.indexOf(i)));
        }

        return area;
    }

    @Override
    // 重写clone()函数
    protected Object clone() {
        Point p1 = (Point) low.clone();
        Point p2 = (Point) high.clone();
        return new Rectangle(p1, p2);
    }

    @Override
    // 重写tostring()方法
    public String toString() {
        return "Rectangle Low:" + low + " High:" + high + " Data:" + data;
    }

    /**
     * 两个Rectangle相交的面积
     *
     * @param rectangle Rectangle
     * @return float
     */
    public BigInteger intersectingArea(Rectangle rectangle) {
        if (!isIntersection(rectangle)) // 如果不相交，相交面积为0
        {
            return BigInteger.ZERO;
        }

        BigInteger ret = BigInteger.ONE;
        // 循环一次，得到一个维度的相交的边，累乘多个维度的相交的边，即为面积
        for (int i = 0; i < rectangle.getDimension(); i++) {
            BigInteger l1 = this.low.indexOf(i);
            BigInteger h1 = this.high.indexOf(i);
            BigInteger l2 = rectangle.low.indexOf(i);
            BigInteger h2 = rectangle.high.indexOf(i);

            // rectangle1在rectangle2的左边
            if ((l1.compareTo(l2) <= 0) && (h1.compareTo(h2) <= 0)) {
                ret = ret.multiply(h1.subtract(l1).subtract(l2.subtract(l1)));
            }
            // if (l1 <= l2 && h1 <= h2) {
            //     ret *= (h1 - l1) - (l2 - l1);
            // }

            // rectangle1在rectangle2的右边
            else if ((l1.compareTo(l2) >= 0) && (h1.compareTo(h2) >= 0)) {
                ret = ret.multiply(h2.subtract(l2).subtract(l1.subtract(l2)));
            }
            // else if (l1 >= l2 && h1 >= h2) {
            //     ret *= (h2 - l2) - (l1 - l2);
            // }

            // rectangle1在rectangle2里面
            else if ((l1.compareTo(l2) >= 0) && (h1.compareTo(h2) <= 0)) {
                ret = ret.multiply(h1.subtract(l1));
            }
            // else if (l1 >= l2 && h1 <= h2) {
            //     ret *= h1 - l1;
            // }

            // rectangle1包含rectangle2
            else if ((l1.compareTo(l2) <= 0) && (h1.compareTo(h2) >= 0)) {
                ret = ret.multiply(h2.subtract(l2));
            }
            // else if (l1 <= l2 && h1 >= h2) {
            //     ret *= h2 - l2;
            // }
        }
        return ret;
    }

    /**
     * @param rectangle
     * @return 判断两个Rectangle是否相交
     */
    public boolean isIntersection(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getDimension() != getDimension()) // 进行判断的两个矩形维度必须相等
        {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }

        for (int i = 0; i < getDimension(); i++) {
            /*
             * 当前矩形左下角的坐标值大于传入矩形右上角的坐标值 || 当前矩形右上角角的坐标值小于传入矩形左下角的坐标值
             */
            if ((low.indexOf(i).compareTo(rectangle.high.indexOf(i)) > 0)
                    || (high.indexOf(i).compareTo(rectangle.low.indexOf(i)) < 0))
                return false;
            // if (low.getFloatCoordinate(i) > rectangle.high.getFloatCoordinate(i)
            //         || high.getFloatCoordinate(i) < rectangle.low.getFloatCoordinate(i)) {
            //     return false; // 没有相交
            // }
        }
        return true;
    }

    /**
     * @return 返回Rectangle的维度
     */
    public int getDimension() {
        return low.getDimension();
    }

    /**
     * 判断rectangle是否被包围
     *
     * @param rectangle
     * @return
     */
    public boolean enclosure(Rectangle rectangle) {
        if (rectangle == null) // 矩形不能为空
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getDimension() != getDimension()) // 判断的矩形必须维度相同
            throw new IllegalArgumentException("Rectangle dimension is different from current dimension.");
        // 只要传入的rectangle有一个维度的坐标越界了就不被包含
        for (int i = 0; i < getDimension(); i++) {
            if ((rectangle.low.indexOf(i).compareTo(low.indexOf(i)) < 0)
                    || (rectangle.high.indexOf(i).compareTo(high.indexOf(i)) > 0))
                return false;
            // if (rectangle.low.getFloatCoordinate(i) < low.getFloatCoordinate(i)
            //         || rectangle.high.getFloatCoordinate(i) > high.getFloatCoordinate(i))
            //     return false;
        }
        return true;
    }

    @Override
    // 重写equals方法
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle rectangle = (Rectangle) obj;
            if (low.equals(rectangle.getLow()) && high.equals(rectangle.getHigh()))
                return true;
        }
        return false;
    }

    // public static void main(String[] args) {
    //     // 新建两point再根据两个point构建一个Rectangle
    //     int[] f1 = {1, 2};
    //     int[] f2 = {3, 4};
    //     Point p1 = new Point(f1);
    //     Point p2 = new Point(f2);
    //     Rectangle rectangle = new Rectangle(p1, p2);
    //     System.out.println(rectangle);
    //     // Point point = rectangle.getHigh();
    //     // point = p1;
    //     // System.out.println(rectangle);

    //     int[] f_1 = {-2, 0};
    //     int[] f_2 = {0, 2};
    //     int[] f_3 = {-2, 1};
    //     int[] f_4 = {3, 3};
    //     int[] f_5 = {1, 0};
    //     int[] f_6 = {2, 4};
    //     p1 = new Point(f_1);
    //     p2 = new Point(f_2);
    //     Point p3 = new Point(f_3);
    //     Point p4 = new Point(f_4);
    //     Point p5 = new Point(f_5);
    //     Point p6 = new Point(f_6);
    //     Rectangle re1 = new Rectangle(p1, p2);
    //     Rectangle re2 = new Rectangle(p3, p4);
    //     Rectangle re3 = new Rectangle(p5, p6);
    //     // Rectangle re4 = new Rectangle(p3, p4); //输入要先左下角，再右上角

    //     System.out.println(re1.isIntersection(re2));
    //     System.out.println(re1.isIntersection(re3));
    //     System.out.println(re1.intersectingArea(re2));
    //     System.out.println(re1.intersectingArea(re3));
    // }
}