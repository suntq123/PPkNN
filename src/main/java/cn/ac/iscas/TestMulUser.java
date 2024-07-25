package cn.ac.iscas;

import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.utils.Util;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import static cn.ac.iscas.secretsharing.AdditiveSecretSharing.generateMultiplicationTriples;

public class TestMulUser {

    public static void main(String[] args) {
        int portC1 = 8001;
        int portC2 = 8002;

        int testNumber = 10;
        int dataBitLength = 20;

        int l = 20 * 2 + (int) Util.log2(2) + 2;
        BigInteger mod = BigInteger.probablePrime(l, new Random());

        AdditiveSecretSharing.MultiplicationTriple[] triples = generateMultiplicationTriples(mod);

        try (Socket socketC1 = new Socket("192.168.0.2", portC1); Socket socketC2 = new Socket("192.168.0.3", portC2);) {

            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            Util.writeBigInteger(mod, writerC1);
            Util.writeBigInteger(triples[0].ai, writerC1);
            Util.writeBigInteger(triples[0].bi, writerC1);
            Util.writeBigInteger(triples[0].ci, writerC1);
            Util.writeInt(testNumber, writerC1);

            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            Util.writeBigInteger(mod, writerC2);
            Util.writeBigInteger(triples[1].ai, writerC2);
            Util.writeBigInteger(triples[1].bi, writerC2);
            Util.writeBigInteger(triples[1].ci, writerC2);
            Util.writeInt(testNumber, writerC2);

            Util.readInt(readerC1);
            Util.readInt(readerC2);

            Random random = new Random();
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


            System.out.println();
            long timeC1 = Util.readLong(readerC1);
            long communicationTimeC1 = Util.readLong(readerC1);
            long computingTimeC1 = Util.readLong(readerC1);
            long timeC2 = Util.readLong(readerC2);
            long communicationTimeC2 = Util.readLong(readerC2);
            long computingTimeC2 = Util.readLong(readerC2);
            System.out.println("Time C1: " + timeC1 + " ns");
            System.out.println("Communication Time C1: " + communicationTimeC1 + " ns");
            System.out.println("Computing Time C1: " + computingTimeC1 + " ns");
            System.out.println("Time C2: " + timeC2 + " ns");
            System.out.println("Communication Time C2: " + communicationTimeC2 + " ns");
            System.out.println("Computing Time C2: " + computingTimeC2 + " ns");

            socketC1.close();
            socketC2.close();

            //multiply(PartyID partyID, BigInteger xi, BigInteger yi, MultiplicationTriple triple,
            //            BigInteger mod, BufferedReader reader, PrintWriter writer)

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
