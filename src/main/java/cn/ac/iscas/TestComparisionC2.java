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

public class TestComparisionC2 {

    public static void main(String[] args) {

        int portC1 = 8004;
        int portC2 = 8005;

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
            AdditiveSecretSharing.MultiplicationTriple triple = new AdditiveSecretSharing.MultiplicationTriple(a2, b2,
                    c2);
            int testNumber = Util.readInt(readerUser);
            int testType = Util.readInt(readerUser);
            System.out.println("testNumber=" + testNumber + ", testType=" + testType + ", mod=" + mod.toString());

            // ComparisonTuple cTuple2 = AdditiveSecretSharing.parseJsonToComparisionTuple(readerUser.readLine());
            RandomNumberTuple rTuple = AdditiveSecretSharing.parseJsonToRandomNumberTuple(readerUser.readLine());

            Util.writeInt(1, writerUser); // 提示User已收到，然后再开始传输数值

            AdditiveSecretSharing.PartyID partyID = AdditiveSecretSharing.PartyID.C2;

            long timeSum = 0L;
            for (int i = 0; i < testNumber; i++) {
                BigInteger x2 = Util.readBigInteger(readerUser);
                BigInteger y2 = Util.readBigInteger(readerUser);

                Util.writeInt(i, writerC1);
                Util.readInt(readerC1);

                BigInteger z2 = null;
                long preTime = System.nanoTime();
                // BigInteger z2 = AdditiveSecretSharing.secureComparision(AdditiveSecretSharing.PartyID.C2,
                //         x2.longValue(), y2.longValue(), triple, cTuple2, mod.longValue(), readerC1, writerC1);
                if (testType == 0) {
                    z2 = AdditiveSecretSharing.secureComparision(partyID, x2, y2, triple, mod, readerC1, writerC1);
                } else if (testType == 2) {
                    z2 = AdditiveSecretSharing.secureComparision(partyID, x2, y2, triple, rTuple, mod, readerC1,
                            writerC1);
                } else if (testType == 3) {
                    z2 = AdditiveSecretSharing.secureComparision(partyID, new BigInteger[] { x2, y2, x2 },
                            new BigInteger[] { y2, x2, y2 }, triple, rTuple, mod, readerC1, writerC1)[2];
                } else if (testType == 4) {
                    z2 = AdditiveSecretSharing.secureEqual(partyID, x2, y2,
                            triple, rTuple, mod, readerC1, writerC1);
                } else if (testType == 5) {
                    z2 = AdditiveSecretSharing.secureEqual(partyID, new BigInteger[] { x2, y2, x2 }, new BigInteger[] { y2, x2, y2 },
                            triple, rTuple, mod, readerC1, writerC1)[2];
                }
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
