package cn.ac.iscas.pailliertest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;

import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.utils.Util;

public class TestPaillierNetwork {
    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataNumber
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
        int dataNumber = Integer.parseInt(args[index++]);

        Socket socketC1 = new Socket(ipC1, portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Socket socketC2 = new Socket(ipC2, portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /* 计算过程 */
        Util.writeInt(testNumber, writerC1);
        Util.writeInt(testNumber, writerC2);
        Util.writeInt(dataNumber, writerC1);
        Util.writeInt(dataNumber, writerC2);

        Paillier paillier = new Paillier(bitLength);
        // PaillierPublicKey publicKey = paillier.getPaillierPublicKey();
        PaillierPrivateKey privateKey = paillier.getPaillierPrivateKey();

        String privateKeyJson = Paillier.parsePrivateKeyToJson(privateKey);
        writerC1.println(privateKeyJson);
        writerC1.flush();
        writerC2.println(privateKeyJson);
        writerC2.flush();

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
        int testNumber = Util.readInt(readerUser);
        int dataNumber = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());
        // PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());
        Paillier.a = new BigInteger(privateKey.publicKey.n.bitLength() / 2 + 1, new SecureRandom());

        BigInteger x = Util.getRandomBigInteger(privateKey.publicKey.n);
        BigInteger ex = Paillier.encrypt(privateKey, x);
        BigInteger[] exs = new BigInteger[dataNumber];
        Arrays.fill(exs, 0, dataNumber, ex);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.nanoTime();
            // Util.writeBigIntegers(exs, writerC2);
            // Util.readBigIntegers(dataNumber, readerC2);
            Util.exchangeBigIntegers(exs, readerC2, writerC2);
            timeSum += System.nanoTime() - timePre;
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
        int dataNumber = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());
        // PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());
        Paillier.a = new BigInteger(privateKey.publicKey.n.bitLength() / 2 + 1, new SecureRandom());

        BigInteger x = Util.getRandomBigInteger(privateKey.publicKey.n);
        BigInteger ex = Paillier.encrypt(privateKey, x);
        BigInteger[] exs = new BigInteger[dataNumber];
        Arrays.fill(exs, 0, dataNumber, ex);

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.nanoTime();
            // Util.writeBigIntegers(exs, writerC1);
            // Util.readBigIntegers(dataNumber, readerC1);
            Util.exchangeBigIntegers(exs, readerC1, writerC1);
            timeSum += System.nanoTime() - timePre;
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {

        // args不是null，其长度为0。
        System.out.println("args: " + Arrays.asList(args));

        // String c1 = "c1 8001";
        // String c2 = "c2 127.0.0.1 8001 8002";
        // // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataNumber
        // String user = "user 127.0.0.1 8001 127.0.0.1 8002 100 2048 4";

        // String c1 = "c1 8001";
        String c2 = "c2 192.168.1.5 8001 8002";
        // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataNumber
        String user = "user 192.168.1.5 8001 127.0.0.1 8002 100 2048 30200";

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
