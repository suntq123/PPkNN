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

public class TestPaillierSMIN {
    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength  num m
    * 
    * 
    * @param args
    * @throws IOException
    */
    public static void user(String[] args) throws IOException {
        /* */
        int index = 1;
        String ipC1 = args[index++];
        int portC1 = Integer.parseInt(args[index++]);
        String ipC2 = args[index++];
        int portC2 = Integer.parseInt(args[index++]);

        int testNumber = Integer.parseInt(args[index++]);
        int bitLength = Integer.parseInt(args[index++]);
        int dataLength = Integer.parseInt(args[index++]);
        int num = Integer.parseInt(args[index++]); // 点的数量
        int m = Integer.parseInt(args[index++]); // 点的维度

        try (Socket socketC1 = new Socket(ipC1, portC1); Socket socketC2 = new Socket(ipC2, portC2);) {
            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

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

            Util.writeInt(num, writerC1);
            Util.writeInt(num, writerC2);

            Util.writeInt(m, writerC1);
            Util.writeInt(m, writerC2);

            Util.writeInt(dataLength, writerC1);
            Util.writeInt(dataLength, writerC2);

            for (int i = 0; i < testNumber; i++) {
                System.out.print(i + " ");

                BigInteger[][] pList = new BigInteger[num][];
                BigInteger[][] epList = new BigInteger[num][m];
                for (int j = 0; j < num; j++) {
                    pList[j] = generateVector(m, dataLength);

                    for (int k = 0; k < m; k++) {
                        epList[j][k] = Paillier.encrypt(publicKey, pList[j][k]);
                    }
                }

                BigInteger[] dList = generateVector(num, dataLength * 2);
                BigInteger[] edList = new BigInteger[num];
                for (int j = 0; j < num; j++) {
                    edList[j] = Paillier.encrypt(publicKey, dList[j]);
                }

                Util.writeBigIntegers(epList, writerC1);
                Util.writeBigIntegers(edList, writerC1);

                BigInteger[] epMin = Util.readBigIntegers(m, readerC1);

                int minIndex = 0;
                for (int j = 1; j < num; j++) {
                    if (dList[minIndex].compareTo(dList[j]) > 0)
                        minIndex = j;
                }
                for (int j = 0; j < m; j++) {
                    BigInteger pj = Paillier.decrypt(privateKey, epMin[j]);
                    if (!pj.equals(pList[minIndex][j])) {
                        System.out.println("j = " + j);
                        System.out.println("Error!");
                        System.out.println("true min pj = " + pList[minIndex][j]);
                        System.out.println("D(E(pj)) = " + pj);

                        return;
                    }
                }
            }
            long timeC1 = Util.readLong(readerC1);
            long timeC2 = Util.readLong(readerC2);
            System.out.println("C1: " + timeC1 + " ms");
            System.out.println("C2: " + timeC2 + " ms");
        }
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
        /* */
        int index = 1;
        int portC1 = Integer.parseInt(args[index++]);

        ServerSocket serverSocket = new ServerSocket(portC1);

        Socket socketUser = serverSocket.accept();
        PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
        BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

        Socket socketC2 = serverSocket.accept();
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /*  */
        int testNumber = Util.readInt(readerUser);
        PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());

        int num = Util.readInt(readerUser);
        int m = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger[][] epList = Util.readBigIntegers(num, m, readerUser);
            BigInteger[] edList = Util.readBigIntegers(num, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.currentTimeMillis();
            BigInteger[] epMin = Paillier.secureMinDistancePointC1(epList, edList, dataLength, publicKey, readerC2,
                    writerC2);
            timeSum += System.currentTimeMillis() - timePre;

            Util.writeBigIntegers(epMin, writerUser);
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
        /*  */
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

        /*  */
        int testNumber = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());

        int num = Util.readInt(readerUser);
        int m = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            // System.out.println(i);
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.currentTimeMillis();
            Paillier.secureMinDistancePointC2(num, m, dataLength, privateKey, readerC1, writerC1);
            timeSum += System.currentTimeMillis() - timePre;
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        // System.out.println(args[0]);

        String c1 = "c1 8001";
        String c2 = "c2 127.0.0.1 8001 8002";
        String user = "user 127.0.0.1 8001 127.0.0.1 8002 10 1024 10 10 5"; // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength num m

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
