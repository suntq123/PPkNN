package cn.ac.iscas;

import com.alibaba.fastjson.JSONObject;

import cn.ac.iscas.kdtree.KDTreeNode;
import cn.ac.iscas.kdtree.KDTreePoint;
import cn.ac.iscas.rtree.Constants;
import cn.ac.iscas.rtree.Point;
import cn.ac.iscas.rtree.RTree;
import cn.ac.iscas.rtree.Rectangle;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.ComparisonTuple;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.MultiplicationTriple;
import cn.ac.iscas.utils.DataProcessor;
import cn.ac.iscas.utils.Util;

import static cn.ac.iscas.utils.DataProcessor.*;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 */
public class SecureKNNDataUser {

    public static void main(String[] args) throws IOException {

        int portC1 = 8004;
        int portC2 = 8005;

        int testType = 2; // 0-SKNN, 1-Linear SKNN, 2-Bucket SKNN
        if (testType == 0)
            System.out.println("Test Type: SKNN");
        else if (testType == 1)
            System.out.println("Test Type: Linear SKNN");
        else if (testType == 2)
            System.out.println("Test Type: Bucket SKNN");

        int pow = 2;
        int testNumber = 10;
        int number = 1000;
        int bitLength = 20;
        int dirNodeCapacity = 50;
        int dataNodeCapacity = 50;
        int dimension = 2;
        float fillFactor = 0.5f;

        int k = 5;

        Random random = new Random(0);
        int s; // s
        int overflow = (int) Math.ceil(Util.log2(dimension));
        if (pow <= 0) {
            // mod = BigInteger.probablePrime(bitLength + 2, new Random());
            s = bitLength + overflow;
        } else {
            // mod = BigInteger.probablePrime(bitLength * pow + 2, new Random());
            s = bitLength * pow + overflow;
        }
        int l = s + 3; //  s < l - 2
        BigInteger mod = BigInteger.TWO.pow(l);
        System.out.println("mod = " + mod);

        MultiplicationTriple[] tripleSecrets = AdditiveSecretSharing.generateMultiplicationTriples(mod);

        ComparisonTuple[] cTuples = AdditiveSecretSharing.generateComparsionTuple(s, mod.longValue());

        BigInteger[][] dataset = generateDataset(dimension, number, bitLength, random);
        // BigInteger[][] dataset = loadGowallaDataset("D:\\Gowalla\\loc-gowalla_totalCheckins\\Gowalla_totalCheckins.txt", number);
        RTree tree = new RTree(dirNodeCapacity, dataNodeCapacity, fillFactor, Constants.RTREE_QUADRATIC, dimension);
        List<DataProcessor.Entry> entries = new ArrayList<>();
        KDTreePoint[] points = new KDTreePoint[number];

        for (int i = 0; i < dataset.length; i++) {
            BigInteger id = dataset[i][dimension];

            BigInteger[] pData = new BigInteger[dimension];
            System.arraycopy(dataset[i], 0, pData, 0, dimension);
            Point p = new Point(pData);

            if (testType == 0) {
                final Rectangle rectangle = new Rectangle(p, p, id);
                tree.insert(rectangle);

                // if ((i + 1) % 10000 == 0) {
                //     List<DataProcessor.Node> arrayT = constructArrayT(tree);
                //     List<List<DataProcessor.Node>> arrayTSecrets = shareArrayT(arrayT, mod);

                //     String filePath = "C:\\Users\\Admin\\Desktop\\test\\sknn\\array T\\virtual dataset\\" + (i + 1);
                //     saveNodeArray(filePath + "_C1.txt", arrayTSecrets.get(0));
                //     saveNodeArray(filePath + "_C2.txt", arrayTSecrets.get(1));
                // }
            }

            entries.add(new Entry(p, id));

            points[i] = new KDTreePoint(id, pData);
        }

        BigInteger[] lbData = new BigInteger[dimension];
        BigInteger[] ubData = new BigInteger[dimension];
        for (int i = 0; i < dimension; i++) {
            lbData[i] = BigInteger.valueOf(0);
            ubData[i] = BigInteger.TWO.pow(bitLength).subtract(BigInteger.ONE);
        }
        KDTreeNode kdTree = new KDTreeNode(dataNodeCapacity, dimension, points, null, lbData, ubData); 
        List<KDTreeNode> leafNodes = KDTreeNode.getAllLeafPoints(kdTree);
        System.out.println("bucket number: " + leafNodes.size());

        Socket socketC1 = new Socket("127.0.0.1", portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Util.writeInt(testType, writerC1);
        Util.writeBigInteger(mod, writerC1);
        writerC1.println(AdditiveSecretSharing.parseMultiplicationTripleToJson(tripleSecrets[0]));
        writerC1.flush();
        Util.writeInt(testNumber, writerC1);
        Util.writeInt(pow, writerC1);

        Socket socketC2 = new Socket("127.0.0.1", portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        Util.writeInt(testType, writerC2);
        Util.writeBigInteger(mod, writerC2);
        writerC2.println(AdditiveSecretSharing.parseMultiplicationTripleToJson(tripleSecrets[1]));
        writerC2.flush();
        Util.writeInt(testNumber, writerC2);
        Util.writeInt(pow, writerC2);

        writerC1.println(AdditiveSecretSharing.parseComparisionTupleToJson(cTuples[0]));
        writerC1.flush();

        writerC2.println(AdditiveSecretSharing.parseComparisionTupleToJson(cTuples[1]));
        writerC2.flush();

        List<DataProcessor.Node> arrayT = constructArrayT(tree);
        List<List<DataProcessor.Node>> arrayTSecrets = shareArrayT(arrayT, mod);
        List<DataProcessor.Node> buckets = generateBuckets(leafNodes);
        List<List<DataProcessor.Node>> bucketsSecrets = shareArrayT(buckets, mod);

        sendNodeArray(arrayTSecrets.get(0), writerC1);
        sendNodeArray(arrayTSecrets.get(1), writerC2);

        List<List<DataProcessor.Entry>> entriesSecrets = shareEntries(entries, mod);
        sendEntries(entriesSecrets.get(0), writerC1);
        sendEntries(entriesSecrets.get(1), writerC2);

        sendNodeArray(bucketsSecrets.get(0), writerC1);
        sendNodeArray(bucketsSecrets.get(1), writerC2);

        for (int j = 0; j < testNumber; j++) {
            System.out.print(j + " ");

            BigInteger[] pointData = new BigInteger[dimension];
            for (int i = 0; i < dimension; i++) {
                pointData[i] = new BigInteger(bitLength, random);
            }
            Point point = new Point(pointData);
            Point[] pointSecrets = DataProcessor.sharePoint(point, mod);
            sendQueryCondition(k, pointSecrets[0], writerC1);
            sendQueryCondition(k, pointSecrets[1], writerC2);

            List<BigInteger[]> r1 = JSONObject.parseArray(readerC1.readLine(), BigInteger[].class);
            List<BigInteger[]> r2 = JSONObject.parseArray(readerC2.readLine(), BigInteger[].class);
            Set<BigInteger> r = new HashSet<>();
            for (int i = 0; i < r1.size(); i++) {
                r.add(AdditiveSecretSharing.add(r1.get(i)[0], r2.get(i)[0], mod));
            }
            // System.out.println("Data user get: " + r);

            Set<BigInteger> validResult = getKNearest(dataset, point.getData(), dimension, pow, k);

            for (BigInteger id : validResult) {
                if (!r.contains(id)) {
                    System.out.println("Result is wrong!");
                    System.out.println("SKNN: " + r);
                    System.out.println("KNN" + validResult);
                    // return;
                }
            }
        }
        System.out.println();
        long timeC1 = Util.readLong(readerC1);
        long communicationTimeC1 = Util.readLong(readerC1);
        long computingTimeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("Time C1: " + timeC1 + " ms");
        System.out.println("Communication Time C1: " + communicationTimeC1 + " ms");
        System.out.println("Computing Time C1: " + computingTimeC1 + " ms");
        System.out.println("Time C2: " + timeC2 + " ms");

        readerC1.close();
        readerC2.close();
        socketC1.close();
        socketC2.close();
    }
}
