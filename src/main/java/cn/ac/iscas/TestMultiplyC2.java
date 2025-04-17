package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import cn.ac.iscas.secretsharing.*;
import cn.ac.iscas.utils.Util;

public class TestMultiplyC2 {

    public static void main(String[] args) {


        int portC1 = 8001;
        int portC2 = 8002;

        try {
            Socket socketC1 = new Socket("127.0.0.1", portC1);
            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            ServerSocket serverSocketUser = new ServerSocket(portC2);
            Socket socketUser = serverSocketUser.accept();
            PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
            BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

            BigInteger mod = Util.readBigInteger(readerUser);
            BigInteger a2 = Util.readBigInteger(readerUser);
            BigInteger b2 = Util.readBigInteger(readerUser);
            BigInteger c2 = Util.readBigInteger(readerUser);
            AdditiveSecretSharing.MultiplicationTriple triple = new AdditiveSecretSharing.MultiplicationTriple(a2, b2, c2);

            int testNumber = Util.readInt(readerUser);
            System.out.println("testNumber=" + testNumber + ", mod=" + mod.toString());

            Util.writeInt(1, writerUser);

            // BigInteger x2 = Util.readBigInteger(readerUser);
            // BigInteger y2 = Util.readBigInteger(readerUser);

            long timeSum = 0L;
            for (int i = 0; i < testNumber; i++) {
                BigInteger x2 = Util.readBigInteger(readerUser);
                BigInteger y2 = Util.readBigInteger(readerUser);

                Util.writeInt(i, writerC1);
                Util.readInt(readerC1);

                long preTime = System.nanoTime();
                BigInteger z2 = AdditiveSecretSharing.multiply(AdditiveSecretSharing.PartyID.C2, x2, y2,
                        triple, mod, readerC1, writerC1);
                timeSum += System.nanoTime() - preTime;

                Util.writeBigInteger(z2, writerUser);
            }
            long timeAvg = timeSum / testNumber;
            System.out.println("Average Time: " + timeAvg + " ns");

            Util.writeLong(timeAvg, writerUser);

            socketC1.close();
            socketUser.close();
            serverSocketUser.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
