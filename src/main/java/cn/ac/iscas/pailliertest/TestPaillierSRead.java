package cn.ac.iscas.pailliertest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.paillier.Paillier.PaillierPublicKey;
import cn.ac.iscas.utils.Util;

public class TestPaillierSRead {
    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength bNum bSize m
    * 
    * @param args
    * @throws IOException
    */
    public static void user(String[] args) throws IOException {
        /*  */
        int index = 1;
        String ipC1 = args[index++];
        int portC1 = Integer.parseInt(args[index++]);
        String ipC2 = args[index++];
        int portC2 = Integer.parseInt(args[index++]);

        int testNumber = Integer.parseInt(args[index++]);
        int bitLength = Integer.parseInt(args[index++]);
        int dataLength = Integer.parseInt(args[index++]);

        int bNum = Integer.parseInt(args[index++]); 
        int bSize = Integer.parseInt(args[index++]); 
        int m = Integer.parseInt(args[index++]); 

        try (Socket socketC1 = new Socket(ipC1, portC1); Socket socketC2 = new Socket(ipC2, portC2);) {
            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            /*  */
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

            Util.writeInt(bNum, writerC1);
            Util.writeInt(bNum, writerC2);

            Util.writeInt(bSize, writerC1);
            Util.writeInt(bSize, writerC2);

            Util.writeInt(m, writerC1);
            Util.writeInt(m, writerC2);

            Random random = new Random();
            for (int testCount = 0; testCount < testNumber; testCount++) {
                System.out.print(testCount + " ");

                int aCount = random.nextInt(bNum / 2) + 1;
                // aCount = 0;
                int tPIndex = 0;
                BigInteger[][] tPoints = new BigInteger[aCount * bSize][m];
                List<BigInteger> alphas = new ArrayList<>();
                for (int i = 0; i < bNum; i++) {
                    if (aCount > 0) {
                        alphas.add(BigInteger.ONE);
                        aCount--;
                    } else {
                        alphas.add(BigInteger.ZERO);
                    }
                }
                Collections.shuffle(alphas, random);

                BigInteger[] eAlphas = new BigInteger[bNum];
                BigInteger[][][] buckets = new BigInteger[bNum][bSize][m];
                BigInteger[][][] ebuckets = new BigInteger[bNum][bSize][m];
                for (int i = 0; i < bNum; i++) {
                    eAlphas[i] = Paillier.encrypt(publicKey, alphas.get(i));
                    for (int j = 0; j < bSize; j++) {
                        for (int k = 0; k < m; k++) {
                            BigInteger t = new BigInteger(dataLength, random);
                            buckets[i][j][k] = t;
                            ebuckets[i][j][k] = Paillier.encrypt(publicKey, t);
                        }

                        if (alphas.get(i).equals(BigInteger.ONE)) {
                            tPoints[tPIndex++] = buckets[i][j];
                        }
                    }
                }

                Util.writeBigIntegers(ebuckets, writerC1);
                Util.writeBigIntegers(eAlphas, writerC1);

                int pNum = Util.readInt(readerC1);
                BigInteger[][] ePoints = Util.readBigIntegers(pNum, m, readerC1);

                if (pNum != tPoints.length) {
                    System.out.println("Error!!!!");
                    System.out.println("pNum = " + pNum);
                    System.out.println("tPoints.length = " + tPoints.length);

                    return;
                }

                for (int i = 0; i < pNum; i++) {
                    for (int j = 0; j < m; j++) {
                        BigInteger t = Paillier.decrypt(privateKey, ePoints[i][j]);

                        if (!t.equals(tPoints[i][j])) {
                            System.out.println("Error!!!!");
                            System.out.println("i = " + i + " , j = "+ j);
                            System.out.println("tPoints[i][j] = " + tPoints[i][j]);
                            System.out.println("D(E(Points[i][j])) = " + t);

                            return;
                        }
                    }
                }
            }
            long timeC1 = Util.readLong(readerC1);
            long timeC2 = Util.readLong(readerC2);
            System.out.println("C1: " + timeC1 + " ms");
            System.out.println("C2: " + timeC2 + " ms");

            socketC1.close();
            socketC2.close();
        }
    }

    /**
    * args: role portC1
    * 
    * @param args
    * @throws IOException
    */
    public static void c1(String[] args) throws IOException {
        /*  */
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

        int bNum = Util.readInt(readerUser);
        int bSize = Util.readInt(readerUser);
        int m = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {

            BigInteger[][][] eBuckets = Util.readBigIntegers(bNum, bSize, m, readerUser);
            BigInteger[] eAlphas = Util.readBigIntegers(bNum, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.currentTimeMillis();
            BigInteger[][] ePoints = Paillier.secureReadC1(eBuckets, eAlphas, publicKey, readerC2, writerC2);
            timeSum += System.currentTimeMillis() - timePre;

            Util.writeInt(ePoints.length, writerUser);
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

        int bNum = Util.readInt(readerUser);
        int bSize = Util.readInt(readerUser);
        int m = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.currentTimeMillis();
            Paillier.secureReadC2(bNum, bSize, m, privateKey, readerC1, writerC1);
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
        String user = "user 127.0.0.1 8001 127.0.0.1 8002 10 1024 10 10 5 3"; // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength bNum bSize m

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
