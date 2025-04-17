package cn.ac.iscas.rtree;

import java.math.BigInteger;

/**
 *
 * @ClassName Rectangle
 * @Description
 */
public class Rectangle implements Cloneable
{
    private BigInteger data;

    private Point low;
    private Point high;

    public Rectangle() {
    }

    public Rectangle(Point p1, Point p2)
    {
        if (p1 == null || p2 == null)
        {
            throw new IllegalArgumentException("Points cannot be null.");
        }
        if (p1.getDimension() != p2.getDimension())
        {
            throw new IllegalArgumentException("Points must be of same dimension.");
        }
        for (int i = 0; i < p1.getDimension(); i++) {
            if (p1.indexOf(i).compareTo(p2.indexOf(i)) > 0) {
                throw new IllegalArgumentException("---");
            }
        }
        low = (Point) p1.clone();
        high = (Point) p2.clone();
    }

    public Rectangle(Point p1, Point p2, boolean isShared) {
        if (p1 == null || p2 == null)
        {
            throw new IllegalArgumentException("Points cannot be null.");
        }
        if (p1.getDimension() != p2.getDimension())
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
     *
     *
     * @return Point
     */
    public Point getLow() {
        return (Point) low.clone();
    }

    /**
     *
     *
     * @return Point
     */
    public Point getHigh() {
        return high;
    }

    /**
     * @param rectangle
     * @return
     */
    public Rectangle getUnionRectangle(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle must be of same dimension.");
        }

        BigInteger[] min = new BigInteger[getDimension()];
        BigInteger[] max = new BigInteger[getDimension()];

        for (int i = 0; i < getDimension(); i++) {
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
     * @return
     */
    public static Rectangle getUnionRectangle(Rectangle r1, Rectangle r2) {
        return r1.getUnionRectangle(r2);

        // if (r1 == null || r2 == null)
        //     throw new IllegalArgumentException("Rectangle cannot be null.");

        // if (r1.getDimension() != r2.getDimension())
        // {
        //     throw new IllegalArgumentException("Rectangle must be of same dimension.");
        // }

        // float[] min = new float[r1.getDimension()];
        // float[] max = new float[r2.getDimension()];

        // for (int i = 0; i < r1.getDimension(); i++) {
        //
        //     min[i] = Math.min(r1.low.getFloatCoordinate(i), r2.low.getFloatCoordinate(i));
        //     max[i] = Math.max(r1.high.getFloatCoordinate(i), r2.high.getFloatCoordinate(i));
        // }

        // return new Rectangle(new Point(min), new Point(max));
    }

    /**
     * @param rectangles
     * @return
     */
    public static Rectangle getUnionRectangle(Rectangle[] rectangles) {
        if (rectangles == null || rectangles.length == 0)
            throw new IllegalArgumentException("Rectangle array is empty.");

        Rectangle r = (Rectangle) rectangles[0].clone();
        for (int i = 1; i < rectangles.length; i++) {
            r = r.getUnionRectangle(rectangles[i]);
        }

        return r;
    }

    /**
     * @return
     */
    public BigInteger getArea() {
        BigInteger area = BigInteger.ONE;
        for (int i = 0; i < getDimension(); i++) {
            area = area.multiply(high.indexOf(i).subtract(low.indexOf(i)));
        }

        return area;
    }

    @Override
    protected Object clone() {
        Point p1 = (Point) low.clone();
        Point p2 = (Point) high.clone();
        return new Rectangle(p1, p2);
    }

    @Override
    public String toString() {
        return "Rectangle Low:" + low + " High:" + high + " Data:" + data;
    }

    /**
     *
     *
     * @param rectangle Rectangle
     * @return float
     */
    public BigInteger intersectingArea(Rectangle rectangle) {
        if (!isIntersection(rectangle))
        {
            return BigInteger.ZERO;
        }

        BigInteger ret = BigInteger.ONE;
        for (int i = 0; i < rectangle.getDimension(); i++) {
            BigInteger l1 = this.low.indexOf(i);
            BigInteger h1 = this.high.indexOf(i);
            BigInteger l2 = rectangle.low.indexOf(i);
            BigInteger h2 = rectangle.high.indexOf(i);

            if ((l1.compareTo(l2) <= 0) && (h1.compareTo(h2) <= 0)) {
                ret = ret.multiply(h1.subtract(l1).subtract(l2.subtract(l1)));
            }
            // if (l1 <= l2 && h1 <= h2) {
            //     ret *= (h1 - l1) - (l2 - l1);
            // }

            else if ((l1.compareTo(l2) >= 0) && (h1.compareTo(h2) >= 0)) {
                ret = ret.multiply(h2.subtract(l2).subtract(l1.subtract(l2)));
            }
            // else if (l1 >= l2 && h1 >= h2) {
            //     ret *= (h2 - l2) - (l1 - l2);
            // }

            else if ((l1.compareTo(l2) >= 0) && (h1.compareTo(h2) <= 0)) {
                ret = ret.multiply(h1.subtract(l1));
            }
            // else if (l1 >= l2 && h1 <= h2) {
            //     ret *= h1 - l1;
            // }

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
     * @return
     */
    public boolean isIntersection(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }

        for (int i = 0; i < getDimension(); i++) {
            if ((low.indexOf(i).compareTo(rectangle.high.indexOf(i)) > 0)
                    || (high.indexOf(i).compareTo(rectangle.low.indexOf(i)) < 0))
                return false;
            // if (low.getFloatCoordinate(i) > rectangle.high.getFloatCoordinate(i)
            //         || high.getFloatCoordinate(i) < rectangle.low.getFloatCoordinate(i)) {
            //     return false;
            // }
        }
        return true;
    }

    /**
     * @return
     */
    public int getDimension() {
        return low.getDimension();
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    public boolean enclosure(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getDimension() != getDimension())
            throw new IllegalArgumentException("Rectangle dimension is different from current dimension.");
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
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle rectangle = (Rectangle) obj;
            if (low.equals(rectangle.getLow()) && high.equals(rectangle.getHigh()))
                return true;
        }
        return false;
    }

    // public static void main(String[] args) {
    //
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
    //     // Rectangle re4 = new Rectangle(p3, p4);

    //     System.out.println(re1.isIntersection(re2));
    //     System.out.println(re1.isIntersection(re3));
    //     System.out.println(re1.intersectingArea(re2));
    //     System.out.println(re1.intersectingArea(re3));
    // }
}