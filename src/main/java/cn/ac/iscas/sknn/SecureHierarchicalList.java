package cn.ac.iscas.sknn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static cn.ac.iscas.secretsharing.AdditiveSecretSharing.*;

import cn.ac.iscas.rtree.Point;
import cn.ac.iscas.rtree.Rectangle;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.MultiplicationTriple;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.PartyID;
import cn.ac.iscas.utils.DataProcessor;

public class SecureHierarchicalList {

    /**
     *
     */
    public static class SecureHierarchicalNode {

        BigInteger[] point = null;

        BigInteger element; // ptr: to a node or a point 
        BigInteger distance; // The distance from this element to the query point.
        BigInteger alpha;
        BigInteger beta;

        public SecureHierarchicalNode(BigInteger[] point, BigInteger element, BigInteger distance, BigInteger alpha,
                BigInteger beta) {
            this.point = point;
            this.element = element;
            this.distance = distance;
            this.alpha = alpha;
            this.beta = beta;
        }

        public SecureHierarchicalNode(BigInteger[] point, BigInteger distance, BigInteger alpha, BigInteger beta) {
            this.point = point;
            this.distance = distance;
            this.alpha = alpha;
            this.beta = beta;
        }

        public SecureHierarchicalNode(BigInteger element, BigInteger distance, BigInteger alpha, BigInteger beta) {
            this.element = element;
            this.distance = distance;
            this.alpha = alpha;
            this.beta = beta;
        }

        public SecureHierarchicalNode(BigInteger distance, BigInteger alpha, BigInteger beta) {
            this.distance = distance;
            this.alpha = alpha;
            this.beta = beta;
        }

        public BigInteger getElement() {
            return element;
        }

        public BigInteger getDistance() {
            return distance;
        }

        public BigInteger getAlpha() {
            return alpha;
        }

        public BigInteger getBeta() {
            return beta;
        }
    }

    public static BigInteger MAX_VALUE;

    private PartyID partyID;
    private MultiplicationTriple triple;
    private BigInteger mod;
    private BufferedReader reader;
    private PrintWriter writer;

    private ComparisonTuple cTuple;

    private List<List<SecureHierarchicalNode>> tree;

    public SecureHierarchicalList(PartyID partyID, MultiplicationTriple triple, ComparisonTuple cTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) {
        this.partyID = partyID;
        this.triple = triple;
        this.cTuple = cTuple;
        this.mod = mod;
        this.reader = reader;
        this.writer = writer;

        this.tree = new ArrayList<>();
        tree.add(new ArrayList<>()); //
    }

    private SecureHierarchicalNode getTopNode() {
        return tree.get(tree.size() - 1).get(0);
    }

    /**
     *
     * 
     * @return
     * @throws IOException
     */
    private BigInteger[] getMinNodeLabel() throws IOException {
        BigInteger[] sums = new BigInteger[] { BigInteger.ZERO, BigInteger.ZERO };
        for (SecureHierarchicalNode node : tree.get(0)) {
            BigInteger te = multiply(partyID, node.beta, node.element, triple, mod, reader, writer);
            sums[0] = add(sums[0], te, mod);

            BigInteger td = multiply(partyID, node.beta, node.distance, triple, mod, reader, writer);
            sums[1] = add(sums[1], td, mod);
        }

        return sums;
    }

    private BigInteger[] getBetas() {
        BigInteger[] betas = new BigInteger[tree.get(0).size()];

        for (int i = 0; i < tree.get(0).size(); i++) {
            betas[i] = tree.get(0).get(i).beta;
        }

        return betas;
    }

    private int getL() {
        return tree.get(0).size();
    }

    private void updateTreeBeta() throws IOException {
        for (int i = tree.size() - 2; i >= 0; i--) {
            for (int j = 0; j < tree.get(i).size(); j++) {
                tree.get(i).get(j).beta = multiply(partyID, tree.get(i + 1).get(j / 2).beta,
                        tree.get(i).get(j).alpha, triple, mod, reader, writer);
            }
        }
    }

    /**
     *
     */
    public void secureInsertion(BigInteger[][] data, BigInteger[][] points) throws IOException {
        for (int i = 0; i < data.length; i++) {
            tree.get(0).add(
                    new SecureHierarchicalNode(points[i], data[i][0], data[i][1], BigInteger.ZERO, BigInteger.ZERO));
        }

        int index = 1;
        int m = (int) Math.ceil(getL() / 2.0);
        do {
            if (tree.size() < index + 1)
                tree.add(new ArrayList<>());

            while (tree.get(index).size() < m)
                tree.get(index).add(new SecureHierarchicalNode(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO));

            if (m == 1)
                break;

            m = (int) Math.ceil(m / 2.0);
            index++;
        } while (true);

        int x = (int) Math.ceil((getL() - data.length + 1) / 2.0), y = (int) Math.ceil(getL() / 2.0);
        for (int i = 1; i < tree.size(); i++) {
            for (int j = x - 1; j < y; j++) {

                int lowerLevel = i - 1; 
                int leftChild = 2 * j, rightChild = 2 * j + 1;
                if (tree.get(lowerLevel).size() < 2 * (j + 1)) {
                    tree.get(lowerLevel).get(leftChild).alpha = shareConstant(partyID, BigInteger.ONE);

                    tree.get(i).get(j).distance = tree.get(lowerLevel).get(leftChild).distance;
                } else {
                    // tree.get(lowerLevel).get(leftChild).alpha = secureComparision(partyID,
                    //         tree.get(lowerLevel).get(leftChild).distance,
                    //         tree.get(lowerLevel).get(rightChild).distance, triple, mod, reader, writer);
                    tree.get(lowerLevel).get(leftChild).alpha = secureComparision(partyID,
                            tree.get(lowerLevel).get(leftChild).distance.longValue(),
                            tree.get(lowerLevel).get(rightChild).distance.longValue(), triple, cTuple, mod.longValue(),
                            reader, writer);

                    tree.get(lowerLevel).get(rightChild).alpha = subtract(shareConstant(partyID, BigInteger.ONE),
                            tree.get(lowerLevel).get(leftChild).alpha, mod);

                    BigInteger d1 = multiply(partyID, tree.get(lowerLevel).get(leftChild).distance,
                            tree.get(lowerLevel).get(leftChild).alpha, triple, mod, reader, writer);
                    BigInteger d2 = multiply(partyID, tree.get(lowerLevel).get(rightChild).distance,
                            tree.get(lowerLevel).get(rightChild).alpha, triple, mod, reader, writer);
                    tree.get(i).get(j).distance = add(d1, d2, mod);
                }
            }

            x = (int) Math.ceil(x / 2.0);
            y = (int) Math.ceil(y / 2.0);
        }

        index = tree.size() - 1;
        tree.get(index).get(0).alpha = shareConstant(partyID, BigInteger.ONE);
        tree.get(index).get(0).beta = shareConstant(partyID, BigInteger.ONE);

        updateTreeBeta();
    }

    /**
     *
     *
     * @throws IOException
     */
    public void secureDeletion() throws IOException {

        for (int i = 0; i < tree.get(0).size(); i++) {
            // C_1,C_2计算<dist> = <(1-beta) * dist + beta * MAX>
            BigInteger t1 = subtract(shareConstant(partyID, BigInteger.ONE), tree.get(0).get(i).beta, mod); // 1-beta
            t1 = multiply(partyID, t1, tree.get(0).get(i).distance, triple, mod, reader, writer); // (1-beta) * dist

            BigInteger t2 = multiply(partyID, tree.get(0).get(i).beta,
                    shareConstant(partyID, shareConstant(partyID, MAX_VALUE)), triple, mod, reader, writer); // beta * MAX

            tree.get(0).get(i).distance = add(t1, t2, mod);
        }

        for (int i = 1; i < tree.size(); i++) {
            int lowerLevel = i - 1;

            // C_1,C_2<temp1>,<temp2>
            BigInteger temp1 = BigInteger.ZERO, temp2 = BigInteger.ZERO;
            for (int j = 0; j < tree.get(i).size(); j++) {
                int leftChild = 2 * j, rightChild = 2 * j + 1;

                BigInteger t1 = multiply(partyID, tree.get(lowerLevel).get(leftChild).distance, tree.get(i).get(j).beta,
                        triple, mod, reader, writer);
                temp1 = add(temp1, t1, mod);

                BigInteger t2 = (tree.get(lowerLevel).size() < 2 * (j + 1)) ? BigInteger.ZERO
                        : multiply(partyID, tree.get(lowerLevel).get(rightChild).distance, tree.get(i).get(j).beta,
                                triple, mod, reader, writer);

                temp2 = add(temp2, t2, mod);
            }

            // C_1,C_2alpha_{temp}=Bool(temp1 < temp2)
            // BigInteger alphaTemp = secureComparision(partyID, temp1, temp2, triple, mod, reader, writer);
            BigInteger alphaTemp = secureComparision(partyID, temp1.longValue(), temp2.longValue(), triple, cTuple,
                    mod.longValue(), reader, writer);

            for (int j = 0; j < tree.get(i).size(); j++) {
                int leftChild = 2 * j, rightChild = 2 * j + 1;

                if (tree.get(lowerLevel).size() < 2 * (j + 1)) {
                    tree.get(lowerLevel).get(leftChild).alpha = shareConstant(partyID, BigInteger.ONE);
                    tree.get(i).get(j).distance = tree.get(lowerLevel).get(leftChild).distance;
                } else {
                    BigInteger t1 = subtract(shareConstant(partyID, BigInteger.ONE), tree.get(i).get(j).beta, mod); // 1-beta
                    t1 = multiply(partyID, t1, tree.get(lowerLevel).get(leftChild).alpha, triple, mod, reader,
                            writer); // (1-beta) * alpha

                    BigInteger t2 = multiply(partyID, tree.get(i).get(j).beta, alphaTemp, triple, mod, reader,
                            writer);

                    tree.get(lowerLevel).get(leftChild).alpha = add(t1, t2, mod);
                    tree.get(lowerLevel).get(rightChild).alpha = subtract(shareConstant(partyID, BigInteger.ONE),
                            tree.get(lowerLevel).get(leftChild).alpha, mod);

                    t1 = multiply(partyID, tree.get(lowerLevel).get(leftChild).distance,
                            tree.get(lowerLevel).get(leftChild).alpha,
                            triple, mod, reader, writer);
                    t2 = multiply(partyID, tree.get(lowerLevel).get(rightChild).distance,
                            tree.get(lowerLevel).get(rightChild).alpha,
                            triple, mod, reader, writer);
                    tree.get(i).get(j).distance = add(t1, t2, mod);
                }
            }
        }

        updateTreeBeta();
    }

    /**
     *
     *
     * @param rectangle
     * @return
     */
    public static BigInteger[][] getRectangleData(Rectangle rectangle) {
        BigInteger[][] data = new BigInteger[rectangle.getDimension()][2];

        for (int i = 0; i < rectangle.getDimension(); i++) {
            data[i][0] = rectangle.getLow().indexOf(i);
            data[i][1] = rectangle.getHigh().indexOf(i);
        }

        return data;
    }

    // public static BigInteger[] getPointData(Point point) {
    //     BigInteger[] data = new BigInteger[point.getDimension()];

    //     for (int i = 0; i < point.getDimension(); i++) {
    //         data[i] = point.indexOf(i);
    //     }

    //     return data;
    // }

    /**
     * {([id], [distance], [p])_1, ..., ([id], [distance], [p])_num}
     * 
     * p point
     */
    public static List<BigInteger[]> secureLinearKNNSearch(PartyID partyID, List<DataProcessor.Entry> entries,
            int k, Point point, int pow, MultiplicationTriple triple, ComparisonTuple cTuple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        MAX_VALUE = BigInteger.valueOf(cTuple.twoPowS - 1L);

        SecureHierarchicalList ssi = new SecureHierarchicalList(partyID, triple, cTuple, mod, reader, writer);

        int entryNumber = entries.size();
        BigInteger[][] entryData = new BigInteger[entryNumber][2];
        BigInteger[][] pointsData = new BigInteger[entryNumber][];
        for (int j = 0; j < entryNumber; j++) {
            DataProcessor.Entry entry = entries.get(j);

            BigInteger distancei = secureMinkowskiDistance(partyID, pow, point.getData(), entry.getPoint().getData(),
                    triple, cTuple, mod, reader, writer);

            entryData[j][0] = entry.getPtr();
            entryData[j][1] = distancei;

            pointsData[j] = entry.getPoint().getData();
        }
        ssi.secureInsertion(entryData, pointsData);

        int count = 0;
        List<BigInteger[]> result = new ArrayList<>();
        while (count < k) {
            BigInteger[] betas = ssi.getBetas();

            BigInteger[] pi = new BigInteger[point.getDimension()];
            for (int i = 0; i < point.getDimension(); i++) {
                pi[i] = BigInteger.ZERO;
                for (int j = 0; j < entryNumber; j++) {
                    BigInteger t = multiply(partyID, entries.get(j).getPoint().indexOf(i), betas[j], triple, mod,
                            reader, writer);

                    pi[i] = add(pi[i], t, mod);
                }
            }

            BigInteger[] minNode = ssi.getMinNodeLabel();

            BigInteger[] r = new BigInteger[2 + point.getDimension()];
            System.arraycopy(minNode, 0, r, 0, 2);
            System.arraycopy(pi, 0, r, 2, point.getDimension());

            result.add(r);
            count++;

            ssi.secureDeletion();
        }

        return result;
    }

    public static List<BigInteger[]> secureBucketKNNSearch(PartyID partyID, List<DataProcessor.Node> bucketsSecret,
            int k, Point point, int pow, MultiplicationTriple triple, ComparisonTuple cTuple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        MAX_VALUE = BigInteger.valueOf(cTuple.twoPowS - 1L);

        SecureHierarchicalList ssi = new SecureHierarchicalList(partyID, triple, cTuple, mod, reader, writer);

        int bNum = bucketsSecret.size();
        int bSize = bucketsSecret.get(0).getEntries().size();
        int d = point.getDimension();
        BigInteger[][] entryData = new BigInteger[bNum][2];
        BigInteger[][] pointsData = new BigInteger[bNum][];
        for (int j = 0; j < bNum; j++) {
            Rectangle mbr = bucketsSecret.get(j).rectangle;

            BigInteger distancei = secureMinDistanceFromBoxToPoint(partyID, pow, getRectangleData(mbr), point.getData(),
                    triple, cTuple, mod, reader, writer);

            entryData[j][0] = BigInteger.valueOf(j);
            entryData[j][1] = distancei;

            pointsData[j] = null;
        }
        ssi.secureInsertion(entryData, pointsData);

        // int count = 0;
        List<DataProcessor.Entry> tpi = new ArrayList<>();
        BigInteger tmdi = shareConstant(partyID, MAX_VALUE);
        while (true) {
            BigInteger[] betas = ssi.getBetas();

            BigInteger[] idsi = new BigInteger[bSize];
            BigInteger[][] psi = new BigInteger[bSize][d];
            for (int i = 0; i < bSize; i++) {
                for (int j = 0; j < d; j++) {
                    psi[i][j] = BigInteger.ZERO;
                    for (int l = 0; l < bNum; l++) {
                        BigInteger t = multiply(partyID, bucketsSecret.get(l).getEntries().get(i).getPoint().indexOf(j),
                                betas[l], triple, mod, reader, writer);

                        psi[i][j] = add(psi[i][j], t, mod);
                    }
                }

                idsi[i] = BigInteger.ZERO;
                for (int l = 0; l < bNum; l++) {
                    BigInteger t = multiply(partyID, bucketsSecret.get(l).getEntries().get(i).getPtr(),
                            betas[l], triple, mod, reader, writer);

                    idsi[i] = add(idsi[i], t, mod);
                }
            }
            BigInteger[] minNode = ssi.getMinNodeLabel();
            ssi.secureDeletion();

            for (int i = 0; i < bSize; i++) {
                Point tp = new Point(psi[i]);
                tpi.add(new DataProcessor.Entry(tp, idsi[i]));
            }

            List<BigInteger[]> tks = secureLinearKNNSearch(partyID, tpi, k, point, pow, triple, cTuple, mod, reader,
                    writer);

            tmdi = tks.get(k - 1)[1];

            BigInteger taui = secureComparision(partyID, tmdi.longValue(), minNode[1].longValue(), triple,
                    cTuple, mod.longValue(), reader, writer);
            BigInteger tau = recover(partyID, taui, mod, reader, writer);

            if (tau.equals(BigInteger.ONE)) {
                return tks;
            }

            tpi.clear();
            for (int i = 0; i < k; i++) {
                BigInteger[] pData = new BigInteger[d];
                System.arraycopy(tks.get(i), 2, pData, 0, d);
                Point tp = new Point(pData);
                tpi.add(new DataProcessor.Entry(tp, tks.get(i)[0]));
            }
        }
    }

    public static List<BigInteger[]> secureKNNSearch(PartyID partyID, List<DataProcessor.Node> arrayTSecret,
            int k, Point point, int pow, MultiplicationTriple triple, ComparisonTuple cTuple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        MAX_VALUE = BigInteger.valueOf(cTuple.twoPowS - 1L);

        List<BigInteger[]> result = new ArrayList<>();

        SecureHierarchicalList ss1 = new SecureHierarchicalList(partyID, triple, cTuple, mod, reader, writer);
        SecureHierarchicalList ss2 = new SecureHierarchicalList(partyID, triple, cTuple, mod, reader, writer);

        // ss1.secureInsertion(new BigInteger[][] {
        //         { BigInteger.ZERO, mod.subtract(BigInteger.ONE).divide(BigInteger.TWO).subtract(BigInteger.TWO) } });
        // ss2.secureInsertion(
        //         new BigInteger[][] { { BigInteger.ZERO, mod.subtract(BigInteger.ONE).divide(BigInteger.TWO) } });
        // ss1.secureInsertion(new int[][] { { 0, (mod - 1) / 2 - 2 } });   
        // ss2.secureInsertion(new int[][] { { 0, (mod - 1) / 2 } });

        ss1.secureInsertion(new BigInteger[][] {
                { BigInteger.ZERO, shareConstant(partyID, BigInteger.ZERO) } },
                new BigInteger[][] {
                        { null, null } });
        ss2.secureInsertion(
                new BigInteger[][] { { BigInteger.ZERO, shareConstant(partyID, MAX_VALUE) } }, new BigInteger[][] {
                        { null, null } });

        BigInteger[] pointData = point.getData();
        int count = 0;
        while (count < k) {

            // BigInteger taui = secureComparision(partyID, ss1.getTopNode().distance, ss2.getTopNode().distance,
            //         triple, mod, reader, writer);
            BigInteger taui = secureComparision(partyID, ss1.getTopNode().distance.longValue(),
                    ss2.getTopNode().distance.longValue(), triple, cTuple, mod.longValue(), reader, writer);

            BigInteger tau = recover(partyID, taui, mod, reader, writer);
            // System.out.println("tau = " + tau);

            if (tau.equals(BigInteger.ONE)) {

                BigInteger labeli = ss1.getMinNodeLabel()[0];
                BigInteger labelb = recover(partyID, labeli, mod, reader, writer);

                int label = labelb.intValue();
                boolean isLeaf = arrayTSecret.get(label).getEntries().get(0).getLeafFlag();

                int entryNumber = arrayTSecret.get(label).getEntries().size();
                if (!isLeaf) {

                    BigInteger[][] entryData = new BigInteger[entryNumber][2];
                    BigInteger[][] pointsData = new BigInteger[entryNumber][];
                    for (int j = 0; j < entryNumber; j++) {
                        DataProcessor.Entry entry = arrayTSecret.get(label).getEntries().get(j);

                        BigInteger[][] boxi = getRectangleData(entry.getRectangle());

                        BigInteger distancei = secureMinDistanceFromBoxToPoint(partyID, pow, boxi, pointData, triple,
                                cTuple, mod, reader, writer);

                        entryData[j][0] = entry.getPtr();
                        entryData[j][1] = distancei;

                        // pointsData[j] = entry.getPoint().getData();
                        pointsData[j] = null;
                    }

                    ss1.secureDeletion();

                    ss1.secureInsertion(entryData, pointsData);
                } else {

                    BigInteger[][] entryData = new BigInteger[entryNumber][2];
                    BigInteger[][] pointsData = new BigInteger[entryNumber][];
                    for (int j = 0; j < entryNumber; j++) {
                        DataProcessor.Entry entry = arrayTSecret.get(label).getEntries().get(j);

                        BigInteger distancei = secureMinkowskiDistance(partyID, pow, entry.getPoint().getData(),
                                pointData, triple, cTuple, mod, reader, writer);

                        entryData[j][0] = entry.getPtr();
                        entryData[j][1] = distancei;

                        // pointsData[j] = entry.getPoint().getData();
                        pointsData[j] = null;
                    }

                    ss2.secureInsertion(entryData, pointsData);

                    ss1.secureDeletion();
                }
            } else {
                result.add(ss2.getMinNodeLabel());
                count++;

                ss2.secureDeletion();
            }
        }

        return result;
    }
}
