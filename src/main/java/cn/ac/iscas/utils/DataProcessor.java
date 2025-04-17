package cn.ac.iscas.utils;

import cn.ac.iscas.kdtree.KDTreeNode;
import cn.ac.iscas.rtree.*;

import java.io.*;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cn.ac.iscas.sknn.SKNNV2;
import com.alibaba.fastjson.JSONObject;

import static cn.ac.iscas.secretsharing.AdditiveSecretSharing.randomSplit;

public class DataProcessor {

    public static class Entry {

        private boolean leafFlag;

        // Rectangle or Point
        private Rectangle rectangle;
        private Point point;

        // 
        private BigInteger ptr;

        public Entry() {
        }

        public Entry(Rectangle rectangle, BigInteger ptr) {
            this.leafFlag = false;

            this.rectangle = rectangle;
            this.ptr = ptr;
        }

        public Entry(Point point, BigInteger ptr) {
            this.leafFlag = true;

            this.point = point;
            this.ptr = ptr;
        }

        public void setLeafFlag(boolean leafFlag) {
            this.leafFlag = leafFlag;
        }

        public void setRectangle(Rectangle rectangle) {
            this.rectangle = rectangle;
        }

        public void setPoint(Point point) {
            this.point = point;
        }

        public void setPtr(BigInteger ptr) {
            this.ptr = ptr;
        }

        public boolean getLeafFlag() {
            return leafFlag;
        }

        public Rectangle getRectangle() {
            return rectangle;
        }

        public Point getPoint() {
            return point;
        }

        public BigInteger getPtr() {
            return ptr;
        }

        public String toString() {
            String s = "";
            if (leafFlag)
                s = "[Point=" + point + " id=" + ptr + "]";
            else
                s = "[MBR=Low:" + rectangle.getLow() + " High:" + rectangle.getHigh() + " lbl:" + ptr + "]";

            return s;
        }
    }

    /**
     *
     */
    public static class Node {

        // MBB
        public Rectangle rectangle;

        // Entry List
        private List<Entry> entries;

        public Node(Rectangle rectangle) {
            this.rectangle = rectangle;
            entries = new ArrayList<>();
        }

        public void add(Entry entry) {
            entries.add(entry);
        }

        public Entry get(int i) {
            return entries.get(i);
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public String toString() {
            StringBuilder s = new StringBuilder("{");
            for (Entry entry : entries) {
                s.append(entry).append(",");
            }
            s.append("}");
            return s.toString();
        }
    }

    /**
     *
     *
     * @param rTree
     * @return
     */
    public static List<Node> constructArrayT(RTree rTree) {

        List<Node> arrayT = new ArrayList<>();

        List<RTNode> list = new ArrayList<>();
        list.add(rTree.getRoot());

        int index = 0;
        while (index < list.size()) {
            Node node = new Node(list.get(index).getNodeRectangle());

            if (!list.get(index).isLeaf()) {
                for (int i = 0; i < list.get(index).getUsedSpace(); i++) {
                    list.add(((RTDirNode) list.get(index)).getChild(i));
                    BigInteger label = BigInteger.valueOf(list.size() - 1);

                    //                    Rectangle rectangle = ((RTDirNode) list.get(index)).getChild(i).getNodeRectangle();
                    Rectangle rectangle = list.get(index).getData()[i];

                    node.add(new Entry(rectangle, label));
                }
            } else {
                for (int i = 0; i < list.get(index).getUsedSpace(); i++) {
                    Rectangle rectangle = list.get(index).getData()[i];

                    Point point = rectangle.getLow();
                    BigInteger id = rectangle.getData();

                    node.add(new Entry(point, id));
                }
            }

            arrayT.add(node);
            index++;
        }

        return arrayT;
    }

    public static List<DataProcessor.Node> generateBuckets(List<KDTreeNode> kdLeafNodes) {
        List<DataProcessor.Node> buckets = new ArrayList<>();

        for (KDTreeNode kdNode : kdLeafNodes) {
            Point lb = new Point(kdNode.lb.data);
            Point ub = new Point(kdNode.ub.data);
            Rectangle rectangle = new Rectangle(lb, ub);

            Node node = new Node(rectangle);
            for (int i = 0; i < kdNode.points.length; i++) {
                Point point = new Point(kdNode.points[i].data);
                BigInteger id = kdNode.points[i].id;

                node.add(new Entry(point, id));
            }

            buckets.add(node);
        }

        int bSize = 0;
        for (Node node : buckets) {
            if (node.getEntries().size() > bSize)
                bSize = node.getEntries().size();
        }

        int fillNum = 0;
        for (Node node : buckets) {
            fillNum += bSize - node.getEntries().size();
            while (node.getEntries().size() < bSize) {
                node.getEntries().add(node.getEntries().get(0));
            }
        }
        System.out.println("Fill Number: " + fillNum);

        return buckets;
    }

    // public static List<DataProcessor.Node> generateBuckets(RTree tree) {
    //     List<DataProcessor.Node> buckets = new ArrayList<>();

    //     List<RTNode> list = new ArrayList<>();
    //     list.add(tree.getRoot());

    //     int index = 0;
    //     while (index < list.size()) {

    //         if (!list.get(index).isLeaf()) {
    //             for (int i = 0; i < list.get(index).getUsedSpace(); i++) {
    //                 list.add(((RTDirNode) list.get(index)).getChild(i));
    //             }
    //         } else {
    //             Node node = new Node(list.get(index).getNodeRectangle());
    //             for (int i = 0; i < list.get(index).getUsedSpace(); i++) {
    //                 Rectangle rectangle = list.get(index).getData()[i];

    //                 Point point = rectangle.getLow();
    //                 BigInteger id = rectangle.getData();

    //                 node.add(new Entry(point, id));
    //             }

    //             buckets.add(node);
    //         }

    //         index++;
    //     }

    //     int bSize = 0;
    //     for (Node node : buckets) {
    //         if (node.getEntries().size() > bSize)
    //             bSize = node.getEntries().size();
    //     }

    //     int fillNum = 0;
    //     for (Node node : buckets) {
    //         fillNum += bSize - node.getEntries().size();
    //         while (node.getEntries().size() < bSize) {
    //             node.getEntries().add(node.getEntries().get(0));
    //         }
    //     }
    //     System.out.println("Fill Number: " + fillNum);

    //     return buckets;
    // }

    public static Point[] sharePoint(Point point, BigInteger mod) {
        BigInteger[] point1Data = new BigInteger[point.getDimension()];
        BigInteger[] point2Data = new BigInteger[point.getDimension()];
        for (int i = 0; i < point.getDimension(); i++) {
            BigInteger[] xSecrets = randomSplit(point.getData()[i], mod);
            point1Data[i] = xSecrets[0];
            point2Data[i] = xSecrets[1];
        }

        return new Point[] { new Point(point1Data), new Point(point2Data) };
    }

    public static Rectangle[] shareRectangle(Rectangle rectangle, BigInteger mod) {
        Point[] lowSecrets = sharePoint(rectangle.getLow(), mod);
        Point[] highSecrets = sharePoint(rectangle.getHigh(), mod);

        return new Rectangle[] { new Rectangle(lowSecrets[0], highSecrets[0], true),
                new Rectangle(lowSecrets[1], highSecrets[1], true) };
    }

    public static List<List<Node>> shareArrayT(List<Node> arrayT, BigInteger mod) {
        List<List<Node>> secrets = new ArrayList<>();

        List<Node> secretC1 = new ArrayList<>();
        List<Node> secretC2 = new ArrayList<>();

        for (Node node : arrayT) {
            Rectangle mbr = node.rectangle;
            Rectangle[] mbrSecrets = shareRectangle(mbr, mod);

            Node node1 = new Node(mbrSecrets[0]);
            Node node2 = new Node(mbrSecrets[1]);
            for (Entry entry : node.entries) {
                Entry entry1, entry2;
                BigInteger[] ptrSecrets = randomSplit(entry.ptr, mod);

                if (entry.leafFlag) {
                    Point[] pointSecrets = sharePoint(entry.point, mod);
                    entry1 = new Entry(pointSecrets[0], ptrSecrets[0]);
                    entry2 = new Entry(pointSecrets[1], ptrSecrets[1]);
                } else {
                    Rectangle[] rectangleSecrets = shareRectangle(entry.rectangle, mod);
                    entry1 = new Entry(rectangleSecrets[0], ptrSecrets[0]);
                    entry2 = new Entry(rectangleSecrets[1], ptrSecrets[1]);
                }

                node1.add(entry1);
                node2.add(entry2);
            }

            secretC1.add(node1);
            secretC2.add(node2);
        }

        secrets.add(secretC1);
        secrets.add(secretC2);

        return secrets;
    }

    public static void sendNodeArray(List<Node> nodeArray, PrintWriter writer) {
        writer.write(JSONObject.toJSONString(nodeArray) + "\n");
        writer.flush();
    }

    public static List<Node> receiveNodeArray(BufferedReader reader) throws IOException {
        return JSONObject.parseArray(reader.readLine(), Node.class);
    }

    public static void saveNodeArray(String filePath, List<Node> nodeArray) throws FileNotFoundException {
        String json = JSONObject.toJSONString(nodeArray);

        File file = new File(filePath);
        PrintWriter writer = new PrintWriter(file);
        writer.println(json);
        writer.close();
    }

    public static List<Node> loadNodeArray(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String json = reader.readLine();
        reader.close();

        return JSONObject.parseArray(json, Node.class);
    }

    public static List<List<Entry>> shareEntries(List<Entry> entries, BigInteger mod) {
        List<List<Entry>> secrets = new ArrayList<>();

        List<Entry> secretC1 = new ArrayList<>();
        List<Entry> secretC2 = new ArrayList<>();

        for (Entry entry : entries) {
            Point[] pointSecrets = sharePoint(entry.point, mod);
            BigInteger[] ptrSecrets = randomSplit(entry.ptr, mod);

            secretC1.add(new Entry(pointSecrets[0], ptrSecrets[0]));
            secretC2.add(new Entry(pointSecrets[1], ptrSecrets[1]));
        }

        secrets.add(secretC1);
        secrets.add(secretC2);

        return secrets;
    }

    public static void sendEntries(List<Entry> entries, PrintWriter writer) {
        writer.write(JSONObject.toJSONString(entries) + "\n");
        writer.flush();
    }

    public static List<Entry> receiveEntries(BufferedReader reader) throws IOException {
        return JSONObject.parseArray(reader.readLine(), Entry.class);
    }

    public static void sendQueryCondition(int k, Point point, PrintWriter writer) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("k", k);
        jsonObject.put("point", point);

        writer.write(jsonObject.toJSONString() + "\n");
        writer.flush();
    }

    public static JSONObject receiveQueryConditionJson(BufferedReader reader) throws IOException {
        return JSONObject.parseObject(reader.readLine());
    }

    /**
     *
     *
     * @param dimension
     * @param number
     * @param bitLength
     * @return 
     */
    public static BigInteger[][] generateDataset(int dimension, int number, int bitLength, Random random) {
        BigInteger[][] dataset = new BigInteger[number][dimension + 1];

        // Random random = new Random();
        for (int i = 0; i < number; i++) {
            for (int j = 0; j < dimension; j++) {
                dataset[i][j] = new BigInteger(bitLength, random);
            }
            dataset[i][dimension] = BigInteger.valueOf(i); // ptr
        }

        return dataset;
    }

    public static Set<BigInteger> getKNearest(BigInteger[][] dataset, BigInteger[] point,
            int dimension, int pow, int k) {
        Set<BigInteger> result = new HashSet<>();

        // <pId, distance>
        List<SimpleEntry<BigInteger, BigInteger>> knnList = new ArrayList<>();
        for (int i = 0; i < dataset.length; i++) {
            BigInteger id = dataset[i][dimension];

            BigInteger d = BigInteger.ZERO;
            for (int j = 0; j < dimension; j++) {
                BigInteger absDis = dataset[i][j].subtract(point[j]).abs();
                if (pow <= 0) {
                    if (d.compareTo(absDis) < 0)
                        d = absDis;
                } else {
                    d = d.add(absDis.pow(pow));
                }
            }

            knnList.add(new SimpleEntry<BigInteger, BigInteger>(id, d));
        }

        knnList.sort((e1, e2) -> {
            return e1.getValue().compareTo(e2.getValue());
        });

        for (int i = 0; i < k; i++) {
            result.add(knnList.get(i).getKey());
        }

        return result;
    }

    // load dataset AG
    public static SKNNV2.AG[] loadDatasetAG(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        List<List<SKNNV2.Point>> dataSetAG = new ArrayList<>();
        List<List<BigInteger>> subLabelList = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] lineSplit = line.split(";");

            List<SKNNV2.Point> points = new ArrayList<>();
            List<BigInteger> groupids = new ArrayList<>();
            for (int i = 0; i < lineSplit.length - 1; i++) {
                String[] pointSplit = lineSplit[i].split(",");
                BigInteger x = new BigInteger(pointSplit[0]);
                BigInteger y = new BigInteger(pointSplit[1]);
                BigInteger id = new BigInteger(pointSplit[2]);
                BigInteger groupid = new BigInteger(pointSplit[3]);
                points.add(new SKNNV2.Point(id, new BigInteger[] { x, y }));
                groupids.add(groupid);
            }
            dataSetAG.add(points);
            subLabelList.add(groupids);
        }

        int agNum = dataSetAG.size();
        int agSize = dataSetAG.get(0).size();
        SKNNV2.AG[] ags = new SKNNV2.AG[agNum];

        for (int i = 0; i < agNum; i++) {
            SKNNV2.Point[] points = new SKNNV2.Point[agSize];
            for (int j = 0; j < agSize; j++) {
                points[j] = dataSetAG.get(i).get(j);
            }

            BigInteger[] subLabels = new BigInteger[agSize];
            for (int j = 0; j < agSize; j++) {
                subLabels[j] = subLabelList.get(i).get(j);
            }

            ags[i] = new SKNNV2.AG(BigInteger.valueOf(i), points, subLabels);
        }

        reader.close();
        return ags;
    }

    // load dataset VG
    public static SKNNV2.VG[] loadDatasetVG(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        List<List<BigInteger>> mbrList = new ArrayList<>();
        List<List<SKNNV2.Point>> dataSetVG = new ArrayList<>();
        List<List<BigInteger>> subLabelList = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] lineSplit = line.split(":");

            String[] lineSplit0 = lineSplit[0].split(",");
            List<BigInteger> mbr = new ArrayList<>();
            for (int i = 0; i < lineSplit0.length; i++){
                mbr.add(new BigInteger(lineSplit0[i]));
            }
            mbrList.add(mbr);

            String[] lineSplit1 = lineSplit[1].split(";");

            List<SKNNV2.Point> points = new ArrayList<>();
            List<BigInteger> groupids = new ArrayList<>();
            for (int i = 0; i < lineSplit1.length - 1; i++) {
                String[] pointSplit = lineSplit1[i].split(",");
                BigInteger x = new BigInteger(pointSplit[0]);
                BigInteger y = new BigInteger(pointSplit[1]);
                BigInteger id = new BigInteger(pointSplit[2]);
                BigInteger groupid = new BigInteger(pointSplit[3]);
                points.add(new SKNNV2.Point(id, new BigInteger[] { x, y }));
                groupids.add(groupid);
            }
            dataSetVG.add(points);
            subLabelList.add(groupids);
        }

        int vgNum = dataSetVG.size();
        int vgSize = dataSetVG.get(0).size();
        SKNNV2.VG[] vgs = new SKNNV2.VG[vgNum];

        for (int i = 0; i < vgNum; i++) {
            BigInteger[] lowData = new BigInteger[2];
            for (int j = 0; j < 2; j++) {
                lowData[j] = mbrList.get(i).get(j);
            }
            BigInteger[] highData = new BigInteger[2];
            for (int j = 2; j < 4; j++) {
                highData[j - 2] = mbrList.get(i).get(j);
            }
            SKNNV2.Point low = new SKNNV2.Point(2);
            low.data = lowData;
            SKNNV2.Point high = new SKNNV2.Point(2);
            high.data = highData;

            SKNNV2.Point[] points = new SKNNV2.Point[vgSize];
            for (int j = 0; j < vgSize; j++) {
                points[j] = dataSetVG.get(i).get(j);
            }

            BigInteger[] subLabels = new BigInteger[vgSize];
            for (int j = 0; j < vgSize; j++) {
                subLabels[j] = subLabelList.get(i).get(j);
            }

            vgs[i] = new SKNNV2.VG(low, high, points, subLabels);
        }

        reader.close();
        return vgs;
    }

    public static BigInteger[][] loadGowallaDataset(String filePath, int number) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));

        List<BigInteger[]> universalDataSet = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] lineSplit = line.split("\t");

            int latitude = (int) (100000 * (Float.parseFloat(lineSplit[2]) + 90));
            int longitude = (int) (100000 * (Float.parseFloat(lineSplit[3]) + 180));
            universalDataSet.add(new BigInteger[] { BigInteger.valueOf(latitude), BigInteger.valueOf(longitude),
                    new BigInteger(lineSplit[4]) });
        }

        Collections.shuffle(universalDataSet);

        List<BigInteger[]> dataset = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            dataset.add(universalDataSet.get(i));
        }

        reader.close();
        return dataset.toArray(new BigInteger[0][]);
    }

    public static void randomSelectGowallaDataset(String datasetPath, String savePath, int number) {
        File datasetFile = new File(datasetPath);
        File saveFile = new File(savePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(datasetFile));
                PrintWriter writer = new PrintWriter(saveFile);) {

            List<String> list = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineSplit = line.split("\t");

                float latitude = Float.parseFloat(lineSplit[2]);
                float longitude = Float.parseFloat(lineSplit[3]);

                list.add(latitude + "," + longitude);
            }

            Collections.shuffle(list);

            for (int i = 0; i < number; i++) {
                writer.println(list.get(i));
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // int dimension = 2;
        // int number = 20;
        // int bitLength = 20;
        // BigInteger mod = BigInteger.probablePrime(bitLength, new Random());

        // // BigInteger[][] dataset = generateDataset(dimension, number, bitLength);
        // BigInteger[][] dataset = loadGowallaDataset(
        //         "Gowalla\\loc-gowalla_totalCheckins\\Gowalla_totalCheckins.txt", number);
        // System.out.println(Arrays.deepToString(dataset));
        // System.out.println(dataset.length);

        // RTree tree = new RTree(2, 5, 0.4f, Constants.RTREE_QUADRATIC, dimension);
        // for (int i = 0; i < number; i++) {
        //     BigInteger id = dataset[i][dimension];

        //     BigInteger[] pData = new BigInteger[dimension];
        //     for (int j = 0; j < dimension; j++) {
        //         pData[j] = dataset[i][j];
        //     }
        //     Point p = new Point(pData);

        //     final Rectangle rectangle = new Rectangle(p, p, id);
        //     tree.insert(rectangle);
        // }

        // List<Node> arrayT = constructArrayT(tree);
        // System.out.println(arrayT.size());
        // System.out.println("array T:" + arrayT);

        // List<List<Node>> arrayTSecrets = shareArrayT(arrayT, mod);
        // System.out.println(arrayTSecrets.get(0));
        // System.out.println(arrayTSecrets.get(1));
        // System.out.println("Done!");

        // System.out.println(arrayTSecrets.get(0));
        // String s = JSONObject.toJSONString(arrayTSecrets.get(0));
        // System.out.println(s);
        // List<Node> list = JSONObject.parseArray(s, Node.class);
        // System.out.println(list);

//        int number = 25000;
//        String datasetPath = "D:\\Gowalla\\loc-gowalla_totalCheckins\\Gowalla_totalCheckins.txt";
//        String savePath = "C:\\Users\\Admin\\Desktop\\test\\sknn\\Gowalla_Random_Select_" + number + ".txt";
//        randomSelectGowallaDataset(datasetPath, savePath, number);
    }

    //    public static void main(String[] args) {
    //        //
    // //        RTree tree = new RTree(4, 0.4f, Constants.RTREE_QUADRATIC, 2);
    //        RTree tree = new RTree(2, 0.5f, Constants.RTREE_QUADRATIC, 2);
    //
    //        int[] f = {
    //                2, 3, 2, 3,
    //                5, 1, 5, 1,
    //                8, 2, 8, 2,
    //                10, 4, 10, 4,
    //                1, 8, 1, 8,
    //                4, 5, 4, 5,
    //                6, 5, 6, 5,
    //                9, 7, 9, 7,
    //        };

    //        BigInteger mod = BigInteger.valueOf(5);

    //        int[] labelList = new int[f.length / 4];
    //        for (int i = 0; i < labelList.length; i++) {
    //            labelList[i] = i;
    //        }

    //        for (int i = 0; i < f.length; ) {
    //            BigInteger id = BigInteger.valueOf(labelList[i / 4]);
    //            Point p1 = new Point(new BigInteger[]{BigInteger.valueOf(f[i++]), BigInteger.valueOf(f[i++])});
    //            Point p2 = new Point(new BigInteger[]{BigInteger.valueOf(f[i++]), BigInteger.valueOf(f[i++])});
    //            final Rectangle rectangle = new Rectangle(p1, p2, id);

    //            tree.insert(rectangle);
    //        }

    //        System.out.println("---------------------------------");
    //        System.out.println("Now the tree is :");
    //        List<RTNode> levelTraversalList = tree.traverseLevelOrder();
    //        System.out.println("：" + levelTraversalList.size());
    //        int prevLevel = levelTraversalList.get(0).getLevel();
    //        for (RTNode node : levelTraversalList) {
    //            if (node.getLevel() != prevLevel) {
    //                prevLevel = node.getLevel();
    //                System.out.println();
    //            }

    //            System.out.print("{");
    //            for (Rectangle rectangle : node.getData()) {
    //                System.out.print(rectangle + ",");
    //            }
    //            System.out.print("}\t");
    //        }
    //        System.out.println();

    //        List<Node> arrayT = constructArrayT(tree);
    //        System.out.println(arrayT.size());
    //        System.out.println(arrayT);

    //        List<List<Node>> arrayTSecrets = shareArrayT(arrayT, mod);
    //        System.out.println(arrayTSecrets.get(0));
    //        System.out.println(arrayTSecrets.get(1));
    //    }
}
