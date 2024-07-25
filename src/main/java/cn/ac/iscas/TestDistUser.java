package cn.ac.iscas;

import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.sknn.SKNNV2;
import cn.ac.iscas.utils.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

import static cn.ac.iscas.secretsharing.AdditiveSecretSharing.*;
import static cn.ac.iscas.secretsharing.AdditiveSecretSharing.parseRandomNumberTupleToJson;
import static cn.ac.iscas.utils.DataProcessor.generateDataset;

public class TestDistUser {
    public static void main(String[] args) {
        int portC1 = 8001;
        int portC2 = 8002;

        int testNumber = 10;
        int dataBitLength = 20;
        int dataNumber = 4096;
        int d = 25;

        int l = 20 * 2 + (int) Util.log2(2) + 2;
        BigInteger mod = BigInteger.probablePrime(l, new Random());

        BigInteger[][] dataset = generateDataset(d, dataNumber, dataBitLength, new Random());
        SKNNV2.Point[][] pointsSecrets = null;
        pointsSecrets = new SKNNV2.Point[2][dataNumber];
        for (int i = 0; i < dataNumber; i++)
        {
            pointsSecrets[0][i] = new SKNNV2.Point(d);
            pointsSecrets[1][i] = new SKNNV2.Point(d);

            BigInteger[] idSecrets = randomSplit(dataset[i][d], mod);
            pointsSecrets[0][i].id = idSecrets[0];
            pointsSecrets[1][i].id = idSecrets[1];

            for (int j = 0; j < d; j++) {
                BigInteger[] pSecrets = randomSplit(dataset[i][j], mod);

                pointsSecrets[0][i].data[j] = pSecrets[0];
                pointsSecrets[1][i].data[j] = pSecrets[1];
            }
        }

        AdditiveSecretSharing.MultiplicationTriple[] triples = generateMultiplicationTriples(mod);

        try (Socket socketC1 = new Socket("192.168.0.2", portC1); Socket socketC2 = new Socket("192.168.0.3", portC2);) {

            PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
            BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

            Util.writeBigInteger(mod, writerC1);
            Util.writeInt(testNumber, writerC1);
            Util.writeInt(dataNumber, writerC1);
            Util.writeInt(d, writerC1);

            Util.writePoints(pointsSecrets[0], writerC1);
            writerC1.println(parseMultiplicationTripleToJson(triples[0]));
            writerC1.flush();

            PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
            BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

            Util.writeBigInteger(mod, writerC2);
            Util.writeInt(testNumber, writerC2);
            Util.writeInt(dataNumber, writerC2);
            Util.writeInt(d, writerC2);

            Util.writePoints(pointsSecrets[1], writerC2);
            writerC2.println(parseMultiplicationTripleToJson(triples[1]));
            writerC2.flush();

            Util.readInt(readerC1);
            Util.readInt(readerC2);

            Random random = new Random();
            for (int i = 0; i < testNumber; i++) {
                BigInteger[] q = new BigInteger[d];
                BigInteger[][] qSecrets = new BigInteger[2][d];
                for (int j = 0; j < d; j++) {
                    q[j] = new BigInteger(dataBitLength, random);

                    BigInteger[] t = randomSplit(q[j], mod);
                    qSecrets[0][j] = t[0];
                    qSecrets[1][j] = t[1];
                }

                Util.writeBigIntegers(qSecrets[0], writerC1);
                Util.writeBigIntegers(qSecrets[1], writerC2);

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
