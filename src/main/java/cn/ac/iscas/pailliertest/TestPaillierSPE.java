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

public class TestPaillierSPE {
    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength m
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

        int m = Integer.parseInt(args[index++]); // 维度

        Socket socketC1 = new Socket(ipC1, portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Socket socketC2 = new Socket(ipC2, portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /*  */
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

        Util.writeInt(m, writerC1);
        Util.writeInt(m, writerC2);

        Random random = new Random();
        for (int i = 0; i < testNumber; i++) {
            System.out.print(i + " ");

            BigInteger[][] p = new BigInteger[m][dataLength];
            BigInteger[][] lb = new BigInteger[m][dataLength];
            BigInteger[][] ub = new BigInteger[m][dataLength];
            boolean isInclose = true;
            for (int j = 0; j < m; j++) {
                BigInteger tpj = new BigInteger(dataLength, random);
                p[j] = Paillier.binaryBitEncrypt(tpj, dataLength, publicKey);

                BigInteger tlbj = new BigInteger(dataLength, random);
                BigInteger tubj = new BigInteger(dataLength, random);
                if (tlbj.compareTo(tubj) > 0) {
                    BigInteger t = tlbj;
                    tlbj = tubj;
                    tubj = t;
                }
                lb[j] = Paillier.binaryBitEncrypt(tlbj, dataLength, publicKey);
                ub[j] = Paillier.binaryBitEncrypt(tubj, dataLength, publicKey);

                if (tpj.compareTo(tlbj) < 0 || tpj.compareTo(tubj) > 0)
                    isInclose = false;
            }

            Util.writeBigIntegers(p, writerC1);
            Util.writeBigIntegers(lb, writerC1);
            Util.writeBigIntegers(ub, writerC1);

            BigInteger eAlpha = Util.readBigInteger(readerC1);
            BigInteger alpha = Paillier.decrypt(privateKey, eAlpha);
            if (!(alpha.equals(BigInteger.ONE) || alpha.equals(BigInteger.ZERO))
                    && (alpha.equals(BigInteger.ONE) && !isInclose) || (alpha.equals(BigInteger.ZERO) && isInclose)) {
                System.out.println("Error!!!!");
                System.out.println("is enclose: " + isInclose);
                System.out.println("alpha: " + alpha);
                break;
            }
        }
        long timeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("C1: " + timeC1 + " ms");
        System.out.println("C2: " + timeC2 + " ms");

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
        int dataLength = Util.readInt(readerUser);
        PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());

        int m = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger[][] p = Util.readBigIntegers(m, dataLength, readerUser);
            BigInteger[][] lb = Util.readBigIntegers(m, dataLength, readerUser);
            BigInteger[][] ub = Util.readBigIntegers(m, dataLength, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.currentTimeMillis();
            BigInteger eAlpha = Paillier.securePointEnclosureC1(p, lb, ub, publicKey, readerC2, writerC2);
            timeSum += System.currentTimeMillis() - timePre;

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
        int dataLength = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());

        int m = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.currentTimeMillis();
            Paillier.securePointEnclosureC2(m, dataLength, privateKey, readerC1, writerC1);
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
        String user = "user 127.0.0.1 8001 127.0.0.1 8002 10 1024 10 5"; // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength m

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
