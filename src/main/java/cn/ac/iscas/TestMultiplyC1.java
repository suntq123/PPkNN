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

public class TestMultiplyC1 {

    public static void main(String[] args) {
        int portC1 = 8001;

        try {
            ServerSocket serverSocket = new ServerSocket(portC1);
            Socket socketC2 = serverSocket.accept();
            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            Socket socketUser = serverSocket.accept();
            PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
            BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

            BigInteger mod = Util.readBigInteger(readerUser);
            BigInteger a1 = Util.readBigInteger(readerUser);
            BigInteger b1 = Util.readBigInteger(readerUser);
            BigInteger c1 = Util.readBigInteger(readerUser);
            AdditiveSecretSharing.MultiplicationTriple triple = new AdditiveSecretSharing.MultiplicationTriple(a1, b1, c1);

            int testNumber = Util.readInt(readerUser);
            System.out.println("testNumber=" + testNumber + ", mod=" + mod.toString());

            Util.writeInt(1, writerUser);

            // BigInteger x1 = Util.readBigInteger(readerUser);
            // BigInteger y1 = Util.readBigInteger(readerUser);

            long timeSum = 0L;
            for (int i = 0; i < testNumber; i++) {
                BigInteger x1 = Util.readBigInteger(readerUser);
                BigInteger y1 = Util.readBigInteger(readerUser);

                Util.writeInt(i, writerC2);
                Util.readInt(readerC2);

                long preTime = System.nanoTime();
                BigInteger z1 = AdditiveSecretSharing.multiply(AdditiveSecretSharing.PartyID.C1, x1, y1,
                        triple, mod, readerC2, writerC2);
                timeSum += System.nanoTime() - preTime;

                Util.writeBigInteger(z1, writerUser);
            }
            long timeAvg = timeSum / testNumber;
            System.out.println("Average Time: " + timeAvg + " ns");

            Util.writeLong(timeAvg, writerUser);

            socketC2.close();
            socketUser.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
