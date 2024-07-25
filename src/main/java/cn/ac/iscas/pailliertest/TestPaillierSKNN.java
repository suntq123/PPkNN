package cn.ac.iscas.pailliertest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import cn.ac.iscas.kdtree.KDTreeNode;
import cn.ac.iscas.kdtree.KDTreePoint;
import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.paillier.Paillier.PaillierPublicKey;
import cn.ac.iscas.utils.DataProcessor;
import cn.ac.iscas.utils.Util;

public class TestPaillierSKNN {

    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataNumber dataLength bSize m k
    * 
    * @param args
    * @throws IOException
    */
    public static void user(String[] args) throws IOException {
        /* 提取测试数据 */
        int index = 1;
        String ipC1 = args[index++];
        int portC1 = Integer.parseInt(args[index++]);
        String ipC2 = args[index++];
        int portC2 = Integer.parseInt(args[index++]);

        Random random;
        if (args[index] == "null") {
            random = new Random();
        } else {
            random = new Random(Long.parseLong(args[index++]));
        }

        int testNumber = Integer.parseInt(args[index++]);
        int bitLength = Integer.parseInt(args[index++]);
        int dataNumber = Integer.parseInt(args[index++]);
        int dataLength = Integer.parseInt(args[index++]);

        int bSize = Integer.parseInt(args[index++]); // 桶的大小
        int m = Integer.parseInt(args[index++]); // 维度
        int k = Integer.parseInt(args[index++]); // 检索数量

        Socket socketC1 = new Socket(ipC1, portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Socket socketC2 = new Socket(ipC2, portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /* 计算过程 */
        Util.writeInt(testNumber, writerC1);
        Util.writeInt(testNumber, writerC2);

        Paillier paillier = new Paillier(bitLength);
        PaillierPublicKey publicKey = paillier.getPaillierPublicKey();
        PaillierPrivateKey privateKey = paillier.getPaillierPrivateKey();

        String publicKeyJson = Paillier.parsePublicKeyToJson(publicKey);
        writerC1.println(publicKeyJson);
        writerC1.flush();

        String privateKeyJson = Paillier.parsePrivateKeyToJson(privateKey);
        writerC2.println(privateKeyJson);
        writerC2.flush();

        Util.writeInt(bSize, writerC1);
        Util.writeInt(bSize, writerC2);

        Util.writeInt(m, writerC1);
        Util.writeInt(m, writerC2);

        Util.writeInt(dataLength, writerC1);
        Util.writeInt(dataLength, writerC2);

        Util.writeInt(k, writerC1);
        Util.writeInt(k, writerC2);

        BigInteger[][] dataset = DataProcessor.generateDataset(m, dataNumber, dataLength, random);

        KDTreePoint[] points = new KDTreePoint[dataNumber];
        for (int i = 0; i < dataNumber; i++) {
            BigInteger[] pData = new BigInteger[m];
            System.arraycopy(dataset[i], 0, pData, 0, m);
            points[i] = new KDTreePoint(pData);
        }

        BigInteger[] lbData = new BigInteger[m];
        BigInteger[] ubData = new BigInteger[m];
        for (int i = 0; i < m; i++) {
            lbData[i] = BigInteger.valueOf(0);
            ubData[i] = BigInteger.TWO.pow(dataLength).subtract(BigInteger.ONE);
        }

        KDTreeNode kdTree = new KDTreeNode(bSize, m, points, null, lbData, ubData); // 请保证最后叶子节点的数据点数量一致
        List<KDTreeNode> leafNodes = KDTreeNode.getAllLeafPoints(kdTree);

        int bNum = leafNodes.size();
        BigInteger[][][] buckets = new BigInteger[bNum][bSize][m];
        BigInteger[][] lbs = new BigInteger[bNum][m];
        BigInteger[][] ubs = new BigInteger[bNum][m];
        for (int i = 0; i < bNum; i++) {
            KDTreeNode node = leafNodes.get(i);

            lbs[i] = node.lb.data;
            ubs[i] = node.ub.data;

            KDTreePoint[] bucketPoints = node.points;
            for (int j = 0; j < bSize; j++) {
                buckets[i][j] = bucketPoints[j].data;
            }
        }

        BigInteger[][][] ebuckets = new BigInteger[bNum][bSize][m];
        BigInteger[][] elbs = new BigInteger[bNum][m];
        BigInteger[][] eubs = new BigInteger[bNum][m];
        for (int i = 0; i < bNum; i++) {
            for (int j = 0; j < m; j++) {
                elbs[i][j] = Paillier.encrypt(privateKey, lbs[i][j]);
                eubs[i][j] = Paillier.encrypt(privateKey, ubs[i][j]);
            }

            for (int j = 0; j < bSize; j++) {
                for (int l = 0; l < m; l++) {
                    ebuckets[i][j][l] = Paillier.encrypt(privateKey, buckets[i][j][l]);
                }
            }
        }

        Util.writeInt(bNum, writerC1);
        Util.writeInt(bNum, writerC2);

        Util.writeBigIntegers(ebuckets, writerC1);
        Util.writeBigIntegers(elbs, writerC1);
        Util.writeBigIntegers(eubs, writerC1);
        for (int testCount = 0; testCount < testNumber; testCount++) {
            System.out.print(testCount + " ");

            BigInteger[] q = new BigInteger[m];
            BigInteger[] eq = new BigInteger[m];
            for (int i = 0; i < m; i++) {
                q[i] = new BigInteger(dataLength, random);
                eq[i] = Paillier.encrypt(privateKey, q[i]);
            }

            Util.writeBigIntegers(eq, writerC1);
            BigInteger[][] eKPoints = Util.readBigIntegers(k, m, readerC1);

            BigInteger[] deKDistances = new BigInteger[k];
            for (int i = 0; i < k; i++) {
                BigInteger[] p = new BigInteger[m];
                for (int j = 0; j < m; j++) {
                    p[j] = Paillier.decrypt(privateKey, eKPoints[i][j]);
                }
                deKDistances[i] = Util.squareEuclideanDistance(q, p);
            }

            BigInteger[] tKDistances = getKNearest(dataset, q, m, k);
            for (int i = 0; i < k; i++) {
                if (!deKDistances[i].equals(tKDistances[i])) {
                    System.out.println("Error!!!!");
                    System.out.println("i = " + i);
                    System.out.println("tKDistances[i] = " + tKDistances[i]);
                    System.out.println("deKDistances[i] = " + deKDistances[i]);
                }
            }

            // BigInteger[][] deKPoints = new BigInteger[k][m];
            // for (int i = 0; i < k; i++) {
            //     for (int j = 0; j < m; j++) {
            //         deKPoints[i][j] = Paillier.decrypt(privateKey, eKPoints[i][j]);
            //     }
            // }
            // BigInteger[] tKPoints = getKNearest(dataset, q, m, k);
            // for (int i = 0; i < k; i++) {
            //     for (int j = 0; j < m; j++) {
            //         if(!tKPoints[i][j].equals(deKPoints[i][j])){
            //             System.out.println("Error!!!!!!!!!!!");
            //             System.out.println("i = " + i);
            //             System.out.println("j = " + j);
            //             System.out.println("tKPoints[i][j] = " + tKPoints[i][j]);
            //             System.out.println("deKPoints[i][j] = " + deKPoints[i][j]);

            //             // return;
            //         }
            //     }
            // }
        }
        long timeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("C1: " + timeC1 + " ms");
        System.out.println("C2: " + timeC2 + " ms");

        socketC1.close();
        socketC2.close();
    }

    public static BigInteger[] getKNearest(BigInteger[][] dataset, BigInteger[] q, int m, int k) {
        BigInteger[] result = new BigInteger[k];

        // <point, distance>
        List<SimpleEntry<BigInteger[], BigInteger>> list = new ArrayList<>(dataset.length);

        for (int i = 0; i < dataset.length; i++) {
            BigInteger[] pData = new BigInteger[m];
            System.arraycopy(dataset[i], 0, pData, 0, m);

            BigInteger d = Util.squareEuclideanDistance(pData, q);

            list.add(new SimpleEntry<BigInteger[], BigInteger>(pData, d));
        }

        // Arrays.sort(list, (e1, e2) -> {
        //     return e1.getValue().compareTo(e2.getValue());
        // });

        Collections.sort(list, (e1, e2) -> {
            return e1.getValue().compareTo(e2.getValue());
        });

        for (int i = 0; i < k; i++) {
            // System.arraycopy(list[i].getKey(), 0, result[i], 0, m);
            result[i] = list.get(i).getValue();
        }

        return result;
    }

    /**
    * args: role portC1
    * 
    * @param args
    * @throws IOException
    */
    public static void c1(String[] args) throws IOException {
        /* 提取测试数据 */
        int index = 1;
        int portC1 = Integer.parseInt(args[index++]);

        ServerSocket serverSocket = new ServerSocket(portC1);

        Socket socketUser = serverSocket.accept();
        PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
        BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

        Socket socketC2 = serverSocket.accept();
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /* 计算过程 */
        int testNumber = Util.readInt(readerUser);
        PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());
        Paillier.a = new BigInteger(publicKey.n.bitLength() / 2 + 1, new SecureRandom());

        int bSize = Util.readInt(readerUser);
        int m = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);
        int k = Util.readInt(readerUser);

        int bNum = Util.readInt(readerUser);

        BigInteger[][][] eBuckets = Util.readBigIntegers(bNum, bSize, m, readerUser);
        BigInteger[][] elbs = Util.readBigIntegers(bNum, m, readerUser);
        BigInteger[][] eubs = Util.readBigIntegers(bNum, m, readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger[] eq = Util.readBigIntegers(m, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.currentTimeMillis();
            BigInteger[][] ePoints = Paillier.secureKNNC1(k, eBuckets, elbs, eubs, eq, dataLength, publicKey, readerC2,
                    writerC2);
            timeSum += System.currentTimeMillis() - timePre;

            Util.writeBigIntegers(ePoints, writerUser);
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC2.close();
        socketUser.close();
        serverSocket.close();
    }

    /**
    * args: role ipC1 portC1 portC2
    * 
    * @param args
    * @throws IOException
    */
    public static void c2(String[] args) throws IOException {
        /* 提取测试数据 */
        int index = 1;
        String ipC1 = args[index++];
        int portC1 = Integer.parseInt(args[index++]);
        int portC2 = Integer.parseInt(args[index++]);

        ServerSocket serverSocket = new ServerSocket(portC2);

        Socket socketUser = serverSocket.accept();
        PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
        BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

        Socket socketC1 = new Socket(ipC1, portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        /* 计算过程 */
        int testNumber = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());
        Paillier.a = new BigInteger(privateKey.publicKey.n.bitLength() / 2 + 1, new SecureRandom());

        int bSize = Util.readInt(readerUser);
        int m = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);
        int k = Util.readInt(readerUser);

        int bNum = Util.readInt(readerUser);
        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.currentTimeMillis();
            Paillier.secureKNNC2(k, bNum, bSize, m, dataLength, privateKey, readerC1, writerC1);
            timeSum += System.currentTimeMillis() - timePre;
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {

        String c1 = "c1 8001";
        String c2 = "c2 127.0.0.1 8001 8002";
        // role ipC1 portC1 ipC2 portC2 randSeed testNumber bitLength dataNumber dataLength bucketSize m k
        // String user = "user 127.0.0.1 8001 127.0.0.1 8002 null 1 1024 12800 10 50 2 5";
        String user = "user 127.0.0.1 8001 127.0.0.1 8002 0 10 1024 10 10 5 2 3";

        args = c1.split(" ");
        args = c2.split(" ");
        args = user.split(" ");

        if (args[0].equals("user"))
            user(args);
        else if (args[0].equals("c1"))
            c1(args);
        else if (args[0].equals("c2"))
            c2(args);
    }
}
