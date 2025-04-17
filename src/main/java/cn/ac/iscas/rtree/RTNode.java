package cn.ac.iscas.rtree;

import java.math.BigInteger;
import java.util.List;

/**
 * @ClassName RTNode
 * @Description
 */
public abstract class RTNode {
    protected RTree rtree;
    protected int level;
    protected Rectangle[] data;
    protected RTNode parent;
    protected int usedSpace;
    protected int insertIndex;
    protected int deleteIndex;

    int capacity;

    /**
     *
     */
    public RTNode(RTree rtree, RTNode parent, int level) {
        this.rtree = rtree;
        this.parent = parent;
        this.level = level;


        if (isLeaf())
            capacity = rtree.getDataNodeCapacity();
        else
            capacity = rtree.getDirNodeCapacity();

        data = new Rectangle[capacity + 1];
        usedSpace = 0;
    }

    /**
     *
     *
     * @return
     */
    public Rectangle[] getData() {
        return data;
    }

    public int getLevel() {
        return level;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    /**
     * @return
     */
    public RTNode getParent() {
        return parent;
    }

    /**
     *
     *
     * @param rectangle
     */
    protected void addData(Rectangle rectangle) {
        if (usedSpace == capacity) {
            throw new IllegalArgumentException("Node is full.");
        }
        data[usedSpace++] = rectangle;
    }

    /**
     *
     * @param i
     */
    protected void deleteData(int i) {
        if (data[i + 1] != null)
        {
            System.arraycopy(data, i + 1, data, i, usedSpace - i - 1);
            data[usedSpace - 1] = null;
        } else
            data[i] = null;
        usedSpace--;
    }

    /**
     *
     *
     * @param list
     */
    protected void condenseTree(List<RTNode> list) {
        if (isRoot()) {
            if (!isLeaf() && usedSpace == 1) {
                RTDirNode root = (RTDirNode) this;

                RTNode child = root.getChild(0);
                root.children.remove(this);
                child.parent = null;
                rtree.setRoot(child);

            }
        } else {
            RTNode parent = getParent();
            int min = Math.round(capacity * rtree.getFillFactor());
            if (usedSpace < min) {
                parent.deleteData(parent.deleteIndex);
                ((RTDirNode) parent).children.remove(this);
                this.parent = null;
                list.add(this);
            } else {
                parent.data[parent.deleteIndex] = getNodeRectangle();
            }
            parent.condenseTree(list);
        }
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    protected int[][] quadraticSplit(Rectangle rectangle) {
        if (rectangle == null) {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }

        data[usedSpace] = rectangle;

        int total = usedSpace + 1;

        int[] mask = new int[total];
        for (int i = 0; i < total; i++) {
            mask[i] = 1;
        }

        int c = total / 2 + 1;

        int minNodeSize = Math.round(capacity * rtree.getFillFactor());

        if (minNodeSize < 2)
            minNodeSize = 2;

        int rem = total;

        int[] group1 = new int[c];
        int[] group2 = new int[c];

        int i1 = 0, i2 = 0;

        int[] seed = pickSeeds();
        group1[i1++] = seed[0];
        group2[i2++] = seed[1];
        rem -= 2;
        mask[group1[0]] = -1;
        mask[group2[0]] = -1;

        while (rem > 0) {
            //            if (minNodeSize - i1 == rem) {
            if (i2 == c) {
                for (int i = 0; i < total; i++)
                {
                    if (mask[i] != -1)
                    {
                        group1[i1++] = i;
                        mask[i] = -1;
                        rem--;
                    }
                }
                //            } else if (minNodeSize - i2 == rem) {
            } else if (i1 == c) {
                for (int i = 0; i < total; i++)
                {
                    if (mask[i] != -1)
                    {
                        group2[i2++] = i;
                        mask[i] = -1;
                        rem--;
                    }
                }
            } else {
                Rectangle mbr1 = (Rectangle) data[group1[0]].clone();
                for (int i = 1; i < i1; i++) {
                    mbr1 = mbr1.getUnionRectangle(data[group1[i]]);
                }
                Rectangle mbr2 = (Rectangle) data[group2[0]].clone();
                for (int i = 1; i < i2; i++) {
                    mbr2 = mbr2.getUnionRectangle(data[group2[i]]);
                }

                BigInteger dif = null;
                BigInteger areaDiff1 = BigInteger.ZERO, areaDiff2 = BigInteger.ZERO;
                int sel = -1;
                for (int i = 0; i < total; i++) {
                    if (mask[i] != -1)
                    {
                        Rectangle a = mbr1.getUnionRectangle(data[i]);
                        areaDiff1 = a.getArea().subtract(mbr1.getArea());

                        Rectangle b = mbr2.getUnionRectangle(data[i]);
                        areaDiff2 = b.getArea().subtract(mbr2.getArea());

                        if (dif == null || areaDiff1.subtract(areaDiff2).abs().compareTo(dif) > 0) {
                            dif = areaDiff1.subtract(areaDiff2).abs();
                            sel = i;
                        }
                        // if (Math.abs(areaDiff1 - areaDiff2) > dif) {
                        //     dif = Math.abs(areaDiff1 - areaDiff2);
                        //     sel = i;
                        // }
                    }
                }

                if (areaDiff1.compareTo(areaDiff2) < 0)
                {
                    group1[i1++] = sel;
                } else if (areaDiff1.compareTo(areaDiff2) > 0) {
                    group2[i2++] = sel;
                } else if (mbr1.getArea().compareTo(mbr2.getArea()) < 0)
                {
                    group1[i1++] = sel;
                } else if (mbr1.getArea().compareTo(mbr2.getArea()) > 0) {
                    group2[i2++] = sel;
                } else if (i1 < i2)
                {
                    group1[i1++] = sel;
                } else if (i1 > i2) {
                    group2[i2++] = sel;
                } else {
                    group1[i1++] = sel;
                }
                mask[sel] = -1;
                rem--;

            }
        } // end while

        int[][] ret = new int[2][];
        ret[0] = new int[i1];
        ret[1] = new int[i2];

        for (int i = 0; i < i1; i++) {
            ret[0][i] = group1[i];
        }
        for (int i = 0; i < i2; i++) {
            ret[1][i] = group2[i];
        }
        return ret;
    }

    /**
     *
     *
     * @return
     */
    protected int[] pickSeeds() {
        BigInteger inefficiency = null;
        int i1 = 0, i2 = 0;

        for (int i = 0; i < usedSpace; i++) {
            for (int j = i + 1; j <= usedSpace; j++)
            {
                Rectangle rectangle = data[i].getUnionRectangle(data[j]);
                BigInteger d = rectangle.getArea().subtract(data[i].getArea()).subtract(data[j].getArea());

                if (inefficiency == null || d.compareTo(inefficiency) > 0) {
                    inefficiency = d;
                    i1 = i;
                    i2 = j;
                }
            }
        }
        return new int[] { i1, i2 };
    }

    /**
     * @return
     */
    public Rectangle getNodeRectangle() {
        if (usedSpace > 0) {
            Rectangle[] rectangles = new Rectangle[usedSpace];
            System.arraycopy(data, 0, rectangles, 0, usedSpace);
            return Rectangle.getUnionRectangle(rectangles);
        } else {
            return new Rectangle(new Point(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO }),
                    new Point(new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO }));
        }
    }

    /**
     * @return
     */
    public boolean isRoot() {
        return (parent == Constants.NULL);
    }

    /**
     * @return
     */
    public boolean isIndex() {
        return (level != 0);
    }

    /**
     * @return
     */
    public boolean isLeaf() {
        return (level == 0);
    }

    /**
     *
     * <p>
     *
     * @param rectangle
     * @return RTDataNode
     */
    protected abstract RTDataNode chooseLeaf(Rectangle rectangle);

    /**
     *
     *
     * @param rectangle
     * @return
     */
    protected abstract RTDataNode findLeaf(Rectangle rectangle);

}