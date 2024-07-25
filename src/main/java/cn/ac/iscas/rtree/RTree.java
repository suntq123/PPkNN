package cn.ac.iscas.rtree;

import java.util.ArrayList;
import java.util.List;


/**
 * @ClassName RTree
 * @Description
 */
public class RTree {
    private RTNode root; // 根节点
    private int treeType; // 树类型
    // private int nodeCapacity = -1; // 结点容量
    private int dirNodeCapacity = -1; // 非叶子节点结点容量
    private int dataNodeCapacity = -1; // 非叶子节点结点容量
    private float fillFactor = -1; // 结点填充因子 ，用于计算每个结点最小条目个数
    private int dimension; // 维度

    public RTree(int dirNodeCapacity, int dataNodeCapacity, float fillFactor, int type, int dimension) {
        this.fillFactor = fillFactor;
        treeType = type;
        // nodeCapacity = capacity;
        this.dirNodeCapacity = dirNodeCapacity;
        this.dataNodeCapacity = dataNodeCapacity;
        this.dimension = dimension;
        root = new RTDataNode(this, Constants.NULL); // 根节点的父节点为NULL
    }

    public RTNode getRoot() {
        return root;
    }

    /**
     * @return RTree的维度
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * 设置跟节点
     */
    public void setRoot(RTNode root) {
        this.root = root;
    }

    /**
     * @return 填充因子
     */
    public float getFillFactor() {
        return fillFactor;
    }

    /**
     * @return 返回结点容量
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
     * @return 返回树的类型
     */
    public int getTreeType() {
        return treeType;
    }

    /**
     * --> 向Rtree中插入Rectangle 1、先找到合适的叶节点 2、再向此叶节点中插入
     *
     * @param rectangle
     */
    public boolean insert(Rectangle rectangle) {
        if (rectangle == null)
            throw new IllegalArgumentException("Rectangle cannot be null.");

        if (rectangle.getHigh().getDimension() != getDimension()) // 矩形维度与树的维度不一致
        {
            throw new IllegalArgumentException("Rectangle dimension different than RTree dimension.");
        }

        RTDataNode leaf = root.chooseLeaf(rectangle);

        return leaf.insert(rectangle);
    }

    /**
     * 从R树中删除Rectangle
     * <p>
     * 1、寻找包含记录的结点--调用算法findLeaf()来定位包含此记录的叶子结点L，如果没有找到则算法终止。<br>
     * 2、删除记录--将找到的叶子结点L中的此记录删除<br>
     * 3、调用算法condenseTree<br>
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
     * 从给定的结点root开始遍历所有的结点
     * 深度优先，先根遍历，先左遍历
     *
     * @param root
     * @return 所有遍历的结点集合
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
     * 从给定节点开始遍历
     * 层次搜索，先左遍历
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
     * 层次遍历整个R-tree
     *
     * @return
     */
    public List<RTNode> traverseLevelOrder(){
        return traverseLevelOrder(root);
    }


    // public static void main(String[] args) throws Exception {
    //     // 结点容量：4；填充因子：0.4；树类型：二维
    //     RTree tree = new RTree(4, 0.4f, Constants.RTREE_QUADRATIC, 2);
    //     // 每一行的四个数构成两个点（一个矩形）
    //     int[] f = {5, 30, 25, 35, 15, 38, 23, 50, 10, 23, 30, 28, 13, 10, 18, 15, 23, 10, 28, 20, 28, 30, 33, 40, 38,
    //             13, 43, 30, 35, 37, 40, 43, 45, 8, 50, 50, 23, 55, 28, 70, 10, 65, 15, 70, 10, 58, 20, 63,};

    //     // 插入结点
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

    //     // 删除结点
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