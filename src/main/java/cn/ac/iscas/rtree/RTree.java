package cn.ac.iscas.rtree;

import java.util.ArrayList;
import java.util.List;


/**
 * @ClassName RTree
 * @Description
 */
public class RTree {
    private RTNode root;
    private int treeType;
    // private int nodeCapacity = -1;
    private int dirNodeCapacity = -1;
    private int dataNodeCapacity = -1;
    private float fillFactor = -1;
    private int dimension;

    public RTree(int dirNodeCapacity, int dataNodeCapacity, float fillFactor, int type, int dimension) {
        this.fillFactor = fillFactor;
        treeType = type;
        // nodeCapacity = capacity;
        this.dirNodeCapacity = dirNodeCapacity;
        this.dataNodeCapacity = dataNodeCapacity;
        this.dimension = dimension;
        root = new RTDataNode(this, Constants.NULL);
    }

    public RTNode getRoot() {
        return root;
    }

    /**
     * @return
     */
    public int getDimension() {
        return dimension;
    }

    /**
     *
     */
    public void setRoot(RTNode root) {
        this.root = root;
    }

    /**
     * @return
     */
    public float getFillFactor() {
        return fillFactor;
    }

    /**
     * @return
     */
    // public int getNodeCapacity() {
    //     return nodeCapacity;
    // }
    public int getDirNodeCapacity() {
        return dirNodeCapacity;
    }

    public int getDataNodeCapacity() {
        return dataNodeCapacity;
    }

    /**
     * @return
     */
    public int getTreeType() {
        return treeType;
    }

    /**
     *
     *
     * @param rectangle
     */
    public boolean insert(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getHigh().getDimension() != getDimension())
        {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.chooseLeaf(rectangle);

        return leaf.insert(rectangle);
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    public int delete(Rectangle rectangle) {
        if (rectangle == null) {
            throw new IllegalArgumentException("Rectangle cannot be null.");
        }

        if (rectangle.getHigh().getDimension() != getDimension()) {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.findLeaf(rectangle);

        if (leaf != null) {
            return leaf.delete(rectangle);
        }

        return -1;
    }

    /**
     *
     */
    public List<RTNode> traversePostOrder(RTNode root) {
        if (root == null)
            throw new IllegalArgumentException("Node cannot be null.");

        List<RTNode> list = new ArrayList<>();
        list.add(root);

        if (!root.isLeaf()) {
            for (int i = 0; i < root.usedSpace; i++) {
                List<RTNode> a = traversePostOrder(((RTDirNode) root).getChild(i));
                list.addAll(a);
            }
        }

        return list;
    }



    /**
     *
     *
     * @param root
     * @return
     */
    public static List<RTNode> traverseLevelOrder(RTNode root) {
        if (root == null) throw new IllegalArgumentException("Node cannot be null.");

        List<RTNode> list = new ArrayList<>();
        list.add(root);

        int index = 0;
        while (index < list.size()) {
            if (!list.get(index).isLeaf()) {
                for (int i = 0; i < list.get(index).usedSpace; i++) {
                    list.add(((RTDirNode) list.get(index)).getChild(i));
                }
            }

            index++;
        }

        return list;
    }

    /**
     *
     *
     * @return
     */
    public List<RTNode> traverseLevelOrder(){
        return traverseLevelOrder(root);
    }


    // public static void main(String[] args) throws Exception {
    //
    //     RTree tree = new RTree(4, 0.4f, Constants.RTREE_QUADRATIC, 2);
    //
    //     int[] f = {5, 30, 25, 35, 15, 38, 23, 50, 10, 23, 30, 28, 13, 10, 18, 15, 23, 10, 28, 20, 28, 30, 33, 40, 38,
    //             13, 43, 30, 35, 37, 40, 43, 45, 8, 50, 50, 23, 55, 28, 70, 10, 65, 15, 70, 10, 58, 20, 63,};

    //     for (int i = 0; i < f.length; ) {
    //         Point p1 = new Point(new int[]{f[i++], f[i++]});
    //         Point p2 = new Point(new int[]{f[i++], f[i++]});
    //         final Rectangle rectangle = new Rectangle(p1, p2);

    //         System.out.println("Insert " + rectangle);

    //         tree.insert(rectangle);

    //         Rectangle[] rectangles = tree.root.data;
    //         System.out.println("level:" + tree.root.level);
    //         for (int j = 0; j < rectangles.length; j++)
    //             System.out.println(rectangles[j]);

    //         System.out.println();
    //     }
    //     System.out.println("---------------------------------");
    //     System.out.println("Insert finished.\n");

    //     System.out.println("---------------------------------");
    //     System.out.println("Begin delete.");

    //     for (int i = 0; i < f.length; ) {
    //         Point p1 = new Point(new int[]{f[i++], f[i++]});
    //         Point p2 = new Point(new int[]{f[i++], f[i++]});
    //         final Rectangle rectangle = new Rectangle(p1, p2);

    //         System.out.println("Delete " + rectangle);

    //         tree.delete(rectangle);

    //         Rectangle[] rectangles = tree.root.data;
    //         System.out.println(tree.root.level);
    //         for (int j = 0; j < rectangles.length; j++)
    //             System.out.println(rectangles[j]);

    //         System.out.println();
    //     }

    //     System.out.println("---------------------------------");
    //     System.out.println("Delete finished.\n");

    //     Rectangle[] rectangles = tree.root.data;
    //     for (int i = 0; i < rectangles.length; i++)
    //         System.out.println(rectangles[i]);

    // }

}