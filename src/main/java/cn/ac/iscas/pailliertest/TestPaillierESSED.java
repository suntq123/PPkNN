package cn.ac.iscas.pailliertest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.paillier.Paillier.PaillierPublicKey;
import cn.ac.iscas.utils.Util;

public class TestPaillierESSED {

    /**
     * args: role ipC1 portC1 ipC2 portC2 bitLength dataLength testNumber m
     * 
     * user 127.0.0.1 8001 127.0.0.1 8002 10 10
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

        int bitLength = Integer.parseInt(args[index++]);
        int dataLength = Integer.parseInt(args[index++]);
        int testNumber = Integer.parseInt(args[index++]);
        int m = Integer.parseInt(args[index++]);

        int sigma = dataLength + 40; // 数值位数+安全参数，安全参数可以取40

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
        // System.out.println("public key: " + publicKeyJson);
        writerC1.println(publicKeyJson);
        writerC1.flush();

        String privateKeyJson = Paillier.parsePrivateKeyToJson(privateKey);
        // System.out.println("private key: " + privateKeyJson);
        writerC2.println(privateKeyJson);
        writerC2.flush();

        Util.writeInt(m, writerC1);
        Util.writeInt(m, writerC2);

        Util.writeInt(sigma, writerC1);
        Util.writeInt(sigma, writerC2);

        for (int i = 0; i < testNumber; i++) {
            System.out.print(i + " ");

            BigInteger[] x = generateVector(m, dataLength);
            BigInteger[] y = generateVector(m, dataLength);

            BigInteger[] ex = new BigInteger[m];
            BigInteger[] ey = new BigInteger[m];
            for (int j = 0; j < m; j++) {
                ex[j] = Paillier.encrypt(publicKey, x[j]);
                ey[j] = Paillier.encrypt(publicKey, y[j]);
            }

            Util.writeBigIntegers(ex, writerC1);
            Util.writeBigIntegers(ey, writerC1);

            BigInteger eDistance = Util.readBigInteger(readerC1);
            BigInteger distance = Util.squareEuclideanDistance(x, y);

            BigInteger dDistance = Paillier.decrypt(privateKey, eDistance);
            if (!dDistance.equals(distance)) {
                System.out.println("Error!");
                System.out.println(i);
                System.out.println("D(E(distance)) = " + dDistance);
                System.out.println("true distance = " + distance);

                break;
            }
        }
        System.out.println();
        long timeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("C1: " + timeC1 + " ns");
        System.out.println("C2: " + timeC2 + " ns");

        socketC1.close();
        socketC2.close();
    }

    public static BigInteger[] generateVector(int m, int dataLength) {
        BigInteger[] v = new BigInteger[m];

        Random random = new Random();
        for (int i = 0; i < m; i++) {
            v[i] = new BigInteger(dataLength, random);
        }

        return v;
    }

    /**
    * args: role portC1
    * c1 8001 
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

        int m = Util.readInt(readerUser);
        int sigma = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger[] ex = Util.readBigIntegers(m, readerUser);
            BigInteger[] ey = Util.readBigIntegers(m, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.nanoTime();
            BigInteger eDistance = Paillier.enhancedSecureSquaredEuclideanDistanceC1(ex, ey, sigma, publicKey, readerC2,
                    writerC2);
            timeSum += System.nanoTime() - timePre;

            Util.writeBigInteger(eDistance, writerUser);
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
    * c2 127.0.0.1 8001 8002
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

        int m = Util.readInt(readerUser);
        int sigma = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.nanoTime();
            Paillier.enhancedSecureSquaredEuclideanDistanceC2(privateKey, m, sigma, readerC1, writerC1);
            timeSum += System.nanoTime() - timePre;
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        // System.out.println(args[0]);

        // // sigma = dataLength + secureParameter， 本实验中secureParameter = 40
        // // 要求 sigma * m < bitLength
        // // 即 (40 + dataLength) * m < bitLength

        // String c1 = "c1 8001";
        // String c2 = "c2 127.0.0.1 8001 8002";
        // String user = "user 127.0.0.1 8001 127.0.0.1 8002 2048 20 10 2"; // role ipC1 portC1 ipC2 portC2 bitLength dataLength testNumber m

        // String c1 = "c1 8001";
        String c2 = "c2 192.168.1.5 8001 8002";
        String user = "user 192.168.1.5 8001 127.0.0.1 8002 2048 20 10 2"; // role ipC1 portC1 ipC2 portC2 bitLength dataLength testNumber m

        // args = c1.split(" ");
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
