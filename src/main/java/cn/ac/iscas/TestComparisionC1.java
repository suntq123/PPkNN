package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.RandomNumberTuple;
import cn.ac.iscas.utils.Util;

public class TestComparisionC1 {

    public static void main(String[] args) {
        int portC1 = 8004;

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
            AdditiveSecretSharing.MultiplicationTriple triple = new AdditiveSecretSharing.MultiplicationTriple(a1, b1,
                    c1);
            int testNumber = Util.readInt(readerUser);
            int testType = Util.readInt(readerUser);
            System.out.println("testNumber=" + testNumber + ", testType=" + testType + ", mod=" + mod.toString());

            // ComparisonTuple cTuple1 = AdditiveSecretSharing.parseJsonToComparisionTuple(readerUser.readLine());
            RandomNumberTuple rTuple = AdditiveSecretSharing.parseJsonToRandomNumberTuple(readerUser.readLine());

            Util.writeInt(1, writerUser);

            AdditiveSecretSharing.PartyID partyID = AdditiveSecretSharing.PartyID.C1;

            long timeSum = 0L;
            for (int i = 0; i < testNumber; i++) {
                BigInteger x1 = Util.readBigInteger(readerUser);
                BigInteger y1 = Util.readBigInteger(readerUser);

                Util.writeInt(i, writerC2);
                Util.readInt(readerC2);

                // System.out.println("x1=" + x1 + ", y1=" + y1);
                BigInteger z1 = null;
                long preTime = System.nanoTime();
                // z1 = AdditiveSecretSharing.secureComparision(AdditiveSecretSharing.PartyID.C1,
                //         x1.longValue(), y1.longValue(), triple, cTuple1, mod.longValue(), readerC2, writerC2);
                if (testType == 0) {
                    z1 = AdditiveSecretSharing.secureComparision(partyID, x1, y1, triple, mod, readerC2, writerC2);
                } else if (testType == 2) {
                    z1 = AdditiveSecretSharing.secureComparision(partyID, x1, y1, triple, rTuple, mod, readerC2,
                            writerC2);
                } else if (testType == 3) {
                    z1 = AdditiveSecretSharing.secureComparision(partyID, new BigInteger[] { x1, y1, x1 },
                            new BigInteger[] { y1, x1, y1 }, triple, rTuple, mod, readerC2, writerC2)[2];
                } else if (testType == 4) {
                    z1 = AdditiveSecretSharing.secureEqual(partyID, x1, y1, triple, rTuple,
                            mod, readerC2, writerC2);
                } else if (testType == 5) {
                    z1 = AdditiveSecretSharing.secureEqual(partyID, new BigInteger[] { x1, y1, x1 }, new BigInteger[] { y1, x1, y1 },
                            triple, rTuple, mod, readerC2, writerC2)[2];
                }
                timeSum += System.nanoTime() - preTime;
                // System.out.println("z1=" + z1);

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
