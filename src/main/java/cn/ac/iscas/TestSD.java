package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.ComparisonTuple;
import cn.ac.iscas.utils.Util;

public class TestSD {

    public static void c1() throws IOException {
        int portC1 = 8004;

        AdditiveSecretSharing.PartyID partyID = AdditiveSecretSharing.PartyID.C1;

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
        int dimension = Util.readInt(readerUser);
        int testType = Util.readInt(readerUser);

        ComparisonTuple cTuple1 = AdditiveSecretSharing.parseJsonToComparisionTuple(readerUser.readLine());

        System.out.println("mod = " + mod);
        System.out.println("a1 = " + a1 + ", b1 = " + b1 + ", c1 = " + c1);
        System.out.println("testNumber = " + testNumber);

        long timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            BigInteger d1 = null;
            if (testType == 1) {
                BigInteger[] p1 = Util.readBigIntegers(dimension, readerUser);
                BigInteger[] q1 = Util.readBigIntegers(dimension, readerUser);

                Util.writeInt(i, writerC2);
                Util.readInt(readerC2);

                //
                long timePre = System.nanoTime();
                d1 = AdditiveSecretSharing.secureMinkowskiDistance(partyID, 2, p1, q1, triple, cTuple1, mod,
                        readerC2, writerC2);
                timeSum += System.nanoTime() - timePre;
            } else if (testType == 2) {
                BigInteger[] p1 = Util.readBigIntegers(dimension, readerUser);
                BigInteger[][] box1 = Util.readBigIntegers(dimension, 2, readerUser);

                Util.writeInt(i, writerC2);
                Util.readInt(readerC2);

                //
                long timePre = System.nanoTime();
                d1 = AdditiveSecretSharing.secureMinDistanceFromBoxToPoint(partyID, 2, box1, p1, triple, cTuple1, mod,
                        readerC2, writerC2);
                timeSum += System.nanoTime() - timePre;
            }
            Util.writeBigInteger(d1, writerUser);
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC2.close();
        socketUser.close();
        serverSocket.close();
    }

    public static void c2() throws IOException {

        int portC1 = 8004;
        int portC2 = 8005;

        AdditiveSecretSharing.PartyID partyID = AdditiveSecretSharing.PartyID.C2;

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
        int dimension = Util.readInt(readerUser);
        int testType = Util.readInt(readerUser);

        ComparisonTuple cTuple2 = AdditiveSecretSharing.parseJsonToComparisionTuple(readerUser.readLine());

        System.out.println("mod = " + mod);
        System.out.println("a2 = " + a2 + ", b2 = " + b2 + ", c2 = " + c2);
        System.out.println("testNumber = " + testNumber);

        long timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            BigInteger d2 = null;
            if (testType == 1) {
                BigInteger[] p2 = Util.readBigIntegers(dimension, readerUser);
                BigInteger[] q2 = Util.readBigIntegers(dimension, readerUser);

                Util.writeInt(i, writerC1);
                Util.readInt(readerC1);

                long timePre = System.nanoTime();
                d2 = AdditiveSecretSharing.secureMinkowskiDistance(partyID, 2, p2, q2, triple, cTuple2, mod,
                        readerC1, writerC1);
                timeSum += System.nanoTime() - timePre;
            } else if (testType == 2) {
                BigInteger[] p2 = Util.readBigIntegers(dimension, readerUser);
                BigInteger[][] box2 = Util.readBigIntegers(dimension, 2, readerUser);

                Util.writeInt(i, writerC1);
                Util.readInt(readerC1);

                long timePre = System.nanoTime();
                d2 = AdditiveSecretSharing.secureMinDistanceFromBoxToPoint(partyID, 2, box2, p2, triple, cTuple2, mod,
                        readerC1, writerC1);
                timeSum += System.nanoTime() - timePre;
            }

            Util.writeBigInteger(d2, writerUser);
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocketUser.close();
    }

    public static void user(int testType) throws IOException {

        int portC1 = 8004;
        int portC2 = 8005;

        int testNumber = 10000;
        int bitLength = 20;
        int dimension = 2;

        Random random = new Random();
        int overflow = (int) Math.ceil(Util.log2(dimension));
        int s = bitLength * 2 + overflow; // s
        int l = s + 3; //  s < l - 2
        BigInteger mod = BigInteger.TWO.pow(l);
        System.out.println("mod = " + mod);

        BigInteger a = Util.getRandomBigInteger(mod);
        BigInteger b = Util.getRandomBigInteger(mod);
        BigInteger c = a.multiply(b).mod(mod);
        System.out.println("a = " + a + ", b = " + b + ", c = " + c);

        BigInteger[] aSecrets = new BigInteger[2], bSecrets = new BigInteger[2], cSecrets = new BigInteger[2];
        boolean flag = true;
        while (flag) {
            aSecrets = AdditiveSecretSharing.randomSplit(a, mod);
            bSecrets = AdditiveSecretSharing.randomSplit(b, mod);
            cSecrets = AdditiveSecretSharing.randomSplit(c, mod);

            BigInteger ta = aSecrets[0].mod(BigInteger.TWO).add(aSecrets[1].mod(BigInteger.TWO));
            BigInteger tb = bSecrets[0].mod(BigInteger.TWO).add(bSecrets[1].mod(BigInteger.TWO));
            BigInteger tc = cSecrets[0].mod(BigInteger.TWO).add(cSecrets[1].mod(BigInteger.TWO));
            if (ta.multiply(tb).equals(tc))
                flag = false;
        }

        ComparisonTuple[] cTuples = AdditiveSecretSharing.generateComparsionTuple(s, mod.longValue());

        Socket socketC1 = new Socket("127.0.0.1", portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        Util.writeBigInteger(mod, writerC1);
        Util.writeBigInteger(aSecrets[0], writerC1);
        Util.writeBigInteger(bSecrets[0], writerC1);
        Util.writeBigInteger(cSecrets[0], writerC1);
        Util.writeInt(testNumber, writerC1);
        Util.writeInt(dimension, writerC1);
        Util.writeInt(testType, writerC1);

        Socket socketC2 = new Socket("127.0.0.1", portC2);
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        Util.writeBigInteger(mod, writerC2);
        Util.writeBigInteger(aSecrets[1], writerC2);
        Util.writeBigInteger(bSecrets[1], writerC2);
        Util.writeBigInteger(cSecrets[1], writerC2);
        Util.writeInt(testNumber, writerC2);
        Util.writeInt(dimension, writerC2);
        Util.writeInt(testType, writerC2);

        writerC1.println(AdditiveSecretSharing.parseComparisionTupleToJson(cTuples[0]));
        writerC1.flush();

        writerC2.println(AdditiveSecretSharing.parseComparisionTupleToJson(cTuples[1]));
        writerC2.flush();

        for (int j = 0; j < testNumber; j++) {
            if (testType == 1) {
                sdc(random, bitLength, dimension, mod, readerC1, writerC1, readerC2, writerC2);
            } else if (testType == 2) {
                smdc(random, bitLength, dimension, mod, readerC1, writerC1, readerC2, writerC2);
            }
        }
        System.out.println();
        long timeC1 = Util.readLong(readerC1);
        long timeC2 = Util.readLong(readerC2);
        System.out.println("Time C1: " + timeC1 + " ns");
        System.out.println("Time C2: " + timeC2 + " ns");

        readerC1.close();
        readerC2.close();
        socketC1.close();
        socketC2.close();
    }

    public static void sdc(Random random, int bitLength, int dimension, BigInteger mod, BufferedReader readerC1,
            PrintWriter writerC1, BufferedReader readerC2, PrintWriter writerC2) throws IOException {
        BigInteger[] p = new BigInteger[dimension];
        BigInteger[] q = new BigInteger[dimension];
        BigInteger[][] pSecrets = new BigInteger[2][dimension];
        BigInteger[][] qSecrets = new BigInteger[2][dimension];

        for (int i = 0; i < dimension; i++) {
            p[i] = new BigInteger(bitLength, random);
            BigInteger[] piSecrets = AdditiveSecretSharing.randomSplit(p[i], mod);
            pSecrets[0][i] = piSecrets[0];
            pSecrets[1][i] = piSecrets[1];

            q[i] = new BigInteger(bitLength, random);
            BigInteger[] qiSecrets = AdditiveSecretSharing.randomSplit(q[i], mod);
            qSecrets[0][i] = qiSecrets[0];
            qSecrets[1][i] = qiSecrets[1];
        }

        Util.writeBigIntegers(pSecrets[0], writerC1);
        Util.writeBigIntegers(qSecrets[0], writerC1);

        Util.writeBigIntegers(pSecrets[1], writerC2);
        Util.writeBigIntegers(qSecrets[1], writerC2);

        BigInteger d1 = Util.readBigInteger(readerC1);
        BigInteger d2 = Util.readBigInteger(readerC2);

        BigInteger ssd = AdditiveSecretSharing.add(d1, d2, mod);

        BigInteger distance = Util.squareEuclideanDistance(p, q);

        if (!ssd.equals(distance)) {
            System.out.println("Error!!!!!");
            System.out.println("real distance: " + distance);
            System.out.println("SS distance: " + ssd);
        }
    }

    public static void smdc(Random random, int bitLength, int dimension, BigInteger mod, BufferedReader readerC1,
            PrintWriter writerC1, BufferedReader readerC2, PrintWriter writerC2) throws IOException {

        BigInteger[] p = new BigInteger[dimension];
        BigInteger[][] box = new BigInteger[dimension][2];
        BigInteger[][] pSecrets = new BigInteger[2][dimension];
        BigInteger[][][] boxSecrets = new BigInteger[2][dimension][2];

        for (int i = 0; i < dimension; i++) {
            p[i] = new BigInteger(bitLength, random);
            BigInteger[] piSecrets = AdditiveSecretSharing.randomSplit(p[i], mod);
            pSecrets[0][i] = piSecrets[0];
            pSecrets[1][i] = piSecrets[1];

            box[i][0] = new BigInteger(bitLength, random);
            box[i][1] = new BigInteger(bitLength, random);

            if(box[i][0].compareTo(box[i][1]) > 0){
                BigInteger t = box[i][0];
                box[i][0] = box[i][1];
                box[i][1] = t;
            }

            BigInteger[] lbiSecrets = AdditiveSecretSharing.randomSplit(box[i][0], mod);
            boxSecrets[0][i][0] = lbiSecrets[0];
            boxSecrets[1][i][0] = lbiSecrets[1];

            BigInteger[] ubiSecrets = AdditiveSecretSharing.randomSplit(box[i][1], mod);
            boxSecrets[0][i][1] = ubiSecrets[0];
            boxSecrets[1][i][1] = ubiSecrets[1];
        }

        Util.writeBigIntegers(pSecrets[0], writerC1);
        Util.writeBigIntegers(boxSecrets[0], writerC1);

        Util.writeBigIntegers(pSecrets[1], writerC2);
        Util.writeBigIntegers(boxSecrets[1], writerC2);

        BigInteger d1 = Util.readBigInteger(readerC1);
        BigInteger d2 = Util.readBigInteger(readerC2);

        BigInteger ssd = AdditiveSecretSharing.add(d1, d2, mod);

        BigInteger distance = BigInteger.ZERO;
        for (int i = 0; i < dimension; i++) {
            if (p[i].compareTo(box[i][0]) < 0) {
                distance = distance.add(box[i][0].subtract(p[i]).pow(2));
            } else if (box[i][1].compareTo(p[i]) < 0) {
                distance = distance.add(p[i].subtract(box[i][1]).pow(2));
            }
        }

        if (!ssd.equals(distance)) {
            System.out.println("Error!!!!!");
            System.out.println("real distance: " + distance);
            System.out.println("SS distance: " + ssd);
        }
    }

    public static void main(String[] args) throws IOException {
        int testType; // 1: SDC, 2: SMDC
        testType = 1;
        testType = 2;

        int role = 3; // 1: C1, 2: C2, 3: user
        role = 1;
        role = 2;
        role = 3;

        if (role == 1)
            c1();
        else if (role == 2)
            c2();
        else if (role == 3)
            user(testType);
    }
}
