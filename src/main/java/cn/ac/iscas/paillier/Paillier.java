package cn.ac.iscas.paillier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.AbstractMap.SimpleEntry;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import cn.ac.iscas.utils.RunningTimer;
import cn.ac.iscas.utils.Util;

class SplitGroup {
    List<Integer> indexs;
    BigInteger[][] points;

    public SplitGroup() {
        this.indexs = new ArrayList<>();
    }

    public SplitGroup(List<Integer> indexs, BigInteger[][] points) {
        this.indexs = indexs;
        this.points = points;
    }
}

public class Paillier {

    public static final BigInteger BIG_FOUR = BigInteger.TWO.pow(2);
    public static final BigInteger BIG_THREE = BigInteger.ONE.add(BigInteger.TWO);

    public class PaillierPublicKey {
        // The public (encryption) key: (n ,g).
        public BigInteger n;
        // public BigInteger g;

        public BigInteger hs;

        public BigInteger nSqure; // n^2

        // PaillierPublicKey(BigInteger n, BigInteger nSqure, BigInteger g) {
        //     this.n = n;
        //     // this.g = g;

        //     this.nSqure = nSqure;
        // }
        PaillierPublicKey(BigInteger n, BigInteger nSqure, BigInteger hs) {
            this.n = n;
            this.hs = hs;

            this.nSqure = nSqure;
        }
    }

    public class PaillierPrivateKey {
        // n, n^2
        // private BigInteger n;
        // private BigInteger nSqure;
        public PaillierPublicKey publicKey;

        // The private (decryption) key: (lambda, mu).
        public BigInteger lambda;
        public BigInteger mu;
        public BigInteger p;
        public BigInteger q;

        public BigInteger pSqure; // p^2
        public BigInteger qSqure; // q^2

        public BigInteger phiPSqure; // Phi(p^2) = (p-1)^2
        public BigInteger phiQSqure; // Phi(q^2) = (q-1)^2

        public BigInteger pSqureModInverseQSqure; // (p^2)^{-1} mod q^2
        public BigInteger qSqureModInversePSqure; // (q^2)^{-1} mod p^2

        PaillierPrivateKey(PaillierPublicKey publicKey, BigInteger lambda, BigInteger mu, BigInteger p, BigInteger q) {
            this.publicKey = publicKey;
            this.lambda = lambda;
            this.mu = mu;
            this.p = p;
            this.q = q;

            this.pSqure = p.pow(2);
            this.qSqure = q.pow(2);

            this.phiPSqure = p.subtract(BigInteger.ONE).pow(2);
            this.phiQSqure = q.subtract(BigInteger.ONE).pow(2);

            this.pSqureModInverseQSqure = pSqure.modInverse(qSqure);
            this.qSqureModInversePSqure = qSqure.modInverse(pSqure);
        }
    }

    private int length; // length of n
    private PaillierPrivateKey privateKey;
    private PaillierPublicKey publicKey;

    private static Random random = new SecureRandom();

    public static BigInteger a;

    /**
     *
     * 
     *
     * 
     * @param length
     */
    public Paillier(int length) {
        this.length = length;

        keyGenerate();

        a = new BigInteger(length / 2 + 1, new SecureRandom());
    }

    public PaillierPublicKey getPaillierPublicKey() {
        return this.publicKey;
    }

    public PaillierPrivateKey getPaillierPrivateKey() {
        return this.privateKey;
    }

    public static String parsePublicKeyToJson(PaillierPublicKey publicKey) {
        return JSON.toJSONString(publicKey);
    }

    public static PaillierPublicKey parseJsonToPublicKey(String json) {
        return JSON.parseObject(json, PaillierPublicKey.class);
    }

    public static String parsePrivateKeyToJson(PaillierPrivateKey privateKey) {
        return JSON.toJSONString(privateKey);
    }

    public static PaillierPrivateKey parseJsonToPrivateKey(String json) {
        return JSON.parseObject(json, PaillierPrivateKey.class);
    }

    /**
     *
     */
    private void keyGenerate() {
        // BigInteger p = BigInteger.probablePrime(length / 2, random);
        // BigInteger q = BigInteger.probablePrime(length / 2, random);
        BigInteger p, q; // p = q = 3 mod 4, and GCD(p-1, q-1) = 2
        do {
            do {
                p = BigInteger.probablePrime(length / 2, random);
            } while (!p.mod(BIG_FOUR).equals(BIG_THREE));
            do {
                q = BigInteger.probablePrime(length / 2, random);
            } while (!q.mod(BIG_FOUR).equals(BIG_THREE));
        } while (!p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)).equals(BigInteger.TWO));

        BigInteger n = p.multiply(q); // n = p q
        BigInteger nSqure = n.pow(2);

        // BigInteger g = n.add(BigInteger.ONE); // g = n + 1
        BigInteger x; // x <- Z*_n
        do {
            x = Util.getRandomBigInteger(n);
        } while (!x.gcd(n).equals(BigInteger.ONE));
        BigInteger h = x.pow(2).negate(); // h = -x^2
        BigInteger hs = h.modPow(n, nSqure); // hs = h^n mod n^2

        // // lambda = Phi(n) = Phi(p) * Phi(q) = (p-1)(q-1)
        // BigInteger lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        // lambda = Phi(n) = Phi(p) * Phi(q) = (p-1)(q-1) / 2
        BigInteger lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)).divide(BigInteger.TWO);

        // mu = lambda^-1 mod n
        BigInteger mu = lambda.modInverse(n);

        // this.publicKey = new PaillierPublicKey(n, nSqure, g);
        this.publicKey = new PaillierPublicKey(n, nSqure, hs);
        this.privateKey = new PaillierPrivateKey(this.publicKey, lambda, mu, p, q);
    }

    /**
     * 
     * @param publicKey
     * @param m
     * @return
     */

    public static BigInteger encrypt(PaillierPublicKey publicKey, BigInteger m) {
        // 0 <= m < n
        if (m.compareTo(BigInteger.ZERO) < 0 || m.compareTo(publicKey.n) >= 0) {
            return null;
        }

        // BigInteger r;
        // do {
        //     r = BigInteger.probablePrime(publicKey.n.bitLength(), random);
        // } while (r.compareTo(publicKey.n) >= 0 || !r.gcd(publicKey.n).equals(BigInteger.ONE));

        // // c = g^m * r^n mod n^2
        // BigInteger c = publicKey.g.modPow(m, publicKey.nSqure)
        //         .multiply(r.modPow(publicKey.n, publicKey.nSqure))
        //         .mod(publicKey.nSqure);

        // // c = (mn + 1) * r^n mod n^2
        // BigInteger c = m.multiply(publicKey.n).add(BigInteger.ONE)
        //         .multiply(r.modPow(publicKey.n, publicKey.nSqure))
        //         .mod(publicKey.nSqure);

        // c = (mn + 1) * hs^a mod n^2
        BigInteger a = new BigInteger(publicKey.n.bitLength() / 2 + 1, random);
        BigInteger c = m.multiply(publicKey.n).add(BigInteger.ONE)
                .multiply(publicKey.hs.modPow(a, publicKey.nSqure))
                .mod(publicKey.nSqure);

        return c;
    }

    public static BigInteger encrypt(PaillierPrivateKey privateKey, BigInteger m) {
        PaillierPublicKey publicKey = privateKey.publicKey;

        if (m.compareTo(BigInteger.ZERO) < 0 || m.compareTo(publicKey.n) >= 0) {
            return null;
        }

        // c = (mn + 1) * hs^a mod n^2
        BigInteger a = new BigInteger(publicKey.n.bitLength() / 2 + 1, random);
        BigInteger c = m.multiply(publicKey.n).add(BigInteger.ONE)
                .multiply(crtModPow(publicKey.hs, a, privateKey))
                .mod(publicKey.nSqure);

        return c;
    }

    public static BigInteger reEncrypt(PaillierPublicKey publicKey, BigInteger c) {
        BigInteger eZero = encrypt(publicKey, BigInteger.ZERO);
        return homomorphicAddition(c, eZero, publicKey.nSqure);
    }

    public static BigInteger crtModPow(BigInteger a, BigInteger b, PaillierPrivateKey key) {

        BigInteger ap = a.mod(key.pSqure); // a_p = a mod p^2
        BigInteger bp = b.mod(key.phiPSqure); // b_p = b mod Phi(p^2), where Phi(p^2) = (p - 1)^2
        BigInteger xp = ap.modPow(bp, key.pSqure); // x_p = a_p^{b_p}

        BigInteger aq = a.mod(key.qSqure); // a_q = a mod q^2
        BigInteger bq = b.mod(key.phiQSqure); // b_q = b mod Phi(q^2), where Phi(q^2) = (q - 1)^2
        BigInteger xq = aq.modPow(bq, key.qSqure); // x_q = a_q^{b_q}

        BigInteger x = xp.multiply(key.qSqureModInversePSqure).multiply(key.qSqure).mod(key.publicKey.nSqure)
                .add(xq.multiply(key.pSqureModInverseQSqure).multiply(key.pSqure)).mod(key.publicKey.nSqure);

        return x;
    }

    /**
     * 
     * @param privateKey
     * @param c
     * @return
     */
    public static BigInteger decrypt(PaillierPrivateKey privateKey, BigInteger c) {

        // m = L(c^lambda mod n^2) * mu mod n
        // L(x) = (x-1)/n
        BigInteger t = crtModPow(c, privateKey.lambda, privateKey);
        // BigInteger t = c.modPow(privateKey.lambda, privateKey.publicKey.nSqure);
        BigInteger l = t.subtract(BigInteger.ONE).divide(privateKey.publicKey.n);
        BigInteger m = l.multiply(privateKey.mu).mod(privateKey.publicKey.n);

        return m;
    }

    /**
     *
     * 
     * D(C1 * C2 mod n^2) = D(E(m1, r1) * E(m2, r2) mod n^2) = m1 + m2 mod n
     * 
     * @param c1
     * @param c2
     * @param nSqure
     * @return
     */
    public static BigInteger homomorphicAddition(BigInteger c1, BigInteger c2, BigInteger nSqure) {
        return c1.multiply(c2).mod(nSqure);
    }

    /**
     *
     * 
     * D(C * g^m2 mod n^2) = D(E(m1, r1) * g^m2 mod n^2) = m1 + m2 mod n
     * 
     * @param c
     * @param m
     * @param publicKey
     * @return
     */
    public static BigInteger homomorphicAddition(BigInteger c, BigInteger m, PaillierPublicKey publicKey) {
        //  0 <= m < n
        if (m.compareTo(BigInteger.ZERO) < 0 || m.compareTo(publicKey.n) >= 0) {
            return null;
        }

        // BigInteger t = publicKey.g.modPow(m, publicKey.nSqure);
        // BigInteger t = encrypt(publicKey, m);
        BigInteger t = m.multiply(publicKey.n).add(BigInteger.ONE);
        return c.multiply(t).mod(publicKey.nSqure);
    }

    /**
     *
     * 
     * D(C1 * C2^-1 mod n^2) = D(E(m1, r1) * E(m2, r2)^-1 mod n^2) = m1 - m2 mod n
     * 
     * @param c1
     * @param c2
     * @param nSqure
     * @return
     */
    public static BigInteger homomorphicSubstraction(BigInteger c1, BigInteger c2, BigInteger nSqure) {
        BigInteger t = c2.modInverse(nSqure);
        return c1.multiply(t).mod(nSqure);
    }

    /**
     *
     * 
     * D(C * g^-m2 mod n^2) = D(E(m1, r1) * g^-m2 mod n^2) = m1 - m2 mod n
     * 
     * @param c
     * @param m
     * @param publicKey
     * @return
     */
    public static BigInteger homomorphicSubstraction(BigInteger c, BigInteger m, PaillierPublicKey publicKey) {
        // BigInteger t = publicKey.g.modPow(m, publicKey.nSqure);
        // BigInteger t = encrypt(publicKey, m);
        BigInteger t = m.multiply(publicKey.n).add(BigInteger.ONE);
        t = t.modInverse(publicKey.nSqure);
        return c.multiply(t).mod(publicKey.nSqure);
    }

    /**
     * 
     * D(c1^m2 mod n^2) = D(E(m1, r1)^m2 mod n^2) = m1 * m2 mod n
     * 
     * @param c
     * @param m
     * @param nSqure
     * @return
     */
    public static BigInteger homomorphicMultiplication(BigInteger c, BigInteger m, BigInteger nSqure) {
        return c.modPow(m, nSqure);
    }

    /**
     *
     * 
     * @param datas
     * @param sigma
     * @return
     */
    public static BigInteger dataPacking(BigInteger[] datas, int sigma) {
        BigInteger result = datas[0];

        int m = datas.length;
        BigInteger base = BigInteger.TWO.pow(sigma); // 2^\sigma
        BigInteger carry = base;
        for (int i = 1; i < m; i++) {
            result = result.add(datas[i].multiply(carry));
            carry = carry.multiply(base);
        }

        return result;
    }

    /**
     *
     * 
     * @param x
     * @param m
     * @param sigma
     * @return
     */
    public static BigInteger[] dataUnpacking(BigInteger x, int m, int sigma) {
        BigInteger[] datas = new BigInteger[m];

        BigInteger base = BigInteger.TWO.pow(sigma);
        for (int i = 0; i < m; i++) {
            datas[i] = x.mod(base);
            x = x.subtract(datas[i]).divide(base);
        }

        return datas;
    }

    /**
     *
     * 
     * @param datas
     * @param sigma
     * @param publicKey
     * @return
     */
    public static BigInteger dataPacking(BigInteger[] datas, int sigma, PaillierPublicKey publicKey) {
        BigInteger result = datas[0];

        int m = datas.length;
        BigInteger base = BigInteger.TWO.pow(sigma); // 2^\sigma
        BigInteger carry = base;
        for (int i = 1; i < m; i++) {
            BigInteger t = homomorphicMultiplication(datas[i], carry, publicKey.nSqure);
            result = homomorphicAddition(result, t, publicKey.nSqure);
            carry = carry.multiply(base);
        }

        return result;
    }

    /**
     *
     * 
     * @param ex
     * @param m
     * @param sigma
     * @param privateKey
     * @return
     */
    public static BigInteger[] dataUnpacking(BigInteger ex, int m, int sigma, PaillierPrivateKey privateKey) {
        BigInteger x = decrypt(privateKey, ex);

        return dataUnpacking(x, m, sigma);
    }

    /**
     *
     * 
     * Elmehdwi Y, Samanthula B K, Jiang W. Secure k-nearest neighbor query over encrypted data
     * in outsourced environments[C]//2014 IEEE 30th International Conference on Data Engineering. IEEE, 2014: 664-675. 
     * 
     *
     * 
     * @param args
     * @throws IOException
     */
    public static BigInteger secureMultiplicationC1(BigInteger ea, BigInteger eb, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        BigInteger ra = Util.getRandomBigInteger(publicKey.n);
        BigInteger rb = Util.getRandomBigInteger(publicKey.n);
        // System.out.println("ra = " + ra);
        // System.out.println("rb = " + rb);

        BigInteger ear = homomorphicAddition(ea, ra, publicKey); // a' = E(a + ra)
        BigInteger ebr = homomorphicAddition(eb, rb, publicKey); // b' = E(b + rb)

        // send a',b' to P2
        Util.writeBigInteger(ear, writer);
        Util.writeBigInteger(ebr, writer);

        // receive h' from P2
        BigInteger eh = Util.readBigInteger(reader); // h'

        BigInteger t = homomorphicMultiplication(ea, publicKey.n.subtract(rb), publicKey.nSqure);
        BigInteger s = homomorphicAddition(eh, t, publicKey.nSqure);

        t = homomorphicMultiplication(eb, publicKey.n.subtract(ra), publicKey.nSqure);
        s = homomorphicAddition(s, t, publicKey.nSqure);

        t = ra.multiply(rb).mod(publicKey.n); // ra * rb
        t = encrypt(publicKey, t); // E(ra * rb)
        t = homomorphicMultiplication(t, publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure); // E(ra * rb)^{N-1}
        t = homomorphicAddition(s, t, publicKey.nSqure);

        return t;
    }

    public static void secureMultiplicationC2(PaillierPrivateKey privateKey, BufferedReader reader,
            PrintWriter writer) throws IOException {
        BigInteger ear = Util.readBigInteger(reader); // a'
        BigInteger ebr = Util.readBigInteger(reader); // b'

        BigInteger ha = decrypt(privateKey, ear); // ha
        BigInteger hb = decrypt(privateKey, ebr); // hb

        BigInteger h = ha.multiply(hb).mod(privateKey.publicKey.n); // h
        BigInteger eh = encrypt(privateKey, h); // h'

        Util.writeBigInteger(eh, writer);
    }

    public static BigInteger[] secureMultiplicationSC1(BigInteger[] eas, BigInteger[] ebs, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int num = eas.length;
        BigInteger[] ras = new BigInteger[num];
        BigInteger[] rbs = new BigInteger[num];
        BigInteger[] ears = new BigInteger[num];
        BigInteger[] ebrs = new BigInteger[num];

        for (int i = 0; i < num; i++) {
            ras[i] = Util.getRandomBigInteger(publicKey.n);
            rbs[i] = Util.getRandomBigInteger(publicKey.n);

            ears[i] = homomorphicAddition(eas[i], ras[i], publicKey); // a' = E(a + ra)
            ebrs[i] = homomorphicAddition(ebs[i], rbs[i], publicKey); // b' = E(b + rb)
        }

        // send a',b' to P2
        Util.writeBigIntegers(ears, writer);
        Util.writeBigIntegers(ebrs, writer);

        // receive h' from P2
        BigInteger[] ehs = Util.readBigIntegers(num, reader); // h'

        BigInteger[] ts = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            BigInteger t = homomorphicMultiplication(eas[i], publicKey.n.subtract(rbs[i]), publicKey.nSqure);
            BigInteger s = homomorphicAddition(ehs[i], t, publicKey.nSqure);

            t = homomorphicMultiplication(ebs[i], publicKey.n.subtract(ras[i]), publicKey.nSqure);
            s = homomorphicAddition(s, t, publicKey.nSqure);

            t = ras[i].multiply(rbs[i]).mod(publicKey.n); // ra * rb
            t = encrypt(publicKey, t); // E(ra * rb)
            t = homomorphicMultiplication(t, publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure); // E(ra * rb)^{N-1}
            t = homomorphicAddition(s, t, publicKey.nSqure);

            ts[i] = t;
        }

        return ts;
    }

    public static void secureMultiplicationSC2(int num, PaillierPrivateKey privateKey, BufferedReader reader,
            PrintWriter writer) throws IOException {
        BigInteger[] ears = Util.readBigIntegers(num, reader); // a'
        BigInteger[] ebrs = Util.readBigIntegers(num, reader); // b'

        BigInteger[] ehs = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            BigInteger ha = decrypt(privateKey, ears[i]); // ha
            BigInteger hb = decrypt(privateKey, ebrs[i]); // hb

            BigInteger h = ha.multiply(hb).mod(privateKey.publicKey.n); // h
            BigInteger eh = encrypt(privateKey, h); // h'

            ehs[i] = eh;
        }

        Util.writeBigIntegers(ehs, writer);
    }

    /**
     * Enhanced Secure Squared Euclidean Distance (ESSED) protocol
     * 
     * Kim H I, Kim H J, Chang J W. A secure kNN query processing algorithm using homomorphic encryption
     *  on outsourced database[J]. Data & knowledge engineering, 2019, 123: 101602.
     * @throws IOException
     */
    // static BigInteger sigmaR = new BigInteger(40, random);
    public static BigInteger enhancedSecureSquaredEuclideanDistanceC1(BigInteger[] ex, BigInteger[] ey,
            int sigma, PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {
        if (ex.length != ey.length)
            return null;

        int m = ex.length;

        BigInteger[] r = new BigInteger[m];
        // Random random = new SecureRandom();
        for (int i = 0; i < m; i++) {
            r[i] = new BigInteger(sigma, random);
            // r[i] = sigmaR;
        }
        BigInteger rPacked = dataPacking(r, sigma);
        BigInteger eR = encrypt(publicKey, rPacked);

        BigInteger[] exy = new BigInteger[m]; // // E(X - Y)
        // BigInteger negateOne = BigInteger.ONE.negate();
        for (int i = 0; i < m; i++) {
            // BigInteger t = homomorphicMultiplication(ey[i], negateOne, publicKey.nSqure);
            // exy[i] = homomorphicAddition(ex[i], t, publicKey.nSqure);
            exy[i] = homomorphicSubstraction(ex[i], ey[i], publicKey.nSqure);
        }
        BigInteger ev = dataPacking(exy, sigma, publicKey);
        ev = homomorphicAddition(ev, eR, publicKey.nSqure);

        Util.writeBigInteger(ev, writer);

        BigInteger ed = Util.readBigInteger(reader);

        BigInteger eDistance = ed;
        for (int i = 0; i < m; i++) {
            BigInteger ri2 = r[i].pow(2); // ri^2
            BigInteger eri2 = encrypt(publicKey, ri2); // E(ri^2)
            // BigInteger eri2Inv = homomorphicMultiplication(eri2, BigInteger.ONE.negate(), publicKey.nSqure); // E(ri^2)^{-1}

            BigInteger t = BigInteger.TWO.multiply(r[i]).negate(); // -2ri
            t = homomorphicMultiplication(exy[i], t, publicKey.nSqure); // E(xi - yi)^{-2ri}
            t = homomorphicSubstraction(t, eri2, publicKey.nSqure); // E(xi - yi)^{-2ri} * E(ri^2)^{-1}

            eDistance = homomorphicAddition(eDistance, t, publicKey.nSqure);
        }

        return eDistance;
    }

    public static void enhancedSecureSquaredEuclideanDistanceC2(PaillierPrivateKey privateKey, int m, int sigma,
            BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger ev = Util.readBigInteger(reader);

        BigInteger[] w = dataUnpacking(ev, m, sigma, privateKey);

        BigInteger d = BigInteger.ZERO;
        for (int i = 0; i < m; i++) {
            d = d.add(w[i].pow(2));
        }
        BigInteger ed = encrypt(privateKey, d);

        Util.writeBigInteger(ed, writer);
    }

    public static BigInteger[] enhancedSecureSquaredEuclideanDistanceSC1(BigInteger[][] exs, BigInteger[][] eys,
            int sigma, PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        int num = exs.length;
        int m = exs[0].length;

        BigInteger[][] rs = new BigInteger[num][m];
        BigInteger[][] exys = new BigInteger[num][m];
        BigInteger[] evs = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < m; j++) {
                // rs[i][j] = sigmaR;
                rs[i][j] = new BigInteger(sigma, random);

                exys[i][j] = homomorphicSubstraction(exs[i][j], eys[i][j], publicKey.nSqure);
            }

            BigInteger rPacked = dataPacking(rs[i], sigma);
            BigInteger eR = encrypt(publicKey, rPacked);

            BigInteger ev = dataPacking(exys[i], sigma, publicKey);
            ev = homomorphicAddition(ev, eR, publicKey.nSqure);

            evs[i] = ev;
        }

        Util.writeBigIntegers(evs, writer);

        BigInteger[] eds = Util.readBigIntegers(num, reader);

        for (int i = 0; i < eds.length; i++) {
            BigInteger eDistance = eds[i];
            for (int j = 0; j < m; j++) {
                BigInteger ri2 = rs[i][j].pow(2); // ri^2
                BigInteger eri2 = encrypt(publicKey, ri2); // E(ri^2)
                // BigInteger eri2Inv = homomorphicMultiplication(eri2, BigInteger.ONE.negate(), publicKey.nSqure); // E(ri^2)^{-1}

                BigInteger t = BigInteger.TWO.multiply(rs[i][j]).negate(); // -2ri
                t = homomorphicMultiplication(exys[i][j], t, publicKey.nSqure); // E(xi - yi)^{-2ri}
                t = homomorphicSubstraction(t, eri2, publicKey.nSqure); // E(xi - yi)^{-2ri} * E(ri^2)^{-1}

                eDistance = homomorphicAddition(eDistance, t, publicKey.nSqure);
            }

            eds[i] = eDistance;
        }

        return eds;
    }

    public static void enhancedSecureSquaredEuclideanDistanceSC2(PaillierPrivateKey privateKey, int num, int m,
            int sigma,
            BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger[] evs = Util.readBigIntegers(num, reader);

        BigInteger[] eds = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            BigInteger[] w = dataUnpacking(evs[i], m, sigma, privateKey);

            BigInteger d = BigInteger.ZERO;
            for (int j = 0; j < m; j++) {
                d = d.add(w[j].pow(2));
            }
            eds[i] = encrypt(privateKey, d);
        }

        Util.writeBigIntegers(eds, writer);
    }

    /**
     * Secure Bit-Not (SBN) protocol
     * 
     *
     * 
     * Kim H I, Kim H J, Chang J W. A secure kNN query processing algorithm using homomorphic encryption
     *  on outsourced database[J]. Data & knowledge engineering, 2019, 123: 101602.
     * 
     * @param ea
     * @param publicKey
     * @return
     */
    public static BigInteger secureBitNot(BigInteger ea, PaillierPublicKey publicKey) {
        BigInteger t = homomorphicMultiplication(ea, publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure); // E(-a) = E(a)^{N-1}
        return homomorphicAddition(t, encrypt(publicKey, BigInteger.ONE), publicKey.nSqure);
    }

    public static BigInteger[] binaryBitEncrypt(BigInteger x, int dataLength, PaillierPublicKey publicKey) {
        BigInteger[] result = new BigInteger[dataLength];

        String s = x.toString(2);
        int t = dataLength - s.length();
        for (int i = 0; i < t; i++) {
            result[i] = encrypt(publicKey, BigInteger.ZERO);
        }

        for (int i = 0; i < s.length(); i++) {
            result[i + t] = encrypt(publicKey, new BigInteger(s.substring(i, i + 1)));
        }

        return result;
    }

    /**
     * Secure Compare (SCMP) protocol 
     * 
     * Kim H I, Kim H J, Chang J W. A secure kNN query processing algorithm using homomorphic encryption
     * on outsourced database[J]. Data & knowledge engineering, 2019, 123: 101602.
     * 
     * @return
     * @throws IOException
     */
    public static BigInteger secureCompareC1(BigInteger[] eu, BigInteger[] ev, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        if (eu.length != ev.length)
            return null;

        int l = eu.length + 1;
        BigInteger[] teu = Arrays.copyOf(eu, l);
        BigInteger[] tev = Arrays.copyOf(ev, l);

        teu[l - 1] = encrypt(publicKey, BigInteger.ZERO);
        tev[l - 1] = encrypt(publicKey, BigInteger.ONE);

        BigInteger[] euvs = secureMultiplicationSC1(teu, tev, publicKey, reader, writer);

        // Random random = new SecureRandom();
        boolean f = random.nextBoolean();
        BigInteger ehi = encrypt(publicKey, BigInteger.ZERO);
        List<BigInteger> eL = new ArrayList<>();
        for (int i = 0; i < l; i++) {
            // BigInteger euvi = secureMultiplicationC1(teu[i], tev[i], publicKey, reader, writer); // E(u_i * v_i)
            BigInteger euvi = euvs[i];

            BigInteger ewi;
            // BigInteger t = homomorphicMultiplication(euvi, publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure); // E(u_i * v_i)^{N-1}
            if (f) {
                // ewi = homomorphicAddition(teu[i], t, publicKey.nSqure); // E(u_i) * E(u_i * v_i)^{N-1}
                ewi = homomorphicSubstraction(teu[i], euvi, publicKey.nSqure);
            } else {
                // ewi = homomorphicAddition(tev[i], t, publicKey.nSqure); // E(v_i) * E(u_i * v_i)^{N-1}
                ewi = homomorphicSubstraction(tev[i], euvi, publicKey.nSqure);
            }

            BigInteger t = homomorphicMultiplication(euvi, publicKey.n.subtract(BigInteger.TWO), publicKey.nSqure); // E(u_i * v_i)^{N-2}
            t = homomorphicAddition(tev[i], t, publicKey.nSqure); // E(v_i) * E(u_i * v_i)^{N-2}
            BigInteger egi = homomorphicAddition(teu[i], t, publicKey.nSqure); // E(u_i) *  E(v_i) * E(u_i * v_i)^{N-2}

            BigInteger ri = Util.getRandomBigInteger(publicKey.n);
            t = homomorphicMultiplication(ehi, ri, publicKey.nSqure); // E(H_{i-1})^{ri}
            ehi = homomorphicAddition(t, egi, publicKey.nSqure); // E(H_{i-1})^{ri} * E(G_i)

            // t = encrypt(publicKey, BigInteger.ONE.negate().mod(publicKey.n));
            // BigInteger ePhii = homomorphicAddition(t, ehi, publicKey.nSqure); // E(-1) * E(H_i)
            // BigInteger ePhii = homomorphicAddition(ehi, BigInteger.ONE.negate().mod(publicKey.n), publicKey);
            BigInteger ePhii = homomorphicSubstraction(ehi, BigInteger.ONE, publicKey);

            t = homomorphicMultiplication(ePhii, ri, publicKey.nSqure);
            eL.add(homomorphicAddition(ewi, t, publicKey.nSqure));
        }

        // random permutation
        Collections.shuffle(eL, random);

        Util.writeBigIntegers(eL.toArray(new BigInteger[l]), writer);

        BigInteger eAlpha = Util.readBigInteger(reader);
        if (f) {
            eAlpha = secureBitNot(eAlpha, publicKey);
        }

        return eAlpha;
    }

    public static void secureCompareC2(int dataLength, PaillierPrivateKey privateKey, BufferedReader reader,
            PrintWriter writer)
            throws IOException {
        int l = dataLength + 1;

        secureMultiplicationSC2(l, privateKey, reader, writer);
        // for (int i = 0; i < l; i++) {
        //     secureMultiplicationC2(privateKey, reader, writer);
        // }

        BigInteger[] eL = Util.readBigIntegers(l, reader);

        BigInteger alpha = BigInteger.ONE;
        for (int i = 0; i < l; i++) {
            BigInteger li = decrypt(privateKey, eL[i]);
            if (li.equals(BigInteger.ZERO)) {
                alpha = BigInteger.ZERO;
                break;
            }
        }

        BigInteger eAlpha = encrypt(privateKey, alpha);
        Util.writeBigInteger(eAlpha, writer);
    }

    public static BigInteger[] secureCompareSC1(BigInteger[][] eus, BigInteger[][] evs, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int num = eus.length;
        int l = eus[0].length + 1;

        BigInteger[] teus = new BigInteger[num * l];
        BigInteger[] tevs = new BigInteger[num * l];

        for (int i = 0; i < num; i++) {
            System.arraycopy(eus[i], 0, teus, i * l, eus[i].length);
            System.arraycopy(evs[i], 0, tevs, i * l, evs[i].length);

            int index = (i + 1) * l - 1;
            teus[index] = encrypt(publicKey, BigInteger.ZERO);
            tevs[index] = encrypt(publicKey, BigInteger.ONE);
        }

        BigInteger[] euvs = secureMultiplicationSC1(teus, tevs, publicKey, reader, writer);

        BigInteger[][] eLs = new BigInteger[num][];
        boolean[] fs = new boolean[num];
        BigInteger nMinusTwo = publicKey.n.subtract(BigInteger.TWO);
        for (int j = 0; j < num; j++) {
            fs[j] = random.nextBoolean();
            BigInteger ehi = encrypt(publicKey, BigInteger.ZERO);
            List<BigInteger> eL = new ArrayList<>();
            for (int i = 0; i < l; i++) {
                int index = j * l + i;

                BigInteger euvi = euvs[index];
                BigInteger ewi;
                if (fs[j]) {
                    ewi = homomorphicSubstraction(teus[index], euvi, publicKey.nSqure); // E(u_i) * E(u_i * v_i)^{N-1}
                } else {
                    ewi = homomorphicSubstraction(tevs[index], euvi, publicKey.nSqure); // E(v_i) * E(u_i * v_i)^{N-1}
                }

                BigInteger t = homomorphicMultiplication(euvi, nMinusTwo, publicKey.nSqure); // E(u_i * v_i)^{N-2}
                t = homomorphicAddition(tevs[index], t, publicKey.nSqure); // E(v_i) * E(u_i * v_i)^{N-2}
                BigInteger egi = homomorphicAddition(teus[index], t, publicKey.nSqure); // E(u_i) *  E(v_i) * E(u_i * v_i)^{N-2}

                BigInteger ri = Util.getRandomBigInteger(publicKey.n);
                t = homomorphicMultiplication(ehi, ri, publicKey.nSqure); // E(H_{i-1})^{ri}
                ehi = homomorphicAddition(t, egi, publicKey.nSqure); // E(H_{i-1})^{ri} * E(G_i)

                BigInteger ePhii = homomorphicSubstraction(ehi, BigInteger.ONE, publicKey); // E(-1) * E(H_i)

                t = homomorphicMultiplication(ePhii, ri, publicKey.nSqure);
                eL.add(homomorphicAddition(ewi, t, publicKey.nSqure));
            }
            // random permutation
            Collections.shuffle(eL, random);

            eLs[j] = eL.toArray(new BigInteger[l]);
        }

        Util.writeBigIntegers(eLs, writer);

        BigInteger[] eAlphas = Util.readBigIntegers(num, reader);
        for (int i = 0; i < num; i++) {
            if (fs[i]) {
                eAlphas[i] = secureBitNot(eAlphas[i], publicKey);
            }
        }

        return eAlphas;
    }

    public static void secureCompareSC2(int num, int dataLength, PaillierPrivateKey privateKey, BufferedReader reader,
            PrintWriter writer)
            throws IOException {
        int l = dataLength + 1;
        secureMultiplicationSC2(l * num, privateKey, reader, writer);

        BigInteger[][] eLs = Util.readBigIntegers(num, l, reader);

        BigInteger[] eAlphas = new BigInteger[num];
        for (int j = 0; j < num; j++) {
            BigInteger alpha = BigInteger.ONE;
            for (int i = 0; i < l; i++) {
                BigInteger li = decrypt(privateKey, eLs[j][i]);
                if (li.equals(BigInteger.ZERO)) {
                    alpha = BigInteger.ZERO;
                    break;
                }
            }

            eAlphas[j] = encrypt(privateKey, alpha);
        }

        Util.writeBigIntegers(eAlphas, writer);
    }

    public static BigInteger encryptedLSBC1(BigInteger t, PaillierPublicKey publicKey, BufferedReader reader,
            PrintWriter writer) throws IOException {
        BigInteger r = Util.getRandomBigInteger(publicKey.n); // r is random in Z_N
        BigInteger er = encrypt(publicKey, r);

        BigInteger ey = homomorphicAddition(t, er, publicKey.nSqure);

        Util.writeBigInteger(ey, writer);
        BigInteger eAlpha = Util.readBigInteger(reader);

        BigInteger exi;
        if (Util.isEven(r)) { // r is even
            exi = eAlpha;
        } else {
            // BigInteger temp = homomorphicMultiplication(alpha, publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure); // alpha^{N-1}
            BigInteger eOne = encrypt(publicKey, BigInteger.ONE);

            // exi = homomorphicAddition(eOne, temp, publicKey.nSqure);
            exi = homomorphicSubstraction(eOne, eAlpha, publicKey.nSqure);
        }

        return exi;
    }

    public static void encryptedLSBC2(PaillierPrivateKey privateKey, BufferedReader reader, PrintWriter writer)
            throws IOException {
        BigInteger ey = Util.readBigInteger(reader);

        BigInteger y = decrypt(privateKey, ey);
        BigInteger alpha;
        if (Util.isEven(y)) {
            alpha = encrypt(privateKey, BigInteger.ZERO);
        } else {
            alpha = encrypt(privateKey, BigInteger.ONE);
        }

        Util.writeBigInteger(alpha, writer);
    }

    public static BigInteger[] encryptedLSBSC1(BigInteger[] ts, PaillierPublicKey publicKey, BufferedReader reader,
            PrintWriter writer) throws IOException {
        int num = ts.length;
        BigInteger[] eys = new BigInteger[num];
        BigInteger[] rs = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            rs[i] = Util.getRandomBigInteger(publicKey.n); // r is random in Z_N
            eys[i] = homomorphicAddition(ts[i], rs[i], publicKey);
        }

        Util.writeBigIntegers(eys, writer);

        BigInteger[] eAlphas = Util.readBigIntegers(num, reader);

        BigInteger[] exis = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            if (Util.isEven(rs[i])) { // r is even
                exis[i] = eAlphas[i];
            } else {
                // BigInteger temp = homomorphicMultiplication(eAlphas[i], publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure); // alpha^{N-1}
                BigInteger eOne = encrypt(publicKey, BigInteger.ONE);
                exis[i] = homomorphicSubstraction(eOne, eAlphas[i], publicKey.nSqure);
            }
        }

        return exis;
    }

    public static void encryptedLSBSC2(int num, PaillierPrivateKey privateKey, BufferedReader reader,
            PrintWriter writer)
            throws IOException {

        BigInteger[] eys = Util.readBigIntegers(num, reader);

        BigInteger[] eAlphas = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            BigInteger y = decrypt(privateKey, eys[i]);
            if (Util.isEven(y)) {
                eAlphas[i] = encrypt(privateKey, BigInteger.ZERO);
            } else {
                eAlphas[i] = encrypt(privateKey, BigInteger.ONE);
            }
        }
        Util.writeBigIntegers(eAlphas, writer);
    }

    /**
     * Secure Bit-Decomposition (SBD) protocol
     * 
     * Samanthula B K K, Chun H, Jiang W. An efficient and probabilistic secure bit-decomposition[C]
     * //Proceedings of the 8th ACM SIGSAC symposium on Information, computer and communications security. 2013: 541-546.
     * 
     * @param ex
     * @param m
     * @return
     * @throws IOException
     */
    public static BigInteger[] secureBitDecompositionC1(BigInteger ex, int dataLength, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger l = BigInteger.TWO.modInverse(publicKey.n); // l = 2^{-1} mod N

        // BigInteger negateOne = BigInteger.ONE.negate();
        BigInteger[] exBD = new BigInteger[dataLength];
        while (true) {
            BigInteger t = ex;

            for (int i = dataLength - 1; i >= 0; i--) {
                exBD[i] = encryptedLSBC1(t, publicKey, reader, writer);

                // BigInteger temp = homomorphicMultiplication(exBD[i], negateOne, publicKey.nSqure); // E(x_i)^{N-1}
                // BigInteger z = homomorphicAddition(t, temp, publicKey.nSqure); // Z = T * E(x_i)^{N-1}
                BigInteger z = homomorphicSubstraction(t, exBD[i], publicKey.nSqure); // T * E(x_i)^{-1}

                t = homomorphicMultiplication(z, l, publicKey.nSqure);
            }

            break;

            // // SVR
            // BigInteger u = exBD[dataLength - 1];
            // BigInteger carry = BigInteger.TWO;
            // for (int i = dataLength - 2; i >= 0; i--) {
            //     BigInteger temp = homomorphicMultiplication(exBD[i], carry, publicKey.nSqure); // (E(x_i))^{2^i}
            //     carry = carry.multiply(BigInteger.TWO);

            //     u = homomorphicAddition(u, temp, publicKey.nSqure);
            // }

            // // BigInteger temp = homomorphicMultiplication(ex, negateOne, publicKey.nSqure);
            // // BigInteger v = homomorphicAddition(u, temp, publicKey.nSqure);
            // BigInteger v = homomorphicSubstraction(u, ex, publicKey.nSqure);

            // BigInteger r = Util.getRandomBigInteger(publicKey.n);
            // BigInteger w = homomorphicMultiplication(v, r, publicKey.nSqure);

            // Util.writeBigInteger(w, writer);
            // BigInteger gamma = Util.readBigInteger(reader);

            // if (gamma.equals(BigInteger.ONE)) {
            //     Util.writeInt(0, writer);
            //     break;
            // } else {
            //     Util.writeInt(1, writer);
            //     throw new IOException("secureBitDecomposition: not equal!");
            // }
        }

        return exBD;
    }

    public static void secureBitDecompositionC2(int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        while (true) {

            for (int i = 0; i < dataLength; i++) {
                encryptedLSBC2(privateKey, reader, writer);
            }

            break;

            // // SVR
            // BigInteger ew = Util.readBigInteger(reader);
            // BigInteger w = decrypt(privateKey, ew);
            // if (w.equals(BigInteger.ZERO))
            //     Util.writeBigInteger(BigInteger.ONE, writer);
            // else
            //     Util.writeBigInteger(BigInteger.ZERO, writer);

            // int flag = Util.readInt(reader);
            // if (flag == 0) {
            //     break;
            // }
        }
    }

    public static BigInteger[][] secureMultiBitDecompositionC1(BigInteger[] exs, int dataLength,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger l = BigInteger.TWO.modInverse(publicKey.n); // l = 2^{-1} mod N

        // BigInteger negateOne = BigInteger.ONE.negate();
        int num = exs.length;
        BigInteger[][] exsBD = new BigInteger[num][dataLength];

        BigInteger[] etxs = new BigInteger[num];
        System.arraycopy(exs, 0, etxs, 0, num);

        for (int i = dataLength - 1; i >= 0; i--) {
            BigInteger[] etBD = encryptedLSBSC1(etxs, publicKey, reader, writer);

            for (int j = 0; j < num; j++) {
                exsBD[j][i] = etBD[j];

                BigInteger z = homomorphicSubstraction(etxs[j], etBD[j], publicKey.nSqure); // T * E(x_i)^{-1}
                etxs[j] = homomorphicMultiplication(z, l, publicKey.nSqure);
            }
        }

        return exsBD;
    }

    public static void secureMultiBitDecompositionC2(int num, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        for (int i = 0; i < dataLength; i++) {
            encryptedLSBSC2(num, privateKey, reader, writer);
        }
    }

    /**
     * Kim H I, Kim H J, Chang J W. A secure kNN query processing algorithm using homomorphic encryption
     * on outsourced database[J]. Data & knowledge engineering, 2019, 123: 101602.
     * 
     * @return
     * @throws IOException
     */
    public static BigInteger securePointEnclosureC1(BigInteger[][] p, BigInteger[][] lb, BigInteger[][] ub,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {
        if (p.length != lb.length && p.length != ub.length)
            return null;

        int m = p.length;

        BigInteger[][] pArray = new BigInteger[m * 2][];
        BigInteger[][] bArray = new BigInteger[m * 2][];
        System.arraycopy(p, 0, pArray, 0, m);
        System.arraycopy(lb, 0, pArray, m, m);
        System.arraycopy(ub, 0, bArray, 0, m);
        System.arraycopy(p, 0, bArray, m, m);

        BigInteger[] eAlpha2s = secureCompareSC1(pArray, bArray, publicKey, reader, writer);

        BigInteger eAlpha1 = encrypt(publicKey, BigInteger.ONE); // E(alpha) = E(1)
        // BigInteger eAlpha2;
        for (int i = 0; i < m; i++) {
            // BigInteger eAlpha2 = secureCompareC1(p[i], ub[i], publicKey, reader, writer);
            BigInteger eAlpha2 = eAlpha2s[i];
            eAlpha1 = secureMultiplicationC1(eAlpha1, eAlpha2, publicKey, reader, writer);
        }

        for (int i = 0; i < m; i++) {
            // BigInteger eAlpha2 = secureCompareC1(lb[i], p[i], publicKey, reader, writer);
            BigInteger eAlpha2 = eAlpha2s[m + i];
            eAlpha1 = secureMultiplicationC1(eAlpha1, eAlpha2, publicKey, reader, writer);
        }

        return eAlpha1;
    }

    public static void securePointEnclosureC2(int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        secureCompareSC2(m * 2, dataLength, privateKey, reader, writer);

        for (int i = 0; i < m; i++) {
            // secureCompareC2(dataLength, privateKey, reader, writer);
            secureMultiplicationC2(privateKey, reader, writer);
        }

        for (int i = 0; i < m; i++) {
            // secureCompareC2(dataLength, privateKey, reader, writer);
            secureMultiplicationC2(privateKey, reader, writer);
        }
    }

    public static BigInteger[] securePointEnclosureSC1(BigInteger[][][] eps, BigInteger[][][] elbs,
            BigInteger[][][] eubs,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        int num = eps.length;
        int m = eps[0].length;

        BigInteger[][] pArray = new BigInteger[num * m * 2][];
        BigInteger[][] bArray = new BigInteger[num * m * 2][];
        for (int i = 0; i < num; i++) {
            int index = i * m * 2;
            System.arraycopy(eps[i], 0, pArray, index, m);
            System.arraycopy(elbs[i], 0, pArray, index + m, m);

            System.arraycopy(eubs[i], 0, bArray, index, m);
            System.arraycopy(eps[i], 0, bArray, index + m, m);
        }

        BigInteger[] eAlpha2s = secureCompareSC1(pArray, bArray, publicKey, reader, writer);

        BigInteger[] eAlpha1s = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            eAlpha1s[i] = encrypt(publicKey, BigInteger.ONE);
        }

        // BigInteger eAlpha1 = encrypt(publicKey, BigInteger.ONE); // E(alpha) = E(1)
        // BigInteger eAlpha2;
        for (int i = 0; i < m; i++) {

            BigInteger[] ts = new BigInteger[num];
            for (int j = 0; j < num; j++) {
                ts[j] = eAlpha2s[j * m * 2 + i];
            }

            eAlpha1s = secureMultiplicationSC1(eAlpha1s, ts, publicKey, reader, writer);

            // BigInteger eAlpha2 = secureCompareC1(p[i], ub[i], publicKey, reader, writer);
            // BigInteger eAlpha2 = eAlpha2s[i];
            // eAlpha1 = secureMultiplicationC1(eAlpha1, eAlpha2, publicKey, reader, writer);
        }

        for (int i = 0; i < m; i++) {

            BigInteger[] ts = new BigInteger[num];
            for (int j = 0; j < num; j++) {
                ts[j] = eAlpha2s[j * m * 2 + i];
            }

            eAlpha1s = secureMultiplicationSC1(eAlpha1s, ts, publicKey, reader, writer);

            // BigInteger eAlpha2 = secureCompareC1(lb[i], p[i], publicKey, reader, writer);
            // BigInteger eAlpha2 = eAlpha2s[m + i];
            // eAlpha1 = secureMultiplicationC1(eAlpha1, eAlpha2, publicKey, reader, writer);
        }

        return eAlpha1s;
    }

    public static void securePointEnclosureSC2(int num, int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        secureCompareSC2(num * m * 2, dataLength, privateKey, reader, writer);

        for (int i = 0; i < m; i++) {
            // secureCompareC2(dataLength, privateKey, reader, writer);
            // secureMultiplicationC2(privateKey, reader, writer);
            secureMultiplicationSC2(num, privateKey, reader, writer);
        }

        for (int i = 0; i < m; i++) {
            // secureCompareC2(dataLength, privateKey, reader, writer);
            // secureMultiplicationC2(privateKey, reader, writer);
            secureMultiplicationSC2(num, privateKey, reader, writer);
        }
    }

    /**
    * 
    *
    * If q < l, the minimum distance is l - q;
    * If u < q, the minimum distance is q - u;
    * If l <= q <= u, the minimum distance is 0.
    *
    * @return
    */
    public static BigInteger secureMinDistanceFromIntervalToPointC1(int dataLength, BigInteger epi, BigInteger elbi,
            BigInteger eubi, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        // BigInteger[] epiBD = secureBitDecompositionC1(epi, dataLength, publicKey, reader, writer);
        // BigInteger[] elbiBD = secureBitDecompositionC1(elbi, dataLength, publicKey, reader, writer);
        // BigInteger[] eubiBD = secureBitDecompositionC1(eubi, dataLength, publicKey, reader, writer);

        BigInteger[] array = new BigInteger[] { epi, elbi, eubi };
        BigInteger[][] arrayBD = secureMultiBitDecompositionC1(array, dataLength, publicKey, reader, writer);
        BigInteger[] epiBD = arrayBD[0];
        BigInteger[] elbiBD = arrayBD[1];
        BigInteger[] eubiBD = arrayBD[2];

        // BigInteger phi1 = secureCompareC1(epiBD, elbiBD, publicKey, reader, writer); // Bool(p < l)
        // BigInteger phi2 = secureCompareC1(eubiBD, epiBD, publicKey, reader, writer); // Bool(u < p)

        BigInteger[] phis = secureCompareSC1(new BigInteger[][] { epiBD, eubiBD }, new BigInteger[][] { elbiBD, epiBD },
                publicKey, reader, writer);
        BigInteger phi1 = phis[0];
        BigInteger phi2 = phis[1];

        // BigInteger temp = homomorphicMultiplication(epi, BigInteger.ONE.negate(), publicKey.nSqure); // pi^{-1}
        // temp = homomorphicAddition(elbi, temp, publicKey.nSqure); // li * pi^{-1}
        BigInteger temp = homomorphicSubstraction(elbi, epi, publicKey.nSqure);
        BigInteger t1 = secureMultiplicationC1(phi1, temp, publicKey, reader, writer);

        // temp = homomorphicMultiplication(eubi, BigInteger.ONE.negate(), publicKey.nSqure); // ubi^{-1}
        // temp = homomorphicAddition(epi, temp, publicKey.nSqure); // pi * ubi^{-1}
        temp = homomorphicSubstraction(epi, eubi, publicKey.nSqure);
        BigInteger t2 = secureMultiplicationC1(phi2, temp, publicKey, reader, writer);

        return homomorphicAddition(t1, t2, publicKey.nSqure);
    }

    public static void secureMinDistanceFromIntervalToPointC2(int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        // secureBitDecompositionC2(dataLength, privateKey, reader, writer);
        // secureBitDecompositionC2(dataLength, privateKey, reader, writer);
        // secureBitDecompositionC2(dataLength, privateKey, reader, writer);

        secureMultiBitDecompositionC2(3, dataLength, privateKey, reader, writer);

        // secureCompareC2(dataLength, privateKey, reader, writer);
        // secureCompareC2(dataLength, privateKey, reader, writer);
        secureCompareSC2(2, dataLength, privateKey, reader, writer);

        secureMultiplicationC2(privateKey, reader, writer);
        secureMultiplicationC2(privateKey, reader, writer);
    }

    public static BigInteger[] secureMinDistanceFromIntervalToPointSC1(int dataLength, BigInteger[] epis,
            BigInteger[] elbis,
            BigInteger[] eubis, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int num = epis.length;

        BigInteger[] array = new BigInteger[num * 3];
        System.arraycopy(epis, 0, array, 0, num);
        System.arraycopy(elbis, 0, array, num, num);
        System.arraycopy(eubis, 0, array, num * 2, num);

        BigInteger[][] arrayBD = secureMultiBitDecompositionC1(array, dataLength, publicKey, reader, writer);

        BigInteger[][] cArray1 = new BigInteger[num * 2][];
        System.arraycopy(arrayBD, 0, cArray1, 0, num);
        System.arraycopy(arrayBD, num * 2, cArray1, num, num);

        BigInteger[][] cArray2 = new BigInteger[num * 2][];
        System.arraycopy(arrayBD, num, cArray2, 0, num);
        System.arraycopy(arrayBD, 0, cArray2, num, num);

        BigInteger[] phis = secureCompareSC1(cArray1, cArray2, publicKey, reader, writer);

        BigInteger[] eplus = new BigInteger[num * 2]; // li * pi^{-1} and pi * ubi^{-1}
        for (int i = 0; i < num; i++) {
            eplus[i] = homomorphicSubstraction(elbis[i], epis[i], publicKey.nSqure); // li * pi^{-1}
            eplus[i + num] = homomorphicSubstraction(epis[i], eubis[i], publicKey.nSqure); // pi * ubi^{-1}
        }

        BigInteger[] cmps = secureMultiplicationSC1(phis, eplus, publicKey, reader, writer);

        BigInteger[] ts = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            ts[i] = homomorphicAddition(cmps[i], cmps[i + num], publicKey.nSqure);
        }

        return ts;
    }

    public static void secureMinDistanceFromIntervalToPointSC2(int num, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        secureMultiBitDecompositionC2(num * 3, dataLength, privateKey, reader, writer);

        secureCompareSC2(num * 2, dataLength, privateKey, reader, writer);

        secureMultiplicationSC2(num * 2, privateKey, reader, writer);
    }

    /**
    *
    * @return
    */
    public static BigInteger secureMinDistanceFromBoxToPointC1(int dataLength, BigInteger[] ep, BigInteger[] elb,
            BigInteger[] eub, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int m = ep.length;
        // BigInteger[] eds = new BigInteger[m];

        // for (int i = 0; i < m; i++) {
        //     // <MD([l_i, u_i], q_i)>
        //     // BigInteger edi = secureMinDistanceFromIntervalToPointC1(dataLength, ep[i], elb[i], eub[i], publicKey,
        //     //         reader, writer);
        //     eds[i] = secureMinDistanceFromIntervalToPointC1(dataLength, ep[i], elb[i], eub[i], publicKey,
        //             reader, writer);

        //     // // <{MD([l_i, u_i], q_i)}^p>
        //     // edi = secureMultiplicationC1(edi, edi, publicKey, reader, writer);
        //     // result = homomorphicAddition(edi, result, publicKey.nSqure);
        // }
        BigInteger[] eds = secureMinDistanceFromIntervalToPointSC1(dataLength, ep, elb, eub, publicKey, reader, writer);

        BigInteger[] edSqures = secureMultiplicationSC1(eds, eds, publicKey, reader, writer);

        BigInteger result = encrypt(publicKey, BigInteger.ZERO);
        for (int i = 0; i < m; i++) {
            result = homomorphicAddition(edSqures[i], result, publicKey.nSqure);
        }

        return result;
    }

    public static void secureMinDistanceFromBoxToPointC2(int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        // for (int i = 0; i < m; i++) {
        //     secureMinDistanceFromIntervalToPointC2(dataLength, privateKey, reader, writer);

        //     // secureMultiplicationC2(privateKey, reader, writer);
        // }
        secureMinDistanceFromIntervalToPointSC2(m, dataLength, privateKey, reader, writer);

        secureMultiplicationSC2(m, privateKey, reader, writer);
    }

    public static BigInteger[] secureMinDistanceFromBoxToPointSC1(int dataLength, BigInteger[][] eps,
            BigInteger[][] elbs, BigInteger[][] eubs, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int num = eps.length;
        int m = eps[0].length;

        BigInteger[] epsArray = new BigInteger[num * m];
        BigInteger[] elbsArray = new BigInteger[num * m];
        BigInteger[] eubsArray = new BigInteger[num * m];
        for (int i = 0; i < num; i++) {
            System.arraycopy(eps[i], 0, epsArray, i * m, m);
            System.arraycopy(elbs[i], 0, elbsArray, i * m, m);
            System.arraycopy(eubs[i], 0, eubsArray, i * m, m);
        }

        BigInteger[] eds = secureMinDistanceFromIntervalToPointSC1(dataLength, epsArray, elbsArray, eubsArray,
                publicKey, reader, writer);

        BigInteger[] edSqures = secureMultiplicationSC1(eds, eds, publicKey, reader, writer);

        BigInteger[] result = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            int index = i * num;
            BigInteger t = encrypt(publicKey, BigInteger.ZERO);
            for (int j = 0; j < m; j++) {
                t = homomorphicAddition(edSqures[index + j], t, publicKey.nSqure);
            }

            result[i] = t;
        }

        return result;
    }

    public static void secureMinDistanceFromBoxToPointSC2(int num, int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        secureMinDistanceFromIntervalToPointSC2(num * m, dataLength, privateKey, reader, writer);

        secureMultiplicationSC2(num * m, privateKey, reader, writer);
    }

    /**
     *
     * E(p_1), E(p_2), ..., E(p_n)
     * E(d_1), E(d_2), ..., E(d_n)
     *
     * 
     * @return
     * @throws IOException
     */
    public static BigInteger[] secureMinDistancePointC1(BigInteger[][] epList, BigInteger[] edList, int dataLength,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        // RunningTimer timer = new RunningTimer(RunningTimer.TimeType.MS);
        // System.out.println("Start:" + timer.cut() + " ms");

        int num = edList.length;
        int disBitLength = 2 * dataLength;

        BigInteger[][] edBDList = secureMultiBitDecompositionC1(edList, disBitLength, publicKey, reader, writer);
        // System.out.println("edBDList SBD: " + timer.cut() + " ms");

        BigInteger edMin = edList[0];
        for (int i = 1; i < num; i++) {
            BigInteger[] edMinBD = secureBitDecompositionC1(edMin, disBitLength, publicKey, reader, writer);
            // BigInteger[] ediBD = secureBitDecompositionC1(edList[i], disBitLength, publicKey, reader, writer);

            BigInteger eAlpha = secureCompareC1(edMinBD, edBDList[i], publicKey, reader, writer); // alpha = bool(d_min, d_i)

            // BigInteger t1 = secureMultiplicationC1(eAlpha, edMin, publicKey, reader, writer); // alpha * d_min

            BigInteger t2 = homomorphicMultiplication(eAlpha, BigInteger.ONE.negate(), publicKey.nSqure); // -alpha
            t2 = homomorphicAddition(t2, BigInteger.ONE, publicKey); // -alpha + 1

            // t2 = secureMultiplicationC1(t2, edList[i], publicKey, reader, writer); // (1-alpha) * d_i

            BigInteger[] ts = secureMultiplicationSC1(new BigInteger[] { eAlpha, t2 },
                    new BigInteger[] { edMin, edList[i] }, publicKey, reader, writer);

            // edMin = homomorphicAddition(t1, t2, publicKey.nSqure); // d_min = alpha * d_min + (1-alpha) * d_i
            edMin = homomorphicAddition(ts[0], ts[1], publicKey.nSqure);
        }
        // System.out.println("edMin: " + timer.cut() + " ms");

        BigInteger[] etdList = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            BigInteger t = homomorphicSubstraction(edMin, edList[i], publicKey.nSqure); // d_min - d_i

            BigInteger r = Util.getRandomBigInteger(publicKey.n);

            etdList[i] = homomorphicMultiplication(t, r, publicKey.nSqure); //  r * (d_min - d_i)
        }
        // System.out.println("etdList: " + timer.cut() + " ms");

        Util.writeBigIntegers(etdList, writer);
        BigInteger[] eBetaList = Util.readBigIntegers(num, reader);

        // System.out.println("eBetaList: " + timer.cut() + " ms");

        int m = epList[0].length;

        BigInteger[] epArray = new BigInteger[m * num];
        BigInteger[] eBetasTemp = new BigInteger[m * num];
        int index = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < num; j++) {
                epArray[index++] = epList[j][i];
            }

            System.arraycopy(eBetaList, 0, eBetasTemp, i * num, num);
        }

        BigInteger[] ts = secureMultiplicationSC1(epArray, eBetasTemp, publicKey, reader, writer);

        BigInteger[] epMin = new BigInteger[m];
        index = 0;
        for (int i = 0; i < m; i++) {
            epMin[i] = encrypt(publicKey, BigInteger.ZERO);

            for (int j = 0; j < num; j++) {
                // BigInteger t = secureMultiplicationC1(eBetaList[j], epList[j][i], publicKey, reader, writer); // bete_j * p_j_i
                BigInteger t = ts[index++];
                epMin[i] = homomorphicAddition(epMin[i], t, publicKey.nSqure);
            }
        }
        // System.out.println("epMin: " + timer.cut() + " ms");

        return epMin;
    }

    public static void secureMinDistancePointC2(int num, int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        int disBitLength = dataLength * 2;

        secureMultiBitDecompositionC2(num, disBitLength, privateKey, reader, writer);

        for (int i = 1; i < num; i++) {
            secureBitDecompositionC2(disBitLength, privateKey, reader, writer);
            // secureBitDecompositionC2(disBitLength, privateKey, reader, writer);
            secureCompareC2(disBitLength, privateKey, reader, writer);

            // secureMultiplicationC2(privateKey, reader, writer);
            // secureMultiplicationC2(privateKey, reader, writer);
            secureMultiplicationSC2(2, privateKey, reader, writer);
        }

        BigInteger[] etdList = Util.readBigIntegers(num, reader);

        BigInteger[] eBetaList = new BigInteger[num];
        boolean minFound = false;
        for (int i = 0; i < num; i++) {
            if (minFound) {
                eBetaList[i] = encrypt(privateKey.publicKey, BigInteger.ZERO);
                continue;
            }

            BigInteger t = decrypt(privateKey, etdList[i]);
            if (t.equals(BigInteger.ZERO)) {
                eBetaList[i] = encrypt(privateKey.publicKey, BigInteger.ONE);
                minFound = true;
            } else {
                eBetaList[i] = encrypt(privateKey.publicKey, BigInteger.ZERO);
            }
        }

        Util.writeBigIntegers(eBetaList, writer);

        secureMultiplicationSC2(num * m, privateKey, reader, writer);
        // for (int i = 0; i < m; i++) {
        //     for (int j = 0; j < num; j++) {
        //         secureMultiplicationC2(privateKey, reader, writer);
        //     }
        // }
    }

    /**
     *
     * 
     * @param epList
     * @param edList
     * @param dataLength
     * @param publicKey
     * @param reader
     * @param writer
     * @throws IOException
     */
    public static BigInteger[][] secureMinDistanceKPointC1(int k, BigInteger[][] epList, BigInteger[] edList,
            int dataLength,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        // RunningTimer timer = new RunningTimer(RunningTimer.TimeType.MS);
        // System.out.println("Start: " + timer.cut() + " ms");
        BigInteger maxVal = BigInteger.TWO.pow(dataLength * 2).subtract(BigInteger.ONE);

        int num = epList.length;
        int m = epList[0].length;
        BigInteger[][] result = new BigInteger[k][m];
        for (int i = 0; i < k; i++) {
            result[i] = secureMinDistancePointC1(epList, edList, dataLength + 1, publicKey, reader, writer); 
            // System.out.println("secureMinDistancePointC1: " + timer.cut() + " ms");

            if (i == k - 1)
                break;

            BigInteger[][] tepList = new BigInteger[num][m];
            for (int j = 0; j < num; j++) {
                for (int l = 0; l < m; l++) {
                    tepList[j][l] = homomorphicSubstraction(epList[j][l], result[i][l], publicKey.nSqure);

                    BigInteger r = Util.getRandomBigInteger(publicKey.n);
                    tepList[j][l] = homomorphicMultiplication(tepList[j][l], r, publicKey.nSqure);
                }
            }

            Util.writeBigIntegers(tepList, writer);

            BigInteger[] eAlphas = Util.readBigIntegers(num, reader);
            // System.out.println("trun to C2: " + timer.cut() + " ms");

            for (int j = 0; j < num; j++) {
                BigInteger t = homomorphicMultiplication(eAlphas[j], maxVal, publicKey.nSqure); // E(alpha * MAX)
                edList[j] = homomorphicAddition(edList[j], t, publicKey.nSqure); // E(d) =  E(d + alpha * MAX)
            }
            // System.out.println("end: " + timer.cut() + " ms");
        }

        return result;
    }

    public static void secureMinDistanceKPointC2(int k, int num, int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        for (int i = 0; i < k; i++) {
            secureMinDistancePointC2(num, m, dataLength + 1, privateKey, reader, writer);

            if (i == k - 1)
                break;

            BigInteger[][] epList = Util.readBigIntegers(num, m, reader);

            BigInteger[] eAlphas = new BigInteger[num];
            for (int j = 0; j < num; j++) {
                boolean isZero = true;
                for (int l = 0; l < m; l++) {
                    BigInteger t = decrypt(privateKey, epList[j][l]);
                    if (!t.equals(BigInteger.ZERO)) {
                        isZero = false;
                        break;
                    }
                }

                if (isZero)
                    eAlphas[j] = encrypt(privateKey, BigInteger.ONE);
                else
                    eAlphas[j] = encrypt(privateKey, BigInteger.ZERO);
            }

            Util.writeBigIntegers(eAlphas, writer);
        }
    }

    /**
    * 
    */
    public static BigInteger secureMinKthC1(int k, BigInteger[] exs, int dataLength,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        BigInteger maxVal = BigInteger.TWO.pow(dataLength);

        int num = exs.length;
        BigInteger[][] epList = new BigInteger[num][1];
        for (int i = 0; i < num; i++) {
            epList[i][0] = exs[i];
        }

        BigInteger[] result = new BigInteger[1];
        for (int i = 0; i < k; i++) {
            result = secureMinDistancePointC1(epList, exs, dataLength + 1, publicKey, reader, writer); 

            if (i == k - 1)
                break;

            BigInteger[][] tepList = new BigInteger[num][1];
            for (int j = 0; j < num; j++) {
                tepList[j][0] = homomorphicSubstraction(epList[j][0], result[0], publicKey.nSqure);
                BigInteger r = Util.getRandomBigInteger(publicKey.n);
                tepList[j][0] = homomorphicMultiplication(tepList[j][0], r, publicKey.nSqure);
            }

            Util.writeBigIntegers(tepList, writer);

            BigInteger[] eAlphas = Util.readBigIntegers(num, reader);

            for (int j = 0; j < num; j++) {
                BigInteger t = homomorphicMultiplication(eAlphas[j], maxVal, publicKey.nSqure); // E(alpha * MAX)
                exs[j] = homomorphicAddition(exs[j], t, publicKey.nSqure); // E(d) =  E(d + alpha * MAX)
            }
        }

        return result[0];
    }

    public static void secureMinKthC2(int k, int num, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        for (int i = 0; i < k; i++) {
            secureMinDistancePointC2(num, 1, dataLength + 1, privateKey, reader, writer);

            if (i == k - 1)
                break;

            BigInteger[][] epList = Util.readBigIntegers(num, 1, reader);

            BigInteger[] eAlphas = new BigInteger[num];
            for (int j = 0; j < num; j++) {
                boolean isZero = true;

                BigInteger t = decrypt(privateKey, epList[j][0]);
                if (!t.equals(BigInteger.ZERO)) {
                    isZero = false;
                }

                if (isZero)
                    eAlphas[j] = encrypt(privateKey, BigInteger.ONE);
                else
                    eAlphas[j] = encrypt(privateKey, BigInteger.ZERO);
            }

            Util.writeBigIntegers(eAlphas, writer);
        }
    }

    /**
     * secure read
     * 
     * @param eBuckets
     * @param eAlphas
     * @param publicKey
     * @param reader
     * @param writer
     * @return
     * @throws IOException
     */
    public static BigInteger[][] secureReadC1(BigInteger[][][] eBuckets, BigInteger[] eAlphas,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        int bNum = eBuckets.length;
        int bSize = eBuckets[0].length;
        int m = eBuckets[0][0].length;

        BigInteger[][][] teBuckets = new BigInteger[bNum][bSize][m];
        BigInteger[][][] r = new BigInteger[bNum][bSize][m];
        for (int i = 0; i < bNum; i++) {
            for (int j = 0; j < bSize; j++) {
                for (int k = 0; k < m; k++) {
                    r[i][j][k] = Util.getRandomBigInteger(publicKey.n);
                    // r[i][j][k] = BigInteger.ZERO;
                    teBuckets[i][j][k] = homomorphicAddition(eBuckets[i][j][k], r[i][j][k], publicKey);
                }
            }
        }

        Util.writeBigIntegers(teBuckets, writer);
        Util.writeBigIntegers(eAlphas, writer);

        int gNum = Util.readInt(reader);
        if (gNum == 0) {
            return null;
        }
        SplitGroup[] groups = new SplitGroup[gNum];
        for (int i = 0; i < gNum; i++) {
            List<Integer> indexs = JSONArray.parseArray(reader.readLine(), Integer.class);
            BigInteger[][] gPoints = Util.readBigIntegers(bSize, m, reader);
            groups[i] = new SplitGroup(indexs, gPoints);
        }

        BigInteger[][] result = new BigInteger[gNum * bSize][m];
        int count = 0;
        for (SplitGroup group : groups) {
            List<Integer> indexs = group.indexs;
            BigInteger[][] eBucket = group.points;

            for (int i = 0; i < bSize; i++) {
                for (int j = 0; j < m; j++) {
                    BigInteger arSum = encrypt(publicKey, BigInteger.ZERO);
                    for (int bIndex : indexs) { // SUM(alpha * r)
                        BigInteger t = homomorphicMultiplication(eAlphas[bIndex], r[bIndex][i][j], publicKey.nSqure);

                        arSum = homomorphicAddition(arSum, t, publicKey.nSqure);
                    }

                    result[count][j] = homomorphicSubstraction(eBucket[i][j], arSum, publicKey.nSqure); // p+r - SUM(alpha * r)
                }
                count++;
            }
        }

        return result;
    }

    public static void secureReadC2(int bNum, int bSize, int m, PaillierPrivateKey privateKey, BufferedReader reader,
            PrintWriter writer) throws IOException {
        BigInteger[][][] teBuckets = Util.readBigIntegers(bNum, bSize, m, reader);
        BigInteger[] eAlphas = Util.readBigIntegers(bNum, reader);

        PaillierPublicKey publicKey = privateKey.publicKey;

        List<SplitGroup> groups = new ArrayList<>();
        boolean[] isUsed = new boolean[bNum];
        Arrays.fill(isUsed, false);
        for (int i = 0; i < bNum; i++) {
            BigInteger alphai = decrypt(privateKey, eAlphas[i]);
            if (alphai.equals(BigInteger.ONE)) {
                List<Integer> indexs = new ArrayList<>();
                indexs.add(Integer.valueOf(i));

                BigInteger[][] eBucket = new BigInteger[bSize][m];
                for (int j = 0; j < bSize; j++) {
                    for (int k = 0; k < m; k++) {
                        eBucket[j][k] = reEncrypt(publicKey, teBuckets[i][j][k]); 
                    }
                }

                SplitGroup group = new SplitGroup(indexs, eBucket);
                isUsed[i] = true;

                groups.add(group);
            }
        }

        Util.writeInt(groups.size(), writer);
        if (groups.size() == 0) {
            return;
        }

        int avgNum = bNum / groups.size();
        int remainder = bNum % groups.size();
        int index = 0;
        for (int i = 0; i < bNum; i++) {
            if (isUsed[i])
                continue;

            if (groups.get(index).indexs.size() > avgNum
                    || (groups.get(index).indexs.size() == avgNum && remainder <= 0)) {
                index++;
                remainder--;
            }
            groups.get(index).indexs.add(Integer.valueOf(i));
            isUsed[i] = true;
        }

        for (SplitGroup group : groups) {
            Collections.shuffle(group.indexs, random);
        }

        for (SplitGroup group : groups) {
            JSONArray indexsJson = new JSONArray(group.indexs);
            writer.println(indexsJson.toJSONString());

            Util.writeBigIntegers(group.points, writer);
        }
        writer.flush();
    }

    /**
     * ebs1[i] XOR ebs2[i]
     * 
     * E(b1 XOR b2) = E(b1 + b2 - 2 * b1 * b2)
     * 
     * @param ebs1
     * @param ebs2
     * @param publicKey
     * @param reader
     * @param writer
     * @throws IOException
     */
    public static BigInteger[] secureBitsXORC1(BigInteger[] ebs1, BigInteger[] ebs2, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        int num = ebs1.length;
        BigInteger[] ebs = new BigInteger[num];

        BigInteger[] ts = secureMultiplicationSC1(ebs1, ebs2, publicKey, reader, writer);
        for (int i = 0; i < num; i++) {
            // BigInteger t = secureMultiplicationC1(ebs1[i], ebs2[i], publicKey, reader, writer); // E(b1 * b2)
            BigInteger t = ts[i];
            t = homomorphicMultiplication(t, BigInteger.TWO, publicKey.nSqure); // E(2 * b1 * b2)

            t = homomorphicSubstraction(ebs1[i], t, publicKey.nSqure); // E(b1 - 2 * b1 * b2)
            t = homomorphicAddition(ebs2[i], t, publicKey.nSqure); // E(b1 + b2 - 2 * b1 * b2)

            ebs[i] = t;
        }

        return ebs;
    }

    public static void secureBitsXORC2(int num, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {
        // for (int i = 0; i < num; i++) {
        //     secureMultiplicationC2(privateKey, reader, writer); // E(b1 * b2)
        // }
        secureMultiplicationSC2(num, privateKey, reader, writer);
    }

    /**
     * 
     * @param k
     * @param exs
     * @param reader
     * @param writer
     * @return
     * @throws IOException
     */
    public static BigInteger[] mySecureMINKC1(int k, BigInteger[] exs, int dataLength, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        RunningTimer timer = new RunningTimer(RunningTimer.TimeType.MS);
        System.out.println("Start: " + timer.cut() + " ms");
        int num = exs.length;

        BigInteger[][] exsBD = secureMultiBitDecompositionC1(exs, dataLength, publicKey, reader, writer);
        System.out.println("secureMultiBitDecompositionC1: " + timer.cut() + " ms");

        int size = num * (num - 1) / 2;
        BigInteger[][] exArray1BD = new BigInteger[size][];
        BigInteger[][] exArray2BD = new BigInteger[size][];
        int index = 0;
        for (int i = 0; i < num - 1; i++) {
            int length = num - 1 - i;
            Arrays.fill(exArray1BD, index, index + length, exsBD[i]);
            System.arraycopy(exsBD, i + 1, exArray2BD, index, length);

            index += length;
        }

        BigInteger[] eAlphas = secureCompareSC1(exArray1BD, exArray2BD, publicKey, reader, writer);
        System.out.println("secureCompareSC1: " + timer.cut() + " ms");

        BigInteger[] eOneMinusAlphas = new BigInteger[eAlphas.length];
        BigInteger eOne = encrypt(publicKey, BigInteger.ONE);
        for (int i = 0; i < eAlphas.length; i++) {
            eOneMinusAlphas[i] = homomorphicSubstraction(eOne, eAlphas[i], publicKey.nSqure);
        }

        BigInteger[] eSums = new BigInteger[num];
        int offset = 0;
        for (int i = 0; i < num; i++) {
            BigInteger eSum = encrypt(publicKey, BigInteger.ZERO);

            index = i - 1;
            for (int j = 0; j < num - 1; j++) {
                if (j < i) {
                    eSum = homomorphicAddition(eSum, eOneMinusAlphas[index], publicKey.nSqure);
                    index += num - 2 - j;
                } else {
                    index = offset + j - i;
                    eSum = homomorphicAddition(eSum, eAlphas[index], publicKey.nSqure);
                }
            }

            eSums[i] = eSum;
            offset += num - 1 - i;
        }
        System.out.println("Sum: " + timer.cut() + " ms");

        Util.writeBigIntegers(eSums, writer);
        BigInteger[] eBetas = Util.readBigIntegers(k * num, reader);
        System.out.println("Waiting for C2: " + timer.cut() + " ms");

        BigInteger[] ePoints = new BigInteger[k * num];
        for (int i = 0; i < k; i++) {
            System.arraycopy(exs, 0, ePoints, i * num, num);
        }
        System.out.println("ePoints: " + timer.cut() + " ms");

        BigInteger[] ets = secureMultiplicationSC1(eBetas, ePoints, publicKey, reader, writer);
        System.out.println("secureMultiplicationSC1: " + timer.cut() + " ms");

        BigInteger[] result = new BigInteger[k];
        for (int i = 0; i < k; i++) {
            BigInteger eSum = encrypt(publicKey, BigInteger.ZERO);

            offset = i * num;
            for (int j = 0; j < num; j++) {
                eSum = homomorphicAddition(eSum, ets[offset + j], publicKey.nSqure);
            }

            result[i] = eSum;
        }
        System.out.println("result: " + timer.cut() + " ms");

        return result;
    }

    public static void mySecureMINKC2(int k, int num, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        secureMultiBitDecompositionC2(num, dataLength, privateKey, reader, writer);

        secureCompareSC2(num * (num - 1) / 2, dataLength, privateKey, reader, writer);

        BigInteger[] eSums = Util.readBigIntegers(num, reader);

        // <index, rank>
        Queue<SimpleEntry<Integer, BigInteger>> queue = new PriorityQueue<>((e1, e2) -> {
            return e1.getValue().compareTo(e2.getValue());
        });
        BigInteger bigNum = BigInteger.valueOf(num);
        for (int i = 0; i < num; i++) {
            BigInteger t = decrypt(privateKey, eSums[i]);

            queue.add(new SimpleEntry<Integer, BigInteger>(Integer.valueOf(i), bigNum.subtract(t)));
        }

        BigInteger[] eBetas = new BigInteger[k * num];
        for (int i = 0; i < k; i++) {
            int index = queue.poll().getKey().intValue();

            int offset = i * num;
            for (int j = 0; j < num; j++) {
                if (j == index) {
                    eBetas[offset + j] = encrypt(privateKey, BigInteger.ONE);
                } else {
                    eBetas[offset + j] = encrypt(privateKey, BigInteger.ZERO);
                }
            }
        }

        Util.writeBigIntegers(eBetas, writer);

        secureMultiplicationSC2(k * num, privateKey, reader, writer);
    }

    /**
     * 
     */
    public static BigInteger[][] mySecureMinDistanceKPointC1(int k, BigInteger[][] epList, BigInteger[] edList,
            int dataLength, PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        int disBitLength = dataLength * 2 + 1;

        RunningTimer timer = new RunningTimer(RunningTimer.TimeType.MS);
        System.out.println("Start: " + timer.cut() + " ms");
        int num = edList.length;
        int m = epList[0].length;

        BigInteger[][] exsBD = secureMultiBitDecompositionC1(edList, disBitLength, publicKey, reader, writer);
        System.out.println("secureMultiBitDecompositionC1: " + timer.cut() + " ms");

        int size = num * (num - 1) / 2;
        BigInteger[][] exArray1BD = new BigInteger[size][];
        BigInteger[][] exArray2BD = new BigInteger[size][];
        int index = 0;
        for (int i = 0; i < num - 1; i++) {
            int length = num - 1 - i;
            Arrays.fill(exArray1BD, index, index + length, exsBD[i]);
            System.arraycopy(exsBD, i + 1, exArray2BD, index, length);

            index += length;
        }

        BigInteger[] eAlphas = secureCompareSC1(exArray1BD, exArray2BD, publicKey, reader, writer);
        System.out.println("secureCompareSC1: " + timer.cut() + " ms");

        BigInteger[] eOneMinusAlphas = new BigInteger[eAlphas.length];
        BigInteger eOne = encrypt(publicKey, BigInteger.ONE);
        for (int i = 0; i < eAlphas.length; i++) {
            eOneMinusAlphas[i] = homomorphicSubstraction(eOne, eAlphas[i], publicKey.nSqure);
        }

        BigInteger[] eSums = new BigInteger[num];
        int offset = 0;
        for (int i = 0; i < num; i++) {
            BigInteger eSum = encrypt(publicKey, BigInteger.ZERO);

            index = i - 1;
            for (int j = 0; j < num - 1; j++) {
                if (j < i) {
                    eSum = homomorphicAddition(eSum, eOneMinusAlphas[index], publicKey.nSqure);
                    index += num - 2 - j;
                } else {
                    index = offset + j - i;
                    eSum = homomorphicAddition(eSum, eAlphas[index], publicKey.nSqure);
                }
            }

            eSums[i] = eSum;
            offset += num - 1 - i;
        }
        System.out.println("Sum: " + timer.cut() + " ms");

        Util.writeBigIntegers(eSums, writer);
        BigInteger[] eBetas = Util.readBigIntegers(k * num, reader);
        System.out.println("Waiting for C2: " + timer.cut() + " ms");

        BigInteger[] ePoints = new BigInteger[k * num * m];
        BigInteger[] eBetaArray = new BigInteger[k * num * m];
        for (int i = 0; i < k; i++) {
            int offset1 = i * num * m;
            for (int j = 0; j < m; j++) {
                int offset2 = offset1 + j * num;
                for (int l = 0; l < num; l++) {
                    ePoints[offset2 + l] = epList[l][j];
                }
                System.arraycopy(eBetas, i * num, eBetaArray, offset2, num);
            }
        }
        System.out.println("ePoints: " + timer.cut() + " ms");

        BigInteger[] ets = secureMultiplicationSC1(eBetaArray, ePoints, publicKey, reader, writer);
        System.out.println("secureMultiplicationSC1: " + timer.cut() + " ms");

        BigInteger[][] result = new BigInteger[k][m];
        for (int i = 0; i < k; i++) {
            int offset1 = i * num * m;
            for (int j = 0; j < m; j++) {
                int offset2 = offset1 + j * num;

                BigInteger eSum = encrypt(publicKey, BigInteger.ZERO);
                for (int l = 0; l < num; l++) {
                    eSum = homomorphicAddition(eSum, ets[offset2 + l], publicKey.nSqure);
                }

                result[i][j] = eSum;
            }
        }

        System.out.println("result: " + timer.cut() + " ms");

        return result;
    }

    public static void mySecureMinDistanceKPointC2(int k, int num, int m, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int disBitLength = dataLength * 2 + 1;

        secureMultiBitDecompositionC2(num, disBitLength, privateKey, reader, writer);

        secureCompareSC2(num * (num - 1) / 2, disBitLength, privateKey, reader, writer);

        BigInteger[] eSums = Util.readBigIntegers(num, reader);

        // <index, rank>
        Queue<SimpleEntry<Integer, BigInteger>> queue = new PriorityQueue<>((e1, e2) -> {
            return e1.getValue().compareTo(e2.getValue());
        });
        BigInteger bigNum = BigInteger.valueOf(num);
        for (int i = 0; i < num; i++) {
            BigInteger t = decrypt(privateKey, eSums[i]);

            queue.add(new SimpleEntry<Integer, BigInteger>(Integer.valueOf(i), bigNum.subtract(t)));
        }

        BigInteger[] eBetas = new BigInteger[k * num];
        for (int i = 0; i < k; i++) {
            int index = queue.poll().getKey().intValue();

            int offset = i * num;
            for (int j = 0; j < num; j++) {
                if (j == index) {
                    eBetas[offset + j] = encrypt(privateKey, BigInteger.ONE);
                } else {
                    eBetas[offset + j] = encrypt(privateKey, BigInteger.ZERO);
                }
            }
        }

        Util.writeBigIntegers(eBetas, writer);

        secureMultiplicationSC2(k * num * m, privateKey, reader, writer);
    }

    public static BigInteger mySecureMINKthC1(int k, BigInteger[] exs, int dataLength, PaillierPublicKey publicKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        RunningTimer timer = new RunningTimer(RunningTimer.TimeType.MS);
        System.out.println("Start: " + timer.cut() + " ms");
        int num = exs.length;

        BigInteger[][] exsBD = secureMultiBitDecompositionC1(exs, dataLength, publicKey, reader, writer);
        System.out.println("secureMultiBitDecompositionC1: " + timer.cut() + " ms");

        int size = num * (num - 1) / 2;
        BigInteger[][] exArray1BD = new BigInteger[size][];
        BigInteger[][] exArray2BD = new BigInteger[size][];
        int index = 0;
        for (int i = 0; i < num - 1; i++) {
            int length = num - 1 - i;
            Arrays.fill(exArray1BD, index, index + length, exsBD[i]);
            System.arraycopy(exsBD, i + 1, exArray2BD, index, length);

            index += length;
        }

        BigInteger[] eAlphas = secureCompareSC1(exArray1BD, exArray2BD, publicKey, reader, writer);
        System.out.println("secureCompareSC1: " + timer.cut() + " ms");

        BigInteger[] eOneMinusAlphas = new BigInteger[eAlphas.length];
        BigInteger eOne = encrypt(publicKey, BigInteger.ONE);
        for (int i = 0; i < eAlphas.length; i++) {
            eOneMinusAlphas[i] = homomorphicSubstraction(eOne, eAlphas[i], publicKey.nSqure);
        }

        BigInteger[] eSums = new BigInteger[num];
        int offset = 0;
        for (int i = 0; i < num; i++) {
            BigInteger eSum = encrypt(publicKey, BigInteger.ZERO);

            index = i - 1;
            for (int j = 0; j < num - 1; j++) {
                if (j < i) {
                    eSum = homomorphicAddition(eSum, eOneMinusAlphas[index], publicKey.nSqure);
                    index += num - 2 - j;
                } else {
                    index = offset + j - i;
                    eSum = homomorphicAddition(eSum, eAlphas[index], publicKey.nSqure);
                }
            }

            eSums[i] = eSum;
            offset += num - 1 - i;
        }
        System.out.println("Sum: " + timer.cut() + " ms");

        Util.writeBigIntegers(eSums, writer);
        BigInteger[] eBetas = Util.readBigIntegers(num, reader);
        System.out.println("Waiting for C2: " + timer.cut() + " ms");

        BigInteger[] ets = secureMultiplicationSC1(eBetas, exs, publicKey, reader, writer);
        System.out.println("secureMultiplicationSC1: " + timer.cut() + " ms");

        BigInteger result = encrypt(publicKey, BigInteger.ZERO);
        for (int i = 0; i < num; i++) {
            result = homomorphicAddition(result, ets[i], publicKey.nSqure);
        }
        System.out.println("result: " + timer.cut() + " ms");

        return result;
    }

    public static void mySecureMINKthC2(int k, int num, int dataLength, PaillierPrivateKey privateKey,
            BufferedReader reader, PrintWriter writer) throws IOException {

        secureMultiBitDecompositionC2(num, dataLength, privateKey, reader, writer);

        secureCompareSC2(num * (num - 1) / 2, dataLength, privateKey, reader, writer);

        BigInteger[] eSums = Util.readBigIntegers(num, reader);

        // <index, rank>
        List<SimpleEntry<Integer, BigInteger>> list = new ArrayList<>();
        BigInteger bigNum = BigInteger.valueOf(num);
        for (int i = 0; i < num; i++) {
            BigInteger t = decrypt(privateKey, eSums[i]);

            list.add(new SimpleEntry<Integer, BigInteger>(Integer.valueOf(i), bigNum.subtract(t)));
        }

        list.sort((e1, e2) -> {
            return e1.getValue().compareTo(e2.getValue());
        });

        BigInteger[] eBetas = new BigInteger[num];
        int index = list.get(k - 1).getKey();
        for (int i = 0; i < num; i++) {
            if (i == index) {
                eBetas[i] = encrypt(privateKey, BigInteger.ONE);
            } else {
                eBetas[i] = encrypt(privateKey, BigInteger.ZERO);
            }
        }

        Util.writeBigIntegers(eBetas, writer);

        secureMultiplicationSC2(num, privateKey, reader, writer);
    }

    /**
     * Paillier SKNN
     * 
     * @return
     * @throws IOException
     */
    public static BigInteger[][] secureKNNC1(int k, BigInteger[][][] eBuckets, BigInteger[][] elbs, BigInteger[][] eubs,
            BigInteger[] eq, int dataLength,
            PaillierPublicKey publicKey, BufferedReader reader, PrintWriter writer) throws IOException {

        RunningTimer timer = new RunningTimer(RunningTimer.TimeType.MS);
        System.out.println("Start: " + timer.cut() + " ms");

        int bNum = eBuckets.length;
        // int bSize = eBuckets[0].length;
        int m = eBuckets[0][0].length;
        int disBitLength = dataLength * 2;
        int sigma = dataLength + 40;

        // BigInteger[][] eqBD = new BigInteger[m][dataLength];
        // for (int i = 0; i < m; i++) {
        //     eqBD[i] = secureBitDecompositionC1(eq[i], dataLength, publicKey, reader, writer);
        // }

        // BigInteger[][][] elbBDs = new BigInteger[bNum][m][dataLength];
        // BigInteger[][][] eubBDs = new BigInteger[bNum][m][dataLength];
        // for (int i = 0; i < bNum; i++) {
        //     for (int j = 0; j < m; j++) {
        //         elbBDs[i][j] = secureBitDecompositionC1(elbs[i][j], dataLength, publicKey, reader, writer);
        //         eubBDs[i][j] = secureBitDecompositionC1(eubs[i][j], dataLength, publicKey, reader, writer);
        //     }
        // }

        BigInteger[] ePointSet = new BigInteger[m + bNum * m * 2];
        System.arraycopy(eq, 0, ePointSet, 0, m);
        int setIndex = m;
        for (int i = 0; i < bNum; i++) {
            for (int j = 0; j < m; j++) {
                ePointSet[setIndex++] = elbs[i][j];
                ePointSet[setIndex++] = eubs[i][j];
            }
        }
        System.out.println("ePointSet generate: " + timer.cut() + " ms");

        BigInteger[][] ePointSetBD = secureMultiBitDecompositionC1(ePointSet, dataLength, publicKey, reader, writer);
        System.out.println("ePointSet SBD: " + timer.cut() + " ms");

        BigInteger[][] eqBD = new BigInteger[m][dataLength];
        BigInteger[][][] elbBDs = new BigInteger[bNum][m][dataLength];
        BigInteger[][][] eubBDs = new BigInteger[bNum][m][dataLength];
        System.arraycopy(ePointSetBD, 0, eqBD, 0, m);
        setIndex = m;
        for (int i = 0; i < bNum; i++) {
            for (int j = 0; j < m; j++) {
                elbBDs[i][j] = ePointSetBD[setIndex++];
                eubBDs[i][j] = ePointSetBD[setIndex++];
            }
        }
        System.out.println("ePointSet distribute: " + timer.cut() + " ms");

        // BigInteger[] eAlphas = new BigInteger[bNum];
        // for (int i = 0; i < bNum; i++) {
        //     eAlphas[i] = securePointEnclosureC1(eqBD, elbBDs[i], eubBDs[i], publicKey, reader, writer);
        // }
        BigInteger[][][] eqBDs = new BigInteger[bNum][][];
        for (int i = 0; i < bNum; i++) {
            eqBDs[i] = eqBD;
        }
        BigInteger[] eAlphas = securePointEnclosureSC1(eqBDs, elbBDs, eubBDs, publicKey, reader, writer);
        System.out.println("SPE: " + timer.cut() + " ms");

        BigInteger[][] eBPoints = secureReadC1(eBuckets, eAlphas, publicKey, reader, writer);  
        System.out.println("First SREAD: " + timer.cut() + " ms");

        Util.writeInt(eBPoints.length, writer);

        // BigInteger[] eDistances = new BigInteger[eBPoints.length];
        // for (int i = 0; i < eBPoints.length; i++) {
        //     eDistances[i] = enhancedSecureSquaredEuclideanDistanceC1(eBPoints[i], eq, sigma, publicKey, reader, writer);
        // }
        BigInteger[][] eqs = new BigInteger[eBPoints.length][];
        for (int i = 0; i < eBPoints.length; i++) {
            eqs[i] = eq;
        }
        BigInteger[] eDistances = enhancedSecureSquaredEuclideanDistanceSC1(eBPoints, eqs, sigma, publicKey, reader,
                writer);
        System.out.println("First ESSED: " + timer.cut() + " ms");

        BigInteger ekthD = secureMinKthC1(k, eDistances, disBitLength, publicKey, reader, writer);
        // BigInteger ekthD = mySecureMINKthC1(k, eDistances, disBitLength, publicKey, reader, writer);
        System.out.println("Kth distance: " + timer.cut() + " ms");

        // BigInteger[] eBQDistances = new BigInteger[bNum];
        // for (int i = 0; i < bNum; i++) {
        //     eBQDistances[i] = secureMinDistanceFromBoxToPointC1(dataLength, eq, elbs[i], eubs[i], publicKey, reader,
        //             writer);
        // }
        eqs = new BigInteger[bNum][];
        for (int i = 0; i < bNum; i++) {
            eqs[i] = eq;
        }
        BigInteger[] eBQDistances = secureMinDistanceFromBoxToPointSC1(dataLength, eqs, elbs, eubs, publicKey, reader,
                writer);
        System.out.println("box to point distance: " + timer.cut() + " ms");

        // BigInteger[] eBetas = new BigInteger[bNum];
        BigInteger[] eKthDisBD = secureBitDecompositionC1(ekthD, disBitLength, publicKey, reader, writer);
        BigInteger[][] eKthDisBDs = new BigInteger[bNum][];
        BigInteger[][] eDsBD = secureMultiBitDecompositionC1(eBQDistances, disBitLength, publicKey, reader, writer);
        for (int i = 0; i < bNum; i++) {
            // BigInteger[] eDiBD = secureBitDecompositionC1(eBQDistances[i], disBitLength, publicKey, reader, writer);
            // BigInteger[] eDiBD = eDsBD[i];

            // eBetas[i] = secureCompareC1(eDiBD, eKthDisBD, publicKey, reader, writer); // bool(d_i < d_k)
            eKthDisBDs[i] = eKthDisBD;
        }
        BigInteger[] eBetas = secureCompareSC1(eDsBD, eKthDisBDs, publicKey, reader, writer);
        System.out.println("distance compare: " + timer.cut() + " ms");

        eBetas = secureBitsXORC1(eBetas, eAlphas, publicKey, reader, writer);
        System.out.println("XOR: " + timer.cut() + " ms");

        // SRead
        BigInteger[][] eNPoints = secureReadC1(eBuckets, eBetas, publicKey, reader, writer); 
        System.out.println("Second SREAD: " + timer.cut() + " ms");

        BigInteger[][] eAllPoints;
        if (eNPoints == null) {
            eAllPoints = new BigInteger[eBPoints.length][];
            System.arraycopy(eBPoints, 0, eAllPoints, 0, eBPoints.length);
        } else {
            eAllPoints = new BigInteger[eBPoints.length + eNPoints.length][];
            System.arraycopy(eBPoints, 0, eAllPoints, 0, eBPoints.length);
            System.arraycopy(eNPoints, 0, eAllPoints, eBPoints.length, eNPoints.length);
        }
        System.out.println("eAllPoints generate: " + timer.cut() + " ms");

        Util.writeInt(eAllPoints.length, writer);
        // BigInteger[] eAllDistances = new BigInteger[eAllPoints.length];
        // for (int i = 0; i < eAllDistances.length; i++) {
        //     eAllDistances[i] = enhancedSecureSquaredEuclideanDistanceC1(eAllPoints[i], eq, sigma, publicKey, reader,
        //             writer);
        // }
        eqs = new BigInteger[eAllPoints.length][];
        for (int i = 0; i < eAllPoints.length; i++) {
            eqs[i] = eq;
        }
        BigInteger[] eAllDistances = enhancedSecureSquaredEuclideanDistanceSC1(eAllPoints, eqs, sigma, publicKey,
                reader, writer);
        System.out.println("point to point ESSED: " + timer.cut() + " ms");

        // SMINK
        BigInteger[][] result = secureMinDistanceKPointC1(k, eAllPoints, eAllDistances, dataLength, publicKey,
                reader, writer);
        // BigInteger[][] result = mySecureMinDistanceKPointC1(k, eAllPoints, eAllDistances, dataLength, publicKey, reader, writer);
        // System.out.println("SMIN K: " + timer.cut() + " ms");

        return result;
    }

    public static void secureKNNC2(int k, int bNum, int bSize, int m, int dataLength,
            PaillierPrivateKey privateKey, BufferedReader reader, PrintWriter writer) throws IOException {

        int sigma = dataLength + 40;
        int disBitLength = dataLength * 2;

        // for (int i = 0; i < m; i++) {
        //     secureBitDecompositionC2(dataLength, privateKey, reader, writer);
        // }

        // for (int i = 0; i < bNum; i++) {
        //     for (int j = 0; j < m; j++) {
        //         secureBitDecompositionC2(dataLength, privateKey, reader, writer);
        //         secureBitDecompositionC2(dataLength, privateKey, reader, writer);
        //     }
        // }
        secureMultiBitDecompositionC2(m + bNum * m * 2, dataLength, privateKey, reader, writer);

        // for (int i = 0; i < bNum; i++) {
        //     securePointEnclosureC2(m, dataLength, privateKey, reader, writer);
        // }
        securePointEnclosureSC2(bNum, m, dataLength, privateKey, reader, writer);

        secureReadC2(bNum, bSize, m, privateKey, reader, writer);

        int eBPointsLength = Util.readInt(reader);
        // for (int i = 0; i < eBPointsLength; i++) {
        //     enhancedSecureSquaredEuclideanDistanceC2(privateKey, m, sigma, reader, writer);
        // }
        enhancedSecureSquaredEuclideanDistanceSC2(privateKey, eBPointsLength, m, sigma, reader, writer);

        secureMinKthC2(k, eBPointsLength, disBitLength, privateKey, reader, writer);
        // mySecureMINKthC2(k, eBPointsLength, disBitLength, privateKey, reader, writer);

        // for (int i = 0; i < bNum; i++) {
        //     secureMinDistanceFromBoxToPointC2(m, dataLength, privateKey, reader, writer);
        // }
        secureMinDistanceFromBoxToPointSC2(bNum, m, dataLength, privateKey, reader, writer);

        secureBitDecompositionC2(disBitLength, privateKey, reader, writer);
        secureMultiBitDecompositionC2(bNum, disBitLength, privateKey, reader, writer);
        // for (int i = 0; i < bNum; i++) {
        //     // secureBitDecompositionC2(disBitLength, privateKey, reader, writer);
        //     secureCompareC2(disBitLength, privateKey, reader, writer);
        // }
        secureCompareSC2(bNum, disBitLength, privateKey, reader, writer);

        secureBitsXORC2(bNum, privateKey, reader, writer);

        // SRead
        secureReadC2(bNum, bSize, m, privateKey, reader, writer);

        int eAllPointsLen = Util.readInt(reader);
        // for (int i = 0; i < eAllPointsLen; i++) {
        //     enhancedSecureSquaredEuclideanDistanceC2(privateKey, m, sigma, reader, writer);
        // }
        enhancedSecureSquaredEuclideanDistanceSC2(privateKey, eAllPointsLen, m, sigma, reader, writer);

        // SMINK
        secureMinDistanceKPointC2(k, eAllPointsLen, m, dataLength, privateKey, reader, writer);
        // mySecureMinDistanceKPointC2(k, eAllPointsLen, m, dataLength, privateKey, reader, writer);
    }

    public static void main(String[] args) {

        int testNumber = 10000;
        int length = 100;

        Paillier paillier = new Paillier(length);
        PaillierPublicKey publicKey = paillier.getPaillierPublicKey();
        PaillierPrivateKey privateKey = paillier.getPaillierPrivateKey();

        Random random = new Random();
        for (int i = 0; i < testNumber; i++) {
            BigInteger m = new BigInteger(publicKey.n.bitLength(), random);
            if (m.compareTo(publicKey.n) >= 0)
                continue;

            BigInteger c = Paillier.encrypt(publicKey, m);
            BigInteger dc = Paillier.decrypt(privateKey, c);
            if (!dc.equals(m)) {
                System.out.println("ERROR!");
                System.out.println("m = " + m);
                System.out.println("decrypt c = " + dc);
            }
        }

        System.out.println("n = " + publicKey.n);

        BigInteger m1 = BigInteger.valueOf(23);
        BigInteger m2 = BigInteger.valueOf(13);
        BigInteger c1 = Paillier.encrypt(publicKey, m1);
        BigInteger c2 = Paillier.encrypt(publicKey, m2);

        System.out.println("m1 + m2 = " + m1.add(m2).mod(publicKey.n));
        System.out.println("pc：" + decrypt(privateKey, homomorphicAddition(c1, m2, publicKey)));
        System.out.println("cc：" + decrypt(privateKey, homomorphicAddition(c1, c2, publicKey.nSqure)));

        System.out.println("m1 - m2 = " + m1.subtract(m2).mod(publicKey.n));
        System.out.println("cp：" + decrypt(privateKey, homomorphicSubstraction(c1, m2, publicKey)));
        System.out.println("cc：" + decrypt(privateKey, homomorphicSubstraction(c1, c2, publicKey.nSqure)));

        System.out.println("m1 * m2 = " + m1.multiply(m2).mod(publicKey.n));
        System.out.println("cp：" + decrypt(privateKey, homomorphicMultiplication(c1, m2, publicKey.nSqure)));

        System.out.println("m1 * (-1) = " + m1.multiply(BigInteger.ONE.negate()).mod(publicKey.n));
        System.out.println("c1^{n-1} = " + decrypt(privateKey,
                homomorphicMultiplication(c1, publicKey.n.subtract(BigInteger.ONE), publicKey.nSqure)));
        System.out.println("c1^{-1} = "
                + decrypt(privateKey, homomorphicMultiplication(c1, BigInteger.ONE.negate(), publicKey.nSqure)));

        // String publicKeyJson = parsePublicKeyToJson(publicKey);
        // System.out.println(publicKeyJson);
        // PaillierPublicKey pubK = parseJsonToPublicKey(publicKeyJson);

        // String privateKeyJson = parsePrivateKeyToJson(privateKey);
        // System.out.println(privateKeyJson);
        // PaillierPrivateKey privK = parseJsonToPrivateKey(privateKeyJson);
        // System.out.println("wwww");

        // /**** Data Packing ***/
        // BigInteger[] datas = new BigInteger[] { BigInteger.valueOf(1), BigInteger.valueOf(2),
        //         BigInteger.valueOf(3) };
        // int sigma = 20;
        // BigInteger x = dataPacking(datas, sigma);
        // BigInteger[] dataUnpacking = dataUnpacking(x, datas.length, sigma);

        // BigInteger[] edatas = new BigInteger[datas.length];
        // for (int i = 0; i < datas.length; i++) {
        //     edatas[i] = encrypt(publicKey, datas[i]);
        // }
        // x = dataPacking(edatas, sigma, publicKey);
        // dataUnpacking = dataUnpacking(x, datas.length, sigma, privateKey);

        // System.out.println();

        /*** SBN ***/
        // BigInteger a = BigInteger.valueOf(11);
        // System.out.println(a.toString(2));
        // BigInteger ea = encrypt(publicKey, a);
        // BigInteger eaBitNot = secureBitNot(ea, publicKey);
        // BigInteger aBitNot = decrypt(privateKey, eaBitNot);
        // System.out.println(aBitNot.bitLength());
        // System.out.println(aBitNot.toString(2));
        // System.out.println(aBitNot);
        // System.out.println(a.negate().add(BigInteger.ONE).mod(publicKey.n));

        // BigInteger[] aBitEncrypt = binaryBitEncrypt(a, 10, publicKey);
        // System.out.println(a.toString(2));
        // for (int i = 0; i < aBitEncrypt.length; i++) {
        //     BigInteger t = decrypt(privateKey, aBitEncrypt[i]);
        //     System.out.print(t);
        // }

        // long timeSum = 0l;
        // for (int i = 0; i < testNumber; i++) {
        //     BigInteger x = Util.getRandomBigInteger(publicKey.n);

        //     long timePre = System.nanoTime();
        //     encrypt(publicKey, x);
        //     timeSum += System.nanoTime() - timePre;
        // }
        // System.out.println("Time: " + timeSum / testNumber);

    }
}
