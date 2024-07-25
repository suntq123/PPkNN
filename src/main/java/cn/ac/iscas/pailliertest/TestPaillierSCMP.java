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

public class TestPaillierSCMP {

    /**
    * args: role ipC1 portC1 ipC2 portC2 testType testNumber bitLength dataLength
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

        int testType = Integer.parseInt(args[index++]);
        int testNumber = Integer.parseInt(args[index++]);
        int bitLength = Integer.parseInt(args[index++]);
        int dataLength = Integer.parseInt(args[index++]);

        Socket socketC1 = new Socket(ipC1, portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Socket socketC2 = new Socket(ipC2, portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /* 计算过程 */
        Util.writeInt(testType, writerC1);
        Util.writeInt(testType, writerC2);
        Util.writeInt(testNumber, writerC1);
        Util.writeInt(testNumber, writerC2);
        Util.writeInt(dataLength, writerC1);
        Util.writeInt(dataLength, writerC2);

        Paillier paillier = new Paillier(bitLength);
        PaillierPublicKey publicKey = paillier.getPaillierPublicKey();
        PaillierPrivateKey privateKey = paillier.getPaillierPrivateKey();

        String publicKeyJson = Paillier.parsePublicKeyToJson(publicKey);
        writerC1.println(publicKeyJson);
        writerC1.flush();

        String privateKeyJson = Paillier.parsePrivateKeyToJson(privateKey);
        writerC2.println(privateKeyJson);
        writerC2.flush();

        Random random = new Random();
        for (int i = 0; i < testNumber; i++) {
            System.out.print(i + " ");
            BigInteger u = new BigInteger(dataLength, random);
            BigInteger v = new BigInteger(dataLength, random);

            if (testType == 0) {
                BigInteger[] eu = Paillier.binaryBitEncrypt(u, dataLength, publicKey);
                BigInteger[] ev = Paillier.binaryBitEncrypt(v, dataLength, publicKey);

                Util.writeBigIntegers(eu, writerC1);
                Util.writeBigIntegers(ev, writerC1);
            } else if (testType == 1) {
                BigInteger eu = Paillier.encrypt(privateKey, u);
                BigInteger ev = Paillier.encrypt(privateKey, v);

                Util.writeBigInteger(eu, writerC1);
                Util.writeBigInteger(ev, writerC1);
            }

            BigInteger eAlpha = Util.readBigInteger(readerC1);
            int alpha = Paillier.decrypt(privateKey, eAlpha).intValue();
            int c = u.compareTo(v);
            if ((c <= 0 && alpha == 0) || (c > 0 && alpha == 1)) {
                System.out.println("Error!!!!");
                System.out.println("u = " + u);
                System.out.println("v = " + v);
                System.out.println("alpha = " + alpha);
                break;
            }
        }
        long timeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("C1: " + timeC1 + " ns");
        System.out.println("C2: " + timeC2 + " ns");

        socketC1.close();
        socketC2.close();
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
        int testType = Util.readInt(readerUser);
        int testNumber = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);
        PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger eu = null, ev = null;

            BigInteger[] euBinary = null;
            BigInteger[] evBinary = null;
            if (testType == 0) {
                euBinary = Util.readBigIntegers(dataLength, readerUser);
                evBinary = Util.readBigIntegers(dataLength, readerUser);
            } else if (testType == 1) {
                eu = Util.readBigInteger(readerUser);
                ev = Util.readBigInteger(readerUser);
            }

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            BigInteger eAlpha = null;
            long timePre = System.nanoTime();
            if (testType == 0) {
                eAlpha = Paillier.secureCompareC1(euBinary, evBinary, publicKey, readerC2, writerC2);
            } else if (testType == 1) {
                BigInteger[] ets = new BigInteger[] { eu, ev };
                BigInteger[][] etsBinary = Paillier.secureMultiBitDecompositionC1(ets, dataLength, publicKey,
                        readerC2, writerC2);
                eAlpha = Paillier.secureCompareC1(etsBinary[0], etsBinary[1], publicKey, readerC2, writerC2);
            }
            timeSum += System.nanoTime() - timePre;

            Util.writeBigInteger(eAlpha, writerUser);
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
        int testType = Util.readInt(readerUser);
        int testNumber = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.nanoTime();
            if (testType == 0) {
                Paillier.secureCompareC2(dataLength, privateKey, readerC1, writerC1);
            } else if (testType == 1) {
                Paillier.secureMultiBitDecompositionC2(2, dataLength, privateKey, readerC1, writerC1);
                Paillier.secureCompareC2(dataLength, privateKey, readerC1, writerC1);
            }
            timeSum += System.nanoTime() - timePre;
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {

        // String c1 = "c1 8001";
        // String c2 = "c2 127.0.0.1 8001 8002";
        // //             role ipC1 portC1 ipC2 portC2 testType testNumber bitLength dataLength
        // String user = "user 127.0.0.1 8001 127.0.0.1 8002 0 10 1024 10";
        // // String user = "user 127.0.0.1 8001 127.0.0.1 8002 1 10 1024 10";

        // String c1 = "c1 8001";
        String c2 = "c2 192.168.1.5 8001 8002";
        //             role ipC1 portC1 ipC2 portC2 testType testNumber bitLength dataLength
        // String user = "user 192.168.1.5 8001 127.0.0.1 8002 0 10 2048 10";
        String user = "user 192.168.1.5 8001 127.0.0.1 8002 1 10 2048 20";

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
