package cn.ac.iscas;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import cn.ac.iscas.paillier.Paillier;
import cn.ac.iscas.paillier.Paillier.PaillierPrivateKey;
import cn.ac.iscas.paillier.Paillier.PaillierPublicKey;
import cn.ac.iscas.utils.Util;
import de.alsclo.voronoi.Voronoi;
import de.alsclo.voronoi.graph.Graph;
import de.alsclo.voronoi.graph.Point;

public class Test {

    // public static void testModInverse() {
    //     int testNumber = 10000;
    //     int bitLength = 1024;

    //     Paillier paillier = new Paillier(bitLength);
    //     PaillierPublicKey publicKey = paillier.getPaillierPublicKey();

    //     BigInteger g = publicKey.g;
    //     BigInteger n = publicKey.n;
    //     BigInteger nSqure = publicKey.nSqure;

    //     long timeNMinusOneSum = 0l;
    //     long timeInverseSum = 0l;
    //     long timeMultiplySum = 0l;
    //     for (int i = 0; i < testNumber; i++) {
    //         long timePre = System.nanoTime();
    //         g.modPow(n.subtract(BigInteger.ONE), nSqure);
    //         timeNMinusOneSum += System.nanoTime() - timePre;

    //         timePre = System.nanoTime();
    //         g.modInverse(nSqure);
    //         timeInverseSum += System.nanoTime() - timePre;

    //         timePre = System.nanoTime();
    //         n.multiply(n).add(BigInteger.ONE).mod(nSqure);
    //         timeMultiplySum += System.nanoTime() - timePre;
    //     }

    //     System.out.println("g^{n-1} % n^2 时间：" + timeNMinusOneSum / testNumber + " ns");
    //     System.out.println("g^{-1} % n^2 时间：" + timeInverseSum / testNumber + " ns");
    //     System.out.println("n*n % n^2 时间：" + timeMultiplySum / testNumber + " ns");
    // }

    public static void testRandom() {
        int testNumber = 10000;
        Random random = new SecureRandom();

        long timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            long timePre = System.nanoTime();
            new SecureRandom();
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("new SecureRandom() 时间: " + timeSum / testNumber + " ns");

        BigInteger n = new BigInteger(1024, random);
        System.out.println("n 长： " + n.bitLength());
        timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            long timePre = System.nanoTime();
            // Util.getRandomBigInteger(n);
            BigInteger r;
            do {
                r = new BigInteger(n.bitLength(), random);
            } while (r.compareTo(n) >= 0);
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("Util.getRandomBigInteger(n) 时间: " + timeSum / testNumber + " ns");

        timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            long timePre = System.nanoTime();
            new BigInteger(1024, random);
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("new BigInteger(1024, random) 时间: " + timeSum / testNumber + " ns");

        timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            long timePre = System.nanoTime();
            // Util.getRandomBigInteger(n);
            BigInteger r;
            do {
                r = new BigInteger(n.bitLength(), random);
            } while (r.compareTo(n) >= 0);
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("Util.getRandomBigInteger(n) 时间: " + timeSum / testNumber + " ns");

        Paillier paillier = new Paillier(2048);
        PaillierPublicKey publicKey = paillier.getPaillierPublicKey();
        PaillierPrivateKey privateKey = paillier.getPaillierPrivateKey();

        timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            BigInteger x = Util.getRandomBigInteger(privateKey.publicKey.n);
            long timePre = System.nanoTime();
            Paillier.encrypt(publicKey, x);
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("2048bit Paillier.encrypt(publicKey, x); 时间: " + timeSum / testNumber + " ns");

        timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            BigInteger x = Util.getRandomBigInteger(privateKey.publicKey.n);
            long timePre = System.nanoTime();
            Paillier.encrypt(privateKey, x);
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("2048bit Paillier.encrypt(privateKey, x) 时间: " + timeSum / testNumber + " ns");

        timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {
            BigInteger x = Util.getRandomBigInteger(privateKey.publicKey.n);
            BigInteger ex = Paillier.encrypt(privateKey, x);
            long timePre = System.nanoTime();
            Paillier.decrypt(privateKey, ex);
            timeSum += System.nanoTime() - timePre;
        }
        System.out.println("2048bit Paillier.decrypt(privateKey, ex) 时间: " + timeSum / testNumber + " ns");
    }

    public static void testOverflow() {
        System.out.println(Long.MAX_VALUE);
        System.out.println(BigInteger.TWO.pow(63));

        long a = Long.MAX_VALUE;
        long b = a + 1;
        System.out.println(b);
        long c = a / 2;
        System.out.println(c);
        System.out.println(c * c);

        // int dataLength = 2;
        // Random random = new Random();
        // for (int i = 0; i < 10; i++) {
        //     System.out.println(new BigInteger(dataLength, random));
        // }
    }

    // public static void testJson(){
    //     BigInteger[] integers = new BigInteger[10];
    //     for (int i = 0; i < integers.length; i++) {
    //         integers[i] = BigInteger.valueOf(i);
    //     }

    //     String json = JSONArray.
    // }

    public static void testVoronoi() {
        Point p1 = new Point(0.5, 1.5);
        Point p2 = new Point(1.5, 1.5);
        Point p3 = new Point(1.5, 0.5);
        Point p4 = new Point(0.5, 0.5);

        Point c = new Point(1, 1);

        List<Point> pointDatas = Arrays.asList(c, p1, p2, p3, p4);
        Collection<Point> points = pointDatas;
        Voronoi voronoi = new Voronoi(points);
        Graph graph = voronoi.getGraph();
        System.out.println(graph);
    }

    public static void main(String[] args) {
        // testModInverse();
        // testRandom();
        // testOverflow();
        // testJson();

        testVoronoi();
    }
}
