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

public class TestPaillierSBDS {
    /**
    * args: role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength num
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
        int num = Integer.parseInt(args[index++]);

        try (Socket socketC1 = new Socket(ipC1, portC1); Socket socketC2 = new Socket(ipC2, portC2);) {
            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            // Socket socketC2 = new Socket(ipC2, portC2);
            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            /*   */
            Util.writeInt(testNumber, writerC1);
            Util.writeInt(testNumber, writerC2);

            Util.writeInt(dataLength, writerC1);
            Util.writeInt(dataLength, writerC2);

            Util.writeInt(num, writerC1);
            Util.writeInt(num, writerC2);

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

                BigInteger[] xs = new BigInteger[num];
                BigInteger[] exs = new BigInteger[num];
                // BigInteger ex = Paillier.encrypt(publicKey, x);
                for (int j = 0; j < num; j++) {
                    xs[j] = new BigInteger(dataLength, random);
                    exs[j] = Paillier.encrypt(privateKey, xs[j]);
                }

                Util.writeBigIntegers(exs, writerC1);

                BigInteger[][] exBDs = Util.readBigIntegers(num, dataLength, readerC1);
                for (int l = 0; l < num; l++) {
                    String s = "";
                    for (int j = dataLength - 1; j >= 0; j--) {
                        BigInteger xj = Paillier.decrypt(privateKey, exBDs[l][j]);
                        s = xj.toString(2) + s;
                    }

                    BigInteger xBD = new BigInteger(s, 2);

                    if (!xs[l].equals(xBD)) {
                        System.out.println("Error!!!!");
                        System.out.println("x = " + xs[l].toString(2));
                        System.out.println("decrypting SBD of x: " + xBD.toString(2));
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

    /**
    * args: role portC1
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
        int dataLength = Util.readInt(readerUser);
        int num = Util.readInt(readerUser);
        PaillierPublicKey publicKey = Paillier.parseJsonToPublicKey(readerUser.readLine());

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            BigInteger[] exs = Util.readBigIntegers(num, readerUser);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.currentTimeMillis();
            BigInteger[][] exBD = Paillier.secureMultiBitDecompositionC1(exs, dataLength, publicKey, readerC2,
                    writerC2);
            timeSum += System.currentTimeMillis() - timePre;

            Util.writeBigIntegers(exBD, writerUser);
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
        int num = Util.readInt(readerUser);
        PaillierPrivateKey privateKey = Paillier.parseJsonToPrivateKey(readerUser.readLine());

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.currentTimeMillis();
            Paillier.secureMultiBitDecompositionC2(num, dataLength, privateKey, readerC1, writerC1);
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
        String user = "user 127.0.0.1 8001 127.0.0.1 8002 10 1024 10 50"; // role ipC1 portC1 ipC2 portC2 testNumber bitLength dataLength num

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
