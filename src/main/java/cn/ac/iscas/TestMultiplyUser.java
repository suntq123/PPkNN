package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.utils.Util;

public class TestMultiplyUser {

    public static void main(String[] args) {

        int portC1 = 8001;
        int portC2 = 8002;

        int testNumber = 10000;
        int dataBitLength = 64;

        // BigInteger mod = Util.getRandomPrimeBigInteger(dataBitLength-1, dataBitLength);
        BigInteger mod = BigInteger.probablePrime(dataBitLength, new Random());
        // BigInteger mod = BigInteger.valueOf(9223372036854775807L / 2);
        // BigInteger a = new BigInteger("646");
        // BigInteger b = new BigInteger("900");
        // BigInteger c = a.multiply(b).mod(mod);

        BigInteger a = new BigInteger(dataBitLength, new Random());
        BigInteger b = new BigInteger(dataBitLength, new Random());
        BigInteger c = a.multiply(b).mod(mod);

        System.out.println("mod = " + mod);
        System.out.println("a = " + a + ", b = " + b);

        BigInteger[] aSecrets = AdditiveSecretSharing.randomSplit(a, mod);
        BigInteger[] bSecrets = AdditiveSecretSharing.randomSplit(b, mod);
        BigInteger[] cSecrets = AdditiveSecretSharing.randomSplit(c, mod);

        try {
            Socket socketC1 = new Socket("127.0.0.1", portC1);
            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            Util.writeBigInteger(mod, writerC1);
            Util.writeBigInteger(aSecrets[0], writerC1);
            Util.writeBigInteger(bSecrets[0], writerC1);
            Util.writeBigInteger(cSecrets[0], writerC1);
            Util.writeInt(testNumber, writerC1);

            Socket socketC2 = new Socket("127.0.0.1", portC2);
            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            Util.writeBigInteger(mod, writerC2);
            Util.writeBigInteger(aSecrets[1], writerC2);
            Util.writeBigInteger(bSecrets[1], writerC2);
            Util.writeBigInteger(cSecrets[1], writerC2);
            Util.writeInt(testNumber, writerC2);

            Util.readInt(readerC1);
            Util.readInt(readerC2);

            Random random = new Random();
            // BigInteger x = new BigInteger(dataBitLength, random);
            // BigInteger[] xSecrets = AdditiveSecretSharing.randomSplit(x, mod);
            // BigInteger y = new BigInteger(dataBitLength, random);
            // BigInteger[] ySecrets = AdditiveSecretSharing.randomSplit(y, mod);

            // System.out.println("x = " + x + ", y = " + y);

            // Util.writeBigInteger(xSecrets[0], writerC1);
            // Util.writeBigInteger(xSecrets[1], writerC2);

            // Util.writeBigInteger(ySecrets[0], writerC1);
            // Util.writeBigInteger(ySecrets[1], writerC2);

            for (int i = 0; i < testNumber; i++) {
                BigInteger x = new BigInteger(dataBitLength, random);
                BigInteger[] xSecrets = AdditiveSecretSharing.randomSplit(x, mod);
                BigInteger y = new BigInteger(dataBitLength, random);
                BigInteger[] ySecrets = AdditiveSecretSharing.randomSplit(y, mod);

                Util.writeBigInteger(xSecrets[0], writerC1);
                Util.writeBigInteger(xSecrets[1], writerC2);

                Util.writeBigInteger(ySecrets[0], writerC1);
                Util.writeBigInteger(ySecrets[1], writerC2);

                BigInteger z1 = Util.readBigInteger(readerC1);
                BigInteger z2 = Util.readBigInteger(readerC2);

                if (!z1.add(z2).mod(mod).equals(x.multiply(y).mod(mod))) { // 结果验证
                    System.out.println("Error: z1 + z2 != x * y");
                }
            }

            long timeC1 = Util.readLong(readerC1);
            long timeC2 = Util.readLong(readerC2);

            System.out.println("C1: " + timeC1 + " ns");
            System.out.println("C2: " + timeC2 + " ns");

            socketC1.close();
            socketC2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
