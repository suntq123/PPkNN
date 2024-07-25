package cn.ac.iscas.pailliertest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.paillier.Paillier.PaillierPublicKey;
import cn.ac.iscas.utils.Util;

public class TestPaillierSMINKth {

    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength num k
    * 
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

        int testNumber = Integer.parseInt(args[index++]);
        int bitLength = Integer.parseInt(args[index++]);
        int dataLength = Integer.parseInt(args[index++]);
        int num = Integer.parseInt(args[index++]); // 点的数量
        int k = Integer.parseInt(args[index++]); // 前k小

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

        Util.writeInt(num, writerC1);
        Util.writeInt(num, writerC2);

        Util.writeInt(dataLength, writerC1);
        Util.writeInt(dataLength, writerC2);

        Util.writeInt(k, writerC1);
        Util.writeInt(k, writerC2);

        Random random = new Random();
        for (int i = 0; i < testNumber; i++) {
            System.out.print(i + " ");

            BigInteger[] xs = new BigInteger[num];
            BigInteger[] exs = new BigInteger[num];
            for (int j = 0; j < num; j++) {
                xs[j] = new BigInteger(num, random);

                exs[j] = Paillier.encrypt(privateKey, xs[j]);
            }

            Util.writeBigIntegers(exs, writerC1);

            BigInteger eKthMin = Util.readBigInteger(readerC1);

            Arrays.sort(xs, 0, num, (b1, b2) -> {
                return b1.compareTo(b2);
            });

            BigInteger tKth = xs[k-1];
            BigInteger dKth = Paillier.decrypt(privateKey, eKthMin);
            if (!tKth.equals(dKth)) {
                System.out.println("Error!!!!!!!");
                System.out.println("true kth: " + tKth);
                System.out.println("D(E(Kth)): " + dKth);
            }
        }
        long timeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("C1: " + timeC1 + " ms");
        System.out.println("C2: " + timeC2 + " ms");

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

        int num = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);
        int k = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger[] exs = Util.readBigIntegers(num, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.currentTimeMillis();
            BigInteger epKMin = Paillier.secureMinKthC1(k, exs, dataLength, publicKey, readerC2, writerC2);
            // BigInteger epKMin = Paillier.mySecureMINKC1(k, exs, dataLength, publicKey, readerC2, writerC2)[k-1];
            // BigInteger epKMin = Paillier.mySecureMINKthC1(k, exs, dataLength, publicKey, readerC2, writerC2);
            timeSum += System.currentTimeMillis() - timePre;

            Util.writeBigInteger(epKMin, writerUser);
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

        int num = Util.readInt(readerUser);
        int dataLength = Util.readInt(readerUser);
        int k = Util.readInt(readerUser);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            // System.out.println(i);
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.currentTimeMillis();
            Paillier.secureMinKthC2(k, num, dataLength, privateKey, readerC1, writerC1);
            // Paillier.mySecureMINKC2(k, num, dataLength, privateKey, readerC1, writerC1);
            // Paillier.mySecureMINKthC2(k, num, dataLength, privateKey, readerC1, writerC1);
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
        String user = "user 127.0.0.1 8001 127.0.0.1 8002 10 1024 10 10 5"; // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength num k

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
