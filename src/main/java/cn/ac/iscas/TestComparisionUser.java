package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.RandomNumberTuple;
import cn.ac.iscas.utils.Util;

public class TestComparisionUser {

    public static void main(String[] args) {

        int portC1 = 8004;
        int portC2 = 8005;

        // 0- ; 1- ; 2-secureComparsion V2; 3-secureComparsion V2 S; 4-secureEqual ; 5-secureEqual S
        int testType = 2;

        int testNumber = 10000;
        int dataLength = 10;
        int l = -1;
        if (testType == 2 || testType == 3) {
            // dataBound = mod.divide(BigInteger.TWO).subtract(BigInteger.ONE);
            l = dataLength + 2;
        } else if (testType == 0 || testType == 4 || testType == 5) {
            // dataBound = mod;
            l = dataLength + 1;
        }
        BigInteger mod = BigInteger.probablePrime(l, new Random());
        BigInteger dataBound = BigInteger.TWO.pow(dataLength).subtract(BigInteger.ONE);
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
        System.out.println(aSecrets[0] + "   " + aSecrets[1]);
        System.out.println(bSecrets[0] + "   " + bSecrets[1]);
        System.out.println(cSecrets[0] + "   " + cSecrets[1]);

        // ComparisonTuple[] cTuples = AdditiveSecretSharing.generateComparsionTuple(s, mod.longValue());

        RandomNumberTuple[] rTuples = AdditiveSecretSharing.generateRandomNumberTuples(l, mod);

        try (Socket socketC1 = new Socket("127.0.0.1", portC1); Socket socketC2 = new Socket("127.0.0.1", portC2);) {

            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            Util.writeBigInteger(mod, writerC1);
            Util.writeBigInteger(aSecrets[0], writerC1);
            Util.writeBigInteger(bSecrets[0], writerC1);
            Util.writeBigInteger(cSecrets[0], writerC1);
            Util.writeInt(testNumber, writerC1);
            Util.writeInt(testType, writerC1);

            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            Util.writeBigInteger(mod, writerC2);
            Util.writeBigInteger(aSecrets[1], writerC2);
            Util.writeBigInteger(bSecrets[1], writerC2);
            Util.writeBigInteger(cSecrets[1], writerC2);
            Util.writeInt(testNumber, writerC2);
            Util.writeInt(testType, writerC2);

            writerC1.println(AdditiveSecretSharing.parseRandomNumberTupleToJson(rTuples[0]));
            writerC1.flush();
            writerC2.println(AdditiveSecretSharing.parseRandomNumberTupleToJson(rTuples[1]));
            writerC2.flush();

            Util.readInt(readerC1);
            Util.readInt(readerC2);

            Random random = new Random();
            for (int i = 0; i < testNumber; i++) {
                BigInteger x = null, y = null;
                if (testType == 0 || testType == 2 || testType == 3) {
                    x = Util.getRandomBigInteger(dataBound);
                    y = Util.getRandomBigInteger(dataBound);
                } else if (testType == 4 || testType == 5) {
                    x = Util.getRandomBigInteger(dataBound);
                    y = (random.nextBoolean()) ? x : Util.getRandomBigInteger(dataBound);
                }

                BigInteger[] xSecrets = AdditiveSecretSharing.randomSplit(x, mod);
                BigInteger[] ySecrets = AdditiveSecretSharing.randomSplit(y, mod);

                Util.writeBigInteger(xSecrets[0], writerC1);
                Util.writeBigInteger(xSecrets[1], writerC2);

                Util.writeBigInteger(ySecrets[0], writerC1);
                Util.writeBigInteger(ySecrets[1], writerC2);

                BigInteger z1 = Util.readBigInteger(readerC1);
                BigInteger z2 = Util.readBigInteger(readerC2);
                BigInteger z = AdditiveSecretSharing.add(z1, z2, mod);

                if (testType == 0 || testType == 2 || testType == 3) {
                    if ((z.equals(BigInteger.ONE) && x.compareTo(y) >= 0)
                            || (z.equals(BigInteger.ZERO) && x.compareTo(y) < 0)
                            || !(z.equals(BigInteger.ZERO) || z.equals(BigInteger.ONE))) {
                        System.out.println("Wrong!!!!!");
                        System.out.println(i + ": x=" + x + ", y=" + y + "   z=" + z);
                        // return;
                    }
                } else if (testType == 4 || testType == 5) {
                    if ((z.equals(BigInteger.ONE) && !x.equals(y)) || (z.equals(BigInteger.ZERO) && x.equals(y))) {
                        System.out.println("Wrong!!!!!");
                        System.out.println(i + ": x=" + x + ", y=" + y + "   z=" + z);
                        // return;
                    }
                } else {
                    System.out.println("Invalid testType !!!!!");
                    return;
                }

                // System.out.println(i + ": x=" + x + ", y=" + y + "   z=" + z);
            }

            long timeC1 = Util.readLong(readerC1);
            long timeC2 = Util.readLong(readerC2);

            System.out.println("C1: " + timeC1 + " ns");
            System.out.println("C2: " + timeC2 + " ns");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
