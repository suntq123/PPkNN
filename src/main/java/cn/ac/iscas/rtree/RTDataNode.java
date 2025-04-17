package cn.ac.iscas.rtree;

import java.util.ArrayList;
import java.util.List;


/**
 * @ClassName RTDataNode
 * @Description
 */
public class RTDataNode extends RTNode {

//    private int id;

    public RTDataNode(RTree rTree, RTNode parent) {
        super(rTree, parent, 0);
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    public boolean insert(Rectangle rectangle) {
        if (usedSpace < rtree.getDataNodeCapacity())
        {
            data[usedSpace++] = rectangle;
            RTDirNode parent = (RTDirNode) getParent();

            if (parent != null)
                parent.adjustTree(this, null);
            return true;
        }
        else {
            RTDataNode[] splitNodes = splitLeaf(rectangle);
            RTDataNode l = splitNodes[0];
            RTDataNode ll = splitNodes[1];

            if (isRoot()) {
                RTDirNode rDirNode = new RTDirNode(rtree, Constants.NULL, level + 1);
                rtree.setRoot(rDirNode);
                rDirNode.addData(l.getNodeRectangle());
                rDirNode.addData(ll.getNodeRectangle());

                ll.parent = rDirNode;
                l.parent = rDirNode;

                rDirNode.children.add(l);
                rDirNode.children.add(ll);

            } else {
                RTDirNode parentNode = (RTDirNode) getParent();
                parentNode.adjustTree(l, ll);
            }

        }
        return true;
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    public RTDataNode[] splitLeaf(Rectangle rectangle) {
        int[][] group = null;

        switch (rtree.getTreeType()) {
            case Constants.RTREE_LINEAR:
                break;
            case Constants.RTREE_QUADRATIC:
                group = quadraticSplit(rectangle);
                break;
            case Constants.RTREE_EXPONENTIAL:
                break;
            case Constants.RSTAR:
                break;
            default:
                throw new IllegalArgumentException("Invalid tree type.");
        }

        RTDataNode l = new RTDataNode(rtree, parent);
        RTDataNode ll = new RTDataNode(rtree, parent);

        int[] group1 = group[0];
        int[] group2 = group[1];

        for (int i = 0; i < group1.length; i++) {
            l.addData(data[group1[i]]);
        }

        for (int i = 0; i < group2.length; i++) {
            ll.addData(data[group2[i]]);
        }
        return new RTDataNode[] { l, ll };
    }

    @Override
    public RTDataNode chooseLeaf(Rectangle rectangle) {
        insertIndex = usedSpace;
        return this;
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    protected int delete(Rectangle rectangle) {
        for (int i = 0; i < usedSpace; i++) {
            if (data[i].equals(rectangle)) {
                deleteData(i);
                List<RTNode> deleteEntriesList = new ArrayList<RTNode>();
                condenseTree(deleteEntriesList);

                for (int j = 0; j < deleteEntriesList.size(); j++) {
                    RTNode node = deleteEntriesList.get(j);
                    if (node.isLeaf())
                    {
                        for (int k = 0; k < node.usedSpace; k++) {
                            rtree.insert(node.data[k]);
                        }
                    } else {
                        List<RTNode> traverseNodes = rtree.traversePostOrder(node);

                        for (int index = 0; index < traverseNodes.size(); index++) {
                            RTNode traverseNode = traverseNodes.get(index);
                            if (traverseNode.isLeaf()) {
                                for (int t = 0; t < traverseNode.usedSpace; t++) {
                                    rtree.insert(traverseNode.data[t]);
                                }
                            }
                        }

                    }
                }

                return deleteIndex;
            } // end if
        } // end for
        return -1;
    }

    @Override
    protected RTDataNode findLeaf(Rectangle rectangle) {
        for (int i = 0; i < usedSpace; i++) {
            if (data[i].enclosure(rectangle)) {
                deleteIndex = i;
                return this;
            }
        }
        return null;
    }

}