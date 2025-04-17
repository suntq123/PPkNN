package cn.ac.iscas.rtree;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName RTDirNode
 * @Description
 */
public class RTDirNode extends RTNode {
    /**
     *
     */
    protected List<RTNode> children;

    public RTDirNode(RTree rtree, RTNode parent, int level) {
        super(rtree, parent, level);
        children = new ArrayList<RTNode>();
    }

    /**
     * @param index
     * @return
     */
    public RTNode getChild(int index) {
        return children.get(index);
    }

    @Override
    public RTDataNode chooseLeaf(Rectangle rectangle) {
        int index;

        switch (rtree.getTreeType()) {
            case Constants.RTREE_LINEAR:

            case Constants.RTREE_QUADRATIC:

            case Constants.RTREE_EXPONENTIAL:
                index = findLeastEnlargement(rectangle);
                break;
            case Constants.RSTAR:
                if (level == 1)
                {
                    index = findLeastOverlap(rectangle);
                } else {
                    index = findLeastEnlargement(rectangle);
                }
                break;

            default:
                throw new IllegalStateException("Invalid tree type.");
        }

        insertIndex = index;

        return getChild(index).chooseLeaf(rectangle);
    }

    /**
     * @param rectangle
     * @return
     */
    private int findLeastOverlap(Rectangle rectangle) {
        // float overlap = Float.POSITIVE_INFINITY;
        BigInteger overlap = null;
        int sel = -1;

        for (int i = 0; i < usedSpace; i++) {
            RTNode node = getChild(i);
            BigInteger ol = BigInteger.ZERO;

            for (int j = 0; j < node.data.length; j++) {
                ol = ol.add(rectangle.intersectingArea(node.data[j]));
                // ol += rectangle.intersectingArea(node.data[j]);
            }
            if(overlap == null || ol.compareTo(overlap) < 0){
                overlap = ol;
                sel = i;
            }
            // if (ol < overlap) {
            //     overlap = ol;
            //     sel = i;
            // }
            else if (ol == overlap) {
                BigInteger area1 = data[i].getUnionRectangle(rectangle).getArea().subtract(data[i].getArea());
                BigInteger area2 = data[sel].getUnionRectangle(rectangle).getArea().subtract(data[sel].getArea());
                // double area1 = data[i].getUnionRectangle(rectangle).getArea() - data[i].getArea();
                // double area2 = data[sel].getUnionRectangle(rectangle).getArea() - data[sel].getArea();

                if (area1 == area2) {
                    sel = (data[sel].getArea().compareTo(data[i].getArea()) <= 0) ? sel : i;
                    // sel = (data[sel].getArea() <= data[i].getArea()) ? sel : i;
                } else {
                    sel = (area1.compareTo(area2) < 0) ? i : sel;
                    // sel = (area1 < area2) ? i : sel;
                }
            }
        }
        return sel;
    }

    /**
     * @param rectangle
     * @return
     */
    private int findLeastEnlargement(Rectangle rectangle) {
        BigInteger area = null;
        // double area = Double.POSITIVE_INFINITY;
        int sel = -1;

        for (int i = 0; i < usedSpace; i++) {
            BigInteger enlargement = data[i].getUnionRectangle(rectangle).getArea().subtract(data[i].getArea());
            if (area == null || enlargement.compareTo(area) < 0) {
                area = enlargement;
                sel = i;
            } else if (enlargement == area) {
                sel = (data[sel].getArea().compareTo(data[i].getArea()) < 0) ? sel : i;
                // sel = (data[sel].getArea() < data[i].getArea()) ? sel : i;
            }
        }

        return sel;
    }

    /**
     *
     *
     * @param node1
     *
     * @param node2
     *
     */
    public void adjustTree(RTNode node1, RTNode node2) {
        data[insertIndex] = node1.getNodeRectangle();
        children.set(insertIndex, node1);

        if (node2 != null) {
            insert(node2);

        }
        else if (!isRoot()) {
            RTDirNode parent = (RTDirNode) getParent();
            parent.adjustTree(this, null);
        }
    }

    /**
     *
     *
     * @param node
     * @return
     */
    protected boolean insert(RTNode node) {
        if (usedSpace < rtree.getDirNodeCapacity()) {
            data[usedSpace++] = node.getNodeRectangle();
            children.add(node);
            node.parent = this;
            RTDirNode parent = (RTDirNode) getParent();
            if (parent != null)
            {
                parent.adjustTree(this, null);
            }
            return false;
        } else {
            RTDirNode[] a = splitIndex(node);
            RTDirNode n = a[0];
            RTDirNode nn = a[1];

            if (isRoot()) {
                RTDirNode newRoot = new RTDirNode(rtree, Constants.NULL, level + 1);

                newRoot.addData(n.getNodeRectangle());
                newRoot.addData(nn.getNodeRectangle());

                newRoot.children.add(n);
                newRoot.children.add(nn);

                n.parent = newRoot;
                nn.parent = newRoot;

                rtree.setRoot(newRoot);
            } else {
                RTDirNode p = (RTDirNode) getParent();
                p.adjustTree(n, nn);
            }
        }
        return true;
    }

    /**
     *
     *
     * @param node
     * @return
     */
    private RTDirNode[] splitIndex(RTNode node) {
        int[][] group = null;
        switch (rtree.getTreeType()) {
            case Constants.RTREE_LINEAR:
                break;
            case Constants.RTREE_QUADRATIC:
                group = quadraticSplit(node.getNodeRectangle());
                children.add(node);
                node.parent = this;
                break;
            case Constants.RTREE_EXPONENTIAL:
                break;
            case Constants.RSTAR:
                break;
            default:
                throw new IllegalStateException("Invalid tree type.");
        }
        RTDirNode index1 = new RTDirNode(rtree, parent, level);
        RTDirNode index2 = new RTDirNode(rtree, parent, level);

        int[] group1 = group[0];
        int[] group2 = group[1];
        for (int i = 0; i < group1.length; i++) {
            index1.addData(data[group1[i]]);
            index1.children.add(this.children.get(group1[i]));
            this.children.get(group1[i]).parent = index1;
        }
        for (int i = 0; i < group2.length; i++) {
            index2.addData(data[group2[i]]);
            index2.children.add(this.children.get(group2[i]));
            this.children.get(group2[i]).parent = index2;
        }
        return new RTDirNode[] { index1, index2 };
    }

    @Override
    protected RTDataNode findLeaf(Rectangle rectangle) {
        for (int i = 0; i < usedSpace; i++) {
            if (data[i].enclosure(rectangle)) {
                deleteIndex = i;
                RTDataNode leaf = children.get(i).findLeaf(rectangle);
                if (leaf != null)
                    return leaf;
            }
        }
        return null;
    }

}