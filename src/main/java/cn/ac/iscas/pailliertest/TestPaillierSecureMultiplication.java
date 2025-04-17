package cn.ac.iscas.pailliertest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.paillier.Paillier.PaillierPublicKey;
import cn.ac.iscas.utils.Util;

public class TestPaillierSecureMultiplication {

    /**
     * args: role ipC1 portC1 ipC2 portC2 bitLength testNumber
     * 
     * user 127.0.0.1 8001 127.0.0.1 8002 10 10
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

        int bitLength = Integer.parseInt(args[index++]);
        int testNumber = Integer.parseInt(args[index++]);

        Socket socketC1 = new Socket(ipC1, portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Socket socketC2 = new Socket(ipC2, portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        /*  */
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

        for (int i = 0; i < testNumber; i++) {
            System.out.print(i + " ");
            
            BigInteger a = Util.getRandomBigInteger(publicKey.n);
            BigInteger b = Util.getRandomBigInteger(publicKey.n);
            // System.out.println("a = " + a);
            // System.out.println("b = " + b);

            BigInteger ea = Paillier.encrypt(publicKey, a);
            BigInteger eb = Paillier.encrypt(publicKey, b);
            // System.out.println("ea = " + ea);
            // System.out.println("eb = " + eb);

            Util.writeBigInteger(ea, writerC1);
            Util.writeBigInteger(eb, writerC1);
            // System.out.println("sent ea and eb.");

            BigInteger eab = Util.readBigInteger(readerC1);
            BigInteger ab = Paillier.decrypt(privateKey, eab);

            if (!a.multiply(b).mod(publicKey.n).equals(ab)) {
                System.out.println("Error!!!!!!!!!!");
                System.out.println(i);

                System.out.println("a = " + a);
                System.out.println("b = " + b);
                System.out.println("a*b mod n = " + a.multiply(b).mod(publicKey.n));
                System.out.println("D(E(a*b)) = " + ab);

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

    /**
    * args: role portC1
    * c1 8001 
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

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            
            BigInteger ea = Util.readBigInteger(readerUser);
            BigInteger eb = Util.readBigInteger(readerUser);
            // System.out.println("ea = " + ea);
            // System.out.println("eb = " + eb);

            Util.writeInt(i, writerC2);
            Util.readInt(readerC2);

            long timePre = System.nanoTime();
            BigInteger eab = Paillier.secureMultiplicationC1(ea, eb, publicKey, readerC2, writerC2);
            timeSum += System.nanoTime() - timePre;

            Util.writeBigInteger(eab, writerUser);
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        writerUser.close();
        socketC2.close();
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

        long timeSum = 0L;
        for (int i = 0; i < testNumber; i++) {
            Util.writeInt(i, writerC1);
            Util.readInt(readerC1);

            long timePre = System.nanoTime();
            Paillier.secureMultiplicationC2(privateKey, readerC1, writerC1);
            timeSum += System.nanoTime() - timePre;
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        writerUser.close();

        socketC1.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        System.out.println(Arrays.asList(args));

        // String c1 = "c1 8001";
        // String c2 = "c2 127.0.0.1 8001 8002";
        // String user = "user 127.0.0.1 8001 127.0.0.1 8002 2048 100";   // role ipC1 portC1 ipC2 portC2 bitLength testNumber

        // String c1 = "c1 8001";
        String c2 = "c2 192.168.1.5 8001 8002";
        String user = "user 192.168.1.5 8001 127.0.0.1 8002 2048 100";   // role ipC1 portC1 ipC2 portC2 bitLength testNumber

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
