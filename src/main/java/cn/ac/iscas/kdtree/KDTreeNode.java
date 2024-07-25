package cn.ac.iscas.kdtree;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 本实验的kd树：
 * 数据点都放在叶子节点中，且每个叶子节点所包含数据点个数都相等。
 */
public class KDTreeNode {

    public boolean isLeaf;
    public int minNum; // 区域包含点的个数最小值（不能小于1），少于该值就不再继续划分。
    public int m; // 维度为m
    public KDTreePoint[] points = null;

    public KDTreePoint lb; // 区域下界点
    public KDTreePoint ub; // 区域上界点

    KDTreeNode parent = null;
    KDTreeNode leftChild = null;
    KDTreeNode rightChild = null;

    public KDTreeNode(int minNum, int m, KDTreePoint[] points, KDTreeNode parent, BigInteger[] lbData,
            BigInteger[] ubData) { // 建树
        this.minNum = minNum;
        this.m = m;
        this.parent = parent;
        this.lb = new KDTreePoint(lbData);
        this.ub = new KDTreePoint(ubData);

        if (points.length / 2 < minNum) { // 不再划分
            this.isLeaf = true;
            this.points = points;
        } else { // 继续划分
            this.isLeaf = false;
            split(points);
        }
    }

    public static List<KDTreeNode> getAllLeafPoints(KDTreeNode root) {
        List<KDTreeNode> leafNodes = new ArrayList<>();

        getAllLeafPoints(root, leafNodes);

        return leafNodes;
    }

    public static void getAllLeafPoints(KDTreeNode root, List<KDTreeNode> leafNodes) {
        if (root.isLeaf) {
            leafNodes.add(root);
            return;
        }

        getAllLeafPoints(root.leftChild, leafNodes);
        getAllLeafPoints(root.rightChild, leafNodes);
    }

    private void split(KDTreePoint[] points) {
        // 找出方差最大的维度进行划分
        int maxDimension = -1;
        BigDecimal maxVar = BigDecimal.ZERO;
        for (int i = 0; i < m; i++) {
            BigInteger sum = BigInteger.ZERO;
            for (int j = 0; j < points.length; j++) {
                sum = sum.add(points[j].data[i]);
            }
            BigDecimal avg = new BigDecimal(sum).divide(new BigDecimal(m), 5, RoundingMode.DOWN);

            BigDecimal variance = BigDecimal.ZERO;
            for (int j = 0; j < points.length; j++) {
                BigDecimal t = new BigDecimal(points[j].data[i]).subtract(avg).pow(2);
                variance = variance.add(t);
            }

            if (maxVar.compareTo(variance) < 0) {
                maxVar = variance;
                maxDimension = i;
            }
        }

        final int index = maxDimension;
        // 将points在maxDimension上由小到大排列。
        Arrays.sort(points, (p1, p2) -> p1.data[index].compareTo(p2.data[index]));

        int rigthSize = points.length / 2;
        int leftSize = points.length - rigthSize;
        KDTreePoint[] leftPoints = new KDTreePoint[leftSize];
        KDTreePoint[] rigthPoints = new KDTreePoint[rigthSize];

        // BigInteger[] midPoint = points[leftSize - 1];
        System.arraycopy(points, 0, leftPoints, 0, leftSize);
        System.arraycopy(points, leftSize, rigthPoints, 0, rigthSize);

        BigInteger[] leftLb = new BigInteger[m];
        BigInteger[] leftUb = new BigInteger[m];
        BigInteger[] rightLb = new BigInteger[m];
        BigInteger[] rightUb = new BigInteger[m];
        System.arraycopy(lb.data, 0, leftLb, 0, m);
        System.arraycopy(ub.data, 0, leftUb, 0, m);
        System.arraycopy(lb.data, 0, rightLb, 0, m);
        System.arraycopy(ub.data, 0, rightUb, 0, m);

        int midIndex = (points.length % 2 == 0) ? leftSize : leftSize - 1;
        BigInteger v = points[midIndex].data[maxDimension];
        leftUb[maxDimension] = v;
        rightLb[maxDimension] = v;

        leftChild = new KDTreeNode(this.minNum, this.m, leftPoints, this, leftLb, leftUb);
        rightChild = new KDTreeNode(this.minNum, this.m, rigthPoints, this, rightLb, rightUb);
    }

    public static void main(String[] args) {
        int[] idData = new int[] { 1, 2, 3, 4, 5, 6 };
        int[][] pData = new int[][] {
                { 2, 3 }, { 4, 7 }, { 5, 4 }, { 7, 2 }, { 8, 1 }, { 9, 6 },
        };
        int[][] bData = new int[][] {
                { 0, 0 },
                { 10, 10 },
        };

        int minNum = 3;
        int m = pData[0].length;

        KDTreePoint[] points = new KDTreePoint[pData.length];
        for (int i = 0; i < points.length; i++) {
            BigInteger id = BigInteger.valueOf(idData[i]);

            BigInteger[] tData = new BigInteger[m];
            for (int j = 0; j < m; j++) {
                tData[j] = BigInteger.valueOf(pData[i][j]);
            }

            points[i] = new KDTreePoint(id, tData);
        }

        BigInteger[] lb = new BigInteger[] { BigInteger.valueOf(bData[0][0]), BigInteger.valueOf(bData[0][1]) };
        BigInteger[] ub = new BigInteger[] { BigInteger.valueOf(bData[1][0]), BigInteger.valueOf(bData[1][1]) };

        KDTreeNode kdTree = new KDTreeNode(minNum, m, points, null, lb, ub);

        List<KDTreeNode> leafNodes = getAllLeafPoints(kdTree);
        System.out.println(leafNodes);
    }
}
